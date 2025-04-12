package fr.rezoleo.discord;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import io.github.ollama4j.OllamaAPI;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class Main {

    public static void main(String[] args) {
        Optional<Config> config = Config.loadConfig();

        // SETUP OLLAMA
        OllamaAPI ollamaAPI = new OllamaAPI("http://localhost:11434");
        ollamaAPI.setRequestTimeoutSeconds(30);
        String model = config.map(Config::getModel).orElse("gemma3");

        //SETUP DISCORD
        DiscordClient discordClient = config.map(Config::getToken).map(DiscordClient::create).orElseThrow();

        discordClient.login().block();

        discordClient.withGateway(client ->
                        registerCommands(client).thenMany(handleCommands(client, ollamaAPI, model)))
                .doOnError(Throwable::printStackTrace).block();
    }

    private static Mono<?> registerCommands(GatewayDiscordClient client) {
        var cmdRequest = ApplicationCommandRequest.builder()
                .name("code")
                .description("Aide à coder quelque chose")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("prompt")
                        .description("Votre requête de programmation")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build()
                ).build();

        return client.getRestClient().getApplicationId()
                .flatMap(applicationId -> client.getRestClient().getApplicationService()
                        .createGlobalApplicationCommand(applicationId, cmdRequest));
    }

    private static Flux<?> handleCommands(GatewayDiscordClient client, OllamaAPI ollamaAPI, String model) {
        CodeHelpCommand command = new CodeHelpCommand(ollamaAPI, model);

        return client.on(ChatInputInteractionEvent.class, event -> {
            if (event.getCommandName().equals("code")) {
                command.process(event);
                return event.deferReply();
            }
            return Mono.empty();
        });
    }

}