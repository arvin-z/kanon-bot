package moe.arvin.kanonbot.commands;

import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public interface Command {
    String getName();

    String getDescription();

    Mono<Void> handle(Message message, String msgArg);
}
