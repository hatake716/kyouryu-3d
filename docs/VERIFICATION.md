# 検証記録 — 1.0.0 / versionCode 1

確認日：2026-09-07。パッケージ：`io.github.hatake716.dinobones`。

## ビルドと自動検証

JDK 17、Gradle 8.14.3、Android SDK 36で次のタスクが成功しました。

```sh
./gradlew assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease bundleRelease
```

- ユニットテスト：3件、失敗0件。全13モデルの個別性・有限座標・範囲・部位、カメラ限界、13項目／104節の図鑑とHTTPSの出典・動画URL、日本語検索・分類を検証。
- Android Lint：エラー0件、警告26件。主に新しい依存バージョンの提案、KTX/TOMLへの整理提案。targetSdkは36。画面寸法取得・旧OSバックアップ指定の提案等も残っています。
- エミュレーターのInstrumentation：3件、失敗0件、60.571秒。[実行結果](verification/instrumentation.txt)。

エミュレーターは `emulator-5554`、API 35、1080 × 2400。アプリ・テストAPKのインストール先と入力操作先をこのシリアルに限定しました。

## 実際の画面操作

- 全13種類を一覧から検索して開き、OpenGLによるメッシュ描画を確認。各画面のスクリーンショットを保存。
- ティラノサウルスで1本指回転、2本指ピンチ拡大、2本指移動を入力し、カメラの変化を検証。
- Activity再生成後の視点復元、リセット、頭骨の強調、全画面表示、図鑑切り替え、お気に入り保存と再起動後の保持。
- アプリ説明・ライセンスのダイアログ。
- 図鑑のWikipedia、YouTube検索、選定したYouTube動画ボタンから発行されるACTION_VIEWのHTTPS URIを検証。外部アプリの起動はテストで捕捉し、動画の再生そのものは自動操作していません。
- ストア用の独自アイコン512 × 512、フィーチャーグラフィック1024 × 500を書き出し、目視確認。

スクリーンショットは [screenshots](screenshots/)。図鑑・モデルの描画結果であり、実物標本を撮影した画像ではありません。

## 配布ファイル

- R8最適化・リソース縮小・アプリ専用鍵による署名を有効化したAPK/AABを作成。
- APK：`apksigner verify --verbose --print-certs` 成功。[署名検証](verification/apk-signature.txt)。
- AAB：`jarsigner -verify` とGoogle bundletool 1.18.3の `validate` 成功。
- APK：`zipalign -c -P 16 -v 4` 成功。バンドル中のAndroidXネイティブライブラリは全4 ABIでELF LOADのアラインメント0x4000を確認。16 KBページサイズの端末での起動試験は未実施。
- サイズとSHA-256：[build-artifacts.json](build-artifacts.json)。ローカルの `artifacts/release/` に配布用コピーとSHA256SUMSを保存。
- 署名付きrelease APKを同じエミュレーターにインストールし、コールド起動成功、versionName 1.0.0 / versionCode 1 / targetSdk 36を確認。縦向き・横向きの3D画面も目視確認し、横向きでは操作ボタンが骨格を覆わないよう右端へ配置。

署名鍵とパスワード、ビルド生成物はGitから除外しています。CIは署名設定を持たないため、そこで生成するrelease AABは未署名です。

## 出典・権利と確認範囲

Wikipedia英語版の全13記事の固定版・履歴・取得情報は [source-provenance.json](source-provenance.json)。日本語版へのリンクも確認しています。選定動画の公開元は [video-links.json](video-links.json)、解決済みのrelease依存60件とファイルハッシュは [runtime-dependencies.json](runtime-dependencies.json)。依存ライセンスはアプリ内でも読めます。

骨格は独自制作の模式復元で、骨数や解剖学的形状の精密再現・専門家の監修・博物館スキャンとの一致は検証していません。図鑑は確認日時点の主要情報で、すべての既知情報の網羅や自動更新ではありません。

物理端末へのインストール、API 26〜36の全OS・機種横断試験、長時間の発熱・電池・メモリー計測、完全なアクセシビリティ監査、Google Play経由の配信試験・審査・公開は未実施です。公開手順は [RELEASE.md](RELEASE.md) に記載しています。
