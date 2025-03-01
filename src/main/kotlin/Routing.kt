package com.tsafran

import com.tsafran.service.TradingViewService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.receiveText
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        post("/webhook") {
            val body = call.receiveText()
            TradingViewService.processWebhook(body)
            call.respond(HttpStatusCode.OK)
        }
    }
}
