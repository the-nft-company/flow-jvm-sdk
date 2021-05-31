package org.onflow.sdk.cadence

import java.lang.annotation.Inherited
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import org.onflow.sdk.Flow

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
annotation class CDIFConverter(
    val converter: KClass<out CadenceDataInterchangeFormatConverter<*>>
)

interface CadenceDataInterchangeFormatConverter<T> {
    fun unmarshall(value: Field<*>): T {
        throw UnsupportedOperationException("${this::class.simpleName} cannot convert from cdif")
    }
    fun marshall(value: T): Field<*> {
        throw UnsupportedOperationException("${this::class.simpleName} cannot convert to cdif")
    }
}

object Marshalling {

    private val converterCache: MutableMap<KClass<*>, CadenceDataInterchangeFormatConverter<*>> = mutableMapOf()

    @JvmStatic
    @JvmOverloads
    @Synchronized
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getConverter(type: KClass<out T>, cache: Boolean = true): CadenceDataInterchangeFormatConverter<T> {
        var ret = if (cache) {
            converterCache[type]
        } else {
            null
        }
        if (ret == null) {
            ret = type.annotations
                .find { it is CDIFConverter }
                ?.let { it as CDIFConverter }
                ?.converter
                ?.createInstance()
        }
        if (ret != null && cache) {
            converterCache[type] = ret
        } else if (ret == null) {
            throw IllegalArgumentException("No CDIFConverter found for ${type.simpleName}")
        }
        return ret as CadenceDataInterchangeFormatConverter<T>
    }

    @JvmStatic
    fun <T: Any> unmarshall(type: KClass<T>, value: Field<*>): T = getConverter(type).unmarshall(value)

    @JvmStatic
    @JvmOverloads
    fun <T: Any> marshall(value: T, clazz: KClass<out T> = value::class): Field<*> = getConverter(clazz).marshall(value)
}

fun <T : Field<*>> cdif(block: CDIFBuilder.() -> T): T = block(CDIFBuilder())

