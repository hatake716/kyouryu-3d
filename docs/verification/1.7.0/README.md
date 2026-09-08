# 検証記録 — 1.7.0 / versionCode 8

確認日：2026-09-08（JST）。パッケージ：`io.github.hatake716.dinobones`。以前の結果は [1.6.0](../1.6.0/README.md) に保存しています。

## 変更範囲

起動画面の見出しを「恐竜の骨格３D」に変更。館内を共通のm単位にし、展示の寸法に応じて展示台・通路・部屋を拡張しました。高さ32 mの屋根、天窓からの採光、窓枠の影、260か所のシダ・木生シダ風の植栽、化石レリーフ、ベンチ、身長1.7 mの比較用シルエットと目盛りを追加。最大移動速度を3.8 m/秒から8 m/秒へ上げ、家具・植栽も衝突判定の対象にしました。

寸法を確認できた公開標本1点と骨長資料を反映した復元18点は元の実寸単位を保持。76点は推定全長・翼開長へ換算し、5点は「縮尺未確定・参考展示」です。全100点の形状・色データと、従来の個別観察用モデルは変更していません。各表示の根拠と限界は [THEATER.md](../../THEATER.md)、全100点の記録は [theater-mesh-provenance.json](../../theater-mesh-provenance.json)。アプリ内にも「館内の縮尺」を追加しました。

## ビルド・単位・梱包

JDK 17 / Gradle 8.14.3 / SDK 36。debug APK・AndroidTest APK・R8最適化release APK・AABをビルド。[ビルド・テスト・Lint](final-build.txt)、[最終アセット反映後のビルド](final-assets-build.txt)、[小型展示の案内板修正後の最終ビルド](final-placard-build.txt)。

- ユニットテスト12件成功。既存8件は全100モデルの座標・部位・識別性、実測資料、4公開メッシュの法線・UV等。館内4件は100展示の対応と移動先・選択、5室を通る中央通路、壁・展示台の衝突、8 m/秒の移動・斜め移動、100メッシュのハッシュ・境界・法線、寸法と展示台・屋根の収容、260植栽を検証。[既存8件](TEST-io.github.hatake716.dinobones.ModelTest.xml)、[館内4件](TEST-io.github.hatake716.dinobones.TheaterWorldTest.xml)。
- 100館内メッシュのSHA-256は1.6.0とすべて一致。頂点や色を作り直さず縮尺・配置を変更しました。manifestと出典台帳が一致し、実寸資料19・推定76・未確定5の分類を検査。[整合性記録](scale-consistency.txt)。
- Lintはエラー0、警告31（SDK・依存更新・KTX等の提案）。[Lint結果](lint.txt)。
- 図鑑100項目・100背景画・3時代地図・出典・選定データを検査。[図鑑検証](catalog-validation.txt)。
- 最終署名済みAPK/AABそれぞれの221 assetsが作業ツリーとバイト一致。館内メッシュ100個と縮尺の説明を含むライセンス資料の同梱を確認。ネイティブライブラリ4ファイルは1.5.0と同じバイト列。[梱包検証](packaged-assets.txt)。

## エミュレーターでの操作

タスク専用 `emulator-5554` / API 35 / 1080 × 2400 / 2 GB RAM / SwiftShader。日常利用端末への入力注入は行っていません。

館内のinstrumentationテスト5件が613.520秒で成功しました。[館内テスト結果](theater-tests.txt)。

- 全100展示：館内マップで検索→展示前へ移動→実際のメッシュ読み込み→対象ボタンの表示→骨格のタップ→一致する学名の詳細図鑑→同じ位置・向きへ復帰。全5室でOpenGLエラー0。[全100展示の記録](all-exhibits.tsv)。
- 右ドラッグで右を向く見回し、2本指での移動と見回し、指を離したときの移動停止、画面再生成後の位置・向きの保持、バックグラウンドでの描画停止と復帰。
- 館内と従来メニューの往復、横画面への回転、横画面の検索とキーボード終了、入口へのリセット。
- Patagotitan・Archaeopteryx・Brachiosaurus・Quetzalcoatlus・Triceratopsの寸法表示と詳細図鑑。小型標本の案内板からも図鑑を開けること。「館内の縮尺」への導線と閉じた後の描画再開。
- Activityを6回連続で再生成。ネイティブ割り当て量は初期96,703,088 bytes、1回目88,301,920 bytes、6回目88,323,136 bytes。各再生成で館内データが残り続ける挙動は認めませんでした。[再生成メモリ記録](recreation-memory.txt)。

全100展示の巡回中に11回採取したPSSの最大値は327,182 KiBで、100番目の標本と同じでした。巡回後の静止画面では5.001秒に109フレーム、キャッシュ20展示でした。[メモリ標本](memory-100.txt)、[描画計測](performance.txt)。ソフトウェア描画のエミュレーターでの観測値です。実機の速度やピークメモリ、長時間の発熱・電池消費を示す値ではありません。

