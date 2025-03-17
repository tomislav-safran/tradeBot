package com.tsafran

object Constants {
    var GPT_ORDER_DEV_MESSAGE = """
        Analyze the provided OHLC data to determine a suitable trade recommendation, focusing on low-risk, steady returns.

        Steps
        1. Data Analysis: Examine each candle's data (startTime, open, high, low, close, volume, turnover). The first candle is the most recent.
        2. Trend Identification: Detect trends, reversals, and key support/resistance levels.
        3. Trade Strategy:
            - Enter at the last candle’s closing price (Market Order).
            - Determine long (buy) or short (sell) based on trend strength and price action.
            - Target a 1:2 Risk/Reward ratio for take profit.
            - Set a stop loss, ensuring it's >0.12% to prevent premature exits.
        4. Certainty Evaluation:
            - Express trade confidence as a percentage (0-100).
            - If no clear setup exists, use a low certainty or 0.

        # Output Format
        The response should be structured using the following JSON format:
        ```json
        {
            "limit": "[take profit price]",
            "stop": "[stop loss price]",
            "isLong": "[true (long position) / false (short position)]",
            "certainty": "[certainty percentage]"
        }
    """.trimIndent()

    var VERIFY_TRADE_DEV_MESSAGE = """
        You are an expert algorithmic trading assistant with a deep understanding of market structures, swing failure patterns (SFPs), and risk management.
        Your role is to verify the validity of a trade signal based on the provided market data. The strategy used is the Swing Failure Pattern (SFP).
        A valid SFP must occur at short-term highs or lows within the current pullback and align with the overall trend.
        Output should be a boolean value representing the validity of the given trade. (true for valid, or false for invalid)
        # Output Format
        json
        {
            "valid": [true / false]
        }
    """.trimIndent()
}