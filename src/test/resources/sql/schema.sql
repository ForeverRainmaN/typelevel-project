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
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date             BIGINT,
    ownerEmail       TEXT,
    active           BOOLEAN,
    company          TEXT,
    title            TEXT,
    description      TEXT,
    externalUrl      TEXT,
    remote           BOOLEAN,
    location         TEXT,
    salaryLo         INTEGER,
    salaryHi         INTEGER,
    currency         TEXT,
    country          TEXT,
    tags             TEXT[],
    image            TEXT,
    seniority        TEXT,
    other            TEXT
);