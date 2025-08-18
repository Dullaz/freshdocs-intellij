package com.github.dullaz.freshdocsplugin.annotator

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import java.util.regex.Pattern

class FreshLineMarkerProvider : LineMarkerProvider {

    companion object {
        private val FRESH_PATTERN = Pattern.compile(
            "<!---\\s*fresh:file\\s+(\\S+)(?:\\s+([a-f0-9]+))?\\s*-->"
        )
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is LeafPsiElement) return null

        try {
            // Only process leaf elements (tokens) that contain our pattern
            val text = element.text ?: return null
            if (!text.contains("fresh:file")) return null

            val matcher = FRESH_PATTERN.matcher(text)
            if (!matcher.find()) return null

            val filePathWithRepo = matcher.group(1) ?: return null
            val filePath = filePathWithRepo.split(":").last()
            val hash = matcher.group(2)

            // Check if the file/directory exists before showing the icon
            val fix = NavigateToFileFix(filePath, hash)
            if (!fix.isAvailable(element.project, null, null)) {
                return null // Don't show icon for non-existent paths
            }

            return LineMarkerInfo(
                element,
                element.textRange,
                com.intellij.icons.AllIcons.Actions.Find, // Navigation icon
                { "Navigate to $filePath" }, // Tooltip function
                { _, _ ->
                    // Click handler
                    fix.invoke(element.project, null, null)
                },
                GutterIconRenderer.Alignment.LEFT,
                { "Navigate to $filePath" } // Accessibility name
            )

        } catch (e: Exception) {
            // Silently ignore errors to avoid breaking the editor
            return null
        }
    }
}