package com.jetbrains.rider.plugins.atomic.listeners

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.TextRange
import com.jetbrains.rider.plugins.atomic.language.AtomicFileType
import com.jetbrains.rider.plugins.atomic.language.AtomicHighlighterFilter

class AtomicDocumentListener : DocumentListener {
    override fun beforeDocumentChange(event: DocumentEvent) {
        val document = event.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        
        if (file.extension != AtomicFileType.defaultExtension) return
        
        val range = TextRange(event.offset, event.offset + event.oldLength)
        AtomicHighlighterFilter.clearOverlappingHighlighters(document, range)
    }
    
    override fun documentChanged(event: DocumentEvent) {
    }
}

class AtomicDocumentListenerStartup : StartupActivity {
    override fun runActivity(project: Project) {
    }
}