class CDIFBuilder {
    fun <T: Any> marshall(value: T, clazz: KClass<out T> = value::class): Field<*> = Flow.marshall(value, clazz)
    fun void(): VoidField = VoidField()
    fun <T> optional(block: CDIFBuilder.() -> Field<T>?): OptionalField<T> = OptionalField(block())
    fun <T> optional(value: Field<T>?): OptionalField<T> = OptionalField(value)
    fun boolean(value: Boolean): BooleanField = BooleanField(value)
    fun string(value: String): StringField = StringField(value)
    fun array(value: Iterable<Field<*>>): ArrayField = ArrayField(value)
    fun <T> array(value: Iterable<T>, mapper:  CDIFBuilder.(T) -> Field<*> ): ArrayField = ArrayField(value.map { mapper(it) })
    fun array(block: CDIFBuilder.() -> Iterable<Field<*>>): ArrayField = ArrayField(block().toList().toTypedArray())
    fun dictionary(value: Iterable<DictionaryFieldEntry>): DictionaryField = DictionaryField(value)
    fun dictionary(block: CDIFBuilder.() -> Array<DictionaryFieldEntry>): DictionaryField = DictionaryField(block().toList().toTypedArray())
    fun dictionaryOfMap(block: CDIFBuilder.() -> Map<Field<*>, Field<*>>): DictionaryField = DictionaryField(block().map { DictionaryFieldEntry(it.key, it.value) })
    fun dictionaryOfNamedMap(block: CDIFBuilder.() -> Map<String, Field<*>>): DictionaryField = DictionaryField(block().map { DictionaryFieldEntry(StringField(it.key), it.value) })
    fun dictionaryOfPairs(block: CDIFBuilder.() -> Iterable<Pair<Field<*>, Field<*>>>): DictionaryField = DictionaryField(block().map { DictionaryFieldEntry(it.first, it.second) })
    fun dictionaryOfNamedPairs(block: CDIFBuilder.() -> Iterable<Pair<String, Field<*>>>): DictionaryField = DictionaryField(block().map { DictionaryFieldEntry(StringField(it.first), it.second) })
    fun address(value: String): AddressField = AddressField(value)
    fun address(value: ByteArray): AddressField = AddressField(value)
    fun path(value: PathValue): PathField = PathField(value)
    fun path(domain: String, identifier: String): PathField = PathField(PathValue(domain, identifier))
    fun capability(value: CapabilityValue): CapabilityField = CapabilityField(value)
    fun capability(path: String, address: String, borrowType: String): CapabilityField = CapabilityField(CapabilityValue(path, address, borrowType))
    fun composite(id: String, fields: Array<CompositeAttribute>): CompositeValue = CompositeValue(id, fields.toList().toTypedArray())
    fun composite(id: String, fields: Iterable<Pair<String, Field<*>>>): CompositeValue = CompositeValue(id, fields.map { CompositeAttribute(it.first, it.second) }.toTypedArray())
    fun composite(id: String, fields: Map<String, Field<*>>): CompositeValue = CompositeValue(id, fields.map { CompositeAttribute(it.key, it.value) }.toTypedArray())
    fun composite(id: String, block: CDIFBuilder.() -> Array<CompositeAttribute>): CompositeValue = CompositeValue(id, block().toList().toTypedArray())
    fun compositeOfPairs(id: String, block: CDIFBuilder.() -> Iterable<Pair<String, Field<*>>>): CompositeValue = CompositeValue(id, block().map { CompositeAttribute(it.first, it.second) }.toTypedArray())
    fun compositeOfNamedMap(id: String, block: CDIFBuilder.() -> Map<String, Field<*>>): CompositeValue = CompositeValue(id, block().map { CompositeAttribute(it.key, it.value) }.toTypedArray())
    fun struct(value: CompositeValue): StructField = StructField(value)
    fun struct(block: CDIFBuilder.() -> CompositeValue): StructField = StructField(block())
    fun resource(value: CompositeValue): ResourceField = ResourceField(value)
    fun resource(block: CDIFBuilder.() -> CompositeValue): ResourceField = ResourceField(block())
    fun event(value: CompositeValue): EventField = EventField(value)
    fun event(block: CDIFBuilder.() -> CompositeValue): EventField = EventField(block())
    fun contract(value: CompositeValue): ContractField = ContractField(value)
    fun contract(block: CDIFBuilder.() -> CompositeValue): ContractField = ContractField(block())
    fun <T : Enum<T>> enum(value: T): EnumField = enum(value.declaringClass.simpleName, uint8(value.ordinal))
    fun enum(value: CompositeValue): EnumField = EnumField(value)
    fun enum(block: CDIFBuilder.() -> CompositeValue): EnumField = EnumField(block())
    fun enum(id: String, value: Field<*>): EnumField = EnumField(compositeOfPairs(id) { listOf("rawValue" to value) })
    fun number(type: String, value: String): NumberField = NumberField(type, value)
    fun number(type: String, value: Number): NumberField = NumberField(type, value.toString())
    fun int(value: Number) : IntNumberField = IntNumberField(value.toString())
    fun uint(value: Number) : UIntNumberField = UIntNumberField(value.toString())
    fun int8(value: Number) : Int8NumberField = Int8NumberField(value.toString())
    fun uint8(value: Number) : UInt8NumberField = UInt8NumberField(value.toString())
    fun int16(value: Number) : Int16NumberField = Int16NumberField(value.toString())
    fun uint16(value: Number) : UInt16NumberField = UInt16NumberField(value.toString())
    fun int32(value: Number) : Int32NumberField = Int32NumberField(value.toString())
    fun uint32(value: Number) : UInt32NumberField = UInt32NumberField(value.toString())
    fun int64(value: Number) : Int64NumberField = Int64NumberField(value.toString())
    fun uint64(value: Number) : UInt64NumberField = UInt64NumberField(value.toString())
    fun int128(value: Number) : Int128NumberField = Int128NumberField(value.toString())
    fun uint128(value: Number) : UInt128NumberField = UInt128NumberField(value.toString())
    fun int256(value: Number) : Int256NumberField = Int256NumberField(value.toString())
    fun uint256(value: Number) : UInt256NumberField = UInt256NumberField(value.toString())
    fun word8(value: Number) : Word8NumberField = Word8NumberField(value.toString())
    fun word16(value: Number) : Word16NumberField = Word16NumberField(value.toString())
    fun word32(value: Number) : Word32NumberField = Word32NumberField(value.toString())
    fun word64(value: Number) : Word64NumberField = Word64NumberField(value.toString())
    fun fix64(value: Number) : Fix64NumberField = Fix64NumberField(value.toString())
    fun ufix64(value: Number) : UFix64NumberField = UFix64NumberField(value.toString())
    fun int(value: String) : IntNumberField = IntNumberField(value)
    fun uint(value: String) : UIntNumberField = UIntNumberField(value)
    fun int8(value: String) : Int8NumberField = Int8NumberField(value)
    fun uint8(value: String) : UInt8NumberField = UInt8NumberField(value)
    fun int16(value: String) : Int16NumberField = Int16NumberField(value)
    fun uint16(value: String) : UInt16NumberField = UInt16NumberField(value)
    fun int32(value: String) : Int32NumberField = Int32NumberField(value)
    fun uint32(value: String) : UInt32NumberField = UInt32NumberField(value)
    fun int64(value: String) : Int64NumberField = Int64NumberField(value)
    fun uint64(value: String) : UInt64NumberField = UInt64NumberField(value)
    fun int128(value: String) : Int128NumberField = Int128NumberField(value)
    fun uint128(value: String) : UInt128NumberField = UInt128NumberField(value)
    fun int256(value: String) : Int256NumberField = Int256NumberField(value)
    fun uint256(value: String) : UInt256NumberField = UInt256NumberField(value)
    fun word8(value: String) : Word8NumberField = Word8NumberField(value)
    fun word16(value: String) : Word16NumberField = Word16NumberField(value)
    fun word32(value: String) : Word32NumberField = Word32NumberField(value)
    fun word64(value: String) : Word64NumberField = Word64NumberField(value)
    fun fix64(value: String) : Fix64NumberField = Fix64NumberField(value)
    fun ufix64(value: String) : UFix64NumberField = UFix64NumberField(value)
}

