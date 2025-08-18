package com.github.dullaz.freshdocsplugin.annotator

data class FreshOutput(
    val line: Int,
    val docPath: String,
    val message: String,
)

fun parseFreshOutput(output: String): FreshOutput? {
    // sample outputs
    //docs/howto.md:1 is stale! 32fa99d -> b427e33
    //docs/howto.md:2 target file not found
    //docs/howto.md:3 is invalid! no hash

    val parts = output.split(" ", limit = 2)
    if (parts.size < 2) return null
    val fileAndLine = parts[0].split(":")
    if (fileAndLine.size < 2) return null

    val line = fileAndLine[1].toIntOrNull() ?: return null
    val docPath = fileAndLine[0]
    val message = parts[1]

    return FreshOutput(line, docPath, message)
}