package com.jetbrains.rider.plugins.atomic.language

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.jetbrains.rider.plugins.atomic.psi.AtomicTypes

class AtomicSyntaxHighlighter : SyntaxHighlighterBase() {
    
    companion object {
        // Keywords
        val KEYWORD: TextAttributesKey = createTextAttributesKey("ATOMIC_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        
        // Sections
        val SECTION: TextAttributesKey = createTextAttributesKey("ATOMIC_SECTION", DefaultLanguageHighlighterColors.KEYWORD)
        
        // Properties
        val PROPERTY_KEY: TextAttributesKey = createTextAttributesKey("ATOMIC_PROPERTY_KEY", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
        
        // Values
        val STRING: TextAttributesKey = createTextAttributesKey("ATOMIC_STRING", DefaultLanguageHighlighterColors.STRING)
        val BOOLEAN: TextAttributesKey = createTextAttributesKey("ATOMIC_BOOLEAN", DefaultLanguageHighlighterColors.KEYWORD)
        val IDENTIFIER: TextAttributesKey = createTextAttributesKey("ATOMIC_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
        
        // Types
        val TYPE: TextAttributesKey = createTextAttributesKey("ATOMIC_TYPE", DefaultLanguageHighlighterColors.CLASS_REFERENCE)
        val GENERIC_TYPE: TextAttributesKey = createTextAttributesKey("ATOMIC_GENERIC_TYPE", DefaultLanguageHighlighterColors.CLASS_REFERENCE)
        
        // Operators
        val OPERATOR: TextAttributesKey = createTextAttributesKey("ATOMIC_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        
        // Comments
        val COMMENT: TextAttributesKey = createTextAttributesKey("ATOMIC_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        
        // Bad character
        val BAD_CHARACTER: TextAttributesKey = createTextAttributesKey("ATOMIC_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)
        
        private val KEYWORD_KEYS = arrayOf(KEYWORD)
        private val SECTION_KEYS = arrayOf(SECTION)
        private val PROPERTY_KEY_KEYS = arrayOf(PROPERTY_KEY)
        private val STRING_KEYS = arrayOf(STRING)
        private val BOOLEAN_KEYS = arrayOf(BOOLEAN)
        private val IDENTIFIER_KEYS = arrayOf(IDENTIFIER)
        private val TYPE_KEYS = arrayOf(TYPE)
        private val GENERIC_TYPE_KEYS = arrayOf(GENERIC_TYPE)
        private val OPERATOR_KEYS = arrayOf(OPERATOR)
        private val COMMENT_KEYS = arrayOf(COMMENT)
        private val BAD_CHAR_KEYS = arrayOf(BAD_CHARACTER)
        private val EMPTY_KEYS = arrayOf<TextAttributesKey>()
    }
    
    override fun getHighlightingLexer(): Lexer = AtomicLexerAdapter()
    
    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            // Keywords
            AtomicTypes.HEADER_KEYWORD,
            AtomicTypes.ENTITY_TYPE_KEYWORD,
            AtomicTypes.AGGRESSIVE_INLINING_KEYWORD,
            AtomicTypes.UNSAFE_KEYWORD,
            AtomicTypes.NAMESPACE_KEYWORD,
            AtomicTypes.CLASS_NAME_KEYWORD,
            AtomicTypes.DIRECTORY_KEYWORD,
            AtomicTypes.SOLUTION_KEYWORD -> KEYWORD_KEYS
            
            // Section keywords
            AtomicTypes.IMPORTS_KEYWORD,
            AtomicTypes.TAGS_KEYWORD,
            AtomicTypes.VALUES_KEYWORD -> SECTION_KEYS
            
            // Values
            AtomicTypes.STRING -> STRING_KEYS
            AtomicTypes.TRUE,
            AtomicTypes.FALSE -> BOOLEAN_KEYS
            AtomicTypes.IDENTIFIER -> IDENTIFIER_KEYS
            
            // Types
            AtomicTypes.TYPE_REFERENCE -> TYPE_KEYS
            
            // Names
            AtomicTypes.VALUE_NAME -> PROPERTY_KEY_KEYS
            AtomicTypes.TAG_NAME -> IDENTIFIER_KEYS
            AtomicTypes.IMPORT_PATH -> STRING_KEYS
            
            // Operators
            AtomicTypes.COLON,
            AtomicTypes.HYPHEN -> OPERATOR_KEYS
            
            // Comments
            AtomicTypes.COMMENT -> COMMENT_KEYS
            
            // Bad character
            TokenType.BAD_CHARACTER -> BAD_CHAR_KEYS
            
            else -> EMPTY_KEYS
        }
    }
}