package com.tsafran.service

import com.tsafran.model.BybitInstrumentInfo
import com.tsafran.model.BybitOrderInfo
import com.tsafran.model.BybitWalletInfo
import com.tsafran.model.TradingViewAlert
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.time.ZonedDateTime
import kotlin.Double

val apiKey: String = System.getenv("API_KEY") ?: error("API_KEY is not set")
val apiSecret: String = System.getenv("API_SECRET") ?: error("API_SECRET is not set")
val recvWindow = "5000"
val baseUrl = "https://api.bybit.com"

val client = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys=true })
    }
}

object TradingViewService {
    private val json = Json { encodeDefaults = true }

    private fun currentTimestamp(): String =
        ZonedDateTime.now().toInstant().toEpochMilli().toString()

    private fun generateSignature(payload: String): String =
        hmacSHA256(payload, apiSecret)

    private fun authHeaders(queryString: String, body: String? = null): Map<String, String> {
        val timestamp = currentTimestamp()
        val payload = "$timestamp$apiKey$recvWindow$queryString${body ?: ""}"
        val signature = generateSignature(payload)

        return mapOf(
            "X-BAPI-SIGN" to signature,
            "X-BAPI-API-KEY" to apiKey,
            "X-BAPI-TIMESTAMP" to timestamp,
            "X-BAPI-RECV-WINDOW" to recvWindow
        )
    }

    suspend fun getWalletBalance(): Double {
        val queryParams = "accountType=UNIFIED"
        val response: BybitWalletInfo = client.get("$baseUrl/v5/account/wallet-balance?$queryParams") {
            authHeaders(queryParams).forEach { (key, value) -> header(key, value) }
        }.body()

        println(response.retMsg)
        return response.result?.list?.firstOrNull()?.totalMarginBalance?.toDoubleOrNull() ?: 0.0
    }

    // Place Order (Using TradingView Alert Data)
    suspend fun placeOrder(alert: TradingViewAlert) {
        if (getActiveOrdersCount() > 0) throw RuntimeException("Only 1 order is allowed at a time")

        val instrumentInfo = getInstrumentInfo(alert.coin)
        val maxDecimals = getMaxDecimalsForSymbol(instrumentInfo.result.list.firstOrNull())

        val walletBalance = getWalletBalance()
        val order = convertAlertToOrder(alert, walletBalance, maxDecimals)
        val jsonBody = json.encodeToString(order)

        val response: HttpResponse = client.post("$baseUrl/v5/order/create") {
            contentType(ContentType.Application.Json)
            authHeaders("", jsonBody).forEach { (key, value) -> header(key, value) }
            setBody(jsonBody)
        }

        println("Order Response: ${response.bodyAsText()}")
    }

    suspend fun getActiveOrdersCount(): Int {
        val queryParams = "category=spot"
        val response: BybitOrderInfo = client.get("$baseUrl/v5/order/realtime?$queryParams") {
            authHeaders(queryParams).forEach { (key, value) -> header(key, value) }
        }.body()

        println(response.retMsg)
        return response.result?.list?.size ?: 0
    }

    suspend fun getInstrumentInfo(symbol: String): BybitInstrumentInfo {
        val queryParams = "category=spot&symbol=$symbol"
        return client.get("$baseUrl/v5/market/instruments-info?$queryParams") {
            authHeaders(queryParams).forEach { (key, value) -> header(key, value) }
        }.body()
    }
}