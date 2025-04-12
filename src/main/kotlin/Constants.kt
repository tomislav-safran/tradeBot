package com.tsafran

object Constants {
    // This is only an example, make sure to update this to the desired strategy
    var GPT_ORDER_DEV_MESSAGE = """
        Review the provided OHLCV and EMA data points to analyze market conditions. 
        Use the data to identify and confirm trends, chart patterns, and key support/resistance levels.
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
        0 means no clear trade opportunity — avoid trading and wait for a better entry
        Values above 0 indicate a quantified confidence level in the recommendation.
    """.trimIndent()
}