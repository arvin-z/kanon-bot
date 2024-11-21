package moe.arvin.kanonbot.listeners.lavalinkListeners;

import dev.arbjerg.lavalink.client.LavalinkNode;
import dev.arbjerg.lavalink.client.event.StatsEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class StatsListener implements LavalinkListener<StatsEvent> {


    @Override
    public Class<StatsEvent> getEventType() {
        return StatsEvent.class;
    }

    @Override
    public Mono<Void> execute(StatsEvent event) {
        final LavalinkNode node = event.getNode();

        LOG.debug("Node: {}: current players: {}/{}", node.getName(), event.getPlayingPlayers(), event.getPlayers());

        return Mono.empty();
    }
}
