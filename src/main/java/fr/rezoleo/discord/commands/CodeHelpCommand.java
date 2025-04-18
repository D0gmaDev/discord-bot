package fr.rezoleo.discord.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.generate.OllamaStreamHandler;
import io.github.ollama4j.utils.OptionsBuilder;
import io.github.ollama4j.utils.PromptBuilder;

import java.io.IOException;

public class CodeHelpCommand implements BotCommand {

    private final OllamaAPI api;
    private final String model;

    public CodeHelpCommand(OllamaAPI ollamaAPI, String model) {
        this.api = ollamaAPI;
        this.model = model;
    }

    @Override
    public ApplicationCommandRequest getRequest() {
        return ApplicationCommandRequest.builder()
                .name("code")
                .description("Aide à coder quelque chose")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("prompt")
                        .description("Votre requête de programmation")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build()
                ).build();
    }

    @Override
    public void process(ChatInputInteractionEvent event) {
        String question = event.getOption("prompt")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow();

        PromptBuilder promptBuilder =
                new PromptBuilder()
                        .addLine("You are an expert coder and understand different programming languages.")
                        .addLine("Given a question, answer ONLY with code.")
                        .addLine("Produce clean, formatted and indented code in markdown format.")
                        .addLine("DO NOT include ANY extra text apart from code. Follow this instruction very strictly!")
                        .addLine("If there's any additional information you want to add, use comments within code.")
                        .addLine("Answer only in the programming language that has been asked for.")
                        .addSeparator()
                        .addLine("Example: Sum 2 numbers in Python")
                        .addLine("Answer:")
                        .addLine("```python")
                        .addLine("def sum(num1: int, num2: int) -> int:")
                        .addLine("    return num1 + num2")
                        .addLine("```")
                        .addSeparator()
                        .add(question);

        Thread.ofVirtual().start(() -> {
            OllamaStreamHandler handler = response -> {
                String endSequence = response.endsWith("```") ? "" : "\n```"; // end the code block if necessary
                String updatedMessage = "## " + question + '\n' + response + endSequence;

                event.getReply().blockOptional().ifPresentOrElse(
                        msg -> event.editReply(updatedMessage).block(),
                        () -> event.createFollowup(updatedMessage).block()
                );
            };

            try {
                this.api.generate(this.model, promptBuilder.build(), false, new OptionsBuilder().build(), handler);
            } catch (OllamaBaseException | IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}
