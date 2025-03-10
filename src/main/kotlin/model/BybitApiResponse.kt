package com.tsafran.model

import kotlinx.serialization.Serializable

@Serializable
data class BybitApiResponse<T>(
    val result: T,
    val retMsg: String
)
