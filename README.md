# HabitGate

Android ローカル用の習慣形成・振り返り用アプリです。 

## ダウンロード

Androidスマホに直接インストールする場合は、以下からAPKをダウンロードしてください。

[APKをダウンロード](https://github.com/beelip/HabitGate/raw/refs/heads/main/build/app-debug.apk)

※ Playストア経由ではないため、インストール時に「不明なアプリのインストールを許可」が必要になる場合があります。

## できること

- 毎日指定時刻に「今日は何をした？」通知を出す
- 「やること」を前日に登録し、チェックイン時にチェックボックスで消化する
- 消化した「やること」は `時` `分` 単位で実績時間を保存する
- 消化しなかった「やること」は翌日に繰り越す
- 「減らすこと」を事前登録し、その日にやってしまった項目だけ時間つきで記録する
- 今日 / 今週 / 今月 の記録を一覧と棒グラフで見る
- CSV を出力して Google Sheets に取り込む
- Google Apps Script の Web App URL を設定すると、保存時に未同期記録をスプレッドシートへ送信する

## 使い方

1. この ZIP を展開する
2. 中身を GitHub リポジトリに push する
3. GitHub の `Actions` タブで `Build Android APK` を実行する
4. 成果物 `HabitGate-debug-apk` をダウンロードする
5. ZIP 内の `app-debug.apk` を Android スマホに入れてインストールする

## 初回起動後にやること

1. 通知権限を許可する
2. 「正確なアラーム権限を開く」から、このアプリに正確なアラームを許可する
3. 通知時刻を設定する
4. 「明日のやること」と「減らすこと」を登録する
5. 指定時刻の通知から入力画面を開いて記録する

Android の仕様上、機種や OS 設定によっては、アプリが勝手に全画面で前面表示されず、高優先度通知として出ます。その場合は通知をタップして入力画面を開いてください。

## Google Sheets 連携

完全な OAuth 連携ではなく、個人利用しやすい Google Apps Script Web App 方式です。

1. 新しい Google スプレッドシートを作る
2. `拡張機能` → `Apps Script` を開く
3. `tools/google-sheets-webhook.gs` の内容を貼り付ける
4. `デプロイ` → `新しいデプロイ` → 種類を `ウェブアプリ` にする
5. 実行ユーザーを `自分` にする
6. アクセスできるユーザーを `全員` にする
7. 発行された Web App URL をアプリの「連携URL」に保存する

以後、チェックイン保存時に未同期データが `records` シートへ送信されます。

## データ構造

端末内 SQLite に次を保存します。

- `do_tasks`: 未消化の「やること」
- `reduce_items`: 事前設定した「減らすこと」
- `records`: 実績記録

CSV の列は次です。

```csv
category,title,duration_minutes,duration_hhmm,actual_date,created_at,synced
```

`category` は以下です。

- `DO`: やること
- `REDUCE`: 減らすこと

## 注意

- これはまず個人利用前提の debug APK です。Play ストア配布用の署名・審査対応はしていません。
- Android のバックグラウンド起動制限により、「必ず画面がポップアップする」挙動は保証できません。通知・正確なアラーム・フルスクリーン通知を組み合わせています。
- Google Apps Script の Web App URL は、知っている人が POST できる入口になります。公開範囲を理解した上で使ってください。

## 開発メモ

- Java + Android 標準 UI
- minSdk 29 / targetSdk 35 / compileSdk 35
- Android Gradle Plugin 8.7.3
- GitHub Actions で debug APK を artifact 化
