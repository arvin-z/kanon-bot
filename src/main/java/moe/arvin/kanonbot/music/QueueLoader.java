package moe.arvin.kanonbot.music;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.client.player.*;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
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
    private final ObjectMapper objectMapper = new ObjectMapper();


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
        List<TrackData> trackDataList = queuePersistenceService.loadQueue(guildId);
        if (trackDataList.isEmpty()) {
            return Mono.empty();
        }

        GuildAudioManager gAM = gAMFactory.get(Snowflake.of(guildId));
        AudioTrackScheduler scheduler = gAM.getScheduler();

        return Flux.fromIterable(trackDataList)
                .flatMap(trackData -> loadAndPrepareTrack(guildId, trackData))
                .doOnNext(scheduler::queue)
                .then();
    }

    private Mono<Track> loadAndPrepareTrack(long guildId, TrackData trackData) {
        return lavalinkClient.getOrCreateLink(guildId)
                .loadItem(trackData.url())
                .flatMap(loadResult -> {
                    Track track = null;
                    if (loadResult instanceof TrackLoaded loaded) {
                        track = loaded.getTrack();
                    } else if (loadResult instanceof SearchResult result) {
                        List<Track> tracks = result.getTracks();
                        if (!tracks.isEmpty()) {
                            track = tracks.get(0);
                        }
                    }

                    if (track == null) {
                        return Mono.empty();
                    }

                    final Track finalTrack = track;
                    return getMember(guildId, trackData.memberId())
                            .map(member -> {
                                ObjectNode userData = objectMapper.createObjectNode();
                                userData.put("userId", member.getId().asString());
                                finalTrack.setUserData(userData);
                                return finalTrack;
                            })
                            .defaultIfEmpty(finalTrack); // If member not found, use track without user data
                });
    }


    private Mono<Member> getMember(long guildId, String userId) {
        if (userId == null || "Unknown".equals(userId)) {
            return Mono.empty();
        }
        return discordClient.getGuildById(Snowflake.of(guildId))
                .flatMap(guild -> guild.getMemberById(Snowflake.of(userId)))
                .onErrorResume(e -> Mono.empty()); // If member fetch fails, continue without it
    }

    private Mono<Boolean> isBotInGuild(long guildId) {
        return discordClient.getGuildById(Snowflake.of(guildId))
                .flatMap(Guild::getSelfMember)
                .map(member -> true)
                .defaultIfEmpty(false)
                .onErrorResume(e -> Mono.just(false));
    }
}
