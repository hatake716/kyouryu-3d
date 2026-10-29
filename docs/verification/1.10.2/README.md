# 1.10.2 / versionCode 13 — 歩行速度の調整

作業日：2026-09-14。パッケージ：`io.github.hatake716.dinobones`。

## 変更

シアターモードの最大歩行速度を **8 m/秒から10 m/秒へ25%上げました**。スティックの倒す量による速度調整、斜め移動の補正、壁・展示台・植栽・家具への衝突判定を継続します。アプリ内の案内とREADMEも更新しました。骨格・復元イラスト・地図・図鑑本文・データ収集の仕様に変更はありません。

バージョンを1.10.2 / code 13へ更新し、従来と同じ本アプリ専用のアップロード鍵でAPK/AABを署名しています。[サイズとSHA-256](build-artifacts.json)。秘密鍵やパスワードは配布資料に含めません。

## ビルド・署名・内容

[ビルド](build.txt)：`testDebugUnitTest lintRelease assembleRelease bundleRelease assembleDebug assembleDebugAndroidTest` 成功。単体テスト16件、失敗0・エラー0。移動距離は最大入力0.1秒で1 m、半分の入力で0.5 mになること、斜め移動・全展示室の通行・壁や標本台の衝突判定を既存テストで確認。[集計](test-summary.json)。Lintはエラー0、警告33。

[APK署名](apk-signature.txt)、[AAB署名](aab-signature.txt)、[AAB証明書](aab-certificate.txt)、[16 KB zipalign](zipalign.txt)、[bundletool](bundle-validation.txt)の検証に成功。証明書SHA-256は `179a7b6d654bfd424e2b4c9da7e737fab60c1b383a5beb46b8efe98b3ffa74cb` で以前と一致。[AAB内Manifest](bundle-manifest.xml)でapplicationId、1.10.2、code 13を確認。

[パッケージ内容](package-content.txt)：APK/AABとも全222 assetsが作業ツリーとバイト一致、100館内メッシュを保持。ネイティブライブラリ4点は前版と同一です。

## 操作の検証

API 35エミュレーターで既存の操作テスト2件を実行。歩行と見回しの二本指同時操作、指を離したときの停止、展示の選択、図鑑一覧との往復、位置の保持、館内マップ、縦横切り替えを確認しました。[実行ログ](gesture-tests.txt)、[JUnit結果](gesture-tests.xml)。GradleのEmulatorConsole接続に関する通知はログへ残っていますが、ADB経由の2テストはどちらも実行され成功しています。

最終署名済みAPKは、エミュレーターで1.10.1から1.10.2へ上書き更新して確認。[起動・歩行・横画面・一覧往復とAPKハッシュ](release-smoke.txt)。クラッシュバッファは空です。[入口](release-entrance.png)、[歩行後](release-after-walking.png)、[横画面](release-landscape.png)、[一覧から戻った画面](release-return.png)を保存しています。

## 掲載画像と配布範囲

ストアの縦4枚・横4枚は、画面構成・画像・モデルが同じため1.10.1で撮影したものを継続利用します。[適合確認](../../store/release-compatibility.json)。元の撮影バージョンとハッシュを維持し、1.10.2で撮影したようには扱いません。ストア用チェッカーは、別バージョンの画像を使う場合に対象リリースごとの確認記録と元APKのハッシュを検証します。

このターンの作業範囲は修正とGoogle Play提出用データの作成までです。実機へのインストール、Google Playへの提出・審査・配信、GitHubへのcommit/pushは実施していません。既存の作業中の動画ファイルは編集せず、今回の配布キットにも含めません。
