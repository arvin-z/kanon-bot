package moe.arvin.kanonbot.music;

import dev.arbjerg.lavalink.client.LavalinkClient;
import discord4j.common.util.Snowflake;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GuildAudioManagerFactory {

    private final LavalinkClient lavalinkClient;
    private final QueuePersistenceService queuePersistenceService;
    private final Map<Snowflake, GuildAudioManager> MANAGERS = new ConcurrentHashMap<>();

    public GuildAudioManagerFactory(LavalinkClient lavalinkClient, QueuePersistenceService queuePersistenceService) {
        this.lavalinkClient = lavalinkClient;
        this.queuePersistenceService = queuePersistenceService;
    }

    public GuildAudioManager get(Snowflake id) {
        return MANAGERS.computeIfAbsent(id, guildId -> new GuildAudioManager(this.lavalinkClient, guildId.asLong(), this.queuePersistenceService));
    }


}
