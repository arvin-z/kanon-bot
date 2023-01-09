package moe.arvin.kanonbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class VoiceChatHandler {

    @Autowired
    private AudioPlayerManager audioPlayerManager;
    private boolean voiceChannelJoined;
    private final GuildAudioManager gAM;
    private VoiceConnection connection;

    public VoiceChatHandler(GuildAudioManager guildAudioManager) {
        this.voiceChannelJoined = false;
        this.gAM = guildAudioManager;
    }

    public void joinVoiceChannel(VoiceChannel vc) {
        if (vc != null) {
            connection = vc.join().withProvider(gAM.getProvider()).withSelfDeaf(true).block();
            voiceChannelJoined = true;
        }
    }

    public void leaveVoiceChannel() {
        if (connection != null && Boolean.TRUE.equals(connection.isConnected().block())) {
            connection.disconnect().block();
            voiceChannelJoined = false;
        }
    }
    
    public void handlePlay(VoiceChannel vc) {
        if (connection == null) {
            joinVoiceChannel(vc);
        } else if (!vc.getId().equals(connection.getChannelId().block())) {
            joinVoiceChannel(vc);
        }
        audioPlayerManager.loadItem("https://www.youtube.com/watch?v=9lNZ_Rnr7Jc", new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                gAM.getScheduler().play(audioTrack);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {

            }

            @Override
            public void noMatches() {

            }

            @Override
            public void loadFailed(FriendlyException e) {
                System.out.println("Something went wrong");
            }
        });
    }

    public boolean isVoiceChannelJoined() {
        return voiceChannelJoined;
    }
}
