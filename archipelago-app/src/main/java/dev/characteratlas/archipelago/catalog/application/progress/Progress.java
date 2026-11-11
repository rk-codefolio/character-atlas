package dev.characteratlas.archipelago.catalog.application.progress;

import java.util.List;
import java.util.Map;

/** 育成情報を表す。 */
public record Progress(
    /* awakening。 */ boolean awakening, /* attribute。 */
    String attribute, /* skills。 */
    Map<String, List<SkillLevel>> skills, /* スキルの属性情報。 */
    Map<String, SkillInfo> skillInfo) {}
