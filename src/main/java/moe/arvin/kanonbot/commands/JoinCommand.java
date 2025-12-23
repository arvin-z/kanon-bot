package moe.arvin.kanonbot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.emoji.Emoji;
import moe.arvin.kanonbot.music.GuildAudioManager;
import moe.arvin.kanonbot.music.GuildAudioManagerFactory;
import moe.arvin.kanonbot.music.TextChatHandler;
import moe.arvin.kanonbot.music.VoiceChatHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class JoinCommand implements Command {

    private final GuildAudioManagerFactory gAMFactory;

    public JoinCommand(GuildAudioManagerFactory gAMFactory) {
        this.gAMFactory = gAMFactory;
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getDescription() {
        return "Join the voice channel";
    }

    @Override
    public Mono<Void> handle(Message message, String msgArg) {
        Optional<Snowflake> guildID = message.getGuildId();
        if (guildID.isEmpty()) {
            return Mono.empty();
        }
        GuildAudioManager gAM = gAMFactory.get(guildID.get());
        final VoiceChatHandler vcHandler = gAM.getVoiceChatHandler();
        final TextChatHandler chatHandler = gAM.getTextChatHandler();
        return joinVC(message, vcHandler, chatHandler);
    }

    public Mono<Void> joinVC(Message msg, VoiceChatHandler vcHandler, TextChatHandler chatHandler) {
        return msg.getAuthorAsMember()
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                .flatMap(voiceChannel -> {
                    if (vcHandler.joinVoiceChannel(voiceChannel)) {
                        TextChatHandler.reactToMessage(msg, Emoji.unicode("\uD83D\uDC4C"));
                        chatHandler.setActiveTextChannelByMsg(msg);
                    }
                    else {
                        TextChatHandler.sendErrorEmbedToMsgChannel(msg,
                                "You have to be connected to a voice channel before you can use this command!");
                    }
                    return Mono.empty();
                })
                .then();
    }
}
