<img width="1024" height="1024" alt="image" src="https://github.com/user-attachments/assets/2859d6c9-2402-4773-ba39-b5a60e056737" />



🤖 Spring AI Telegram Bot

This project is a Spring Boot based AI-powered Telegram Bot that integrates:
Spring AI (OpenAI/Gemini) for text-based conversations and chat memory.
Gemini Speech Service for Text-to-Speech (TTS) and speech handling.
Telegram Webhooks for real-time chat interactions.
Ngrok for exposing your local server to Telegram securely.

The bot can:

Respond to text queries.
Convert text responses into speech.
Maintain user-specific chat memory.
Manage user data and track chat history.

📂 Project Structure
├── pom.xml
└── src
    └── main
        ├── java/com/example/bot
        │   ├── SpringAiBotApplication.java      # Main entry point
        │   ├── config/OpenAIConfig.java         # Config for Spring AI / Gemini
        │   ├── controller
        │   │   ├── BotController.java           # For local testing (chat + TTS)
        │   │   └── TelegramWebHook.java         # Main Telegram webhook endpoint
        │   ├── entity
        │   │   ├── SpeechResult.java
        │   │   └── UserData.java
        │   └── service
        │       ├── BotService.java              # Core bot logic
        │       ├── ChatMemoryService.java       # Stores per-user chat memory
        │       ├── GeminiSpeechService.java     # Handles text-to-speech
        │       ├── PcmToMp3Converter.java       # Converts PCM → MP3
        │       └── TelegramWebHookService.java  # Handles Telegram updates
        └── resources/application.properties

⚙️ Setup Instructions
1️⃣ Clone the repository
2️⃣ Configure application.properties


🏗️ System Architecture
flowchart TD
    User["👤 Telegram User"] -->|Sends Message / Voice| TelegramAPI["📡 Telegram API"]
    TelegramAPI -->|Webhook Event| Webhook["/Telegram/webhook (Spring Boot)"]

    subgraph Bot["🤖 Spring AI Bot (Spring Boot)"]
        Webhook -->|Detect Text/Voice| BotService["BotService"]
        BotService --> ChatMemory["🗂️ ChatMemoryService"]
        BotService --> LLM["🧠 Spring AI (Gemini/OpenAI)"]
        BotService --> SpeechService["🔊 GeminiSpeechService"]
        SpeechService --> PCMtoMP3["🎶 PcmToMp3Converter"]
    end

    LLM --> BotService
    ChatMemory --> BotService
    SpeechService --> BotService

    BotService --> Webhook
    Webhook -->|Send Reply (Text/Audio)| TelegramAPI
    TelegramAPI --> User


Update src/main/resources/application.properties with your keys:
spring.application.name=SpringAiBot

# OpenAI / Gemini keys
spring.ai.openai.api-key=<YOUR_OPENAI_OR_GEMINI_KEY>
# Telegram Bot Token (from BotFather)
telegram.bot.token=<YOUR_TELEGRAM_BOT_TOKEN>

3️⃣ Run the application
mvn spring-boot:run
The bot will start at:
http://localhost:8080

🌍 Expose API using Ngrok
Telegram requires a public HTTPS endpoint for webhooks. Run:
ngrok http 8080
You’ll get a forwarding URL like:
https://<ngrok-id>.ngrok-free.app

📡 API Endpoints
🔹 Text-to-Text (Local Test)
curl --location 'http://localhost:8080/bot/chat?message=Hi%20model%20what%20are%20up%20to'

🔹 Text-to-Speech (Local Test)
curl --location 'http://localhost:8080/bot/tts?message=Hey!%20Not%20much%2C%20just%20hanging%20out%20here...' \
--header 'Content-Type: application/octet-stream'

🔹 Text-to-Text (Ngrok)
curl --location 'https://<ngrok-id>.ngrok-free.app/bot/chat?message=Hi%20model%20what%20are%20up%20to'

🤝 Telegram Bot Integration
1️⃣ Create a Bot

Open Telegram → search for BotFather.
Run /newbot → follow instructions.

Copy your bot TOKEN.

2️⃣ Set Webhook
curl --location 'https://api.telegram.org/bot<TOKEN>/setWebHook' \
--form 'url="https://<ngrok-id>.ngrok-free.app/Telegram/webhook"'

3️⃣ Delete Webhook
curl --location 'https://api.telegram.org/bot<TOKEN>/deleteWebHook'

4️⃣ Get Pending Updates
curl --location 'https://api.telegram.org/bot<TOKEN>/getUpdates'

5️⃣ Manage Updates

Delete or skip messages:

curl --location 'https://api.telegram.org/bot<TOKEN>/getUpdates?offset=<OFFSET_FROM_RESPONSE>'

📊 Extra Endpoints
🔹 Get last 10 messages of a user
curl --location 'http://localhost:8080/Telegram/getUsersMessages?chatId=<CHAT_ID>'

🔹 Get number of users in the application
curl --location 'http://localhost:8080/Telegram/getUsers'

🛠️ How It Works

BotController → Local testing for chat & TTS.
TelegramWebHook → Production controller for Telegram.
Determines if the incoming message is text or speech.
Uses Spring AI (ChatClient) + ChatMemoryService for contextual replies.
Uses GeminiSpeechService for TTS.
Responses are sent back via Telegram.

🚀 Tech Stack

Spring Boot (REST APIs, configuration, dependency injection
Spring AI (OpenAI/Gemini LLM integration)
Telegram Bot API (via webhooks)
Ngrok (tunneling for local dev)
Java Sound API (PCM → MP3 conversion)

📌 Notes
Use /Telegram/webhook for Telegram production.
Use /bot/* endpoints for local testing.
Make sure Ngrok is running whenever Telegram is hitting the webhook.

👤 Author

Mirza Masroor Baig
🚀 Built projects on AI Bots.
