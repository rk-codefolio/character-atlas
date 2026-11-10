package dev.characteratlas.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CharacterProfile(String id, String name, String attribute, String rarity, Integer archivePriority,
                               Map<Stat, BigDecimal> stats, Map<Stat, BigDecimal> maxAwakenedStats, List<Skill> skills) {
    public CharacterProfile {
        stats = Map.copyOf(stats);
        maxAwakenedStats = Map.copyOf(maxAwakenedStats);
        skills = List.copyOf(skills);
    }

    public CharacterProfile(String id, String name, String attribute, String rarity, Integer archivePriority,
                            Map<Stat, BigDecimal> stats, List<Skill> skills) {
        this(id, name, attribute, rarity, archivePriority, stats, stats, skills);
    }

    public Map<Stat, BigDecimal> stats(boolean awakened) {
        return awakened ? maxAwakenedStats : stats;
    }

    public enum Stat { SPEED, HP, ATTACK, DEFENCE, CRITICAL_RATE, CRITICAL_DAMAGE, EFFECT_HIT, EFFECT_RESIST }

    public record Skill(String id, String name, int level, String description,
                        TextOrigin textOrigin, List<Match> matches) {
        public Skill {
            matches = List.copyOf(matches);
        }
    }

    public enum TextOrigin { PACKAGED_JA, OTHER_PACKAGED_LANGUAGE, MISSING }

    public record Match(String termId, Evidence evidence) {}

    public enum Evidence {
        DECLARED_EFFECT,
        DECLARED_TAG,
        RELATED_CANDIDATE;

        public boolean isCandidate() {
            return this == RELATED_CANDIDATE;
        }
    }
}
