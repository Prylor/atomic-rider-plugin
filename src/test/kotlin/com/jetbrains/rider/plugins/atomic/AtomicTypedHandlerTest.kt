package com.jetbrains.rider.plugins.atomic

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.rider.plugins.atomic.language.AtomicTypedHandler
import com.jetbrains.rider.plugins.atomic.psi.AtomicFile
import org.junit.Test
import org.mockito.Mockito.*

class AtomicTypedHandlerTest : BasePlatformTestCase() {
    
    private lateinit var handler: AtomicTypedHandler
    
    override fun setUp() {
        super.setUp()
        handler = AtomicTypedHandler()
    }
    
    @Test
    fun testAutoPopupTriggersForLetterInValuesSection() {
        val fileContent = """
            header: "Test"
            values:
            - health: 
        """.trimIndent()
        
        val psiFile = myFixture.configureByText("test.atomic", fileContent) as AtomicFile
        val editor = myFixture.editor
        
        editor.caretModel.moveToOffset(fileContent.indexOf("health:") + 8)
        
        val result = handler.checkAutoPopup('i', project, editor, psiFile)
        
        assertEquals(TypedHandlerDelegate.Result.STOP, result)
    }
    
    @Test
    fun testAutoPopupTriggersForDotInNamespace() {
        val fileContent = """
            namespace: System
        """.trimIndent()
        
        val psiFile = myFixture.configureByText("test.atomic", fileContent) as AtomicFile
        val editor = myFixture.editor
        
        editor.caretModel.moveToOffset(fileContent.indexOf("System") + 6)
        
        val result = handler.checkAutoPopup('.', project, editor, psiFile)
        
        assertEquals(TypedHandlerDelegate.Result.STOP, result)
    }
    
    @Test
    fun testAutoPopupTriggersForSlashInDirectory() {
        val fileContent = """
            directory: Assets
        """.trimIndent()
        
        val psiFile = myFixture.configureByText("test.atomic", fileContent) as AtomicFile
        val editor = myFixture.editor
        
        editor.caretModel.moveToOffset(fileContent.indexOf("Assets") + 6)
        
        val result = handler.checkAutoPopup('/', project, editor, psiFile)
        
        assertEquals(TypedHandlerDelegate.Result.STOP, result)
    }
    
    @Test
    fun testAutoPopupDoesNotTriggerOutsideAtomicFile() {
        val psiFile = myFixture.configureByText("test.txt", "some content")
        val editor = myFixture.editor
        
        val result = handler.checkAutoPopup('a', project, editor, psiFile)
        
        assertEquals(TypedHandlerDelegate.Result.CONTINUE, result)
    }
    
    @Test
    fun testAutoPopupForGenericTypes() {
        val fileContent = """
            values:
            - items: List
        """.trimIndent()
        
        val psiFile = myFixture.configureByText("test.atomic", fileContent) as AtomicFile
        val editor = myFixture.editor
        
        editor.caretModel.moveToOffset(fileContent.indexOf("List") + 4)
        
        val result = handler.checkAutoPopup('<', project, editor, psiFile)
        
        assertEquals(TypedHandlerDelegate.Result.STOP, result)
    }
}