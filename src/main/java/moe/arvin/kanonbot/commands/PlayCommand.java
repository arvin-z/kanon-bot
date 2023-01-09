package moe.arvin.kanonbot.commands;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import moe.arvin.kanonbot.music.TextChatHandler;
import moe.arvin.kanonbot.music.VoiceChatHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PlayCommand implements Command {

    private final VoiceChatHandler vcHandler;
    private final TextChatHandler chatHandler;

    public PlayCommand(VoiceChatHandler voiceChatHandler, TextChatHandler textChatHandler) {
        this.vcHandler = voiceChatHandler;
        this.chatHandler = textChatHandler;
    }

    @Override
    public String getName() {
        return "play";
    }

    @Override
    public Mono<Void> handle(Message message) {
        return message.getAuthorAsMember()
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                .doOnSuccess(vcHandler::handlePlay)
                .then();
    }
}
