package com.tsafran

import com.tsafran.model.TradingViewAlert
import com.tsafran.service.TradingViewService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, "OK")
        }
        get("/historic-candles/{symbol}") {
            val symbol = call.parameters["symbol"]!!
            val interval = call.request.queryParameters["interval"]!!
            val limit = call.request.queryParameters["limit"]!!

            val response = TradingViewService.getHistoricCandles(symbol, interval, limit)
            call.respond(response)

        }
        post("/order") {
            val body = call.receive<TradingViewAlert>()
            TradingViewService.placeOrder(body)
            call.respond(HttpStatusCode.OK)
        }
    }
}
