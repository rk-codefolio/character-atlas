# 風綴りの群島

架空のゲーム「風綴りの群島」の渡り手を、名前や技の効果から探せる図鑑です。Next.js の画面から Spring Boot の API を呼び、PostgreSQL のデータを共通の Java 検索ユースケースで処理します。

## 使用バージョン

- Java 25 LTS、Gradle 9.9.0、JUnit 6.1.3
- Spring Boot 4.1.2、Flyway 13.12.1、PostgreSQL 18.6、pgJDBC 42.7.14
- Node.js 26 LTS、Next.js 16.4.6、React 19.3.1

## Dockerで起動する

Docker Desktopなどで Docker Engine と Compose v2 を起動してください。リポジトリのルートから次のコマンドを実行します。

```sh
cd portfolio-demo
cp .env.example .env
```

`.env` の `ARCHIPELAGO_DB_PASSWORD` にローカル専用の任意のパスワードを設定します。英数字だけで20文字以上にするとそのまま記入できます。ポート3000が使用中なら `ARCHIPELAGO_WEB_PORT` を変更します。

```sh
docker compose up --build --wait
```

[渡り手図鑑を開く](http://localhost:3000)。ポートを変更した場合は、URLの末尾も同じ値にします。初回起動時に Flyway がスキーマを作成し、架空の6人と技・検索条件をデータベースへ登録します。Webだけをローカルのブラウザに公開し、APIとデータベースはCompose内で接続します。

### 試し方

1. 渡り手一覧で「速度」「降順」を選びます。ソラノが先頭に表示されます。
2. 「技で探す」で「護り」と「回復」を選び、ANDで検索します。明示された2人の後に「関連候補」のトワが並びます。
3. ORに切り替えると、どちらか一方だけを持つ渡り手も表示されます。
4. 名前を開くと属性・ステータス・スキルと、スキルに紐づく効果を確認できます。

停止するには `docker compose down` を実行します。DBのボリュームは次回も使います。PostgreSQL 16の旧構成から切り替える場合、18では新しいボリュームを使い、架空データを初期登録します。旧ボリュームはそのまま残ります。起動に失敗した場合は `docker compose ps` と `docker compose logs api web` で状況を確認できます。

## ファイルの位置

- `src/main/resources/db/migration/V100__archipelago_catalog.sql` が架空データの初期登録です。
- `src/main/java` が検索APIとPostgreSQLからの読み取りを担当します。
- `web/app` が図鑑の画面です。属性の記号と配色は画面のスタイルで定義しています。
- [catalog-core](../catalog-core/README.md) にドメインモデルと検索ロジック、JUnitテストがあります。
