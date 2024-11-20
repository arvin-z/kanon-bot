package moe.arvin.kanonbot.music;

import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.client.Link;
import dev.arbjerg.lavalink.client.player.LavalinkPlayer;


public final class GuildAudioManager {

    private final LavalinkClient lavalinkClient;
    private final long guildId;

    private final TextChatHandler textChatHandler;
    private final VoiceChatHandler voiceChatHandler;
    private final AudioTrackScheduler scheduler;

    public GuildAudioManager(LavalinkClient lavalinkClient, long guildId) {
        this.lavalinkClient = lavalinkClient;
        this.guildId = guildId;



        textChatHandler = new TextChatHandler();
        voiceChatHandler = new VoiceChatHandler(this);
        scheduler = new AudioTrackScheduler(this, this.textChatHandler);
    }

    public Link getLink() {
        return this.lavalinkClient.getOrCreateLink(this.guildId);
    }

    public LavalinkPlayer getPlayer() {
        return this.getLink().getPlayer().block();
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
