package moe.arvin.kanonbot.music;

import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.client.Link;
import dev.arbjerg.lavalink.client.player.LavalinkPlayer;

import java.util.Optional;


public final class GuildAudioManager {

    private final LavalinkClient lavalinkClient;
    private final long guildId;

    private final TextChatHandler textChatHandler;
    private final VoiceChatHandler voiceChatHandler;
    private final AudioTrackScheduler scheduler;

    public GuildAudioManager(LavalinkClient lavalinkClient, long guildId, QueuePersistenceService queuePersistenceService) {
        this.lavalinkClient = lavalinkClient;
        this.guildId = guildId;



        textChatHandler = new TextChatHandler();
        voiceChatHandler = new VoiceChatHandler(this);
        scheduler = new AudioTrackScheduler(this, this.textChatHandler, queuePersistenceService);
    }

    public long getGuildId() {
        return guildId;
    }

    public Link getOrCreateLink() {
        return this.lavalinkClient.getOrCreateLink(this.guildId);
    }

    public Optional<Link> getCachedLink() {
        return Optional.ofNullable(
                this.lavalinkClient.getLinkIfCached(this.guildId)
        );
    }

    public Optional<LavalinkPlayer> getCachedPlayer() {
        return this.getCachedLink().map(Link::getCachedPlayer);
    }

    public TextChatHandler getTextChatHandler() {
        return textChatHandler;
    }

    public VoiceChatHandler getVoiceChatHandler() {
        return voiceChatHandler;
    }

    public AudioTrackScheduler getScheduler() {
        return scheduler;
    }

}
