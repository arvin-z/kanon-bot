package moe.arvin.kanonbot.music;

import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class VoiceChatHandler {

    private boolean voiceChannelJoined;
    private final AudioProvider provider;
    private VoiceConnection connection;

    public VoiceChatHandler(LavaPlayerAudioProvider lavaPlayerAudioProvider) {
        this.voiceChannelJoined = false;
        this.provider = lavaPlayerAudioProvider;
    }

    public void joinVoiceChannel(VoiceChannel vc) {
        if (vc != null) {
            connection = vc.join().withProvider(provider).withSelfDeaf(true).block();
            voiceChannelJoined = true;
        }
    }

    public void leaveVoiceChannel() {
        if (connection != null && Boolean.TRUE.equals(connection.isConnected().block())) {
            connection.disconnect().block();
            voiceChannelJoined = false;
        }
    }

    public boolean isVoiceChannelJoined() {
        return voiceChannelJoined;
    }
}
