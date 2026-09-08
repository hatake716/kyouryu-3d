# 検証記録 — 1.9.0 / versionCode 10

確認日：2026-09-08（JST）。パッケージ：`io.github.hatake716.dinobones`。仕様は [THEATER.md](../../THEATER.md)。前版の記録は [1.8.0](../1.8.0/README.md)。

## 変更範囲

100種類の各展示に復元イラストの立て札を追加しました。名前札の左隣、展示の前後に計200台を配置し、両面で表示・タップできます。タップすると同梱の復元画を画像全体の表示で開き、拡大・移動・リセット、作者・出典・ライセンス確認に対応します。閲覧中は館内の描画と歩行を停止し、閉じると同じ位置へ戻ります。

個別3D画面は横画面の操作欄を右側へ移動し、GLの表示領域を見出しやカードで隠さない構成にしました。縦画面も骨格と操作欄を分離。各軸を8分割した占有セルの境界で透視投影の距離を求め、骨格の形・縦横比・視点に合わせて全身を大きく表示します。館内骨格のm単位の縮尺、100種類の原メッシュ、復元画の原本は維持しています。

## ビルド・梱包

JDK 17 / Gradle 8.14.3 / SDK 36。[release APK・AABのビルドとユニットテスト](build-release.txt)が成功しました。

- ユニットテスト14件成功。既存モデル8件、館内5件、カメラ1件。館内試験は100種の前後200台への到達・画像用タップと骨格用タップの区別、各画像の原本と帰属情報も検査。カメラ試験では公開標本・大型竜脚類・翼竜・小型種・海生種の7モデルについて、3縦横比と6視点で全頂点が初期の投影範囲内に収まること、表示が小さすぎないことを確認しました。
- [Lint](lint-results.xml)：エラー0、警告33。スタイル・API更新等の提案を含みます。
- [APK/AABの内容検証](package-content.txt)：各222 assetsが作業ツリーとバイト一致。館内メッシュ100点、原画像と帰属情報を同梱。ネイティブライブラリ4点は1.5.0以降と同一です。
- [APK署名](apk-signature.txt)、[16 KB zipalign](zipalign.txt)、[AAB署名](aab-signature.txt)、[bundletoolによる構造検証](bundle-validation.txt)が成功。署名証明書SHA-256は `179a7b6d654bfd424e2b4c9da7e737fab60c1b383a5beb46b8efe98b3ffa74cb`。jarsignerの自己署名・タイムスタンプ等の通知はログに保持しています。
- ファイルの大きさとSHA-256は [build-artifacts.json](../../build-artifacts.json)。

## エミュレーターでの操作・描画

API 35、1080 × 2400 / 2400 × 1080、SwiftShaderを使用しました。以下8件の異なるinstrumentationテストについて成功を確認しています。

[新機能2件](new-ui-tests.txt)は289.428秒で成功しました。

1. 全100種類の立て札を画面のタップで選択し、正しい種名の復元画の読み込みと同じ館内位置への復帰を確認しました。[100種類の結果](illustration-tour.txt)。5展示室で作者表示を確認し、ピンチ、リセット、画面回転、Activity再生成、閉じる操作も試験しました。閲覧中の館内描画停止とOpenGLエラー0を確認しています。
2. Triceratops、Tyrannosaurus、Patagotitan、Brachiosaurus、Pteranodon、Mosasaurus、Archaeopteryxを縦横で描画しました。横画面でGL領域が画面高の65%以上あり、ネイティブViewと実際の表示境界が一致することを検査。回転・拡大、全画面への往復とActivity再生成後のカメラ保持、リセット、カードの上スワイプと図鑑の下スワイプを確認しました。

既存操作6件のうち5件は [初回の回帰試験](regression-tests-initial.txt)で成功しました。館内移動と同時見回し、骨格から図鑑への往復、全5室の森林描画、横画面での地図検索と従来メニュー、Triceratopsの部位分離・表面テクスチャ・実寸資料、Stegosaurusの868 mmの資料を確認しています。

背景設定の試験では、固定した縦画面座標でカードを探す古い手順が失敗しました。実際のカードへスクロールする方法に更新し、Composeのダイアログが閉じた後に画面の描画完了を待つよう検証手順も修正しました。[修正後の背景設定試験](background-test.txt)は15.018秒で成功。背景を消した際の画素色、濃さ、画像全体・画面に広げる設定、カメラ状態と再生成後の保持を確認しました。

[6回の再生成メモリ記録](recreation-memory.txt)：初期97,915,776 bytes、1回目86,070,144 bytes、6回目88,533,216 bytes。許容増分24 MiBの検査が成功し、新しい画像アトラスが再生成のたびに残り続ける挙動は認めませんでした。物理端末でのGPU性能・メモリ計測ではありません。

## 署名済みAPKの更新と端末確認

署名済み1.8.0でティラノサウルスをお気に入りへ登録し、最終1.9.0へデータを消さずに更新しました。館内の立て札を実際にタップしてTriceratopsの復元画・作者・出典を確認し、画像を横画面でも表示。Tyrannosaurus、Triceratops、Patagotitan、Pteranodon、Archaeopteryxの個別3Dを縦横で確認し、各展示への復帰と従来の図鑑一覧への往復も成功しました。お気に入りの保持と現在プロセスのクラッシュ記録が空であることを確認しています。[releaseの操作記録](release-smoke.txt)、[インストール済みAPKのSHA-256](release-installed-sha256.txt)。

[変更前の横画面](before-landscape.png) と [1.9.0の横画面](../../screenshots/release-1.9-skeleton-landscape-triceratops.png) はTriceratopsの初期視点です。[立て札の配置](../../screenshots/release-1.9-theater-entrance-portrait.png)、[復元画の閲覧](../../screenshots/release-1.9-illustration-landscape.png)、[作者・出典](../../screenshots/release-1.9-illustration-source.png) も最終署名済みAPKの画面です。横画面のキャプチャは幅が高さより大きいことを取得時に検査しました。

Pixel 10a（API 37）を1.8.0 / code 9から1.9.0 / code 10へ `install --no-incremental -r` で更新しました。端末内APKのSHA-256がエミュレーターで試験した配布物と一致し、通常起動のStatusがok、MainActivityが最前面、現在プロセスのクラッシュ記録が空であることを確認しました。起動コマンドのWaitTimeは577 msです。[実機更新記録](device-install.txt)。実機のアプリデータの削除・アンインストール・キーやジェスチャーの注入は行っていません。操作と描画の検証はエミュレーターで実施しました。

画面例の素材の帰属は [スクリーンショットの説明](../../screenshots/README.md) およびアプリ内の「作者・出典」「背景と出典」「モデルと実寸資料」にあります。
