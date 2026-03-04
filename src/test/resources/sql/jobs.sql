CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date BIGINT,
    ownerEmail TEXT,
    active BOOLEAN,
    company TEXT,
    title TEXT,
    description TEXT,
    externalUrl TEXT,
    remote BOOLEAN,
    location TEXT,
    salaryLo INTEGER,
    salaryHi INTEGER,
    currency TEXT,
    country TEXT,
    tags TEXT[],
    image TEXT,
    seniority TEXT,
    other TEXT
);
