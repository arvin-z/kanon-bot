package moe.arvin.kanonbot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import moe.arvin.kanonbot.music.GuildAudioManager;
import moe.arvin.kanonbot.music.GuildAudioManagerFactory;
import moe.arvin.kanonbot.music.TextChatHandler;
import moe.arvin.kanonbot.music.VoiceChatHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class DCCommand implements Command{

    private final GuildAudioManagerFactory gAMFactory;

    public DCCommand(GuildAudioManagerFactory gAMFactory) {
        this.gAMFactory = gAMFactory;
    }

    @Override
    public String getName() {
        return "dc";
    }

    @Override
    public String getDescription() {
        return "Disconnect from the voice channel";
    }

    @Override
    public Mono<Void> handle(Message message, String msgArg) {
        Optional<Snowflake> guildID = message.getGuildId();
        if (guildID.isEmpty()) {
            return Mono.empty();
        }
        GuildAudioManager gAM = gAMFactory.get(guildID.get());
        final VoiceChatHandler vcHandler = gAM.getVoiceChatHandler();

        return leaveVC(message, vcHandler);
    }

    public Mono<Void> leaveVC(Message msg, VoiceChatHandler vcHandler) {
        return msg.getAuthorAsMember()
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                .doOnSuccess(voiceChannel -> {
                    if (vcHandler.leaveVoiceChannel(voiceChannel)) {
                        TextChatHandler.reactToMessage(msg, ReactionEmoji.unicode("\uD83D\uDC4B"));
                    } else {
                        TextChatHandler.sendErrorEmbedToMsgChannel(msg,
                                "You have to be connected to the voice channel before you can use this command!");
                    }
                })
                .then();
    }
}
