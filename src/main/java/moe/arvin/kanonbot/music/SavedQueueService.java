package moe.arvin.kanonbot.music;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.regex.Pattern;

@Service
public class SavedQueueService {

    private static final int MAX_NAME_LENGTH = 80;
    private static final Pattern VALID_NAME = Pattern.compile("[\\p{L}\\p{N} _.-]+");

    private final JdbcTemplate jdbcTemplate;

    public SavedQueueService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void save(long scopeId, String name, List<String> mediaUrls) {
        String displayName = validateName(name);
        if (mediaUrls.isEmpty()) {
            throw new IllegalArgumentException("The current queue is empty.");
        }
        if (mediaUrls.stream().anyMatch(url -> url == null || url.isBlank() || url.length() > 2048)) {
            throw new IllegalArgumentException("The queue contains an invalid media URL.");
        }

        String normalizedName = normalize(displayName);
        jdbcTemplate.update(
                "MERGE INTO saved_queue (scope_id, normalized_name, display_name, updated_at) " +
                        "KEY (scope_id, normalized_name) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                scopeId, normalizedName, displayName
        );
        jdbcTemplate.update(
                "DELETE FROM saved_queue_item WHERE scope_id = ? AND normalized_name = ?",
                scopeId, normalizedName
        );
        jdbcTemplate.batchUpdate(
                "INSERT INTO saved_queue_item (scope_id, normalized_name, queue_order, media_url) " +
                        "VALUES (?, ?, ?, ?)",
                IntStream.range(0, mediaUrls.size()).boxed().toList(),
                mediaUrls.size(),
                (statement, queueOrder) -> {
                    statement.setLong(1, scopeId);
                    statement.setString(2, normalizedName);
                    statement.setInt(3, queueOrder);
                    statement.setString(4, mediaUrls.get(queueOrder));
                }
        );
    }

    public List<String> listNames(long scopeId) {
        return jdbcTemplate.queryForList(
                "SELECT display_name FROM saved_queue WHERE scope_id = ? " +
                        "ORDER BY LOWER(display_name), display_name",
                String.class,
                scopeId
        );
    }

    public Optional<SavedQueue> find(long scopeId, String name) {
        String displayName = validateName(name);
        String normalizedName = normalize(displayName);
        List<String> names = jdbcTemplate.queryForList(
                "SELECT display_name FROM saved_queue WHERE scope_id = ? AND normalized_name = ?",
                String.class,
                scopeId,
                normalizedName
        );
        if (names.isEmpty()) {
            return Optional.empty();
        }

        List<String> mediaUrls = jdbcTemplate.queryForList(
                "SELECT media_url FROM saved_queue_item WHERE scope_id = ? AND normalized_name = ? " +
                        "ORDER BY queue_order",
                String.class,
                scopeId,
                normalizedName
        );
        return Optional.of(new SavedQueue(names.get(0), List.copyOf(mediaUrls)));
    }

    public String validateName(String name) {
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Provide a queue name.");
        }
        if (trimmedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Queue names can be at most " + MAX_NAME_LENGTH + " characters.");
        }
        if (!VALID_NAME.matcher(trimmedName).matches()) {
            throw new IllegalArgumentException(
                    "Queue names may only contain letters, numbers, spaces, periods, underscores, and hyphens."
            );
        }
        return trimmedName;
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
