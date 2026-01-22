import java.util.Collections

interface Element: Walkable<Element>
interface Walkable<T> {
    val children: List<T>
}
interface CompilerMessages

sealed interface ReasonedBoolean {
    val value: Boolean get() = false
    class False(s: String?=null,causes: Array<*>?=null): ReasonedBoolean
    class True(s: String?=null,causes: Array<*>?=null): ReasonedBoolean
}

enum class BitSize(val bits: Int) {
    BIT_1(1),
    BIT_8(8),
    BIT_16(16),
    BIT_32(32),
    BIT_64(64),
    BIT_128(128),
    BIT_256(256);

    companion object {
        fun byBits(bits: Int): BitSize? {
            return entries.find { it.bits == bits }
        }
    }
}

sealed interface Accessor {
    val accessFromSelf: List<Access<*>>
}

sealed interface Access<T : Accessible<T>> {
    
    var target: T?
    
    var accessor: Accessor
    
    val reference: String

    fun resolveAccessTarget(messages: CompilerMessages): T?
    fun finalize(messages: CompilerMessages) {
        target = resolveAccessTarget(messages)
        target?.accessToSelf?.add(this)
    }

    sealed interface TypeAccess : Access<TypeDefinition>
}

sealed interface Accessible<T : Accessible<T>> {
    
    val accessToSelf: MutableList<in Access<T>>
    val name: String?
}

sealed interface Type : Element {
    fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type

    fun acceptsInstanceOf(type: Type): ReasonedBoolean {
        return if (type is Undefined) ReasonedBoolean.True("While UNDEFINED cannot be assigned to anything, the error lies in the definition of the type not its usage");
        else if (type is Union) {
            val optionAssignability = type.options.map { mapAssignabilityBoolean(accepts(it), it, this) }
            if (optionAssignability.all { boolean -> boolean.value }) ReasonedBoolean.True(
                "Each type in the union is assignable to $this",
                optionAssignability.toTypedArray()
            )
            else {
                val assignWholeUnion = accepts(type)
                if(assignWholeUnion.value) return assignWholeUnion;
                ReasonedBoolean.False(
                    "Not all types in the union ($type), nor the union itself are/is assignable to $this",
                    (optionAssignability.filter { !it.value } + assignWholeUnion).toTypedArray()
                )
            }
        } else accepts(type)
    }

    fun mapAssignabilityBoolean(boolean: ReasonedBoolean, from: Type, to: Type): ReasonedBoolean {
        return if (boolean.value) ReasonedBoolean.True("$from is assignable to $to", arrayOf(boolean))
        else ReasonedBoolean.False("$from is not assignable to $to", arrayOf(boolean))
    }

    fun accepts(type: Type): ReasonedBoolean

    sealed interface Primitive : Type {
        val size: BitSize
    }

    
    data class Int(override val size: BitSize) : Primitive {
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type = this
        override fun toString(): String {
            return "i${size.bits}"
        }

        override fun accepts(type: Type): ReasonedBoolean {
            return if (type is Int && type.size.bits <= this.size.bits) ReasonedBoolean.True(
                "integers accept integers their size and smaller"
            )
            else if (type is UInt && type.size.bits * 2 <= this.size.bits) ReasonedBoolean.True(
                "integers accept unsigned integers that are at most half their size"
            )
            else if (type is Float) ReasonedBoolean.False("Integers only accept integers. To assign non int numbers convert them first")
            else if (type is Int) ReasonedBoolean.False("Integers only accept integers their size or less, since assigning for example a i64 to a i32 can cause number overflow")
            else if (type is UInt) ReasonedBoolean.False("Integers only accept unsigned integers that are at most half their size (you can assign u32 to i64 but not u32 to i32)")
            else ReasonedBoolean.False("$type cannot be assigned to $this")
        }

        
        override val children: List<Element> = Collections.emptyList()
    }

    
    data class UInt(override val size: BitSize) : Primitive {
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type = this
        override fun toString(): String {
            return "u${size.bits}"
        }

