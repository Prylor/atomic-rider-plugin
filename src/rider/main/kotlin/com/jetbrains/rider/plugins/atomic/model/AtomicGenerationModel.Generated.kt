@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package com.jetbrains.rider.plugins.atomic.model

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.*

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.*
import kotlin.time.Duration
import kotlin.reflect.KClass
import kotlin.jvm.JvmStatic



/**
 * #### Generated from [AtomicGenerationModel.kt:10]
 */
class AtomicGenerationModel private constructor(
    private val _generateApi: RdCall<AtomicFileData, String>,
    private val _getTypeCompletions: RdCall<TypeCompletionRequest, TypeCompletionResponse>,
    private val _validateType: RdCall<TypeValidationRequest, TypeValidationResponse>,
    private val _getAvailableProjects: RdCall<Unit, Array<String>>,
    private val _getNamespaceCompletions: RdCall<NamespaceCompletionRequest, NamespaceCompletionResponse>,
    private val _validateNamespace: RdCall<NamespaceValidationRequest, NamespaceValidationResponse>,
    private val _findMethodUsages: RdCall<FindMethodUsagesRequest, FindMethodUsagesResponse>,
    private val _findTagUsages: RdCall<FindTagUsagesRequest, FindTagUsagesResponse>,
    private val _renameValue: RdCall<RenameValueRequest, RenameResponse>,
    private val _renameTag: RdCall<RenameTagRequest, RenameResponse>,
    private val _addAtomicFileToProject: RdCall<String, Boolean>,
    private val _generationStatus: RdSignal<String>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            val classLoader = javaClass.classLoader
            serializers.register(LazyCompanionMarshaller(RdId(-5798417293315207619), classLoader, "com.jetbrains.rider.plugins.atomic.model.AtomicValueData"))
            serializers.register(LazyCompanionMarshaller(RdId(7413358007655593589), classLoader, "com.jetbrains.rider.plugins.atomic.model.HeaderProperty"))
            serializers.register(LazyCompanionMarshaller(RdId(-3757383714910189180), classLoader, "com.jetbrains.rider.plugins.atomic.model.AtomicFileData"))
            serializers.register(LazyCompanionMarshaller(RdId(18626679321377), classLoader, "com.jetbrains.rider.plugins.atomic.model.TypeKind"))
            serializers.register(LazyCompanionMarshaller(RdId(8977287870100910172), classLoader, "com.jetbrains.rider.plugins.atomic.model.TypeCompletionItem"))
            serializers.register(LazyCompanionMarshaller(RdId(1487357542705122470), classLoader, "com.jetbrains.rider.plugins.atomic.model.TypeCompletionRequest"))
            serializers.register(LazyCompanionMarshaller(RdId(9214595676492627210), classLoader, "com.jetbrains.rider.plugins.atomic.model.TypeCompletionResponse"))
            serializers.register(LazyCompanionMarshaller(RdId(-5264401009261253271), classLoader, "com.jetbrains.rider.plugins.atomic.model.TypeValidationRequest"))
            serializers.register(LazyCompanionMarshaller(RdId(2824265376340047015), classLoader, "com.jetbrains.rider.plugins.atomic.model.TypeValidationResponse"))
            serializers.register(LazyCompanionMarshaller(RdId(-5399402274201952917), classLoader, "com.jetbrains.rider.plugins.atomic.model.NamespaceCompletionRequest"))
            serializers.register(LazyCompanionMarshaller(RdId(-1360773836821642011), classLoader, "com.jetbrains.rider.plugins.atomic.model.NamespaceCompletionResponse"))
            serializers.register(LazyCompanionMarshaller(RdId(6295583247541222958), classLoader, "com.jetbrains.rider.plugins.atomic.model.NamespaceValidationRequest"))
            serializers.register(LazyCompanionMarshaller(RdId(-7751104136974222206), classLoader, "com.jetbrains.rider.plugins.atomic.model.NamespaceValidationResponse"))
            serializers.register(LazyCompanionMarshaller(RdId(-7495289087906179582), classLoader, "com.jetbrains.rider.plugins.atomic.model.MethodUsageLocation"))
            serializers.register(LazyCompanionMarshaller(RdId(-970532926612069424), classLoader, "com.jetbrains.rider.plugins.atomic.model.FindMethodUsagesRequest"))
            serializers.register(LazyCompanionMarshaller(RdId(6806967422497884960), classLoader, "com.jetbrains.rider.plugins.atomic.model.FindMethodUsagesResponse"))
            serializers.register(LazyCompanionMarshaller(RdId(6338498526375587407), classLoader, "com.jetbrains.rider.plugins.atomic.model.FindTagUsagesRequest"))
            serializers.register(LazyCompanionMarshaller(RdId(-6420730493108924287), classLoader, "com.jetbrains.rider.plugins.atomic.model.FindTagUsagesResponse"))
            serializers.register(LazyCompanionMarshaller(RdId(1630434537249653679), classLoader, "com.jetbrains.rider.plugins.atomic.model.RenameValueRequest"))
            serializers.register(LazyCompanionMarshaller(RdId(-2954442384796203610), classLoader, "com.jetbrains.rider.plugins.atomic.model.RenameTagRequest"))
            serializers.register(LazyCompanionMarshaller(RdId(-6337544262039093102), classLoader, "com.jetbrains.rider.plugins.atomic.model.RenameResponse"))
        }
        
        
        
        
        private val __StringArraySerializer = FrameworkMarshallers.String.array()
        
        const val serializationHash = 8091421013007210735L
        
    }
    override val serializersOwner: ISerializersOwner get() = AtomicGenerationModel
    override val serializationHash: Long get() = AtomicGenerationModel.serializationHash
    
    //fields
    val generateApi: IRdCall<AtomicFileData, String> get() = _generateApi
    val getTypeCompletions: IRdCall<TypeCompletionRequest, TypeCompletionResponse> get() = _getTypeCompletions
    val validateType: IRdCall<TypeValidationRequest, TypeValidationResponse> get() = _validateType
    val getAvailableProjects: IRdCall<Unit, Array<String>> get() = _getAvailableProjects
    val getNamespaceCompletions: IRdCall<NamespaceCompletionRequest, NamespaceCompletionResponse> get() = _getNamespaceCompletions
    val validateNamespace: IRdCall<NamespaceValidationRequest, NamespaceValidationResponse> get() = _validateNamespace
    val findMethodUsages: IRdCall<FindMethodUsagesRequest, FindMethodUsagesResponse> get() = _findMethodUsages
    val findTagUsages: IRdCall<FindTagUsagesRequest, FindTagUsagesResponse> get() = _findTagUsages
    val renameValue: IRdCall<RenameValueRequest, RenameResponse> get() = _renameValue
    val renameTag: IRdCall<RenameTagRequest, RenameResponse> get() = _renameTag
    val addAtomicFileToProject: IRdCall<String, Boolean> get() = _addAtomicFileToProject
    val generationStatus: IAsyncSource<String> get() = _generationStatus
    //methods
    //initializer
    init {
        _generateApi.async = true
        _getTypeCompletions.async = true
        _validateType.async = true
        _getAvailableProjects.async = true
        _getNamespaceCompletions.async = true
        _validateNamespace.async = true
        _findMethodUsages.async = true
        _findTagUsages.async = true
        _renameValue.async = true
        _renameTag.async = true
        _addAtomicFileToProject.async = true
        _generationStatus.async = true
    }
    
    init {
        bindableChildren.add("generateApi" to _generateApi)
        bindableChildren.add("getTypeCompletions" to _getTypeCompletions)
        bindableChildren.add("validateType" to _validateType)
        bindableChildren.add("getAvailableProjects" to _getAvailableProjects)
        bindableChildren.add("getNamespaceCompletions" to _getNamespaceCompletions)
        bindableChildren.add("validateNamespace" to _validateNamespace)
        bindableChildren.add("findMethodUsages" to _findMethodUsages)
        bindableChildren.add("findTagUsages" to _findTagUsages)
        bindableChildren.add("renameValue" to _renameValue)
        bindableChildren.add("renameTag" to _renameTag)
        bindableChildren.add("addAtomicFileToProject" to _addAtomicFileToProject)
        bindableChildren.add("generationStatus" to _generationStatus)
    }
    
    //secondary constructor
    internal constructor(
    ) : this(
        RdCall<AtomicFileData, String>(AtomicFileData, FrameworkMarshallers.String),
        RdCall<TypeCompletionRequest, TypeCompletionResponse>(TypeCompletionRequest, TypeCompletionResponse),
        RdCall<TypeValidationRequest, TypeValidationResponse>(TypeValidationRequest, TypeValidationResponse),
        RdCall<Unit, Array<String>>(FrameworkMarshallers.Void, __StringArraySerializer),
        RdCall<NamespaceCompletionRequest, NamespaceCompletionResponse>(NamespaceCompletionRequest, NamespaceCompletionResponse),
        RdCall<NamespaceValidationRequest, NamespaceValidationResponse>(NamespaceValidationRequest, NamespaceValidationResponse),
        RdCall<FindMethodUsagesRequest, FindMethodUsagesResponse>(FindMethodUsagesRequest, FindMethodUsagesResponse),
        RdCall<FindTagUsagesRequest, FindTagUsagesResponse>(FindTagUsagesRequest, FindTagUsagesResponse),
        RdCall<RenameValueRequest, RenameResponse>(RenameValueRequest, RenameResponse),
        RdCall<RenameTagRequest, RenameResponse>(RenameTagRequest, RenameResponse),
        RdCall<String, Boolean>(FrameworkMarshallers.String, FrameworkMarshallers.Bool),
        RdSignal<String>(FrameworkMarshallers.String)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("AtomicGenerationModel (")
        printer.indent {
            print("generateApi = "); _generateApi.print(printer); println()
            print("getTypeCompletions = "); _getTypeCompletions.print(printer); println()
            print("validateType = "); _validateType.print(printer); println()
            print("getAvailableProjects = "); _getAvailableProjects.print(printer); println()
            print("getNamespaceCompletions = "); _getNamespaceCompletions.print(printer); println()
            print("validateNamespace = "); _validateNamespace.print(printer); println()
            print("findMethodUsages = "); _findMethodUsages.print(printer); println()
            print("findTagUsages = "); _findTagUsages.print(printer); println()
            print("renameValue = "); _renameValue.print(printer); println()
            print("renameTag = "); _renameTag.print(printer); println()
            print("addAtomicFileToProject = "); _addAtomicFileToProject.print(printer); println()
            print("generationStatus = "); _generationStatus.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): AtomicGenerationModel   {
        return AtomicGenerationModel(
            _generateApi.deepClonePolymorphic(),
            _getTypeCompletions.deepClonePolymorphic(),
            _validateType.deepClonePolymorphic(),
            _getAvailableProjects.deepClonePolymorphic(),
            _getNamespaceCompletions.deepClonePolymorphic(),
            _validateNamespace.deepClonePolymorphic(),
            _findMethodUsages.deepClonePolymorphic(),
            _findTagUsages.deepClonePolymorphic(),
            _renameValue.deepClonePolymorphic(),
            _renameTag.deepClonePolymorphic(),
            _addAtomicFileToProject.deepClonePolymorphic(),
            _generationStatus.deepClonePolymorphic()
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default
}
val com.jetbrains.rd.ide.model.Solution.atomicGenerationModel get() = getOrCreateExtension("atomicGenerationModel", ::AtomicGenerationModel)



/**
 * #### Generated from [AtomicGenerationModel.kt:31]
 */
data class AtomicFileData (
    val headerProperties: Array<HeaderProperty>,
    val imports: Array<String>,
    val tags: Array<String>,
    val values: Array<AtomicValueData>,
    val filePath: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<AtomicFileData> {
        override val _type: KClass<AtomicFileData> = AtomicFileData::class
        override val id: RdId get() = RdId(-3757383714910189180)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): AtomicFileData  {
            val headerProperties = buffer.readArray {HeaderProperty.read(ctx, buffer)}
            val imports = buffer.readArray {buffer.readString()}
            val tags = buffer.readArray {buffer.readString()}
            val values = buffer.readArray {AtomicValueData.read(ctx, buffer)}
            val filePath = buffer.readString()
            return AtomicFileData(headerProperties, imports, tags, values, filePath)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: AtomicFileData)  {
            buffer.writeArray(value.headerProperties) { HeaderProperty.write(ctx, buffer, it) }
            buffer.writeArray(value.imports) { buffer.writeString(it) }
            buffer.writeArray(value.tags) { buffer.writeString(it) }
            buffer.writeArray(value.values) { AtomicValueData.write(ctx, buffer, it) }
            buffer.writeString(value.filePath)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as AtomicFileData
        
        if (!(headerProperties contentDeepEquals other.headerProperties)) return false
        if (!(imports contentDeepEquals other.imports)) return false
        if (!(tags contentDeepEquals other.tags)) return false
        if (!(values contentDeepEquals other.values)) return false
        if (filePath != other.filePath) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + headerProperties.contentDeepHashCode()
        __r = __r*31 + imports.contentDeepHashCode()
        __r = __r*31 + tags.contentDeepHashCode()
        __r = __r*31 + values.contentDeepHashCode()
        __r = __r*31 + filePath.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("AtomicFileData (")
        printer.indent {
            print("headerProperties = "); headerProperties.print(printer); println()
            print("imports = "); imports.print(printer); println()
            print("tags = "); tags.print(printer); println()
            print("values = "); values.print(printer); println()
            print("filePath = "); filePath.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:19]
 */
data class AtomicValueData (
    val name: String,
    val type: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<AtomicValueData> {
        override val _type: KClass<AtomicValueData> = AtomicValueData::class
        override val id: RdId get() = RdId(-5798417293315207619)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): AtomicValueData  {
            val name = buffer.readString()
            val type = buffer.readString()
            return AtomicValueData(name, type)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: AtomicValueData)  {
            buffer.writeString(value.name)
            buffer.writeString(value.type)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as AtomicValueData
        
        if (name != other.name) return false
        if (type != other.type) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + name.hashCode()
        __r = __r*31 + type.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("AtomicValueData (")
        printer.indent {
            print("name = "); name.print(printer); println()
            print("type = "); type.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:114]
 */
data class FindMethodUsagesRequest (
    val valueName: String,
    val methodNames: Array<String>,
    val projectPath: String,
    val generatedFilePath: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<FindMethodUsagesRequest> {
        override val _type: KClass<FindMethodUsagesRequest> = FindMethodUsagesRequest::class
        override val id: RdId get() = RdId(-970532926612069424)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): FindMethodUsagesRequest  {
            val valueName = buffer.readString()
            val methodNames = buffer.readArray {buffer.readString()}
            val projectPath = buffer.readString()
            val generatedFilePath = buffer.readString()
            return FindMethodUsagesRequest(valueName, methodNames, projectPath, generatedFilePath)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: FindMethodUsagesRequest)  {
            buffer.writeString(value.valueName)
            buffer.writeArray(value.methodNames) { buffer.writeString(it) }
            buffer.writeString(value.projectPath)
            buffer.writeString(value.generatedFilePath)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as FindMethodUsagesRequest
        
        if (valueName != other.valueName) return false
        if (!(methodNames contentDeepEquals other.methodNames)) return false
        if (projectPath != other.projectPath) return false
        if (generatedFilePath != other.generatedFilePath) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + valueName.hashCode()
        __r = __r*31 + methodNames.contentDeepHashCode()
        __r = __r*31 + projectPath.hashCode()
        __r = __r*31 + generatedFilePath.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("FindMethodUsagesRequest (")
        printer.indent {
            print("valueName = "); valueName.print(printer); println()
            print("methodNames = "); methodNames.print(printer); println()
            print("projectPath = "); projectPath.print(printer); println()
            print("generatedFilePath = "); generatedFilePath.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:121]
 */
data class FindMethodUsagesResponse (
    val usages: Array<MethodUsageLocation>
) : IPrintable {
    //companion
    
    companion object : IMarshaller<FindMethodUsagesResponse> {
        override val _type: KClass<FindMethodUsagesResponse> = FindMethodUsagesResponse::class
        override val id: RdId get() = RdId(6806967422497884960)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): FindMethodUsagesResponse  {
            val usages = buffer.readArray {MethodUsageLocation.read(ctx, buffer)}
            return FindMethodUsagesResponse(usages)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: FindMethodUsagesResponse)  {
            buffer.writeArray(value.usages) { MethodUsageLocation.write(ctx, buffer, it) }
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as FindMethodUsagesResponse
        
        if (!(usages contentDeepEquals other.usages)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + usages.contentDeepHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("FindMethodUsagesResponse (")
        printer.indent {
            print("usages = "); usages.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:125]
 */
data class FindTagUsagesRequest (
    val tagName: String,
    val methodNames: Array<String>,
    val projectPath: String,
    val generatedFilePath: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<FindTagUsagesRequest> {
        override val _type: KClass<FindTagUsagesRequest> = FindTagUsagesRequest::class
        override val id: RdId get() = RdId(6338498526375587407)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): FindTagUsagesRequest  {
            val tagName = buffer.readString()
            val methodNames = buffer.readArray {buffer.readString()}
            val projectPath = buffer.readString()
            val generatedFilePath = buffer.readString()
            return FindTagUsagesRequest(tagName, methodNames, projectPath, generatedFilePath)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: FindTagUsagesRequest)  {
            buffer.writeString(value.tagName)
            buffer.writeArray(value.methodNames) { buffer.writeString(it) }
            buffer.writeString(value.projectPath)
            buffer.writeString(value.generatedFilePath)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as FindTagUsagesRequest
        
        if (tagName != other.tagName) return false
        if (!(methodNames contentDeepEquals other.methodNames)) return false
        if (projectPath != other.projectPath) return false
        if (generatedFilePath != other.generatedFilePath) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + tagName.hashCode()
        __r = __r*31 + methodNames.contentDeepHashCode()
        __r = __r*31 + projectPath.hashCode()
        __r = __r*31 + generatedFilePath.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("FindTagUsagesRequest (")
        printer.indent {
            print("tagName = "); tagName.print(printer); println()
            print("methodNames = "); methodNames.print(printer); println()
            print("projectPath = "); projectPath.print(printer); println()
            print("generatedFilePath = "); generatedFilePath.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:132]
 */
data class FindTagUsagesResponse (
    val usages: Array<MethodUsageLocation>
) : IPrintable {
    //companion
    
    companion object : IMarshaller<FindTagUsagesResponse> {
        override val _type: KClass<FindTagUsagesResponse> = FindTagUsagesResponse::class
        override val id: RdId get() = RdId(-6420730493108924287)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): FindTagUsagesResponse  {
            val usages = buffer.readArray {MethodUsageLocation.read(ctx, buffer)}
            return FindTagUsagesResponse(usages)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: FindTagUsagesResponse)  {
            buffer.writeArray(value.usages) { MethodUsageLocation.write(ctx, buffer, it) }
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as FindTagUsagesResponse
        
        if (!(usages contentDeepEquals other.usages)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + usages.contentDeepHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("FindTagUsagesResponse (")
        printer.indent {
            print("usages = "); usages.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:25]
 */
data class HeaderProperty (
    val key: String,
    val value: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<HeaderProperty> {
        override val _type: KClass<HeaderProperty> = HeaderProperty::class
        override val id: RdId get() = RdId(7413358007655593589)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): HeaderProperty  {
            val key = buffer.readString()
            val value = buffer.readString()
            return HeaderProperty(key, value)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: HeaderProperty)  {
            buffer.writeString(value.key)
            buffer.writeString(value.value)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as HeaderProperty
        
        if (key != other.key) return false
        if (value != other.value) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + key.hashCode()
        __r = __r*31 + value.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("HeaderProperty (")
        printer.indent {
            print("key = "); key.print(printer); println()
            print("value = "); value.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:106]
 */
data class MethodUsageLocation (
    val filePath: String,
    val line: Int,
    val column: Int,
    val methodName: String,
    val usageText: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<MethodUsageLocation> {
        override val _type: KClass<MethodUsageLocation> = MethodUsageLocation::class
        override val id: RdId get() = RdId(-7495289087906179582)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): MethodUsageLocation  {
            val filePath = buffer.readString()
            val line = buffer.readInt()
            val column = buffer.readInt()
            val methodName = buffer.readString()
            val usageText = buffer.readString()
            return MethodUsageLocation(filePath, line, column, methodName, usageText)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: MethodUsageLocation)  {
            buffer.writeString(value.filePath)
            buffer.writeInt(value.line)
            buffer.writeInt(value.column)
            buffer.writeString(value.methodName)
            buffer.writeString(value.usageText)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as MethodUsageLocation
        
        if (filePath != other.filePath) return false
        if (line != other.line) return false
        if (column != other.column) return false
        if (methodName != other.methodName) return false
        if (usageText != other.usageText) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + filePath.hashCode()
        __r = __r*31 + line.hashCode()
        __r = __r*31 + column.hashCode()
        __r = __r*31 + methodName.hashCode()
        __r = __r*31 + usageText.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("MethodUsageLocation (")
        printer.indent {
            print("filePath = "); filePath.print(printer); println()
            print("line = "); line.print(printer); println()
            print("column = "); column.print(printer); println()
            print("methodName = "); methodName.print(printer); println()
            print("usageText = "); usageText.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:85]
 */
data class NamespaceCompletionRequest (
    val prefix: String,
    val projectPath: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<NamespaceCompletionRequest> {
        override val _type: KClass<NamespaceCompletionRequest> = NamespaceCompletionRequest::class
        override val id: RdId get() = RdId(-5399402274201952917)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): NamespaceCompletionRequest  {
            val prefix = buffer.readString()
            val projectPath = buffer.readString()
            return NamespaceCompletionRequest(prefix, projectPath)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: NamespaceCompletionRequest)  {
            buffer.writeString(value.prefix)
            buffer.writeString(value.projectPath)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as NamespaceCompletionRequest
        
        if (prefix != other.prefix) return false
        if (projectPath != other.projectPath) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + prefix.hashCode()
        __r = __r*31 + projectPath.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("NamespaceCompletionRequest (")
        printer.indent {
            print("prefix = "); prefix.print(printer); println()
            print("projectPath = "); projectPath.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:90]
 */
data class NamespaceCompletionResponse (
    val namespaces: Array<String>
) : IPrintable {
    //companion
    
    companion object : IMarshaller<NamespaceCompletionResponse> {
        override val _type: KClass<NamespaceCompletionResponse> = NamespaceCompletionResponse::class
        override val id: RdId get() = RdId(-1360773836821642011)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): NamespaceCompletionResponse  {
            val namespaces = buffer.readArray {buffer.readString()}
            return NamespaceCompletionResponse(namespaces)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: NamespaceCompletionResponse)  {
            buffer.writeArray(value.namespaces) { buffer.writeString(it) }
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as NamespaceCompletionResponse
        
        if (!(namespaces contentDeepEquals other.namespaces)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + namespaces.contentDeepHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("NamespaceCompletionResponse (")
        printer.indent {
            print("namespaces = "); namespaces.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:95]
 */
data class NamespaceValidationRequest (
    val namespace: String,
    val projectPath: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<NamespaceValidationRequest> {
        override val _type: KClass<NamespaceValidationRequest> = NamespaceValidationRequest::class
        override val id: RdId get() = RdId(6295583247541222958)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): NamespaceValidationRequest  {
            val namespace = buffer.readString()
            val projectPath = buffer.readString()
            return NamespaceValidationRequest(namespace, projectPath)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: NamespaceValidationRequest)  {
            buffer.writeString(value.namespace)
            buffer.writeString(value.projectPath)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as NamespaceValidationRequest
        
        if (namespace != other.namespace) return false
        if (projectPath != other.projectPath) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + namespace.hashCode()
        __r = __r*31 + projectPath.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("NamespaceValidationRequest (")
        printer.indent {
            print("namespace = "); namespace.print(printer); println()
            print("projectPath = "); projectPath.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:100]
 */
data class NamespaceValidationResponse (
    val isValid: Boolean,
    val hasTypes: Boolean
) : IPrintable {
    //companion
    
    companion object : IMarshaller<NamespaceValidationResponse> {
        override val _type: KClass<NamespaceValidationResponse> = NamespaceValidationResponse::class
        override val id: RdId get() = RdId(-7751104136974222206)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): NamespaceValidationResponse  {
            val isValid = buffer.readBool()
            val hasTypes = buffer.readBool()
            return NamespaceValidationResponse(isValid, hasTypes)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: NamespaceValidationResponse)  {
            buffer.writeBool(value.isValid)
            buffer.writeBool(value.hasTypes)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as NamespaceValidationResponse
        
        if (isValid != other.isValid) return false
        if (hasTypes != other.hasTypes) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + isValid.hashCode()
        __r = __r*31 + hasTypes.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("NamespaceValidationResponse (")
        printer.indent {
            print("isValid = "); isValid.print(printer); println()
            print("hasTypes = "); hasTypes.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:150]
 */
data class RenameResponse (
    val success: Boolean,
    val regeneratedFilePath: String?,
    val updatedUsages: Array<MethodUsageLocation>,
    val errorMessage: String?
) : IPrintable {
    //companion
    
    companion object : IMarshaller<RenameResponse> {
        override val _type: KClass<RenameResponse> = RenameResponse::class
        override val id: RdId get() = RdId(-6337544262039093102)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RenameResponse  {
            val success = buffer.readBool()
            val regeneratedFilePath = buffer.readNullable { buffer.readString() }
            val updatedUsages = buffer.readArray {MethodUsageLocation.read(ctx, buffer)}
            val errorMessage = buffer.readNullable { buffer.readString() }
            return RenameResponse(success, regeneratedFilePath, updatedUsages, errorMessage)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RenameResponse)  {
            buffer.writeBool(value.success)
            buffer.writeNullable(value.regeneratedFilePath) { buffer.writeString(it) }
            buffer.writeArray(value.updatedUsages) { MethodUsageLocation.write(ctx, buffer, it) }
            buffer.writeNullable(value.errorMessage) { buffer.writeString(it) }
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as RenameResponse
        
        if (success != other.success) return false
        if (regeneratedFilePath != other.regeneratedFilePath) return false
        if (!(updatedUsages contentDeepEquals other.updatedUsages)) return false
        if (errorMessage != other.errorMessage) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + success.hashCode()
        __r = __r*31 + if (regeneratedFilePath != null) regeneratedFilePath.hashCode() else 0
        __r = __r*31 + updatedUsages.contentDeepHashCode()
        __r = __r*31 + if (errorMessage != null) errorMessage.hashCode() else 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("RenameResponse (")
        printer.indent {
            print("success = "); success.print(printer); println()
            print("regeneratedFilePath = "); regeneratedFilePath.print(printer); println()
            print("updatedUsages = "); updatedUsages.print(printer); println()
            print("errorMessage = "); errorMessage.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:143]
 */
data class RenameTagRequest (
    val atomicFilePath: String,
    val oldName: String,
    val newName: String,
    val projectPath: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<RenameTagRequest> {
        override val _type: KClass<RenameTagRequest> = RenameTagRequest::class
        override val id: RdId get() = RdId(-2954442384796203610)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RenameTagRequest  {
            val atomicFilePath = buffer.readString()
            val oldName = buffer.readString()
            val newName = buffer.readString()
            val projectPath = buffer.readString()
            return RenameTagRequest(atomicFilePath, oldName, newName, projectPath)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RenameTagRequest)  {
            buffer.writeString(value.atomicFilePath)
            buffer.writeString(value.oldName)
            buffer.writeString(value.newName)
            buffer.writeString(value.projectPath)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as RenameTagRequest
        
        if (atomicFilePath != other.atomicFilePath) return false
        if (oldName != other.oldName) return false
        if (newName != other.newName) return false
        if (projectPath != other.projectPath) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + atomicFilePath.hashCode()
        __r = __r*31 + oldName.hashCode()
        __r = __r*31 + newName.hashCode()
        __r = __r*31 + projectPath.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("RenameTagRequest (")
        printer.indent {
            print("atomicFilePath = "); atomicFilePath.print(printer); println()
            print("oldName = "); oldName.print(printer); println()
            print("newName = "); newName.print(printer); println()
            print("projectPath = "); projectPath.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:136]
 */
data class RenameValueRequest (
    val atomicFilePath: String,
    val oldName: String,
    val newName: String,
    val projectPath: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<RenameValueRequest> {
        override val _type: KClass<RenameValueRequest> = RenameValueRequest::class
        override val id: RdId get() = RdId(1630434537249653679)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RenameValueRequest  {
            val atomicFilePath = buffer.readString()
            val oldName = buffer.readString()
            val newName = buffer.readString()
            val projectPath = buffer.readString()
            return RenameValueRequest(atomicFilePath, oldName, newName, projectPath)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RenameValueRequest)  {
            buffer.writeString(value.atomicFilePath)
            buffer.writeString(value.oldName)
            buffer.writeString(value.newName)
            buffer.writeString(value.projectPath)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as RenameValueRequest
        
        if (atomicFilePath != other.atomicFilePath) return false
        if (oldName != other.oldName) return false
        if (newName != other.newName) return false
        if (projectPath != other.projectPath) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + atomicFilePath.hashCode()
        __r = __r*31 + oldName.hashCode()
        __r = __r*31 + newName.hashCode()
        __r = __r*31 + projectPath.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("RenameValueRequest (")
        printer.indent {
            print("atomicFilePath = "); atomicFilePath.print(printer); println()
            print("oldName = "); oldName.print(printer); println()
            print("newName = "); newName.print(printer); println()
            print("projectPath = "); projectPath.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:48]
 */
data class TypeCompletionItem (
    val typeName: String,
    val fullTypeName: String,
    val namespace: String,
    val assemblyName: String,
    val isGeneric: Boolean,
    val typeKind: TypeKind
) : IPrintable {
    //companion
    
    companion object : IMarshaller<TypeCompletionItem> {
        override val _type: KClass<TypeCompletionItem> = TypeCompletionItem::class
        override val id: RdId get() = RdId(8977287870100910172)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): TypeCompletionItem  {
            val typeName = buffer.readString()
            val fullTypeName = buffer.readString()
            val namespace = buffer.readString()
            val assemblyName = buffer.readString()
            val isGeneric = buffer.readBool()
            val typeKind = buffer.readEnum<TypeKind>()
            return TypeCompletionItem(typeName, fullTypeName, namespace, assemblyName, isGeneric, typeKind)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: TypeCompletionItem)  {
            buffer.writeString(value.typeName)
            buffer.writeString(value.fullTypeName)
            buffer.writeString(value.namespace)
            buffer.writeString(value.assemblyName)
            buffer.writeBool(value.isGeneric)
            buffer.writeEnum(value.typeKind)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as TypeCompletionItem
        
        if (typeName != other.typeName) return false
        if (fullTypeName != other.fullTypeName) return false
        if (namespace != other.namespace) return false
        if (assemblyName != other.assemblyName) return false
        if (isGeneric != other.isGeneric) return false
        if (typeKind != other.typeKind) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + typeName.hashCode()
        __r = __r*31 + fullTypeName.hashCode()
        __r = __r*31 + namespace.hashCode()
        __r = __r*31 + assemblyName.hashCode()
        __r = __r*31 + isGeneric.hashCode()
        __r = __r*31 + typeKind.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("TypeCompletionItem (")
        printer.indent {
            print("typeName = "); typeName.print(printer); println()
            print("fullTypeName = "); fullTypeName.print(printer); println()
            print("namespace = "); namespace.print(printer); println()
            print("assemblyName = "); assemblyName.print(printer); println()
            print("isGeneric = "); isGeneric.print(printer); println()
            print("typeKind = "); typeKind.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:57]
 */
data class TypeCompletionRequest (
    val prefix: String,
    val imports: Array<String>,
    val projectPath: String,
    val namespaceFilter: String?
) : IPrintable {
    //companion
    
    companion object : IMarshaller<TypeCompletionRequest> {
        override val _type: KClass<TypeCompletionRequest> = TypeCompletionRequest::class
        override val id: RdId get() = RdId(1487357542705122470)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): TypeCompletionRequest  {
            val prefix = buffer.readString()
            val imports = buffer.readArray {buffer.readString()}
            val projectPath = buffer.readString()
            val namespaceFilter = buffer.readNullable { buffer.readString() }
            return TypeCompletionRequest(prefix, imports, projectPath, namespaceFilter)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: TypeCompletionRequest)  {
            buffer.writeString(value.prefix)
            buffer.writeArray(value.imports) { buffer.writeString(it) }
            buffer.writeString(value.projectPath)
            buffer.writeNullable(value.namespaceFilter) { buffer.writeString(it) }
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as TypeCompletionRequest
        
        if (prefix != other.prefix) return false
        if (!(imports contentDeepEquals other.imports)) return false
        if (projectPath != other.projectPath) return false
        if (namespaceFilter != other.namespaceFilter) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + prefix.hashCode()
        __r = __r*31 + imports.contentDeepHashCode()
        __r = __r*31 + projectPath.hashCode()
        __r = __r*31 + if (namespaceFilter != null) namespaceFilter.hashCode() else 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("TypeCompletionRequest (")
        printer.indent {
            print("prefix = "); prefix.print(printer); println()
            print("imports = "); imports.print(printer); println()
            print("projectPath = "); projectPath.print(printer); println()
            print("namespaceFilter = "); namespaceFilter.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:64]
 */
data class TypeCompletionResponse (
    val items: Array<TypeCompletionItem>
) : IPrintable {
    //companion
    
    companion object : IMarshaller<TypeCompletionResponse> {
        override val _type: KClass<TypeCompletionResponse> = TypeCompletionResponse::class
        override val id: RdId get() = RdId(9214595676492627210)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): TypeCompletionResponse  {
            val items = buffer.readArray {TypeCompletionItem.read(ctx, buffer)}
            return TypeCompletionResponse(items)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: TypeCompletionResponse)  {
            buffer.writeArray(value.items) { TypeCompletionItem.write(ctx, buffer, it) }
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as TypeCompletionResponse
        
        if (!(items contentDeepEquals other.items)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + items.contentDeepHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("TypeCompletionResponse (")
        printer.indent {
            print("items = "); items.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:40]
 */
enum class TypeKind {
    Class, 
    Interface, 
    Struct, 
    Enum, 
    Delegate;
    
    companion object : IMarshaller<TypeKind> {
        val marshaller = FrameworkMarshallers.enum<TypeKind>()
        
        
        override val _type: KClass<TypeKind> = TypeKind::class
        override val id: RdId get() = RdId(18626679321377)
        
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): TypeKind {
            return marshaller.read(ctx, buffer)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: TypeKind)  {
            marshaller.write(ctx, buffer, value)
        }
    }
}


/**
 * #### Generated from [AtomicGenerationModel.kt:69]
 */
data class TypeValidationRequest (
    val typeName: String,
    val imports: Array<String>,
    val projectPath: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<TypeValidationRequest> {
        override val _type: KClass<TypeValidationRequest> = TypeValidationRequest::class
        override val id: RdId get() = RdId(-5264401009261253271)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): TypeValidationRequest  {
            val typeName = buffer.readString()
            val imports = buffer.readArray {buffer.readString()}
            val projectPath = buffer.readString()
            return TypeValidationRequest(typeName, imports, projectPath)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: TypeValidationRequest)  {
            buffer.writeString(value.typeName)
            buffer.writeArray(value.imports) { buffer.writeString(it) }
            buffer.writeString(value.projectPath)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as TypeValidationRequest
        
        if (typeName != other.typeName) return false
        if (!(imports contentDeepEquals other.imports)) return false
        if (projectPath != other.projectPath) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + typeName.hashCode()
        __r = __r*31 + imports.contentDeepHashCode()
        __r = __r*31 + projectPath.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("TypeValidationRequest (")
        printer.indent {
            print("typeName = "); typeName.print(printer); println()
            print("imports = "); imports.print(printer); println()
            print("projectPath = "); projectPath.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AtomicGenerationModel.kt:75]
 */
data class TypeValidationResponse (
    val isValid: Boolean,
    val fullTypeName: String?,
    val suggestedImport: String?,
    val suggestedImports: Array<String>,
    val isAmbiguous: Boolean,
    val ambiguousNamespaces: Array<String>
) : IPrintable {
    //companion
    
    companion object : IMarshaller<TypeValidationResponse> {
        override val _type: KClass<TypeValidationResponse> = TypeValidationResponse::class
        override val id: RdId get() = RdId(2824265376340047015)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): TypeValidationResponse  {
            val isValid = buffer.readBool()
            val fullTypeName = buffer.readNullable { buffer.readString() }
            val suggestedImport = buffer.readNullable { buffer.readString() }
            val suggestedImports = buffer.readArray {buffer.readString()}
            val isAmbiguous = buffer.readBool()
            val ambiguousNamespaces = buffer.readArray {buffer.readString()}
            return TypeValidationResponse(isValid, fullTypeName, suggestedImport, suggestedImports, isAmbiguous, ambiguousNamespaces)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: TypeValidationResponse)  {
            buffer.writeBool(value.isValid)
            buffer.writeNullable(value.fullTypeName) { buffer.writeString(it) }
            buffer.writeNullable(value.suggestedImport) { buffer.writeString(it) }
            buffer.writeArray(value.suggestedImports) { buffer.writeString(it) }
            buffer.writeBool(value.isAmbiguous)
            buffer.writeArray(value.ambiguousNamespaces) { buffer.writeString(it) }
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as TypeValidationResponse
        
        if (isValid != other.isValid) return false
        if (fullTypeName != other.fullTypeName) return false
        if (suggestedImport != other.suggestedImport) return false
        if (!(suggestedImports contentDeepEquals other.suggestedImports)) return false
        if (isAmbiguous != other.isAmbiguous) return false
        if (!(ambiguousNamespaces contentDeepEquals other.ambiguousNamespaces)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + isValid.hashCode()
        __r = __r*31 + if (fullTypeName != null) fullTypeName.hashCode() else 0
        __r = __r*31 + if (suggestedImport != null) suggestedImport.hashCode() else 0
        __r = __r*31 + suggestedImports.contentDeepHashCode()
        __r = __r*31 + isAmbiguous.hashCode()
        __r = __r*31 + ambiguousNamespaces.contentDeepHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("TypeValidationResponse (")
        printer.indent {
            print("isValid = "); isValid.print(printer); println()
            print("fullTypeName = "); fullTypeName.print(printer); println()
            print("suggestedImport = "); suggestedImport.print(printer); println()
            print("suggestedImports = "); suggestedImports.print(printer); println()
            print("isAmbiguous = "); isAmbiguous.print(printer); println()
            print("ambiguousNamespaces = "); ambiguousNamespaces.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}
