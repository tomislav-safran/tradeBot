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
