package com.jetbrains.rider.plugins.atomic.fileTemplates

import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory
import com.jetbrains.rider.plugins.atomic.language.AtomicIcons

class AtomicFileTemplateProvider : FileTemplateGroupDescriptorFactory {
    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        val group = FileTemplateGroupDescriptor("Atomic", AtomicIcons.FILE)
        group.addTemplate(FileTemplateDescriptor("Atomic File.atomic", AtomicIcons.FILE))
        return group
    }
}