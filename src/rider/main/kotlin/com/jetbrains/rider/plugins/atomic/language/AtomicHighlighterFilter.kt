package com.jetbrains.rider.plugins.atomic.language

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.TextRange
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

object AtomicHighlighterFilter {
    private val activeHighlighters = ConcurrentHashMap<Document, ConcurrentSkipListSet<HighlighterKey>>()
    
    data class HighlighterKey(
        val startOffset: Int,
        val endOffset: Int,
        val attributeId: String
    ) : Comparable<HighlighterKey> {
        override fun compareTo(other: HighlighterKey): Int {
            val startCompare = startOffset.compareTo(other.startOffset)
            if (startCompare != 0) return startCompare
            
            val endCompare = endOffset.compareTo(other.endOffset)
            if (endCompare != 0) return endCompare
            
            return attributeId.compareTo(other.attributeId)
        }
    }
    
    fun canRegisterHighlighter(
        document: Document,
        range: TextRange,
        attributeId: String
    ): Boolean {
        val key = HighlighterKey(range.startOffset, range.endOffset, attributeId)
        val documentHighlighters = activeHighlighters.computeIfAbsent(document) { 
            ConcurrentSkipListSet()
        }
        
        return documentHighlighters.add(key)
    }
    
    fun removeHighlighter(
        document: Document,
        range: TextRange,
        attributeId: String
    ) {
        val key = HighlighterKey(range.startOffset, range.endOffset, attributeId)
        activeHighlighters[document]?.remove(key)
    }
    
    fun clearDocumentHighlighters(document: Document) {
        activeHighlighters.remove(document)
    }
    
    fun clearOverlappingHighlighters(
        document: Document,
        range: TextRange
    ) {
        val documentHighlighters = activeHighlighters[document] ?: return
        
        val toRemove = documentHighlighters.filter { key ->
                key.startOffset < range.endOffset && key.endOffset > range.startOffset
        }
        
        documentHighlighters.removeAll(toRemove.toSet())
    }
}