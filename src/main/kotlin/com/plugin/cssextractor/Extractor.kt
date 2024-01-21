package com.plugin.cssextractor

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.util.IconLoader
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.awt.datatransfer.StringSelection

class Extractor : AnAction("Extract CSS", "Extract CSS selectors from JSX", IconLoader.getIcon("/logo/small.svg", Extractor::class.java)) {

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val selectionModel = editor?.selectionModel
        val selectedText = selectionModel?.selectedText ?: return
        val isJsx = isJsx(selectedText)
        val selectors = extractCssSelectors(selectedText, isJsx)
        val cssSelectorsString = selectors.joinToString("\n\n") { ".$it {\n\n}" }
        val copyPasteManager = CopyPasteManager.getInstance()
        copyPasteManager.setContents(StringSelection(cssSelectorsString))
    }

    private fun extractCssSelectors(text: String, isJsx: Boolean): Set<String> {
        val doc = if (isJsx) Jsoup.parse(text, "", Parser.xmlParser()) else Jsoup.parse(text)
        val elements = doc.select(if (isJsx) "[class], [className]" else "[class]")
        return elements.flatMap { element ->
            val classNames = if (isJsx) element.attr("className").split("\\s+".toRegex()) else element.classNames()
            classNames.map { className ->
                if (className.contains("styles")) {
                    className.substringAfter("styles.").replace(Regex("[{}'\"`]"), "").trim()
                } else {
                    className.replace(Regex("[{}'\"`]"), "").trim()
                }
            }
        }
        .filter { it.isNotEmpty() }
        .toSet()
    }

    private fun isJsx(text: String): Boolean {
        return "className=" in text
    }
}