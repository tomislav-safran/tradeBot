package com.tsafran.model

import kotlinx.serialization.Serializable

@Serializable
data class PositionResult(val list: List<PositionInfo>?)

@Serializable
data class PositionInfo(
    val symbol: String,
    val side: String,
    val takeProfit: String,
    val stopLoss: String,
    val unrealisedPnl: String,
)