既存機能のinstrumentationテスト3件も94.095秒で成功し、今回の操作試験は計8件が成功しました。Triceratopsの部位分離・実寸資料、Stegosaurusの868 mmの骨長資料、Tyrannosaurus・Carnotaurus・Patagotitanを含む11種類の精細表示と拡大、Patagotitanの画面再生成・EGLコンテキスト再作成を検証。個別観察のOpenGLエラー0、表面画像と接触陰影の再生成を確認しました。背景のOFF・濃度・表示方式と画面再生成後の状態保存も成功しました。[既存機能の最終3件](legacy-tests.txt)。

配布用APKの画像確認で、小型標本の前にある案内板が骨格を隠していることを検出しました。観察台の前面（高さ0.42〜0.82 m）へ下げ、台の上の小さな骨格を遮らないよう修正しました。寸法・配置・移動や選択判定は変更していません。最終版で小型・大型5種類の寸法表示と出典画面のテストを再実施し、36.074秒で成功しました。[修正後のテスト](final-placard-test.txt)。重複を除く計8種類の操作試験の採用ログは [対応表](instrumentation-summary.json) に記録しています。

## 署名・配布物

最終ファイルのサイズとSHA-256は [build-artifacts.json](../../build-artifacts.json)。APKの[署名](apk-signature.txt)と[16 KB zipalign](zipalign.txt)、AABの[署名](aab-signature.txt)と[bundletool 1.18.3検証](bundle-validation.txt)が成功。

証明書SHA-256：`179a7b6d654bfd424e2b4c9da7e737fab60c1b383a5beb46b8efe98b3ffa74cb`。1.6.0と同一です。jarsignerの自己署名・タイムスタンプ未付与、およびZIP内のmanifest順序に関する通知をログに保存しています。署名の検証とbundletoolの構造検証は成功しています。

## 最終releaseと物理端末

エミュレーターへ署名済み1.6.0をインストールし、UIでティラノサウルスをお気に入りへ登録した後、最終1.7.0へ `install --no-incremental -r` で更新しました。起動画面の名称・館内マップ・縮尺の説明・ライセンス、Triceratops・Tyrannosaurus・Patagotitan・Archaeopteryx・Brachiosaurusの展示と一致する図鑑、館内への復帰、Triceratopsの精細3Dと資料、お気に入り保持、従来メニュー、横画面、入口へのリセットを確認しました。[最終release記録](release-smoke.txt)。

小型標本の案内板は台の前面に収まり、骨格を隠さず表示できました。未加工画面は [入口の横画面](../../screenshots/release-1.7-theater-entrance-landscape.png)、[Patagotitanを見上げる画面](../../screenshots/release-1.7-theater-patagotitan-landscape.png)、[Archaeopteryxの観察台](../../screenshots/release-1.7-theater-archaeopteryx.png)、[寸法の根拠](../../screenshots/release-1.7-theater-scale-notes.png)。[画像の帰属](../../screenshots/README.md) を継続しています。

最終APKのSHA-256は `3f04f827f659633fd9e044ea2e8e029a8b174ac8bf1608eb5e2de22f32ddb573`。[APK/AABの最終記録](release-final-hashes.json)。エミュレーターにインストールされたAPKも [同じSHA-256](release-installed-sha256.txt) でした。

Pixel 10a（API 37）を1.6.0 / code 7から1.7.0 / code 8へ `install --no-incremental -r` で更新しました。端末内APKのSHA-256が最終配布物と一致し、MainActivityのコールド起動（665 ms）、現在のプロセスの継続、最前面のActivity、現在プロセスのクラッシュ記録が空であることを確認しました。[端末更新記録](device-install.txt)。

物理端末のデータ消去・アンインストール、ジェスチャーやキー入力の注入は行っていません。更新時のお気に入り保持と実操作・描画の検証はエミュレーターで行いました。実機での手触りや長時間使用の発熱は今回の確認範囲に含みません。

## 確認範囲の限界

全100点が実測標本ではありません。推定全長を頭尾方向の幅へ合わせた表示には、姿勢・曲がった体軸・未計測の骨の比率による近似が残ります。骨長を反映した18点も、未計測部は模式形状です。資料が不足する5種類は実物大と表示しません。各寸法とその区別は「館内の縮尺」に掲載しています。

建物・天窓・植物・化石レリーフは独自制作です。実在館の正確な再現・公認・監修を意味しません。公開メッシュ・背景画の作者と利用条件は継続して表示します。全機種のGPU、解剖学的な専門監修、実機での長時間の散策は今回の確認範囲に含みません。

Google Playへの提出・審査・公開、およびGitHubへのcommit・pushは本作業では行っていません。
