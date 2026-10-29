# リリース手順 — 1.10.2

パッケージ：`io.github.hatake716.dinobones`。versionCode **13**。Android 8.0（API 26）以上、OpenGL ES 2.0必須。compileSdk / targetSdkは **36**。

1.10.2ではシアターモードの最大歩行速度を8 m/秒から10 m/秒へ25%上げました。Google Play提出用のAABと直接インストール用のAPKを、既存と同じ署名鍵で作成しています。この変更のGoogle Play提出・審査申請・GitHubへの公開は行っていません。

## 配布ファイル

ローカル配置は `artifacts/release/`。以前の1.10.1のファイルも保持しています。

| ファイル | 用途 |
|---|---|
| `kyouryu-3d-1.10.2.aab` | Google Play Consoleへの提出 |
| `kyouryu-3d-1.10.2.apk` | 手動インストール／検証 |
| `kyouryu-3d-1.10.2-play-kit.zip` | 説明文・既存のストア画像・公開ページ・権利資料・検証記録 |
| `SHA256SUMS-1.10.2.txt` | 上記3ファイルの整合確認 |

AABは端末へ直接インストールできません。[サイズ・SHA-256](build-artifacts.json)、[1.10.2の検証記録](verification/1.10.2/README.md)。ストアの縦横8枚は1.10.1で撮影したものを、画面構成が同じ1.10.2でも使用します。画像の撮影元バージョンやハッシュは書き換えていません。

アップロード証明書SHA-256：`179a7b6d654bfd424e2b4c9da7e737fab60c1b383a5beb46b8efe98b3ffa74cb`。秘密鍵・パスワードはリポジトリと配布キットへ含めません。Google Play App Signingでは、アップロード鍵とPlayが端末へ配布するAPKの署名鍵が異なる場合があります。

## 公開用資料

- [PLAY_STORE.md](PLAY_STORE.md)：コピー用の日本語説明文、画像、サポートメール。
- [PLAY_CONSOLE.md](PLAY_CONSOLE.md)：データセーフティ、子どもを含む全年齢の方針、IARC、審査用操作、残るConsole作業。
- [ストア画像一覧](store/README.md)：縦4枚・横4枚、アイコン、フィーチャーグラフィック、代替テキスト。
- [プライバシーポリシー](https://hatake716.github.io/kyouryu-3d/privacy/)、[保護者向けガイド](https://hatake716.github.io/kyouryu-3d/parents/)、[出典一覧](https://hatake716.github.io/kyouryu-3d/credits/)。
- [CONTENT_LICENSE.md](CONTENT_LICENSE.md) と [ストア画像の帰属](store/CREDITS.md)：原本・作者・ライセンス・変更の記録。
- [今回の公開準備の検証記録](verification/play-preparation/README.md)。

## ビルド・検証の再現

JDK 17、Android SDK Platform 36 / Build Tools 36.0.0を使用します。`keystore.properties.example`を参考に、別途管理している本アプリ専用の署名設定をローカルへ配置します。既存のアプリの更新では同じアップロード鍵を使い、無関係な別アプリの鍵や新規生成した鍵へ置き換えないでください。

```sh
python3 tools/build_catalog.py
python3 tools/catalog/write_profiles.py
python3 tools/check_catalog.py
python3 tools/check_store_assets.py
./gradlew testDebugUnitTest lintDebug
ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest
./gradlew assembleRelease bundleRelease
```

出力：`app/build/outputs/apk/release/app-release.apk`、`app/build/outputs/bundle/release/app-release.aab`。署名設定がない環境のrelease出力は未署名です。GitHubの通常のAndroid CIも秘密鍵を使わず、アップロード可能な署名済みAABの配布を代替しません。

公開キットの再生成：`python3 tools/package_play_release.py`。既存の検証済みAPK/AABのハッシュが一致することを確認し、許可した資料だけをZIP化します。秘密鍵は暗号化して別の安全な場所へバックアップしてください。

## Pagesの公開

`.github/workflows/pages.yml`は`main`上のサポートページだけを公開します。`docs/index.html`、`site.css`、`privacy/`、`parents/`、`credits/`を配信し、巨大なモデル・検証画像・署名済みバイナリはサイトに含めません。

Pagesの設定はGitHub Actionsを利用する方式です。ワークフローの成功だけでなく、プライバシーURLがログイン不要のHTTPSで200を返し、アプリ名・開発者名・連絡先・本文を読めることを確認します。サイトの変更後も同様に確認してください。

## Play Consoleでの提出

1. メール **acesmash@gmail.com**、サイトとプライバシーURL、掲載文章・画像を登録。
2. アクセス制限なし、広告なし、アプリ内購入なし、データ収集・共有なしの現在の実装に合わせ、各申告を入力。
3. 全年齢を対象としてFamiliesの要件を確認し、実際の内容・外部リンクを確認したうえでIARCを取得。
4. 価格・配信国とPlay App Signingを設定。versionCode 13が未使用であることを確認してAABを内部テストへ提出。
5. Playからインストールした版を検証し、Pre-launch reportや互換性の警告を確認。必要なクローズドテストと製品版アクセス申請を行う。
6. 審査に提出し、承認後の公開状態を確認する。

対象APIは[Google Playの公式要件](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)、公開ポリシーは[ユーザーデータの公式要件](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en)を2026-09-08に確認。実際の提出時にも最新のConsole案内を確認してください。
