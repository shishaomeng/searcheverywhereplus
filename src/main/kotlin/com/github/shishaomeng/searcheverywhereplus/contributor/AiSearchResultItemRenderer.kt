package com.github.shishaomeng.searcheverywhereplus.contributor

import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*

/**
 * 智能搜索项渲染
 *
 * @author shishaomeng
 */
class AiSearchResultItemRenderer() : ListCellRenderer<SearchResult> {
    override fun getListCellRendererComponent(
        list: JList<out SearchResult>?,
        result: SearchResult,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val panel = JPanel(BorderLayout())
        val titleLabel = JLabel("${result.title}")
        titleLabel.icon = result.action.icon

        val descLabel = JLabel("")
        if (result.description is Icon) {
            descLabel.icon = result.description as Icon
        } else {
            descLabel.text = "<html><span style='color:#71757C'>${result.description}</span></html>"
        }

        val queryLabel = JLabel("<html><b>${if (result.searchText.length > 30) "${result.searchText.take(30)}..." else result.searchText}</b></html>")

        panel.add(titleLabel, BorderLayout.WEST)
        panel.add(queryLabel, BorderLayout.CENTER)
        panel.add(descLabel, BorderLayout.EAST)
        panel.background = if (isSelected) list?.selectionBackground else list?.background
        return panel
    }
}