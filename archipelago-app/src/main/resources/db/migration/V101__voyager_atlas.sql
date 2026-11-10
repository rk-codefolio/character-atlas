CREATE TABLE voyager_role (
    character_id text NOT NULL REFERENCES catalog_character ON DELETE CASCADE,
    role text NOT NULL,
    PRIMARY KEY (character_id, role)
);
CREATE TABLE voyager_growth (
    character_id text PRIMARY KEY REFERENCES catalog_character ON DELETE CASCADE,
    awakened boolean NOT NULL,
    advanced boolean NOT NULL
);
CREATE TABLE voyager_skill_meta (
    character_id text NOT NULL,
    skill_id text NOT NULL,
    passive boolean NOT NULL,
    cooldown integer CHECK (cooldown >= 0),
    PRIMARY KEY (character_id, skill_id),
    FOREIGN KEY (character_id, skill_id) REFERENCES catalog_skill ON DELETE CASCADE
);
CREATE TABLE voyager_skill_level (
    character_id text NOT NULL,
    skill_id text NOT NULL,
    level integer NOT NULL CHECK (level > 0),
    description text NOT NULL,
    PRIMARY KEY (character_id, skill_id, level),
    FOREIGN KEY (character_id, skill_id) REFERENCES catalog_skill ON DELETE CASCADE
);
CREATE TABLE voyager_effect_tip (
    term_id text NOT NULL REFERENCES catalog_term ON DELETE CASCADE,
    level integer NOT NULL CHECK (level > 0),
    label text NOT NULL,
    kind text NOT NULL CHECK (kind IN ('Buff', 'Debuff', 'Effect')),
    description text NOT NULL,
    PRIMARY KEY (term_id, level)
);

UPDATE catalog_snapshot SET revision = 'archipelago-v2' WHERE singleton;

-- The names, affiliations, ranks, and values are authored for this fictional setting.
WITH islands AS (
    SELECT island, position
    FROM unnest(ARRAY['月綱','玻璃岬','北灯','霧鐘','硝子礁','潮棚','白帆','雲波','銀砂','雨影','風花'])
        WITH ORDINALITY AS ports(island, position)
), given_names AS (
    SELECT given_name, position
    FROM unnest(ARRAY['イオ','セナ','ルカ','ミナ','ハル','カヤ','リオ','ノエ','ユノ','レナ'])
        WITH ORDINALITY AS crew(given_name, position)
), voyagers AS (
    SELECT (islands.position - 1) * 10 + given_names.position AS n, given_name || '・' || island AS name
    FROM islands CROSS JOIN given_names
)
INSERT INTO catalog_character (character_id, display_name, attribute, rarity, archive_priority)
SELECT 'voyager-' || lpad(n::text, 3, '0'), name, (ARRAY['風','潮','灯','岩','星'])[1 + n % 5],
       CASE WHEN n % 6 = 0 THEN '導き手' ELSE '渡り手' END, 60 + n
FROM voyagers WHERE n <= 101;

INSERT INTO catalog_stat (character_id, stat, value)
SELECT character_id, stat, value
FROM catalog_character
CROSS JOIN LATERAL (VALUES
    ('HP', (1000 + archive_priority * 37 % 760)::numeric),
    ('ATTACK', (140 + archive_priority * 11 % 85)::numeric),
    ('DEFENCE', (125 + archive_priority * 13 % 110)::numeric),
    ('SPEED', (90 + archive_priority * 7 % 42)::numeric),
    ('CRITICAL_RATE', 0.08 + (archive_priority % 15) * 0.01),
    ('CRITICAL_DAMAGE', 1.35 + (archive_priority % 16) * 0.01),
    ('EFFECT_HIT', 0.10 + (archive_priority % 21) * 0.01),
    ('EFFECT_RESIST', 0.12 + (archive_priority % 19) * 0.01)
) AS values_by_stat(stat, value)
WHERE character_id LIKE 'voyager-%' OR stat NOT IN ('HP', 'ATTACK', 'DEFENCE', 'SPEED');

