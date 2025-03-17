package com.tsafran.model

import kotlinx.serialization.Serializable

@Serializable
data class InstrumentInfoResult(val list: List<InstrumentInfo>)

@Serializable
data class LinearInstrumentInfoResult(val list: List<LinearInstrumentInfo>)

@Serializable
data class InstrumentInfo(
    val symbol: String,
    val baseCoin: String,
    val quoteCoin: String,
    val lotSizeFilter: LotSizeFilter,
    val priceFilter: PriceFilter
)

@Serializable
data class LinearInstrumentInfo(
    val symbol: String,
    val baseCoin: String,
    val quoteCoin: String,
    val lotSizeFilter: LinearLotSizeFilter,
    val priceFilter: PriceFilter
)

@Serializable
data class LotSizeFilter (
    val basePrecision: String,
    val quotePrecision: String,
)

@Serializable
data class LinearLotSizeFilter (
    val qtyStep: String,
)

@Serializable
data class PriceFilter (
    val tickSize: String
)

data class MaxDecimalsDTO(
    val lotDecimals: Int,
    val priceDecimals: Int
)