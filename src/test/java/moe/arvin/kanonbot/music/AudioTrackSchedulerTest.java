package moe.arvin.kanonbot.music;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioTrackSchedulerTest {

    @Test
    void shouldNotRestartLocalLoopBeforeLoopEnd() {
        assertFalse(AudioTrackScheduler.shouldRestartLocalLoop(29999, 30000));
    }

    @Test
    void shouldRestartLocalLoopWhenAtLoopEnd() {
        assertTrue(AudioTrackScheduler.shouldRestartLocalLoop(30000, 30000));
    }

    @Test
    void shouldRestartLocalLoopAfterLoopEnd() {
        assertTrue(AudioTrackScheduler.shouldRestartLocalLoop(30001, 30000));
    }
}
