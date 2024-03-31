package moe.arvin.kanonbot.music;

import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

public class AudioTrackScheduler extends AudioEventAdapter {

    private final List<AudioTrack> queue;
    private int nowPlayingIdx;
    private final AudioPlayer player;

    // 0 = disabled, 1 = looping track, 2 = looping queue
    private int loopState;

    private Timer localLoopTimer;
    private boolean localLoopActive;

    private TextChatHandler textChat;
    Message playStartMsg;

    private FilterChainConfiguration filterChainConfiguration;

    private long positionBasis;

    public AudioTrackScheduler(final AudioPlayer player, TextChatHandler txtChat, FilterChainConfiguration fcc) {
        nowPlayingIdx = -1;
        queue = Collections.synchronizedList(new ArrayList<>());
        this.player = player;
        this.textChat = txtChat;
        this.filterChainConfiguration = fcc;
        this.player.setFrameBufferDuration(300);
        this.localLoopActive = false;
        this.localLoopTimer = new Timer();
        this.positionBasis = 0;
    }

    public List<AudioTrack> getQueue() {
        return queue;
    }

    public void shuffleQueue() {
        if (!isPlaying()) {
            Collections.shuffle(queue);
        } else {
            AudioTrack curr = queue.get(nowPlayingIdx);
            Collections.shuffle(queue);
            nowPlayingIdx = queue.indexOf(curr);
        }
    }

    public String queueToString(int pageNum, char cmdPrefix) {
        if (queue.isEmpty()) {
            return "```nim\nThe queue is empty ;-;\n```";
        } else {
            int totalEntries = queue.size();
            int entriesPerPage = 15;
            int pageRemainder = totalEntries % entriesPerPage;
            int pageCount = pageRemainder == 0 ? totalEntries / entriesPerPage : totalEntries /entriesPerPage + 1;
            if (pageNum < 1) {
                pageNum = 1;
            } else if (pageNum > pageCount) {
                pageNum = pageCount;
            }
            StringBuilder sb = new StringBuilder("```nim\n");
            sb.append(queueStringBuilder(pageNum, entriesPerPage));
            if (pageNum == pageCount) {
                sb.append("\n   This is the end of the queue!\n");
            }
            else {
                sb.append("\nUse ")
                        .append(cmdPrefix)
                        .append("queue ")
                        .append(pageNum + 1)
                        .append(" to see the next page!\n");
            }
            sb.append("```");
            return sb.toString();
        }
    }

    public String queueStringBuilder(int page, int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = size * (page - 1); i < size * page && i < queue.size(); i++) {
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
        return sb.toString();
    }

