package fr.rezoleo.discord.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;

public interface BotCommand {

    ApplicationCommandRequest getRequest();

    void process(ChatInputInteractionEvent event);

}
