package fr.rezoleo.discord;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public class Config {

    private static final String PATH = "config.json";

    @JsonProperty("discord_token")
    private String discordToken;

    @JsonProperty("ollama_host")
    private String ollamaHost;

    @JsonProperty("llm_model")
    private String llmModel;

    @JsonProperty("ping_list")
    private List<String> pingList;

    public String getDiscordToken() {
        return this.discordToken;
    }

    public String getOllamaHost() {
        return this.ollamaHost;
    }

    public String getLLMModel() {
        return this.llmModel;
    }

    public List<String> getPingList() {
        return this.pingList;
    }

    public static Optional<Config> loadConfig() {
        ObjectMapper mapper = new ObjectMapper();
        InputStream input = Main.class.getClassLoader().getResourceAsStream(PATH);
        try {
            return Optional.ofNullable(mapper.readValue(input, Config.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
