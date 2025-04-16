package com.tsafran.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderResult(val list: List<OrderInfo>?)

@Serializable
data class OrderInfo(
    val symbol: String,
    val qty: String,
    val price: String,
    val side: String,
    val takeProfit: String,
    val stopLoss: String,
)