package com.github.dullaz.freshdocsplugin.annotator

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class StaleDocsAnnotator: ExternalAnnotator<PsiFile, List<StaleDocsAnnotator.ValidationResult>>() {

    data class ValidationResult(val range: TextRange, val message: String)


    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): PsiFile? {
        return file
    }

    override fun doAnnotate(file: PsiFile?): List<ValidationResult>? {
        val filePath = file?.virtualFile?.path ?: return null

        val command = listOf("freshdocs", "validate", filePath)

        val processBuilder = ProcessBuilder(command)
        file.project.basePath?.let { processBuilder.directory(java.io.File(it)) }

        val validationResults = mutableListOf<ValidationResult>()

        try {
            val process = processBuilder.start()

            // Wait for the process to finish
            val success = process.waitFor(10, TimeUnit.SECONDS)
            if (!success) {
                // Handle timeout if necessary
                return null
            }

            // Read the output from the process
            BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8))
                .useLines { lines ->
                    lines.forEach { line ->

                        val output = parseFreshOutput(line) ?: return@forEach

                        // Get the document for the file to convert line number to character offset
                        val document: Document? = file.viewProvider.document
                        if (document != null) {
                            val lineIndex = output.line
                            if (lineIndex >= 0 && lineIndex < document.lineCount) {
                                val startOffset = document.getLineStartOffset(lineIndex)
                                val endOffset = document.getLineEndOffset(lineIndex)
                                val range = TextRange(startOffset, endOffset)

                                // Add the result to our list
                                validationResults.add(ValidationResult(range, output.message))
                            }
                        }
                    }
            }
        } catch (e: Exception) {
            // Log the exception to the IntelliJ console for debugging
            println("freshdocs-plugin: Failed to run CLI tool. Error: ${e.message}")
            return null
        }



        return validationResults
    }

    override fun apply(file: PsiFile, results: List<ValidationResult>?, holder: AnnotationHolder) {
        results ?: return

        for (result in results) {
            holder.newAnnotation(HighlightSeverity.WEAK_WARNING, result.message)
                .range(result.range)
                .highlightType(ProblemHighlightType.WEAK_WARNING)
                .create()
        }
    }
}