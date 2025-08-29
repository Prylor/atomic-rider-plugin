package com.jetbrains.rider.plugins.atomic.language

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile
import com.jetbrains.rider.plugins.atomic.psi.*
import com.intellij.codeInsight.AutoPopupController
import com.intellij.openapi.application.ApplicationManager

/**
 * Quick fix to insert all missing required fields at once
 */
class InsertAllMissingFieldsQuickFix(
    private val missingFields: List<RequiredField>
) : IntentionAction {
    
    override fun getText(): String = "Insert all missing required fields"
    
    override fun getFamilyName(): String = "Insert missing fields"
    
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file is AtomicFile && missingFields.isNotEmpty()
    }
    
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file !is AtomicFile) return
        
        val document = editor.document
        val text = document.text
        
        
        val headerFields = missingFields.filter { it.section == FieldSection.HEADER }
        val propertyFields = missingFields.filter { it.section == FieldSection.PROPERTY }
        
        
        val insertions = mutableListOf<FieldInsertion>()
        
        
        if (headerFields.isNotEmpty()) {
            val headerInsertPoint = findHeaderInsertionPoint(file, text)
            val headerText = headerFields.joinToString("\n") { field ->
                "${field.name}: "
            }
            insertions.add(FieldInsertion(headerInsertPoint, headerText))
        }
        
        
        if (propertyFields.isNotEmpty()) {
            val propertyInsertPoint = findPropertyInsertionPoint(file, text)
            val propertyText = propertyFields.joinToString("\n") { field ->
                "${field.name}: "
            }
            
            
            val prefix = if (propertyInsertPoint > 0 && text[propertyInsertPoint - 1] != '\n') {
                "\n\n"
            } else if (headerFields.isNotEmpty()) {
                "\n"
            } else {
                ""
            }
            
            insertions.add(FieldInsertion(propertyInsertPoint, prefix + propertyText))
        }
        
        
        insertions.sortedByDescending { it.offset }.forEach { insertion ->
            document.insertString(insertion.offset, insertion.text)
        }
        
        
        if (insertions.isNotEmpty()) {
            val firstInsertion = insertions.minByOrNull { it.offset } ?: return
            val firstFieldLine = firstInsertion.text.lines().firstOrNull() ?: return
            val colonIndex = firstFieldLine.indexOf(':')
            if (colonIndex != -1) {
                val cursorOffset = firstInsertion.offset + colonIndex + 2 
                editor.caretModel.moveToOffset(cursorOffset)
                
                
                val firstField = missingFields.firstOrNull() ?: return
                ApplicationManager.getApplication().invokeLater {
                    when (firstField.name) {
                        "directory", "solution", "aggressiveInlining" -> {
                            
                            AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
                        }
                        else -> {
                            
                        }
                    }
                }
            }
        }
    }
    
    override fun startInWriteAction(): Boolean = true
    
    private fun findHeaderInsertionPoint(file: AtomicFile, text: String): Int {
        
        val existingHeaderFields = file.children.filterIsInstance<AtomicHeaderSection>().firstOrNull()
        
        if (existingHeaderFields != null) {
            
            val lastHeaderProperty = existingHeaderFields.children.lastOrNull()
            if (lastHeaderProperty != null) {
                return lastHeaderProperty.textRange.endOffset + 1
            }
        }
        
        
        return 0
    }
    
    private fun findPropertyInsertionPoint(file: AtomicFile, text: String): Int {
        
        
        
        
        val headerSection = file.children.filterIsInstance<AtomicHeaderSection>().firstOrNull()
        var insertPoint = if (headerSection != null) {
            headerSection.textRange.endOffset
        } else {
            0
        }
        
        
        val propertyKeywords = setOf("namespace", "className", "directory", "solution")
        var lastPropertyOffset = -1
        
        for (child in file.children) {
            val childText = child.text.trim()
            if (propertyKeywords.any { childText.startsWith("$it:") }) {
                lastPropertyOffset = child.textRange.endOffset
            }
        }
        
        if (lastPropertyOffset > insertPoint) {
            insertPoint = lastPropertyOffset
        }
        
        
        val sections = listOf(
            file.children.filterIsInstance<AtomicImportsSection>().firstOrNull(),
            file.children.filterIsInstance<AtomicTagsSection>().firstOrNull(),
            file.children.filterIsInstance<AtomicValuesSection>().firstOrNull()
        ).filterNotNull()
        
        if (sections.isNotEmpty()) {
            val firstSectionOffset = sections.minOf { it.textRange.startOffset }
            if (insertPoint >= firstSectionOffset) {
                
                insertPoint = firstSectionOffset
                
                
                while (insertPoint > 0 && text[insertPoint - 1].isWhitespace()) {
                    insertPoint--
                }
            }
        }
        
        
        if (insertPoint > 0 && insertPoint < text.length && text[insertPoint - 1] != '\n') {
            return insertPoint
        }
        
        return insertPoint
    }
    
    private data class FieldInsertion(val offset: Int, val text: String)
}

