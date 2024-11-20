package moe.arvin.kanonbot.listeners.lavalinkListeners;

import dev.arbjerg.lavalink.client.LavalinkNode;
import dev.arbjerg.lavalink.client.event.TrackEndEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TrackEndListener implements LavalinkListener<TrackEndEvent> {


    @Override
    public Class<TrackEndEvent> getEventType() {
        return TrackEndEvent.class;
    }

    @Override
    public Mono<Void> execute(TrackEndEvent event) {
        final LavalinkNode node = event.getNode();

        LOG.info("Node: {}: Ending track (guild={}, uri={})", node.getName(), event.getGuildId(), event.getTrack().getInfo().getUri());

        return Mono.empty();
    }
}
