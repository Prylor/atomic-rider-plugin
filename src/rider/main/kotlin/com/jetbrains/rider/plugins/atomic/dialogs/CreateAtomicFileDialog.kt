package com.jetbrains.rider.plugins.atomic.dialogs

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import com.jetbrains.rider.plugins.atomic.services.AtomicGenerationService
import com.jetbrains.rider.plugins.atomic.model.TypeKind
import kotlinx.coroutines.runBlocking
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class CreateAtomicFileDialog(
    private val project: Project,
    private val defaultDirectory: String
) : DialogWrapper(project) {
    
    private val fileNameField = JBTextField()
    private val classNameField = JBTextField("AtomicAPIExtensions")
    private val namespaceField = createNamespaceField()
    private val entityTypeField = createEntityTypeField()
    private val directoryField = TextFieldWithBrowseButton()
    private val headerField = JBTextField()
    private val aggressiveInliningCheckBox = JBCheckBox("Aggressive Inlining", true)
    private val unsafeCheckBox = JBCheckBox("Unsafe Access", false)
    
    data class Result(
        val fileName: String,
        val className: String,
        val namespace: String?,
        val entityType: String?,
        val directory: String?,
        val header: String?,
        val aggressiveInlining: Boolean,
        val unsafe: Boolean
    )
    
    init {
        title = "Create Atomic File"
        setOKButtonText("Create")
        
        
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withTitle("Select Output Directory")
            .withDescription("Choose directory for generated C# file (relative to project root)")
        
        
        val projectBasePath = project.basePath
        
        directoryField.addBrowseFolderListener(project, descriptor)
        
        
        directoryField.textField.addActionListener {
            val text = directoryField.text
            if (projectBasePath != null && text.isNotBlank()) {
                try {
                    val projectRoot = java.nio.file.Paths.get(projectBasePath)
                    val selectedPath = java.nio.file.Paths.get(text)
                    
                    if (selectedPath.isAbsolute && selectedPath.startsWith(projectRoot)) {
                        
                        val relativePath = projectRoot.relativize(selectedPath).toString().replace('\\', '/')
                        directoryField.text = if (relativePath.isEmpty()) "." else relativePath
                    }
                } catch (e: Exception) {
                    
                }
            }
        }
        
        
        if (projectBasePath != null) {
            val projectRoot = java.nio.file.Paths.get(projectBasePath)
            val currentDir = java.nio.file.Paths.get(defaultDirectory)
            
            try {
                if (currentDir.startsWith(projectRoot)) {
                    
                    val relativePath = projectRoot.relativize(currentDir).toString().replace('\\', '/')
                    directoryField.text = if (relativePath.isEmpty()) "." else relativePath
                } else {
                    
                    directoryField.text = "."
                }
            } catch (e: Exception) {
                directoryField.text = "."
            }
        } else {
            directoryField.text = "."
        }
        
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        return panel {
            row("File name:") {
                cell(fileNameField)
                    .columns(COLUMNS_LARGE)
                    .focused()
                    .validationOnApply { 
                        when {
                            it.text.isBlank() -> ValidationInfo("File name cannot be empty")
                            !it.text.endsWith(".atomic") -> ValidationInfo("File name must end with .atomic")
                            else -> null
                        }
                    }
                    .comment("Name of the .atomic file (e.g., MyEntity.atomic)")
            }
            
            separator()
            
            group("Header Properties") {
                row("Header:") {
                    cell(headerField)
                        .columns(COLUMNS_LARGE)
                        .comment("Custom header text for generated file (optional)")
                }

                row("Class name:") {
                    cell(classNameField)
                        .columns(COLUMNS_LARGE)
                        .validationOnApply {
                            if (it.text.isBlank()) ValidationInfo("Class name cannot be empty")
                            else null
                        }
                        .comment("Name of the generated C# class")
                }
                
                row("Namespace:") {
                    cell(namespaceField)
                        .comment("C# namespace for the generated code (optional) - supports autocomplete")
                }
                
                row("Entity type:") {
                    cell(entityTypeField)
                        .comment("Type of the entity (e.g., Entity, GameObject) - supports autocomplete")
                }
                
                row("Output directory:") {
                    cell(directoryField)
                        .columns(COLUMNS_LARGE)
                        .comment("Directory for generated C# file (relative to project root)")
                        .validationOnInput { field ->
                            val inputPath = field.text
                            if (inputPath.isBlank()) {
                                return@validationOnInput null
                            }
                            
                            val projectBasePath = project.basePath ?: return@validationOnInput null
                            
                            try {
                                val projectRoot = java.nio.file.Paths.get(projectBasePath)
                                val normalizedInput = inputPath.replace('\\', '/')
                                
                                
                                val inputPathObj = java.nio.file.Paths.get(normalizedInput)
                                
                                if (inputPathObj.isAbsolute && !inputPathObj.startsWith(projectRoot)) {
                                    
                                    ValidationInfo("Directory should be inside the project. It will be processed to extract a relative path.")
                                        .asWarning()
                                        .withOKEnabled()
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }
                }
                
                row {
                    cell(aggressiveInliningCheckBox)
                        .comment("Enable aggressive inlining for generated methods")
                }
                
                row {
                    cell(unsafeCheckBox)
                        .comment("Enable unsafe access methods (ref returns) for generated code")
                }
            }
        }.withBorder(JBUI.Borders.empty(10))
    }
    
    override fun doValidate(): ValidationInfo? {
        if (fileNameField.text.isBlank()) {
            return ValidationInfo("Please enter a file name", fileNameField)
        }
        
        if (!fileNameField.text.endsWith(".atomic")) {
            fileNameField.text = fileNameField.text + ".atomic"
        }
        
        if (classNameField.text.isBlank()) {
            return ValidationInfo("Please enter a class name", classNameField)
        }
        
        return super.doValidate()
    }
    
    fun getResult(): Result {
        return Result(
            header = headerField.text.takeIf { it.isNotBlank() },
            fileName = fileNameField.text,
            className = classNameField.text,
            namespace = namespaceField.text.takeIf { it.isNotBlank() },
            entityType = entityTypeField.text.takeIf { it.isNotBlank() },
            directory = directoryField.text.takeIf { it.isNotBlank() },
            aggressiveInlining = aggressiveInliningCheckBox.isSelected,
            unsafe = unsafeCheckBox.isSelected
        )
    }
    
    private fun createEntityTypeField(): EditorTextField {
        val document = EditorFactory.getInstance().createDocument("")
        val field = object : EditorTextField(document, project, PlainTextFileType.INSTANCE, false, true) {
            override fun createEditor(): EditorEx {
                val editor = super.createEditor() as EditorEx
                
                
                editor.setOneLineMode(true)
                
                
                editor.contentComponent.addKeyListener(object : java.awt.event.KeyAdapter() {
                    override fun keyReleased(e: java.awt.event.KeyEvent) {
                        if (e.keyCode == java.awt.event.KeyEvent.VK_SPACE && e.isControlDown) {
                            
                            showTypeCompletion(editor)
                        }
                    }
                    
                    override fun keyTyped(e: java.awt.event.KeyEvent) {
                        if (e.keyChar.isLetter() || e.keyChar == '.') {
                            ApplicationManager.getApplication().invokeLater {
                                showTypeCompletion(editor)
                            }
                        }
                    }
                })
                
                return editor
            }
        }
        
        
        field.setOneLineMode(true)
        field.preferredSize = java.awt.Dimension(400, JBTextField().preferredSize.height)
        
        return field
    }
    
    private fun showTypeCompletion(editor: EditorEx) {
        val service = AtomicGenerationService.getInstance(project)
        val currentText = editor.document.text
        val caretOffset = editor.caretModel.offset
        
        
        var wordStart = caretOffset
        while (wordStart > 0 && (currentText[wordStart - 1].isLetterOrDigit() || currentText[wordStart - 1] == '_')) {
            wordStart--
        }
        
        
        val prefix = currentText.substring(wordStart, caretOffset)
        if (prefix.isEmpty() && !currentText.endsWith('.')) return
        
        
        val beforeWord = if (wordStart > 0) currentText.substring(0, wordStart) else ""
        val namespaceFilter = if (beforeWord.endsWith('.')) {
            
            var nsStart = beforeWord.length - 1
            while (nsStart > 0 && beforeWord[nsStart - 1] != ' ' && beforeWord[nsStart - 1] != '\t') {
                nsStart--
            }
            beforeWord.substring(nsStart, beforeWord.length - 1).trim()
        } else null
        
        try {
            val types = runBlocking {
                service.getTypeCompletions(prefix, emptyList(), project.basePath ?: "", namespaceFilter)
            }
            
            if (types.isEmpty()) return
            
            val lookupElements = types.map { typeItem ->
                val isNamespace = typeItem.namespace.isEmpty() && typeItem.typeName == typeItem.fullTypeName
                
                val icon = if (isNamespace) {
                    AllIcons.Nodes.Package
                } else {
                    when (typeItem.typeKind) {
                        TypeKind.Class -> AllIcons.Nodes.Class
                        TypeKind.Interface -> AllIcons.Nodes.Interface
                        TypeKind.Struct -> AllIcons.Nodes.Static
                        TypeKind.Enum -> AllIcons.Nodes.Enum
                        TypeKind.Delegate -> AllIcons.Nodes.Lambda
                    }
                }
                
                LookupElementBuilder.create(typeItem.typeName)
                    .withIcon(icon)
                    .withTypeText(if (isNamespace) "namespace" else typeItem.namespace)
                    .withTailText(if (!isNamespace && typeItem.isGeneric) "<>" else "", true)
                    .withInsertHandler { context, item ->
                        if (isNamespace) {
                            
                            context.document.insertString(context.tailOffset, ".")
                            context.editor.caretModel.moveToOffset(context.tailOffset + 1)
                            
                            ApplicationManager.getApplication().invokeLater {
                                showTypeCompletion(editor)
                            }
                        } else if (typeItem.isGeneric) {
                            
                            context.document.insertString(context.tailOffset, "<>")
                            context.editor.caretModel.moveToOffset(context.tailOffset + 1)
                        }
                    }
            }.toTypedArray()
            
            val lookupManager = LookupManager.getInstance(project)
            if (lookupElements.isNotEmpty()) {
                lookupManager.showLookup(editor, lookupElements, prefix)
            }
            
        } catch (e: Exception) {
            
        }
    }
    
    private fun createNamespaceField(): EditorTextField {
        val document = EditorFactory.getInstance().createDocument("")
        val field = object : EditorTextField(document, project, PlainTextFileType.INSTANCE, false, true) {
            override fun createEditor(): EditorEx {
                val editor = super.createEditor() as EditorEx
                
                
                editor.setOneLineMode(true)
                
                
                editor.contentComponent.addKeyListener(object : java.awt.event.KeyAdapter() {
                    override fun keyReleased(e: java.awt.event.KeyEvent) {
                        if (e.keyCode == java.awt.event.KeyEvent.VK_SPACE && e.isControlDown) {
                            
                            showNamespaceCompletion(editor)
                        }
                    }
                    
                    override fun keyTyped(e: java.awt.event.KeyEvent) {
                        if (e.keyChar.isLetter() || e.keyChar == '.') {
                            ApplicationManager.getApplication().invokeLater {
                                showNamespaceCompletion(editor)
                            }
                        }
                    }
                })
                
                return editor
            }
        }
        
        
        field.setOneLineMode(true)
        field.preferredSize = java.awt.Dimension(400, JBTextField().preferredSize.height)
        
        return field
    }
    
    private fun showNamespaceCompletion(editor: EditorEx) {
        val service = AtomicGenerationService.getInstance(project)
        val currentText = editor.document.text
        val caretOffset = editor.caretModel.offset
        
        
        val prefix = currentText.substring(0, caretOffset)
        if (prefix.isEmpty()) return
        
        try {
            val namespaces = runBlocking {
                service.getNamespaceCompletions(prefix, project.basePath ?: "")
            }
            
            if (namespaces.isEmpty()) return
            
            val lookupElements = namespaces.map { namespace ->
                LookupElementBuilder.create(namespace)
                    .withIcon(AllIcons.Nodes.Package)
                    .withTypeText("namespace")
                    .withBoldness(namespace.startsWith("System"))
            }.toTypedArray()
            
            val lookupManager = LookupManager.getInstance(project)
            if (lookupElements.isNotEmpty()) {
                lookupManager.showLookup(editor, lookupElements, prefix)
            }
            
        } catch (e: Exception) {
            
        }
    }
}