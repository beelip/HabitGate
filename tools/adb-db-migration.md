# 署名不一致時の DB 移行メモ

Android は、同じ `applicationId` でも署名キーが違う APK を上書きインストールできません。
旧版を GitHub Actions の一時 debug キーでビルドしていた場合、通常の上書き更新が失敗することがあります。

debug APK であれば、PC から `adb` と `run-as` を使って DB を退避・復元できる場合があります。

## 1. 旧版が入っている状態で DB を退避

```bash
adb exec-out run-as com.habitgate.app cat databases/friction_habit.db > friction_habit.db
adb exec-out run-as com.habitgate.app cat databases/friction_habit.db-wal > friction_habit.db-wal 2>/dev/null || true
adb exec-out run-as com.habitgate.app cat databases/friction_habit.db-shm > friction_habit.db-shm 2>/dev/null || true
```

## 2. 旧版をアンインストールして新版を入れる

```bash
adb uninstall com.habitgate.app
adb install habit-gate.apk
```

## 3. 新版を一度起動してから、DB を復元

```bash
adb push friction_habit.db /sdcard/Download/friction_habit.db
adb shell run-as com.habitgate.app cp /sdcard/Download/friction_habit.db databases/friction_habit.db
```

`-wal` / `-shm` も退避できている場合は同様に戻します。

```bash
adb push friction_habit.db-wal /sdcard/Download/friction_habit.db-wal
adb push friction_habit.db-shm /sdcard/Download/friction_habit.db-shm
adb shell run-as com.habitgate.app cp /sdcard/Download/friction_habit.db-wal databases/friction_habit.db-wal
adb shell run-as com.habitgate.app cp /sdcard/Download/friction_habit.db-shm databases/friction_habit.db-shm
```

## 4. アプリを再起動

新版起動時に 必要に応じて DB 移行が走ります。v0.3.0 以降は、通常はアプリ内の CSV エクスポート / インポートを使う方が安全です。

## 注意

- これは debug APK 向けの退避手段です。
- 端末や Android バージョンによっては `run-as` が使えない場合があります。
- 作業前に旧版アプリから CSV 出力しておくことを推奨します。
