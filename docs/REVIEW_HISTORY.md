# レビューの指摘と対応結果

公開版の変更を、指摘元、判断、対応差分、確認方法から追えます。人間レビューの項目は、開発者本人が残したIssueの記録に沿って整理しています。[レビュー原文](https://github.com/rk-codefolio/character-atlas/issues/1)

## レビューの進め方

1. 対象コミットを固定する。人間レビューでは、機能拡張とCodexの第2回レビュー対応が入った [`c0f976e`](https://github.com/rk-codefolio/character-atlas/commit/c0f976e) を起点にした。
2. 指摘を実装と照らし合わせ、対応方針を決める。開発者は、図鑑・投稿・モデレーションの責務、SQLの配置、型と例外の契約、テストの合格条件を指定した。
3. 対応を責務ごとにPRに分ける。各行のPR差分から、変更箇所と追加された検証を確認できる。
4. マージ後のコードをCodexで再レビューする。残った指摘は別のPRにまとめ、修正後に再検証する。[第3回レビュー](AI_DEVELOPMENT.md#codexコードレビュー第3回)

## 人間レビュー

| 指摘 | 判断と対応結果 | 差分・確認先 |
| --- | --- | --- |
| 図鑑・投稿・モデレーションの責務と依存方向が追いにくい | 機能とレイヤーで分割。RESTの入出力、ユースケース、業務ルール、DB・外部連携の役割を分け、外部実装への境界にインターフェースを置いた | [PR #6](https://github.com/rk-codefolio/character-atlas/pull/6/files) · [構成図](ARCHITECTURE.md) |
| PostgreSQLアクセスとSQLの配置を揃えたい | アプリの永続化をMyBatisへ移行し、Flywayを継続。単純な固定SQLはMapper、JOINや動的SQLなどはMapper XMLに配置した | [PR #7](https://github.com/rk-codefolio/character-atlas/pull/7/files) · [PR #11](https://github.com/rk-codefolio/character-atlas/pull/11/files) · [PostgreSQL統合テスト](../archipelago-app/src/test/java/dev/characteratlas/archipelago/ArchipelagoIntegrationTest.java) |
| 型や欠損値の扱い、命名、コメント、書式を揃えたい | 固定の選択肢にはenum、データや規則が異なる閉じた型にはsealedを使う方針を定義。型を責務ごとに配置し、Optional、Stream、Javadoc、Java書式の基準を明文化。公開アプリを `archipelago-app` に改名した | [PR #8](https://github.com/rk-codefolio/character-atlas/pull/8/files) · [PR #11](https://github.com/rk-codefolio/character-atlas/pull/11/files) · [開発ルール](DEVELOPMENT_RULES.md) |
| 検証とエラー応答の責務が分かりにくい | 入力自体の制約はドメイン、登録データの実在確認はユースケースで扱う方針とした。エラーを型とメッセージ定義で表し、RESTの応答は共通ハンドラーで変換するよう整理した | [PR #11](https://github.com/rk-codefolio/character-atlas/pull/11/files) · [ドメイン一覧](DOMAINS.md) |
| テストの意図と合格条件が見えにくい | 日本語で前提・操作・期待結果を記録するルールを設け、単体・統合テストを合算したJavaの行カバレッジが90%未満なら `check` を失敗させるようにした | [PR #9](https://github.com/rk-codefolio/character-atlas/pull/9/files) · [検証結果](https://github.com/rk-codefolio/character-atlas/actions/workflows/verification.yml) |
| レビューの方針を後続の変更でも使いたい | 設計、テスト、PR運用を文書化。人間レビューとCodexレビューへの対応は別PRとし、マージ後にリモートの作業ブランチを削除する手順を定めた | [PR #5](https://github.com/rk-codefolio/character-atlas/pull/5/files) · [開発ルール](DEVELOPMENT_RULES.md) |
| APIの外部アクセス対策 | Issueで別途検討とした | [レビュー原文](https://github.com/rk-codefolio/character-atlas/issues/1) |

## Codexレビュー

| 回 | 主な指摘と対応結果 | 全指摘・差分 |
| --- | --- | --- |
| 初回公開 | PostgreSQLの起動確認をTCPに変更。検索クリア時にANDへ戻し、詳細APIで覚醒前後の値を区別した | [3件の指摘](AI_DEVELOPMENT.md#codexコードレビュー) · [PR #2](https://github.com/rk-codefolio/character-atlas/pull/2/files) |
| 機能拡張後 | 投稿の任意欄の型を検証し、検索・詳細のURLを復元できるようにした。通報時の投稿詳細を保存し、通知文の長さを検証した | [6件の指摘](AI_DEVELOPMENT.md#codexコードレビュー第2回) · [PR #4](https://github.com/rk-codefolio/character-atlas/pull/4/files) |
| 人間レビュー対応後 | PostgreSQLを使うテストの結果再利用を防止。既存DBの設定引き継ぎを追記し、投稿の詳細を種別ごとの型で保持するようにした | [3件の指摘](AI_DEVELOPMENT.md#codexコードレビュー第3回) · [PR #10](https://github.com/rk-codefolio/character-atlas/pull/10/files) |

## 原因から修正と検証を追う例

- **PostgreSQLの起動順**。初期化に使う一時サーバーでも従来の起動確認が通る可能性があり、APIがTCPで接続できる時点とのずれがあった。起動確認にTCPの接続先を指定した。確認する場合は、検証用の空のDBボリュームでComposeを起動し、DBの初期化後にAPIが起動することを見る。[レビュー指摘](AI_DEVELOPMENT.md#codexコードレビュー) · [修正差分](https://github.com/rk-codefolio/character-atlas/pull/2/files)
- **投稿詳細の型を保つ境界**。ユースケースから永続化へ渡す途中で投稿の詳細が型を失っていた。種別ごとの詳細型は投稿モデルに置き、図鑑との照合はユースケース、既存のJSON形式への変換は永続化アダプターに置いた。テストでは入力の種別や値を検証し、通報後に元投稿が変わっても当初の攻略詳細が保存されることを確認する。[修正差分](https://github.com/rk-codefolio/character-atlas/pull/10/files) · [統合テスト](../archipelago-app/src/test/java/dev/characteratlas/archipelago/ArchipelagoIntegrationTest.java)
