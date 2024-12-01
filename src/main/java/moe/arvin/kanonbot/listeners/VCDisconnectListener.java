package moe.arvin.kanonbot.listeners;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;

import moe.arvin.kanonbot.music.GuildAudioManager;
import moe.arvin.kanonbot.music.GuildAudioManagerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
                gAM.getScheduler().stop();
                gAM.getVoiceChatHandler().setVoiceChannelJoined(false);
            }
        }
        // LISTEN FOR other users disconnecting
        else {
            return VCDisconnectListener.onUserEvent(event);
        }
        return Mono.empty();
    }

    private static Mono<Void> onUserEvent(VoiceStateUpdateEvent event) {
        // final Snowflake guildID = event.getCurrent().getGuildId();
        // return Mono.defer(() -> Mono.justOrEmpty());
        return Mono.empty();
    }
}
