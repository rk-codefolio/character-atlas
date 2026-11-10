CREATE TABLE catalog_snapshot (
    singleton boolean PRIMARY KEY CHECK (singleton),
    revision text NOT NULL
);

CREATE TABLE catalog_character (
    character_id text PRIMARY KEY,
    display_name text NOT NULL,
    attribute text,
    rarity text
);

CREATE TABLE catalog_stat (
    character_id text NOT NULL REFERENCES catalog_character ON DELETE CASCADE,
    stat text NOT NULL,
    value numeric NOT NULL,
    PRIMARY KEY (character_id, stat)
);

CREATE TABLE catalog_skill (
    character_id text NOT NULL REFERENCES catalog_character ON DELETE CASCADE,
    skill_id text NOT NULL,
    position integer NOT NULL CHECK (position >= 0),
    display_name text NOT NULL,
    numeric_level integer NOT NULL CHECK (numeric_level > 0),
    description text,
    text_origin text NOT NULL CHECK (text_origin IN ('PACKAGED_JA', 'OTHER_PACKAGED_LANGUAGE', 'MISSING')),
    PRIMARY KEY (character_id, skill_id),
    UNIQUE (character_id, position)
);

CREATE TABLE catalog_term (
    term_id text PRIMARY KEY,
    label text NOT NULL,
    kind text NOT NULL CHECK (kind IN ('EFFECT', 'TAG'))
);

CREATE TABLE catalog_skill_match (
    character_id text NOT NULL,
    skill_id text NOT NULL,
    term_id text NOT NULL REFERENCES catalog_term,
    evidence text NOT NULL CHECK (evidence IN ('DECLARED_EFFECT', 'DECLARED_TAG', 'RELATED_CANDIDATE')),
    PRIMARY KEY (character_id, skill_id, term_id),
    FOREIGN KEY (character_id, skill_id) REFERENCES catalog_skill ON DELETE CASCADE
);
