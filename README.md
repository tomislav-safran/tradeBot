# tradeBot 🤖📈

**Automated trading bot built with Kotlin + Ktor**  
Deploys easily via Docker. Integrates with Bybit and OpenAI for smart, customizable trading logic.

## 🚀 Features

- 📩 Accepts webhook alerts from your favorite charting platform (e.g., TradingView)
- 📊 Places **spot** or **perpetual** trades on Bybit with **TP/SL**
- 🧠 Optional AI-powered alert validation
- 🤖 Fully automated **ChatGPT-driven trades**

## 🔧 Setup

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

## 📬 Webhook Format
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

## ⚠️ Notes
This project is still very much WIP, usage with live accounts not recommended.
Uses Bybit’s API for real order execution. Use with caution on live accounts.
ChatGPT-powered trades are experimental – use with proper risk management.
