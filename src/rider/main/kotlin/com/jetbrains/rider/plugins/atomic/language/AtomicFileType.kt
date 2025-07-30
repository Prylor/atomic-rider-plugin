package com.jetbrains.rider.plugins.atomic.language

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object AtomicFileType : LanguageFileType(AtomicLanguage) {
    override fun getName(): String = "Atomic File"
    
    override fun getDescription(): String = "Atomic entity API configuration file"
    
    override fun getDefaultExtension(): String = "atomic"
    
    override fun getIcon(): Icon = AtomicIcons.FILE
}