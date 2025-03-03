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
        get("/balance") {
            val balance = TradingViewService.getWalletBalance()
            call.respond(HttpStatusCode.OK, balance)
        }
        get("/orders-count") {
            val count = TradingViewService.getActiveOrdersCount()
            call.respond(HttpStatusCode.OK, count)
        }
        post("/order") {
            val body = call.receive<TradingViewAlert>()
            TradingViewService.placeOrder(body)
            call.respond(HttpStatusCode.OK)
        }
    }
}
