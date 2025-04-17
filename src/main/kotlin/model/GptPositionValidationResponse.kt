package com.tsafran.model

import kotlinx.serialization.Serializable

@Serializable
data class GptPositionValidationResponse(
    val closePosition: Boolean,
)
