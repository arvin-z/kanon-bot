package moe.arvin.kanonbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AudioTrackScheduler extends AudioEventAdapter {

    private final List<AudioTrack> queue;
    private int nowPlayingIdx;
    private final AudioPlayer player;

    public AudioTrackScheduler(final AudioPlayer player) {
        nowPlayingIdx = -1;
        queue = Collections.synchronizedList(new ArrayList<>());
        this.player = player;
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
                sb.append(ellipsizeAndPad(queue.get(i).toString(), 40));
                sb.append(" ");
                if (i == nowPlayingIdx) {
                    sb.append(queue.get(i).getPosition()).append(" left\n");
                } else {
                    sb.append(queue.get(i).getDuration()).append("\n");
                }
                if (i == nowPlayingIdx) {
                    sb.append("    ⬑ current track                        \n");
                }
            }
            sb.append("\n   This is the end of the queue!\n```");
            return sb.toString();
        }
    }

    public boolean play(final AudioTrack track) {
        return play(track, false);
    }

    public boolean play(final AudioTrack track, final boolean force) {
        queue.add(track);
        final boolean playing = player.startTrack(track, !force);

        if (playing) {
            this.nowPlayingIdx = queue.size() - 1;
        }

        return playing;
    }

    public boolean skip() {
        if (!queue.isEmpty()) {
            if (nowPlayingIdx < queue.size()-1) {
                if (play(queue.get(nowPlayingIdx+1), true)) {
                    nowPlayingIdx++;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            skip();
        }
    }

    public static String ellipsizeAndPad(String s, int maxLength) {
        String ellip = "...";
        String newS;
        if (s == null || s.length() <= maxLength
                || s.length() < ellip.length()) {
            newS = s;
        } else {
            newS = s.substring(0, maxLength - ellip.length()).concat(ellip);
        }
        return String.format("%-" + maxLength + "s", newS);
    }
}
