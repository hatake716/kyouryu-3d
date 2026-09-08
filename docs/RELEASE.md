# リリース手順 — 1.10.1

パッケージ：`io.github.hatake716.dinobones`。versionCode **12**。Android 8.0（API 26）以上、OpenGL ES 2.0必須。compileSdk / targetSdkは **36**。

Google Play提出用のAAB、直接インストール用のAPK、掲載文、縦横8枚の画像、公開ポリシーと申告ガイドを用意しています。**Google Playへのアップロード・審査・配信は別工程で、未実施です。**

## 検証済みの配布ファイル

ローカル配置は `artifacts/release/`。Gitのソースには大きな配布バイナリや署名鍵を含めず、[GitHub Releases](https://github.com/hatake716/kyouryu-3d/releases)の1.10.1のドラフト添付で管理します。ドラフトは権限のある開発者だけが閲覧でき、一般公開版ではありません。

| ファイル | 用途 | サイズ |
|---|---|---:|
| `kyouryu-3d-1.10.1.aab` | Google Play Consoleへの提出 | 190,377,351 bytes |
| `kyouryu-3d-1.10.1.apk` | 手動インストール／検証 | 188,974,683 bytes |
| `kyouryu-3d-1.10.1-play-kit.zip` | 説明文・画像・公開ページ・権利資料 | 生成後のSHA256SUMSを参照 |
| `SHA256SUMS.txt` | 添付ファイルの整合確認 | `sha256sum -c SHA256SUMS.txt` |

AABは端末へ直接インストールできません。署名済みの同一APKで撮影・操作確認をしています。各配布ファイルのサイズ・SHA-256は [build-artifacts.json](build-artifacts.json)、署名・16 KB配置・AABの検証は [1.10.1の検証記録](verification/1.10.1/README.md)を参照。

アップロード証明書SHA-256：`179a7b6d654bfd424e2b4c9da7e737fab60c1b383a5beb46b8efe98b3ffa74cb`。秘密鍵・パスワードはリポジトリと配布キットへ含めません。Google Play App Signingでは、アップロード鍵とPlayが端末へ配布するAPKの署名鍵が異なる場合があります。手動インストール版からの移行方法は、Play配信版の署名・内部テストで確認してください。

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
4. 価格・配信国とPlay App Signingを設定。versionCode 12が未使用であることを確認してAABを内部テストへ提出。
5. Playからインストールした版を検証し、Pre-launch reportや互換性の警告を確認。必要なクローズドテストと製品版アクセス申請を行う。
6. 審査に提出し、承認後の公開状態を確認する。

対象APIは[Google Playの公式要件](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)、公開ポリシーは[ユーザーデータの公式要件](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en)を2026-09-08に確認。実際の提出時にも最新のConsole案内を確認してください。
