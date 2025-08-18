package com.github.dullaz.freshdocsplugin.annotator

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.wm.ToolWindowManager

class NavigateToFileFix(private val filePath: String?, private val hash: String?) : IntentionAction {

    override fun getText(): String = "Navigate to ${filePath ?: "file"}"

    override fun getFamilyName(): String = "Fresh file navigation"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return !filePath.isNullOrBlank() && findTarget(project, filePath) != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (filePath.isNullOrBlank()) return

        try {
            ApplicationManager.getApplication().runReadAction {
                val target = findTarget(project, filePath)

                if (target != null) {
                    ApplicationManager.getApplication().invokeLater {
                        try {
                            if (target.isDirectory) {
                                openAndFocusDirectory(project, target)
                            } else {
                                FileEditorManager.getInstance(project).openFile(target, true)
                            }
                        } catch (e: Exception) {
                            println("Error opening file: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error in NavigateToFileFix: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun startInWriteAction(): Boolean = false

    private fun findTarget(project: Project, path: String): VirtualFile? {
        return try {
            val projectBase = project.projectFile ?: return null

            // Convert colon notation to path separators
            val normalizedPath = path.replace(':', '/')

            // Handle wildcard patterns
            if (normalizedPath.contains("*")) {
                val dirPath = normalizedPath.substringBeforeLast('/')
                return projectBase.findFileByRelativePath(dirPath)
            }

            // Try to find exact file first
            projectBase.findFileByRelativePath(normalizedPath)
                ?: projectBase.findFileByRelativePath("$normalizedPath.go") // Try with .go extension
                ?: projectBase.findFileByRelativePath(normalizedPath).takeIf { it?.isDirectory == true }
        } catch (e: Exception) {
            println("Error finding target: ${e.message}")
            null
        }
    }

    private fun openAndFocusDirectory(project: Project, directory: VirtualFile) {
        try {
            // Open project view if not already open
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val projectViewWindow = toolWindowManager.getToolWindow("Project")

            projectViewWindow?.let { window ->
                if (!window.isVisible) {
                    window.show()
                }
            }

            // Focus on the directory
            val projectView = ProjectView.getInstance(project)
            projectView.select(null, directory, true)
        } catch (e: Exception) {
            println("Error opening directory: ${e.message}")
        }
    }
}