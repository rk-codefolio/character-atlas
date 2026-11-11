# PostgreSQL起動判定の観測

2026年11月11日に、PR #2の修正理由を独立した検証環境で再確認しました。今回の観測は、当時のログに追加した事後検証です。[当時の修正と検証記録](https://github.com/rk-codefolio/character-atlas/pull/2)

## 仮説と観測方法

PostgreSQLの公式イメージは、初期化用スクリプトを実行する間、一時サーバーを起動します。接続先を指定しないpg_isreadyがUnixソケットへ接続できても、別コンテナーのAPIから使うTCPが利用できるとは限らない、という仮説を確認しました。

対象はアプリと同じ `postgres:18.6-alpine`。空の一時DBを使い、初期化中の状態を観測するために `/docker-entrypoint-initdb.d/10-observation.sql` へ `SELECT pg_sleep(12);` を置きました。これは観測時間を確保するための検証専用設定です。Dockerのディスク空き容量に制約があるため、PGDATAは256MiBのtmpfs内に設定しました。

コンテナーの起動中に、次の二つを同じDBに対して実行し、それぞれの終了コード、標準出力、サーバーログを記録しました。

```sh
docker exec atlas-startup-observation pg_isready -U archipelago -d archipelago
docker exec atlas-startup-observation pg_isready -h 127.0.0.1 -U archipelago -d archipelago
```

## 観測結果

| 状態 | Unixソケット | TCP 127.0.0.1 | ログでの確認 |
| --- | --- | --- | --- |
| 初期化スクリプト実行中 | accepting connections、終了コード0 | no response、終了コード2 | 一時サーバーがUnixソケットで待ち受け |
| 初期化完了後の本サーバー | accepting connections、終了コード0 | accepting connections、終了コード0 | IPv4での待ち受けが開始 |

[取得した出力とログ抜粋](evidence/postgres-startup-2026-11-11.txt)

修正前の判定を初期化中に実行すると、APIが使うTCPの準備前に成功する状態を再現できました。現在のComposeは `-h 127.0.0.1` を指定し、DBのhealthyをAPIの起動条件にしています。今回、別の空の一時DBで現行Composeを起動し、DBのhealthy、APIのhealthy、Webの起動を確認しました。同じ環境からANDの3件、ORの4件をChromeで描画しています。[現在のCompose](../archipelago-app/compose.yaml) · [検索画面](../README.md#3分で見る検索)

この観測は接続判定の差を確かめたものです。旧構成でAPIの起動失敗が発生する頻度の測定や、通常の永続ボリュームでの初期化は今回の検証範囲に含めていません。