INSERT INTO voyager_role (character_id, role)
SELECT character_id, (ARRAY['攻撃','防御・保護','サポート','妨害'])[1 + archive_priority % 4] FROM catalog_character
UNION
SELECT character_id, 'サポート' FROM catalog_character WHERE character_id IN ('marea','towa','yura') OR archive_priority % 7 = 0;

INSERT INTO voyager_growth
SELECT character_id, character_id IN ('sora','towa') OR archive_priority >= 61 AND archive_priority % 13 = 0,
       character_id = 'sora' OR archive_priority >= 61 AND archive_priority % 9 = 0
FROM catalog_character;

INSERT INTO catalog_awakened_stat (character_id, stat, value)
SELECT stat.character_id, stat.stat, stat.value + CASE stat.stat
    WHEN 'HP' THEN 135 WHEN 'SPEED' THEN 8.5 WHEN 'ATTACK' THEN 16 WHEN 'DEFENCE' THEN 12
    ELSE 0.02 END
FROM catalog_stat stat JOIN voyager_growth growth USING (character_id)
WHERE growth.awakened
ON CONFLICT (character_id, stat) DO UPDATE SET value = EXCLUDED.value;

INSERT INTO catalog_term (term_id, label, kind) VALUES
    ('gale-step', '風渡り', 'EFFECT'), ('tide-screen', '潮幕', 'EFFECT'),
    ('beacon-light', '灯継ぎ', 'EFFECT'), ('rock-cover', '岩庇', 'EFFECT'),
    ('star-trail', '星標', 'EFFECT'), ('white-wake', '白波', 'EFFECT'),
    ('steady-hand', '舵取り', 'EFFECT'), ('night-watch', '夜警', 'EFFECT'),
    ('salt-mist', '塩霧', 'EFFECT'), ('slow-drift', '漂い', 'EFFECT'),
    ('reef-lock', '岩留め', 'EFFECT'), ('lost-bearing', '方位喪失', 'EFFECT'),
    ('fading-light', '灯消え', 'EFFECT'), ('cross-wave', '横波', 'EFFECT'),
    ('rough-sail', '帆擦れ', 'EFFECT'), ('fog-mark', '霧印', 'EFFECT'),
    ('moon-anchor', '月錨', 'EFFECT'), ('haven-song', '港歌', 'EFFECT'),
    ('cd-hasten', 'CD短縮', 'TAG'), ('cd-delay', 'CD増加', 'TAG');

INSERT INTO voyager_effect_tip (term_id, level, label, kind, description)
SELECT t.term_id, 1, t.label, tips.kind, tips.description
FROM (VALUES
    ('guard', 'Buff', '次に受ける直接ダメージを、渡り手の最大HPの12%まで吸収する。'),
    ('restore', 'Effect', '対象のHPを回復する。回復量は使った技の説明を参照。'),
    ('swift', 'Buff', '次の通常ターンまで速度を10%上げる。'),
    ('gale-step', 'Buff', '2ターンの間、速度を8%上げる。'),
    ('tide-screen', 'Buff', '次の1回、受ける直接ダメージを10%軽減する。'),
    ('beacon-light', 'Buff', '次の通常ターン開始時、最大HPの7%を回復する。'),
    ('rock-cover', 'Buff', '1ターンの間、防御力を12%上げる。'),
    ('star-trail', 'Buff', '次の攻撃で会心率を8ポイント上げる。'),
    ('white-wake', 'Buff', '次の航行ゲージ更新で進捗を5ポイント追加する。'),
    ('steady-hand', 'Buff', '次の付与判定で効果的中を10ポイント上げる。'),
    ('night-watch', 'Buff', '次の付与判定で効果抵抗を10ポイント上げる。'),
    ('salt-mist', 'Debuff', '次のターン開始時、最大HPの3%の間接ダメージを受ける。'),
    ('slow-drift', 'Debuff', '1ターンの間、速度を8%下げる。'),
    ('reef-lock', 'Debuff', '次の通常ターンに追加行動を得られない。'),
    ('lost-bearing', 'Debuff', '次の攻撃の会心率を10ポイント下げる。'),
    ('fading-light', 'Debuff', '次の回復量を20%下げる。'),
    ('cross-wave', 'Debuff', '次の航行ゲージ更新で進捗を5ポイント失う。'),
    ('rough-sail', 'Debuff', '次の攻撃の威力を10%下げる。'),
    ('fog-mark', 'Debuff', '次の付与判定で効果的中を10ポイント下げる。'),
    ('moon-anchor', 'Effect', '次に行動するまで航行ゲージの減少を一度防ぐ。通常解除の対象外。'),
    ('haven-song', 'Effect', '次のターン開始時、味方全員に航行ゲージを3ポイント与える。')
) AS tips(term_id, kind, description)
JOIN catalog_term t USING (term_id);

