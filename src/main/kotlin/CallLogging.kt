package com.tsafran

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging

fun Application.configureCallLogging() {
    install(CallLogging) {}
}