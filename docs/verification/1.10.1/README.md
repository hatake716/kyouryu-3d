# 検証記録 — 1.10.1 / versionCode 12

確認日：2026-09-08（JST）。パッケージ：`io.github.hatake716.dinobones`。前版は [1.10.0](../1.10.0/README.md)。

## 変更範囲

横画面では通知バー・ナビゲーションバーを隠し、画面端からのスワイプで一時表示します。縦画面に戻すと両方のバーを復元します。館内・図鑑・個別3D・復元イラストと各案内ダイアログへ適用しました。シアターはカメラ穴周辺も含めて端まで描画し、上部のタイトルとボタンは穴を避けます。横画面左上のタイトルは縦画面と同じ土色から透明へ変化する背景と明るい文字色にしました。

Android 15で同じウィンドウを再利用するActivity再生成時、OSの内容領域への余白適用が残るケースを再現しました。API 35だけ補助のレイアウトフラグを併用し、再生成後も館内Viewが原点(0, 0)から2400 × 1080を使うことを確認しました。骨格・復元画・本文・ライセンスの素材は変更していません。

## ビルドと配布形式

[署名済みAPK/AABビルド](build.txt)成功。[Lint](lint-results.xml)はエラー0・警告33。

[APK署名](apk-signature.txt)、[16 KB zipalign](zipalign.txt)、[AAB署名](aab-signature.txt)、[bundletool](bundle-validation.txt)の検証に成功しました。署名証明書SHA-256は従来と同じ `179a7b6d654bfd424e2b4c9da7e737fab60c1b383a5beb46b8efe98b3ffa74cb`。jarsignerの自己署名・タイムスタンプ等の通知はログに保持します。

[パッケージ内容](package-content.txt)：各222 assetsが作業ツリーとバイト一致、100館内メッシュを同梱。ネイティブライブラリ4点は従来版と同一。[配布ファイルのサイズとSHA-256](build-artifacts.json)。

## エミュレーターでの確認

API 35、1080 × 2400 / 2400 × 1080、SwiftShaderを使用。

[全画面の操作試験](immersive-tests.txt)は29.668秒で成功。起動・館内マップ・案内・図鑑一覧への往復・Activity再生成・一時停止からの復帰で、バーの非表示と全画面の描画領域を確認しました。[再生成後のView領域](layout-recreated.txt)、[復帰後のView領域](layout-resumed.txt)。

[端からスワイプ直後](immersive-edge-swipe.png)に時刻・通知・ナビゲーションの表示が現れ、[約5秒後](immersive-bars-auto-hidden.png)に再び隠れることを画像で確認しました。[縦画面へ戻した画面](immersive-portrait-restored.png)では両方のバーを復元。[館内の案内](immersive-help.png)と[館内マップ](immersive-map.png)にも不要なバーはありません。

初回の自動操作はAndroidの全画面チュートリアルに遮られたため、エミュレーター上でその案内を閉じる手順を追加しました。縦画面の検査は、表示対象のないキャプションバーを含めず、通知バーとナビゲーションバーを個別に検査します。

[個別3Dの回帰試験](viewer-regression.txt)は66.221秒で成功。トリケラトプス・ティラノサウルス・パタゴティタン・ブラキオサウルス・プテラノドン・モササウルス・アーケオプテリクスの7種類で縦横の表示領域を確認しました。回転・拡大・通常表示と観察用全画面の往復・Activity再生成時の視点保持・詳細図鑑の上下スワイプも成功しました。

## 最終署名済みAPK

API 35エミュレーターで1.10.0から最終1.10.1へ上書き更新。[操作記録](release-smoke.txt)に配布APKとのSHA-256一致と空のクラッシュバッファを記録しました。

[横画面の館内](../../screenshots/release-1.10.1-theater-landscape.png)、[館内マップ](../../screenshots/release-1.10.1-map.png)、[復元イラスト](../../screenshots/release-1.10.1-illustration.png)、[画像の出典画面](../../screenshots/release-1.10.1-illustration-source.png)、[縦画面への復帰](../../screenshots/release-1.10.1-theater-portrait.png)を未加工PNGで保存し、目視確認しました。

## 実機への反映

Pixel 10a（API 37）へ、1.10.0 / code 11から1.10.1 / code 12を `install --no-incremental -r` で上書き更新しました。アンインストールやデータ消去はしていません。[実機インストール記録](device-install.txt)に、端末上のAPKが検証済み配布APKとSHA-256一致すること、安全な起動、稼働プロセス、空のクラッシュバッファを記録しました。実機へのタップ・スワイプ・キー入力は行っていません。ジェスチャーと画面遷移の検証はエミュレーターで実施しています。

この端末更新の検証を完了した時点では、Google Playへのアップロード・公開、GitHubへのコミット・pushは未実施です。その後の掲載資料とGitHub反映は [公開準備の検証記録](../play-preparation/README.md) にまとめています。
