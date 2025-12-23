package moe.arvin.kanonbot.music;

import dev.arbjerg.lavalink.client.AbstractAudioLoadResultHandler;
import dev.arbjerg.lavalink.client.player.*;
import discord4j.core.object.entity.Member;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioLoader extends AbstractAudioLoadResultHandler {
    private final GuildAudioManager gAM;
    Logger LOG = LoggerFactory.getLogger(AudioLoader.class);
    TextChatHandler textChan;
    Member mem;

    public AudioLoader(GuildAudioManager gAM, TextChatHandler textChan, Member mem) {
        this.gAM = gAM;
        this.textChan = textChan;
        this.mem = mem;
    }

    @Override
    public void ontrackLoaded(@NotNull TrackLoaded trackLoaded) {
        final Track track = trackLoaded.getTrack();

        boolean nowPlaying = gAM.getScheduler().play(track, mem);
        if (!nowPlaying && mem != null) {
            textChan.sendEmbed(VoiceChatHandler.getQueuedEmbed(track, mem));
        }
    }

    @Override
    public void onPlaylistLoaded(@NotNull PlaylistLoaded playlistLoaded) {

        if (playlistLoaded.getTracks().size()==1) {
            Track selected = playlistLoaded.getTracks().get(0);
            boolean nowPlaying = gAM.getScheduler().play(selected, mem);
            if (!nowPlaying && mem != null) {
                textChan.sendEmbed(VoiceChatHandler.getQueuedEmbed(selected, mem));
            }
        } else {
            int trackCount = 0;
            for (Track track : playlistLoaded.getTracks()) {
                gAM.getScheduler().play(track, mem);
                trackCount++;
            }
            if (mem != null) {
                textChan.sendEmbed(VoiceChatHandler.getQueuedEmbed(trackCount));
            }
        }

    }

    @Override
    public void onSearchResultLoaded(@NotNull SearchResult searchResult) {
        if (searchResult.getTracks().isEmpty()) {
            noMatches();
            return;
        }
        Track selected = searchResult.getTracks().get(0);
        boolean nowPlaying = gAM.getScheduler().play(selected, mem);
        if (!nowPlaying && mem != null) {
            textChan.sendEmbed(VoiceChatHandler.getQueuedEmbed(selected, mem));
        }
    }

    @Override
    public void noMatches() {
        LOG.info("Failed to load track(s): No matches were found");
    }

    @Override
    public void loadFailed(@NotNull LoadFailed loadFailed) {
        LOG.info("Failed to load track(s): {}", loadFailed.getException().getMessage());
    }
}
