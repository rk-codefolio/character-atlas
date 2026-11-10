package dev.characteratlas.archipelago.catalog.application;

import dev.characteratlas.domain.CharacterProfile;
import java.util.List;
import java.util.Map;

public interface CatalogOptions {
    Options get();
    boolean isSearchable(String id);
    List<Content> contents();
    List<EffectTip> effects(String termId);
    Progress progress(CharacterProfile character);

    public record Content(String id, String name, String kind, String group, List<String> aliases) {}
    public record Matrix(String name, List<Integer> thresholds) {}
    public record EffectTip(String id, String name, int level, String description) {}
    public record SkillLevel(int level, String description) {}
    public record SkillInfo(Boolean passive, Integer cooldown) {}
    public record Progress(boolean awakening, String attribute, Map<String, List<SkillLevel>> skills, Map<String, SkillInfo> skillInfo) {}
    public record Options(List<Content> contents, List<String> shells, List<Matrix> matrices, List<String> mainStats,
                   List<String> template, Map<String, List<String>> roles, Map<String, String> effectKinds,
                   Map<String, String> attributes, List<String> advancedIds) {}
}
