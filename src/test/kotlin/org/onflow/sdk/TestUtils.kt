package org.onflow.sdk

import org.onflow.sdk.crypto.Crypto

object TestUtils {

    fun newEmulatorAccessApi(): FlowAccessApi = Flow.newAccessApi("localhost", 3570)

    fun newMainnetAccessApi(): FlowAccessApi = Flow.newAccessApi(MAINNET_HOSTNAME)

    fun newTestnetAccessApi(): FlowAccessApi = Flow.newAccessApi(TESTNET_HOSTNAME)

    val MAINNET_HOSTNAME = "access.mainnet.nodes.onflow.org"
    val TESTNET_HOSTNAME = "access.devnet.nodes.onflow.org"

    // the stuff below needs to match what is in flow/flow.json

    val MAIN_ACCOUNT_ADDRESS = FlowAddress("f8d6e0586b0a20c7")

    val MAIN_ACCOUNT_PUBLIC_KEY = Crypto.decodePublicKey(
        "fc3dc7555aa9b7c97418302f7b96c0f69dca269aae8b282948180e48ea6f37879fb42e7d65e9962a1736768a27eda76d1694786036c6f66af437a76dda3f8a98",
        SignatureAlgorithm.ECDSA_SECP256k1
    )

    val MAIN_ACCOUNT_PRIVATE_KEY = Crypto.decodePrivateKey(
        "bded6524495e2b19c10352ac76990886940cbb5f2eb2c61aaeabb760f62d5f9c",
        SignatureAlgorithm.ECDSA_SECP256k1
    )

    val MAIN_ACCOUNT_SIGNER = Crypto.getSigner(MAIN_ACCOUNT_PRIVATE_KEY, HashAlgorithm.SHA3_256)
}
