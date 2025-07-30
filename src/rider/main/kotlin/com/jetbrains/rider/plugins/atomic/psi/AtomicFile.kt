package com.jetbrains.rider.plugins.atomic.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.jetbrains.rider.plugins.atomic.language.AtomicFileType
import com.jetbrains.rider.plugins.atomic.language.AtomicLanguage

class AtomicFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, AtomicLanguage) {
    override fun getFileType(): FileType = AtomicFileType

    override fun toString(): String = "Atomic File"
}