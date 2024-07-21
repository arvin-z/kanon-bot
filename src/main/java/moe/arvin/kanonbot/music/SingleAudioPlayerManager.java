package moe.arvin.kanonbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;

import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup;
import com.sedmelluq.lava.extensions.youtuberotator.planner.AbstractRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingNanoIpRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.IpBlock;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.*;
import dev.lavalink.youtube.clients.skeleton.Client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;



public final class SingleAudioPlayerManager {

    private static final AudioPlayerManager PLAYER_MANAGER;
    private static final YoutubeAudioSourceManager youtube;
    private static final Logger logger = LoggerFactory.getLogger(SingleAudioPlayerManager.class);

    static  {
        PLAYER_MANAGER = new DefaultAudioPlayerManager();
        PLAYER_MANAGER.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        PLAYER_MANAGER.getConfiguration().setFilterHotSwapEnabled(true);
        youtube = new YoutubeAudioSourceManager(
                true,
                new Client[] { new Music(), new Web(), new AndroidTestsuite(), new TvHtml5Embedded()}
        );
        PLAYER_MANAGER.registerSourceManager(youtube);
        AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER, com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class);
        AudioSourceManagers.registerLocalSource(PLAYER_MANAGER);

    }

    public static void initYoutubeRotation(String ipv6Block) {
        if (ipv6Block != null && !ipv6Block.isEmpty()) {
            logger.info("Enabling IPv6 rotation for YouTube");
            final List<IpBlock> blocks = Collections.singletonList(new Ipv6Block(ipv6Block));
            final AbstractRoutePlanner planner = new RotatingNanoIpRoutePlanner(blocks);
            YoutubeIpRotatorSetup rotator = new YoutubeIpRotatorSetup(planner);
            rotator.forConfiguration(youtube.getHttpInterfaceManager(), false)
                    .withMainDelegateFilter(null)
                    .setup();

        } else {
            logger.info("No IPv6 block set, IP rotation will be disabled");
        }
    }

    public static AudioPlayerManager getInstance() {
        return PLAYER_MANAGER;
    }
}
