package moe.arvin.kanonbot.commands;

import discord4j.core.object.entity.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PingCommand implements Command {
    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public String getDescription() {
        return "Reply with pong";
    }

    @Override
    public Mono<Void> handle(Message message, String msgArg) {
        return message.getChannel()
                .flatMap(channel -> channel.createMessage("pong"))
                .then();
    }
}
