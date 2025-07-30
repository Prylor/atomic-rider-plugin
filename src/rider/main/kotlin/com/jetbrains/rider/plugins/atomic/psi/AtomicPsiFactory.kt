package com.jetbrains.rider.plugins.atomic.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.jetbrains.rider.plugins.atomic.language.AtomicFileType

class AtomicPsiFactory(private val project: Project) {
    
    fun createFile(text: String): AtomicFile {
        val name = "dummy.atomic"
        return PsiFileFactory.getInstance(project)
            .createFileFromText(name, AtomicFileType, text) as AtomicFile
    }
    
    fun createValueName(name: String): PsiElement {
        val file = createFile("values:\n- $name: DummyType")
        val valuesSection = file.firstChild
        var currentChild = valuesSection.firstChild
        while (currentChild != null) {
            if (currentChild is AtomicValuesSection) {
                val valueItem = currentChild.valueItemList.firstOrNull()
                if (valueItem != null) {
                    return valueItem.node.findChildByType(AtomicTypes.VALUE_NAME)?.psi
                        ?: throw IllegalStateException("VALUE_NAME not found")
                }
            }
            currentChild = currentChild.nextSibling
        }
        throw IllegalStateException("Could not create VALUE_NAME element")
    }
    
    fun createTagName(name: String): PsiElement {
        val file = createFile("tags:\n- $name")
        val tagsSection = file.firstChild
        var currentChild = tagsSection.firstChild
        while (currentChild != null) {
            if (currentChild is AtomicTagsSection) {
                val tagItem = currentChild.tagItemList.firstOrNull()
                if (tagItem != null) {
                    return tagItem.node.findChildByType(AtomicTypes.TAG_NAME)?.psi
                        ?: throw IllegalStateException("TAG_NAME not found")
                }
            }
            currentChild = currentChild.nextSibling
        }
        throw IllegalStateException("Could not create TAG_NAME element")
    }
}