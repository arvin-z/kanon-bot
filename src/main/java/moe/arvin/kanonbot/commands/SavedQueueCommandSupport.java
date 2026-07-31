package moe.arvin.kanonbot.commands;

import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

final class SavedQueueCommandSupport {

    private SavedQueueCommandSupport() {
    }

    static long scopeId(Message message) {
        return message.getGuildId().orElse(message.getChannelId()).asLong();
    }

    static Mono<Void> success(Message message, String description) {
        return reply(message, description, Color.MOON_YELLOW);
    }

    static Mono<Void> error(Message message, String description) {
        return reply(message, description, Color.RED);
    }

    private static Mono<Void> reply(Message message, String description, Color color) {
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(color)
                .description(description)
                .build();
        return message.getChannel()
                .flatMap(channel -> channel.createMessage(embed))
                .then();
    }
}
