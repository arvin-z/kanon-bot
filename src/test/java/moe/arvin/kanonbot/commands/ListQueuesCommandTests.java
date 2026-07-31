package moe.arvin.kanonbot.commands;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListQueuesCommandTests {

    @Test
    void formatsEmptyAndPopulatedQueueLists() {
        assertThat(ListQueuesCommand.formatNames(List.of()))
                .isEqualTo("No queues have been saved here.");
        assertThat(ListQueuesCommand.formatNames(List.of("Favorites", "Road Trip")))
                .isEqualTo("Saved queues:\n- Favorites\n- Road Trip");
    }

    @Test
    void staysWithinDiscordMessageLimit() {
        List<String> names = java.util.stream.IntStream.range(0, 100)
                .mapToObj(index -> "A".repeat(75) + index)
                .toList();

        assertThat(ListQueuesCommand.formatNames(names))
                .hasSizeLessThanOrEqualTo(2000)
                .contains("more");
    }
}