    public long getRelativePosition() {
        AudioTrack currTrack = player.getPlayingTrack();
        long pos = currTrack.getPosition() - positionBasis;
        double speed = filterChainConfiguration.timescale().speed();
        return (long)(pos * speed + positionBasis);
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
            builder.footer(makeProgressBar(getRelativePosition(), currTrack.getDuration()), null);
        }
        return builder.build();
    }

    public boolean isPlaying() {
        if (!queue.isEmpty()) {
            return nowPlayingIdx < queue.size() && nowPlayingIdx >= 0;
        }
        return false;
    }

    public int toggleLoop() {

        if (loopState == 0) {
            this.loopState = 1;
        } else if (loopState == 1) {
            this.loopState = 2;
        } else if (loopState == 2) {
            loopState = 0;
        }
        return loopState;
    }

    public boolean playFromStart() {
        if (queue.size() > 0) {
            if (nowPlayingIdx >= 0) {
                queue.set(nowPlayingIdx, queue.get(nowPlayingIdx).makeClone());
            }
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

    public void clear() {
        stop();
        queue.clear();
    }

    public boolean jump(int trackNum) {
        int i = trackNum-1;
        if (i < queue.size() && i >= 0) {
            if (nowPlayingIdx >= 0) {
                queue.set(nowPlayingIdx, queue.get(nowPlayingIdx).makeClone());
            }
            if (play(queue.get(i), true, false)) {
                nowPlayingIdx = i;
                return true;
            }
        }
        return false;
    }

    public boolean remove(int trackNum) {
        int i = trackNum-1;
        if (i >= queue.size() || i < 0) {
            return false;
        }
        if (i > nowPlayingIdx) {
            queue.remove(i);
        } else if (i < nowPlayingIdx) {
            queue.remove(i);
            nowPlayingIdx--;
        } else {
            queue.remove(i);
            if (queue.isEmpty()) {
                stop();
            } else {
                nowPlayingIdx--;
                play(queue.get(nowPlayingIdx), true, false);
            }
        }
        return true;
    }

    public boolean pause() {
        if (isPlaying()) {
            player.setPaused(true);
            return true;
        }
        return false;
    }

    public boolean unpause() {
        if (isPlaying()) {
            player.setPaused(false);
            return true;
        }
        return false;
    }

    public boolean localLoop(int s, int e) {
        if (isPlaying()) {
            int dur = e - s;
            if (localLoopActive) {
                localLoopTimer.cancel();
                localLoopTimer = new Timer();
            }
            localLoopTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    seek(s);
                }
            }, 0, dur * 1000L);
            localLoopActive = true;
            return true;
        }
        return false;
    }

    public boolean localLoop() {
        if (isPlaying()) {
            if (localLoopActive) {
                localLoopTimer.cancel();
                localLoopTimer = new Timer();
                localLoopActive = false;
                return true;
            }
        }
        return false;
    }

    public boolean seek(int sec) {
        if (isPlaying()) {
            long s = (long) Math.abs(sec);
            long ms = s * 1000;
            long dur = player.getPlayingTrack().getDuration();
            long safeSeek = Math.min(Math.max(ms, 0), dur-1);
            positionBasis = safeSeek;
            player.getPlayingTrack().setPosition(safeSeek);
            return true;
        }
        return false;
    }

    public boolean forwardOrRewind(int sec) {
        if (isPlaying()) {
            int ms = sec * 1000;
            long dur = player.getPlayingTrack().getDuration();
            long currPos = getRelativePosition();
            long newPos = currPos + ms;
            long safePos = Math.min(Math.max(newPos, 0), dur-1);
            positionBasis = safePos;
            player.getPlayingTrack().setPosition(safePos);
            return true;
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
                if (loopState == 2) {
                    nowPlayingIdx = 0;
                    playFromStart();
                } else {
                    stop();
                    nowPlayingIdx = -1;
                }
                return true;
            }
        }
        return false;
    }

    public boolean repeatPrev() {
        if (!queue.isEmpty()) {
            return play(queue.get(nowPlayingIdx), true, false);
        }
        return false;
    }

    public boolean back() {
        if (!queue.isEmpty()) {
            if (nowPlayingIdx < queue.size() && nowPlayingIdx > 0) {
                if (play(queue.get(nowPlayingIdx-1), true, false)) {
                    nowPlayingIdx--;
                    return true;
                }
            } else if (nowPlayingIdx < queue.size() && nowPlayingIdx == 0) {
                AudioTrack cloneTrack = queue.get(nowPlayingIdx).makeClone();
                boolean played = play(cloneTrack, true, false);
                if (played) {
                    queue.set(nowPlayingIdx, cloneTrack);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean changeSpeed(double multiplier) {
        if (isPlaying()) {
            positionBasis = getRelativePosition();
            filterChainConfiguration.timescale().setSpeed(multiplier);
            this.player.setFilterFactory(filterChainConfiguration.factory());
            return true;
        }
        return false;
    }

    public boolean changePitch(double val) {
        if (isPlaying()) {
            filterChainConfiguration.timescale().setPitch(val);
            this.player.setFilterFactory(filterChainConfiguration.factory());
            return true;
        }
        return false;
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        System.out.println(nowPlayingIdx);
        positionBasis = 0;
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
        builder.title("Now playing");
        String memID = (String) track.getUserData();
        builder.color(Color.MOON_YELLOW);
        builder.description("[" +
                ellipsize(track.getInfo().title, 65, false) +
                "](" + track.getInfo().uri + ") [<@" + memID + ">]");
        this.playStartMsg = textChat.getActiveTextChannel().createMessage(builder.build()).share().block();
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (localLoopActive) {
            localLoopTimer.cancel();
            localLoopTimer = new Timer();
            localLoopActive = false;
        }
        queue.set(nowPlayingIdx, track.makeClone());
        if (this.playStartMsg != null) {
            playStartMsg.delete().share().block();
        }
        if (endReason.mayStartNext) {
            if (loopState == 1) {
                repeatPrev();
            } else {
                skip();
            }
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
