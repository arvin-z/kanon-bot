package moe.arvin.kanonbot.music;

import dev.arbjerg.lavalink.client.player.LavalinkPlayer;
import dev.arbjerg.lavalink.client.player.Track;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import moe.arvin.kanonbot.util.URLUtil;

import java.util.Optional;

public class VoiceChatHandler {

    private final GuildAudioManager gAM;
    private boolean voiceChannelJoined;
    private final TextChatHandler textChan;

    public VoiceChatHandler(GuildAudioManager guildAudioManager) {
        this.voiceChannelJoined = false;
        this.gAM = guildAudioManager;
        this.textChan = this.gAM.getTextChatHandler();
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
            vc.sendConnectVoiceState(false, true).subscribe();
            voiceChannelJoined = true;
            return true;
        } else {
            return false;
        }
    }

    public boolean leaveVoiceChannel(VoiceChannel vc) {
        gAM.getScheduler().stop();
        if (vc != null) {
            if (voiceChannelJoined) {
                vc.sendDisconnectVoiceState().subscribe();
                voiceChannelJoined = false;
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean handlePlay(Member mem, String trackArg, MessageChannel messageChannel) {
        // return value: true if output is handled, false if it isn't
        VoiceState vs = mem.getVoiceState().block();
        if (vs == null) {
            return true;
        }
        VoiceChannel vc = vs.getChannel().block();
        if (vc == null) {
            return true;
        }

        // connect to same VC
        if (!voiceChannelJoined) {
            joinVoiceChannel(vc);
        }

        if (trackArg == null || trackArg.isEmpty()) {
            // command with no args
            // bind chat
            textChan.setActiveTextChannel(messageChannel);

            Optional<LavalinkPlayer> cPlayer = gAM.getCachedPlayer();
            if (cPlayer.isPresent() && cPlayer.get().getPaused()) {
                // resume
                gAM.getOrCreateLink()
                        .getPlayer()
                        .flatMap((player) -> player.setPaused(false))
                        .subscribe();
                return false;
            } else if (!gAM.getScheduler().isPlaying()) {
                // nothing playing
                if (!gAM.getScheduler().getQueue().isEmpty()) {
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

        // load with trackArg
        this.textChan.setActiveTextChannel(messageChannel);
        this.gAM.getOrCreateLink()
                .loadItem(trackArg)
                .subscribe(new AudioLoader(this.gAM, this.textChan, mem));
        return true;
    }

    @SuppressWarnings("unused")
    public boolean isVoiceChannelJoined() {
        return voiceChannelJoined;
    }

    public static EmbedCreateSpec getQueuedEmbed(Track track, Member mem) {
        String memID = mem.getId().asString();
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
        builder.color(Color.MOON_YELLOW);
        builder.description("Queued [" +
                AudioTrackScheduler.ellipsize(track.getInfo().getTitle(), 65, false) +
                "](" + track.getInfo().getUri() + ") [<@" + memID + ">]");
        return builder.build();
    }

    public static EmbedCreateSpec getQueuedEmbed(int numTracks) {
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
        builder.color(Color.MOON_YELLOW);
        builder.description("Queued **" + numTracks + "** tracks");
        return builder.build();
    }

    @SuppressWarnings("unused")
    public static EmbedCreateSpec getNoVCEmbed() {
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
        builder.color(Color.RED);
        builder.description("You have to be connected to a voice channel before you can use this command!");
        return builder.build();
    }
}
