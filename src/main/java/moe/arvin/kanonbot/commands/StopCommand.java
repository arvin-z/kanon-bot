package moe.arvin.kanonbot.commands;

import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import moe.arvin.kanonbot.music.GuildAudioManager;
import moe.arvin.kanonbot.music.TextChatHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class StopCommand implements Command {

    private final GuildAudioManager gAM;

    public StopCommand(GuildAudioManager guildAudioManager) {
        this.gAM = guildAudioManager;
    }

    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public Mono<Void> handle(Message message, String msgArg) {
        if (!gAM.getVoiceChatHandler().userInVoiceChannelFromMsg(message)) {
            TextChatHandler.sendErrorEmbedToMsgChannel(message,
                    "You have to be connected to a voice channel before you can use this command!");
            return Mono.empty();
        }
        gAM.getScheduler().stop();
        return message.addReaction(ReactionEmoji.unicode("\uD83D\uDED1"))
                .then();
    }
}
