package fr.rezoleo.discord;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.utils.OptionsBuilder;
import io.github.ollama4j.utils.PromptBuilder;

import java.io.IOException;

public class CodeHelpCommand {

    private final OllamaAPI api;
    private final String model;

    public CodeHelpCommand(OllamaAPI ollamaAPI, String model) {
        this.api = ollamaAPI;
        this.model = model;
    }

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
                        .addLine(
                                "DO NOT include ANY extra text apart from code. Follow this instruction very strictly!")
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
            try {
                String response = this.api.generate(this.model, promptBuilder.build(), false, new OptionsBuilder().build()).getResponse();
                System.out.println(response);
                event.createFollowup(response).subscribe();
            } catch (OllamaBaseException | IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}
