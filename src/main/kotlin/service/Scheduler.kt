package com.tsafran.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime

class Scheduler(private val task: suspend () -> Unit) {
    private val logger = KotlinLogging.logger {}

    private var job: Job? = null

    fun start() {
        if (job != null) return

        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val now = LocalDateTime.now()
                val nextRun = calculateNextRun(now)
                val delayMillis = Duration.between(now, nextRun).toMillis()

                logger.info {"Next execution at: $nextRun (in ${delayMillis / 1000} seconds)"}
                delay(delayMillis) // Wait until the next scheduled time

                try {
                    task()
                } catch (e: Exception) {
                    logger.error {"Error executing scheduled task: ${e.message}"}
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        logger.info { "Scheduler stopped." }
    }

    private fun calculateNextRun(now: LocalDateTime): LocalDateTime {
        val nextQuarter = now
            .withMinute((now.minute / 15 + 1) * 15 % 60)
            .withSecond(1)
            .withNano(0)
            .let { if (now.minute / 15 + 1 > 3) it.plusHours(1) else it }
        return nextQuarter
    }
}