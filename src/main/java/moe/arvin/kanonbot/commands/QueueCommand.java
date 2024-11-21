package moe.arvin.kanonbot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import moe.arvin.kanonbot.music.GuildAudioManager;
import moe.arvin.kanonbot.music.GuildAudioManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class QueueCommand implements Command {

    @Value("${kanonbot.prefix}")
    private char cmdPrefix;

    private final GuildAudioManagerFactory gAMFactory;

    public QueueCommand(GuildAudioManagerFactory gAMFactory) {
        this.gAMFactory = gAMFactory;
    }

    @Override
    public String getName() {
        return "queue";
    }

    @Override
    public String getDescription() {
        return "List the currently queued tracks";
    }

    @Override
    public Mono<Void> handle(Message message, String msgArg) {
        Optional<Snowflake> guildID = message.getGuildId();
        if (guildID.isEmpty()) {
            return Mono.empty();
        }
        GuildAudioManager gAM = gAMFactory.get(guildID.get());

        int pageArg;
        try {
            pageArg = Integer.parseInt(msgArg);
        } catch (NumberFormatException e) {
            pageArg = -1;
        }

        final int page = pageArg;

        return message.getChannel()
                .flatMap(channel -> channel.createMessage(gAM.getScheduler().queueToString(page, cmdPrefix)))
                .then();
    }
}
