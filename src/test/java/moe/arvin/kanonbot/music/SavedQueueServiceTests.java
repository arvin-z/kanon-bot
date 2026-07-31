package moe.arvin.kanonbot.music;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SavedQueueServiceTests {

    private SavedQueueService service;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:saved-queues;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("schema.sql"));
        populator.execute(dataSource);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("DELETE FROM saved_queue_item");
        jdbcTemplate.update("DELETE FROM saved_queue");
        service = new SavedQueueService(jdbcTemplate);
    }

    @Test
    void savesAndLoadsUrlsInQueueOrderIncludingDuplicates() {
        service.save(10L, "Road Trip", List.of(
                "https://example.com/one",
                "https://example.com/two",
                "https://example.com/one"
        ));

        assertThat(service.find(10L, "road trip"))
                .contains(new SavedQueue("Road Trip", List.of(
                        "https://example.com/one",
                        "https://example.com/two",
                        "https://example.com/one"
                )));
    }

    @Test
    void replacesNamesCaseInsensitivelyAndKeepsScopesSeparate() {
        service.save(10L, "Favorites", List.of("https://example.com/old"));
        service.save(10L, "FAVORITES", List.of("https://example.com/new"));
        service.save(20L, "Favorites", List.of("https://example.com/other-scope"));

        assertThat(service.listNames(10L)).containsExactly("FAVORITES");
        assertThat(service.find(10L, "favorites").orElseThrow().mediaUrls())
                .containsExactly("https://example.com/new");
        assertThat(service.find(20L, "favorites").orElseThrow().mediaUrls())
                .containsExactly("https://example.com/other-scope");
    }

    @Test
    void rejectsEmptyQueuesAndUnsafeNames() {
        assertThatThrownBy(() -> service.save(10L, "Empty", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The current queue is empty.");
        assertThatThrownBy(() -> service.validateName("@everyone"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("may only contain");
    }
}
