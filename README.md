# Discord AI Bot

A Discord bot that provides code assistance using Ollama's LLM capabilities. The bot helps users with coding questions
and provides intelligent responses using a Large Language Model.

## Features

- Code assistance through Discord slash commands
- Integration with Ollama for LLM-powered responses

## Prerequisites

- Java 21 or higher
- Ollama running locally or accessible via network
- Discord Bot Token

## Installation

1. Clone the repository:

```bash
git clone https://github.com/D0gmaDev/discord-bot.git
cd discord-bot
```

2. Edit the `config.json` file in the `src/main/resources` directory with the following structure:

```json
{
    "discord_token": "your-discord-bot-token",
    "ollama_host": "http://localhost:11434",
    "llm_model": "gemma3"
}
```

3. Build the project:

```bash
./gradlew build
```

## Configuration

The bot requires a `config.json` file with the following parameters:

- `discord_token`: Your Discord bot token
- `ollama_host`: URL of your Ollama instance (default: http://localhost:11434)
- `llm_model`: The LLM model to use (default: gemma3)

## Usage

1. Start the bot:

```bash
./gradlew run
```

2. Available Commands:
    - `/code`: Get help with coding questions

## Dependencies

- [Discord4J](https://github.com/Discord4J) : Discord API wrapper
- [Ollama4J](https://github.com/ollama4j/ollama4j) : Java client for Ollama
