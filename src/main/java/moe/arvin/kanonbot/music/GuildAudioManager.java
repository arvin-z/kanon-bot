package moe.arvin.kanonbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import discord4j.common.util.Snowflake;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public final class GuildAudioManager {

    private static final Map<Snowflake, GuildAudioManager> MANAGERS = new ConcurrentHashMap<>();

    public static GuildAudioManager of(final Snowflake id) {
        return MANAGERS.computeIfAbsent(id, ignored -> new GuildAudioManager());
    }

    private final AudioPlayer player;
    private final AudioTrackScheduler scheduler;
    private final LavaPlayerAudioProvider provider;
    private final TextChatHandler textChatHandler;
    private final VoiceChatHandler voiceChatHandler;

    private GuildAudioManager() {
        AudioPlayerManager audioPlayerManager = SingleAudioPlayerManager.getInstance();
        player = audioPlayerManager.createPlayer();
        textChatHandler = new TextChatHandler();
        voiceChatHandler = new VoiceChatHandler(this, audioPlayerManager);
        scheduler = new AudioTrackScheduler(player, textChatHandler);
        provider = new LavaPlayerAudioProvider(player);
        player.addListener(scheduler);

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
}
