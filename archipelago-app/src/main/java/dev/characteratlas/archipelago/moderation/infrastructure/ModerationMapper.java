package dev.characteratlas.archipelago.moderation.infrastructure;

import dev.characteratlas.archipelago.moderation.application.AuditEntry;
import dev.characteratlas.archipelago.moderation.application.PostEntry;
import dev.characteratlas.archipelago.moderation.application.Report;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** モデレーションのSQL Mapperを表す。 */
@Mapper
interface ModerationMapper {
  /** 追加を扱う。 */
  @Insert(
      """
      INSERT INTO moderation_log (actor_id, action, target_id, before_value, after_value)
      VALUES (#{actor}, #{action}, #{target}, CAST(#{before} AS jsonb), CAST(#{after,jdbcType=VARCHAR} AS jsonb))
      """)
  void append(
      @Param("actor") String actor,
      @Param("action") String action,
      @Param("target") String target,
      @Param("before") String before,
      @Param("after") String after);

  /** entriesを扱う。 */
  @Select(
      """
      SELECT log_id, recorded_at, actor_id, action, target_id, before_value::text, after_value::text
      FROM moderation_log ORDER BY log_id DESC LIMIT 50 OFFSET #{offset}
      """)
  List<AuditEntry> entries(@Param("offset") int offset);

  /** postsを扱う。 */
  List<PostEntry> posts(@Param("offset") int offset);

  /** 追加するの通報を扱う。 */
  @Insert(
      """
      INSERT INTO post_report (report_id, post_id, reason, note, post_snapshot)
      SELECT #{report}, p.post_id, #{reason}, #{note}, jsonb_build_object('kind', p.kind, 'subject', p.subject_id,
          'title', p.title, 'body', p.body, 'details', p.details)
      FROM review_post p WHERE p.post_id = #{post}
      ON CONFLICT (post_id) WHERE resolved_at IS NULL DO NOTHING
      """)
  int addReport(
      @Param("report") UUID report,
      @Param("post") UUID post,
      @Param("reason") String reason,
      @Param("note") String note);

  /** 投稿の存在するか確認するを扱う。 */
  boolean postExists(@Param("id") UUID id);

  /** reportsを扱う。 */
  @Select(
      """
      SELECT report_id, post_id, reason, note, post_snapshot::text, created_at, resolved_at, notified_at
      FROM post_report WHERE CAST(#{id,jdbcType=OTHER} AS uuid) IS NULL OR report_id = #{id,jdbcType=OTHER}
      ORDER BY (resolved_at IS NOT NULL), created_at DESC, report_id LIMIT 50 OFFSET #{offset}
      """)
  List<Report> reports(@Param("id") UUID id, @Param("offset") int offset);

  /** 通報を対応済みにし、監査記録を残す。 */
  @Update(
      "UPDATE post_report SET resolved_at = now() WHERE report_id = #{id} AND resolved_at IS NULL")
  int resolve(@Param("id") UUID id);

  /** nextを扱う。 */
  @Select(
      """
      SELECT report_id, attempts FROM post_report
      WHERE notified_at IS NULL AND resolved_at IS NULL AND next_attempt <= now()
      ORDER BY next_attempt LIMIT 1 FOR UPDATE SKIP LOCKED
      """)
  PendingNotification next();

  /** 送信済みにするを扱う。 */
  @Update("UPDATE post_report SET notified_at = now() WHERE report_id = #{id}")
  void delivered(@Param("id") UUID id);

  /** 再試行するを扱う。 */
  @Update(
      """
      UPDATE post_report SET attempts = attempts + 1, next_attempt = now() + #{delay} * interval '1 second'
      WHERE report_id = #{id}
      """)
  void retry(@Param("id") UUID id, @Param("delay") int delay);
}
