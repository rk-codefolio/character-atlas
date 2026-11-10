# 風綴りの群島 · 渡り手図鑑

架空ゲーム「風綴りの群島」の図鑑です。107人の渡り手を探し、スキルや育成値を確認できます。45件のコンテンツを掲載し、コメント・ビルド・攻略投稿、通報、管理者による対応を扱います。

**[Dockerで起動する](portfolio-demo/README.md#dockerで起動する)** · [操作を試す](portfolio-demo/README.md#試し方) · [構成図・仕様](docs/ARCHITECTURE.md) · [Codexとの開発記録](docs/AI_DEVELOPMENT.md)

| プロジェクト | 担当 |
| --- | --- |
| [catalog-core](catalog-core/README.md) | Java 25 LTS・DDD。キャラクター、技、効果、検索根拠と検索ユースケース |
| [portfolio-demo](portfolio-demo/README.md) | Spring Boot・PostgreSQL・Flyway・Next.js。22のREST API、架空データ、図鑑・投稿・管理の画面 |

## Codexを使った開発

使用ツールは **OpenAI Codex** です。私が要件と優先順位を決め、画面を見ながら仕様を修正しました。Codexには資料の照合、設計案、実装、JUnitテスト、Dockerでの動作確認を依頼しました。

私が工夫した点は、検索で迷ったときの扱いを決め、閲覧者が実際に操作して確認できる形にしたことです。

- **検索の根拠を順位に反映**。根拠が弱い候補も残し、明示された効果で条件を満たすキャラクターを先に表示します。
- **画面の役割を絞る**。提案で比重が大きかったイベントは調査資料として扱い、一覧・効果検索・詳細を主な導線にしました。一覧は図鑑順を初期値にし、比較表を設けず並べ替えに留めました。
- **同じ検索基盤で試せる形にする**。共通のJava基盤と架空データを扱うアプリを分け、実際の画面・API・DBをDockerで起動できるようにしました。

初回公開までの人間の作業時間は **約5時間**（対話履歴からの概算）です。[見積もりの内訳と対話の試行錯誤](docs/AI_DEVELOPMENT.md) · [公開ソースを使った分析事例](docs/WESNOTH_ANALYSIS.md)

**Codexコードレビュー実施済み**。初回公開時と機能拡張後にレビューを実施しました。[初回の結果](docs/AI_DEVELOPMENT.md#codexコードレビュー) · [第2回の結果と対応](docs/AI_DEVELOPMENT.md#codexコードレビュー第2回)

### 調査を検索仕様に落とした例

架空の技「夜渡りの灯」は「回復」を明示し、「護り」とは関連候補で結び付けています。**護りと回復のAND検索**では、両方が明示されたマレアとユラの後に、トワが「関連候補」として並びます。**OR検索**では、トワも回復を明示しているため、明示一致のグループに入ります。表示順位を見れば、元の調査で区別した根拠が検索結果にどう反映されるか確認できます。

[検索のテスト](catalog-core/src/test/java/dev/characteratlas/application/CharacterCatalogTest.java) · [架空データの初期登録](portfolio-demo/src/main/resources/db/migration/V100__archipelago_catalog.sql) · [全機能版の追加データ](portfolio-demo/src/main/resources/db/migration/V101__voyager_atlas.sql) · [画面での試し方](portfolio-demo/README.md#試し方)

## 共通基盤のテスト

JDK 25がある環境では、リポジトリのルートから次のコマンドを実行できます。

```sh
cd catalog-core
./gradlew test
```
