package moe.arvin.kanonbot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import moe.arvin.kanonbot.music.GuildAudioManager;
import moe.arvin.kanonbot.music.TextChatHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class PitchCommand implements Command {

    @Override
    public String getName() {
        return "pitch";
    }

    @Override
    public String getDescription() {
        return "Change the audio pitch";
    }

    @Override
    public Mono<Void> handle(Message message, String msgArg) {
        Optional<Snowflake> guildID = message.getGuildId();
        if (guildID.isEmpty()) {
            return Mono.empty();
        }
        GuildAudioManager gAM = GuildAudioManager.of(guildID.get());
        if (!gAM.getVoiceChatHandler().userInVoiceChannelFromMsg(message)) {
            TextChatHandler.sendErrorEmbedToMsgChannel(message,
                    "You have to be connected to a voice channel before you can use this command!");
            return Mono.empty();
        }
        double multiplier;
        try {
            multiplier = (double) Math.round(Double.parseDouble(msgArg) * 10) / 10;
            if (multiplier < 0.1 || multiplier > 2.0) {
                throw new Exception("invalid pitch value");
            }
        } catch (Exception e) {
            TextChatHandler.sendErrorEmbedToMsgChannel(message,
                    "You must give a valid value between 0.1 and 2.0!");
            return Mono.empty();
        }
        boolean pitchChanged = gAM.getScheduler().changePitch(multiplier);
        if (pitchChanged) {
            return message.addReaction(ReactionEmoji.unicode("\uD83D\uDC4C"))
                    .then();
        } else {
            TextChatHandler.sendErrorEmbedToMsgChannel(message, "You must be playing a track to use this command!");
            return Mono.empty();
        }
    }
}
