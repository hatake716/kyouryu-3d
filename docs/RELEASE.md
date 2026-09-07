# リリース手順

パッケージID：`io.github.hatake716.dinobones`
minSdk：26 / compileSdk・targetSdk：36 / OpenGL ES 2.0必須。

## 署名

この開発環境ではアプリ専用のアップロード鍵を新規作成し、ローカルの `keystore.properties` に設定しています。別環境では以下に従って自身の鍵を設定してください。

`keystore.properties.example` を `keystore.properties` としてコピーし、アプリ専用のアップロード鍵を設定します。秘密鍵・パスワードはGitに含めません。既存の別アプリの鍵は使用しません。

```properties
storeFile=/absolute/path/to/kyouryu-upload.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=kyouryu-upload
keyPassword=YOUR_KEY_PASSWORD
```

署名設定がない場合、release AAB/APKは未署名です。Play Consoleへアップロードする前に署名を確認してください。Google Play App Signingを使う場合、アップロード鍵と配布APKの署名鍵が異なることがあります。

```sh
./gradlew testDebugUnitTest lintDebug
ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest
./gradlew assembleRelease bundleRelease
```

出力：`app/build/outputs/apk/release/app-release.apk`、`app/build/outputs/bundle/release/app-release.aab`。

AABはPlay Console提出用で、端末に直接インストールできません。APKは直接インストール用です。秘密鍵と署名設定を暗号化して別の安全な場所にバックアップしてください。

## 公開準備

1. `CONTENT_LICENSE.md`、アプリ内の出典・帰属・変更表示を確認する。
2. `docs/privacy/index.html` を公開し、ログイン不要で読めるHTTPS URLを確認する。GitHub Pagesを使う場合はリポジトリのPages設定が別途必要。
3. Play Consoleでアプリを作成し、サポート連絡先・データセーフティ・対象年齢・IARCを実際の方針に従って登録する。
4. `PLAY_STORE.md`の文案と独自のストア素材、動作画面を登録する。
5. 署名したAABを内部テストへアップロードし、Playで配信されるビルドを検証する。必要なテスト期間・開発者アカウントの要件に従う。
6. ストア審査に提出する。ローカルビルド成功は、審査通過や公開を意味しない。

2026-09-07の確認先：
- 対象API：https://support.google.com/googleplay/android-developer/answer/11926878
- 知的財産：https://support.google.com/googleplay/android-developer/answer/9888072
- ユーザーデータ・プライバシー：https://support.google.com/googleplay/android-developer/answer/10144311

初期版の精度範囲：独自の模式骨格と主要情報の図鑑。精密な標本スキャン、全既知情報の網羅、研究成果の自動更新、専門家による監修は含みません。
