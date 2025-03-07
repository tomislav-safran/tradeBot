package com.tsafran.model

import kotlinx.serialization.Serializable

@Serializable
data class BybitOrderInfo(
    val result: OrderResult?,
    val retMsg: String?
)

@Serializable
data class OrderResult(val list: List<OrderInfo>)

@Serializable
data class OrderInfo(
    val symbol: String
)