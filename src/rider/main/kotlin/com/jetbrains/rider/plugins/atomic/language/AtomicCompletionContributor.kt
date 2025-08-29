package com.jetbrains.rider.plugins.atomic.language

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentsOfType
import com.intellij.util.ProcessingContext
import com.jetbrains.rider.plugins.atomic.psi.*
import com.jetbrains.rider.plugins.atomic.services.AtomicGenerationService
import com.jetbrains.rider.plugins.atomic.model.TypeKind
import kotlinx.coroutines.runBlocking
import com.intellij.patterns.ElementPattern
import com.intellij.codeInsight.AutoPopupController
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtil

class AtomicCompletionContributor : CompletionContributor() {
    
    companion object {
        fun addImportToAtomicFile(project: Project, document: Document, namespace: String) {
            val virtualFile = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(document)
            if (virtualFile?.extension != "atomic") {
                return
            }
            
            WriteCommandAction.runWriteCommandAction(project, "Add Import", null, Runnable {
                
                PsiDocumentManager.getInstance(project).commitDocument(document)
                
                val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) as? AtomicFile ?: return@Runnable
                
                if (!psiFile.isValid || psiFile.virtualFile?.extension != "atomic") {
                    return@Runnable
                }
                var importsSection = psiFile.children.filterIsInstance<AtomicImportsSection>().firstOrNull()
                
                if (importsSection == null) {
                    fun findImportsSection(element: PsiElement): AtomicImportsSection? {
                        for (child in element.children) {
                            if (child is AtomicImportsSection) return child
                            findImportsSection(child)?.let { return it }
                        }
                        return null
                    }
                    importsSection = findImportsSection(psiFile)
                }
                
                if (importsSection != null) {
                    
                    val existingImports = importsSection.importItemList.mapNotNull { importItem ->
                        importItem.node.findChildByType(AtomicTypes.IMPORT_PATH)?.text
                    }
                    
                    if (namespace in existingImports) {
                        return@Runnable 
                    }
                    
                    
                    val lastImportItem = importsSection.importItemList.lastOrNull()
                    if (lastImportItem != null) {
                        
                        val offset = lastImportItem.textRange.endOffset
                        document.insertString(offset, "\n- $namespace")
                    } else {
                        
                        val importsKeyword = importsSection.node.findChildByType(AtomicTypes.IMPORTS_KEYWORD)
                        val colonNode = importsSection.node.findChildByType(AtomicTypes.COLON)
                        if (colonNode != null) {
                            val offset = colonNode.textRange.endOffset
                            document.insertString(offset, "\n- $namespace")
                        } else if (importsKeyword != null) {
                            
                            val offset = importsKeyword.textRange.endOffset
                            document.insertString(offset, ":\n- $namespace")
                        }
                    }
                } else {
                    
                    val text = document.text
                    
                    var insertOffset = 0
                    var insertText = ""
                    
                    
                    val existingImportsMatch = Regex("^imports:.*$", RegexOption.MULTILINE).find(text)
                    if (existingImportsMatch != null) {
                        
                        
                        val lineEnd = existingImportsMatch.range.last
                        document.insertString(lineEnd, "\n- $namespace")
                        return@Runnable
                    }
                    
                    val headerPattern = Regex("^(entityType:|aggressiveInlining:|unsafe:|namespace:|className:|directory:|solution:).*$", RegexOption.MULTILINE)
                    val headerMatches = headerPattern.findAll(text).toList()
                    
                    if (headerMatches.isNotEmpty()) {
                        
                        val lastHeaderMatch = headerMatches.maxByOrNull { it.range.first }
                        if (lastHeaderMatch != null) {
                            
                            var lineEnd = lastHeaderMatch.range.last
                            while (lineEnd < text.length && text[lineEnd] != '\n') {
                                lineEnd++
                            }
                            insertOffset = lineEnd
                            insertText = "\n\nimports:\n- $namespace"
                        }
                    } else {
                        
                        val tagsMatch = Regex("^tags:.*$", RegexOption.MULTILINE).find(text)
                        val valuesMatch = Regex("^values:.*$", RegexOption.MULTILINE).find(text)
                        
                        when {
                            tagsMatch != null -> {
                                insertOffset = tagsMatch.range.first
                                insertText = "imports:\n- $namespace\n\n"
                            }
                            valuesMatch != null -> {
                                insertOffset = valuesMatch.range.first
                                insertText = "imports:\n- $namespace\n\n"
                            }
                            else -> {
                                
                                insertOffset = text.length
                                insertText = if (text.isNotEmpty() && !text.endsWith('\n')) {
                                    "\n\nimports:\n- $namespace"
                                } else {
                                    "imports:\n- $namespace"
                                }
                            }
                        }
                    }
                    
                    document.insertString(insertOffset, insertText)
                }
                
                
                PsiDocumentManager.getInstance(project).commitDocument(document)
            })
        }
    }
    
    init {
        
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .andNot(PlatformPatterns.psiElement().inside(AtomicValuesSection::class.java))
                .andNot(PlatformPatterns.psiElement().inside(AtomicImportsSection::class.java))
                .andNot(PlatformPatterns.psiElement().inside(AtomicTagsSection::class.java)),
            GeneralKeywordCompletionProvider()
        )
        
        
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .afterLeaf(PlatformPatterns.psiElement(AtomicTypes.COLON)
                    .afterLeaf(PlatformPatterns.psiElement(AtomicTypes.AGGRESSIVE_INLINING_KEYWORD))
                ),
            BooleanValueCompletionProvider()
        )
        
        
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .afterLeaf(PlatformPatterns.psiElement(AtomicTypes.COLON)
                    .afterLeaf(PlatformPatterns.psiElement(AtomicTypes.UNSAFE_KEYWORD))
                ),
            BooleanValueCompletionProvider()
        )
        
        
        
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .inside(PlatformPatterns.psiElement(AtomicTypes.VALUES_SECTION)),
            CSharpTypeCompletionProvider()
        )
        
        
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(AtomicTypes.TYPE_REFERENCE),
            CSharpTypeCompletionProvider()
        )
        
        
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(AtomicTypes.IDENTIFIER)
                .inside(PlatformPatterns.psiElement(AtomicTypes.VALUES_SECTION))
                .afterLeaf(PlatformPatterns.psiElement(AtomicTypes.COLON)),
            CSharpTypeCompletionProvider()
        )
        
        
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withParent(PlatformPatterns.psiElement(AtomicTypes.VALUE_ITEM)),
            CSharpTypeCompletionProvider()
        )
        
        
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            GenericTypeParameterCompletionProvider()
        )
        
        
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .afterLeaf(PlatformPatterns.psiElement(AtomicTypes.COLON)
                    .afterLeaf(PlatformPatterns.psiElement(AtomicTypes.SOLUTION_KEYWORD))
                ),
            SolutionCompletionProvider()
        )
        
        
        extend(
            CompletionType.BASIC,
            importPathPattern(),
            NamespaceCompletionProvider()
        )
        
        
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .afterLeaf(PlatformPatterns.psiElement(AtomicTypes.COLON)
                    .afterLeaf(PlatformPatterns.psiElement(AtomicTypes.NAMESPACE_KEYWORD))
                ),
            NamespaceCompletionProvider()
        )
        
        
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .afterLeaf(PlatformPatterns.psiElement(AtomicTypes.COLON)
                    .afterLeaf(PlatformPatterns.psiElement(AtomicTypes.DIRECTORY_KEYWORD))
                ),
            DirectoryPathCompletionProvider()
        )
        
        
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .afterLeaf(PlatformPatterns.psiElement(AtomicTypes.COLON)
                    .afterLeaf(PlatformPatterns.psiElement(AtomicTypes.ENTITY_TYPE_KEYWORD))
                ),
            EntityTypeCompletionProvider()
        )
    }
    
    private class GeneralKeywordCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val originalFile = parameters.originalFile
            if (originalFile !is AtomicFile || !originalFile.isValid || originalFile.virtualFile?.extension != "atomic") {
                return
            }
            
            val element = parameters.position
            val file = element.containingFile as? AtomicFile ?: return
            
            val prefix = result.prefixMatcher.prefix
            
            println("AtomicCompletion: Triggered with prefix='$prefix', element=${element.javaClass.simpleName}")
            
            
            val text = file.text
            val offset = parameters.offset
            
            
            var lineStart = offset
            while (lineStart > 0 && text[lineStart - 1] != '\n' && text[lineStart - 1] != '\r') {
                lineStart--
            }
            
            
            val lineText = if (lineStart < offset && lineStart >= 0 && offset <= text.length) {
                text.substring(lineStart, offset)
            } else {
                ""
            }
            
            println("AtomicCompletion: Line text='$lineText'")
            
            
            var currentParent = element.parent
            while (currentParent != null && currentParent !is AtomicFile) {
                if (currentParent is AtomicValuesSection) {
                    println("AtomicCompletion: In values section, skipping keywords")
                    return 
                }
                currentParent = currentParent.parent
            }
            
            
            if (lineText.contains(':')) {
                
                if (lineText.trim().startsWith("-")) {
                    println("AtomicCompletion: Value type position, skipping keywords")
                    return 
                }
                
                
                println("AtomicCompletion: After property colon, skipping keywords")
                return
            }
            
            
            var parent = element.parent
            while (parent != null && parent !is AtomicFile && parent !is AtomicHeaderSection && 
                   parent !is AtomicImportsSection && parent !is AtomicTagsSection && 
                   parent !is AtomicValuesSection) {
                parent = parent.parent
            }
            
            println("AtomicCompletion: Parent context = ${parent?.javaClass?.simpleName ?: "null"}")
            
            
            if (parent is AtomicFile || parent is AtomicHeaderSection) {
                println("AtomicCompletion: Suggesting all keywords")
                
                
                val allKeywords = mapOf(
                    
                    "entityType" to "The C# type to extend",
                    "aggressiveInlining" to "Enable aggressive inlining",
                    "unsafe" to "Enable unsafe access methods",
                    "namespace" to "Target namespace",
                    "className" to "Generated class name", 
                    "directory" to "Output directory",
                    "solution" to "Target C# project",
                    
                    
                    "imports" to "Import C# namespaces",
                    "tags" to "Define tags for the entity",
                    "values" to "Define entity values"
                )
                
                // Check which keywords are already used in the document
                val usedKeywords = mutableSetOf<String>()
                val documentText = file.text
                
                // Check for each keyword with colon (e.g., "header:", "namespace:")
                allKeywords.keys.forEach { keyword ->
                    // Use regex to check if keyword exists at the beginning of a line followed by colon
                    val pattern = Regex("^\\s*$keyword\\s*:", RegexOption.MULTILINE)
                    if (pattern.containsMatchIn(documentText)) {
                        usedKeywords.add(keyword)
                        println("AtomicCompletion: Keyword '$keyword' already used, skipping")
                    }
                }
                
                allKeywords.forEach { (keyword, description) ->
                    // Skip keywords that are already used
                    if (keyword in usedKeywords) {
                        return@forEach
                    }
                    
                    result.addElement(
                        LookupElementBuilder.create(keyword)
                            .withIcon(AtomicIcons.FILE)
                            .withTypeText(when(keyword) {
                                "imports", "tags", "values" -> "section"
                                else -> "property"
                            })
                            .withTailText(" - $description", true)
                            .bold()
                            .withInsertHandler { insertContext, _ ->
                                val editor = insertContext.editor
                                val offset = insertContext.tailOffset
                                editor.document.insertString(offset, ": ")
                                editor.caretModel.moveToOffset(offset + 2)
                            }
                    )
                }
            }
        }
    }
    
    
    private class BooleanValueCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            
            result.addElement(
                LookupElementBuilder.create("true")
                    .withIcon(AtomicIcons.FILE)
                    .withTypeText("boolean")
                    .bold()
            )
            result.addElement(
                LookupElementBuilder.create("false")
                    .withIcon(AtomicIcons.FILE)
                    .withTypeText("boolean")
                    .bold()
            )
        }
    }
    
    private class CSharpTypeCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val originalFile = parameters.originalFile
            if (originalFile !is AtomicFile || !originalFile.isValid || originalFile.virtualFile?.extension != "atomic") {
                return
            }
            

            val element = parameters.position
            val project = parameters.position.project
            val service = AtomicGenerationService.getInstance(project)
            val isAutoTriggered = parameters.isAutoPopup
            val prefix = result.prefixMatcher.prefix
            val text = element.containingFile.text
            val offset = parameters.offset
            
            
            var lineStart = offset
            while (lineStart > 0 && text[lineStart - 1] != '\n' && text[lineStart - 1] != '\r') {
                lineStart--
            }
            
            val lineText = if (lineStart < offset) text.substring(lineStart, offset) else ""

            
            var inValuesSection = false
            var currentParent = element.parent
            while (currentParent != null && currentParent !is AtomicFile) {
                if (currentParent is AtomicValuesSection) {
                    inValuesSection = true
                    break
                }
                currentParent = currentParent.parent
            }
            
            
            if (!inValuesSection) {
                return
            }
            
            
            var insideGeneric = false
            var angleDepth = 0
            for (i in (offset - 1) downTo lineStart) {
                when (text[i]) {
                    '>' -> angleDepth++
                    '<' -> {
                        angleDepth--
                        if (angleDepth < 0) {
                            insideGeneric = true
                            break
                        }
                    }
                }
            }
            
            if (insideGeneric) {
                return
            }
            
            
            val beforeCursor = lineText.trimEnd()
            if (!beforeCursor.contains(":") || beforeCursor.endsWith(":")) {
                return
            }
            
            
            val actualPrefix = getActualPrefix(parameters)

            
            val fullText = getFullTypeText(parameters)

            val namespaceFilter = if (fullText.contains('.')) {
                val lastDotIndex = fullText.lastIndexOf('.')
                val caretPositionInType = parameters.offset - (parameters.position.textRange.startOffset - actualPrefix.length)

                if (caretPositionInType > lastDotIndex) {
                    
                    val ns = fullText.substring(0, lastDotIndex)
                    ns
                } else null
            } else null
            

            val prefixToUse = if (namespaceFilter != null) {
                
                val afterDot = fullText.substringAfterLast('.')
                if (afterDot != fullText) afterDot else ""
            } else if (actualPrefix.isNotEmpty()) {
                actualPrefix
            } else {
                prefix
            }
            
            
            val resultWithConfidence = if (namespaceFilter != null) {
                
                result.withPrefixMatcher(prefixToUse).caseInsensitive()
            } else {
                result.withPrefixMatcher(prefixToUse).caseInsensitive()
            }
            
            
            val atomicFile = parameters.originalFile as? AtomicFile
            val imports: List<String> = atomicFile?.let { af ->
                
                af.children.filterIsInstance<AtomicImportsSection>().firstOrNull()?.let { importsSection ->
                    importsSection.importItemList.mapNotNull { importItem ->
                        importItem.node.findChildByType(AtomicTypes.IMPORT_PATH)?.text
                    }
                } ?: emptyList()
            } ?: emptyList()
            
            
            val projectPath = project.basePath ?: ""
            

            
            try {
                val backendTypes = runBlocking {
                    try {
                        service.getTypeCompletions(prefixToUse, imports, projectPath, namespaceFilter)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emptyList()
                    }
                }
                

                if (namespaceFilter != null) {
                    backendTypes.take(5).forEach {
                        println("  - ${it.typeName} (${it.namespace})")
                    }
                }
                
                backendTypes.forEach { typeItem ->
                    
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
                    
                    
                    val needsImport = !isNamespace && typeItem.namespace.isNotEmpty() && !imports.contains(typeItem.namespace)
                    
                    
                    
                    val lookupString = if (typeItem.namespace.isNotEmpty()) {
                        "${typeItem.typeName}_${typeItem.namespace.hashCode()}"
                    } else {
                        typeItem.typeName
                    }
                    
                    val element = LookupElementBuilder.create(lookupString)
                        .withLookupString(typeItem.typeName) 
                        .withPresentableText(typeItem.typeName) 
                        .withIcon(icon)
                        .withTypeText(if (isNamespace) "namespace" else typeItem.namespace)
                        .withTailText(if (!isNamespace && typeItem.isGeneric) "<>" else "", true)
                        .withInsertHandler { insertContext, _ ->
                            val document = insertContext.document
                            val project = insertContext.project
                            val editor = insertContext.editor
                            val startOffset = insertContext.startOffset
                            val tailOffset = insertContext.tailOffset
                            
                            if (isNamespace) {
                                
                                document.replaceString(startOffset, tailOffset, typeItem.typeName + ".")
                                
                                val newOffset = startOffset + typeItem.typeName.length + 1
                                editor.caretModel.moveToOffset(newOffset)
                                
                                ApplicationManager.getApplication().invokeLater {
                                    AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
                                }
                            } else {
                                
                                document.replaceString(startOffset, tailOffset, typeItem.typeName)
                                
                                
                                if (typeItem.isGeneric) {
                                    val offset = insertContext.tailOffset
                                    document.insertString(offset, "<>")
                                    editor.caretModel.moveToOffset(offset + 1)
                                }
                                
                                
                                val textBeforeStart = if (startOffset > 0) {
                                    val lookBackLength = minOf(startOffset, 100) 
                                    document.getText(com.intellij.openapi.util.TextRange(startOffset - lookBackLength, startOffset))
                                } else {
                                    ""
                                }
                                
                                
                                val isAfterNamespace = typeItem.namespace.isNotEmpty() && 
                                    textBeforeStart.trimEnd().endsWith("${typeItem.namespace}.")
                                
                                
                                if (needsImport && typeItem.namespace.isNotEmpty() && !isAfterNamespace) {
                                    ApplicationManager.getApplication().invokeLater {
                                        AtomicCompletionContributor.addImportToAtomicFile(project, document, typeItem.namespace)
                                    }
                                }
                            }
                        }
                    
                    
                    if (namespaceFilter != null) {
                        
                        resultWithConfidence.addElement(element)
                    } else {
                        
                        result.addElement(element)
                    }
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            
            if (isAutoTriggered) {
                result.restartCompletionWhenNothingMatches()
            }
        }
        
        
        private fun getActualPrefix(parameters: CompletionParameters): String {
            val element = parameters.position
            val document = parameters.editor.document
            val offset = parameters.offset
            
            
            val text = document.text
            var actualOffset = offset
            
            
            if (element.text == "IntellijIdeaRulezzz") {
                actualOffset = element.textOffset
            }
            
            
            var wordStart = actualOffset
            while (wordStart > 0 && text[wordStart - 1].isLetterOrDigit()) {
                wordStart--
            }
            
            val prefix = if (wordStart < actualOffset) {
                text.substring(wordStart, actualOffset)
            } else {
                ""
            }
            
            return prefix
        }
        
        private fun getFullTypeText(parameters: CompletionParameters): String {
            val element = parameters.position
            val document = parameters.editor.document
            val text = document.text
            val offset = parameters.offset
            
            
            var lineStart = offset
            while (lineStart > 0 && text[lineStart - 1] != '\n' && text[lineStart - 1] != '\r') {
                lineStart--
            }
            
            var lineEnd = offset
            while (lineEnd < text.length && text[lineEnd] != '\n' && text[lineEnd] != '\r') {
                lineEnd++
            }
            
            val line = text.substring(lineStart, lineEnd)
            val colonIndex = line.indexOf(':')
            if (colonIndex == -1) return ""
            
            
            val afterColon = line.substring(colonIndex + 1).trimStart()
            
            
            var typeStart = 0
            while (typeStart < afterColon.length && !afterColon[typeStart].isLetterOrDigit()) {
                typeStart++
            }
            
            
            val caretInLine = offset - lineStart
            val caretInAfterColon = caretInLine - colonIndex - 1 - (line.length - colonIndex - 1 - afterColon.length)
            
            if (caretInAfterColon < 0) return ""
            
            
            val typeText = if (caretInAfterColon <= afterColon.length) {
                afterColon.substring(typeStart, typeStart + caretInAfterColon - typeStart)
            } else {
                afterColon.substring(typeStart)
            }
            
            
            return typeText.replace("IntellijIdeaRulezzz", "")
        }
    }
    
    private class GenericTypeParameterCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val originalFile = parameters.originalFile
            if (originalFile !is AtomicFile || !originalFile.isValid || originalFile.virtualFile?.extension != "atomic") {
                return
            }
            
            val element = parameters.position
            val text = element.containingFile.text
            val offset = parameters.offset
            
            
            var insideGeneric = false
            var angleDepth = 0
            
            
            for (i in (offset - 1) downTo 0) {
                when (text[i]) {
                    '>' -> angleDepth++
                    '<' -> {
                        angleDepth--
                        if (angleDepth < 0) {
                            insideGeneric = true
                            break
                        }
                    }
                    '\n', '\r' -> break 
                }
            }
            
            if (!insideGeneric) {
                return
            }
            
            
            var inValuesSection = false
            var parent = element.parent
            while (parent != null && parent !is AtomicFile) {
                if (parent is AtomicValuesSection) {
                    inValuesSection = true
                    break
                }
                parent = parent.parent
            }
            
            if (!inValuesSection) {
                return
            }
            
            println("GenericTypeParameterCompletionProvider: Inside generic type parameters!")
            
            
            val project = parameters.position.project
            val service = AtomicGenerationService.getInstance(project)
            
            
            val rawPrefix = result.prefixMatcher.prefix
            val actualPrefix = if (rawPrefix.contains('<')) {
                
                rawPrefix.substringAfterLast('<')
            } else {
                getActualPrefix(parameters)
            }
            
            val prefixToUse = actualPrefix
            
            println("GenericTypeParameterCompletionProvider: rawPrefix='$rawPrefix', actualPrefix='$actualPrefix', prefixToUse='$prefixToUse'")
            
            
            val resultWithCorrectPrefix = result.withPrefixMatcher(prefixToUse).caseInsensitive()
            
            
            val atomicFile = parameters.originalFile as? AtomicFile
            val imports: List<String> = atomicFile?.let { af ->
                af.children.filterIsInstance<AtomicImportsSection>().firstOrNull()?.let { importsSection ->
                    importsSection.importItemList.mapNotNull { importItem ->
                        importItem.node.findChildByType(AtomicTypes.IMPORT_PATH)?.text
                    }
                } ?: emptyList()
            } ?: emptyList()
            
            val projectPath = project.basePath ?: ""
            
            
            try {
                val backendTypes = runBlocking {
                    try {
                        service.getTypeCompletions(prefixToUse, imports, projectPath, null)
                    } catch (e: Exception) {
                        println("GenericTypeParameterCompletionProvider: Backend error - ${e.message}")
                        emptyList()
                    }
                }
                
                println("GenericTypeParameterCompletionProvider: Got ${backendTypes.size} types from backend")
                
                backendTypes.forEach { typeItem ->
                    val icon = when (typeItem.typeKind) {
                        TypeKind.Class -> AllIcons.Nodes.Class
                        TypeKind.Interface -> AllIcons.Nodes.Interface
                        TypeKind.Struct -> AllIcons.Nodes.Static
                        TypeKind.Enum -> AllIcons.Nodes.Enum
                        TypeKind.Delegate -> AllIcons.Nodes.Lambda
                    }
                    
                    
                    val needsImport = typeItem.namespace.isNotEmpty() && !imports.contains(typeItem.namespace)
                    
                    
                    val lookupString = if (typeItem.namespace.isNotEmpty()) {
                        "${typeItem.typeName}_${typeItem.namespace.hashCode()}"
                    } else {
                        typeItem.typeName
                    }
                    
                    val element = LookupElementBuilder.create(lookupString)
                        .withLookupString(typeItem.typeName)
                        .withPresentableText(typeItem.typeName)
                        .withIcon(icon)
                        .withTypeText(typeItem.namespace)
                        .withTailText(if (typeItem.isGeneric) "<>" else "", true)
                        .withInsertHandler { insertContext, _ ->
                            val document = insertContext.document
                            val project = insertContext.project
                            val editor = insertContext.editor
                            val startOffset = insertContext.startOffset
                            val tailOffset = insertContext.tailOffset
                            document.replaceString(startOffset, tailOffset, typeItem.typeName)
                            
                            
                            if (typeItem.isGeneric) {
                                val offset = insertContext.tailOffset
                                document.insertString(offset, "<>")
                                editor.caretModel.moveToOffset(offset + 1)
                            }
                            
                            
                            if (needsImport && typeItem.namespace.isNotEmpty()) {
                                ApplicationManager.getApplication().invokeLater {
                                    AtomicCompletionContributor.addImportToAtomicFile(project, document, typeItem.namespace)
                                }
                            }
                        }
                    
                    resultWithCorrectPrefix.addElement(element)
                }
            } catch (e: Exception) {
                println("GenericTypeParameterCompletionProvider: Exception - ${e.message}")
                e.printStackTrace()
            }
        }
        
        private fun getActualPrefix(parameters: CompletionParameters): String {
            val element = parameters.position
            val document = parameters.editor.document
            val offset = parameters.offset
            
            val text = document.text
            var actualOffset = offset
            
            
            if (element.text == "IntellijIdeaRulezzz") {
                actualOffset = element.textOffset
            }
            
            
            var wordStart = actualOffset
            while (wordStart > 0 && text[wordStart - 1].isLetterOrDigit()) {
                wordStart--
            }
            
            val prefix = if (wordStart < actualOffset) {
                text.substring(wordStart, actualOffset)
            } else {
                ""
            }
            
            return prefix
        }
    }
    
    private class SolutionCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val originalFile = parameters.originalFile
            if (originalFile !is AtomicFile || !originalFile.isValid || originalFile.virtualFile?.extension != "atomic") {
                return
            }
            
            val project = parameters.position.project
            val service = AtomicGenerationService.getInstance(project)
            
            
            if (!ApplicationManager.getApplication().isDispatchThread) {
                try {
                    val projects = runBlocking {
                        service.getAvailableProjects()
                    }
                    
                    projects.forEach { projectName ->
                        
                        val insertText = if (projectName.contains('-') || projectName.contains(' ')) {
                            "\"$projectName\""
                        } else {
                            projectName
                        }
                        
                        result.addElement(
                            LookupElementBuilder.create(insertText)
                                .withPresentableText(projectName)
                                .withIcon(AllIcons.Nodes.Project)
                                .withTypeText("C# Project")
                                .withBoldness(true)
                        )
                    }
                } catch (e: Exception) {
                    
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun importPathPattern(): ElementPattern<PsiElement> {
        return PlatformPatterns.psiElement()
            .withParent(PlatformPatterns.psiElement(AtomicTypes.IMPORT_ITEM))
    }
    
    private class NamespaceCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val originalFile = parameters.originalFile
            if (originalFile !is AtomicFile || !originalFile.isValid || originalFile.virtualFile?.extension != "atomic") {
                return
            }
            
            val element = parameters.position
            val project = element.project
            val service = AtomicGenerationService.getInstance(project)
            
            
            val prefix = result.prefixMatcher.prefix
            val projectPath = project.basePath ?: ""
            
            println("NamespaceCompletionProvider: Getting namespaces for prefix='$prefix'")
            
            
            try {
                val namespaces = runBlocking {
                    try {
                        service.getNamespaceCompletions(prefix, projectPath)
                    } catch (e: Exception) {
                        println("NamespaceCompletionProvider: Backend error - ${e.message}")
                        emptyList()
                    }
                }
                
                println("NamespaceCompletionProvider: Got ${namespaces.size} namespaces from backend")
                
                namespaces.forEach { namespace ->
                    result.addElement(
                        LookupElementBuilder.create(namespace)
                            .withIcon(AllIcons.Nodes.Package)
                            .withTypeText("namespace")
                            .withBoldness(namespace.startsWith("System"))
                    )
                }
            } catch (e: Exception) {
                println("NamespaceCompletionProvider: Exception - ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private class DirectoryPathCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val originalFile = parameters.originalFile
            if (originalFile !is AtomicFile || !originalFile.isValid || originalFile.virtualFile?.extension != "atomic") {
                return
            }
            
            val project = parameters.position.project
            val projectBasePath = project.basePath ?: return
            val projectBaseDir = VfsUtil.findFileByIoFile(java.io.File(projectBasePath), true) ?: return
            
            if (!projectBaseDir.isValid) {
                return
            }
            
            
            val element = parameters.position
            val document = parameters.editor.document
            val text = document.text
            val lineStart = findLineStart(text, parameters.offset)
            val lineText = getLineText(text, lineStart, parameters.offset)
            
            
            val colonIndex = lineText.indexOf(':')
            if (colonIndex == -1) return
            
            val pathPrefix = lineText.substring(colonIndex + 1).trim()
                .removeSurrounding("\"")
                .replace("IntellijIdeaRulezzz", "") 
            
            println("DirectoryPathCompletionProvider: pathPrefix='$pathPrefix'")
            
            
            val (parentPath, namePrefix) = if (pathPrefix.contains('/')) {
                val lastSlashIndex = pathPrefix.lastIndexOf('/')
                val parent = pathPrefix.substring(0, lastSlashIndex + 1)
                val prefix = pathPrefix.substring(lastSlashIndex + 1)
                parent to prefix
            } else {
                "" to pathPrefix
            }
            
            println("DirectoryPathCompletionProvider: parentPath='$parentPath', namePrefix='$namePrefix'")
            
            
            val parentDir = if (parentPath.isEmpty()) {
                projectBaseDir
            } else {
                projectBaseDir.findFileByRelativePath(parentPath.removeSuffix("/"))
            }
            
            if (parentDir == null || !parentDir.isValid || !parentDir.isDirectory) {
                println("DirectoryPathCompletionProvider: Parent directory not found, invalid, or not a directory")
                return
            }
            
            
            val resultWithPrefix = result.withPrefixMatcher(namePrefix).caseInsensitive()
            
            
            parentDir.children.filter { it.isValid && it.isDirectory }.forEach { dir ->
                val relativePath = VfsUtil.getRelativePath(dir, projectBaseDir) ?: return@forEach
                
                
                val isImportant = when (dir.name) {
                    "Assets", "Packages", "ProjectSettings" -> true
                    else -> false
                }
                
                
                val isMetaFolder = dir.name.endsWith(".meta")
                if (isMetaFolder) return@forEach 
                
                val icon = when {
                    dir.name == "Assets" -> AllIcons.Modules.ResourcesRoot
                    dir.name == "Scripts" -> AllIcons.Nodes.Package
                    else -> AllIcons.Nodes.Folder
                }
                
                
                val lookupString = dir.name
                
                
                val insertString = if (parentPath.isEmpty()) {
                    "${dir.name}/"
                } else {
                    "$parentPath${dir.name}/"
                }
                
                resultWithPrefix.addElement(
                    LookupElementBuilder.create(lookupString)
                        .withPresentableText(dir.name)
                        .withIcon(icon)
                        .withTypeText("directory")
                        .withBoldness(isImportant)
                        .withInsertHandler { insertContext, _ ->
                            val doc = insertContext.document
                            val offset = insertContext.startOffset
                            val endOffset = insertContext.tailOffset
                            
                            
                            var pathStart = offset
                            while (pathStart > 0 && doc.text[pathStart - 1] != ':') {
                                pathStart--
                            }
                            
                            
                            while (pathStart < doc.text.length && doc.text[pathStart].isWhitespace()) {
                                pathStart++
                            }
                            
                            
                            doc.replaceString(pathStart, endOffset, insertString)
                            
                            
                            val newOffset = pathStart + insertString.length
                            insertContext.editor.caretModel.moveToOffset(newOffset)
                            
                            
                            ApplicationManager.getApplication().invokeLater {
                                AutoPopupController.getInstance(project).scheduleAutoPopup(insertContext.editor)
                            }
                        }
                )
            }
            
            
            if (parentPath.isNotEmpty() && parentPath != "/") {
                resultWithPrefix.addElement(
                    LookupElementBuilder.create("..")
                        .withPresentableText(".. (parent directory)")
                        .withIcon(AllIcons.Actions.Back)
                        .withTypeText("go up")
                        .withInsertHandler { insertContext, _ ->
                            val doc = insertContext.document
                            val offset = insertContext.startOffset
                            
                            
                            var pathStart = offset
                            while (pathStart > 0 && doc.text[pathStart - 1] != ':') {
                                pathStart--
                            }
                            while (pathStart < doc.text.length && doc.text[pathStart].isWhitespace()) {
                                pathStart++
                            }
                            
                            
                            val newPath = if (parentPath.count { it == '/' } > 1) {
                                parentPath.substringBeforeLast('/', "").substringBeforeLast('/') + "/"
                            } else {
                                ""
                            }
                            
                            
                            doc.replaceString(pathStart, insertContext.tailOffset, newPath)
                            
                            
                            val newOffset = pathStart + newPath.length
                            insertContext.editor.caretModel.moveToOffset(newOffset)
                            
                            ApplicationManager.getApplication().invokeLater {
                                AutoPopupController.getInstance(project).scheduleAutoPopup(insertContext.editor)
                            }
                        }
                )
            }
            
            
            result.restartCompletionWhenNothingMatches()
        }
        
        private fun findLineStart(text: String, offset: Int): Int {
            var lineStart = offset
            while (lineStart > 0 && text[lineStart - 1] != '\n' && text[lineStart - 1] != '\r') {
                lineStart--
            }
            return lineStart
        }
        
        private fun getLineText(text: String, lineStart: Int, offset: Int): String {
            return if (lineStart < offset && lineStart >= 0 && offset <= text.length) {
                text.substring(lineStart, offset)
            } else {
                ""
            }
        }
    }
    
    private class EntityTypeCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val originalFile = parameters.originalFile
            if (originalFile !is AtomicFile || !originalFile.isValid || originalFile.virtualFile?.extension != "atomic") {
                return
            }
            
            val element = parameters.position
            val project = element.project
            val service = AtomicGenerationService.getInstance(project)
            
            
            val prefix = result.prefixMatcher.prefix
            val actualPrefix = getActualPrefix(parameters)
            val prefixToUse = if (actualPrefix.isNotEmpty()) actualPrefix else prefix
            
            println("EntityTypeCompletionProvider: prefix='$prefix', actualPrefix='$actualPrefix'")
            
            
            val fullText = getFullTypeText(parameters)
            val namespaceFilter = if (fullText.contains('.')) {
                val lastDotIndex = fullText.lastIndexOf('.')
                val caretPositionInType = parameters.offset - (parameters.position.textRange.startOffset - actualPrefix.length)
                
                if (caretPositionInType > lastDotIndex) {
                    fullText.substring(0, lastDotIndex)
                } else null
            } else null
            
            
            val searchPrefix = if (namespaceFilter != null) {
                val afterDot = fullText.substringAfterLast('.')
                if (afterDot != fullText) afterDot else ""
            } else {
                prefixToUse
            }
            
            
            val resultWithPrefix = if (namespaceFilter != null) {
                result.withPrefixMatcher(searchPrefix).caseInsensitive()
            } else {
                result.withPrefixMatcher(prefixToUse).caseInsensitive()
            }
            
            
            val atomicFile = parameters.originalFile as? AtomicFile
            val imports: List<String> = atomicFile?.let { af ->
                af.children.filterIsInstance<AtomicImportsSection>().firstOrNull()?.let { importsSection ->
                    importsSection.importItemList.mapNotNull { importItem ->
                        importItem.node.findChildByType(AtomicTypes.IMPORT_PATH)?.text
                    }
                } ?: emptyList()
            } ?: emptyList()
            
            val projectPath = project.basePath ?: ""
            
            
            try {
                val backendTypes = runBlocking {
                    try {
                        service.getTypeCompletions(searchPrefix, imports, projectPath, namespaceFilter)
                    } catch (e: Exception) {
                        println("EntityTypeCompletionProvider: Backend error - ${e.message}")
                        emptyList()
                    }
                }
                
                println("EntityTypeCompletionProvider: Got ${backendTypes.size} types from backend")
                
                backendTypes.forEach { typeItem ->
                    
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
                    
                    
                    val needsImport = !isNamespace && typeItem.namespace.isNotEmpty() && !imports.contains(typeItem.namespace)
                    
                    
                    val lookupString = if (typeItem.namespace.isNotEmpty() && !isNamespace) {
                        "${typeItem.typeName}_${typeItem.namespace.hashCode()}"
                    } else {
                        typeItem.typeName
                    }
                    
                    val element = LookupElementBuilder.create(lookupString)
                        .withLookupString(typeItem.typeName)
                        .withPresentableText(typeItem.typeName)
                        .withIcon(icon)
                        .withTypeText(if (isNamespace) "namespace" else typeItem.namespace)
                        .withTailText(if (!isNamespace && typeItem.isGeneric) "<>" else "", true)
                        .withInsertHandler { insertContext, _ ->
                            val document = insertContext.document
                            val editor = insertContext.editor
                            val startOffset = insertContext.startOffset
                            val tailOffset = insertContext.tailOffset
                            
                            if (isNamespace) {
                                
                                document.replaceString(startOffset, tailOffset, typeItem.typeName + ".")
                                val newOffset = startOffset + typeItem.typeName.length + 1
                                editor.caretModel.moveToOffset(newOffset)
                                
                                ApplicationManager.getApplication().invokeLater {
                                    AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
                                }
                            } else {
                                
                                document.replaceString(startOffset, tailOffset, typeItem.typeName)
                                
                                
                                if (typeItem.isGeneric) {
                                    val offset = insertContext.tailOffset
                                    document.insertString(offset, "<>")
                                    editor.caretModel.moveToOffset(offset + 1)
                                }
                                
                                
                                val textBeforeStart = if (startOffset > 0) {
                                    val lookBackLength = minOf(startOffset, 100)
                                    document.getText(com.intellij.openapi.util.TextRange(startOffset - lookBackLength, startOffset))
                                } else {
                                    ""
                                }
                                
                                val isAfterNamespace = typeItem.namespace.isNotEmpty() && 
                                    textBeforeStart.trimEnd().endsWith("${typeItem.namespace}.")
                                
                                
                                if (needsImport && typeItem.namespace.isNotEmpty() && !isAfterNamespace) {
                                    ApplicationManager.getApplication().invokeLater {
                                        AtomicCompletionContributor.addImportToAtomicFile(project, document, typeItem.namespace)
                                    }
                                }
                            }
                        }
                    
                    
                    if (namespaceFilter != null) {
                        resultWithPrefix.addElement(element)
                    } else {
                        result.addElement(element)
                    }
                }
            } catch (e: Exception) {
                println("EntityTypeCompletionProvider: Exception - ${e.message}")
                e.printStackTrace()
            }
        }
        
        private fun getActualPrefix(parameters: CompletionParameters): String {
            val element = parameters.position
            val document = parameters.editor.document
            val offset = parameters.offset
            val text = document.text
            var actualOffset = offset
            
            if (element.text == "IntellijIdeaRulezzz") {
                actualOffset = element.textOffset
            }
            
            var wordStart = actualOffset
            while (wordStart > 0 && text[wordStart - 1].isLetterOrDigit()) {
                wordStart--
            }
            
            return if (wordStart < actualOffset) {
                text.substring(wordStart, actualOffset)
            } else {
                ""
            }
        }
        
        private fun getFullTypeText(parameters: CompletionParameters): String {
            val document = parameters.editor.document
            val text = document.text
            val offset = parameters.offset
            
            var lineStart = offset
            while (lineStart > 0 && text[lineStart - 1] != '\n' && text[lineStart - 1] != '\r') {
                lineStart--
            }
            
            var lineEnd = offset
            while (lineEnd < text.length && text[lineEnd] != '\n' && text[lineEnd] != '\r') {
                lineEnd++
            }
            
            val line = text.substring(lineStart, lineEnd)
            val colonIndex = line.indexOf(':')
            if (colonIndex == -1) return ""
            
            val afterColon = line.substring(colonIndex + 1).trimStart()
            
            var typeStart = 0
            while (typeStart < afterColon.length && !afterColon[typeStart].isLetterOrDigit()) {
                typeStart++
            }
            
            val caretInLine = offset - lineStart
            val caretInAfterColon = caretInLine - colonIndex - 1 - (line.length - colonIndex - 1 - afterColon.length)
            
            if (caretInAfterColon < 0) return ""
            
            val typeText = if (caretInAfterColon <= afterColon.length) {
                afterColon.substring(typeStart, typeStart + caretInAfterColon - typeStart)
            } else {
                afterColon.substring(typeStart)
            }
            
            return typeText.replace("IntellijIdeaRulezzz", "")
        }
    }
}