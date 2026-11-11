package dev.characteratlas.archipelago;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.characteratlas.archipelago.moderation.infrastructure.DiscordModerationAccess;
import dev.characteratlas.archipelago.moderation.infrastructure.DiscordReportNotifications;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "catalog.discord.client-id=1234", "catalog.discord.client-secret=test-only",
      "catalog.discord.owner-id=1111", "catalog.discord.moderator-ids=2222"
    })
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "ATLAS_TEST_DB_URL", matches = ".+")
class ArchipelagoIntegrationTest {
  private static final String ORIGIN = "http://localhost:3000";
  private static final String SCHEMA = "atlas_it_" + UUID.randomUUID().toString().replace("-", "");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> System.getenv("ATLAS_TEST_DB_URL"));
    registry.add("spring.datasource.hikari.schema", () -> SCHEMA);
    registry.add("spring.flyway.schemas", () -> SCHEMA);
    registry.add("spring.flyway.default-schema", () -> SCHEMA);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource dataSource;

  /** 架空データをDBに適用して一覧・詳細・検索を呼び、件数と覚醒差分、検索根拠を確認する。 */
  @Test
  void listsTheFullCatalogAndSeparatesGrowthAndEvidence() throws Exception {
    mvc.perform(get("/api/characters"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(107)))
        .andExpect(jsonPath("$[0].id").value("marea"));
    mvc.perform(get("/api/review-options"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contents", hasSize(45)))
        .andExpect(jsonPath("$.shells", hasSize(47)))
        .andExpect(jsonPath("$.matrices", hasSize(27)))
        .andExpect(jsonPath("$.advancedIds[0]").value("sora"));
    assertEquals(
        448,
        jdbc.queryForObject(
            "SELECT count(DISTINCT term_id) FROM voyager_effect_tip", Integer.class));
    mvc.perform(get("/api/characters/sora"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stats.SPEED").value(132))
        .andExpect(jsonPath("$.maxAwakenedStats.SPEED").value(140.5));
    mvc.perform(get("/api/characters/sora/progress"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.awakening").value(true))
        .andExpect(jsonPath("$.skills.sail", hasSize(3)));
    mvc.perform(get("/api/characters/marea/progress"))
        .andExpect(jsonPath("$.awakening").value(false));
    mvc.perform(get("/api/characters/marea/effects"))
        .andExpect(jsonPath("$.reef[0].name").value("護り"));
    mvc.perform(get("/api/search").param("group", "護り", "回復").param("mode", "AND"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[2].candidateOnly").value(true));
    mvc.perform(get("/api/search").param("group", "護り", "回復").param("mode", "OR"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(4)));
    mvc.perform(get("/api/search").param("role", "サポート").param("content", "route-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].character.id").value("marea"));
  }

  /** V100のDBに投稿し最新まで更新して、投稿の保持と新規テーブルの利用を確認する。 */
  @Test
  void upgradesAnExistingCatalogAndKeepsItsPosts() {
    String schema = SCHEMA + "_upgrade";
    Flyway.configure()
        .dataSource(dataSource)
        .schemas(schema)
        .defaultSchema(schema)
        .target("100")
        .load()
        .migrate();
    assertEquals(
        6,
        jdbc.queryForObject(
            "SELECT count(*) FROM " + schema + ".catalog_character", Integer.class));
    UUID originalPost = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO "
            + schema
            + ".review_post (post_id,kind,subject_id,title,body,details) "
            + "VALUES (?, 'COMMENT', 'marea', '既存投稿', '更新前の投稿', '{}'::jsonb)",
        originalPost);

    Flyway.configure()
        .dataSource(dataSource)
        .schemas(schema)
        .defaultSchema(schema)
        .load()
        .migrate();
    assertEquals(
        107,
        jdbc.queryForObject(
            "SELECT count(*) FROM " + schema + ".catalog_character", Integer.class));
    assertEquals(
        "既存投稿",
        jdbc.queryForObject(
            "SELECT title FROM " + schema + ".review_post WHERE post_id = ?",
            String.class,
            originalPost));
    assertEquals(
        0, jdbc.queryForObject("SELECT count(*) FROM " + schema + ".post_report", Integer.class));
    assertEquals(
        0,
        jdbc.queryForObject("SELECT count(*) FROM " + schema + ".moderation_log", Integer.class));
  }

  /** ビルド・攻略の入力をAPIに送り、不正な組み合わせの拒否と有効な投稿の取得を確認する。 */
  @Test
  void validatesBuildsAndContentPostsWithFictionalIds() throws Exception {
    String build =
        """
        {"kind":"BUILD","subject":"voyager-001","title":"帆の配分","body":"",
         "details":{"shell":"潮風の羅針盤","matrices":[{"name":"追風","points":6},{"name":"灯継","points":4}],
          "mainStats":["HP%","攻撃力%","効果命中"],"contentTags":["route-01","route-03"]}}
        """;
    mvc.perform(
            post("/api/posts")
                .header("Origin", ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(build.replace("灯継", "追風")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/posts")
                .header("Origin", ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(build))
        .andExpect(status().isCreated());
    mvc.perform(get("/api/posts").param("kind", "BUILD").param("subject", "voyager-001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("帆の配分"));
    mvc.perform(
            post("/api/posts")
                .header("Origin", ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"kind":"CONTENT","subject":"route-02","title":"測量","body":"2人で交代する。",
                     "details":{"relatedCharacters":["marea","voyager-001"]}}
                    """))
        .andExpect(status().isCreated());
    mvc.perform(get("/api/posts").param("kind", "CONTENT").param("subject", "route-02"))
        .andExpect(jsonPath("$[0].relatedCharacterNames", hasSize(2)));
  }

  /** 投稿と通報を用意し、ロールに応じた操作の許可と監査記録の保持を確認する。 */
  @Test
  void enforcesModerationRolesAndStoresReportsAndAuditEvents() throws Exception {
    String comment =
        """
        {"kind":"COMMENT","subject":"marea","body":"確認用のコメント","details":{}}
        """;
    mvc.perform(
            post("/api/posts")
                .header("Origin", "https://example.invalid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(comment))
        .andExpect(status().isForbidden());
    String id =
        mvc.perform(
                post("/api/posts")
                    .header("Origin", ORIGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(comment))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replace("\"", "");
    var report =
        post("/api/posts/{id}/reports", id)
            .header("Origin", ORIGIN)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"SPAM\",\"note\":\"確認\"}");
    mvc.perform(report).andExpect(status().isAccepted());
    mvc.perform(report).andExpect(status().isAccepted());
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT count(*) FROM post_report WHERE post_id = ?",
            Integer.class,
            UUID.fromString(id)));
    UUID reportId =
        jdbc.queryForObject(
            "SELECT report_id FROM post_report WHERE post_id = ?", UUID.class, UUID.fromString(id));
    mvc.perform(get("/api/admin/reports")).andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/reports").session(session("2222")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].note").value("確認"));
    mvc.perform(
            put("/api/admin/reports/{id}", reportId)
                .session(session("2222"))
                .header("Origin", ORIGIN))
        .andExpect(status().isNoContent());
    mvc.perform(
            put("/api/posts/{id}", id)
                .session(session("2222"))
                .header("Origin", ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(comment.replace("確認用", "編集済み")))
        .andExpect(status().isNoContent());
    mvc.perform(delete("/api/posts/{id}", id).session(session("2222")).header("Origin", ORIGIN))
        .andExpect(status().isNoContent());
    var setting =
        put("/api/admin/search-contents/route-45")
            .header("Origin", ORIGIN)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"visible\":true}");
    mvc.perform(setting.session(session("2222"))).andExpect(status().isForbidden());
    mvc.perform(setting.session(session("1111"))).andExpect(status().isNoContent());
    mvc.perform(get("/api/search-contents"))
        .andExpect(jsonPath("$[?(@.id == 'route-45')]", hasSize(1)));
    mvc.perform(get("/api/admin/logs").session(session("2222"))).andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/logs").session(session("1111")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(4)));
    mvc.perform(get("/api/admin/discord/start"))
        .andExpect(status().isSeeOther())
        .andExpect(header().string("Location", startsWith("https://discord.com/oauth2/authorize")));
    assertEquals(
        Map.of("parse", List.of(), "users", List.of("1111", "2222")),
        DiscordReportNotifications.payload(List.of("1111", "2222"), ORIGIN, reportId)
            .get("allowed_mentions"));
  }

  /** 任意欄に不正な型を渡し、投稿APIが400を返すことを確認する。 */
  @Test
  void rejectsDetailsThatCannotBeRendered() throws Exception {
    // 各入力のHTTP検証で発生する検査例外をそのままJUnitに渡すため、通常のforを使う。
    for (String details :
        List.of("{\"stage\":{}}", "{\"contentTags\":{}}", "{\"contentTags\":[{}]}")) {
      String guide =
          """
          {"kind":"CONTENT","subject":"route-01","title":"攻略","body":"確認用","details":%s}
          """
              .formatted(details);
      mvc.perform(
              post("/api/posts")
                  .header("Origin", ORIGIN)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(guide))
          .andExpect(status().isBadRequest());
    }
    String build =
        """
        {"kind":"BUILD","subject":"marea","title":"装備","body":"",
         "details":{"shell":"潮風の羅針盤","matrices":[{"name":"追風"},{"name":"灯継"}],
          "mainStats":["HP%","攻撃力%","効果命中"],"targetStats":{}}}
        """;
    mvc.perform(
            post("/api/posts")
                .header("Origin", ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(build))
        .andExpect(status().isBadRequest());
  }

  /** 通報後に元投稿の詳細を変更し、通報時点の詳細がDBに残ることを確認する。 */
  @Test
  void retainsPostDetailsInTheReportAfterTheOriginalChanges() throws Exception {
    String guide =
        """
        {"kind":"CONTENT","subject":"route-01","title":"攻略","body":"確認用",
         "details":{"stage":"通報時の難易度","relatedCharacters":["marea"]}}
        """;
    String id =
        mvc.perform(
                post("/api/posts")
                    .header("Origin", ORIGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(guide))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replace("\"", "");
    mvc.perform(
            post("/api/posts/{id}/reports", id)
                .header("Origin", ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"OTHER\"}"))
        .andExpect(status().isAccepted());
    UUID postId = UUID.fromString(id);
    jdbc.update("UPDATE review_post SET details = '{}'::jsonb WHERE post_id = ?", postId);
    assertEquals(
        "通報時の難易度",
        jdbc.queryForObject(
            """
            SELECT post_snapshot->'details'->>'stage' FROM post_report WHERE post_id = ?
            """,
            String.class,
            postId));
  }

  /** 通知文が上限を超える宛先を設定し、設定エラーになることを確認する。 */
  @Test
  void rejectsWebhookConfigurationsThatCannotSendTheNotification() {
    String moderators =
        LongStream.range(1_000_000_000_000_000_000L, 1_000_000_000_000_000_090L)
            .mapToObj(Long::toString)
            .collect(Collectors.joining(","));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DiscordReportNotifications.validateConfiguration(
                "https://discord.com/api/webhooks/1/test-only",
                ORIGIN,
                "1111",
                List.of(moderators.split(","))));
  }

  /** 対象は管理画面と検索の境界。セッションと不正な条件を送り、権限とHTTPステータスを確認する。 */
  @Test
  void exposesModerationSessionAndRejectsInvalidSearchConditions() throws Exception {
    mvc.perform(get("/api/admin/session").session(session("1111")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.owner").value(true));
    mvc.perform(get("/api/admin/posts").session(session("2222")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").isNotEmpty());
    mvc.perform(post("/api/admin/logout").session(session("2222")).header("Origin", ORIGIN))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/characters/unknown")).andExpect(status().isNotFound());
    mvc.perform(get("/api/terms").param("shared", "true")).andExpect(status().isOk());
    mvc.perform(get("/api/search").param("role", "unknown")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/search").param("content", "unknown")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/search").param("term", "unknown")).andExpect(status().isBadRequest());
  }

  private static MockHttpSession session(String id) {
    var session = new MockHttpSession();
    session.setAttribute(DiscordModerationAccess.USER_SESSION, id);
    return session;
  }
}
