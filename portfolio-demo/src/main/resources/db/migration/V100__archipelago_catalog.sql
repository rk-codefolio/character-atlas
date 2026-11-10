INSERT INTO catalog_snapshot (singleton, revision) VALUES (true, 'archipelago-v1');

INSERT INTO catalog_character (character_id, display_name, attribute, rarity, archive_priority) VALUES
    ('marea', 'マレア', '潮', '導き手', 10),
    ('sora', 'ソラノ', '風', '渡り手', 20),
    ('towa', 'トワ', '灯', '導き手', 30),
    ('nagi', 'ナギ', '風', '渡り手', 40),
    ('sango', 'サンゴ', '潮', '渡り手', 50),
    ('yura', 'ユラ', '灯', '渡り手', 60);

INSERT INTO catalog_stat (character_id, stat, value) VALUES
    ('marea', 'SPEED', 106), ('marea', 'HP', 1540), ('marea', 'ATTACK', 165), ('marea', 'DEFENCE', 210),
    ('sora',  'SPEED', 132), ('sora',  'HP', 1080), ('sora',  'ATTACK', 232), ('sora',  'DEFENCE', 128),
    ('towa',  'SPEED', 111), ('towa',  'HP', 1220), ('towa',  'ATTACK', 174), ('towa',  'DEFENCE', 149),
    ('nagi',  'SPEED', 125), ('nagi',  'HP', 1150), ('nagi',  'ATTACK', 204), ('nagi',  'DEFENCE', 135),
    ('sango', 'SPEED',  98), ('sango', 'HP', 1690), ('sango', 'ATTACK', 157), ('sango', 'DEFENCE', 238),
    ('yura',  'SPEED', 116), ('yura',  'HP', 1360), ('yura',  'ATTACK', 188), ('yura',  'DEFENCE', 164);

INSERT INTO catalog_term (term_id, label, kind) VALUES
    ('guard', '護り', 'EFFECT'),
    ('restore', '回復', 'EFFECT'),
    ('swift', '追い風', 'EFFECT'),
    ('tether', '足止め', 'TAG');

INSERT INTO catalog_skill (character_id, skill_id, position, display_name, numeric_level, description, text_origin) VALUES
    ('marea', 'reef', 0, '珊瑚の壁', 3, '味方全体に潮の護りを張る。', 'PACKAGED_JA'),
    ('marea', 'spring', 1, '沖の湧き水', 3, '最も消耗した味方を回復する。', 'PACKAGED_JA'),
    ('sora', 'sail', 0, '帆を張る', 3, '味方全体に追い風を与える。', 'PACKAGED_JA'),
    ('sora', 'knot', 1, '帆索投げ', 3, '敵単体を足止めする。', 'PACKAGED_JA'),
    ('towa', 'lamp', 0, '夜渡りの灯', 3, '味方単体を回復する。灯が消えるまで外套を温める。', 'PACKAGED_JA'),
    ('nagi', 'eddy', 0, '島影の風', 3, '味方単体に追い風を与える。', 'PACKAGED_JA'),
    ('nagi', 'mooring', 1, '仮泊の錨', 3, '敵単体を足止めする。', 'PACKAGED_JA'),
    ('sango', 'breakwater', 0, '防波礁', 3, '味方全体に護りを付与する。', 'PACKAGED_JA'),
    ('yura', 'lantern', 0, '浜辺の灯籠', 3, '味方全体を回復する。', 'PACKAGED_JA'),
    ('yura', 'candle', 1, '導き火', 3, '味方単体に護りを付与する。', 'PACKAGED_JA');

INSERT INTO catalog_skill_match (character_id, skill_id, term_id, evidence) VALUES
    ('marea', 'reef', 'guard', 'DECLARED_EFFECT'),
    ('marea', 'spring', 'restore', 'DECLARED_EFFECT'),
    ('sora', 'sail', 'swift', 'DECLARED_EFFECT'),
    ('sora', 'knot', 'tether', 'DECLARED_TAG'),
    ('towa', 'lamp', 'restore', 'DECLARED_EFFECT'),
    ('towa', 'lamp', 'guard', 'RELATED_CANDIDATE'),
    ('nagi', 'eddy', 'swift', 'DECLARED_EFFECT'),
    ('nagi', 'mooring', 'tether', 'DECLARED_TAG'),
    ('sango', 'breakwater', 'guard', 'DECLARED_EFFECT'),
    ('yura', 'lantern', 'restore', 'DECLARED_EFFECT'),
    ('yura', 'candle', 'guard', 'DECLARED_EFFECT');