        override fun accepts(type: Type): ReasonedBoolean {
            return if (type is UInt && type.size.bits <= this.size.bits) ReasonedBoolean.True(
                "unsigned integers accept unsigned integers their size and smaller"
            )
            else if (type is UInt) ReasonedBoolean.False("$type is too large to fit into a $this")
            else if (type is Int || type is Float) ReasonedBoolean.False("Unsigned integers only accept unsigned integers. To assign non uint numbers convert them first")
            else ReasonedBoolean.False("$type cannot be assigned to $this")
        }

        
        override val children: List<Element> = Collections.emptyList()

    }

    
    data class Float(override val size: BitSize = BitSize.BIT_32) : Primitive {
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type = this
        override fun toString(): String {
            return "f${size.bits}"
        }

        override fun accepts(type: Type): ReasonedBoolean {
            if (type is Float && type.size.bits <= this.size.bits) return ReasonedBoolean.True(
                "floats accept floats their size and smaller"
            )
            if (type is Float) return ReasonedBoolean.False("Floats only accept floats their size or smaller")
            return ReasonedBoolean.False("$type cannot be assigned to $this")
        }

        
        override val children: List<Element> = Collections.emptyList()

    }

    
    data object Null : Type {
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type = this
        override fun toString(): String {
            return "null"
        }

        override fun accepts(type: Type): ReasonedBoolean {
            return if (type is Null) ReasonedBoolean.True("null is assignable to the null type")
            else ReasonedBoolean.False("null is the only value assignable to the null type")
        }

        
        override val children: List<Element> = Collections.emptyList()

    }

    /**
     * This type is not usable in the language. It is the type used at compile time when a type is not resolvable
     */
    
    data object Undefined : Type {
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type = this
        override fun accepts(type: Type): ReasonedBoolean {
            return ReasonedBoolean.True("While the UNDEFINED type does not accept any types, the error is to be treated at the source (the type definition) an not its usage")
        }

        
        override val children: List<Element> = Collections.emptyList()
    }

    
    data class Array(
        val elementType: Type
    ) : Type {
        fun Type.cacheResolved(fn: (messages: CompilerMessages, generics: Map<String, Type>)-> Type):
                ((messages: CompilerMessages, generics: Map<String, Type>)-> Type) {
            TODO()
        }
        private val cachedResolve = cacheResolved { messages, generics ->
            Array(elementType.resolve(messages, generics))
        }
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type {
            return Array(elementType.resolve(messages, generics))
        }

        override fun accepts(type: Type): ReasonedBoolean {
            if (type !is Array) return ReasonedBoolean.False("$type is not an array and can therefore not be assigned to an array")
            val elementsAccept = this.elementType.acceptsInstanceOf(type.elementType)
            return if (elementsAccept.value) {
                ReasonedBoolean.True("$type is an array with an assignable element type", arrayOf(elementsAccept))
            } else {
                ReasonedBoolean.False(
                    "Element type of $type (${type.elementType}) is not compatible with ${this.elementType}",
                    arrayOf(elementsAccept)
                )
            }
        }

        
        override val children: List<Element> = listOfNotNull(elementType)
    }

    
    data object Boolean : Primitive {
        override fun toString(): String {
            return "boolean"
        }

        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type = this

