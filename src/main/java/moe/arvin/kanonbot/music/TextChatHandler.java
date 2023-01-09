package moe.arvin.kanonbot.music;

import discord4j.core.object.entity.channel.MessageChannel;
import org.springframework.stereotype.Service;

@Service
public class TextChatHandler {


    private MessageChannel activeTextChannel;

    public void setActiveTextChannel(MessageChannel newChannel) {
        this.activeTextChannel = newChannel;
    }

    public MessageChannel getActiveTextChannel() {
        return activeTextChannel;
    }

    public void sendMsg(String s) {
        activeTextChannel.createMessage(s).block();
    }
}
