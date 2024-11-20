package moe.arvin.kanonbot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import moe.arvin.kanonbot.music.GuildAudioManager;
import moe.arvin.kanonbot.music.GuildAudioManagerFactory;
import moe.arvin.kanonbot.music.VoiceChatHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class PlayCommand implements Command {

    private final GuildAudioManagerFactory gAMFactory;

    public PlayCommand(GuildAudioManagerFactory gAMFactory) {
        this.gAMFactory = gAMFactory;
    }

    @Override
    public String getName() {
        return "play";
    }

    @Override
    public String getDescription() {
        return "Play the provided track";
    }

    @Override
    public Mono<Void> handle(Message message, String msgArg) {
        Optional<Snowflake> guildID = message.getGuildId();
        if (guildID.isEmpty()) {
            return Mono.empty();
        }
        GuildAudioManager gAM = gAMFactory.get(guildID.get());
        final VoiceChatHandler vcHandler = gAM.getVoiceChatHandler();
        MessageChannel chan = getChannel(message);
        return message.getAuthorAsMember()
                .doOnSuccess(member -> {
                    if (!vcHandler.handlePlay(member, msgArg, chan))
                        addMsgReaction(message);
                })
                .then();
    }

    public MessageChannel getChannel(Message message) {
        return message.getChannel().block();
    }

    public void addMsgReaction(Message message) {
        message.addReaction(ReactionEmoji.unicode("\uD83D\uDC4C")).block();
    }
}
