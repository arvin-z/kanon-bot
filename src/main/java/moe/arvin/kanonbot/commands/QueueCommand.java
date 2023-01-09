package moe.arvin.kanonbot.commands;

import discord4j.core.object.entity.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class QueueCommand implements Command {

    @Override
    public String getName() {
        return "queue";
    }

    @Override
    public Mono<Void> handle(Message message) {
        return message.getChannel()
                .flatMap(channel -> channel.createMessage("```nim\nThe queue is empty ;-;\n```"))
                .then();
    }
}
