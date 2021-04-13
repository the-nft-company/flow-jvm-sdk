package org.onflow.sdk

// this files contains types for the cadence data interchange format

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.math.BigDecimal
import java.math.BigInteger

const val TYPE_VOID = "Void"
const val TYPE_OPTIONAL = "Optional"
const val TYPE_BOOLEAN = "Bool"
const val TYPE_STRING = "String"
const val TYPE_ARRAY = "Array"
const val TYPE_DICTIONARY = "Dictionary"
const val TYPE_ADDRESS = "Address"
const val TYPE_PATH = "Path"
const val TYPE_CAPABILITY = "Capability"
const val TYPE_STRUCT = "Struct"
const val TYPE_RESOURCE = "Resource"
const val TYPE_EVENT = "Event"
const val TYPE_CONTRACT = "Contract"
const val TYPE_ENUM = "Enum"

@JsonTypeInfo(
    use=JsonTypeInfo.Id.NAME,
    include=JsonTypeInfo.As.EXISTING_PROPERTY,
    property="type",
    visible = true)
@JsonSubTypes(value = [
    Type(value = VoidField::class, name = TYPE_VOID),
    Type(value = OptionalField::class, name = TYPE_OPTIONAL),
    Type(value = BooleanField::class, name = TYPE_BOOLEAN),
    Type(value = StringField::class, name = TYPE_STRING),
    Type(value = NumberField::class, names = arrayOf("Int", "UInt", "Int8", "UInt8", "Int16", "UInt16", "Int32", "UInt32", "Int64", "UInt64","Int128", "UInt128", "Int256", "UInt256", "Word8", "Word16", "Word32", "Word64", "Fix64", "UFix64")),
    Type(value = ArrayField::class, name = TYPE_ARRAY),
    Type(value = DictionaryField::class, name = TYPE_DICTIONARY),
    Type(value = AddressField::class, name = TYPE_ADDRESS),
    Type(value = PathField::class, name = TYPE_PATH),
    Type(value = CapabilityField::class, name = TYPE_CAPABILITY),
    Type(value = StructField::class, name = TYPE_STRUCT),
    Type(value = ResourceField::class, name = TYPE_RESOURCE),
    Type(value = EventField::class, name = TYPE_EVENT),
    Type(value = ContractField::class, name = TYPE_CONTRACT),
    Type(value = EnumField::class, name = TYPE_ENUM)
])
abstract class Field<T> constructor(
    val type: String,
    val value: T?
)

open class VoidField : Field<Void>(TYPE_VOID, null)

open class OptionalField<T>(value: Field<T>?) : Field<Field<T>>(TYPE_OPTIONAL, value)

open class BooleanField(value: Boolean) : Field<Boolean>(TYPE_BOOLEAN, value)

open class StringField(value: String) : Field<String>(TYPE_STRING, value)

@ExperimentalUnsignedTypes
open class NumberField(type: String, value: String?) : Field<String>(type, value) {
    fun toUInt(): UInt? = value?.toUInt()
    fun toInt(): Int? = value?.toInt()
    fun toULong(): ULong? = value?.toULong()
    fun toLong(): Long? = value?.toLong()
    fun toBigInteger(): BigInteger? = value?.toBigInteger()
    fun toFloat(): Float? = value?.toFloat()
    fun toDouble(): Double? = value?.toDouble()
    fun toBigDecimal(): BigDecimal? = value?.toBigDecimal()
}

open class ArrayField(value: Array<Field<*>>) : Field<Array<Field<*>>>(TYPE_ARRAY, value)

open class DictionaryField(value: List<Pair<Field<*>, Field<*>>>) : Field<List<Pair<Field<*>, Field<*>>>>(TYPE_DICTIONARY, value)

open class AddressField(value: String) : Field<String>(TYPE_ADDRESS, value)

open class PathValue(val domain: String, val identifier: String)
open class PathField(value: PathValue) : Field<PathValue>(TYPE_PATH, value)

open class CapabilityValue(val path: String, val address: String, val borrowType: String)
open class CapabilityField(value: CapabilityValue) : Field<CapabilityValue>(TYPE_CAPABILITY, value)

open class CompositeField(type: String, value: CompositeValue) : Field<CompositeValue>(type, value) {
    val id: String? get() = value?.id
    operator fun <T : Field<*>> get(name: String): T? = value?.getField(name)
    operator fun contains(name: String): Boolean = value?.getField<Field<*>>(name) != null
}
open class CompositeAttribute(val name: String, val value: Field<*>)
open class CompositeValue(val id: String, val fields: Array<CompositeAttribute>)  {
    fun <T : Field<*>> getField(name: String): T? = fields.find { it.name == name }?.value as T?
    operator fun <T> get(name: String): T? = getField<Field<*>>(name)?.value as T?
    operator fun contains(name: String): Boolean =fields.find { it.name == name } != null
}
open class StructField(value: CompositeValue) : CompositeField(TYPE_STRUCT, value)
open class ResourceField(value: CompositeValue) : CompositeField(TYPE_RESOURCE, value)
open class EventField(value: CompositeValue) : CompositeField(TYPE_EVENT, value)
open class ContractField(value: CompositeValue) : CompositeField(TYPE_CONTRACT, value)
open class EnumField(value: CompositeValue) : CompositeField(TYPE_ENUM, value)
