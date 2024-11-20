package moe.arvin.kanonbot.listeners.lavalinkListeners;

import dev.arbjerg.lavalink.client.LavalinkNode;
import dev.arbjerg.lavalink.client.event.TrackStartEvent;
import discord4j.common.util.Snowflake;
import moe.arvin.kanonbot.music.GuildAudioManager;
import moe.arvin.kanonbot.music.GuildAudioManagerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TrackStartListener implements LavalinkListener<TrackStartEvent> {

    private final GuildAudioManagerFactory gAMFactory;

    public TrackStartListener(GuildAudioManagerFactory gAMFactory) {
        this.gAMFactory = gAMFactory;
    }


    @Override
    public Class<TrackStartEvent> getEventType() {
        return TrackStartEvent.class;
    }

    @Override
    public Mono<Void> execute(TrackStartEvent event) {
        final LavalinkNode node = event.getNode();

        LOG.info("Node: {}: Starting track (guild={}, uri={})", node.getName(), event.getGuildId(), event.getTrack().getInfo().getUri());
        GuildAudioManager gAM = this.gAMFactory.get(Snowflake.of(event.getGuildId()));
        gAM.getScheduler().onTrackStart(event.getTrack());

        return Mono.empty();
    }
}
