package moe.arvin.kanonbot.music;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class QueuePersistenceService {

    private static final Logger log = LoggerFactory.getLogger(QueuePersistenceService.class);
    private static final Path LEGACY_QUEUE_DIR = Paths.get("queues");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public QueuePersistenceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public void saveQueue(long guildId, List<TrackData> tracks) {
        if (tracks.isEmpty()) {
            deleteQueue(guildId);
            return;
        }

        jdbcTemplate.update(
                "MERGE INTO restart_queue (guild_id, updated_at) " +
                        "KEY (guild_id) VALUES (?, CURRENT_TIMESTAMP)",
                guildId
        );
        jdbcTemplate.update("DELETE FROM restart_queue_item WHERE guild_id = ?", guildId);
        jdbcTemplate.batchUpdate(
                "INSERT INTO restart_queue_item (guild_id, queue_order, media_url, member_id) " +
                        "VALUES (?, ?, ?, ?)",
                IntStream.range(0, tracks.size()).boxed().toList(),
                tracks.size(),
                (statement, queueOrder) -> {
                    TrackData track = tracks.get(queueOrder);
                    statement.setLong(1, guildId);
                    statement.setInt(2, queueOrder);
                    statement.setString(3, track.url());
                    statement.setString(4, track.memberId());
                }
        );
        log.debug("Saved restart queue for guild {} ({} tracks)", guildId, tracks.size());
    }

    public List<TrackData> loadQueue(long guildId) {
        List<TrackData> tracks = jdbcTemplate.query(
                "SELECT media_url, member_id FROM restart_queue_item " +
                        "WHERE guild_id = ? ORDER BY queue_order",
                (resultSet, rowNumber) -> new TrackData(
                        resultSet.getString("media_url"),
                        resultSet.getString("member_id")
                ),
                guildId
        );
        log.info("Loaded {} restart queue entries for guild {}", tracks.size(), guildId);
        return tracks;
    }

    @Transactional
    public void deleteQueue(long guildId) {
        jdbcTemplate.update("DELETE FROM restart_queue WHERE guild_id = ?", guildId);
        log.debug("Deleted restart queue for guild {}", guildId);
    }

    public List<Long> getAllSavedGuildIds() {
        return jdbcTemplate.queryForList(
                "SELECT guild_id FROM restart_queue ORDER BY guild_id",
                Long.class
        );
    }

    public void migrateLegacyQueues() {
        migrateLegacyQueues(LEGACY_QUEUE_DIR);
    }

    void migrateLegacyQueues(Path queueDirectory) {
        if (!Files.isDirectory(queueDirectory)) {
            return;
        }

        try (var paths = Files.list(queueDirectory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(this::migrateLegacyQueue);
        } catch (IOException error) {
            log.error("Failed to inspect legacy queue directory {}", queueDirectory, error);
        }
    }

    private void migrateLegacyQueue(Path queueFile) {
        String filename = queueFile.getFileName().toString();
        try {
            long guildId = Long.parseLong(filename.substring(0, filename.length() - ".json".length()));
            boolean alreadyMigrated = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM restart_queue WHERE guild_id = ?",
                    Integer.class,
                    guildId
            ) > 0;

            if (!alreadyMigrated) {
                List<TrackData> tracks = objectMapper.readValue(queueFile.toFile(), new TypeReference<>() {
                });
                saveQueue(guildId, tracks);
            }

            Path backupFile = nextBackupPath(queueFile);
            Files.move(queueFile, backupFile);
            log.info("Migrated legacy queue {} to H2; backup retained at {}", guildId, backupFile);
        } catch (IOException | NumberFormatException error) {
            log.error("Failed to migrate legacy queue file {}", queueFile, error);
        }
    }

    private Path nextBackupPath(Path queueFile) {
        Path backupFile = queueFile.resolveSibling(queueFile.getFileName() + ".migrated");
        int suffix = 1;
        while (Files.exists(backupFile)) {
            backupFile = queueFile.resolveSibling(queueFile.getFileName() + ".migrated." + suffix);
            suffix++;
        }
        return backupFile;
    }
}
