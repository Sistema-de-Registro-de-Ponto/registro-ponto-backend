-- Evolução para bancos criados antes do Flyway (ex.: produção com Hibernate ddl-auto).
-- Idempotente: seguro se a coluna/constraint já existir no estado desejado.

-- journeys: campos do check-out
SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'journeys'
      AND COLUMN_NAME = 'duration_seconds'
);
SET @sql := IF(
    @col_exists = 0,
    'ALTER TABLE journeys ADD COLUMN duration_seconds BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'journeys'
      AND COLUMN_NAME = 'summary'
);
SET @sql := IF(
    @col_exists = 0,
    'ALTER TABLE journeys ADD COLUMN summary VARCHAR(2000) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- journey_planned_activities: snapshot do backlog + FK opcional após encerrar jornada
SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'journey_planned_activities'
      AND COLUMN_NAME = 'snapshot_planned_activity_id'
);
SET @sql := IF(
    @col_exists = 0,
    'ALTER TABLE journey_planned_activities ADD COLUMN snapshot_planned_activity_id BIGINT NULL AFTER planned_activity_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Preenche snapshot para linhas antigas ainda ligadas ao backlog
UPDATE journey_planned_activities
SET snapshot_planned_activity_id = planned_activity_id
WHERE snapshot_planned_activity_id IS NULL
  AND planned_activity_id IS NOT NULL;

ALTER TABLE journey_planned_activities
    MODIFY COLUMN planned_activity_id BIGINT NULL;
