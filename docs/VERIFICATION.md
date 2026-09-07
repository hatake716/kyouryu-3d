# 検証記録 — 1.1.0 / versionCode 2

確認日：2026-09-07。パッケージ：`io.github.hatake716.dinobones`。

## ビルドと自動検証

JDK 17、Gradle 8.14.3、Android SDK 36で次のタスクが成功しました。

```sh
./gradlew assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease bundleRelease
```

- ユニットテスト：3件、失敗0件。全100モデルの個別性・有限座標・範囲・部位、カメラ限界、100項目／693節（注記を含む）の図鑑とHTTPSの出典・動画URL、日本語検索・分類を検証。[実行結果](verification/unit-tests.xml)。
- Android Lint：エラー0件、警告26件。主に新しい依存バージョンの提案、KTX/TOMLへの整理提案。targetSdkは36。画面寸法取得・旧OSバックアップ指定の提案等も残っています。
- エミュレーターのInstrumentation：5件、失敗0件、358.473秒。[実行結果](verification/instrumentation.txt)。
- `tools/check_catalog.py`：100項目、100画像の実ファイルハッシュ・帰属・ライセンス、出典記録、候補1468項目の閲覧数記録、選定した100項目の3か月分のデータを検証。

エミュレーターは `emulator-5554`、API 35、1080 × 2400。アプリ・テストAPKのインストール先と入力操作先をこのシリアルに限定しました。

## 実際の画面操作

- 全100種類を一覧から検索して開き、OpenGLによるメッシュ描画と背景テクスチャの読み込みを確認。各画面のスクリーンショットをローカルの `artifacts/verification-1.1.0/evidence/` に保存し、代表画面をGitに収録。
- 検索欄に最初の1文字を入れておすすめ欄が隠れた後も入力フォーカスが保持され、続く文字列で絞り込めることを検証。
- ティラノサウルスで1本指回転、2本指ピンチ拡大、2本指移動を入力し、カメラの変化を検証。
- Activity再生成後の視点復元、リセット、頭骨の強調、全画面表示、図鑑切り替え、お気に入り保存と再起動後の保持。
- アプリ説明・ライセンスのダイアログ。
- 背景の濃さ0で実際の描画ピクセルが背景なしになること、背景設定で3D視点が変化しないこと、画像全体表示・全画面を覆う表示、Activity再生成後の背景設定と視点の保持を検証。背景の出典ダイアログも目視確認。
- 図鑑のWikipedia、YouTube検索、選定したYouTube動画、背景画像の出典・ライセンスの各ボタンから発行されるACTION_VIEWのHTTPS URIを検証。外部アプリの起動はテストで捕捉し、動画の再生そのものは自動操作していません。
- ストア用の独自アイコン512 × 512、フィーチャーグラフィック1024 × 500を書き出し、目視確認。

スクリーンショットは [screenshots](screenshots/)。図鑑・モデルの描画結果であり、実物標本を撮影した画像ではありません。背景を含む画像の権利は [スクリーンショットの説明](screenshots/README.md) を参照してください。

## 配布ファイル

- R8最適化・リソース縮小・アプリ専用鍵による署名を有効化したAPK/AABを作成。
- APK：`apksigner verify --verbose --print-certs` 成功。[署名検証](verification/apk-signature.txt)。
- AAB：`jarsigner -verify` とGoogle bundletool 1.18.3の `validate` 成功。
- APK：`zipalign -c -P 16 -v 4` 成功。バンドル中のAndroidXネイティブライブラリは全4 ABIでELF LOADのアラインメント0x4000を確認。[検証記録](verification/native-alignment.txt)。16 KBページサイズの端末での起動試験は未実施。
- サイズとSHA-256：[build-artifacts.json](build-artifacts.json)。ローカルの `artifacts/release/` に配布用コピーとSHA256SUMSを保存。
- 署名付き1.0.0を同じエミュレーターにインストールしてお気に入りを登録後、署名付き1.1.0を `adb install -r` で更新。お気に入りの保持、コールド起動成功、versionName 1.1.0 / versionCode 2 / targetSdk 36を確認。縦向き・横向きの背景付き3D画面も目視確認。横向きでは操作ボタンを右端へ配置。[更新試験の記録](verification/release-smoke.txt)。
- 1.0.0と1.1.0の署名証明書は同一。SHA-256：`179a7b6d654bfd424e2b4c9da7e737fab60c1b383a5beb46b8efe98b3ffa74cb`。

署名鍵とパスワード、ビルド生成物はGitから除外しています。CIは署名設定を持たないため、そこで生成するrelease AABは未署名です。

## 出典・権利と確認範囲

Wikipediaの全100項目の固定版・履歴・取得情報は [source-provenance.json](source-provenance.json)。元の13項目は主に英語版からの翻案、追加87項目は基本カードの編集と日本語版本文の抄録です。選定動画の公開元は [video-links.json](video-links.json)、解決済みのrelease依存60件とファイルハッシュは [runtime-dependencies.json](runtime-dependencies.json)。この更新で依存は追加・更新していません。依存ライセンスはアプリ内でも読めます。

背景100点を目視し、98点のCommons画像は個別の作者・CC BY／CC BY-SAまたはパブリックドメイン表示を記録しました。2点はAI生成で、生成記録と仮説表示を付けています。[画像の帰属](ARTWORK_CREDITS.md)、[生成記録](GENERATED_ARTWORK.md)。ライセンス情報の確認は、あらゆる法的問題が起こらないことの保証ではありません。

人気の目安は英語版Wikipediaの2026年6〜8月の人による閲覧数です。候補1468項目の取得記録のうち、選外の7項目だけは1〜2か月分の応答であることを記録し、欠測を0とみなしていません。収録100項目はすべて3か月分を取得済みです。候補の範囲と除外条件は [POPULARITY.md](POPULARITY.md) に明記しています。

骨格は独自制作の模式復元で、追加87項目は共通の体型構造に属ごとの設定を加えています。骨数や解剖学的形状の精密再現・専門家の監修・博物館スキャンとの一致は検証していません。図鑑は確認日時点の主要情報で、情報量は種類により異なり、すべての既知情報の網羅や自動更新ではありません。

物理端末へのインストール、API 26〜36の全OS・機種横断試験、長時間の発熱・電池・メモリー計測、完全なアクセシビリティ監査、Google Play経由の配信試験・審査・公開は未実施です。公開手順は [RELEASE.md](RELEASE.md) に記載しています。
