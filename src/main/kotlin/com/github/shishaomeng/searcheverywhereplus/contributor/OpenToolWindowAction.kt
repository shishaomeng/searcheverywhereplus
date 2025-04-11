package com.github.shishaomeng.searcheverywhereplus.contributor

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import javax.swing.Icon

class OpenToolWindowAction(text: String, icon : Icon, private val twId: String) : AnAction() {
    init {
        this.templatePresentation.text = text
        this.templatePresentation.icon = icon
    }

    override fun actionPerformed(p0: AnActionEvent) {
        p0.project?.let {
            val toolWindowManager = ToolWindowManager.getInstance(it)
            val toolWindow: ToolWindow = toolWindowManager.getToolWindow(twId) ?: return
            if (toolWindow.isVisible) {
                toolWindow.hide()
            }else {
                toolWindow.show()
            }
        }
    }

    private fun openToolWindow(twId: String, currentProject: Project) {
        val toolWindowManager = ToolWindowManager.getInstance(currentProject)
        val toolWindow: ToolWindow? = toolWindowManager.getToolWindow(twId)
        toolWindow?.show()
    }
}