package com.jetbrains.rider.plugins.atomic.language

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.jetbrains.rider.plugins.atomic.psi.*
import com.jetbrains.rider.plugins.atomic.psi.impl.AtomicPsiImplUtil

class AtomicTypeHighlightingAnnotator : Annotator {
    
    companion object {
        
        private val PRIMITIVE_TYPE = TextAttributesKey.createTextAttributesKey(
            "ATOMIC_PRIMITIVE_TYPE",
            DefaultLanguageHighlighterColors.KEYWORD 
        )
        
        private val CLASS_TYPE = TextAttributesKey.createTextAttributesKey(
            "ATOMIC_CLASS_TYPE", 
            DefaultLanguageHighlighterColors.CLASS_REFERENCE 
        )
        
        private val GENERIC_BRACKETS = TextAttributesKey.createTextAttributesKey(
            "ATOMIC_GENERIC_BRACKETS",
            DefaultLanguageHighlighterColors.BRACES 
        )
        
        private val COMMA = TextAttributesKey.createTextAttributesKey(
            "ATOMIC_COMMA",
            DefaultLanguageHighlighterColors.COMMA 
        )
        
        
        private val PRIMITIVE_TYPES = setOf(
            "bool", "byte", "sbyte", "char", "decimal", "double", "float",
            "int", "uint", "long", "ulong", "short", "ushort", "string",
            "object", "void", "dynamic"
        )
    }
    
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is AtomicValueItem -> highlightTypeReference(element, holder)
        }
    }
    
    private fun highlightTypeReference(valueItem: AtomicValueItem, holder: AnnotationHolder) {
        val typeReference = AtomicPsiImplUtil.getTypeReference(valueItem) ?: return
        val colonNode = valueItem.node.findChildByType(AtomicTypes.COLON) ?: return
        
        
        val startOffset = colonNode.textRange.endOffset
        val text = valueItem.text
        val colonIndex = text.indexOf(':')
        if (colonIndex == -1) return
        
        
        val typeText = text.substring(colonIndex + 1).trim()
        val baseOffset = valueItem.textRange.startOffset + colonIndex + 1 + (text.length - colonIndex - 1 - typeText.length)
        
        highlightType(typeText, baseOffset, holder)
    }
    
    private fun highlightType(typeText: String, baseOffset: Int, holder: AnnotationHolder) {
        var i = 0
        val length = typeText.length
        
        while (i < length) {
            when {
                
                typeText[i].isWhitespace() -> {
                    i++
                }
                
                
                typeText[i] == '<' || typeText[i] == '>' -> {
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(TextRange(baseOffset + i, baseOffset + i + 1))
                        .textAttributes(GENERIC_BRACKETS)
                        .create()
                    i++
                }
                
                
                typeText[i] == ',' -> {
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(TextRange(baseOffset + i, baseOffset + i + 1))
                        .textAttributes(COMMA)
                        .create()
                    i++
                }
                
                
                typeText[i].isLetter() || typeText[i] == '_' -> {
                    val start = i
                    while (i < length && (typeText[i].isLetterOrDigit() || typeText[i] == '_' || typeText[i] == '.')) {
                        i++
                    }
                    
                    val typeName = typeText.substring(start, i)
                    val lastDot = typeName.lastIndexOf('.')
                    val shortName = if (lastDot >= 0) typeName.substring(lastDot + 1) else typeName
                    
                    
                    val isPrimitive = PRIMITIVE_TYPES.contains(shortName)
                    val textAttributes = if (isPrimitive) PRIMITIVE_TYPE else CLASS_TYPE
                    
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(TextRange(baseOffset + start, baseOffset + i))
                        .textAttributes(textAttributes)
                        .create()
                }
                
                
                typeText[i] == '[' -> {
                    val start = i
                    while (i < length && typeText[i] != ']') {
                        i++
                    }
                    if (i < length && typeText[i] == ']') {
                        i++
                        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                            .range(TextRange(baseOffset + start, baseOffset + i))
                            .textAttributes(GENERIC_BRACKETS)
                            .create()
                    }
                }
                
                else -> {
                    i++
                }
            }
        }
    }
}