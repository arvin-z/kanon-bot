package moe.arvin.kanonbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.AndroidWithThumbnail;
import dev.lavalink.youtube.clients.MusicWithThumbnail;
import dev.lavalink.youtube.clients.WebWithThumbnail;
import dev.lavalink.youtube.clients.skeleton.Client;

public final class SingleAudioPlayerManager {

    private static final AudioPlayerManager PLAYER_MANAGER;

    static  {
        PLAYER_MANAGER = new DefaultAudioPlayerManager();
        PLAYER_MANAGER.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        PLAYER_MANAGER.getConfiguration().setFilterHotSwapEnabled(true);
        AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER, com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class);
        AudioSourceManagers.registerLocalSource(PLAYER_MANAGER);
        YoutubeAudioSourceManager youtube = new YoutubeAudioSourceManager(
                true,
                new Client[] { new MusicWithThumbnail(), new WebWithThumbnail(), new AndroidWithThumbnail() }
        );
        PLAYER_MANAGER.registerSourceManager(youtube);
    }

    public static AudioPlayerManager getInstance() {
        return PLAYER_MANAGER;
    }
}
