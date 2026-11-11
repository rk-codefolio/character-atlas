# 投稿詳細の型設計と変更範囲

2026年11月11日に、レビュー対応後のコードを読み直して整理した設計の振り返りです。比較表は今回の検討であり、当時すべての案を比較したという記録ではありません。実際の変更は [PR #10](https://github.com/rk-codefolio/character-atlas/pull/10/files) で確認できます。

## 解決した問題

REST入力の詳細はJSONで届きます。詳細がユースケースと永続化の境界でも汎用JSONのままだと、投稿種別と詳細の組み合わせや各値の制約が、どこで保証されるか追いにくくなります。

現在はRESTの境界でJSONを具体的な詳細型へ変換し、ドメインで生成条件を検証します。ユースケースは投稿先や装具の実在を照合し、永続化アダプターが従来のJSON形式へ戻します。たとえばビルドの航路紋の重複や負のポイントは型の生成時に拒否され、存在しない装具はユースケースで拒否されます。[入力変換](../archipelago-app/src/main/java/dev/characteratlas/archipelago/posting/presentation/ReviewPostController.java) · [投稿モデル](../archipelago-app/src/main/java/dev/characteratlas/archipelago/posting/domain/PostDraft.java) · [ユースケース](../archipelago-app/src/main/java/dev/characteratlas/archipelago/posting/application/PostService.java)

## 代替案と現在の設計のコスト

| 案 | 得られること | 負担・制約 |
| --- | --- | --- |
| 汎用JSONをユースケースまで渡す | 種別や項目を追加する際、Javaの型を増やす作業を減らせる | 各処理で項目の有無と型を読む必要があり、不正な組み合わせを静的に追いにくい |
| 現在のsealed型で種別ごとの詳細を表す | 許可した詳細型を列挙でき、switchの網羅性をコンパイラーで確認できる。詳細型と投稿モデルの生成条件を単体テストで確認できる | 種別追加時に複数のswitchとJSON変換を更新する必要がある。JSONとJavaの二つの表現について互換性を検証する負担がある |
| 種別ごとに保存テーブルも分ける | DB側でも種別ごとの列や制約を設けやすい | スキーマと既存データの移行、投稿一覧と通報スナップショットの読み取りを再設計する必要がある |

現在の投稿種類は3つで、種類ごとの入力規則があります。この構成では、ドメインで型と制約を表し、既存JSONへの変換を永続化側へ集める設計が対応しています。読み取り結果の `Post.details` は現在もJSONです。書き込み時に型を保つ保証と、画面へ返す読み取り形式は別々に確認する必要があります。[詳細型](../archipelago-app/src/main/java/dev/characteratlas/archipelago/posting/domain/details/PostingDetails.java) · [保存時の変換](../archipelago-app/src/main/java/dev/characteratlas/archipelago/posting/infrastructure/persistence/MyBatisPostStore.java) · [読み取りモデル](../archipelago-app/src/main/java/dev/characteratlas/archipelago/posting/application/Post.java)

## 投稿の種類を一つ増やす場合

以下は変更範囲の分析です。新しい投稿機能を実装した結果ではありません。

「新規追加なので既存の投稿レコードを書き換えずに済む」という見通しは、保存形式と既存種別の契約を保つ場合に成立します。一方、現在の型の定義、既存の分岐、DBの制約は追加に合わせて変更が必要です。

| 対象 | 現在の実装と変更箇所 | 確認すること |
| --- | --- | --- |
| 型とドメイン規則 | `PostKind` にenum値、`PostingDetails` に実装型とpermitsを追加。`PostDraft` の詳細型と種別の対応、タイトル・本文の必須条件を見直す | 既存3種の制約を保ち、新種別と詳細の不一致が拒否される |
| ユースケース | `PostService.validate` のswitchに新しい投稿先・参照項目の実在確認を追加 | 新種別に必要な図鑑参照が確認され、既存の投稿先も引き続き有効 |
| DBのスキーマ | `review_post.kind` のCHECK制約はCOMMENT・BUILD・CONTENTだけを許可。新しいFlywayマイグレーションで許可値を追加する | 初期セットアップと既存DBからの更新の両方で新種別を保存できる。適用済みのV3は保持する |
| 保存形式と既存レコード | `MyBatisPostStore.toWrite` に新詳細型のJSON変換を追加。既存のkindとdetailsの形式を維持するなら、既存レコードの書き換えは通常不要 | 旧レコードの一覧・編集・再保存で値が保持され、既存の通報スナップショットも読める |
| API | `ReviewPostController.draft` に入力変換を追加。既存の投稿作成・一覧・管理者編集APIで新種別を扱う | 正常な入力、不正な型、参照不在、権限不足を確認。既存URLや既存種別のリクエスト形式を維持できるかを確認する |
| 画面とモデレーション | `posts.jsx` と `admin.jsx` にラベル、入力・表示の分岐を追加。新種別の投稿先を開く導線も用意する | 投稿・一覧・管理者編集・通報・監査記録で新種別の内容を確認できる。旧画面への混在データの影響も確認する |
| テスト | ドメインの型と制約の単体テスト、REST・DB・通報の統合テスト、旧スキーマからの移行確認を追加 | 新種別の正常系と境界値に加え、既存3種の回帰テストが通る |

DBのCHECK制約は [V3__review_posts.sql](../catalog-core/src/main/resources/db/migration/V3__review_posts.sql)、既存の回帰テストは [PostDraftTest](../archipelago-app/src/test/java/dev/characteratlas/archipelago/posting/domain/PostDraftTest.java) と [ArchipelagoIntegrationTest](../archipelago-app/src/test/java/dev/characteratlas/archipelago/ArchipelagoIntegrationTest.java) で確認できます。

## 確認済みの結果

PR #10では、種別と詳細の不一致、重複した航路紋、過大な難易度、負のポイントを拒否するJUnitの実行と、Java 25・PostgreSQLでの両モジュールのcheck成功が記録されています。現在のmainでも単体・統合テストと合算カバレッジの検査が通過しています。[PR #10の検証記録](https://github.com/rk-codefolio/character-atlas/pull/10) · [mainのCI結果](https://github.com/rk-codefolio/character-atlas/actions/runs/42949236326)
