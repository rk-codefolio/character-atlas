package dev.characteratlas.archipelago.moderation.presentation.report;

/** 通報の提出内容を表す。 */
public record ReportSubmission(/* 通報理由。 */ ReportReason reason, /* 通報の補足。 */ String note) {}
