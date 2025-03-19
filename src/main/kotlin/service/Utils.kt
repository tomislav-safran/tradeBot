package com.tsafran.service

import com.tsafran.model.BybitOrder
import com.tsafran.model.InstrumentInfo
import com.tsafran.model.LinearInstrumentInfo
import com.tsafran.model.MaxDecimalsDTO
import com.tsafran.model.OrderAlert
import java.math.BigDecimal
import java.math.RoundingMode
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun calculatePositionSize(alert: OrderAlert, walletBalance: Double, lotDecimals: Int): Double {
    val riskAmountUSD = walletBalance * 0.01 // Risk 1% of balance in USD

    val risk = if (alert.isLong) {
        alert.close - alert.stop
    } else {
        alert.stop - alert.close
    }

    // Calculate Position Size (in coin)
    val positionSize = riskAmountUSD / risk

    if (positionSize.equals(0.0)) {
        error("Position cannot be 0")
    }

    return BigDecimal(positionSize).setScale(lotDecimals, RoundingMode.HALF_UP).toDouble()
}

fun hmacSHA256(data: String, key: String): String {
    val hmac = Mac.getInstance("HmacSHA256")
    hmac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
    return hmac.doFinal(data.toByteArray()).joinToString("") { "%02x".format(it) }
}

fun convertAlertToLimitTpSlOrder(alert: OrderAlert, walletBalance: Double, maxDecimals: MaxDecimalsDTO): BybitOrder {
    val positionSize = calculatePositionSize(alert, walletBalance, maxDecimals.lotDecimals)
    val side = if (alert.isLong) "Buy" else "Sell"

    val price = BigDecimal(alert.close).setScale(maxDecimals.priceDecimals, RoundingMode.HALF_UP).toString()
    val tp = BigDecimal(alert.limit).setScale(maxDecimals.priceDecimals, RoundingMode.HALF_UP).toString()
    val sl = BigDecimal(alert.stop).setScale(maxDecimals.priceDecimals, RoundingMode.HALF_UP).toString()

    return BybitOrder(
        symbol = alert.coin,
        side = side,
        qty = positionSize.toString(),
        price = price,
        takeProfit = tp,
        stopLoss = sl,
    )
}

fun convertAlertToFutureMarketTpSlOrder(alert: OrderAlert, walletBalance: Double, maxDecimals: MaxDecimalsDTO): BybitOrder {
    val positionSize = calculatePositionSize(alert, walletBalance, maxDecimals.lotDecimals)
    val side = if (alert.isLong) "Buy" else "Sell"

    val tp = BigDecimal(alert.limit).setScale(maxDecimals.priceDecimals, RoundingMode.HALF_UP).toString()
    val sl = BigDecimal(alert.stop).setScale(maxDecimals.priceDecimals, RoundingMode.HALF_UP).toString()

    return BybitOrder(
        category = "linear",
        orderType = "Market",
        tpslMode = "Full",
        symbol = alert.coin,
        side = side,
        qty = positionSize.toString(),
        takeProfit = tp,
        stopLoss = sl,
    )
}

fun getMaxDecimalsForSymbol(instrument: InstrumentInfo?): MaxDecimalsDTO {
    instrument?.let {
        val basePrecision = instrument.lotSizeFilter.basePrecision
        val tickSize = instrument.priceFilter.tickSize

        return MaxDecimalsDTO(
            lotDecimals = BigDecimal(basePrecision).stripTrailingZeros().scale(),
            priceDecimals = BigDecimal(tickSize).stripTrailingZeros().scale()
        )
    }
    error("Instrument not found")
}

fun getMaxDecimalsForLinearSymbol(instrument: LinearInstrumentInfo?): MaxDecimalsDTO {
    instrument?.let {
        val basePrecision = instrument.lotSizeFilter.qtyStep
        val tickSize = instrument.priceFilter.tickSize

        return MaxDecimalsDTO(
            lotDecimals = BigDecimal(basePrecision).stripTrailingZeros().scale(),
            priceDecimals = BigDecimal(tickSize).stripTrailingZeros().scale()
        )
    }
    error("Instrument not found")
}