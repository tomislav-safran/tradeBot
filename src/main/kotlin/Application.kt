package com.tsafran

import com.tsafran.service.configureCallLogging
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureHTTP()
    configureRouting()
    configureRequestValidation()
    configureCallLogging()
}
