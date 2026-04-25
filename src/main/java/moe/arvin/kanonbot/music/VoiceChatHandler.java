package moe.arvin.kanonbot.music;

import dev.arbjerg.lavalink.client.player.LavalinkPlayer;
import dev.arbjerg.lavalink.client.player.Track;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.AudioChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import moe.arvin.kanonbot.util.URLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class VoiceChatHandler {

    private static final Logger LOG = LoggerFactory.getLogger(VoiceChatHandler.class);

    private final GuildAudioManager gAM;
    private boolean voiceChannelJoined;
    private volatile AudioChannel currentChannel;
    private volatile boolean softDisconnecting;
    private final TextChatHandler textChan;
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicReference<ScheduledFuture<?>> reconnectTask = new AtomicReference<>();

    public VoiceChatHandler(GuildAudioManager guildAudioManager) {
        this.voiceChannelJoined = false;
        this.softDisconnecting = false;
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
        AudioChannel vc = vs.getChannel().block();
        return vc != null;
    }

    public boolean joinVoiceChannel(AudioChannel vc) {
        if (vc != null) {
            vc.sendConnectVoiceState(false, true).subscribe();
            voiceChannelJoined = true;
            currentChannel = vc;
            return true;
        } else {
            return false;
        }
    }

    public boolean leaveVoiceChannel(AudioChannel vc) {
        cancelAloneReconnectCycle();
        gAM.getScheduler().stop();
        if (vc != null) {
            if (voiceChannelJoined) {
                vc.sendDisconnectVoiceState().subscribe();
                voiceChannelJoined = false;
                currentChannel = null;
            }
            return true;
        } else {
            return false;
        }
    }

    public void setVoiceChannelJoined(boolean joined) {
        this.voiceChannelJoined = joined;
    }

    public AudioChannel getCurrentChannel() {
        return currentChannel;
    }

    public boolean isSoftDisconnecting() {
        return softDisconnecting;
    }

    public void setSoftDisconnecting(boolean value) {
        this.softDisconnecting = value;
    }

    /**
     * Disconnects from the voice channel without stopping the scheduler or clearing the queue,
     * so playback can resume after rejoining.
     */
    public void softDisconnect() {
        AudioChannel ch = currentChannel;
        if (ch != null && voiceChannelJoined) {
            softDisconnecting = true;
            ch.sendDisconnectVoiceState().subscribe();
            voiceChannelJoined = false;
        }
    }

    /**
     * Starts a cycle that disconnects and rejoins every 30 minutes while the bot is alone.
     * Safe to call multiple times; any existing cycle is cancelled first.
     */
    public void startAloneReconnectCycle() {
        cancelAloneReconnectCycle();
        ScheduledFuture<?> task = reconnectScheduler.scheduleWithFixedDelay(() -> {
            try {
                softDisconnect();
                Thread.sleep(5_000);
                AudioChannel ch = currentChannel;
                if (ch != null) {
                    joinVoiceChannel(ch);
                } else {
                    cancelAloneReconnectCycle();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cancelAloneReconnectCycle();
            } catch (Exception e) {
                LOG.warn("Alone-reconnect cycle failed for guild {}: {}", gAM.getGuildId(), e.getMessage());
                cancelAloneReconnectCycle();
            }
        }, 30, 30, TimeUnit.MINUTES);
        reconnectTask.set(task);
        LOG.debug("Alone-reconnect cycle started for guild {}", gAM.getGuildId());
    }

    /**
     * Cancels any running alone-reconnect cycle.
     */
    public void cancelAloneReconnectCycle() {
        ScheduledFuture<?> task = reconnectTask.getAndSet(null);
        if (task != null) {
            task.cancel(false);
            LOG.debug("Alone-reconnect cycle cancelled for guild {}", gAM.getGuildId());
        }
    }

    public boolean handlePlay(Member mem, String trackArg, MessageChannel messageChannel) {
        // return value: true if the output is handled, false if it isn't
        VoiceState vs = mem.getVoiceState().block();
        if (vs == null) {
            return true;
        }
        AudioChannel vc = vs.getChannel().block();
        if (vc == null) {
            return true;
        }

        // connect to the same VC
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
        if (!URLUtil.isValidURL(trackArg, new String[]{"http", "https"})) {
            if (trackArg.startsWith("deez:")) {
                trackArg = trackArg.replaceFirst("deez:", "dzsearch:");
            } else if (!trackArg.startsWith("ytsearch:") && !trackArg.startsWith("dzsearch:")) {
                trackArg = "ytsearch:" + trackArg;
            }
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
