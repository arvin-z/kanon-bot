package moe.arvin.kanonbot.listeners.lavalinkListeners;

import dev.arbjerg.lavalink.client.LavalinkNode;
import dev.arbjerg.lavalink.client.event.ReadyEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ReadyListener implements LavalinkListener<ReadyEvent> {


    @Override
    public Class<ReadyEvent> getEventType() {
        return ReadyEvent.class;
    }

    @Override
    public Mono<Void> execute(ReadyEvent event) {
        final LavalinkNode node = event.getNode();

        LOG.info("Node {}: Ready with session ID {}", node.getName(), node.getSessionId());

        return Mono.empty();
    }
}
