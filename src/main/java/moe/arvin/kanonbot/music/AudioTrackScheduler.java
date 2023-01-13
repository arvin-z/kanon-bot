package moe.arvin.kanonbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AudioTrackScheduler extends AudioEventAdapter {

    private final List<AudioTrack> queue;
    private int nowPlayingIdx;
    private final AudioPlayer player;

    private TextChatHandler textChat;
    Message playStartMsg;

    public AudioTrackScheduler(final AudioPlayer player, TextChatHandler txtChat) {
        nowPlayingIdx = -1;
        queue = Collections.synchronizedList(new ArrayList<>());
        this.player = player;
        this.textChat = txtChat;
    }

    public List<AudioTrack> getQueue() {
        return queue;
    }

    public String queueToString() {
        if (queue.isEmpty()) {
            return "```nim\nThe queue is empty ;-;\n```";
        } else {
            StringBuilder sb = new StringBuilder("```nim\n");
            for (int i = 0; i < queue.size(); i++) {
                if (i == nowPlayingIdx) {
                    sb.append("    ⬐ current track                        \n");
                }
                sb.append(i + 1).append(") ");
                sb.append(ellipsize(queue.get(i).getInfo().title, 37, true));
                sb.append(" ");
                if (i == nowPlayingIdx) {
                    sb.append(convertMsToHms(queue.get(i).getDuration()-queue.get(i).getPosition())).append(" left\n");
                } else {
                    sb.append(convertMsToHms(queue.get(i).getDuration())).append("\n");
                }
                if (i == nowPlayingIdx) {
                    sb.append("    ⬑ current track                        \n");
                }
            }
            sb.append("\n   This is the end of the queue!\n```");
            return sb.toString();
        }
    }

    public EmbedCreateSpec nowPlayingToEmbed() {
        AudioTrack currTrack = player.getPlayingTrack();
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
        if (!isPlaying()) {
            builder.color(Color.RED);
            builder.description("You must be playing a track to use this command!");
        } else {
            String memID = (String) currTrack.getUserData();
            builder.color(Color.MOON_YELLOW);
            builder.description("[" +
                    ellipsize(currTrack.getInfo().title, 65, false) +
                    "](" + currTrack.getInfo().uri + ") [<@" + memID + ">]");
            builder.footer(makeProgressBar(currTrack.getPosition(), currTrack.getDuration()), null);
        }
        return builder.build();
    }

    public boolean isPlaying() {
        if (!queue.isEmpty()) {
            return nowPlayingIdx < queue.size() && nowPlayingIdx >= 0;
        }
        return false;
    }

    public boolean playFromStart() {
        if (queue.size() > 0) {
            if (play(queue.get(0), true, false)) {
                nowPlayingIdx = 0;
                return true;
            }
        }
        return false;
    }

    public boolean play(final AudioTrack track, Member mem) {
        track.setUserData(mem.getId().asString());
        return play(track, false, true);
    }

    public boolean play(final AudioTrack track, final boolean force, final boolean addToQueue) {
        if (addToQueue) {
            queue.add(track);
        }
        if (queue.size() == 1) {
            this.nowPlayingIdx = 0;
        } else if (nowPlayingIdx == -1) {
            nowPlayingIdx = queue.size() - 1;
        }

        return player.startTrack(track, !force);
    }

    public boolean stop() {
        if (!queue.isEmpty()) {
            if (player.getPlayingTrack() != null) {
                player.stopTrack();
                nowPlayingIdx = -1;
                return true;
            }
        }
        return false;
    }

    public boolean skip() {
        if (!queue.isEmpty()) {
            if (nowPlayingIdx < queue.size()-1 && nowPlayingIdx >= 0) {
                if (play(queue.get(nowPlayingIdx+1), true, false)) {
                    nowPlayingIdx++;
                    return true;
                }
            } else if (nowPlayingIdx >= queue.size()-1) {
                stop();
                nowPlayingIdx = -1;
            }
        }
        return false;
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
        builder.title("Now playing");
        String memID = (String) track.getUserData();
        builder.color(Color.MOON_YELLOW);
        builder.description("[" +
                ellipsize(track.getInfo().title, 65, false) +
                "](" + track.getInfo().uri + ") [<@" + memID + ">]");
        this.playStartMsg = textChat.getActiveTextChannel().createMessage(builder.build()).block();
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        queue.set(nowPlayingIdx, track.makeClone());
        if (this.playStartMsg != null) {
            playStartMsg.delete().share().block();
        }
        if (endReason.mayStartNext) {
            skip();
        }
    }

    public static String makeProgressBar(long pos, long dur) {
        double progress = ((double) pos / dur);
        int progressIndex = (int) (progress * 20);
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<20; i++) {
            if (i == progressIndex) {
                sb.append("\uD83D\uDD35");
            }
            sb.append("▬");
        }
        if (progressIndex == 20) {
            sb.append("\uD83D\uDD35");
        }
        sb.append(" ").append(convertMsToTextHms(pos)).append(" / ").append(convertMsToTextHms(dur));
        return sb.toString();
    }

    public static String ellipsize(String s, int maxLength, boolean pad) {
        String ellip = "…";
        String newS;
        if (s == null) {
            newS = null;
        } else if (s.length() <= maxLength
                || s.length() < ellip.length()) {
            newS = s.replace("[" , "［").replace("]", "］");
        } else {
            newS = s.substring(0, maxLength - ellip.length()).trim().concat(ellip)
                    .replace("[" , "［").replace("]", "］");
        }
        if (pad) {
            return String.format("%-" + maxLength + "s", newS);
        } else{
            return newS;
        }
    }

    public static String convertMsToHms(long ms) {
        Duration duration = Duration.ofMillis(ms);
        long seconds = duration.getSeconds();
        long HH = seconds / 3600;
        long MM = (seconds % 3600) / 60;
        long SS = seconds % 60;
        if (HH == 0) {
            return String.format("%02d:%02d", MM, SS);
        } else {
            return String.format("%02d:%02d:%02d", HH, MM, SS);
        }

    }

    public static String convertMsToTextHms(long ms) {
        Duration duration = Duration.ofMillis(ms);
        long seconds = duration.getSeconds();
        long HH = seconds / 3600;
        long MM = (seconds % 3600) / 60;
        long SS = seconds % 60;
        if (HH == 0 && MM == 0) {
            return String.format("%ds", SS);
        } else if (HH == 0) {
            return String.format("%dm %ds", MM, SS);
        } else {
            return String.format("%dh %dm %ds", HH, MM, SS);
        }

    }


}
