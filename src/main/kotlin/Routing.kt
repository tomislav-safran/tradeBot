package com.tsafran

import com.tsafran.model.OrderAlert
import com.tsafran.service.OpenAIService
import com.tsafran.service.OpenAIService.placeAIOrder
import com.tsafran.service.Scheduler
import com.tsafran.service.BybitService
import com.tsafran.service.BybitService.getActiveOrdersCount
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val gptOrderScheduler = Scheduler(::placeAIOrder)

    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, "OK")
        }
        post("/spot/order") {
            val body = call.receive<OrderAlert>()
            BybitService.placeLimitTpSlOrder(body)
            call.respond(HttpStatusCode.OK)
        }
        post("/linear/order") {
            val body = call.receive<OrderAlert>()
            BybitService.placeFutureMarketTpSlOrder(body)
            call.respond(HttpStatusCode.OK)
        }
        post("/linear/order/ai") {
            val body = call.receive<OrderAlert>()
            if (OpenAIService.verifyTradeWithAI(body, "15", "90", "linear")) {
                BybitService.placeFutureMarketTpSlOrder(body)
                call.respond(HttpStatusCode.OK)
            }
            call.respond(HttpStatusCode.OK, "AI deemed trade to be invalid")
        }
        post("/start-gpt-trader") {
            gptOrderScheduler.start()
            call.respondText("Scheduler started")
        }

        post("/stop-gpt-trader") {
            gptOrderScheduler.stop()
            call.respondText("Scheduler stopped")
        }
    }
}
