package com.tsafran.service

import com.tsafran.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.time.ZonedDateTime
import kotlin.Double

private val apiKey: String = System.getenv("BYBIT_API_KEY") ?: error("BYBIT_API_KEY is not set")
private val apiSecret: String = System.getenv("BYBIT_API_SECRET") ?: error("BYBIT_API_SECRET is not set")
private val recvWindow = "5000"
private val baseUrl = "https://api.bybit.com"

private val logger = KotlinLogging.logger {}

private val client = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

object BybitService {
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

    private suspend fun getWalletBalance(): Double {
        val queryParams = "accountType=UNIFIED"
        val response: BybitApiResponse<WalletResult> = client.get("$baseUrl/v5/account/wallet-balance?$queryParams") {
            authHeaders(queryParams).forEach { (key, value) -> header(key, value) }
        }.body()

        logger.info { response.retMsg }
        return response.result.list.firstOrNull()?.totalMarginBalance?.toDoubleOrNull() ?: 0.0
    }

    // Place Order (Using TradingView Alert Data)
    suspend fun placeLimitTpSlOrder(alert: OrderAlert) {
        if (getActiveOrdersCount() > 0) throw RuntimeException("Only 1 order is allowed at a time")

        val instrumentInfo = getSpotInstrumentInfo(alert.coin)
        val maxDecimals = getMaxDecimalsForSymbol(instrumentInfo.result.list.firstOrNull())

        val walletBalance = getWalletBalance()
        val order = convertAlertToLimitTpSlOrder(alert, walletBalance, maxDecimals)
        val jsonBody = json.encodeToString(order)

        val response = placeOrder(jsonBody)

        val responseBody = response.bodyAsText()

        logger.info { "Order: $order" }
        logger.info { "Order Response: $responseBody" }
    }

    suspend fun placeFutureMarketTpSlOrder(alert: OrderAlert) {
        if (getActiveOrdersCount("linear", alert.coin) > 0) throw RuntimeException("Only 1 order is allowed at a time")

        val instrumentInfo = getLinearInstrumentInfo(alert.coin)
        val maxDecimals = getMaxDecimalsForLinearSymbol(instrumentInfo.result.list.firstOrNull())

        val walletBalance = getWalletBalance()
        val order = convertAlertToFutureMarketTpSlOrder(alert, walletBalance, maxDecimals)
        val jsonBody = json.encodeToString(order)

        val response = placeOrder(jsonBody)

        if (alert.useTrailingStop) {
            setTrailingStop(alert)
        }

        val responseBody = response.bodyAsText()

        logger.info { "Order: $order" }
        logger.info { "Order Response: $responseBody" }
    }

    suspend fun closePosition(activeOrder: PositionInfo) {
        val order = BybitCancelOrder(
            symbol = activeOrder.symbol,
            side = if (activeOrder.side == "Buy") "Sell" else "Buy"
        )
        val jsonBody = json.encodeToString(order)
        placeOrder(jsonBody)
    }

    private suspend fun placeOrder(body: String): HttpResponse {
        return client.post("$baseUrl/v5/order/create") {
            contentType(ContentType.Application.Json)
            authHeaders("", body).forEach { (key, value) -> header(key, value) }
            setBody(body)
        }
    }

    private suspend fun setTrailingStop(alert: OrderAlert): HttpResponse {
        val close = alert.close.toBigDecimal()
        val stop = alert.stop.toBigDecimal()

        val slDistance = close.subtract(stop).abs()
        //val trailingStopTrigger = if (alert.isLong) close.add(slDistance) else close.subtract(slDistance)

        val tradingStopOrder = BybitTradingStopOrder(
            symbol = alert.coin,
            trailingStop = slDistance.toString()
        )

        val body = json.encodeToString(tradingStopOrder)

        return client.post("$baseUrl/v5/position/trading-stop") {
            contentType(ContentType.Application.Json)
            authHeaders("", body).forEach { (key, value) -> header(key, value) }
            setBody(body)
        }
    }

    suspend fun getActiveOrdersCount(category: String = "spot", symbol: String? = null): Int {
        val response = getActiveOrder(category, symbol)

        logger.info { response.retMsg }
        return response.result.list?.size ?: 0
    }

    suspend fun getActiveOrder(category: String = "spot", symbol: String? = null): BybitApiResponse<OrderResult> {
        var queryParams = "category=$category"
        queryParams += symbol?.let { "&symbol=$symbol" }

        return client.get("$baseUrl/v5/order/realtime?$queryParams") {
            authHeaders(queryParams).forEach { (key, value) -> header(key, value) }
        }.body()
    }

    suspend fun getActivePosition(category: String = "linear", symbol: String? = null): BybitApiResponse<PositionResult> {
        var queryParams = "category=$category"
        queryParams += symbol?.let { "&symbol=$symbol" }

        return client.get("$baseUrl/v5/position/list?$queryParams") {
            authHeaders(queryParams).forEach { (key, value) -> header(key, value) }
        }.body()
    }

    private suspend fun getSpotInstrumentInfo(symbol: String): BybitApiResponse<InstrumentInfoResult> {
        return getInstrumentInfo(symbol, "spot").body()
    }

    private suspend fun getLinearInstrumentInfo(symbol: String): BybitApiResponse<LinearInstrumentInfoResult> {
        return getInstrumentInfo(symbol, "linear").body()
    }

    private suspend fun getInstrumentInfo(symbol: String, category: String): HttpResponse {
        val queryParams = "category=$category&symbol=$symbol"
        return client.get("$baseUrl/v5/market/instruments-info?$queryParams") {
            authHeaders(queryParams).forEach { (key, value) -> header(key, value) }
        }.body()
    }

    suspend fun getHistoricCandles(
        symbol: String,
        interval: String,
        limit: String,
        category: String = "spot",
    ): BybitApiResponse<HistoricCandlesResult> {
        return client.get("$baseUrl/v5/market/kline") {
            url {
                parameters.append("category", category)
                parameters.append("symbol", symbol)
                parameters.append("interval", interval)
                parameters.append("limit", limit)
            }
        }.body()
    }
}