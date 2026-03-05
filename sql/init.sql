СREATE DATABASE board;
\c board;

DO $$ BEGIN
    CREATE TYPE role_type AS ENUM ('ADMIN', 'RECRUITER');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

CREATE TABLE IF NOT EXISTS users (
    email          TEXT PRIMARY KEY,
    hashedPassword TEXT NOT NULL,
    firstName      TEXT,
    lastName       TEXT,
    company        TEXT,
    role           role_type NOT NULL
);

CREATE TABLE IF NOT EXISTS jobs (
    id            UUID DEFAULT gen_random_uuid(),
    date          BIGINT NOT NULL,
    ownerEmail    TEXT NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT false,
    company       TEXT NOT NULL,
    title         TEXT NOT NULL,
    description   TEXT NOT NULL,
    externalUrl   TEXT NOT NULL,
    remote        BOOLEAN NOT NULL DEFAULT false,
    location      TEXT,
    salaryLo      INTEGER,
    salaryHi      INTEGER,
    currency      TEXT,
    country       TEXT,
    tags          TEXT[],
    image         TEXT,
    seniority     TEXT,
    other         TEXT,
    CONSTRAINT pk_jobs PRIMARY KEY (id)
);
