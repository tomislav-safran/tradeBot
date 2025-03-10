package com.tsafran.model

import kotlinx.serialization.Serializable

@Serializable
data class WalletResult(val list: List<WalletInfo>)

@Serializable
data class WalletInfo(
    val totalMarginBalance: String,
    val coin: List<CoinInfo>?
)

@Serializable
data class CoinInfo(
    val coin: String,
    val walletBalance: String
)
