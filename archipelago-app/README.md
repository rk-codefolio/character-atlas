# 風綴りの群島

架空の群島を旅する107人の渡り手と45件のコンテンツを収録した図鑑です。データ、肖像、戦闘の説明、投稿例はこの架空世界のために用意しました。Next.js、Spring BootのREST API、PostgreSQL、共通のJava検索基盤をDockerで動かせます。

## 使用バージョン

- Java 25 LTS、Gradle 9.9.0、JUnit 6.1.3
- Spring Boot 4.1.2、MyBatis Spring Boot Starter 4.0.0、Flyway 13.12.1、PostgreSQL 18.6、pgJDBC 42.7.14
- Node.js 26 LTS、Next.js 16.4.6、React 19.3.1

## Dockerで起動する

Docker DesktopなどでDocker EngineとCompose v2を起動し、リポジトリのルートで次を実行します。

```sh
cd archipelago-app
cp .env.example .env
```

`.env` の `ARCHIPELAGO_DB_PASSWORD` にローカル専用の任意のパスワードを設定します。ポート3000が使用中なら `ARCHIPELAGO_WEB_PORT` を変更します。

```sh
docker compose up --build --wait
```

[渡り手図鑑を開く](http://localhost:3000)。ポートを変更した場合はURLの末尾を同じ値にします。Flywayがスキーマと架空データを登録します。Webポートはローカルに公開され、WebからREST APIとPostgreSQLへはCompose内で接続します。

### 試し方

1. **渡り手一覧**で「速度」「降順」を選びます。覚醒後の速度が140.5のソラノが先頭です。
2. **詳細検索**の「＋ 条件を追加」で「護り」「回復」を選びます。ANDは3人で、マレア・ユラの後に「関連候補」のトワが並びます。ORは4人です。「クリア」でANDと全員表示に戻ります。役割や複数の関連コンテンツも指定できます。
3. **ソラノの詳細**を開き、覚醒前132と最大覚醒後140.5を切り替えます。コメント、ビルド、スキルのタブがあり、スキルのCDや段階別の効果を確認できます。
4. **コンテンツ一覧**で常設・期間限定を確認します。北灯台の巡視には攻略投稿があり、関連する渡り手を複数選んで投稿できます。投稿カードから通報できます。
5. **ゲーム仕様**で、航行ゲージ、属性相性、効果的中・抵抗、装具など20の説明ページを開けます。

停止は `docker compose down` で行います。DBのボリュームは次回も使います。旧PostgreSQL 16の構成から切り替える場合、18では別ボリュームを使います。起動に失敗した場合は `docker compose ps` と `docker compose logs api web` で確認できます。

## Discord認証と通報通知

管理画面は `/?admin=1` から開きます。DiscordのOAuth2アプリに `http://localhost:3000/api/admin/discord/callback` を登録し、同じWebオリジンでアクセスします。ポートやHTTPSを変更する場合は、コールバックURLと `CATALOG_FRONTEND_ORIGIN` を合わせます。HTTPSを使う場合は `CATALOG_SECURE_COOKIE=true` を設定します。

`.env` にアプリの `CATALOG_DISCORD_CLIENT_ID` と `CATALOG_DISCORD_CLIENT_SECRET`、オーナーのユーザーID `CATALOG_DISCORD_OWNER_ID`、モデレーターのユーザーIDをカンマで区切った `CATALOG_DISCORD_MODERATOR_IDS` を設定します。設定済みのモデレーターは投稿編集・削除と通報対応、オーナーは加えて操作ログ閲覧と検索用コンテンツ候補の変更ができます。

Discordへの通報通知を使う場合は `CATALOG_DISCORD_REPORT_WEBHOOK` を設定します。通報はDBに記録され、設定されたオーナーとモデレーターが通知のメンション対象です。秘密情報はローカルの `.env` に保存します。閲覧、投稿、通報の保存はDiscordを設定せずに試せます。

## 統合テスト

Java 25で、空の検証用PostgreSQLに対し `ATLAS_TEST_DB_URL` と `ARCHIPELAGO_DB_PASSWORD` を設定し、このディレクトリで `../catalog-core/gradlew test` を実行します。テストはユーザー `archipelago` を使って専用のスキーマを作ります。検索と覚醒差分、投稿の入力検証、通報、モデレーターとオーナーの権限差を検証します。[統合テスト](src/test/java/dev/characteratlas/archipelago/ArchipelagoIntegrationTest.java)

[構成図とREST API一覧](../docs/ARCHITECTURE.md)
