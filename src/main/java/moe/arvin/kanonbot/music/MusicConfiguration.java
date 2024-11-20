package moe.arvin.kanonbot.music;

import dev.arbjerg.lavalink.client.Helpers;
import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.client.NodeOptions;
import dev.arbjerg.lavalink.client.event.ClientEvent;
import jakarta.annotation.PreDestroy;
import moe.arvin.kanonbot.listeners.lavalinkListeners.LavalinkListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MusicConfiguration {

    @Value("${kanonbot.token}")
    private String token;

    @Value("${kanonbot.lavaname}")
    private String lavaName;

    @Value("${kanonbot.lavauri}")
    private String lavaUri;

    @Value("${kanonbot.lavapass}")
    private String lavaPass;

    private LavalinkClient lavalinkClient;

    @Bean
    public <T extends ClientEvent> LavalinkClient lavalinkClient(List<LavalinkListener<T>> lavalinkListeners) {
        lavalinkClient = new LavalinkClient(Helpers.getUserIdFromToken(token));


        lavalinkClient.addNode(new NodeOptions.Builder()
                .setName(lavaName)
                .setServerUri(lavaUri)
                .setPassword(lavaPass)
                .setHttpTimeout(5000L)
                .build());

        for (LavalinkListener<T> listener : lavalinkListeners) {

            lavalinkClient.on(listener.getEventType())
                    .flatMap(listener::execute)
                    .onErrorResume(listener::handleError)
                    .subscribe();
        }

        return lavalinkClient;

    }

    @PreDestroy
    public void shutdownClient() {
        if (lavalinkClient != null) {
            lavalinkClient.close();
        }
    }


}