        override fun accepts(type: Type): ReasonedBoolean {
            return if (type is Boolean) {
                ReasonedBoolean.True("boolean is assignable to boolean")
            } else {
                ReasonedBoolean.False("only boolean is assignable to boolean")
            }
        }

        
        override val children: List<Element> = Collections.emptyList()
        override val size: BitSize
            get() = BitSize.BIT_1
    }

    
    data class FunctionTypeSignature(val returnType: Type?, val parameters: List<Parameter>) :
        Type {
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type {
            return FunctionTypeSignature(
                returnType?.resolve(messages, generics),
                parameters.map { it.copy(type = it.type.resolve(messages, generics)) })
        }

        override fun accepts(type: Type): ReasonedBoolean {
            TODO("Not yet implemented")
        }

        
        data class Parameter(val name: String, val type: Type, var optional: kotlin.Boolean) :
            Element {
            
            override val children: List<Element> = listOf(type)
            override fun toString(): String {
                return "$name: $type"
            }
        }

        
        override val children: List<Element> = parameters + listOfNotNull(returnType)

        override fun toString(): String {
            return "(${parameters.joinToString(", ") { "${it.type}" }}) -> ${returnType ?: "void"}"
        }
    }

    
    data class TypeReference(
        override val reference: String,
        val genericParameters: List<Type>
    ) : Type, Access.TypeAccess {
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type {
            var target = target
            if (target == null) {
                val resolved = resolveAccessTarget(messages)
                target = resolved
                this.target = resolved
            }
            return when (target) {
                is TypeDefinition.DirectTypeDefinition -> target.resolve(messages, generics)
                is TypeDefinition.TypeParameter -> generics[reference]
                    ?: Type.Undefined

                is TypeDefinition.ParameterizedType -> {
                    TODO()    
                }
            }
        }

        public lateinit var resolvedCache: Type
        override fun accepts(type: Type): ReasonedBoolean {
            if (!this::resolvedCache.isInitialized) throw Error("Type reference $this cannot be used in type checking. Resolve it first")
            if (resolvedCache == this) {
                if (type is TypeReference) { TODO() } else {
                    return ReasonedBoolean.False("$type is not assignable to $this")
                }
            }
            return resolvedCache.accepts(type)
        }

        
        override val children: List<Element> = genericParameters.map { it }

        
        override var target: TypeDefinition? = null;

        
        override lateinit var accessor: Accessor;

        override fun resolveAccessTarget(messages: CompilerMessages): TypeDefinition {
            TODO()
        }

        override fun finalize(messages: CompilerMessages) {
            super.finalize(messages)
            resolvedCache = resolve(messages, mapOf())
        }
    }

    
    class Union(var options: List<Type>) : Type {
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type {
            return Union(options.map { it.resolve(messages, generics) }
                .flatMap { type -> if (type is Union) type.options else listOf(type) }
                .distinct()
            )
        }

        override fun accepts(type: Type): ReasonedBoolean {
            val optionsAccept = options.map { it.toString() to it.acceptsInstanceOf(type) }
            val matches = optionsAccept.filter { it.second.value }
            if (matches.isNotEmpty()) return ReasonedBoolean.True(
                "$type is assignable to one or more options of $this",
                causes = matches.toTypedArray()
            )
            return ReasonedBoolean.False(
                "$type is not assignable to any of: ${options.joinToString(", ")}",
                causes = optionsAccept.toTypedArray()
            )
        }

        override val children: List<Element> get() = options
        override fun toString(): String {
            return options.joinToString(" | ")
        }
    }

    
    data class StructuralInterface(val fields: Map<String, TypeDefinition.ParameterizedType.Struct.Field>) : Type {
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type {
            return StructuralInterface(fields.mapValues { (_, b) -> b.copy(type = b.type.resolve(messages, generics)) })
        }

        override fun accepts(type: Type): ReasonedBoolean {
            return ReasonedBoolean.False("structural inferface assignability not implemented yet")
        }

        override val children: List<Element> get() = fields.values.toList()
        override fun toString(): String {
            return "{${fields.entries.joinToString(", ")}}"
        }
    }
}

sealed class TypeDefinition : Accessible<TypeDefinition>, Element {
    abstract override val name: String
    override val accessToSelf: MutableList<in Access<TypeDefinition>> = mutableListOf()

    /**
     * A type that is not directly usable since it is parameterized
     *
     * Examples:
     * - List&lt;T>
     * - Either&lt;A,B>
     * - Optional&lt;T>
     *
     * These types are only full types when referenced with their parameters filled like `List<String>`
     *
     * To form a [Type] from a [ParameterizedType] you need a [Type.TypeReference]
     */
    sealed class ParameterizedType : TypeDefinition() {
        abstract val generics: List<TypeParameter>
        abstract fun toType(messages: CompilerMessages, typeParameters: Map<String, Type>): Type;
        
