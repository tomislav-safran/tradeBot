package com.tsafran.model

import kotlinx.serialization.Serializable

@Serializable
data class OpenAiMarketAlert(
    val limit: Double,
    val stop: Double,
    val isLong: Boolean,
    val probability: Double
)