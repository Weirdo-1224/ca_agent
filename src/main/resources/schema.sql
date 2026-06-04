CREATE TABLE IF NOT EXISTS analysis_task (
    id              BIGSERIAL PRIMARY KEY,
    task_id         VARCHAR(64)  NOT NULL UNIQUE,
    task_name       VARCHAR(255) NOT NULL,
    domain          VARCHAR(64),
    target_products_json TEXT,
    analysis_goal   TEXT,
    status          VARCHAR(32)  NOT NULL,
    iteration_count INTEGER      DEFAULT 0,
    max_iterations  INTEGER      DEFAULT 2,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_analysis_task_task_id ON analysis_task(task_id);

CREATE TABLE IF NOT EXISTS evidence (
    id               BIGSERIAL PRIMARY KEY,
    evidence_id      VARCHAR(64)  NOT NULL UNIQUE,
    task_id          VARCHAR(64)  NOT NULL,
    product_name     VARCHAR(128) NOT NULL,
    source_type      VARCHAR(32),
    source_title     VARCHAR(512),
    url              TEXT,
    content_snippet  TEXT,
    collected_at     TIMESTAMP,
    reliability      VARCHAR(16),
    used_for_json    TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evidence_task_id ON evidence(task_id);
CREATE INDEX IF NOT EXISTS idx_evidence_product_name ON evidence(product_name);

CREATE TABLE IF NOT EXISTS claim (
    id              BIGSERIAL PRIMARY KEY,
    claim_id        VARCHAR(64)  NOT NULL UNIQUE,
    task_id         VARCHAR(64)  NOT NULL,
    product_name    VARCHAR(128) NOT NULL,
    dimension       VARCHAR(64),
    statement       TEXT,
    confidence      NUMERIC(3,2),
    evidence_ids_json TEXT,
    risk_level      VARCHAR(16),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_claim_task_id ON claim(task_id);

CREATE TABLE IF NOT EXISTS report (
    id              BIGSERIAL PRIMARY KEY,
    report_id       VARCHAR(64)  NOT NULL UNIQUE,
    task_id         VARCHAR(64)  NOT NULL UNIQUE,
    report_title    VARCHAR(512),
    report_format   VARCHAR(16),
    sections_json   TEXT,
    source_list_json TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_report_task_id ON report(task_id);

CREATE TABLE IF NOT EXISTS review_issue (
    id                 BIGSERIAL PRIMARY KEY,
    issue_id           VARCHAR(64)  NOT NULL UNIQUE,
    task_id            VARCHAR(64)  NOT NULL,
    severity           VARCHAR(16),
    type               VARCHAR(64),
    description        TEXT,
    target_agent       VARCHAR(32),
    target_product     VARCHAR(128),
    target_dimension   VARCHAR(64),
    repair_instruction TEXT,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_review_issue_task_id ON review_issue(task_id);

CREATE TABLE IF NOT EXISTS agent_run (
    id            BIGSERIAL PRIMARY KEY,
    run_id        VARCHAR(64)  NOT NULL UNIQUE,
    task_id       VARCHAR(64)  NOT NULL,
    agent_type    VARCHAR(32)  NOT NULL,
    input_type    VARCHAR(64),
    output_type   VARCHAR(64),
    status        VARCHAR(32),
    start_time    TIMESTAMP,
    end_time      TIMESTAMP,
    duration_ms   BIGINT,
    error_message TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agent_run_task_id ON agent_run(task_id);

CREATE TABLE IF NOT EXISTS repair_instruction (
    id               BIGSERIAL PRIMARY KEY,
    instruction_id   VARCHAR(64)  NOT NULL UNIQUE,
    task_id          VARCHAR(64)  NOT NULL,
    repair_id        VARCHAR(64),
    from_agent       VARCHAR(32),
    target_agent     VARCHAR(32),
    issue_ids_json   TEXT,
    repair_type      VARCHAR(32),
    target_product   VARCHAR(128),
    target_dimension VARCHAR(64),
    instruction      TEXT,
    priority         VARCHAR(16),
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_repair_instruction_task_id ON repair_instruction(task_id);
