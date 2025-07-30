package com.jetbrains.rider.plugins.atomic.references

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.rider.plugins.atomic.language.AtomicLanguage
import com.jetbrains.rider.plugins.atomic.psi.AtomicValueItem
import com.jetbrains.rider.plugins.atomic.psi.AtomicTypes

class AtomicValueReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    }
}