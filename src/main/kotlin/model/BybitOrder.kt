package com.tsafran.model

import kotlinx.serialization.Serializable

@Serializable
data class BybitOrder(
    val category: String? = "spot",
    val symbol: String,
    val isLeverage: Int = 1,
    val side: String,
    val orderType: String = "Limit",
    val qty: String,
    val marketUnit: String = "baseCoin",
    val price: String? = null,
    val timeInForce: String = "GTC",
    val takeProfit: String? = null,
    val stopLoss: String? = null,
    val tpslMode: String? = null,
    val tpOrderType: String = "Market", // TP Order Type (Limit or Market)
    val slOrderType: String = "Market", // SL Order Type (Limit or Market)
)

@Serializable
data class BybitBatchOrder (
    val category: String = "spot",
    val request: List<BybitOrder>
)
