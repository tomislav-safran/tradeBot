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
import java.time.ZoneOffset

class Scheduler() {
    private val logger = KotlinLogging.logger {}
    private var job: Job? = null

    fun start(interval: Duration, task: suspend () -> Unit) {
        if (job != null) return

        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val now = LocalDateTime.now()
                val nextRun = calculateNextRun(now, interval)
                val delayMillis = Duration.between(now, nextRun).toMillis()

                logger.info { "Next execution at: $nextRun (in ${delayMillis / 1000} seconds)" }
                delay(delayMillis)

                try {
                    task()
                } catch (e: Exception) {
                    logger.error { "Error executing scheduled task: ${e.message}" }
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        logger.info { "Scheduler stopped." }
    }

    private fun calculateNextRun(now: LocalDateTime, interval: Duration): LocalDateTime {
        val intervalSeconds = interval.seconds
        val currentEpoch = now.toEpochSecond(ZoneOffset.UTC)
        val nextEpoch = ((currentEpoch / intervalSeconds) + 1) * intervalSeconds
        return LocalDateTime.ofEpochSecond(nextEpoch, 0, ZoneOffset.UTC)
    }
}