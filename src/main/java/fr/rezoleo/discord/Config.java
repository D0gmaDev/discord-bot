package fr.rezoleo.discord;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class Config {

    private static final String PATH = "config.json";

    @SuppressWarnings("unused")
    private String token;
    @SuppressWarnings("unused")
    private String model;

    public String getToken() {
        return this.token;
    }

    public String getModel() {
        return this.model;
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
