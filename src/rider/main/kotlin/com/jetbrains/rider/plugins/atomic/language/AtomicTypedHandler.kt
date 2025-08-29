package com.jetbrains.rider.plugins.atomic.language

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile

class AtomicTypedHandler : TypedHandlerDelegate() {
    
    override fun checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (file !is AtomicFile || !file.isValid) {
            return Result.CONTINUE
        }
        
        
        val caretOffset = editor.caretModel.offset
        val text = editor.document.text
        
        if (charTyped == ':' || charTyped == ' ') {
            var i = caretOffset - 1
            while (i >= 0) {
                val lineStart = text.lastIndexOf('\n', i - 1) + 1
                val line = text.substring(lineStart, i + 1).trim()
                
                if (line.startsWith("values:")) {
                    AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
                    return Result.STOP
                }
                
                if (line.matches(Regex("^(header|imports|tags):\\s*$"))) {
                    break
                }
                
                i = lineStart - 1
                if (i <= 0) break
            }
        }
        
        if (charTyped.isLetter()) {
            val lineStart = text.lastIndexOf('\n', caretOffset - 1) + 1
            val currentLine = text.substring(lineStart, caretOffset)
            
            if (currentLine.matches(Regex("^\\s*-\\s*\\w+:\\s+\\w*$"))) {
                AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
                return Result.STOP
            }
            
            var i = caretOffset - 1
            while (i >= 0 && text[i] != '\n' && text[i] != ':') {
                i--
            }
            if (i >= 0 && text[i] == ':') {
                var searchPos = i
                while (searchPos >= 0) {
                    val searchLineStart = text.lastIndexOf('\n', searchPos - 1) + 1
                    val searchLine = text.substring(searchLineStart, Math.min(searchPos + 10, text.length)).trim()
                    if (searchLine.startsWith("values:")) {
                        AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
                        return Result.STOP
                    }
                    if (searchLine.matches(Regex("^(header|imports|tags):"))) {
                        break
                    }
                    searchPos = searchLineStart - 1
                    if (searchPos <= 0) break
                }
            }
            
            if (currentLine.contains("namespace:") || currentLine.contains("entityType:") || currentLine.trim().startsWith("-")) {
                AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
                return Result.STOP
            }
        }
        
        if (charTyped == '/') {
            val lineStart = text.lastIndexOf('\n', caretOffset - 1) + 1
            val currentLine = if (lineStart < caretOffset) {
                text.substring(lineStart, caretOffset)
            } else {
                ""
            }
            
            if (currentLine.contains("directory:")) {
                AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
                return Result.STOP
            }
        }
        
        if (charTyped == '.') {
            val lineStart = text.lastIndexOf('\n', caretOffset - 1) + 1
            val currentLine = if (lineStart < caretOffset) {
                text.substring(lineStart, caretOffset)
            } else {
                ""
            }
            
            if (currentLine.contains("namespace:") || currentLine.contains("entityType:") || 
                currentLine.trim().startsWith("-") || currentLine.contains(":")) {
                AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
                return Result.STOP
            }
        }
        
        if (charTyped == '<' || (charTyped.isLetter() && isInsideAngleBrackets(editor, caretOffset))) {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
            return Result.STOP
        }
        
        return Result.CONTINUE
    }
    
    private fun isInsideAngleBrackets(editor: Editor, offset: Int): Boolean {
        val text = editor.document.text
        var angleDepth = 0
        
        
        for (i in (offset - 1) downTo 0) {
            when (text[i]) {
                '>' -> angleDepth++
                '<' -> {
                    angleDepth--
                    if (angleDepth < 0) {
                        return true
                    }
                }
                '\n', '\r' -> break 
            }
        }
        
        return false
    }
}