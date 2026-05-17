CREATE TABLE IF NOT EXISTS managers (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    first_name  VARCHAR(120) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_managers_user_id (user_id),
    CONSTRAINT fk_managers_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
