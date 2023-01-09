package moe.arvin.kanonbot.commands;

import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.voice.AudioProvider;
import moe.arvin.kanonbot.music.LavaPlayerAudioProvider;
import moe.arvin.kanonbot.music.TextChatHandler;
import moe.arvin.kanonbot.music.VoiceChatHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class JoinCommand implements Command {
    private final VoiceChatHandler vcHandler;
    private final TextChatHandler chatHandler;

    public JoinCommand(VoiceChatHandler voiceChatHandler, TextChatHandler textChatHandler) {
        this.vcHandler = voiceChatHandler;
        this.chatHandler = textChatHandler;
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public Mono<Void> handle(Message message) {
        return setTextChannel(message)
                .then(joinVC(message));
    }

    public Mono<Void> setTextChannel(Message msg) {
        return msg.getChannel()
                .doOnSuccess(chatHandler::setActiveTextChannel)
                .then();
    }

    public Mono<Void> joinVC(Message msg) {
        return msg.getAuthorAsMember()
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                .doOnSuccess(vcHandler::joinVoiceChannel)
                .then();
    }
}
