package moe.arvin.kanonbot.music;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.arbjerg.lavalink.client.Link;
import dev.arbjerg.lavalink.client.player.FilterBuilder;
import dev.arbjerg.lavalink.client.player.LavalinkPlayer;
import dev.arbjerg.lavalink.client.player.Track;
import dev.arbjerg.lavalink.protocol.v4.Omissible;
import dev.arbjerg.lavalink.protocol.v4.Timescale;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import dev.arbjerg.lavalink.protocol.v4.Message.EmittedEvent.TrackEndEvent.AudioTrackEndReason;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;


import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AudioTrackScheduler {

    private final GuildAudioManager gAM;
    private final List<Track> queue;
    private final QueuePersistenceService queuePersistenceService;

    private int nowPlayingIdx;

    // 0 = disabled, 1 = looping track, 2 = looping queue
    private int loopState;

    private Timer localLoopTimer;
    private boolean localLoopActive;

    private final TextChatHandler textChat;
    Message playStartMsg;

    private long positionBasis;

    private double currentSpeed;
    private double currentPitch;

    private boolean isTransitioning;
    private int targetIndex; // Add this to track intended index during transitions

    public AudioTrackScheduler(GuildAudioManager guildAudioManager, TextChatHandler txtChat, QueuePersistenceService queuePersistenceService) {
        this.gAM = guildAudioManager;
        this.queuePersistenceService = queuePersistenceService;
        nowPlayingIdx = -1;
        queue = Collections.synchronizedList(new ArrayList<>());
        this.textChat = txtChat;
        this.localLoopActive = false;
        this.localLoopTimer = new Timer();
        this.positionBasis = 0;
        this.currentSpeed = 1.0;
        this.currentPitch = 1.0;
        this.isTransitioning = false;
        this.targetIndex = -1;
    }

    private void persistQueue() {
        if (queuePersistenceService != null) {
            List<TrackData> trackData = queue.stream()
                    .map(track -> {
                        String url = track.getInfo().getUri();
                        String memberId = "Unknown";
                        JsonNode userData = track.getUserData();
                        if (userData.has("userId")) {
                            memberId = userData.get("userId").asText();
                        }
                        return new TrackData(url, memberId);
                    })
                    .collect(Collectors.toList());
            queuePersistenceService.saveQueue(gAM.getGuildId(), trackData);
        }
    }

    public void queue(final Track track) {
        queue.add(track);
        persistQueue();
    }

    public List<Track> getQueue() {
        return queue;
    }

    public void shuffleQueue() {
        if (!isPlaying()) {
            Collections.shuffle(queue);
        } else {
            Track curr = queue.get(nowPlayingIdx);
            Collections.shuffle(queue);
            nowPlayingIdx = queue.indexOf(curr);
        }
        persistQueue();
    }

    public String queueToString(int pageNum, char cmdPrefix) {
        if (queue.isEmpty()) {
            return "```nim\nThe queue is empty ;-;\n```";
        } else {
            int totalEntries = queue.size();
            int entriesPerPage = 15;
            int pageRemainder = totalEntries % entriesPerPage;
            int pageCount = pageRemainder == 0 ? totalEntries / entriesPerPage : totalEntries /entriesPerPage + 1;
            if (pageNum < 1 && nowPlayingIdx >= 0) {
                pageNum = getCurrentPageNum(entriesPerPage);
            } else if (pageNum < 1) {
                pageNum = 1;
            } else if (pageNum > pageCount) {
                pageNum = pageCount;
            }
            StringBuilder sb = new StringBuilder("```nim\n");
            if (pageNum != 1) {
                sb.append("Use ")
                        .append(cmdPrefix)
                        .append("queue ")
                        .append(pageNum - 1)
                        .append(" to see the previous page!\n\n");
            }
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

    public int getCurrentPageNum(int size) {
        return (nowPlayingIdx / size) + 1;
    }

    public String queueStringBuilder(int page, int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = size * (page - 1); i < size * page && i < queue.size(); i++) {
            if (i == nowPlayingIdx) {
                sb.append("    ⬐ current track                        \n");
            }
            sb.append(i + 1).append(") ");
            sb.append(ellipsize(queue.get(i).getInfo().getTitle(), 37, true));
            sb.append(" ");
            if (i == nowPlayingIdx) {
                final Link link = this.gAM.getOrCreateLink();
                final LavalinkPlayer player = link.getCachedPlayer();

                if (player == null) {
                    throw new IllegalStateException("player is null!");
                }

                sb.append(convertMsToHms(queue.get(i).getInfo().getLength()-player.getState().getPosition())).append(" left\n");
            } else {
                sb.append(convertMsToHms(queue.get(i).getInfo().getLength())).append("\n");
            }
            if (i == nowPlayingIdx) {
                sb.append("    ⬑ current track                        \n");
            }
        }
        return sb.toString();
    }

    public long getRelativePosition() {
        final Link link = this.gAM.getOrCreateLink();
        final LavalinkPlayer player = link.getCachedPlayer();

        if (player == null) {
            throw new IllegalStateException("player is null!");
        }

        final Track currTrack = player.getTrack();

        if (currTrack == null) {
            throw new IllegalStateException("track is null!");
        }

        long pos = player.getState().getPosition() - positionBasis;

        double speed;
        Omissible<Timescale> timescaleOmissible = link.getCachedPlayer().getFilters().getTimescale();
        if (timescaleOmissible instanceof Omissible.Present<Timescale> present) {
            speed = present.getValue().getSpeed();
        } else {
            speed = 1.0;
        }

        return (long)(pos * speed + positionBasis);
    }

    public EmbedCreateSpec nowPlayingToEmbed() {
        final Link link = this.gAM.getOrCreateLink();
        final LavalinkPlayer player = link.getCachedPlayer();

        if (player == null) {
            throw new IllegalStateException("player is null!");
        }

        final Track currTrack = player.getTrack();


        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
        if (!isPlaying() || currTrack == null) {
            builder.color(Color.RED);
            builder.description("You must be playing a track to use this command!");
        } else {
            String memID;
            JsonNode userData = currTrack.getUserData();
            if (userData.has("userId")) {
                memID = userData.get("userId").asText();
            } else {
                memID = "Unknown";
            }

            builder.color(Color.MOON_YELLOW);
            builder.description("[" +
                    ellipsize(currTrack.getInfo().getTitle(), 65, false) +
                    "](" + currTrack.getInfo().getUri() + ") [<@" + memID + ">]");
            builder.footer(makeProgressBar(getRelativePosition(), currTrack.getInfo().getLength()), null);
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

    @SuppressWarnings("UnusedReturnValue")
    public boolean playFromStart() {
        if (!queue.isEmpty()) {
            if (nowPlayingIdx >= 0) {
                queue.set(nowPlayingIdx, queue.get(nowPlayingIdx).makeClone());
            }
            targetIndex = 0;
            return play(queue.get(0), true, false);
        }
        return false;
    }

    public boolean play(final Track track, Member mem) {
        if (mem != null) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode userData = mapper.createObjectNode();
            userData.put("userId", mem.getId().asString());
            track.setUserData(userData);
        }
        return play(track, false, true);
    }

    public boolean play(final Track track, final boolean force, final boolean addToQueue) {
        if (addToQueue) {
            queue(track);
            if (queue.size() == 1) {
                targetIndex = 0;
            } else if (nowPlayingIdx == -1) {
                targetIndex = queue.size() - 1;
            }
        }

        AtomicBoolean isPlaying = new AtomicBoolean(false);
        this.gAM.getCachedPlayer().ifPresentOrElse(
                (player) -> {
                    if (player.getTrack() == null || force) {
                        isPlaying.set(true);
                        isTransitioning = true;
                    }
                    this.gAM.getCachedLink().ifPresent(
                            (link) -> link.createOrUpdatePlayer()
                                    .setTrack(track)
                                    .setNoReplace(!force)
                                    .subscribe()
                    );
                },
                () -> {
                    isTransitioning = true;
                    this.gAM.getCachedLink().ifPresent(
                            (link) -> link.createOrUpdatePlayer()
                                    .setTrack(track)
                                    .setNoReplace(!force)
                                    .subscribe()
                    );
                }
        );

        return isPlaying.get();
    }


    @SuppressWarnings("UnusedReturnValue")
    public boolean stop() {
        if (!queue.isEmpty()) {
            this.gAM.getCachedPlayer().ifPresent(
                    (player) -> player.setTrack(null)
                            .subscribe()
            );
            return true;
        }
        return false;
    }

    public void clear() {
        stop();
        queue.clear();
        persistQueue();
    }

    public boolean jump(int trackNum) {
        int i = trackNum - 1;
        if (i < queue.size() && i >= 0) {
            targetIndex = i;
            if (nowPlayingIdx >= 0) {
                queue.set(nowPlayingIdx, queue.get(nowPlayingIdx).makeClone());
            }
            return play(queue.get(i), true, false);
        }
        return false;
    }

    public boolean jumpTitle(String searchArg) {
        if (queue.isEmpty() || searchArg == null) {
            return false;
        }

        String searchArgLower = searchArg.toLowerCase();

        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getInfo().getTitle().toLowerCase().contains(searchArgLower)) {
                return jump(i+1);
            }
        }
        return false;
    }

    public boolean remove(int trackNum) {
        int i = trackNum - 1;
        if (i >= queue.size() || i < 0) {
            return false;
        }

        if (i > nowPlayingIdx) {
            // Removing a track after the current one - just remove it
            queue.remove(i);
        } else if (i < nowPlayingIdx) {
            // Removing a track before the current one
            queue.remove(i);
            // Need to trigger a track restart to update the index
            targetIndex = nowPlayingIdx - 1;
            Track currentTrack = queue.get(targetIndex);
            play(currentTrack, true, false);
        } else {
            // Removing the currently playing track
            queue.remove(i);
            if (queue.isEmpty()) {
                stop();
                targetIndex = -1;
                // nowPlayingIdx will be set to -1 in onTrackEnd
            } else if (i >= queue.size()) {
                // Was last track, move to start if looping
                if (loopState == 2) {
                    targetIndex = 0;
                    playFromStart();
                } else {
                    stop();
                    targetIndex = -1;
                    // nowPlayingIdx will be set to -1 in onTrackEnd
                }
            } else {
                // Play next track in queue
                targetIndex = i; // Keep same index since we removed current
                play(queue.get(i), true, false);
            }
        }
        persistQueue();
        return true;
    }

    public boolean pause() {
        if (isPlaying()) {
            this.gAM.getOrCreateLink()
                    .getPlayer()
                    .flatMap((player) -> player.setPaused(true))
                    .subscribe();
            return true;
        }
        return false;
    }

    public boolean unpause() {
        if (isPlaying()) {
            this.gAM.getOrCreateLink()
                    .getPlayer()
                    .flatMap((player) -> player.setPaused(false))
                    .subscribe();
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
        final Link link = this.gAM.getOrCreateLink();
        final LavalinkPlayer cPlayer = link.getCachedPlayer();
        if (cPlayer == null) {
            return false;
        }
        final Track track = cPlayer.getTrack();

        if (isPlaying() && track != null) {
            long s = Math.abs(sec);
            long ms = s * 1000;
            long dur = track.getInfo().getLength();
            long safeSeek = Math.min(Math.max(ms, 0), dur-1);
            positionBasis = safeSeek;

            link.getPlayer()
                    .flatMap((player) -> player.setPosition(safeSeek))
                    .subscribe();

            return true;
        }
        return false;
    }

    public boolean forwardOrRewind(int sec) {
        final Link link = this.gAM.getOrCreateLink();
        final LavalinkPlayer cPlayer = link.getCachedPlayer();
        if (cPlayer == null) {
            return false;
        }
        final Track track = cPlayer.getTrack();

        if (isPlaying() && track != null) {
            int ms = sec * 1000;
            long dur = track.getInfo().getLength();
            long currPos = getRelativePosition();
            long newPos = currPos + ms;
            long safePos = Math.min(Math.max(newPos, 0), dur-1);
            positionBasis = safePos;
            link.getPlayer()
                    .flatMap((player) -> player.setPosition(safePos))
                    .subscribe();
            return true;
        }
        return false;
    }

    public boolean skip() {
        if (!queue.isEmpty() && nowPlayingIdx != -1) {
            if (nowPlayingIdx < queue.size() - 1 && nowPlayingIdx >= 0) {
                isTransitioning = true;
                targetIndex = nowPlayingIdx + 1;
                return play(queue.get(nowPlayingIdx + 1), true, false);
            } else if (nowPlayingIdx >= queue.size() - 1) {
                if (loopState == 2) {
                    isTransitioning = true;
                    targetIndex = 0;
                    return playFromStart();
                } else {
                    stop();
                    nowPlayingIdx = -1;
                    targetIndex = -1;
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean repeatPrev() {
        if (!queue.isEmpty()) {
            return play(queue.get(nowPlayingIdx), true, false);
        }
        return false;
    }

    public boolean back() {
        if (!queue.isEmpty()) {
            if (nowPlayingIdx < queue.size() && nowPlayingIdx > 0) {
                targetIndex = nowPlayingIdx - 1;
                return play(queue.get(targetIndex), true, false);
            } else if (nowPlayingIdx < queue.size() && nowPlayingIdx == 0) {
                // At first track - replay it
                targetIndex = 0;
                Track cloneTrack = queue.get(0).makeClone();
                boolean played = play(cloneTrack, true, false);
                if (played) {
                    queue.set(0, cloneTrack);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean changeSpeed(double multiplier) {
        if (isPlaying()) {
            final Link link = this.gAM.getOrCreateLink();
            final LavalinkPlayer cPlayer = link.getCachedPlayer();

            if (cPlayer == null) {
                return false;
            }

            positionBasis = getRelativePosition();

            Timescale newTimescale;
            Omissible<Timescale> timescaleOmissible = cPlayer.getFilters().getTimescale();
            if (timescaleOmissible instanceof Omissible.Present<Timescale> present) {
                newTimescale = new Timescale(multiplier, present.getValue().getPitch(), present.getValue().getRate());
            } else {
                newTimescale = new Timescale(multiplier, 1.0, 1.0);
            }

            link.createOrUpdatePlayer()
                    .setFilters(
                            new FilterBuilder()
                                    .setTimescale(
                                            newTimescale
                                    )
                                    .build()
                    )
                    .subscribe();

            this.currentSpeed = multiplier;

            return true;
        }
        return false;
    }

    public boolean changePitch(double val) {
        if (isPlaying()) {
            final Link link = this.gAM.getOrCreateLink();
            final LavalinkPlayer cPlayer = link.getCachedPlayer();

            if (cPlayer == null) {
                return false;
            }

            Timescale newTimescale;
            Omissible<Timescale> timescaleOmissible = cPlayer.getFilters().getTimescale();
            if (timescaleOmissible instanceof Omissible.Present<Timescale> present) {
                newTimescale = new Timescale(present.getValue().getSpeed(), val, present.getValue().getRate());
            } else {
                newTimescale = new Timescale(1.0, val, 1.0);
            }

            link.createOrUpdatePlayer()
                    .setFilters(
                            new FilterBuilder()
                                    .setTimescale(
                                            newTimescale
                                    )
                                    .build()
                    )
                    .subscribe();

            this.currentPitch = val;

            return true;
        }
        return false;
    }

    public EmbedCreateSpec getSpeedEmbed() {
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
        return builder.color(Color.MOON_YELLOW)
                .title("Speed")
                .description(String.format("%.2f", this.currentSpeed) + "x")
                .build();
    }

    public EmbedCreateSpec getPitchEmbed() {
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
        return builder.color(Color.MOON_YELLOW)
                .title("Pitch")
                .description(String.format("%.2f", this.currentPitch) + "x")
                .build();
    }

    public void onTrackStart(Track track) {
        positionBasis = 0;

        // Update nowPlayingIdx based on the transition state
        if (isTransitioning && targetIndex >= 0) {
            nowPlayingIdx = targetIndex;
            isTransitioning = false;
            targetIndex = -1;
        }

        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
        builder.title("Now playing");

        String memID;
        JsonNode userData = track.getUserData();
        if (userData.has("userId")) {
            memID = userData.get("userId").asText();
        } else {
            memID = "Unknown";
        }


        builder.color(Color.MOON_YELLOW);
        builder.description("[" +
                ellipsize(track.getInfo().getTitle(), 65, false) +
                "](" + track.getInfo().getUri() + ") [<@" + memID + ">]");
        this.playStartMsg = textChat.getActiveTextChannel().createMessage(builder.build()).share().block();
    }

    public void onTrackEnd(Track lastTrack, AudioTrackEndReason endReason) {
        if (localLoopActive) {
            localLoopTimer.cancel();
            localLoopTimer = new Timer();
            localLoopActive = false;
        }

        // Use lastTrack instead of currentTrack
        if (lastTrack != null && nowPlayingIdx >= 0 && nowPlayingIdx < queue.size()) {
            queue.set(nowPlayingIdx, lastTrack.makeClone());
        }

        if (this.playStartMsg != null) {
            playStartMsg.delete().share().block();
        }

        if (!isTransitioning) {
            if (endReason.getMayStartNext()) {
                isTransitioning = true;
                if (loopState == 1) {
                    targetIndex = nowPlayingIdx;
                    repeatPrev();
                } else if (nowPlayingIdx < queue.size() - 1) {
                    targetIndex = nowPlayingIdx + 1;
                    skip();
                } else if (loopState == 2) {
                    targetIndex = 0;
                    playFromStart();
                } else {
                    // Remove currentTrack = null since we don't have that field anymore
                    nowPlayingIdx = -1;
                    targetIndex = -1;
                    stop();
                }
            } else {
                // Remove currentTrack = null since we don't have that field anymore
                nowPlayingIdx = -1;
                targetIndex = -1;
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
