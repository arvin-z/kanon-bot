package moe.arvin.kanonbot.commands;

import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import moe.arvin.kanonbot.music.GuildAudioManager;
import moe.arvin.kanonbot.music.VoiceChatHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DCCommand implements Command{

    private final VoiceChatHandler vcHandler;

    public DCCommand(GuildAudioManager guildAudioManager) {
        this.vcHandler = guildAudioManager.getVoiceChatHandler();
    }

    @Override
    public String getName() {
        return "dc";
    }

    @Override
    public Mono<Void> handle(Message message, String msgArg) {
        vcHandler.leaveVoiceChannel();
        return message.addReaction(ReactionEmoji.unicode("\uD83D\uDC4B"))
                .then();
    }
}
