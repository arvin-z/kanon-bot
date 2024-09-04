package moe.arvin.kanonbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import discord4j.common.util.Snowflake;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public final class GuildAudioManager {

    @Value("${kanonbot.ipv6block}")
    private String ipv6Block;

    @Value("${kanonbot.potoken}")
    private String poToken;

    @Value("${kanonbot.visitordata}")
    private String visitorData;

    @Value("${kanonbot.useoauth}")
    private String useOAuth;

    @Value("${kanonbot.oauthtoken}")
    private String oAuthToken;

    private static final Map<Snowflake, GuildAudioManager> MANAGERS = new ConcurrentHashMap<>();

    public static GuildAudioManager of(final Snowflake id) {
        return MANAGERS.computeIfAbsent(id, ignored -> new GuildAudioManager());
    }

    private final AudioPlayer player;
    private final AudioTrackScheduler scheduler;
    private final LavaPlayerAudioProvider provider;
    private final TextChatHandler textChatHandler;
    private final VoiceChatHandler voiceChatHandler;

    private final FilterChainConfiguration filterChainConfiguration;

    private GuildAudioManager() {
        AudioPlayerManager audioPlayerManager = SingleAudioPlayerManager.getInstance();
        player = audioPlayerManager.createPlayer();
        textChatHandler = new TextChatHandler();
        voiceChatHandler = new VoiceChatHandler(this, audioPlayerManager);
        filterChainConfiguration = new FilterChainConfiguration();
        scheduler = new AudioTrackScheduler(player, textChatHandler, filterChainConfiguration);
        provider = new LavaPlayerAudioProvider(player);
        player.addListener(scheduler);

    }

    @PostConstruct
    private void initialize() {
        SingleAudioPlayerManager.initYoutubeRotation(ipv6Block);
        SingleAudioPlayerManager.initPoToken(poToken, visitorData);
        if (useOAuth.equals("true")) {
            SingleAudioPlayerManager.initOAuth(oAuthToken);
        }
    }

    public AudioPlayer getPlayer() {
        return player;
    }

    public AudioTrackScheduler getScheduler() {
        return scheduler;
    }

    public LavaPlayerAudioProvider getProvider() {
        return provider;
    }

    public TextChatHandler getTextChatHandler() {
        return textChatHandler;
    }

    public VoiceChatHandler getVoiceChatHandler() {
        return voiceChatHandler;
    }

    public FilterChainConfiguration getFilterChainConfiguration() {
        return filterChainConfiguration;
    }
}
