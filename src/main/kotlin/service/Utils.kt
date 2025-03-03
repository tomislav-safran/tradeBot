package com.tsafran.service

import com.tsafran.model.BybitOrder
import com.tsafran.model.TradingViewAlert
import java.math.BigDecimal
import java.math.RoundingMode
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun calculatePositionSize(alert: TradingViewAlert, walletBalance: Double): Double {
    val riskAmountUSD = walletBalance * 0.01 // Risk 1% of balance in USD

    val risk = if (alert.isLong) {
        alert.close - alert.stop
    } else {
        alert.stop - alert.close
    }

    // Adjust for trading fees (0.1% entry + 0.1% exit)
    val totalFees = 0.002 * alert.close

    // Calculate Position Size (in coin)
    val positionSize = riskAmountUSD / (risk + totalFees)

    if (positionSize * alert.close > walletBalance * 9) {
        throw RuntimeException("Leverage is bigger than 10X")
    }

    if (positionSize.equals(0.0)) {
        throw RuntimeException("Position cannot be 0")
    }

    return BigDecimal(positionSize).setScale(3, RoundingMode.HALF_UP).toDouble()
}

fun hmacSHA256(data: String, key: String): String {
    val hmac = Mac.getInstance("HmacSHA256")
    hmac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
    return hmac.doFinal(data.toByteArray()).joinToString("") { "%02x".format(it) }
}

fun convertAlertToOrder(alert: TradingViewAlert, walletBalance: Double): BybitOrder {
    val positionSize = calculatePositionSize(alert, walletBalance)
    val side = if (alert.isLong) "Buy" else "Sell"

    return BybitOrder(
        symbol = alert.coin,
        side = side,
        qty = positionSize.toString(),
        price = alert.close.toString(),
        takeProfit = alert.limit.toString(),
        stopLoss = alert.stop.toString(),
    )
}