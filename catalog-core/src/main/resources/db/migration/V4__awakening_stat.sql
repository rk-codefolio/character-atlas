CREATE TABLE catalog_awakened_stat (
    character_id text NOT NULL REFERENCES catalog_character ON DELETE CASCADE,
    stat text NOT NULL,
    value numeric NOT NULL,
    PRIMARY KEY (character_id, stat)
);

INSERT INTO catalog_awakened_stat (character_id, stat, value)
SELECT character_id, stat, value FROM catalog_stat;
