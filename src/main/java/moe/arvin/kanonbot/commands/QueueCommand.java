package moe.arvin.kanonbot.commands;

import discord4j.core.object.entity.Message;
import moe.arvin.kanonbot.music.GuildAudioManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class QueueCommand implements Command {

    private final GuildAudioManager gAM;

    public QueueCommand(GuildAudioManager guildAudioManager) {
        this.gAM = guildAudioManager;
    }

    @Override
    public String getName() {
        return "queue";
    }

    @Override
    public Mono<Void> handle(Message message, String msgArg) {
        return message.getChannel()
                .flatMap(channel -> channel.createMessage(gAM.getScheduler().queueToString()))
                .then();
    }
}