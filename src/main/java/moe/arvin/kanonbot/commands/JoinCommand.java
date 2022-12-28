package moe.arvin.kanonbot.commands;

import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.voice.AudioProvider;
import moe.arvin.kanonbot.music.LavaPlayerAudioProvider;
import moe.arvin.kanonbot.music.VoiceChatHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class JoinCommand implements Command {
    private final VoiceChatHandler vcHandler;

    public JoinCommand(VoiceChatHandler voiceChatHandler) {
        this.vcHandler = voiceChatHandler;
    }
    @Override
    public String getName() {
        return "join";
    }

    @Override
    public Mono<Void> handle(Message message) {
        return message.getAuthorAsMember()
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                .doOnSuccess(vcHandler::joinVoiceChannel)
                .then();
    }
}
