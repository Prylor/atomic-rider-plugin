package com.jetbrains.rider.plugins.atomic.psi

import com.intellij.psi.tree.IElementType
import com.jetbrains.rider.plugins.atomic.language.AtomicLanguage

object AtomicElementTypes {
    @JvmField
    val VALUE_NAME_IDENTIFIER = IElementType("VALUE_NAME_IDENTIFIER", AtomicLanguage)
}