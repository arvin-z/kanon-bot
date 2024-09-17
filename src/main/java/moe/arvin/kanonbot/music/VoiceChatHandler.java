package moe.arvin.kanonbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import moe.arvin.kanonbot.util.URLUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

public class VoiceChatHandler {

    private final AudioPlayerManager audioPlayerManager;
    private boolean voiceChannelJoined;
    private final GuildAudioManager gAM;
    private VoiceConnection connection;
    private final TextChatHandler textChan;

    public VoiceChatHandler(GuildAudioManager guildAudioManager, AudioPlayerManager audioPlayerManager) {
        this.voiceChannelJoined = false;
        this.gAM = guildAudioManager;
        this.textChan = this.gAM.getTextChatHandler();
        this.audioPlayerManager = audioPlayerManager;
    }

    public boolean userInVoiceChannelFromMsg(Message m) {
        Member gM = m.getAuthorAsMember().block();
        if (gM == null) {
            return false;
        }
        VoiceState vs = gM.getVoiceState().block();
        if (vs == null) {
            return false;
        }
        VoiceChannel vc = vs.getChannel().block();
        return vc != null;
    }

    public boolean joinVoiceChannel(VoiceChannel vc) {
        if (vc != null) {
            connection = vc.join().withProvider(gAM.getProvider()).withSelfDeaf(true).block();
            voiceChannelJoined = true;
            return true;
        } else {
            return false;
        }
    }

    public void leaveVoiceChannel() {
        gAM.getScheduler().stop();
        if (connection != null && Boolean.TRUE.equals(connection.isConnected().block())) {
            // pause player
            connection.disconnect().block();
            voiceChannelJoined = false;

        }
    }
    
    public boolean handlePlay(Member mem, String trackArg, MessageChannel messageChannel) {
        // return value: true if output is handled, false if it isn't
        VoiceState vs = mem.getVoiceState().block();
        if (vs == null) {
            TextChatHandler.sendErrorEmbedToMsgChannel(messageChannel,
                    "You have to be connected to a voice channel before you can use this command!");
            return true;
        }
        VoiceChannel vc = vs.getChannel().block();
        if (vc == null) {
            TextChatHandler.sendErrorEmbedToMsgChannel(messageChannel,
                    "You have to be connected to a voice channel before you can use this command!");
            return true;
        }

        // connect to same VC
        if (connection == null) {
            joinVoiceChannel(vc);
        } else if (!vc.getId().equals(connection.getChannelId().block())) {
            joinVoiceChannel(vc);
        }

        if (trackArg == null || trackArg.isEmpty()) {
            // command with no args
            // bind chat
            textChan.setActiveTextChannel(messageChannel);
            if (gAM.getPlayer().isPaused()) {
                // resume
                gAM.getPlayer().setPaused(false);
                return false;
            } else if (!gAM.getScheduler().isPlaying()) {
                // nothing playing
                if (gAM.getScheduler().getQueue().size() > 0) {
                    // start from start
                    gAM.getScheduler().playFromStart();
                }
                return true;
            } else {
                return true;
            }
        }
        String[] URLPrefixes = {"http", "https"};
        if (!URLUtil.isValidURL(trackArg, URLPrefixes)) {
            trackArg = "ytsearch: " + trackArg;
        }

        audioPlayerManager.loadItem(trackArg, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                textChan.setActiveTextChannel(messageChannel);
                boolean nowPlaying = gAM.getScheduler().play(audioTrack, mem);
                if (!nowPlaying) {
                    textChan.sendEmbed(getQueuedEmbed(audioTrack, mem));
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                textChan.setActiveTextChannel(messageChannel);
                if (audioPlaylist.getTracks().size()==1 || audioPlaylist.isSearchResult()) {
                    AudioTrack selected = audioPlaylist.getSelectedTrack()==null ?
                            audioPlaylist.getTracks().get(0) : audioPlaylist.getSelectedTrack();
                    boolean nowPlaying = gAM.getScheduler().play(selected, mem);
                    if (!nowPlaying) {
                        textChan.sendEmbed(getQueuedEmbed(selected, mem));
                    }
                } else if (audioPlaylist.getSelectedTrack() != null) {
                    boolean nowPlaying = gAM.getScheduler().play(audioPlaylist.getSelectedTrack(), mem);
                    if (!nowPlaying) {
                        textChan.sendEmbed(getQueuedEmbed(audioPlaylist.getSelectedTrack(), mem));
                    }
                } else {
                    int trackCount = 0;
                    for (AudioTrack audioTrack : audioPlaylist.getTracks()) {
                        gAM.getScheduler().play(audioTrack, mem);
                        trackCount++;
                    }
                    textChan.sendEmbed(getQueuedEmbed(trackCount));
                }

            }

            @Override
            public void noMatches() {

            }

            @Override
            public void loadFailed(FriendlyException e) {
                System.out.println("Something went wrong");
            }
        });
        return true;
    }

    public boolean isVoiceChannelJoined() {
        return voiceChannelJoined;
    }

    public EmbedCreateSpec getQueuedEmbed(AudioTrack track, Member mem) {
        String memID = mem.getId().asString();
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
        builder.color(Color.MOON_YELLOW);
        builder.description("Queued [" +
                AudioTrackScheduler.ellipsize(track.getInfo().title, 65, false) +
                "](" + track.getInfo().uri + ") [<@" + memID + ">]");
        return builder.build();
    }

    public EmbedCreateSpec getQueuedEmbed(int numTracks) {
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
        builder.color(Color.MOON_YELLOW);
        builder.description("Queued **" + Integer.toString(numTracks) + "** tracks");
        return builder.build();
    }

    public EmbedCreateSpec getNoVCEmbed() {
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
        builder.color(Color.RED);
        builder.description("You have to be connected to a voice channel before you can use this command!");
        return builder.build();
    }
}
