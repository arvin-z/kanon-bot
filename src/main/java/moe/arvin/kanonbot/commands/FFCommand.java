package moe.arvin.kanonbot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.emoji.Emoji;
import moe.arvin.kanonbot.music.GuildAudioManager;
import moe.arvin.kanonbot.music.GuildAudioManagerFactory;
import moe.arvin.kanonbot.music.TextChatHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class FFCommand implements Command {

    private final GuildAudioManagerFactory gAMFactory;

    public FFCommand(GuildAudioManagerFactory gAMFactory) {
        this.gAMFactory = gAMFactory;
    }

    @Override
    public String getName() {
        return "ff";
    }

    @Override
    public String getDescription() {
        return "Go forward by the specified number of seconds";
    }

    @Override
    public Mono<Void> handle(Message message, String msgArg) {
        Optional<Snowflake> guildID = message.getGuildId();
        if (guildID.isEmpty()) {
            return Mono.empty();
        }
        GuildAudioManager gAM = gAMFactory.get(guildID.get());
        if (!gAM.getVoiceChatHandler().userInVoiceChannelFromMsg(message)) {
            TextChatHandler.sendErrorEmbedToMsgChannel(message,
                    "You have to be connected to a voice channel before you can use this command!");
            return Mono.empty();
        }
        int seconds;
        try {
            seconds = Integer.parseInt(msgArg);
        } catch (NumberFormatException e) {
            TextChatHandler.sendErrorEmbedToMsgChannel(message,
                    "You must give a valid time in seconds!");
            return Mono.empty();
        }
        boolean forwarded = gAM.getScheduler().forwardOrRewind(seconds);
        if (forwarded) {
            return message.addReaction(Emoji.unicode("\uD83D\uDC4C"))
                    .then();
        } else {
            TextChatHandler.sendErrorEmbedToMsgChannel(message, "You must be playing a track to use this command!");
            return Mono.empty();
        }
    }
}