WITH templates(position, name, category) AS (
    VALUES (0, '航海の手ほどき', '帆技'), (1, '潮目の合図', '合図'), (2, '島守の心得', '心得')
), techniques AS (
    SELECT c.character_id, c.attribute, c.archive_priority, t.position, t.name, t.category,
           CASE t.position
             WHEN 0 THEN (ARRAY['gale-step','tide-screen','beacon-light','rock-cover','star-trail','white-wake','steady-hand','night-watch'])[1 + c.archive_priority % 8]
             WHEN 1 THEN (ARRAY['salt-mist','slow-drift','reef-lock','lost-bearing','fading-light','cross-wave','rough-sail','fog-mark'])[1 + c.archive_priority % 8]
             ELSE (ARRAY['moon-anchor','haven-song','beacon-light','rock-cover'])[1 + c.archive_priority % 4]
           END AS term_id
    FROM catalog_character c CROSS JOIN templates t WHERE c.character_id LIKE 'voyager-%'
), inserted AS (
    INSERT INTO catalog_skill (character_id, skill_id, position, display_name, numeric_level, description, text_origin)
    SELECT x.character_id, 'voyage-' || x.position, x.position, x.attribute || 'の' || x.name, 3,
           CASE x.position WHEN 0 THEN '味方一人に' WHEN 1 THEN '敵一人に' ELSE '自分に' END || term.label || 'を付与する。',
           'PACKAGED_JA'
    FROM techniques x JOIN catalog_term term USING (term_id)
    RETURNING character_id, skill_id, position
)
INSERT INTO catalog_skill_match (character_id, skill_id, term_id, evidence)
SELECT i.character_id, i.skill_id, x.term_id, 'DECLARED_EFFECT'
FROM inserted i JOIN techniques x USING (character_id, position)
UNION ALL
SELECT i.character_id, i.skill_id, CASE WHEN x.archive_priority % 2 = 0 THEN 'cd-hasten' ELSE 'cd-delay' END, 'DECLARED_TAG'
FROM inserted i JOIN techniques x USING (character_id, position)
WHERE i.position = 1 AND x.archive_priority % 3 = 0;

UPDATE catalog_skill AS skill
SET description = skill.description || CASE match.term_id
    WHEN 'cd-hasten' THEN '味方一人の待ち時間を1ターン短くする。'
    ELSE '対象の待ち時間を1ターン延ばす。' END
FROM catalog_skill_match match
WHERE skill.character_id = match.character_id AND skill.skill_id = match.skill_id
  AND match.term_id IN ('cd-hasten', 'cd-delay');

INSERT INTO voyager_skill_meta (character_id, skill_id, passive, cooldown)
SELECT character_id, skill_id, position = 2, CASE position WHEN 0 THEN 0 WHEN 1 THEN 3 ELSE NULL END
FROM catalog_skill;

INSERT INTO voyager_skill_level (character_id, skill_id, level, description)
SELECT character_id, skill_id, stage.level,
       CASE stage.level WHEN 1 THEN '航海開始後、一度だけ発動する。' || description
                        WHEN 2 THEN '最初の二巡で発動する。' || description
                        ELSE description END
FROM catalog_skill CROSS JOIN generate_series(1, 3) AS stage(level);

