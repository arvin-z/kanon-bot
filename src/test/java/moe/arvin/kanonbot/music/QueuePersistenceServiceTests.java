package moe.arvin.kanonbot.music;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QueuePersistenceServiceTests {

    private QueuePersistenceService service;
    private SavedQueueService savedQueueService;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:restart-queues;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("DELETE FROM restart_queue_item");
        jdbcTemplate.update("DELETE FROM restart_queue");
        jdbcTemplate.update("DELETE FROM saved_queue_item");
        jdbcTemplate.update("DELETE FROM saved_queue");
        service = new QueuePersistenceService(jdbcTemplate);
        savedQueueService = new SavedQueueService(jdbcTemplate);
    }

    @Test
    void storesRestartQueuesInOrderAndDeletesThem() {
        List<TrackData> tracks = List.of(
                new TrackData("https://example.com/one", "100"),
                new TrackData("https://example.com/two", "200")
        );

        service.saveQueue(20L, tracks);
        service.saveQueue(10L, List.of(new TrackData("https://example.com/other", "300")));

        assertThat(service.getAllSavedGuildIds()).containsExactly(10L, 20L);
        assertThat(service.loadQueue(20L)).containsExactlyElementsOf(tracks);
        assertThat(savedQueueService.listNames(20L)).isEmpty();

        service.deleteQueue(20L);
        assertThat(service.loadQueue(20L)).isEmpty();
        assertThat(service.getAllSavedGuildIds()).containsExactly(10L);
    }

    @Test
    void replacingAQueueRemovesItsOldItems() {
        service.saveQueue(10L, List.of(
                new TrackData("https://example.com/old-one", "100"),
                new TrackData("https://example.com/old-two", "100")
        ));
        service.saveQueue(10L, List.of(new TrackData("https://example.com/new", "200")));

        assertThat(service.loadQueue(10L))
                .containsExactly(new TrackData("https://example.com/new", "200"));
    }

    @Test
    void migratesLegacyJsonAndRetainsARecoverableBackup(@TempDir Path legacyDirectory) throws Exception {
        Path legacyFile = legacyDirectory.resolve("42.json");
        List<TrackData> tracks = List.of(
                new TrackData("https://example.com/one", "100"),
                new TrackData("https://example.com/two", "Unknown")
        );
        new ObjectMapper().writeValue(legacyFile.toFile(), tracks);

        service.migrateLegacyQueues(legacyDirectory);

        assertThat(service.loadQueue(42L)).containsExactlyElementsOf(tracks);
        assertThat(legacyFile).doesNotExist();
        assertThat(legacyDirectory.resolve("42.json.migrated")).exists();
    }
}
