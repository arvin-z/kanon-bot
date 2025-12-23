package moe.arvin.kanonbot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.emoji.Emoji;
import moe.arvin.kanonbot.music.GuildAudioManager;
import moe.arvin.kanonbot.music.GuildAudioManagerFactory;
import moe.arvin.kanonbot.music.TextChatHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class LocalLoopCommand implements Command {

    private final GuildAudioManagerFactory gAMFactory;

    public LocalLoopCommand(GuildAudioManagerFactory gAMFactory) {
        this.gAMFactory = gAMFactory;
    }

    @Override
    public String getName() {
        return "localloop";
    }

    @Override
    public String getDescription() {
        return "Loop between the 2 specified points of a track";
    }

    @Override
    public Mono<Void> handle(Message message, String msgArg) {
        Optional<Snowflake> guildID = message.getGuildId();
        if (guildID.isEmpty()) {
            return Mono.empty();
        }
        GuildAudioManager gAM = gAMFactory.get(guildID.get());
        if (!gAM.getVoiceChatHandler().userInVoiceChannelFromMsg(message)) {
            TextChatHandler.sendErrorEmbedToMsgChannel(message,
                    "You have to be connected to a voice channel before you can use this command!");
            return Mono.empty();
        }
        if (msgArg == null || msgArg.isEmpty()) {
            boolean res = gAM.getScheduler().localLoop();
            if (res) {
                TextChatHandler.sendErrorEmbedToMsgChannel(message,"Local Loop cancelled!" );
                return Mono.empty();
            } else {
                TextChatHandler.sendErrorEmbedToMsgChannel(message,"There is no Local Loop active!" );
                return Mono.empty();
            }
        }
        else {
            int beginT;
            int endT;
            try {
                String[] times = msgArg.split(" ");
                beginT = Integer.parseInt(times[0]);
                endT = Integer.parseInt(times[1]);
            } catch (Exception e) {
                TextChatHandler.sendErrorEmbedToMsgChannel(message,
                        "You must give 2 valid times in seconds!");
                return Mono.empty();
            }
            if (endT <= beginT) {
                TextChatHandler.sendErrorEmbedToMsgChannel(message, "Start time must be before end time!");
                return Mono.empty();
            }
            boolean res = gAM.getScheduler().localLoop(beginT, endT);
            if (res) {
                return message.addReaction(Emoji.unicode("\uD83D\uDD04"))
                        .then();
            } else{
                TextChatHandler.sendErrorEmbedToMsgChannel(message, "You must be playing a track to use this command!");
                return Mono.empty();
            }
        }

    }
}
