package com.tsafran.service

import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.core.JsonValue
import com.openai.models.ChatModel
import com.openai.models.ResponseFormatJsonSchema
import com.openai.models.ResponseFormatJsonSchema.JsonSchema
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.tsafran.Constants
import com.tsafran.model.GptPositionValidationResponse
import com.tsafran.model.GptSchedulerCommand
import com.tsafran.model.HistoricCandlesResult
import com.tsafran.model.OpenAiMarketAlert
import com.tsafran.model.OrderAlert
import com.tsafran.model.OrderInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Map

private val openAiApiKey: String = System.getenv("OPEN_AI_API_KEY") ?: error("OPEN_AI_API_KEY is not set")
private val openAIClient = OpenAIOkHttpClient.builder()
    .apiKey(openAiApiKey)
    .responseValidation(true)
    .build()

private val logger = KotlinLogging.logger {}

object OpenAIService {
    private fun getGPTCompletion(devMessage: String, userMessage: String, responseSchema: JsonSchema.Schema): String? {
        val createParams: ChatCompletionCreateParams = ChatCompletionCreateParams.builder()
            .model(ChatModel.O3_MINI)
            .maxCompletionTokens(2048)
            .responseFormat(ResponseFormatJsonSchema.builder()
                .jsonSchema(JsonSchema.builder()
                    .name("order")
                    .schema(responseSchema)
                    .strict(true)
                    .build())
                .build())
            .addSystemMessage(devMessage)
            .addUserMessage(userMessage)
            .build()

        val completions = openAIClient.chat().completions().create(createParams)

        val completionContent = completions.choices().firstOrNull()?.message()?.content()?.orElse(null)

        if (completionContent == null || completionContent.isEmpty()) {
            logger.error { "GPT did not return any valid content." }
            return null
        }

        logger.info { "GPT response: $completionContent" }

        return completionContent
    }

    private fun getAiOrderSuggestion(candles: HistoricCandlesResult, devMessageOverride: String?): OpenAiMarketAlert {
        val structuredResponseSchema: JsonSchema.Schema = JsonSchema.Schema.builder()
            .putAdditionalProperty("type", JsonValue.from("object"))
            .putAdditionalProperty(
                "properties", JsonValue.from(
                    Map.of(
                        "limit", Map.of("type", "number"),
                        "stop", Map.of("type", "number"),
                        "isLong", Map.of("type", "boolean"),
                        "probability", Map.of("type", "number")
                    )
                )
            )
            .putAdditionalProperty("required", JsonValue.from(listOf("limit", "stop", "isLong", "probability")))
            .putAdditionalProperty("additionalProperties", JsonValue.from(false))
            .build()

        val ohlcv = mapCandleResultToOhlcv(candles)
        val ema = calculateEMA(ohlcv.map { candle -> candle.close.toDouble() })
        val ema50 = if (ohlcv.size >= 50) calculateEMA(ohlcv.take(50).map { candle -> candle.close.toDouble() }) else 0.0

        val userMessage = """
            EMA${candles.list.size}: ${BigDecimal(ema).setScale(2, RoundingMode.HALF_UP)},
            ${if (ema50 != 0.0) "EMA50: ${BigDecimal(ema50).setScale(2, RoundingMode.HALF_UP)}," else ""}
            Last ${ohlcv.size} candles: (format: [open, high, low, close, volume]), first candle is the latest one.
            ${ohlcv.toString().replace("\"","")}
        """.trimIndent()

        val devMessage = devMessageOverride?.trimIndent() ?: Constants.GPT_ORDER_DEV_MESSAGE

        val completionContent = getGPTCompletion(devMessage, userMessage, structuredResponseSchema)

        val json = Json {
            ignoreUnknownKeys = true
        }

        completionContent?.let {
            return json.decodeFromString<OpenAiMarketAlert>(completionContent)
        }

        return OpenAiMarketAlert(0.0, 0.0, false, 0.0)
    }

    private fun validateOpenPosition(candles: HistoricCandlesResult, devMessageOverride: String?, position: OrderInfo): GptPositionValidationResponse {
        val structuredResponseSchema: JsonSchema.Schema = JsonSchema.Schema.builder()
            .putAdditionalProperty("type", JsonValue.from("object"))
            .putAdditionalProperty(
                "properties", JsonValue.from(
                    Map.of(
                        "closePosition", Map.of("type", "boolean"),
                    )
                )
            )
            .putAdditionalProperty("required", JsonValue.from(listOf("closePosition")))
            .putAdditionalProperty("additionalProperties", JsonValue.from(false))
            .build()

        val ohlcv = mapCandleResultToOhlcv(candles)
        val ema = calculateEMA(ohlcv.map { candle -> candle.close.toDouble() })

        val userMessage = """
            Open position info: price = ${position.price}, side = ${position.side}, take profit = ${position.takeProfit}, stop loss = ${position.stopLoss},
            EMA${candles.list.size}: ${BigDecimal(ema).setScale(2, RoundingMode.HALF_UP)},
            Last ${ohlcv.size} candles: (format: [open, high, low, close, volume]), first candle is the latest one.
            ${ohlcv.toString().replace("\"","")}
        """.trimIndent()

        val devMessage = devMessageOverride?.trimIndent() ?: Constants.GPT_ORDER_VALIDATION_DEV_MESSAGE

        val completionContent = getGPTCompletion(devMessage, userMessage, structuredResponseSchema)

        val json = Json {
            ignoreUnknownKeys = true
        }

        completionContent?.let {
            return json.decodeFromString<GptPositionValidationResponse>(completionContent)
        }

        return GptPositionValidationResponse(false)
    }

    suspend fun placeAIOrder(schedulerCommand: GptSchedulerCommand) {
        for (symbol in schedulerCommand.symbols) {
            val activeOrderResult = BybitService.getActiveOrder("linear", symbol).result

            // active position exists for symbol
            if ((activeOrderResult.list?.size ?: 0) > 0) {
                if (schedulerCommand.validateOpenPositions) {
                    logger.info { "Validating $symbol position" }
                    val order = activeOrderResult.list!![0]
                    val candles = BybitService.getHistoricCandles(symbol, schedulerCommand.intervalMinutes.toString(), schedulerCommand.validationCandleLookBack, "linear").result
                    val gptResponse = validateOpenPosition(candles, schedulerCommand.validationDevMessageOverride, order)
                    if (gptResponse.closePosition) {
                        BybitService.closePosition(order)
                    }
                }

                logger.info { "Skipping $symbol order" }
                continue
            }

            logger.info { "Preparing $symbol order" }

            val candles = BybitService.getHistoricCandles(symbol, schedulerCommand.intervalMinutes.toString(), schedulerCommand.candleLookBack, "linear").result
            val gptResponse = getAiOrderSuggestion(candles, schedulerCommand.devMessageOverride)

            if (gptResponse.probability >= schedulerCommand.probabilityThreshold) {
                val alert = OrderAlert(
                    coin = symbol,
                    close = candles.list[0][4].toDouble(),
                    stop = gptResponse.stop,
                    limit = gptResponse.limit,
                    isLong = gptResponse.isLong,
                    useTrailingStop = schedulerCommand.useTrailingStop
                )

                BybitService.placeFutureMarketTpSlOrder(alert)
            }
        }
    }
}