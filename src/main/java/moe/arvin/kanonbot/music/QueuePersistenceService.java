package moe.arvin.kanonbot.music;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class QueuePersistenceService {

    private static final Logger log = LoggerFactory.getLogger(QueuePersistenceService.class);
    private static final Path QUEUE_DIR = Paths.get("queues");
    private final ObjectMapper objectMapper;

    public QueuePersistenceService() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        try {
            Files.createDirectories(QUEUE_DIR);
        } catch (IOException e) {
            log.error("Failed to create queues directory", e);
        }
    }

    public void saveQueue(long guildId, List<TrackData> tracks) {
        if (tracks.isEmpty()) {
            deleteQueue(guildId);
            return;
        }

        Path queueFile = QUEUE_DIR.resolve(guildId + ".json");
        try {
            objectMapper.writeValue(queueFile.toFile(), tracks);
            log.debug("Saved queue for guild {} ({} tracks)", guildId, tracks.size());
        } catch (IOException e) {
            log.error("Failed to save queue for guild {}", guildId, e);
        }
    }

    public List<TrackData> loadQueue(long guildId) {
        Path queueFile = QUEUE_DIR.resolve(guildId + ".json");
        if (!Files.exists(queueFile)) {
            return new ArrayList<>();
        }

        try {
            List<TrackData> tracks = objectMapper.readValue(queueFile.toFile(), new TypeReference<>() {});
            log.info("Loaded {} track data for guild {}", tracks.size(), guildId);
            return tracks;
        } catch (IOException e) {
            log.error("Failed to load queue for guild {}", guildId, e);
            return new ArrayList<>();
        }
    }

    public void deleteQueue(long guildId) {
        Path queueFile = QUEUE_DIR.resolve(guildId + ".json");
        try {
            Files.deleteIfExists(queueFile);
            log.debug("Deleted queue file for guild {}", guildId);
        } catch (IOException e) {
            log.error("Failed to delete queue for guild {}", guildId, e);
        }
    }

    public List<Long> getAllSavedGuildIds() {
        try (var stream = Files.list(QUEUE_DIR)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(path -> path.getFileName().toString().replace(".json", ""))
                    .map(Long::parseLong)
                    .toList();
        } catch (IOException e) {
            log.error("Failed to list saved guild queues", e);
            return new ArrayList<>();
        }
    }
}