/**
 * Quick fix to insert a single missing required field
 */
class InsertMissingFieldQuickFix(
    private val field: RequiredField
) : IntentionAction {
    
    override fun getText(): String = "Insert '${field.name}' field"
    
    override fun getFamilyName(): String = "Insert missing field"
    
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file is AtomicFile
    }
    
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file !is AtomicFile) return
        
        val document = editor.document
        val text = document.text
        
        val insertPoint = when (field.section) {
            FieldSection.HEADER -> findHeaderInsertionPoint(file, text)
            FieldSection.PROPERTY -> findPropertyInsertionPoint(file, text)
        }
        
        val fieldText = "${field.name}: "
        
        
        val prefix = if (insertPoint > 0 && text[insertPoint - 1] != '\n') {
            "\n"
        } else {
            ""
        }
        
        val suffix = if (insertPoint < text.length && text[insertPoint] != '\n') {
            "\n"
        } else {
            ""
        }
        
        document.insertString(insertPoint, prefix + fieldText + suffix)
        
        
        val colonOffset = insertPoint + prefix.length + field.name.length + 1
        editor.caretModel.moveToOffset(colonOffset + 1) 
        
        
        ApplicationManager.getApplication().invokeLater {
            when (field.name) {
                "directory", "solution" -> {
                    
                    AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
                }
                "aggressiveInlining" -> {
                    
                    AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
                }
                else -> {
                    
                }
            }
        }
    }
    
    override fun startInWriteAction(): Boolean = true
    
    private fun findHeaderInsertionPoint(file: AtomicFile, text: String): Int {
        
        val existingHeaderFields = file.children.filterIsInstance<AtomicHeaderSection>().firstOrNull()
        
        if (existingHeaderFields != null) {
            val lastHeaderProperty = existingHeaderFields.children.lastOrNull()
            if (lastHeaderProperty != null) {
                return lastHeaderProperty.textRange.endOffset
            }
        }
        
        return 0
    }
    
    private fun findPropertyInsertionPoint(file: AtomicFile, text: String): Int {
        
        val headerSection = file.children.filterIsInstance<AtomicHeaderSection>().firstOrNull()
        var insertPoint = if (headerSection != null) {
            headerSection.textRange.endOffset
        } else {
            0
        }
        
        val propertyKeywords = setOf("namespace", "className", "directory", "solution")
        var lastPropertyOffset = -1
        
        for (child in file.children) {
            val childText = child.text.trim()
            if (propertyKeywords.any { childText.startsWith("$it:") }) {
                lastPropertyOffset = child.textRange.endOffset
            }
        }
        
        if (lastPropertyOffset > insertPoint) {
            insertPoint = lastPropertyOffset
        }
        
        val sections = listOf(
            file.children.filterIsInstance<AtomicImportsSection>().firstOrNull(),
            file.children.filterIsInstance<AtomicTagsSection>().firstOrNull(),
            file.children.filterIsInstance<AtomicValuesSection>().firstOrNull()
        ).filterNotNull()
        
        if (sections.isNotEmpty()) {
            val firstSectionOffset = sections.minOf { it.textRange.startOffset }
            if (insertPoint >= firstSectionOffset) {
                insertPoint = firstSectionOffset
                
                while (insertPoint > 0 && text[insertPoint - 1].isWhitespace()) {
                    insertPoint--
                }
            }
        }
        
        return insertPoint
    }
}

/**
 * Represents a required field in an .atomic file
 */
data class RequiredField(
    val name: String,
    val section: FieldSection,
    val description: String
) {
    companion object {
        val ENTITY_TYPE = RequiredField("entityType", FieldSection.HEADER, "The entity interface type to extend")
        val NAMESPACE = RequiredField("namespace", FieldSection.PROPERTY, "Target namespace for generated code")
        val CLASS_NAME = RequiredField("className", FieldSection.PROPERTY, "Name of the generated class")
        val DIRECTORY = RequiredField("directory", FieldSection.PROPERTY, "Output directory path")
        val SOLUTION = RequiredField("solution", FieldSection.PROPERTY, "Target C# project/solution")
        
        val ALL_REQUIRED = listOf(ENTITY_TYPE, NAMESPACE, CLASS_NAME, DIRECTORY)
    }
}

/**
 * Section where a field belongs
 */
enum class FieldSection {
    HEADER,    
    PROPERTY   
}