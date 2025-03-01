package com.tsafran.service

object TradingViewService {
    fun processWebhook(body: String) {
        println("Processing webhook with body: $body")
    }
}