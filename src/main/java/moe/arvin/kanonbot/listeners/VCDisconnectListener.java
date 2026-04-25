package moe.arvin.kanonbot.listeners;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.channel.AudioChannel;

import moe.arvin.kanonbot.music.GuildAudioManager;
import moe.arvin.kanonbot.music.GuildAudioManagerFactory;
import moe.arvin.kanonbot.music.VoiceChatHandler;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
public class VCDisconnectListener implements EventListener<VoiceStateUpdateEvent>  {

    private final GuildAudioManagerFactory gAMFactory;

    public VCDisconnectListener(GuildAudioManagerFactory gAMFactory) {
        this.gAMFactory = gAMFactory;
    }

    @Override
    public Class<VoiceStateUpdateEvent> getEventType() {
        return VoiceStateUpdateEvent.class;
    }

    @Override
    public Mono<Void> execute(VoiceStateUpdateEvent event) {
        final Snowflake userID = event.getCurrent().getUserId();
        final Snowflake guildID = event.getCurrent().getGuildId();
        // LISTEN FOR this bot disconnecting
        if (userID.equals(event.getClient().getSelfId())) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("{Guild ID: {}} Voice state update event: {}", guildID.asString(), event);
            }
            if (event.isLeaveEvent()) {
                GuildAudioManager gAM = gAMFactory.get(guildID);
                VoiceChatHandler vcHandler = gAM.getVoiceChatHandler();
                boolean isSoftDc = vcHandler.isSoftDisconnecting();
                vcHandler.setSoftDisconnecting(false);
                vcHandler.setVoiceChannelJoined(false);
                if (!isSoftDc) {
                    // Real disconnect (e.g. kicked) — stop playback and cancel any pending cycle
                    gAM.getScheduler().stop();
                    vcHandler.cancelAloneReconnectCycle();
                }
            }
        }
        // LISTEN FOR other users disconnecting
        else {
            return onUserEvent(event);
        }
        return Mono.empty();
    }

    private Mono<Void> onUserEvent(VoiceStateUpdateEvent event) {
        final Snowflake guildID = event.getCurrent().getGuildId();
        GuildAudioManager gAM = gAMFactory.get(guildID);
        VoiceChatHandler vcHandler = gAM.getVoiceChatHandler();

        AudioChannel botChannel = vcHandler.getCurrentChannel();
        if (botChannel == null) {
            return Mono.empty();
        }
        Snowflake botChannelId = botChannel.getId();

        // User joined bot's channel — cancel the alone-reconnect cycle
        Optional<Snowflake> currentChannelId = event.getCurrent().getChannelId();
        if (currentChannelId.isPresent() && currentChannelId.get().equals(botChannelId)) {
            vcHandler.cancelAloneReconnectCycle();
            return Mono.empty();
        }

        // User left bot's channel — check if bot is now alone
        Optional<VoiceState> oldState = event.getOld();
        if (oldState.isPresent()) {
            Optional<Snowflake> oldChannelId = oldState.get().getChannelId();
            if (oldChannelId.isPresent() && oldChannelId.get().equals(botChannelId)) {
                return botChannel.getVoiceStates()
                        .filter(vs -> !vs.getUserId().equals(event.getClient().getSelfId()))
                        .count()
                        .flatMap(count -> {
                            if (count == 0) {
                                vcHandler.startAloneReconnectCycle();
                            }
                            return Mono.<Void>empty();
                        });
            }
        }

        return Mono.empty();
    }
}
