package com.nftco.flow.sdk.crypto

import java.security.PrivateKey

data class PrivateKey(
    val key: PrivateKey,
    val ecCoupleComponentSize: Int,
    val hex: String
)
