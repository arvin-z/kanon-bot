package moe.arvin.kanonbot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import moe.arvin.kanonbot.music.GuildAudioManager;
import moe.arvin.kanonbot.music.GuildAudioManagerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class NPCommand implements Command {

    private final GuildAudioManagerFactory gAMFactory;

    public NPCommand(GuildAudioManagerFactory gAMFactory) {
        this.gAMFactory = gAMFactory;
    }

    @Override
    public String getName() {
        return "np";
    }

    @Override
    public String getDescription() {
        return "Show the currently playing track";
    }

    @Override
    public Mono<Void> handle(Message message, String msgArg) {
        Optional<Snowflake> guildID = message.getGuildId();
        if (guildID.isEmpty()) {
            return Mono.empty();
        }
        GuildAudioManager gAM = gAMFactory.get(guildID.get());
        return message.getChannel()
                .flatMap(channel -> channel.createMessage(gAM.getScheduler().nowPlayingToEmbed()))
                .then();
    }
}
