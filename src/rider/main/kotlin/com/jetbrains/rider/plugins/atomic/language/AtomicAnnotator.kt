package com.jetbrains.rider.plugins.atomic.language

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.jetbrains.rider.plugins.atomic.psi.*
import com.jetbrains.rider.plugins.atomic.psi.impl.AtomicPsiImplUtil
import com.jetbrains.rider.plugins.atomic.services.AtomicGenerationService
import kotlinx.coroutines.runBlocking
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.intellij.openapi.application.ReadAction
import java.util.concurrent.ConcurrentHashMap
import com.jetbrains.rider.plugins.atomic.model.TypeValidationResponse
import com.jetbrains.rider.plugins.atomic.model.NamespaceValidationResponse

class AtomicAnnotator : Annotator {
    
    companion object {
        // Track pending validations to avoid duplicate requests
        private val pendingValidations = ConcurrentHashMap<String, Boolean>()
        private const val VALIDATION_TIMEOUT_MS = 100L // Quick timeout for annotations
    }
    
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        
        if (element is AtomicFile) {
            checkRequiredFields(element, holder)
            return
        }
        
        when (element) {
            is AtomicEntityTypeProp -> annotateEntityTypeProp(element, holder)
            is AtomicAggressiveInliningProp -> annotateAggressiveInliningProp(element, holder)
            is AtomicNamespaceProp -> annotateNamespaceProp(element, holder)
            is AtomicClassNameProp -> annotateClassNameProp(element, holder)
            is AtomicDirectoryProp -> annotateDirectoryProp(element, holder)
            is AtomicSolutionProp -> annotateSolutionProp(element, holder)
            is AtomicImportItem -> annotateImport(element, holder)
            is AtomicTagItem -> annotateTag(element, holder)
            is AtomicValueItem -> annotateValue(element, holder)
        }
    }
    
    private fun checkRequiredFields(file: AtomicFile, holder: AnnotationHolder) {
        
        val existingFields = mutableSetOf<String>()
        
        
        file.children.filterIsInstance<AtomicHeaderSection>().forEach { headerSection ->
            headerSection.children.forEach { child ->
                when (child) {
                    is AtomicEntityTypeProp -> existingFields.add("entityType")
                }
            }
        }
        
        
        fun checkElement(element: PsiElement) {
            element.children.forEach { child ->
                when (child) {
                    is AtomicNamespaceProp -> existingFields.add("namespace")
                    is AtomicClassNameProp -> existingFields.add("className")
                    is AtomicDirectoryProp -> existingFields.add("directory")
                    is AtomicSolutionProp -> existingFields.add("solution")
                }
                checkElement(child)
            }
        }
        checkElement(file)
        
        
        val missingFields = mutableListOf<RequiredField>()
        RequiredField.ALL_REQUIRED.forEach { field ->
            if (!existingFields.contains(field.name)) {
                missingFields.add(field)
            }
        }
        
        
        if (missingFields.isNotEmpty()) {
            
            val firstElement = file.firstChild
            val errorRange = if (firstElement != null && firstElement.textRange.length > 0) {
                firstElement.textRange
            } else {
                
                com.intellij.openapi.util.TextRange(0, minOf(1, file.textLength))
            }
            
            val missingFieldNames = missingFields.joinToString(", ") { "'${it.name}'" }
            val annotation = holder.newAnnotation(
                HighlightSeverity.ERROR, 
                "Missing required fields: $missingFieldNames. These fields are required for code generation."
            ).range(errorRange)
            
            
            if (missingFields.size > 1) {
                annotation.withFix(InsertAllMissingFieldsQuickFix(missingFields))
            }
            
            missingFields.forEach { field ->
                annotation.withFix(InsertMissingFieldQuickFix(field))
            }
            
            annotation.create()
        }
        
        
        file.children.filterIsInstance<AtomicTagsSection>().forEach { tagsSection ->
            if (tagsSection.tagItemList.isEmpty()) {
                
                tagsSection.node.findChildByType(AtomicTypes.TAGS_KEYWORD)?.let { tagsKeyword ->
                    holder.newAnnotation(HighlightSeverity.WARNING, "Tags section is empty")
                        .range(tagsKeyword.textRange)
                        .create()
                }
            }
        }
        
        
        file.children.filterIsInstance<AtomicValuesSection>().forEach { valuesSection ->
            if (valuesSection.valueItemList.isEmpty()) {
                
                valuesSection.node.findChildByType(AtomicTypes.VALUES_KEYWORD)?.let { valuesKeyword ->
                    holder.newAnnotation(HighlightSeverity.WARNING, "Values section is empty")
                        .range(valuesKeyword.textRange)
                        .create()
                }
            }
        }
    }
    
    private fun annotateEntityTypeProp(property: AtomicEntityTypeProp, holder: AnnotationHolder) {
        if (!hasValueAfterColon(property)) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Property 'entityType' requires a value")
                .range(property.textRange)
                .create()
        } else {
            
            val typeName = AtomicPsiImplUtil.getValue(property)
            if (typeName != null && typeName.isNotBlank()) {
                validateTypeForEntityType(property, typeName, holder)
            }
        }
    }
    
    private fun annotateAggressiveInliningProp(property: AtomicAggressiveInliningProp, holder: AnnotationHolder) {
        val value = property.node.findChildByType(AtomicTypes.TRUE) ?: 
                   property.node.findChildByType(AtomicTypes.FALSE)
        if (value == null) {
            val identifier = property.node.findChildByType(AtomicTypes.IDENTIFIER)
            if (identifier != null && identifier.text != "true" && identifier.text != "false") {
                holder.newAnnotation(HighlightSeverity.ERROR, "Property 'aggressiveInlining' must be 'true' or 'false'")
                    .range(identifier.textRange)
                    .create()
            }
        }
    }
    
    private fun annotateNamespaceProp(property: AtomicNamespaceProp, holder: AnnotationHolder) {
        if (!hasValueAfterColon(property)) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Property 'namespace' requires a value")
                .range(property.textRange)
                .create()
        }
    }
    
    private fun annotateClassNameProp(property: AtomicClassNameProp, holder: AnnotationHolder) {
        if (!hasValueAfterColon(property)) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Property 'className' requires a value")
                .range(property.textRange)
                .create()
        }
    }
    
    private fun annotateDirectoryProp(property: AtomicDirectoryProp, holder: AnnotationHolder) {
        if (!hasValueAfterColon(property)) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Property 'directory' requires a value")
                .range(property.textRange)
                .create()
        }
    }
    
    private fun annotateSolutionProp(property: AtomicSolutionProp, holder: AnnotationHolder) {
        if (!hasValueAfterColon(property)) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Property 'solution' requires a value")
                .range(property.textRange)
                .create()
        }
    }
    
    private fun hasValueAfterColon(property: PsiElement): Boolean {
        var currentNode = property.node.findChildByType(AtomicTypes.COLON)
        if (currentNode != null) {
            currentNode = currentNode.treeNext
            while (currentNode != null) {
                
                if (currentNode.elementType == AtomicTypes.CRLF) {
                    break
                }
                
                
                val text = currentNode.text.trim()
                if (text.isNotEmpty()) {
                    
                    return true
                }
                
                currentNode = currentNode.treeNext
            }
        }
        return false
    }
    
    private fun annotateImport(import: AtomicImportItem, holder: AnnotationHolder) {
        val importPath = import.node.findChildByType(AtomicTypes.IMPORT_PATH)
        if (importPath == null || importPath.text.isBlank()) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Import statement requires a namespace")
                .range(import.textRange)
                .create()
        } else {
            
            validateNamespaceExists(import, importPath.text, holder)
            
            
            val file = import.containingFile as? AtomicFile ?: return
            val importsSection = file.node.findChildByType(AtomicTypes.IMPORTS_SECTION)?.psi as? AtomicImportsSection
            val allImports = importsSection?.importItemList?.mapNotNull { 
                it.node.findChildByType(AtomicTypes.IMPORT_PATH)?.text 
            } ?: emptyList()
            
            val namespace = importPath.text
            if (allImports.count { it == namespace } > 1) {
                holder.newAnnotation(HighlightSeverity.WARNING, "Duplicate import '$namespace'")
                    .range(importPath.textRange)
                    .create()
            }
        }
    }
    
    private fun annotateTag(tag: AtomicTagItem, holder: AnnotationHolder) {
        val tagName = tag.node.findChildByType(AtomicTypes.TAG_NAME)
        if (tagName == null || tagName.text.isBlank()) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Tag requires a name")
                .range(tag.textRange)
                .create()
        } else {
            
            val file = tag.containingFile as? AtomicFile ?: return
            val tagsSection = file.node.findChildByType(AtomicTypes.TAGS_SECTION)?.psi as? AtomicTagsSection
            val allTags = tagsSection?.tagItemList?.mapNotNull { 
                it.node.findChildByType(AtomicTypes.TAG_NAME)?.text 
            } ?: emptyList()
            
            val name = tagName.text
            if (allTags.count { it == name } > 1) {
                holder.newAnnotation(HighlightSeverity.WARNING, "Duplicate tag '$name'")
                    .range(tagName.textRange)
                    .create()
            }
        }
    }
    
    private fun annotateValue(value: AtomicValueItem, holder: AnnotationHolder) {
        val valueName = value.node.findChildByType(AtomicTypes.VALUE_NAME)
        
        if (valueName == null || valueName.text.isBlank()) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Value requires a name")
                .range(value.textRange)
                .create()
        }
        
        
        val typeReference = AtomicPsiImplUtil.getTypeReference(value)
        if (typeReference == null || typeReference.isBlank()) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Value requires a type")
                .range(value.textRange)
                .create()
        } else {
            
            val typeText = typeReference
            if (typeText.contains("<") || typeText.contains(">")) {
                val openCount = typeText.count { char -> char == '<' }
                val closeCount = typeText.count { char -> char == '>' }
                if (openCount != closeCount) {
                    
                    val colonNode = value.node.findChildByType(AtomicTypes.COLON)
                    if (colonNode != null) {
                        val startOffset = colonNode.textRange.endOffset + 1 
                        val endOffset = value.textRange.endOffset
                        holder.newAnnotation(HighlightSeverity.ERROR, "Unmatched generic type brackets")
                            .range(com.intellij.openapi.util.TextRange(startOffset, endOffset))
                            .create()
                    }
                } else {
                    
                    if (!isValidGenericType(typeText)) {
                        val colonNode = value.node.findChildByType(AtomicTypes.COLON)
                        if (colonNode != null) {
                            val startOffset = colonNode.textRange.endOffset + 1
                            val endOffset = value.textRange.endOffset
                            holder.newAnnotation(HighlightSeverity.ERROR, "Invalid generic type syntax")
                                .range(com.intellij.openapi.util.TextRange(startOffset, endOffset))
                                .create()
                        }
                    }
                }
            }
            
            
            validateTypeExists(value, typeText, holder)
        }
        
        
        if (valueName != null) {
            val file = value.containingFile as? AtomicFile ?: return
            val valuesSection = file.node.findChildByType(AtomicTypes.VALUES_SECTION)?.psi as? AtomicValuesSection
            val allValues = valuesSection?.valueItemList?.mapNotNull {
                it.node.findChildByType(AtomicTypes.VALUE_NAME)?.text
            } ?: emptyList()
            
            val name = valueName.text
            if (allValues.count { it == name } > 1) {
                holder.newAnnotation(HighlightSeverity.WARNING, "Duplicate value name '$name'")
                    .range(valueName.textRange)
                    .create()
            }
        }
    }
    
    private fun isValidGenericType(typeText: String): Boolean {
        
        
        
        
        val trimmed = typeText.replace(" ", "")
        
        
        if (trimmed.contains("<>")) {
            return false
        }
        
        
        if (trimmed.contains("<,") || trimmed.contains(",>") || trimmed.contains(",,")) {
            return false
        }
        
        
        val genericPattern = Regex("<\\s*>")
        if (genericPattern.containsMatchIn(typeText)) {
            return false
        }
        
        
        val cleaned = typeText.replace(" ", "")
        
        
        if (cleaned.contains(">>") && !cleaned.contains(">>").let { 
            
            var depth = 0
            var i = 0
            while (i < cleaned.length) {
                when (cleaned[i]) {
                    '<' -> depth++
                    '>' -> {
                        depth--
                        if (depth < 0) return false
                    }
                }
                i++
            }
            depth == 0
        }) {
            return false
        }
        
        
        val lastCloseIndex = typeText.lastIndexOf('>')
        if (lastCloseIndex != -1 && lastCloseIndex < typeText.length - 1) {
            val afterBracket = typeText.substring(lastCloseIndex + 1).trim()
            
            if (afterBracket.isNotEmpty() && !afterBracket.matches(Regex("^(\\[\\])*$"))) {
                return false
            }
        }
        
        
        var depth = 0
        var i = 0
        var lastWasClose = false
        
        while (i < typeText.length) {
            when (typeText[i]) {
                '<' -> {
                    depth++
                    lastWasClose = false
                }
                '>' -> {
                    depth--
                    if (depth < 0) return false
                    
                    
                    if (i < typeText.length - 1) {
                        val nextChar = typeText[i + 1]
                        if (nextChar != '>' && nextChar != ',' && nextChar != ' ' && nextChar != '[' && nextChar != ']') {
                            
                            val remaining = typeText.substring(i + 1).trim()
                            if (remaining.isNotEmpty() && !remaining.matches(Regex("^(\\[\\])*$"))) {
                                return false
                            }
                        }
                    }
                    
                    lastWasClose = true
                }
            }
            i++
        }
        
        return depth == 0
    }
    
    private fun validateTypeExists(valueItem: AtomicValueItem, typeText: String, holder: AnnotationHolder) {
        val project = valueItem.project
        val service = AtomicGenerationService.getInstance(project)
        
        
        if (!service.isReady()) {
            
            service.registerFileForReAnnotation(valueItem.containingFile)
            return
        }
        
        
        val file = valueItem.containingFile as? AtomicFile ?: return
        val imports = file.children.filterIsInstance<AtomicImportsSection>().firstOrNull()?.let { importsSection ->
            importsSection.importItemList.mapNotNull { importItem ->
                importItem.node.findChildByType(AtomicTypes.IMPORT_PATH)?.text
            }
        } ?: emptyList()
        
        
        val fullTypeNameWithoutGenerics = typeText.substringBefore('<').trim()
        val isFullyQualified = fullTypeNameWithoutGenerics.contains('.')
        
        if (isFullyQualified) {
            
            
            val namespace = fullTypeNameWithoutGenerics.substringBeforeLast('.')
            val typeName = fullTypeNameWithoutGenerics.substringAfterLast('.')
            
            
            val primitiveTypes = setOf(
                "bool", "byte", "sbyte", "char", "decimal", "double", "float",
                "int", "uint", "long", "ulong", "short", "ushort", "string",
                "object", "void", "dynamic"
            )
            
            if (primitiveTypes.contains(typeName)) {
                return 
            }
            
            val validationKey = "type_${fullTypeNameWithoutGenerics}_${imports.joinToString(",")}"
            
            val validationResult = runBlocking {
                withTimeoutOrNull(VALIDATION_TIMEOUT_MS) {
                    try {
                        service.validateType(fullTypeNameWithoutGenerics, imports, project.basePath ?: "")
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            
            if (validationResult == null) {
                if (!pendingValidations.containsKey(validationKey)) {
                    pendingValidations[validationKey] = true
                    
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            service.validateType(fullTypeNameWithoutGenerics, imports, project.basePath ?: "")
                            
                            withContext(Dispatchers.Main) {
                                ReadAction.run<Exception> {
                                    if (valueItem.isValid) {
                                        val file = valueItem.containingFile
                                        if (file != null && file.isValid) {
                                            DaemonCodeAnalyzer.getInstance(project).restart(file)
                                        }
                                    }
                                }
                            }
                        } finally {
                            pendingValidations.remove(validationKey)
                        }
                    }
                }
                return
            }
            
            if (!validationResult.isValid) {
                
                val namespaceValidation = runBlocking {
                    withTimeoutOrNull(VALIDATION_TIMEOUT_MS) {
                        try {
                            service.validateNamespace(fullTypeNameWithoutGenerics, project.basePath ?: "")
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                
                val colonNode = valueItem.node.findChildByType(AtomicTypes.COLON)
                if (colonNode != null) {
                    val valueText = valueItem.text
                    val colonIndex = valueText.indexOf(':')
                    if (colonIndex != -1) {
                        val afterColon = valueText.substring(colonIndex + 1)
                        val typeStartInAfterColon = afterColon.indexOf(fullTypeNameWithoutGenerics)
                        if (typeStartInAfterColon != -1) {
                            val startOffset = valueItem.textRange.startOffset + colonIndex + 1 + typeStartInAfterColon
                            val endOffset = startOffset + fullTypeNameWithoutGenerics.length
                            
                            val message = if (namespaceValidation?.isValid == true) {
                                "'$fullTypeNameWithoutGenerics' is a namespace, not a type"
                            } else {
                                "Cannot resolve type '$fullTypeNameWithoutGenerics'"
                            }
                            
                            holder.newAnnotation(HighlightSeverity.ERROR, message)
                                .range(com.intellij.openapi.util.TextRange(startOffset, endOffset))
                                .create()
                        }
                    }
                }
            }
            
            
            val genericTypes = extractGenericTypes(typeText)
            for (genericType in genericTypes) {
                validateTypeExists(valueItem, genericType, holder)
            }
            
            return
        }
        
        
        
        val mainTypeName = extractMainTypeName(typeText)
        if (mainTypeName.isEmpty()) return
        
        
        val primitiveTypes = setOf(
            "bool", "byte", "sbyte", "char", "decimal", "double", "float",
            "int", "uint", "long", "ulong", "short", "ushort", "string",
            "object", "void", "dynamic"
        )
        
        if (primitiveTypes.contains(mainTypeName)) {
            return 
        }
        
        
        val typeNameWithoutArray = mainTypeName.replace(Regex("\\[\\]"), "")
        
        try {
            
            val validationResult = runBlocking {
                withTimeoutOrNull(VALIDATION_TIMEOUT_MS) {
                    try {
                        service.validateType(typeNameWithoutArray, imports, project.basePath ?: "")
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            
            if (validationResult != null) {
                if (validationResult.isAmbiguous) {
                    
                    val colonNode = valueItem.node.findChildByType(AtomicTypes.COLON)
                    if (colonNode != null) {
                        val valueText = valueItem.text
                        val colonIndex = valueText.indexOf(':')
                        if (colonIndex != -1) {
                            val afterColon = valueText.substring(colonIndex + 1)
                            val typeStartInAfterColon = afterColon.indexOf(mainTypeName)
                            if (typeStartInAfterColon != -1) {
                                val startOffset = valueItem.textRange.startOffset + colonIndex + 1 + typeStartInAfterColon
                                val endOffset = startOffset + mainTypeName.length
                                
                                val namespaceList = validationResult.ambiguousNamespaces.joinToString(", ") { "'$it'" }
                                val message = if (validationResult.ambiguousNamespaces.size == 2) {
                                    "Type '$mainTypeName' is ambiguous between ${validationResult.ambiguousNamespaces[0]} and ${validationResult.ambiguousNamespaces[1]}"
                                } else {
                                    "Type '$mainTypeName' is ambiguous between: $namespaceList"
                                }
                                
                                val annotation = holder.newAnnotation(HighlightSeverity.ERROR, message)
                                    .range(com.intellij.openapi.util.TextRange(startOffset, endOffset))
                                
                                
                                for (namespace in validationResult.ambiguousNamespaces) {
                                    annotation.withFix(UseFullyQualifiedTypeQuickFix(mainTypeName, namespace))
                                }
                                
                                
                                for (namespace in validationResult.ambiguousNamespaces) {
                                    annotation.withFix(RemoveImportToResolveAmbiguityQuickFix(namespace, mainTypeName))
                                }
                                
                                annotation.create()
                            }
                        }
                    }
                } else if (!validationResult.isValid) {
                    val colonNode = valueItem.node.findChildByType(AtomicTypes.COLON)
                    if (colonNode != null) {
                        
                        val valueText = valueItem.text
                        val colonIndex = valueText.indexOf(':')
                        if (colonIndex != -1) {
                            val afterColon = valueText.substring(colonIndex + 1)
                            val typeStartInAfterColon = afterColon.indexOf(mainTypeName)
                            if (typeStartInAfterColon != -1) {
                                val startOffset = valueItem.textRange.startOffset + colonIndex + 1 + typeStartInAfterColon
                                val endOffset = startOffset + mainTypeName.length
                                
                                val message = when {
                                    validationResult.suggestedImports.size > 1 -> 
                                        "Cannot resolve type '$mainTypeName'. Multiple namespaces available."
                                    validationResult.suggestedImports.size == 1 -> 
                                        "Cannot resolve type '$mainTypeName'. Import namespace '${validationResult.suggestedImports[0]}'?"
                                    validationResult.suggestedImport != null -> 
                                        "Cannot resolve type '$mainTypeName'. Import namespace '${validationResult.suggestedImport}'?"
                                    else -> 
                                        "Cannot resolve type '$mainTypeName'"
                                }
                                
                                val annotation = holder.newAnnotation(HighlightSeverity.ERROR, message)
                                    .range(com.intellij.openapi.util.TextRange(startOffset, endOffset))
                                
                                
                                if (validationResult.suggestedImports.isNotEmpty()) {
                                    for (namespace in validationResult.suggestedImports) {
                                        annotation.withFix(AddImportQuickFix(namespace))
                                    }
                                } else if (validationResult.suggestedImport != null) {
                                    
                                    annotation.withFix(AddImportQuickFix(validationResult.suggestedImport))
                                }
                                
                                annotation.create()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            
        }
        
        
        val genericTypes = extractGenericTypes(typeText)
        for (genericType in genericTypes) {
            validateTypeExists(valueItem, genericType, holder)
        }
    }
    
    private fun extractMainTypeName(typeText: String): String {
        
        val genericStart = typeText.indexOf('<')
        val arrayStart = typeText.indexOf('[')
        
        val endIndex = when {
            genericStart == -1 && arrayStart == -1 -> typeText.length
            genericStart == -1 -> arrayStart
            arrayStart == -1 -> genericStart
            else -> minOf(genericStart, arrayStart)
        }
        
        val fullTypeName = typeText.substring(0, endIndex).trim()
        
        
        return if (fullTypeName.contains('.')) {
            fullTypeName.substringAfterLast('.')
        } else {
            fullTypeName
        }
    }
    
    private fun extractGenericTypes(typeText: String): List<String> {
        val result = mutableListOf<String>()
        val genericStart = typeText.indexOf('<')
        if (genericStart == -1) return result
        
        val genericEnd = typeText.lastIndexOf('>')
        if (genericEnd == -1 || genericEnd <= genericStart) return result
        
        val genericContent = typeText.substring(genericStart + 1, genericEnd)
        
        
        var current = StringBuilder()
        var depth = 0
        
        for (char in genericContent) {
            when (char) {
                '<' -> {
                    depth++
                    current.append(char)
                }
                '>' -> {
                    depth--
                    current.append(char)
                }
                ',' -> {
                    if (depth == 0) {
                        result.add(current.toString().trim())
                        current = StringBuilder()
                    } else {
                        current.append(char)
                    }
                }
                else -> current.append(char)
            }
        }
        
        if (current.isNotEmpty()) {
            result.add(current.toString().trim())
        }
        
        return result
    }
    
    private fun validateNamespaceExists(importItem: AtomicImportItem, namespace: String, holder: AnnotationHolder) {
        val project = importItem.project
        val service = AtomicGenerationService.getInstance(project)
        
        
        if (!service.isReady()) {
            
            service.registerFileForReAnnotation(importItem.containingFile)
            return
        }
        
        try {
            val validationKey = "namespace_$namespace"
            
            // Try to get validation result with a very short timeout
            val validationResult = runBlocking {
                withTimeoutOrNull(VALIDATION_TIMEOUT_MS) {
                    try {
                        service.validateNamespace(namespace, project.basePath ?: "")
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            
            if (validationResult == null) {
                // Validation timed out, schedule async validation if not already pending
                if (!pendingValidations.containsKey(validationKey)) {
                    pendingValidations[validationKey] = true
                    
                    // Do validation in background and re-annotate when done
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            service.validateNamespace(namespace, project.basePath ?: "")
                            
                            // Re-annotate the file when validation completes
                            // Must switch to EDT and use read action for PSI access
                            withContext(Dispatchers.Main) {
                                ReadAction.run<Exception> {
                                    if (importItem.isValid) {
                                        val file = importItem.containingFile
                                        if (file != null && file.isValid) {
                                            DaemonCodeAnalyzer.getInstance(project).restart(file)
                                        }
                                    }
                                }
                            }
                        } finally {
                            pendingValidations.remove(validationKey)
                        }
                    }
                }
                return // Skip annotation for now
            }
            
            if (validationResult != null && !validationResult.isValid) {
                val importPath = importItem.node.findChildByType(AtomicTypes.IMPORT_PATH)
                if (importPath != null) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Namespace '$namespace' does not exist")
                        .range(importPath.textRange)
                        .create()
                }
            } else if (validationResult != null && validationResult.isValid) {
                
                val file = importItem.containingFile as? AtomicFile ?: return
                val isUsed = isNamespaceUsed(file, namespace)
                
                if (!isUsed) {
                    val importPath = importItem.node.findChildByType(AtomicTypes.IMPORT_PATH)
                    if (importPath != null) {
                        val annotation = holder.newAnnotation(HighlightSeverity.WARNING, "Import '$namespace' is not used")
                            .range(importPath.textRange)
                            .withFix(RemoveImportQuickFix(namespace))
                        
                        annotation.create()
                    }
                }
            }
        } catch (e: Exception) {
            
        }
    }
    
    private fun isNamespaceUsed(file: AtomicFile, namespace: String): Boolean {
        
        val allImports = file.children.filterIsInstance<AtomicImportsSection>().firstOrNull()?.let { importsSection ->
            importsSection.importItemList.mapNotNull { importItem ->
                importItem.node.findChildByType(AtomicTypes.IMPORT_PATH)?.text
            }
        } ?: emptyList()
        
        
        val entityTypeProp = findEntityTypeProp(file)
        if (entityTypeProp != null) {
            val entityType = AtomicPsiImplUtil.getValue(entityTypeProp)
            if (entityType != null && entityType.isNotBlank()) {
                if (isTypeUsingNamespace(entityType, namespace, allImports, file)) {
                    return true
                }
            }
        }
        
        
        val valuesSection = file.children.filterIsInstance<AtomicValuesSection>().firstOrNull()
        if (valuesSection != null) {
            
            for (valueItem in valuesSection.valueItemList) {
                val typeReference = AtomicPsiImplUtil.getTypeReference(valueItem)
                if (typeReference != null && typeReference.isNotBlank()) {
                    if (isTypeUsingNamespace(typeReference, namespace, allImports, file)) {
                        return true
                    }
                }
            }
        }
        
        return false
    }
    
    private fun findEntityTypeProp(file: AtomicFile): AtomicEntityTypeProp? {
        
        file.children.filterIsInstance<AtomicHeaderSection>().forEach { headerSection ->
            headerSection.entityTypePropList.firstOrNull()?.let { return it }
        }
        
        
        file.children.filterIsInstance<AtomicEntityTypeProp>().firstOrNull()?.let { return it }
        
        
        fun findInElement(element: PsiElement): AtomicEntityTypeProp? {
            element.children.forEach { child ->
                if (child is AtomicEntityTypeProp) return child
                findInElement(child)?.let { return it }
            }
            return null
        }
        
        return findInElement(file)
    }
    
    private fun isTypeUsingNamespace(typeReference: String, targetNamespace: String, allImports: List<String>, file: AtomicFile): Boolean {
        
        val fullTypeNameWithoutGenerics = typeReference.substringBefore('<').trim()
        if (fullTypeNameWithoutGenerics.contains('.')) {
            
            return false
        }
        
        
        val mainTypeName = extractMainTypeName(typeReference)
        
        
        val primitiveTypes = setOf(
            "bool", "byte", "sbyte", "char", "decimal", "double", "float",
            "int", "uint", "long", "ulong", "short", "ushort", "string",
            "object", "void", "dynamic"
        )
        
        if (primitiveTypes.contains(mainTypeName)) {
            return false
        }
        
        val service = AtomicGenerationService.getInstance(file.project)
        val projectPath = file.project.basePath ?: ""
        
        
        val importsWithoutTarget = allImports.filter { it != targetNamespace }
        val validationWithout = runBlocking {
            withTimeoutOrNull(VALIDATION_TIMEOUT_MS) {
                try {
                    service.validateType(mainTypeName, importsWithoutTarget, projectPath)
                } catch (e: Exception) {
                    null
                }
            }
        }
        
        
        val validationWith = runBlocking {
            withTimeoutOrNull(VALIDATION_TIMEOUT_MS) {
                try {
                    service.validateType(mainTypeName, allImports, projectPath)
                } catch (e: Exception) {
                    null
                }
            }
        }
        
        
        
        
        if (validationWithout != null && !validationWithout.isValid && 
            validationWith != null && validationWith.isValid) {
            return true
        }
        
        
        if (validationWithout != null && !validationWithout.isValid) {
            if (validationWithout.suggestedImports.contains(targetNamespace) ||
                validationWithout.suggestedImport == targetNamespace) {
                return true
            }
        }
        
        
        val genericTypes = extractGenericTypes(typeReference)
        for (genericType in genericTypes) {
            if (isTypeUsingNamespace(genericType, targetNamespace, allImports, file)) {
                return true
            }
        }
        
        return false
    }
    
    private fun validateTypeForEntityType(property: AtomicEntityTypeProp, typeText: String, holder: AnnotationHolder) {
        val project = property.project
        val service = AtomicGenerationService.getInstance(project)
        
        
        if (!service.isReady()) {
            service.registerFileForReAnnotation(property.containingFile)
            return
        }
        
        
        val file = property.containingFile as? AtomicFile ?: return
        val imports = file.children.filterIsInstance<AtomicImportsSection>().firstOrNull()?.let { importsSection ->
            importsSection.importItemList.mapNotNull { importItem ->
                importItem.node.findChildByType(AtomicTypes.IMPORT_PATH)?.text
            }
        } ?: emptyList()
        
        
        val fullTypeNameWithoutGenerics = typeText.substringBefore('<').trim()
        val isFullyQualified = fullTypeNameWithoutGenerics.contains('.')
        
        if (isFullyQualified) {
            
            val namespace = fullTypeNameWithoutGenerics.substringBeforeLast('.')
            val typeName = fullTypeNameWithoutGenerics.substringAfterLast('.')
            
            
            if (isPrimitiveType(typeName)) {
                return
            }
            
            val validationResult = runBlocking {
                withTimeoutOrNull(VALIDATION_TIMEOUT_MS) {
                    try {
                        service.validateType(fullTypeNameWithoutGenerics, imports, project.basePath ?: "")
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            
            if (validationResult == null || !validationResult.isValid) {
                
                val colonNode = property.node.findChildByType(AtomicTypes.COLON)
                if (colonNode != null) {
                    val valueNode = property.node.findChildByType(AtomicTypes.IDENTIFIER)
                    if (valueNode != null) {
                        holder.newAnnotation(HighlightSeverity.ERROR, "Cannot resolve type '$fullTypeNameWithoutGenerics'")
                            .range(valueNode.textRange)
                            .create()
                    }
                }
            }
            
            
            val genericTypes = extractGenericTypes(typeText)
            for (genericType in genericTypes) {
                validateTypeForEntityType(property, genericType, holder)
            }
            
            return
        }
        
        
        val mainTypeName = extractMainTypeName(typeText)
        if (mainTypeName.isEmpty() || isPrimitiveType(mainTypeName)) {
            return
        }
        
        val typeNameWithoutArray = mainTypeName.replace(Regex("\\[\\]"), "")
        
        try {
            val validationResult = runBlocking {
                withTimeoutOrNull(VALIDATION_TIMEOUT_MS) {
                    try {
                        service.validateType(typeNameWithoutArray, imports, project.basePath ?: "")
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            
            if (validationResult != null) {
                val colonNode = property.node.findChildByType(AtomicTypes.COLON)
                val valueNode = property.node.findChildByType(AtomicTypes.IDENTIFIER)
                
                if (validationResult.isAmbiguous && colonNode != null && valueNode != null) {
                    
                    val namespaceList = validationResult.ambiguousNamespaces.joinToString(", ") { "'$it'" }
                    val message = if (validationResult.ambiguousNamespaces.size == 2) {
                        "Type '$mainTypeName' is ambiguous between ${validationResult.ambiguousNamespaces[0]} and ${validationResult.ambiguousNamespaces[1]}"
                    } else {
                        "Type '$mainTypeName' is ambiguous between: $namespaceList"
                    }
                    
                    val annotation = holder.newAnnotation(HighlightSeverity.ERROR, message)
                        .range(valueNode.textRange)
                    
                    
                    for (namespace in validationResult.ambiguousNamespaces) {
                        annotation.withFix(UseFullyQualifiedTypeQuickFix(mainTypeName, namespace))
                    }
                    
                    for (namespace in validationResult.ambiguousNamespaces) {
                        annotation.withFix(RemoveImportToResolveAmbiguityQuickFix(namespace, mainTypeName))
                    }
                    
                    annotation.create()
                } else if (!validationResult.isValid && colonNode != null && valueNode != null) {
                    val message = when {
                        validationResult.suggestedImports.size > 1 -> 
                            "Cannot resolve type '$mainTypeName'. Multiple namespaces available."
                        validationResult.suggestedImports.size == 1 -> 
                            "Cannot resolve type '$mainTypeName'. Import namespace '${validationResult.suggestedImports[0]}'?"
                        validationResult.suggestedImport != null -> 
                            "Cannot resolve type '$mainTypeName'. Import namespace '${validationResult.suggestedImport}'?"
                        else -> 
                            "Cannot resolve type '$mainTypeName'"
                    }
                    
                    val annotation = holder.newAnnotation(HighlightSeverity.ERROR, message)
                        .range(valueNode.textRange)
                    
                    
                    if (validationResult.suggestedImports.isNotEmpty()) {
                        for (namespace in validationResult.suggestedImports) {
                            annotation.withFix(AddImportQuickFix(namespace))
                        }
                    } else if (validationResult.suggestedImport != null) {
                        annotation.withFix(AddImportQuickFix(validationResult.suggestedImport))
                    }
                    
                    annotation.create()
                }
            }
        } catch (e: Exception) {
            
        }
        
        
        val genericTypes = extractGenericTypes(typeText)
        for (genericType in genericTypes) {
            validateTypeForEntityType(property, genericType, holder)
        }
    }
    
    private fun isPrimitiveType(typeName: String): Boolean {
        val primitiveTypes = setOf(
            "bool", "byte", "sbyte", "char", "decimal", "double", "float",
            "int", "uint", "long", "ulong", "short", "ushort", "string",
            "object", "void", "dynamic"
        )
        return primitiveTypes.contains(typeName)
    }
}