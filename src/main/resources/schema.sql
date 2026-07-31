CREATE TABLE IF NOT EXISTS saved_queue (
    scope_id BIGINT NOT NULL,
    normalized_name VARCHAR(80) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (scope_id, normalized_name)
);

CREATE TABLE IF NOT EXISTS saved_queue_item (
    scope_id BIGINT NOT NULL,
    normalized_name VARCHAR(80) NOT NULL,
    queue_order INTEGER NOT NULL,
    media_url VARCHAR(2048) NOT NULL,
    PRIMARY KEY (scope_id, normalized_name, queue_order),
    CONSTRAINT fk_saved_queue_item_queue
        FOREIGN KEY (scope_id, normalized_name)
        REFERENCES saved_queue (scope_id, normalized_name)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS restart_queue (
    guild_id BIGINT NOT NULL PRIMARY KEY,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS restart_queue_item (
    guild_id BIGINT NOT NULL,
    queue_order INTEGER NOT NULL,
    media_url VARCHAR(2048) NOT NULL,
    member_id VARCHAR(32),
    PRIMARY KEY (guild_id, queue_order),
    CONSTRAINT fk_restart_queue_item_queue
        FOREIGN KEY (guild_id)
        REFERENCES restart_queue (guild_id)
        ON DELETE CASCADE
);
