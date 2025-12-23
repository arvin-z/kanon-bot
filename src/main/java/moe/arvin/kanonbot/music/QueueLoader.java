package moe.arvin.kanonbot.music;

import dev.arbjerg.lavalink.client.LavalinkClient;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class QueueLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(QueueLoader.class);
    private final GatewayDiscordClient discordClient;
    private final GuildAudioManagerFactory gAMFactory;
    private final QueuePersistenceService queuePersistenceService;
    private final LavalinkClient lavalinkClient;

    public QueueLoader(GatewayDiscordClient discordClient, GuildAudioManagerFactory gAMFactory, QueuePersistenceService queuePersistenceService, LavalinkClient lavalinkClient) {
        this.discordClient = discordClient;
        this.gAMFactory = gAMFactory;
        this.queuePersistenceService = queuePersistenceService;
        this.lavalinkClient = lavalinkClient;
    }

    @Override
    public void run(String @NonNull ... args) {
        log.info("Starting queue loading process...");
        List<Long> savedGuildIds = queuePersistenceService.getAllSavedGuildIds();

        Flux.fromIterable(savedGuildIds)
                .flatMap(guildId -> isBotInGuild(guildId)
                        .flatMap(isMember -> {
                            if (isMember) {
                                return loadGuildQueue(guildId);
                            } else {
                                log.info("Bot is no longer in guild {}, deleting queue file.", guildId);
                                queuePersistenceService.deleteQueue(guildId);
                                return Mono.empty();
                            }
                        }))
                .then()
                .block(); // Block until all guilds are processed

        log.info("Finished queue loading process.");
    }

    private Mono<Void> loadGuildQueue(long guildId) {
        log.info("Re-loading queue for guild: {}", guildId);
        List<String> urls = queuePersistenceService.loadQueue(guildId);
        if (urls.isEmpty()) {
            return Mono.empty();
        }

        GuildAudioManager gAM = gAMFactory.get(Snowflake.of(guildId));
        AudioLoader audioLoader = new AudioLoader(gAM, gAM.getTextChatHandler(), null);

        return Flux.fromIterable(urls)
                .flatMap(url -> lavalinkClient.getOrCreateLink(guildId).loadItem(url))
                .doOnNext(audioLoader) // Use the AudioLoader as a consumer for each loaded item
                .then();
    }

    private Mono<Boolean> isBotInGuild(long guildId) {
        return discordClient.getGuildById(Snowflake.of(guildId))
                .flatMap(Guild::getSelfMember)
                .map(member -> true)
                .defaultIfEmpty(false)
                .onErrorResume(e -> Mono.just(false));
    }
}
