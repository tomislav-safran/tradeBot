package com.tsafran.service

import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.core.JsonValue
import com.openai.models.ChatModel
import com.openai.models.ResponseFormatJsonSchema
import com.openai.models.ResponseFormatJsonSchema.JsonSchema
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.tsafran.Constants
import com.tsafran.model.GptSchedulerCommand
import com.tsafran.model.HistoricCandlesResult
import com.tsafran.model.OpenAiMarketAlert
import com.tsafran.model.OpenAiTradeValidityResponse
import com.tsafran.model.OrderAlert
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
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
            .model(ChatModel.GPT_4_5_PREVIEW)
            .maxCompletionTokens(2048)
            .responseFormat(ResponseFormatJsonSchema.builder()
                .jsonSchema(JsonSchema.builder()
                    .name("order")
                    .schema(responseSchema)
                    .build())
                .build())
            .addDeveloperMessage(devMessage)
            .addUserMessage(Json.encodeToString(userMessage))
            .build()

        val completions = openAIClient.chat().completions().create(createParams)

        val completionContent = completions.choices().firstOrNull()?.message()?.content()?.orElse(null)

        if (completionContent == null) {
            error("GPT did not return any valid content.")
        }

        logger.info { "GPT response: $completionContent" }

        return completionContent
    }

    private fun getAiOrderSuggestion(candles: HistoricCandlesResult): OpenAiMarketAlert {
        val structuredResponseSchema: JsonSchema.Schema = JsonSchema.Schema.builder()
            .putAdditionalProperty("type", JsonValue.from("object"))
            .putAdditionalProperty(
                "properties", JsonValue.from(
                    Map.of(
                        "limit", Map.of("type", "number"),
                        "stop", Map.of("type", "number"),
                        "isLong", Map.of("type", "boolean"),
                        "certainty", Map.of("type", "number")
                    )
                )
            )
            .build()

        val userMessage = """
            Last 90 candles: (format: [timestamp, open, high, low, close, volume, turnover])
            ${Json.encodeToString(candles)}
        """

        val completionContent = getGPTCompletion(Constants.GPT_ORDER_DEV_MESSAGE, userMessage, structuredResponseSchema)

        val json = Json {
            ignoreUnknownKeys = true
        }

        return json.decodeFromString<OpenAiMarketAlert>(completionContent!!)
    }

    suspend fun placeAIOrder(schedulerCommand: GptSchedulerCommand) {
        for (symbol in schedulerCommand.symbols) {
            if (BybitService.getActiveOrdersCount("linear", symbol) > 0) {
                logger.info { "Skipping $symbol order" }
                break
            }

            val candles = BybitService.getHistoricCandles(symbol, "15", schedulerCommand.candleLookBack, "linear").result
            val gptResponse = getAiOrderSuggestion(candles)

            if (gptResponse.certainty >= schedulerCommand.certaintyThreshold) {
                val alert = OrderAlert(
                    coin = symbol,
                    close = candles.list[0][4].toDouble(),
                    stop = gptResponse.stop,
                    limit = gptResponse.limit,
                    isLong = gptResponse.isLong
                )

                BybitService.placeFutureMarketTpSlOrder(alert)
            }
        }
    }


    suspend fun verifyTradeWithAI(alert: OrderAlert, timeframe: String, candleLookBackPeriod: String, category: String): Boolean {
        val candles = BybitService.getHistoricCandles(alert.coin, timeframe, candleLookBackPeriod, category).result

        val structuredResponseSchema: JsonSchema.Schema = JsonSchema.Schema.builder()
            .putAdditionalProperty("type", JsonValue.from("object"))
            .putAdditionalProperty(
                "properties", JsonValue.from(
                    Map.of("valid", Map.of("type", "boolean"))
                )
            ).build()

        val userMessage = """
            Trade signal: {
                close price: ${alert.close}
                stop price: ${alert.stop}
                limit price: ${alert.limit}
                trade direction: ${if (alert.isLong) "Long" else "Short"}
            }
            Last $candleLookBackPeriod candles: (format: [timestamp, open, high, low, close, volume, turnover])
            ${Json.encodeToString(candles)}
        """.trimIndent()

        val completionContent = getGPTCompletion(Constants.VERIFY_TRADE_GENERIC_DEV_MESSAGE, userMessage, structuredResponseSchema)
        return Json.decodeFromString<OpenAiTradeValidityResponse>(completionContent!!).valid
    }

}