        data class Struct(
            override val name: String,
            override val generics: List<TypeParameter>,
            private val fields: MutableSet<Field>,
            private val embedded: MutableSet<Lazy<Struct>> = mutableSetOf()
        ) : ParameterizedType() {
            data class Field(override val name: String, var mutable: kotlin.Boolean, val type: Type) : Element,
                Accessible<Field> {
                override val children: List<Element> get() = listOf(type)
                
                override val accessToSelf: MutableList<in Access<Field>> = mutableListOf()
            }

            val allFields: Set<Field> get() = embedded.flatMap { it.value.allFields }.toSet() + fields
            override val children: List<Element> get() = fields.toList() + embedded.map { it.value }
            override fun toType(
                messages: CompilerMessages,
                typeParameters: Map<String, Type>
            ): Type {
                TODO("Not yet implemented")
            }
        }

        
        data class Alias(
            override val name: String,
            override val generics: List<TypeParameter>,
            var type: Type
        ) : ParameterizedType() {
            override val children: List<Element> get() = listOf(type)
            override fun toType(messages: CompilerMessages, typeParameters: Map<String, Type>): Type {
                return type.resolve(messages, typeParameters);
            }
        }

        
        data class Interface(
            override val name: String,
            override val generics: List<TypeParameter>,
            val methods: Map<String, List<NamedFunctionSignature>>
        ) : ParameterizedType() {
            constructor(
                name: String,
                generics: List<TypeParameter>,
                methods: List<NamedFunctionSignature>
            ) : this(name, generics, methods.groupBy { it.name })

            override val children: List<Element> get() = methods.values.flatten()
            override fun toType(
                messages: CompilerMessages,
                typeParameters: Map<String, Type>
            ): Type {
                TODO("Not yet implemented")
            }
        }

        
        data class Class(
            override val name: String,
            override val generics: List<TypeParameter>,
            val fields: List<Struct.Field>,
            val methods: List<Function>
        ) : ParameterizedType() {
            override val children: List<Element>
                get() = fields + methods // + generics

            override fun toType(
                messages: CompilerMessages,
                typeParameters: Map<String, Type>
            ): Type {
                TODO("Not yet implemented")
            }

        }
    }

    /**
     * A [Type] template that is specific enought to be a type by itself
     */
    sealed class DirectTypeDefinition : TypeDefinition(), Type {
        
        data class Enum(
            override val name:String,
            val constants: List<Constant>
        ) : DirectTypeDefinition() {
            override val children: List<Element> get() = constants
            override fun resolve(
                messages: CompilerMessages,
                generics: Map<String, Type>
            ): Type {
                return this
            }

            override fun accepts(type: Type): ReasonedBoolean {
                return if (type == this) ReasonedBoolean.True("$type is the same enum as $this")
                else if (type is Enum) ReasonedBoolean.False("$type is not the same enum as $this")
                else ReasonedBoolean.False("$type is not an enum")
            }

            init {
                constants.forEach { it.enum = this }
            }

            
            data class Constant(override val name: String) : Element, Accessible<Constant> {
                override val children: List<Element> get() = listOf()
                
                override val accessToSelf: MutableList<in Access<Constant>> = mutableListOf()
                
                lateinit var enum: Enum
            }
        }
    }

    
    class TypeParameter(
        override val name: String
    ) : TypeDefinition() {
        
        override val children: List<Element> = emptyList();

        override fun toString(): String {
            return name
        }
    }
}
class Function(
    override val name: String?,
    val returnType: Type?,
    val parameters: List<Parameter>,
) : Element, Accessible<Function>, Accessor {
    override val children: List<Element>
        get() = TODO("Not yet implemented")
    override val accessToSelf: MutableList<in Access<Function>>
        get() = TODO("Not yet implemented")
    override val accessFromSelf: List<Access<*>>
        get() = TODO("Not yet implemented")

    data class Parameter(
        val name: String,
        val declaredType: Type
    ) : Element {
        override val children: List<Element>
            get() = listOf(declaredType)
    }
}
class NamedFunctionSignature(val name: String, val typeSignature: Type.FunctionTypeSignature) :
    Element {
    override val children: List<Element>
        get() = listOf(typeSignature)
}
fun Type.cacheResolved(fn: (messages: CompilerMessages, generics: Map<String, Type>)-> Type):
        ((messages: CompilerMessages, generics: Map<String, Type>)-> Type) {
    TODO()
}