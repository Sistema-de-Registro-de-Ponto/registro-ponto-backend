-- Schema inicial (ambientes novos). Bancos já existentes em produção recebem baseline na V1 e aplicam só V2+.

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS colaborators (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    first_name  VARCHAR(120) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_colaborators_user_id (user_id),
    CONSTRAINT fk_colaborators_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS planned_activities (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    collaborator_id BIGINT       NOT NULL,
    description     VARCHAR(500) NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_planned_activities_collaborator (collaborator_id),
    CONSTRAINT fk_planned_activities_collaborator FOREIGN KEY (collaborator_id) REFERENCES colaborators (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS journeys (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    collaborator_id  BIGINT       NOT NULL,
    started_at       DATETIME(6)  NOT NULL,
    ended_at         DATETIME(6)  NULL,
    duration_seconds BIGINT       NULL,
    summary          VARCHAR(2000) NULL,
    status           VARCHAR(20)  NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_journeys_collaborator (collaborator_id),
    CONSTRAINT fk_journeys_collaborator FOREIGN KEY (collaborator_id) REFERENCES colaborators (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS journey_planned_activities (
    id                           BIGINT       NOT NULL AUTO_INCREMENT,
    journey_id                   BIGINT       NOT NULL,
    planned_activity_id          BIGINT       NULL,
    snapshot_planned_activity_id BIGINT       NULL,
    description                  VARCHAR(500) NOT NULL,
    checked                      TINYINT(1)   NOT NULL DEFAULT 0,
    created_at                   DATETIME(6)  NOT NULL,
    updated_at                   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_jpa_journey (journey_id),
    KEY idx_jpa_planned_activity (planned_activity_id),
    CONSTRAINT fk_jpa_journey FOREIGN KEY (journey_id) REFERENCES journeys (id),
    CONSTRAINT fk_jpa_planned_activity FOREIGN KEY (planned_activity_id) REFERENCES planned_activities (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS unplanned_activities (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    journey_id  BIGINT       NOT NULL,
    description VARCHAR(500) NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_unplanned_activities_journey (journey_id),
    CONSTRAINT fk_unplanned_activities_journey FOREIGN KEY (journey_id) REFERENCES journeys (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
