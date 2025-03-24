package com.tsafran.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderAlert(
    val coin: String,
    val close: Double,
    val limit: Double,
    val stop: Double,
    val isLong: Boolean,
    val useTrailingStop: Boolean = false,
)

@Serializable
data class GptSchedulerCommand(
    val symbols: List<String>,
    val candleLookBack: String,
    val certaintyThreshold: Int,
    val intervalMinutes: Long,
    val useTrailingStop: Boolean
)
