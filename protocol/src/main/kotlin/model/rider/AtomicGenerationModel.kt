package model.rider

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rider.model.nova.ide.SolutionModel

@Suppress("unused")
object AtomicGenerationModel : Ext(SolutionModel.Solution) {
    init {
        // Устанавливаем пространства имен для сгенерированного кода
        setting(CSharp50Generator.Namespace, "ReSharperPlugin.AtomicPlugin.Model")
        setting(Kotlin11Generator.Namespace, "com.jetbrains.rider.plugins.atomic.model")

        // Структуры данных
        
        // Данные для одного объявления значения
        val AtomicValueData = structdef("AtomicValueData") {
            field("name", string)
            field("type", string)
        }
        
        // Заголовочное свойство для пар ключ-значение
        val HeaderProperty = structdef("HeaderProperty") {
            field("key", string)
            field("value", string)
        }
        
        // Полные данные атомарного файла
        val AtomicFileData = structdef("AtomicFileData") {
            field("headerProperties", array(HeaderProperty))
            field("imports", array(string))
            field("tags", array(string))
            field("values", array(AtomicValueData))
            field("filePath", string)
        }
        
        // Структуры для автодополнения типов
        val TypeKind = enum("TypeKind") {
            +"Class"
            +"Interface"
            +"Struct"
            +"Enum"
            +"Delegate"
        }
        
        val TypeCompletionItem = structdef("TypeCompletionItem") {
            field("typeName", string)
            field("fullTypeName", string)
            field("namespace", string)
            field("assemblyName", string)
            field("isGeneric", bool)
            field("typeKind", TypeKind)
        }
        
        val TypeCompletionRequest = structdef("TypeCompletionRequest") {
            field("prefix", string)
            field("imports", array(string))
            field("projectPath", string)
            field("namespaceFilter", string.nullable)
        }
        
        val TypeCompletionResponse = structdef("TypeCompletionResponse") {
            field("items", array(TypeCompletionItem))
        }
        
        // Структуры для проверки типа
        val TypeValidationRequest = structdef("TypeValidationRequest") {
            field("typeName", string)
            field("imports", array(string))
            field("projectPath", string)
        }
        
        val TypeValidationResponse = structdef("TypeValidationResponse") {
            field("isValid", bool)
            field("fullTypeName", string.nullable)
            field("suggestedImport", string.nullable)
            field("suggestedImports", array(string))
            field("isAmbiguous", bool)
            field("ambiguousNamespaces", array(string))
        }
        
        // Структуры для автодополнения пространств имен
        val NamespaceCompletionRequest = structdef("NamespaceCompletionRequest") {
            field("prefix", string)
            field("projectPath", string)
        }
        
        val NamespaceCompletionResponse = structdef("NamespaceCompletionResponse") {
            field("namespaces", array(string))
        }
        
        // Структуры для проверки пространства имен
        val NamespaceValidationRequest = structdef("NamespaceValidationRequest") {
            field("namespace", string)
            field("projectPath", string)
        }
        
        val NamespaceValidationResponse = structdef("NamespaceValidationResponse") {
            field("isValid", bool)
            field("hasTypes", bool)
        }
        
        // Структуры для поиска использований
        val MethodUsageLocation = structdef("MethodUsageLocation") {
            field("filePath", string)
            field("line", int)
            field("column", int)
            field("methodName", string)
            field("usageText", string)
        }
        
        val FindMethodUsagesRequest = structdef("FindMethodUsagesRequest") {
            field("valueName", string)
            field("methodNames", array(string))
            field("projectPath", string)
            field("generatedFilePath", string)
        }
        
        val FindMethodUsagesResponse = structdef("FindMethodUsagesResponse") {
            field("usages", array(MethodUsageLocation))
        }
        
        val FindTagUsagesRequest = structdef("FindTagUsagesRequest") {
            field("tagName", string)
            field("methodNames", array(string))
            field("projectPath", string)
            field("generatedFilePath", string)
        }
        
        val FindTagUsagesResponse = structdef("FindTagUsagesResponse") {
            field("usages", array(MethodUsageLocation))
        }
        
        val RenameValueRequest = structdef("RenameValueRequest") {
            field("atomicFilePath", string)
            field("oldName", string)
            field("newName", string)
            field("projectPath", string)
        }
        
        val RenameTagRequest = structdef("RenameTagRequest") {
            field("atomicFilePath", string)
            field("oldName", string)
            field("newName", string)
            field("projectPath", string)
        }
        
        val RenameResponse = structdef("RenameResponse") {
            field("success", bool)
            field("regeneratedFilePath", string.nullable)
            field("updatedUsages", array(MethodUsageLocation))
            field("errorMessage", string.nullable)
        }
        
        // Вызовы RPC
        
        // Сгенерировать API из данных атомарного файла
        call("generateApi", AtomicFileData, string).async
        
        // Получить автодополнения типов
        call("getTypeCompletions", TypeCompletionRequest, TypeCompletionResponse).async
        
        // Проверить тип
        call("validateType", TypeValidationRequest, TypeValidationResponse).async
        
        // Получить доступные проекты C# в решении
        call("getAvailableProjects", void, array(string)).async
        
        // Получить автодополнения пространств имен
        call("getNamespaceCompletions", NamespaceCompletionRequest, NamespaceCompletionResponse).async
        
        // Проверить пространство имен
        call("validateNamespace", NamespaceValidationRequest, NamespaceValidationResponse).async
        
        // Найти использования метода
        call("findMethodUsages", FindMethodUsagesRequest, FindMethodUsagesResponse).async
        
        // Найти использования тега
        call("findTagUsages", FindTagUsagesRequest, FindTagUsagesResponse).async
        
        // Переименовать значение
        call("renameValue", RenameValueRequest, RenameResponse).async
        
        // Переименовать тег
        call("renameTag", RenameTagRequest, RenameResponse).async
        
        // Добавить атомарный файл в проект
        call("addAtomicFileToProject", string, bool).async
        
        // События для обновления статуса
        sink("generationStatus", string).async
    }
}
