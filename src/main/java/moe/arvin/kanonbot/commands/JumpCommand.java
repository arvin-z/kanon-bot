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
public class JumpCommand implements Command {

    private final GuildAudioManagerFactory gAMFactory;

    public JumpCommand(GuildAudioManagerFactory gAMFactory) {
        this.gAMFactory = gAMFactory;
    }

    @Override
    public String getName() {
        return "jump";
    }

    @Override
    public String getDescription() {
        return "Jump to the specified track number";
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
        int trackNum;
        try {
            trackNum = Integer.parseInt(msgArg);
        } catch (NumberFormatException e) {
            boolean jumpedTitle = gAM.getScheduler().jumpTitle(msgArg);
            if (jumpedTitle) {
                return message.addReaction(Emoji.unicode("\uD83D\uDC4C"))
                        .then();
            } else {
                TextChatHandler.sendErrorEmbedToMsgChannel(message, "No matching track found in queue!");
                return Mono.empty();
            }
        }
        boolean jumped = gAM.getScheduler().jump(trackNum);
        if (jumped) {
            return message.addReaction(Emoji.unicode("\uD83D\uDC4C"))
                    .then();
        } else {
            TextChatHandler.sendErrorEmbedToMsgChannel(message, "That track number is not valid!");
            return Mono.empty();
        }

    }
}
