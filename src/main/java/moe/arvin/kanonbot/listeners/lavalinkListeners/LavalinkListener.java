package moe.arvin.kanonbot.listeners.lavalinkListeners;

import dev.arbjerg.lavalink.client.event.ClientEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public interface LavalinkListener<T extends ClientEvent> {

    Logger LOG = LoggerFactory.getLogger(LavalinkListener.class);

    Class<T> getEventType();
    Mono<Void> execute(T event);

    default Mono<Void> handleError(Throwable error) {
        LOG.error("Unable to process {}", getEventType().getSimpleName(), error);
        return Mono.empty();
    }
}
