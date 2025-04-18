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
data class BybitCancelOrder(
    val category: String = "linear",
    val symbol: String,
    val side: String,
    val orderType: String = "Market",
    val qty: String = "0",
    val reduceOnly: Boolean = true,
    val closeOnTrigger: Boolean = true,
)

@Serializable
data class BybitTradingStopOrder(
    val category: String = "linear",
    val symbol: String,
    val trailingStop: String,
    val activePrice: String? = null,
    val tpslMode: String = "Full",
    val positionIdx: Int = 0,
)

@Serializable
data class BybitBatchOrder (
    val category: String = "spot",
    val request: List<BybitOrder>
)