INSERT INTO review_post (post_id, kind, subject_id, title, body, details) VALUES
    ('a71a5000-0000-4000-8000-000000000001', 'COMMENT', 'marea', '北灯台の巡視で活躍',
     '潮幕を張る役と組ませると、二巡目に回復が間に合いました。', '{"contentTags":["route-01"]}'),
    ('a71a5000-0000-4000-8000-000000000002', 'BUILD', 'sora', '先に灯台へ着くための配分',
     '味方の進捗を上げるため、速度を優先します。',
     '{"shell":"潮風の羅針盤","matrices":[{"name":"追風","points":6},{"name":"灯継","points":4}],"mainStats":["HP%","攻撃力%","効果命中"],"contentTags":["route-01","route-03"]}'),
    ('a71a5000-0000-4000-8000-000000000003', 'CONTENT', 'route-01', '北灯台での交代順',
     '最初にトワで回復し、ソラノの追い風で次の巡回へつなぎます。装具はHPと効果命中を優先します。',
     '{"relatedCharacters":["towa","sora"]}');

-- Regional effects combine an original location name with a common effect family.
WITH region AS (
    SELECT ((place.position - 1) * 10 + coast.position - 1) AS number, place.name || coast.name AS name
    FROM unnest(ARRAY['朝','黄昏','星明','白夜','霧深','冬凪','遠雷'])
        WITH ORDINALITY AS place(name, position)
    CROSS JOIN unnest(ARRAY['岬','島','港','湾','礁','浜','洞','瀬','沖','棚'])
        WITH ORDINALITY AS coast(name, position)
    WHERE (place.position - 1) * 10 + coast.position <= 61
), family AS (
    SELECT * FROM (VALUES
        (1, 'の風衣', 'Buff', '次の通常ターンまで速度を%s%%上げる。'),
        (2, 'の波盾', 'Buff', '次に受ける直接ダメージを%s%%軽減する。'),
        (3, 'の灯守', 'Buff', '次のターン開始時に最大HPの%s%%を回復する。'),
        (4, 'の塩傷', 'Debuff', '次のターン開始時に最大HPの%s%%を失う。'),
        (5, 'の逆流', 'Debuff', '次の航行ゲージ更新時に進捗を%sポイント失う。'),
        (6, 'の目眩', 'Debuff', '次の攻撃の会心率を%sポイント下げる。'),
        (7, 'の航海印', 'Effect', '次の通常ターン開始時に航行ゲージを%s進める。')
    ) AS definitions(position, suffix, kind, description)
), regional_effect AS (
    SELECT 'atlas-effect-' || lpad((region.number * 7 + family.position)::text, 3, '0') AS id,
           region.name || family.suffix AS label, family.kind, family.description,
           (region.number + family.position) % 7 + 4 AS strength
    FROM region CROSS JOIN family
), inserted AS (
    INSERT INTO catalog_term (term_id, label, kind)
    SELECT id, label, 'EFFECT' FROM regional_effect
    RETURNING term_id
)
INSERT INTO voyager_effect_tip (term_id, level, label, kind, description)
SELECT effect.id, stage.level, effect.label, effect.kind, format(effect.description, effect.strength + stage.level - 1)
FROM regional_effect effect JOIN inserted ON inserted.term_id = effect.id
CROSS JOIN generate_series(1, 3) AS stage(level);

INSERT INTO catalog_skill_match (character_id, skill_id, term_id, evidence)
SELECT character_id, 'voyage-0', 'atlas-effect-' || lpad((1 + archive_priority % 18)::text, 3, '0'), 'DECLARED_EFFECT'
FROM catalog_character WHERE character_id LIKE 'voyager-%'
UNION ALL
SELECT character_id, 'voyage-2', 'atlas-effect-' || lpad((19 + archive_priority - 61)::text, 3, '0'), 'DECLARED_EFFECT'
FROM catalog_character WHERE character_id LIKE 'voyager-%';

WITH extra AS (
    SELECT match.character_id, match.skill_id, term.label
    FROM catalog_skill_match match JOIN catalog_term term USING (term_id)
    WHERE term.term_id LIKE 'atlas-effect-%'
)
UPDATE catalog_skill skill SET description = skill.description || 'さらに' || extra.label || 'を発動する。'
FROM extra WHERE skill.character_id = extra.character_id AND skill.skill_id = extra.skill_id;

UPDATE voyager_skill_level level SET description = skill.description
FROM catalog_skill skill
WHERE level.character_id = skill.character_id AND level.skill_id = skill.skill_id AND level.level = skill.numeric_level;
