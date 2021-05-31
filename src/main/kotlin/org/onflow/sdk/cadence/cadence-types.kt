package org.onflow.sdk.cadence

import java.math.BigDecimal
import org.onflow.sdk.HashAlgorithm
import org.onflow.sdk.SignatureAlgorithm

@CDIFConverter(DeployedContractConverter::class)
open class DeployedContract(
    val name: String,
    val code: ByteArray
)

@CDIFConverter(PublicKeyConverter::class)
open class PublicKey(
    val publicKey: ByteArray,
    val signatureAlgorithm: SignatureAlgorithm,
    val isValid: Boolean
)

@CDIFConverter(AccountKeyConverter::class)
open class AccountKey(
    val keyIndex: Int,
    val publicKey: PublicKey,
    val hashAlgorithm: HashAlgorithm,
    val weight: BigDecimal,
    val isRevoked: Boolean
)

class AccountKeyConverter : CadenceDataInterchangeFormatConverter<AccountKey> {
    override fun unmarshall(value: Field<*>): AccountKey = parseCdif(value) {
        AccountKey(
            keyIndex = int("keyIndex"),
            publicKey = unmarshall("publicKey"),
            hashAlgorithm = enum("hashAlgorithm"),
            weight = bigDecimal("weight"),
            isRevoked = boolean("isRevoked")
        )
    }
    override fun marshall(value: AccountKey): Field<*> {
        return cdif {
            struct {
                compositeOfPairs("AccountKey") {
                    listOf(
                        "keyIndex" to int(value.keyIndex),
                        "publicKey" to marshall(value.publicKey),
                        "hashAlgorithm" to enum(value.hashAlgorithm),
                        "weight" to ufix64(value.weight),
                        "isRevoked" to boolean(value.isRevoked),
                    )
                }
            }
        }
    }
}

class PublicKeyConverter : CadenceDataInterchangeFormatConverter<PublicKey> {
    override fun unmarshall(value: Field<*>): PublicKey = parseCdif(value) {
        PublicKey(
            publicKey = byteArray("publicKey"),
            signatureAlgorithm = enum("signatureAlgorithm"),
            isValid = boolean("isValid")
        )
    }

    override fun marshall(value: PublicKey): Field<*> {
        return cdif {
            struct {
                compositeOfPairs("PublicKey") {
                    listOf(
                        "publicKey" to array { value.publicKey.map { uint8(it) } },
                        "signatureAlgorithm" to enum("SignatureAlgorithm", uint8(value.signatureAlgorithm.ordinal)),
                        "isValid" to boolean(value.isValid)
                    )
                }
            }
        }
    }
}

class DeployedContractConverter : CadenceDataInterchangeFormatConverter<DeployedContract> {
    override fun unmarshall(value: Field<*>): DeployedContract = parseCdif(value) {
        DeployedContract(
            name = string("name"),
            code = byteArray("code")
        )
    }
    override fun marshall(value: DeployedContract): Field<*> {
        return cdif {
            struct {
                compositeOfPairs("DeployedContract") {
                    listOf(
                        "name" to string(value.name),
                        "code" to array { value.code.map { uint8(it) } }
                    )
                }
            }
        }
    }
}
