package moe.arvin.kanonbot.listeners.lavalinkListeners;

import dev.arbjerg.lavalink.client.LavalinkNode;
import dev.arbjerg.lavalink.client.event.TrackStartEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TrackStartListener implements LavalinkListener<TrackStartEvent> {


    @Override
    public Class<TrackStartEvent> getEventType() {
        return TrackStartEvent.class;
    }

    @Override
    public Mono<Void> execute(TrackStartEvent event) {
        final LavalinkNode node = event.getNode();

        LOG.info("Node: {}: Starting track (guild={}, uri={})", node.getName(), event.getGuildId(), event.getTrack().getInfo().getUri());

        return Mono.empty();
    }
}
