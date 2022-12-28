package moe.arvin.kanonbot.commands;

import discord4j.core.object.entity.Message;
import moe.arvin.kanonbot.music.VoiceChatHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DCCommand implements Command{

    private final VoiceChatHandler vcHandler;

    public DCCommand(VoiceChatHandler voiceChatHandler) {
        this.vcHandler = voiceChatHandler;
    }

    @Override
    public String getName() {
        return "dc";
    }

    @Override
    public Mono<Void> handle(Message message) {
        vcHandler.leaveVoiceChannel();
        return Mono.empty();
    }
}
