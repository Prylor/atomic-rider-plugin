package com.jetbrains.rider.plugins.atomic.navigation

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandlerBase
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.jetbrains.rider.plugins.atomic.psi.AtomicValueItem
import com.jetbrains.rider.plugins.atomic.psi.AtomicTypes

class AtomicGotoDeclarationHandler : GotoDeclarationHandlerBase() {
    
    override fun getGotoDeclarationTarget(element: PsiElement?, editor: Editor?): PsiElement? {
        if (element == null) return null
        
        if (element.node?.elementType == AtomicTypes.VALUE_NAME && element.parent is AtomicValueItem) {
            return null
        }
        
        return null
    }
}