# 検証記録 — 1.8.0 / versionCode 9

確認日：2026-09-08（JST）。パッケージ：`io.github.hatake716.dinobones`。前回の縮尺変更と全100展示の操作確認は [1.7.0の記録](../1.7.0/README.md) に保存しています。

## 変更範囲

本アプリ用に新規生成した、中生代の巨大森林を思わせる壁画を外周と仕切り壁の両面へ追加しました。横画面の上部全体を覆う暗いグラデーションを除き、明るい土色のタイトルカードと操作ボタンを1列へまとめました。天井材を明るい土色へ変更し、天井の間接光、空、遠景の霞を調整しています。

館内の寸法・100種類の骨格メッシュ・移動速度は1.7.0のままです。原画像の生成履歴・プロンプト・利用条件は [壁画の記録](../../forest-mural-provenance.json)、仕様は [THEATER.md](../../THEATER.md)。壁画は特定の年代・産地の植生を確定した復元図ではありません。

## ビルドと梱包

JDK 17 / Gradle 8.14.3 / SDK 36。debug APK・AndroidTest APK・R8最適化release APK・AABをビルド。[最終ビルド](final-build.txt)。

- ユニットテスト12件成功。既存8件では全100モデルの座標・部位・実測資料・公開メッシュ等、館内4件では全100展示の対応・到達・選択、衝突、移動、寸法・メッシュのハッシュ・境界等を確認。[モデル8件](TEST-io.github.hatake716.dinobones.ModelTest.xml)、[館内4件](TEST-io.github.hatake716.dinobones.TheaterWorldTest.xml)。
- Lintはエラー0、警告32。従来31件に加え、Bitmapの拡縮をKTXへ置き換える提案が1件。依存更新・SDK等の提案を含みます。[Lint](lint.txt)。
- 図鑑100項目・100背景画・3時代地図・出典・選定データを確認。[図鑑検証](catalog-validation.txt)。
- APK/AABそれぞれの222 assetsが作業ツリーとバイト一致。森林壁画のPNG・ライセンスの同梱、館内用100メッシュ、ネイティブライブラリ4点が従来と同じであることを検査。[梱包検証](packaged-assets.txt)。
- 森林画像は1774 × 887、3,222,853 bytes。生成時のPNGと同梱元が全バイト一致し、C2PAを保持しています。全100館内メッシュのハッシュも出典台帳と一致。[素材の整合性](asset-consistency.txt)。

APKの[署名検証](apk-signature.txt)・[16 KB zipalign](zipalign.txt)、AABの[署名検証](aab-signature.txt)・[bundletool 1.18.3による構造検証](bundle-validation.txt)が成功しています。証明書SHA-256は前版と同じ `179a7b6d654bfd424e2b4c9da7e737fab60c1b383a5beb46b8efe98b3ffa74cb`。jarsignerの自己署名・タイムスタンプ未付与・manifestの格納順序に関する通知はログに保持しています。ファイルサイズとSHA-256は [build-artifacts.json](../../build-artifacts.json)。

## 操作・描画・資源の確認

タスク専用 `emulator-5554` / API 35 / 1080 × 2400 / 2 GB RAM / SwiftShaderを使用。物理端末へ検証用のキー操作やスワイプは送っていません。

instrumentationテスト5件が117.946秒で成功しました。[操作試験](emulator-tests.txt)。

- 5つの展示室で壁画テクスチャの転送、描画フレーム更新、OpenGLエラー0を確認。縦画面→横画面→Activity再生成→縦画面で、壁画の復元と位置・向きの保持を検証しました。
- 2本指でのスティック移動と見回し、右ドラッグの方向、骨格をタップして正しい詳細図鑑を開き元の場所へ戻る操作、バックグラウンドでの描画停止・復帰を確認しました。
- 従来の図鑑一覧への往復、横画面での館内マップ検索・キーボード終了、入口へのリセットを確認しました。
- 6回の連続したActivity再生成が成功。初期のネイティブ割り当て量96,693,360 bytes、1回目84,803,040 bytes、6回目88,532,192 bytesで、各再生成で壁画が残り続ける挙動は認めませんでした。追加テクスチャ6枚分の残存も検出できるよう、許容増分を24 MiBにして検査しました。[再生成メモリ記録](recreation-memory.txt)。
- 個別3D画面でTriceratopsの部位分離、実寸資料、表面画像の再生成と、Stegosaurusの868 mmの実測資料を確認しました。

画面とメモリはソフトウェア描画のエミュレーターでの確認です。実機のGPUメモリ・描画速度の測定値ではありません。

## 署名済みreleaseと物理端末

署名済み1.7.0でUIからティラノサウルスをお気に入りへ登録し、最終1.8.0へ `install --no-incremental -r` で更新しました。起動画面の森林・明るい横画面のヘッダー、天井、館内マップ、ライセンス、Tyrannosaurus・Archaeopteryx・Patagotitan・Triceratopsの展示と一致する図鑑、館内への復帰、個別の精細3D、従来メニューへの往復を確認しました。お気に入りの保持も成功しています。[最終release記録](release-smoke.txt)。

[更新前の横画面](before-landscape.png) と [更新後の横画面](../../screenshots/release-1.8-theater-entrance-landscape.png) は同じ入口の視点で撮影しました。取得時に画像の幅が高さより大きいことも検査しています。[縦画面](../../screenshots/release-1.8-theater-entrance-portrait.png)、[大型骨格と壁画](../../screenshots/release-1.8-theater-patagotitan-landscape.png) も未加工です。[画面に含まれる素材の帰属](../../screenshots/README.md)。

エミュレーターにインストールされたAPKと配布用APKのSHA-256は一致しています。[最終APK/AABのハッシュ](release-final-hashes.json)、[インストール済みAPK](release-installed-sha256.txt)。

Pixel 10a（API 37）を1.7.0 / code 8から1.8.0 / code 9へ `install --no-incremental -r` で更新しました。端末内APKのSHA-256と最終配布物が一致し、MainActivityのコールド起動（719 ms）、現在プロセスの継続、最前面のActivity、現在プロセスのクラッシュ記録が空であることを確認しました。[端末更新記録](device-install.txt)。

物理端末のアンインストール・データ消去、キーやジェスチャーの入力注入は行っていません。お気に入りの保持と実際の操作・描画の確認はエミュレーターで行っています。

## 確認範囲

森林は単一の新規生成画像を壁面に配置した創作の景観です。描画時に2048 × 1024のテクスチャへ補間し、ミップマップと左右の反転繰り返し、透視投影、薄い霞を適用しています。追加する壁画は1テクスチャと28面で、通路の開口は保持します。実在する博物館の写真・壁画を転用していません。

全100種の骨格と寸法の値は前版から変更していません。今回は壁画・照明・上部UIの変更に合わせて5室、画面回転、再生成、操作を重点的に確認しています。前版で実施した全100展示の図鑑巡回を今回も実施したという意味ではありません。実寸・推定寸法・未確定の区別は [館内の仕様](../../THEATER.md) を継続します。

実機での長時間の発熱・電池消費や全機種のGPUは今回の確認範囲に含みません。Google Playへの提出・公開とGitHubへのcommit・pushは行っていません。
