package com.tsafran.model

import kotlinx.serialization.Serializable

@Serializable
data class OpenAiAlert(
    val entry: Double,
    val limit: Double,
    val stop: Double,
    val isLong: Boolean,
    val certainty: Double
)

@Serializable
data class OpenAiMarketAlert(
    val limit: Double,
    val stop: Double,
    val isLong: Boolean,
    val certainty: Double
)

@Serializable
data class OpenAiTradeValidityResponse(
    val valid: Boolean,
)