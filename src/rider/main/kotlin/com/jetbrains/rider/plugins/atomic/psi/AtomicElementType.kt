package com.jetbrains.rider.plugins.atomic.psi

import com.intellij.psi.tree.IElementType
import com.jetbrains.rider.plugins.atomic.language.AtomicLanguage

class AtomicElementType(debugName: String) : IElementType(debugName, AtomicLanguage)