package moe.arvin.kanonbot.listeners.lavalinkListeners;

import dev.arbjerg.lavalink.client.LavalinkNode;
import dev.arbjerg.lavalink.client.event.TrackEndEvent;
import discord4j.common.util.Snowflake;
import moe.arvin.kanonbot.music.GuildAudioManager;
import moe.arvin.kanonbot.music.GuildAudioManagerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TrackEndListener implements LavalinkListener<TrackEndEvent> {

    private final GuildAudioManagerFactory gAMFactory;

    public TrackEndListener(@Lazy GuildAudioManagerFactory gAMFactory) {
        this.gAMFactory = gAMFactory;
    }


    @Override
    public Class<TrackEndEvent> getEventType() {
        return TrackEndEvent.class;
    }

    @Override
    public Mono<Void> execute(TrackEndEvent event) {
        final LavalinkNode node = event.getNode();

        LOG.info("Node: {}: Ending track (guild={}, uri={})", node.getName(), event.getGuildId(), event.getTrack().getInfo().getUri());
        GuildAudioManager gAM = this.gAMFactory.get(Snowflake.of(event.getGuildId()));
        gAM.getScheduler().onTrackEnd(event.getTrack(), event.getEndReason());

        return Mono.empty();
    }
}
