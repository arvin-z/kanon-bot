package moe.arvin.kanonbot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import moe.arvin.kanonbot.music.GuildAudioManager;
import moe.arvin.kanonbot.music.GuildAudioManagerFactory;
import moe.arvin.kanonbot.music.TextChatHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class LoopCommand implements Command {

    private final GuildAudioManagerFactory gAMFactory;

    public LoopCommand(GuildAudioManagerFactory gAMFactory) {
        this.gAMFactory = gAMFactory;
    }

    @Override
    public String getName() {
        return "loop";
    }

    @Override
    public String getDescription() {
        return "Toggle between looping the track or the queue.";
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
        gAM.getTextChatHandler().setActiveTextChannelByMsg(message);
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
        builder.color(Color.MOON_YELLOW);
        int loopType = gAM.getScheduler().toggleLoop();
        if (loopType == 0) {
            builder.description("Looping is now **disabled**.");
        } else if (loopType == 1) {
            builder.description("Now looping the **current track**.");
        } else if (loopType == 2) {
            builder.description("Now looping the **queue**.");
        }
        gAM.getTextChatHandler().sendEmbed(builder.build());
        return Mono.empty();
    }
}
