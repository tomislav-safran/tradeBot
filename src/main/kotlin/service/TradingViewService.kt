package com.tsafran.service

import com.tsafran.model.TradingViewAlert
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.ZonedDateTime

val apiKey: String = System.getenv("API_KEY") ?: error("API_KEY is not set")
val apiSecret: String = System.getenv("API_SECRET") ?: error("API_SECRET is not set")
val recvWindow = "5000"
val baseUrl = "https://api.bybit.com"

object TradingViewService {
    suspend fun getWalletBalance(): Double {
        val client = HttpClient()
        val timestamp = ZonedDateTime.now().toInstant().toEpochMilli().toString()

        val queryParams = mapOf("accountType" to "UNIFIED",)
        val queryString = queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }

        // Construct payload for signature
        val payload = "$timestamp$apiKey$recvWindow$queryString"

        // Generate HMAC SHA256 Signature
        val signature = hmacSHA256(payload, apiSecret)

        // Send the GET request
        val response: HttpResponse = client.get("$baseUrl/v5/account/wallet-balance?$queryString") {
            header("X-BAPI-SIGN", signature)
            header("X-BAPI-API-KEY", apiKey)
            header("X-BAPI-TIMESTAMP", timestamp)
            header("X-BAPI-RECV-WINDOW", recvWindow)
        }

        val jsonResponse = response.bodyAsText()
        client.close()

        return Json.parseToJsonElement(jsonResponse)
            .jsonObject["result"]
            ?.jsonObject?.get("list")
            ?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("totalMarginBalance")
            ?.jsonPrimitive?.doubleOrNull ?: 0.0
    }

    // Place Order (Using TradingView Alert Data)
    suspend fun placeOrder(alert: TradingViewAlert) {
        if (getActiveOrdersCount() > 0) throw RuntimeException("Only 1 order is allowed at a time")

        val walletBalance = getWalletBalance()
        val order = convertAlertToOrder(alert, walletBalance) // Convert Alert to Order
        val client = HttpClient()

        val timestamp = ZonedDateTime.now().toInstant().toEpochMilli().toString()
        val json = Json { encodeDefaults = true }
        val jsonBody = json.encodeToString(order)

        // Generate Signature
        val payload = "$timestamp$apiKey$recvWindow$jsonBody"
        val signature = hmacSHA256(payload, apiSecret)

        // Send HTTP request
        val response: HttpResponse = client.post("$baseUrl/v5/order/create") {
            contentType(ContentType.Application.Json)
            header("X-BAPI-API-KEY", apiKey)
            header("X-BAPI-SIGN", signature)
            header("X-BAPI-TIMESTAMP", timestamp)
            header("X-BAPI-RECV-WINDOW", recvWindow)
            setBody(jsonBody)
        }

        println("Order Response: ${response.bodyAsText()}")
        client.close()
    }

    suspend fun getActiveOrdersCount(): Int {
        val client = HttpClient()
        val timestamp = ZonedDateTime.now().toInstant().toEpochMilli().toString()

        val queryParams = mapOf("category" to "spot")
        val queryString = queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }

        // Construct payload for signature
        val payload = "$timestamp$apiKey$recvWindow$queryString"

        // Generate HMAC SHA256 Signature
        val signature = hmacSHA256(payload, apiSecret)

        // Send the GET request
        val response: HttpResponse = client.get("$baseUrl/v5/order/realtime?$queryString") {
            header("X-BAPI-SIGN", signature)
            header("X-BAPI-API-KEY", apiKey)
            header("X-BAPI-TIMESTAMP", timestamp)
            header("X-BAPI-RECV-WINDOW", recvWindow)
        }

        val jsonResponse = response.bodyAsText()
        client.close()

        return Json.parseToJsonElement(jsonResponse)
            .jsonObject["result"]
            ?.jsonObject?.get("list")
            ?.jsonArray?.size ?: 0
    }
}