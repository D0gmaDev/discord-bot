package fr.rezoleo.discord;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import fr.rezoleo.discord.commands.BotCommand;
import fr.rezoleo.discord.commands.CodeHelpCommand;
import io.github.ollama4j.OllamaAPI;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

public class Main {

    public static void main(String[] args) {
        Optional<Config> config = Config.loadConfig();

        // SETUP OLLAMA
        OllamaAPI ollamaAPI = new OllamaAPI(config.map(Config::getOllamaHost).orElse("http://localhost:11434"));
        ollamaAPI.setRequestTimeoutSeconds(30);
        String model = config.map(Config::getLLMModel).orElse("gemma3");

        //SETUP DISCORD
        DiscordClient discordClient = config.map(Config::getDiscordToken).map(DiscordClient::create).orElseThrow();
        discordClient.login().block();

        //REGISTER COMMANDS
        Map<String, BotCommand> commands = Map.of(
                "code", new CodeHelpCommand(ollamaAPI, model)
        );

        discordClient.withGateway(client ->
                        registerCommands(commands, client).thenMany(handleCommands(commands, client)))
                .doOnError(Throwable::printStackTrace).block();
    }

    private static Flux<?> registerCommands(Map<String, BotCommand> commands, GatewayDiscordClient client) {
        var appService = client.getRestClient().getApplicationService();

        return client.getRestClient().getApplicationId().flatMapMany(applicationId ->
                Flux.fromIterable(commands.values()).map(BotCommand::getRequest).flatMap(cmdRequest ->
                        appService.createGlobalApplicationCommand(applicationId, cmdRequest)));
    }

    private static Flux<?> handleCommands(Map<String, BotCommand> commands, GatewayDiscordClient client) {
        return client.on(ChatInputInteractionEvent.class, event -> {
            if (commands.containsKey(event.getCommandName())) {
                commands.get(event.getCommandName()).process(event);
                return event.deferReply();
            }
            return Mono.empty();
        });
    }
}
