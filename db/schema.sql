-- AssessX Database Schema

CREATE TABLE users (
    id           BIGSERIAL PRIMARY KEY,
    github_id    BIGINT      UNIQUE NOT NULL,
    github_login VARCHAR(100) UNIQUE NOT NULL,
    name         VARCHAR(255) NOT NULL,
    role         VARCHAR(20)  NOT NULL CHECK (role IN ('STUDENT', 'TEACHER')),
    created_at   TIMESTAMP   DEFAULT NOW()
);

CREATE TABLE groups (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE user_groups (
    user_id  BIGINT REFERENCES users(id)  ON DELETE CASCADE,
    group_id BIGINT REFERENCES groups(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, group_id)
);

CREATE TABLE tests (
    id             BIGSERIAL PRIMARY KEY,
    title          VARCHAR(255) NOT NULL,
    questions      JSONB        NOT NULL,
    answers        JSONB        NOT NULL,
    points         INTEGER      NOT NULL CHECK (points > 0),
    time_limit_sec INTEGER      NOT NULL CHECK (time_limit_sec > 0),
    created_by     BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    created_at     TIMESTAMP    DEFAULT NOW()
);

CREATE TABLE code_practices (
    id             BIGSERIAL PRIMARY KEY,
    title          VARCHAR(255) NOT NULL,
    description    TEXT         NOT NULL,
    points         INTEGER      NOT NULL CHECK (points > 0),
    time_limit_sec INTEGER      NOT NULL CHECK (time_limit_sec > 0),
    created_by     BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    created_at     TIMESTAMP    DEFAULT NOW()
);

CREATE TABLE practice_unit_tests (
    id          BIGSERIAL PRIMARY KEY,
    practice_id BIGINT NOT NULL REFERENCES code_practices(id) ON DELETE CASCADE,
    test_code   TEXT   NOT NULL
);

CREATE TABLE assignments (
    id          BIGSERIAL PRIMARY KEY,
    group_id    BIGINT    REFERENCES groups(id)        ON DELETE CASCADE,
    test_id     BIGINT    REFERENCES tests(id)         ON DELETE CASCADE,
    practice_id BIGINT    REFERENCES code_practices(id) ON DELETE CASCADE,
    deadline    TIMESTAMP,
    created_by  BIGINT    REFERENCES users(id)         ON DELETE SET NULL,
    created_at  TIMESTAMP DEFAULT NOW(),
    CONSTRAINT one_type CHECK (
        (test_id IS NOT NULL AND practice_id IS NULL) OR
        (test_id IS NULL     AND practice_id IS NOT NULL)
    )
);

CREATE TABLE results (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT    NOT NULL REFERENCES users(id)          ON DELETE CASCADE,
    assignment_id  BIGINT    NOT NULL REFERENCES assignments(id)    ON DELETE CASCADE,
    test_id        BIGINT    REFERENCES tests(id)                   ON DELETE SET NULL,
    practice_id    BIGINT    REFERENCES code_practices(id)          ON DELETE SET NULL,
    attempt_number INTEGER   NOT NULL DEFAULT 1 CHECK (attempt_number > 0),
    points         INTEGER   NOT NULL DEFAULT 0,
    max_points     INTEGER   NOT NULL CHECK (max_points > 0),
    started_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    submitted_at   TIMESTAMP,
    expires_at     TIMESTAMP,
    CONSTRAINT one_type CHECK (
        (test_id IS NOT NULL AND practice_id IS NULL) OR
        (test_id IS NULL     AND practice_id IS NOT NULL)
    ),
    CONSTRAINT valid_points CHECK (points >= 0 AND points <= max_points),
    UNIQUE (user_id, assignment_id, attempt_number)
);

CREATE TABLE code_submissions (
    id           BIGSERIAL PRIMARY KEY,
    result_id    BIGINT  NOT NULL REFERENCES results(id) ON DELETE CASCADE,
    code         TEXT    NOT NULL,
    test_output  TEXT,
    passed_tests INTEGER NOT NULL DEFAULT 0 CHECK (passed_tests >= 0),
    total_tests  INTEGER NOT NULL DEFAULT 0 CHECK (total_tests >= 0),
    submitted_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT valid_test_counts CHECK (passed_tests <= total_tests)
);

CREATE INDEX idx_results_user        ON results(user_id);
CREATE INDEX idx_results_assignment  ON results(assignment_id);
CREATE INDEX idx_assignments_group   ON assignments(group_id);
CREATE INDEX idx_user_groups_group   ON user_groups(group_id);
CREATE INDEX idx_submissions_result  ON code_submissions(result_id);
