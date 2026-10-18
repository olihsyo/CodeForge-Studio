package com.codeforge.studio.editor

import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONArray
import org.json.JSONObject

class CodeEditorBridge(
    private val onContentChangedCallback: (String, Boolean) -> Unit,
    private val onCursorChangedCallback: (Int, Int) -> Unit,
    private val onReadyCallback: () -> Unit
) {

    @JavascriptInterface
    fun onContentChanged(content: String, isDirty: Boolean) {
        onContentChangedCallback(content, isDirty)
    }

    @JavascriptInterface
    fun onCursorChanged(line: Int, col: Int) {
        onCursorChangedCallback(line, col)
    }

    @JavascriptInterface
    fun onEditorReady() {
        onReadyCallback()
    }

    companion object {
        fun setContent(webView: WebView, content: String, language: String) {
            val jsonContent = JSONObject.quote(content)
            val jsonLang = JSONObject.quote(language)
            webView.evaluateJavascript("window.CodeForgeEditor && window.CodeForgeEditor.setContent($jsonContent, $jsonLang);", null)
        }

        fun setLanguage(webView: WebView, language: String) {
            val jsonLang = JSONObject.quote(language)
            webView.evaluateJavascript("window.CodeForgeEditor && window.CodeForgeEditor.setLanguage($jsonLang);", null)
        }

        fun setFontSize(webView: WebView, sizeSp: Int) {
            webView.evaluateJavascript("window.CodeForgeEditor && window.CodeForgeEditor.setFontSize($sizeSp);", null)
        }

        fun setTabWidth(webView: WebView, tabWidth: Int) {
            webView.evaluateJavascript("window.CodeForgeEditor && window.CodeForgeEditor.setTabWidth($tabWidth);", null)
        }

        fun setWordWrap(webView: WebView, enabled: Boolean) {
            webView.evaluateJavascript("window.CodeForgeEditor && window.CodeForgeEditor.setWordWrap($enabled);", null)
        }

        fun gotoLine(webView: WebView, line: Int) {
            webView.evaluateJavascript("window.CodeForgeEditor && window.CodeForgeEditor.gotoLine($line);", null)
        }

        fun undo(webView: WebView) {
            webView.evaluateJavascript("window.CodeForgeEditor && window.CodeForgeEditor.undo();", null)
        }

        fun redo(webView: WebView) {
            webView.evaluateJavascript("window.CodeForgeEditor && window.CodeForgeEditor.redo();", null)
        }

        fun search(webView: WebView, query: String, replaceWith: String? = null, replaceAll: Boolean = false) {
            val jsonQuery = JSONObject.quote(query)
            val jsonReplace = if (replaceWith != null) JSONObject.quote(replaceWith) else "null"
            webView.evaluateJavascript("window.CodeForgeEditor && window.CodeForgeEditor.search($jsonQuery, $jsonReplace, $replaceAll);", null)
        }

        fun setDiagnostics(webView: WebView, diagnosticsJson: String) {
            webView.evaluateJavascript("window.CodeForgeEditor && window.CodeForgeEditor.setDiagnostics($diagnosticsJson);", null)
        }

        fun insertText(webView: WebView, text: String) {
            val jsonText = JSONObject.quote(text)
            webView.evaluateJavascript("window.CodeForgeEditor && window.CodeForgeEditor.insertText($jsonText);", null)
        }

        fun format(webView: WebView) {
            webView.evaluateJavascript("window.CodeForgeEditor && window.CodeForgeEditor.formatDocument && window.CodeForgeEditor.formatDocument();", null)
        }
    }
}
