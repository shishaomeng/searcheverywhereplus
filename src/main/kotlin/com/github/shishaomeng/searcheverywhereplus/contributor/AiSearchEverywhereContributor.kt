package com.github.shishaomeng.searcheverywhereplus.contributor

import com.github.shishaomeng.searcheverywhereplus.service.BLLLMService
import com.github.shishaomeng.searcheverywhereplus.service.ChatCompletionResponse
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.searcheverywhere.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.Processor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.intellij.lang.annotations.Language
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.Dimension
import java.io.InputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import javax.swing.*

/**
 * 智能搜索“随处搜”贡献器
 *
 * @param event 动作事件
 * @author shishaomeng
 */
class AiSearchEverywhereContributor(event: AnActionEvent) : WeightedSearchEverywhereContributor<SearchResult>,
    SearchEverywhereExtendedInfoProvider,
    SearchEverywherePreviewProvider {
    private val searchIcon = IconLoader.getIcon("/icons/search/taobao.svg", javaClass)
    private val aiSearchIcon = IconLoader.getIcon("/icons/search/aisearch2.svg", javaClass)
    private val deepSeekIcon = IconLoader.getIcon("/icons/search/deepseek.svg", javaClass)
    private var dajuIcon = IconLoader.getIcon("/icons/search/daju.svg", javaClass)

    private val workBaseUrl = "https://work.alibaba-inc.com/nwpipe/newsearch?type=all&keywords="
    private val codeBaseUrl = "https://code.alibaba-inc.com/dashboard/search?type=code&q="

    private val currentProject: Project = event.project ?: throw IllegalStateException("Project is null")

    private val client = BLLLMService()

    override fun getSearchProviderId(): String = "DeepSeekProvider"
    override fun getGroupName(): String = "客运内外"
    override fun getSortWeight(): Int = 10

    override fun processSelectedItem(selected: SearchResult, p1: Int, p2: String): Boolean {
        when (selected.action.type) {
            "openBrowser" -> {
                selected.action.param.let { openBrowser(it) }
            }

            "openToolWindow" -> {
                openToolWindow(selected.action.param)
            }

            "showLLMDialog" -> {
                showLLMDialog(selected)
            }
        }
        return true
    }



    override fun showInFindResults(): Boolean = true
    override fun isShownInSeparateTab(): Boolean = true

    override fun fetchWeightedElements(
        pattern: String,
        progressIndicator: ProgressIndicator,
        consumer: Processor<in FoundItemDescriptor<SearchResult>>
    ) {
        val items = listOf(
            SearchResult("搜内外 ", pattern, "Jump to Alibaba Work", Action("openBrowser", workBaseUrl + pattern, searchIcon)),
            SearchResult("搜代码 ", pattern, "Jump to Alibaba Code", Action("openBrowser", codeBaseUrl + pattern, searchIcon)),
//            SearchResult("问大局 ", pattern, "Search internal RD knowledge", Action("openToolWindow", "Daju Assist", dajuIcon)),
            SearchResult("AI搜索 ", pattern, deepSeekIcon, Action("showLLMDialog", pattern, aiSearchIcon)),
        )
        items.forEach({
            consumer.process(
                FoundItemDescriptor(
                    it, 1000
                )
            )
        })
    }


    override fun getElementsRenderer(): ListCellRenderer<in SearchResult> {
        return AiSearchResultItemRenderer()
    }

    override fun getDataForItem(element: SearchResult, dataId: String): Any? = null

    override fun createExtendedInfo(): ExtendedInfo {
        val description = fun(_: Any): String { return "由客户运营技术提供支持" }
        val shortcut = fun(_: Any?): AnAction { return OpenToolWindowAction(
            "Talk to Daju Assistant",
            icon = AllIcons.Actions.Search,
            twId = "Daju"
        )}

        return ExtendedInfo(description, shortcut)
    }

    private fun showLLMDialog(item: SearchResult) {
        val currentSearchEverywhereUI = SearchEverywhereManager.getInstance(currentProject).currentlyShownUI
        // 创建加载面板
        val loadingPanel = JPanel(BorderLayout()).apply {
            preferredSize = Dimension(800, 600)
        }

        // 创建 JDialog
        JDialog().apply {
            title =
                "智能搜索 >>>  ${if (item.searchText.length > 30) "${item.searchText.take(30)}..." else item.searchText}"
            contentPane = JPanel(BorderLayout()).apply {
                add(loadingPanel, BorderLayout.CENTER)
            }
            isResizable = true // 允许调整大小
            setSize(800, 600) // 设置初始大小
            setLocationRelativeTo(currentSearchEverywhereUI)
            isVisible = true
        }

        // 异步调用 LLM
        fun performQuery() {
            loadingPanel.removeAll()
            val label = JBLabel("搜索结果智能生成中...", AnimatedIcon.Default(), SwingConstants.CENTER).apply {
                horizontalAlignment = SwingConstants.CENTER
                verticalAlignment = SwingConstants.CENTER
            }
            loadingPanel.add(label, BorderLayout.CENTER)
            loadingPanel.revalidate()
            loadingPanel.repaint()

            // 启动协程处理异步请求
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = client.search(item.searchText)
                    withContext(Dispatchers.EDT) {
                        loadingPanel.removeAll()
                        val markdownViewer = createMarkdownPreviewComponent(item.searchText, response)
                        val scrollPane = JBScrollPane(markdownViewer).apply {
                            preferredSize = Dimension(600, 400) // 设置滚动面板的大小
                        }
                        loadingPanel.add(scrollPane, BorderLayout.CENTER)
                        loadingPanel.revalidate()
                        loadingPanel.repaint()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.EDT) {
                        loadingPanel.removeAll()
                        // 创建错误信息面板
                        val errorPanel = JPanel(BorderLayout()).apply {
                            add(JBLabel("发生错误: ${e.message}", UIUtil.ComponentStyle.LARGE).apply {
                                horizontalAlignment = SwingConstants.CENTER
                                verticalAlignment = SwingConstants.CENTER
                                preferredSize = Dimension(600, 100)
                            }, BorderLayout.CENTER)
                            add(JButton("重试", AllIcons.Actions.Refresh).apply {
                                preferredSize = Dimension(100, 30)
                                addActionListener { performQuery() }
                            }, BorderLayout.AFTER_LAST_LINE)
                        }
                        loadingPanel.add(errorPanel, BorderLayout.CENTER)
                        loadingPanel.revalidate()
                        loadingPanel.repaint()
                    }
                }
            }
        }

        // 首次执行查询
        performQuery()
    }


    /*
     * 创建 Markdown 预览组件
     *
     * @param query 查询语句
     * @param response 响应
     */
    private fun createMarkdownPreviewComponent(query: String, response: ChatCompletionResponse): JComponent {
        // 检查 JCEF 是否支持
        if (!JBCefApp.isSupported()) {
            return JPanel().apply {
                add(JBLabel("JCEF is not supported on this platform"))
            }
        }

        // 创建 JCEF 浏览器
        val browser = JBCefBrowser().apply {
            // 设置浏览器大小
            component.preferredSize = JBUI.size(600, 400)
        }

        // 注册到父 Disposable
        Disposer.register(currentProject, browser)

        @Language("Markdown")
        val content = response.choices.firstOrNull()?.message?.content ?: "** No data **"

        // 生成 Markdown HTML
        val contentHtml = runReadAction {
            // 使用 Markdown 解析器和 HTML 生成器
            val flavour = CommonMarkFlavourDescriptor()
            val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(content)
            HtmlGenerator(content, parsedTree, flavour).generateHtml()
        }

        // 设置 HTML 内容
        browser.loadHTML(createFullHtml(query, response, contentHtml))

        return browser.component
    }

    /*
     * 创建 Markdown 预览的完整HTML
     *
     * @param query 查询语句
     * @param response 响应
     * @param markdownHtml Markdown文本生成的 HTML
     * @return 完整的 HTML
     */
    private fun createFullHtml(query: String, response: ChatCompletionResponse, markdownHtml: String): String {

        val templateStream: InputStream = javaClass.getResourceAsStream("/ai_search_result_page.html")
            ?: return "ai_search_result_page not found"

        val templateContent = templateStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

        return templateContent
            .replace("{query}", query)
            .replace("{modelName}", response.model)
            .replace("{completionTokens}", response.usage.completion_tokens.toString())
            .replace("{totalTokens}", response.usage.total_tokens.toString())
            .replace("{markdownHtml}", markdownHtml)
    }


    private fun openBrowser(url: String) {
        val desktop: Desktop? = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(URI.create(url))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun openToolWindow(toolWindowId: String) {
        val toolWindowManager = ToolWindowManager.getInstance(currentProject)
        val toolWindow: ToolWindow? = toolWindowManager.getToolWindow(toolWindowId)
        toolWindow?.show()
    }
}