package com.tsafran.model

data class OHLCV(
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String,
) {
    override fun toString(): String {
        return "[$open,$high,$low,$close,$volume]"
    }
}

