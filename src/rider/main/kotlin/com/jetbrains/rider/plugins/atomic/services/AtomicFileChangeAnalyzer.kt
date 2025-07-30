package com.jetbrains.rider.plugins.atomic.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.rider.plugins.atomic.psi.*

/**
 * Analyzes changes in atomic files to determine if regeneration is needed
 */
@Service(Service.Level.PROJECT)
class AtomicFileChangeAnalyzer(private val project: Project) {
    
    companion object {
        private val LOG = Logger.getInstance(AtomicFileChangeAnalyzer::class.java)
        
        fun getInstance(project: Project): AtomicFileChangeAnalyzer {
            return project.getService(AtomicFileChangeAnalyzer::class.java)
        }
        
        private val REGENERATION_TRIGGER_FIELDS = setOf(
            "header",
            "entityType", 
            "aggressiveInlining",
            "unsafe",
            "namespace",
            "imports",
            "tags",
            "values"
        )
        
        private val NON_TRIGGER_FIELDS = setOf(
            "directory",
            "className",
            "solution"
        )
    }
    
    fun shouldRegenerate(atomicFile: AtomicFile, previousContent: String?, currentContent: String?): Boolean {
        if (previousContent == null || currentContent == null) {
            LOG.info("AtomicFileChangeAnalyzer: Content is null, triggering regeneration")
            return true
        }
        
        if (previousContent == currentContent) {
            LOG.info("AtomicFileChangeAnalyzer: No changes detected")
            return false
        }
        
        try {
            val previousFields = parseContent(previousContent)
            val currentFields = parseContent(currentContent)
            
            for (field in REGENERATION_TRIGGER_FIELDS) {
                val previousValue = previousFields[field]
                val currentValue = currentFields[field]
                
                if (previousValue != currentValue) {
                    LOG.info("AtomicFileChangeAnalyzer: Field '$field' changed from '$previousValue' to '$currentValue' - regeneration needed")
                    return true
                }
            }
            
            
            for (field in NON_TRIGGER_FIELDS) {
                val previousValue = previousFields[field]
                val currentValue = currentFields[field]
                
                if (previousValue != currentValue) {
                    LOG.info("AtomicFileChangeAnalyzer: Field '$field' changed from '$previousValue' to '$currentValue' - no regeneration needed")
                }
            }
            
            LOG.info("AtomicFileChangeAnalyzer: Only non-trigger fields changed, skipping regeneration")
            return false
            
        } catch (e: Exception) {
            LOG.error("AtomicFileChangeAnalyzer: Error analyzing changes, triggering regeneration", e)
            return true
        }
    }
    
    private fun parseContent(content: String): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        val lines = content.split("\n")
        var currentSection = ""
        val sectionContent = mutableListOf<String>()
        
        for (line in lines) {
            val trimmed = line.trim()
            
            
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue
            }
            
            
            if (trimmed.endsWith(":") && !trimmed.contains(": ")) {
                
                if (currentSection.isNotEmpty() && sectionContent.isNotEmpty()) {
                    fields[currentSection] = sectionContent.joinToString("\n")
                    sectionContent.clear()
                }
                currentSection = trimmed.dropLast(1)
            }
            
            else if (trimmed.contains(": ") && currentSection.isEmpty()) {
                val colonIndex = trimmed.indexOf(": ")
                val key = trimmed.substring(0, colonIndex).trim()
                val value = trimmed.substring(colonIndex + 2).trim()
                fields[key] = value
            }
            
            else if (currentSection.isNotEmpty()) {
                sectionContent.add(trimmed)
            }
        }
        
        
        if (currentSection.isNotEmpty() && sectionContent.isNotEmpty()) {
            fields[currentSection] = sectionContent.joinToString("\n")
        }
        
        return fields
    }
    
    private val contentCache = mutableMapOf<String, String>()
    
    fun getPreviousContent(filePath: String): String? {
        return contentCache[filePath]
    }
    
    fun updateContent(filePath: String, content: String) {
        contentCache[filePath] = content
    }
}