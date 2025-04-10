package com.tsafran

object Constants {
    // This is only an example, make sure to update this to the desired strategy
    var GPT_ORDER_DEV_MESSAGE = """
        Review the provided OHLC data points to analyze market conditions. Use the data to identify trends, chart patterns, and key support/resistance levels.
        In addition to price data, take EMA, volume and turnover into account to confirm or reject trend changes. 
        After analyzing the data, return a trade recommendation based on the price action.
        We are placing a market order at the latest price (last candle’s close).
        Decide whether to place a long or short order.
        For long positions, set "isLong": true.
        For short positions, set "isLong": false.
        Determine and set a realistic stop loss and take profit.
        For long positions: stop loss must be below the entry!, take profit must be above the entry!.
        For short positions: stop loss must be above the entry!, take profit must be below the entry!.
        The take profit distance should be bigger than stop lass, and should be at least twice the distance of the stop loss (2:1 reward:risk ratio).
        Finally, estimate your confidence in the trade by setting a "certainty" value between 0 and 100.
        0 means the trade should not be taken.
        100 means guaranteed success.
        Set certainty to 0 when there is no clear trade opportunity to wait for a better entry.
    """.trimIndent()

    var VERIFY_TRADE_GENERIC_DEV_MESSAGE = """
        Verify the validity of a trade based on provided trade parameters and recent market data including candle information.
        Given the entry price, stop loss price, take profit price, and trade direction (long or short), assess the feasibility of the trade using the provided OHLC (Open, High, Low, Close), volume, and turnover data provided.
        Review the provided data points to understand market trends, look for patterns and support and resistance levels and use volume and turnover to confirm the trend changes.
        A trade should never go against a strong trend!.
        Use volume and turnover to confirm trend changes.
        Output should be a boolean value representing the validity of the given trade. (true for valid, or false for invalid)
        # Output Format
        json
        {
            "valid": [true / false]
        }
    """.trimIndent()
}