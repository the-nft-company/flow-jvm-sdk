package com.nftco.flow.sdk

import kotlinx.serialization.Serializable
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class JsonCadenceTest {

    @Serializable
    data class StorageInfo(
        val capacity: Int,
        val used: Int,
        val available: Int
    )

    @Serializable
    data class StorageInfoComplex(
        val capacity: ULong,
        val used: ULong,
        val available: ULong,
        val foo: Foo
    )

    @Serializable
    data class Foo(
        val bar: Int,
    )

    val flow = TestUtils.newMainnetAccessApi()

    @Test
    fun `Can parse new JSON Cadence`() {
        val tx = flow.getTransactionResultById(FlowId("663869d910278d7b6caf793396f6f2c5b91aace7180c2c70cfb3b0b6efd7a049"))!!
        val events = tx.events.map { it.payload.jsonCadence }
        Assertions.assertThat(events).hasSize(4)
    }

    @Test
    fun decodeOptional() {
        val result = flow.simpleFlowScript {
            script {
                """
                pub fun main(): Bool? {
                 return nil
                }
                """.trimIndent()
            }
        }
        val data = result.jsonCadence.decode<Boolean?>()
        Assertions.assertThat(data).isEqualTo(null)
    }

    @Test
    fun decodeOptional2() {
        val result = flow.simpleFlowScript {
            script {
                """
                pub fun main(): Bool? {
                 return true
                }
                """.trimIndent()
            }
        }
        val data = result.jsonCadence.decode<Boolean?>()
        Assertions.assertThat(data).isEqualTo(true)
    }

    @Test
    fun decodeBoolean() {
        val result = flow.simpleFlowScript {
            script {
                """
                pub fun main(): Bool {
                 return true
                }
                """.trimIndent()
            }
        }
        val data = result.jsonCadence.decode<Boolean>()
        Assertions.assertThat(data).isEqualTo(true)
    }

    @Test
    fun decodeArray() {
        val result = flow.simpleFlowScript {
            script {
                """
                pub fun main(): [UInt64] {
                 return [1,3,4,5]
                }
                """.trimIndent()
            }
        }

        val data = result.jsonCadence.decode<List<ULong>>()
        Assertions.assertThat(data.first()).isEqualTo(1UL)
        Assertions.assertThat(data).hasSize(4)
    }

    @Test
    fun decodeUFix64() {
        val result = flow.simpleFlowScript {
            script {
                """
                pub fun main(): UFix64 {
                 return 0.789111
                }
                """.trimIndent()
            }
        }

        val data = result.jsonCadence.decode<Double>()
        Assertions.assertThat(data).isEqualTo(0.789111)
    }

    @Test
    fun decodeStruct() {
        val result = flow.simpleFlowScript {
            script {
                """
                  pub struct StorageInfo {
                      pub let capacity: Int
                      pub let used: Int
                      pub let available: Int

                      init(capacity: Int, used: Int, available: Int) {
                          self.capacity = capacity
                          self.used = used
                          self.available = available
                      }
                  }

                  pub fun main(addr: Address): [StorageInfo] {
                    let acct = getAccount(addr)
                    return [StorageInfo(capacity: 1,
                                       used: 2,
                                       available: 3)]
                  }
                """.trimIndent()
            }

            arg { address("0x84221fe0294044d7") }
        }

        val data = result.jsonCadence.decode<List<StorageInfo>>().first()
        Assertions.assertThat(data.capacity).isEqualTo(1)
        Assertions.assertThat(data.used).isEqualTo(2)
        Assertions.assertThat(data.available).isEqualTo(3)
    }

    @Test
    fun decodeComplexDict() {
        val result = flow.simpleFlowScript {
            script {
                """
                            pub struct StorageInfo {
                                pub let capacity: UInt64
                                pub let used: UInt64
                                pub let available: UInt64
                                pub let foo: Foo

                                init(capacity: UInt64, used: UInt64, available: UInt64, foo: Foo) {
                                    self.capacity = capacity
                                    self.used = used
                                    self.available = available
                                    self.foo = foo
                                }
                            }
                            
                            pub struct Foo {
                                pub let bar: Int

                                    init(bar: Int) {
                                        self.bar = bar
                                    }
                            }

                            pub fun main(addr: Address): {String: [StorageInfo]} {
                              let acct = getAccount(addr)
                              
                               let foo = Foo(bar: 1)
                              return {"test": [StorageInfo(capacity: acct.storageCapacity,
                                                used: acct.storageUsed,
                                                available: acct.storageCapacity - acct.storageUsed,
                                                foo: foo)]}
                            }
                """.trimIndent()
            }

            arg { address("0x84221fe0294044d7") }
        }

        val data = result.decode<Map<String, List<StorageInfoComplex>>>()
        Assertions.assertThat(data["test"]!!.first().foo.bar).isEqualTo(1)
    }
}
