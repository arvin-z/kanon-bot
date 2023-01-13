package moe.arvin.kanonbot.commands;

import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import moe.arvin.kanonbot.music.GuildAudioManager;
import moe.arvin.kanonbot.music.TextChatHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PauseCommand implements Command {

    private final GuildAudioManager gAM;

    public PauseCommand(GuildAudioManager guildAudioManager) {
        this.gAM = guildAudioManager;
    }

    @Override
    public String getName() {
        return "pause";
    }

    @Override
    public Mono<Void> handle(Message message, String msgArg) {
        if (!gAM.getVoiceChatHandler().userInVoiceChannelFromMsg(message)) {
            TextChatHandler.sendErrorEmbedToMsgChannel(message,
                    "You have to be connected to a voice channel before you can use this command!");
            return Mono.empty();
        }
        boolean paused = gAM.getScheduler().pause();
        if (paused) {
            return message.addReaction(ReactionEmoji.unicode("⏸️"))
                    .then();
        } else {
            TextChatHandler.sendErrorEmbedToMsgChannel(message, "You must be playing a track to use this command!");
        }
        return Mono.empty();
    }
}
