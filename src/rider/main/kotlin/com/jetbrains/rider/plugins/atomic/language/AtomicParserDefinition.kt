package com.jetbrains.rider.plugins.atomic.language

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.jetbrains.rider.plugins.atomic.parser.AtomicParser
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile
import com.jetbrains.rider.plugins.atomic.psi.AtomicTypes

class AtomicParserDefinition : ParserDefinition {
    override fun createLexer(project: Project): Lexer = AtomicLexerAdapter()

    override fun createParser(project: Project): PsiParser = AtomicParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = COMMENTS

    override fun getStringLiteralElements(): TokenSet = STRINGS

    override fun createElement(node: ASTNode): PsiElement = AtomicTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = AtomicFile(viewProvider)

    companion object {
        val FILE = IFileElementType(AtomicLanguage)
        val COMMENTS = TokenSet.create(AtomicTypes.COMMENT)
        val STRINGS = TokenSet.create(AtomicTypes.STRING)
    }
}