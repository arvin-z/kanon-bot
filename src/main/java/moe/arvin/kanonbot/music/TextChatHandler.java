package moe.arvin.kanonbot.music;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.emoji.Emoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

public class TextChatHandler {


    private MessageChannel activeTextChannel;

    public void setActiveTextChannel(MessageChannel newChannel) {
        this.activeTextChannel = newChannel;
    }

    public void setActiveTextChannelByMsg(Message msg) {
        this.activeTextChannel = msg.getChannel().block();
    }

    public MessageChannel getActiveTextChannel() {
        return activeTextChannel;
    }

    public void sendEmbed(EmbedCreateSpec s) {
        activeTextChannel.createMessage(s).block();
    }

    public static void reactToMessage(Message m, Emoji e) {
        m.addReaction(e).block();
    }

    public static void sendErrorEmbedToMsgChannel(Message m, String e) {
        MessageChannel c = m.getChannel().block();
        sendErrorEmbedToMsgChannel(c, e);
    }

    public static void sendErrorEmbedToMsgChannel(MessageChannel c, String e) {
        if (c == null) {
            return;
        }
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
        builder.color(Color.RED);
        builder.description(e);
        c.createMessage(builder.build()).block();
    }


}
