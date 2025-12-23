package moe.arvin.kanonbot.music;

import dev.arbjerg.lavalink.client.AbstractAudioLoadResultHandler;
import dev.arbjerg.lavalink.client.player.PlaylistLoaded;
import dev.arbjerg.lavalink.client.player.SearchResult;
import dev.arbjerg.lavalink.client.player.Track;
import dev.arbjerg.lavalink.client.player.TrackLoaded;
import discord4j.core.object.entity.Member;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioLoader extends AbstractAudioLoadResultHandler {
    private final GuildAudioManager gAM;
    private final Logger LOG = LoggerFactory.getLogger(AudioLoader.class);
    private final TextChatHandler textChan;
    private final Member mem;

    public AudioLoader(GuildAudioManager gAM, TextChatHandler textChan, Member mem) {
        this.gAM = gAM;
        this.textChan = textChan;
        this.mem = mem;
    }

    @Override
    public void ontrackLoaded(@NotNull TrackLoaded trackLoaded) {
        final Track track = trackLoaded.getTrack();
        handleTrack(track);
    }

    @Override
    public void onPlaylistLoaded(@NotNull PlaylistLoaded playlistLoaded) {
        if (playlistLoaded.getTracks().isEmpty()) {
            noMatches();
            return;
        }

        if (playlistLoaded.getTracks().size() == 1) {
            handleTrack(playlistLoaded.getTracks().get(0));
        } else {
            int trackCount = 0;
            for (Track track : playlistLoaded.getTracks()) {
                gAM.getScheduler().play(track, mem);
                trackCount++;
            }
            textChan.sendEmbed(VoiceChatHandler.getQueuedEmbed(trackCount));
        }
    }

    @Override
    public void onSearchResultLoaded(@NotNull SearchResult searchResult) {
        if (searchResult.getTracks().isEmpty()) {
            noMatches();
            return;
        }
        handleTrack(searchResult.getTracks().get(0));
    }

    private void handleTrack(Track track) {
        boolean nowPlaying = gAM.getScheduler().play(track, mem);
        if (!nowPlaying) {
            textChan.sendEmbed(VoiceChatHandler.getQueuedEmbed(track, mem));
        }
    }

    @Override
    public void noMatches() {
        LOG.info("Failed to load track(s): No matches were found");
    }

    @Override
    public void loadFailed(@NotNull dev.arbjerg.lavalink.client.player.LoadFailed loadFailed) {
        LOG.info("Failed to load track(s): {}", loadFailed.getException().getMessage());
    }
}
