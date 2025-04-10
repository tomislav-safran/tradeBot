# tradeBot 🤖📈

**Automated trading bot built with Kotlin + Ktor**  
Deploys easily via Docker. Integrates with Bybit and OpenAI for smart, customizable trading logic.

## Features

- Accepts webhook alerts from your favorite charting platform (e.g., TradingView)
- Places **spot** or **perpetual** trades on Bybit with **TP/SL**
- Fully automated **ChatGPT-driven trades**

## Setup

1. Clone the repo:
   ```
   git clone https://github.com/your-username/tradeBot.git
   cd tradeBot
   ```
   
2. Add your API keys in compose.yml:
   ```
   environment:
     BYBIT_API_KEY: ""
     BYBIT_API_SECRET: ""
     OPEN_AI_API_KEY: ""
   ```
   
3. Run with Docker Compose:
   ```
   docker-compose up --build
   ```

## Webhook Format
Send webhook alerts from TradingView or any platform in a supported JSON format:
```
{
  "coin": "BTCUSDT",
  "close": 67250.0,
  "limit": 67300.0,
  "stop": 67000.0,
  "isLong": true,
  "useTrailingStop": false
}
```

## GPT Automated trades
Send a request to **port/start-gpt-trader** with the following body to start the automated trading bot:
```
{
    "symbols": ["BTCUSDT", "SOLUSDT"],    // list of symbols you want to trade
    "candleLookBack": "100",              // the amount of historic candles to use as context
    "certaintyThreshold": 75,             // how sure do you want the bot to be before placing a trade
    "intervalMinutes": 30,                // trading interval
    "useTrailingStop": false              // set to true to use trailing stops instead of fixed ones 
}
```

## ⚠️ Notes
This project is still very much WIP, usage with live accounts not recommended.
Uses Bybit’s API for real order execution. Use with caution on live accounts.
ChatGPT-powered trades are experimental – use with proper risk management.
