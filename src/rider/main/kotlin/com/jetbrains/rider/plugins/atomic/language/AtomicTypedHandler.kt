package com.jetbrains.rider.plugins.atomic.language

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile

class AtomicTypedHandler : TypedHandlerDelegate() {
    
    override fun checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (file !is AtomicFile) {
            return Result.CONTINUE
        }
        
        
        if (charTyped == ':' || charTyped == ' ') {
            val caretOffset = editor.caretModel.offset
            val text = editor.document.text
            
            
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
            val caretOffset = editor.caretModel.offset
            val lineStart = editor.document.text.lastIndexOf('\n', caretOffset - 1) + 1
            val currentLine = editor.document.text.substring(lineStart, caretOffset)
            
            if (currentLine.matches(Regex("^\\s*-\\s*\\w+:\\s+\\w*$"))) {
                AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
                return Result.STOP
            }
        }
        
        
        if (charTyped == '<' || (charTyped.isLetter() && isInsideAngleBrackets(editor, editor.caretModel.offset))) {
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