fun <T> parseCdif(root: Field<*>, block: CDIFParser.() -> T): T {
    if (root !is CompositeField) {
        throw IllegalArgumentException("root !is CompositeField")
    }
    val parser = CDIFParser()
    parser.push(root)
    return block(parser)
}

class CDIFParser {
    var compositeStack: MutableList<CompositeField> = mutableListOf()
    val composite: CompositeField get() = compositeStack.last()
    val compositeValue: CompositeValue get() = composite.value!!

    fun push(composite: CompositeField) = compositeStack.add(composite)
    fun pop(): CompositeField = compositeStack.removeLast()

    fun <T : Field<*>> field(name: String): T = compositeValue.getRequiredField(name)

    fun <T> with(composite: CompositeField, block: CDIFParser.() -> T): T {
        push(composite)
        try {
            return block(this)
        } finally {
            pop()
        }
    }

    fun <T, F : Field<*>> optional(name: String, block: CDIFParser.(field: F) -> T): T? {
        val field = compositeValue.getField<F>(name)
        return if (field != null) {
            block(field)
        } else {
            null
        }
    }

    inline fun <reified T : Any> unmarshall(name: String): T = Flow.unmarshall(T::class, field(name))
    inline fun <reified T : Any> unmarshall(name: String, type: KClass<T>): T = Flow.unmarshall(type, field(name))

    inline fun <reified T : Any> unmarshall(field: Field<*>): T = Flow.unmarshall(T::class, field)
    inline fun <reified T : Any> unmarshall(field: Field<*>, type: KClass<T>): T = Flow.unmarshall(type, field)

    fun <T, F : Field<*>> field(name: String, block: CDIFParser.(field: F) -> T): T = block(field(name))
    fun boolean(name: String): Boolean = field<BooleanField>(name).value!!
    fun string(name: String): String = field<StringField>(name).value!!
    fun address(name: String): String = field<AddressField>(name).value!!
    fun short(name: String): Short = field<NumberField>(name).toShort()!!
    fun int(name: String): Int = field<NumberField>(name).toInt()!!
    fun long(name: String): Long = field<NumberField>(name).toLong()!!
    fun bigInteger(name: String): BigInteger = field<NumberField>(name).toBigInteger()!!
    fun float(name: String): Float = field<NumberField>(name).toFloat()!!
    fun double(name: String): Double = field<NumberField>(name).toDouble()!!
    fun bigDecimal(name: String): BigDecimal = field<NumberField>(name).toBigDecimal()!!
    fun <T> array(name: String, block: CDIFParser.(field: ArrayField) -> T): T = block(field(name))
    fun <T> arrayValues(name: String, mapper: CDIFParser.(field: Field<*>) -> T): List<T> = field<ArrayField>(name).value!!.map { mapper(it) }
    fun byteArray(name: String): ByteArray = arrayValues(name) { (it as UInt8NumberField).toByte()!! }.toByteArray()
    fun <T> dictionary(name: String, block: CDIFParser.(field: DictionaryField) -> T): T = block(field(name))
    fun <K, V> dictionaryPairs(name: String, mapper: CDIFParser.(key: Field<*>, value: Field<*>) -> Pair<K, V>): List<Pair<K, V>> = field<DictionaryField>(name).value!!.map { mapper(it.key, it.value) }
    fun <K, V> dictionaryMap(name: String, mapper: CDIFParser.(key: Field<*>, value: Field<*>) -> Pair<K, V>): Map<K, V> = dictionaryPairs(name, mapper).toMap()
    inline fun <reified T : Enum<T>, V : Field<*>> enum(name: String, crossinline mapper: (V) -> T): T {
        return with(field(name)) {
            val field = compositeValue.getRequiredField<V>("rawValue")
            mapper(field)
        }
    }
    inline fun <reified T : Enum<T>> enum(name: String): T = enum<T, UInt8NumberField>(name) { f -> f.toInt()!!.let { enumValues<T>()[it] } }
}
