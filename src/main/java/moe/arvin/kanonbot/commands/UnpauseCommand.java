package moe.arvin.kanonbot.commands;

import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import moe.arvin.kanonbot.music.GuildAudioManager;
import moe.arvin.kanonbot.music.TextChatHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class UnpauseCommand implements Command {

    private final GuildAudioManager gAM;

    public UnpauseCommand(GuildAudioManager guildAudioManager) {
        this.gAM = guildAudioManager;
    }

    @Override
    public String getName() {
        return "unpause";
    }

    @Override
    public Mono<Void> handle(Message message, String msgArg) {
        if (!gAM.getVoiceChatHandler().userInVoiceChannelFromMsg(message)) {
            TextChatHandler.sendErrorEmbedToMsgChannel(message,
                    "You have to be connected to a voice channel before you can use this command!");
            return Mono.empty();
        }
        boolean unpaused = gAM.getScheduler().unpause();
        if (unpaused) {
            return message.addReaction(ReactionEmoji.unicode("\uD83D\uDC4C️"))
                    .then();
        } else {
            TextChatHandler.sendErrorEmbedToMsgChannel(message, "You must be playing a track to use this command!");
        }
        return Mono.empty();
    }
}
