package com.tsafran.model

import kotlinx.serialization.Serializable

@Serializable
data class BybitInstrumentInfo(
    val result: InstrumentInfoResult,
    val retMsg: String?
)

@Serializable
data class InstrumentInfoResult(val list: List<InstrumentInfo>)

@Serializable
data class InstrumentInfo(
    val symbol: String,
    val baseCoin: String,
    val quoteCoin: String,
    val lotSizeFilter: LotSizeFilter,
    val priceFilter: PriceFilter
)

@Serializable
data class LotSizeFilter (
    val basePrecision: String,
    val quotePrecision: String,
)

@Serializable
data class PriceFilter (
    val tickSize: String
)

data class MaxDecimalsDTO(
    val lotDecimals: Int,
    val priceDecimals: Int
)