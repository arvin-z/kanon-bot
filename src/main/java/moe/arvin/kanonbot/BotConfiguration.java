package moe.arvin.kanonbot;


import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.libraries.discord4j.D4JVoiceHandler;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.gateway.intent.IntentSet;
import jakarta.annotation.PreDestroy;
import moe.arvin.kanonbot.listeners.EventListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class BotConfiguration {
    @Value("${kanonbot.token}")
    private String token;

    private GatewayDiscordClient discordClient;


    @Bean
    public <T extends Event> GatewayDiscordClient gatewayDiscordClient(List<EventListener<T>> eventListeners, LavalinkClient lavalinkClient) {
        discordClient = DiscordClientBuilder.create(token).build()
                .gateway()
                .setEnabledIntents(IntentSet.all())
                .setInitialPresence(ignore -> ClientPresence.online(ClientActivity.listening(" ")))
                .login()
                .block();

        if (discordClient == null) {
            return null;
        }

        D4JVoiceHandler.install(discordClient, lavalinkClient);

        for (EventListener<T> listener : eventListeners) {

            discordClient.on(listener.getEventType())
                    .flatMap(listener::execute)
                    .onErrorResume(listener::handleError)
                    .subscribe();
        }

        return discordClient;
    }

    @PreDestroy
    public void shutdownDiscordClient() {
        if (discordClient != null) {
            discordClient.logout().block();
        }
    }
}
