package com.jetbrains.rider.plugins.atomic.psi.impl

import com.intellij.psi.tree.IElementType
import com.jetbrains.rider.plugins.atomic.psi.*

object AtomicPsiImplUtil {
    @JvmStatic
    fun getKey(element: AtomicHeaderProp): String = "header"
    
    @JvmStatic
    fun getValue(element: AtomicHeaderProp): String? {
        val string = element.node.findChildByType(AtomicTypes.STRING)
        return string?.text?.trim('"')
    }
    
    @JvmStatic
    fun getKey(element: AtomicEntityTypeProp): String = "entityType"
    
    @JvmStatic
    fun getValue(element: AtomicEntityTypeProp): String? {
        return element.node.findChildByType(AtomicTypes.IDENTIFIER)?.text
    }
    
    @JvmStatic
    fun getKey(element: AtomicAggressiveInliningProp): String = "aggressiveInlining"
    
    @JvmStatic
    fun getBooleanValue(element: AtomicAggressiveInliningProp): Boolean {
        val value = element.node.findChildByType(AtomicTypes.TRUE)
        return value != null
    }
    
    @JvmStatic
    fun getKey(element: AtomicUnsafeProp): String = "unsafe"
    
    @JvmStatic
    fun getBooleanValue(element: AtomicUnsafeProp): Boolean {
        val value = element.node.findChildByType(AtomicTypes.TRUE)
        return value != null
    }
    
    @JvmStatic
    fun getKey(element: AtomicNamespaceProp): String = "namespace"
    
    @JvmStatic
    fun getValue(element: AtomicNamespaceProp): String? {
        return element.node.findChildByType(AtomicTypes.NAMESPACE_VALUE)?.text
    }
    
    @JvmStatic
    fun getKey(element: AtomicClassNameProp): String = "className"
    
    @JvmStatic
    fun getValue(element: AtomicClassNameProp): String? {
        return element.node.findChildByType(AtomicTypes.IDENTIFIER)?.text
    }
    
    @JvmStatic
    fun getKey(element: AtomicDirectoryProp): String = "directory"
    
    @JvmStatic
    fun getValue(element: AtomicDirectoryProp): String? {
        val string = element.node.findChildByType(AtomicTypes.STRING)
        if (string != null) {
            return string.text?.trim('"')
        }
        
        val typeRef = element.node.findChildByType(AtomicTypes.TYPE_REFERENCE)
        if (typeRef != null) {
            return typeRef.text
        }
        
        val identifier = element.node.findChildByType(AtomicTypes.IDENTIFIER)
        if (identifier != null) {
            return identifier.text
        }
        
        return null
    }
    
    @JvmStatic
    fun getKey(element: AtomicSolutionProp): String = "solution"
    
    @JvmStatic
    fun getValue(element: AtomicSolutionProp): String? {
        val string = element.node.findChildByType(AtomicTypes.STRING)
        if (string != null) {
            return string.text?.trim('"')
        }
        
        val identifier = element.node.findChildByType(AtomicTypes.IDENTIFIER)
        if (identifier != null) {
            return identifier.text
        }
        
        return null
    }
    
    @JvmStatic
    fun getImports(element: AtomicImportsSection): List<AtomicImportItem> {
        return element.importItemList
    }
    
    @JvmStatic
    fun getImportPath(element: AtomicImportItem): String? {
        return element.node.findChildByType(AtomicTypes.IMPORT_PATH)?.text
    }
    
    @JvmStatic
    fun getTagName(element: AtomicTagItem): String? {
        return element.node.findChildByType(AtomicTypes.TAG_NAME)?.text
    }
    
    @JvmStatic
    fun getTags(element: AtomicTagsSection): List<AtomicTagItem> {
        return element.tagItemList
    }
    
    
    @JvmStatic
    fun getValues(element: AtomicValuesSection): List<AtomicValueItem> {
        return element.valueItemList
    }
    
    @JvmStatic
    fun getValueName(element: AtomicValueItem): String? {
        return element.node.findChildByType(AtomicTypes.VALUE_NAME)?.text
    }
    
    
    @JvmStatic
    fun getTypeReference(element: AtomicValueItem): String? {
        // Collect all TYPE_REFERENCE tokens that form the complete type
        // This handles cases like Tuple<Tuple<TestType3>> where we have multiple tokens
        val colonNode = element.node.findChildByType(AtomicTypes.COLON)
        if (colonNode != null) {
            val typeTokens = mutableListOf<String>()
            var currentNode = colonNode.treeNext
            
            while (currentNode != null && currentNode.elementType != AtomicTypes.CRLF) {
                if (currentNode.elementType == AtomicTypes.TYPE_REFERENCE) {
                    typeTokens.add(currentNode.text)
                }
                currentNode = currentNode.treeNext
            }
            
            return typeTokens.joinToString("")
        }
        
        return element.node.findChildByType(AtomicTypes.TYPE_REFERENCE)?.text
    }
}