package moe.arvin.kanonbot.commands;

import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

final class SavedQueueCommandSupport {

    private SavedQueueCommandSupport() {
    }

    static long scopeId(Message message) {
        return message.getGuildId().orElse(message.getChannelId()).asLong();
    }

    static Mono<Void> reply(Message message, String content) {
        return message.getChannel()
                .flatMap(channel -> channel.createMessage(content))
                .then();
    }
}
