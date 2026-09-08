# 検証記録 — 1.6.0 / versionCode 7

確認日：2026-09-08（JST）。パッケージ：`io.github.hatake716.dinobones`。

## 変更範囲

初期画面を一人称の仮想博物館に変更した。収録100種類を、つながった5室に20種類ずつ展示。スティック移動、2本指での同時見回し、壁・展示台との衝突判定、骨格のタップによる詳細図鑑、館内マップの検索・展示前への移動、従来の図鑑一覧へのショートカットを追加した。詳細図鑑から戻ると館内の位置・向きを保持する。

ロイヤル・ティレル古生物学博物館の大骨格展示を参考にした独自の建物であり、実在館の正確な建築・展示配置の再現ではない。ロゴ・写真・図面を同梱していない。館内モデルの縮尺・出典・ライセンスと操作は [THEATER.md](../../THEATER.md)。従来の個別観察用メッシュ・テクスチャを館内用の軽量データへ置き換えてはいない。以前の検証記録は [1.5.0](../1.5.0/README.md)。

## ビルド・形状・梱包

JDK 17 / Gradle 8.14.3 / SDK 36。debug APK・AndroidTest APK・R8最適化release APK・AABをビルド。[最終ビルド記録](release-build.log)。

- ユニットテスト11件成功。既存8件では全100モデルの座標・部位・識別性、実測資料、4公開メッシュの法線・UV等を確認。追加3件では100展示の一意な対応、全展示の移動先と選択、5室を通る中央通路、壁・展示台の衝突、斜め移動、100館内メッシュのハッシュ・インデックス・法線・境界を検証。[既存8件](TEST-io.github.hatake716.dinobones.ModelTest.xml)、[追加3件](TEST-io.github.hatake716.dinobones.TheaterWorldTest.xml)。
- 館内用メッシュは100点、合計5,181,041三角形、gzip圧縮後112,738,725 bytes。近隣だけを描画し、全モデルを同時にGPUへ載せない。入力・出力のハッシュ、縮尺、元の利用条件を [変換台帳](../../theater-mesh-provenance.json) に記録。
- Lintはエラー0、警告31（依存更新・SDK・KTX等の提案）。[記録](lint.txt)。
- 図鑑100項目・100背景画・3時代地図・出典・選定データを検査。[図鑑検証](catalog-validation.txt)。
- 最終署名済みAPK/AABそれぞれの220 assetsが作業ツリーとバイト一致。館内メッシュが100個であること、ライセンス・manifestの同梱、ネイティブライブラリ4ファイルが1.5.0と同じバイト列であることを確認。[梱包検証とハッシュ](packaged-assets.txt)。

## エミュレーターでの操作

タスク専用 `emulator-5554` / API 35 / 1080 × 2400 / 2 GB RAM / SwiftShader。日常利用端末への入力注入は行っていない。

計11種類のinstrumentationテストが成功。最終の館内4件は546.521秒で完了した。途中の失敗記録も保持し、[最終成功結果の対応表](instrumentation-summary.json) に各テストの採用ログを記載した。

- 館内4件：100種類すべてについて地図検索→展示前へ移動→実際のメッシュ読み込み→対象ボタンの表示→骨格のタップ→一致する学名の詳細図鑑→同じ位置・向きへの復帰を検証。全5室でOpenGLエラー0。右ドラッグで右を向く動作、2本指での歩行と見回し、既存メニューとの往復、横画面の地図検索とキーボードの終了、画面再生成・バックグラウンド停止・再開、6回連続のActivity再生成も確認。[最終館内テスト4件](final-theater-all.txt)、[100展示の記録](all-exhibits.tsv)。
- 既存7件：背景のOFF・濃度・表示方式と状態保存、時代地図の切り替え、白亜紀の透過、図鑑の上下スワイプ、全時代フィルターの保存、検索、Wikipedia/YouTubeリンク、個別モデルを確認。[背景・検索・リンク](final-regressions.txt)、[時代とスワイプ](final-models-and-periods.txt)、[最終個別モデル2件](final-models.txt)。これらの混合ログにある以前のモデル失敗は、最終個別モデルログで解消を確認した。
- 個別観察ではTriceratopsの部位分離と標本資料、Stegosaurusの868 mmの実測資料、Tyrannosaurus・Carnotaurus・Patagotitan・Camarasaurus・Spinosaurus・Velociraptor・Stegosaurus・Brachiosaurus・Ankylosaurus・Pteranodon・Mosasaurusを確認。テクスチャ、拡大、接触陰影、OpenGLエラー0。PatagotitanはActivity再生成・EGLコンテキストの破棄と再作成も含む。

## 検証中に修正した点

画面移動ごとに博物館のGLビューを作り直すとネイティブメモリが増えるため、同じActivity内でビューを保持し、図鑑等を表示中は非表示・停止して再利用する方式へ変更した。さらにActivity再生成時、停止中のEGLコンテキストでGPU資源の削除が実行されず、約32 MBずつ残ることを検出した。GLスレッドの終了前に1×1の一時描画面で同じコンテキストを有効にして削除を完了することで、ネイティブ割り当て量が約59.89 MBから6回の再生成後に約60.20 MBとなり、毎回約32 MBが残る現象の解消を確認した。[最終再生成メモリ記録](recreation-memory.txt)。

館内地図の検索確定時はDialog内のキーボードとフォーカスを閉じるよう修正。従来の「全時代」は画面遷移後も保持するよう状態を上位へ移した。個別モデルのテストはComposeの描画完了を待ってからネイティブの操作を行うよう修正し、実表示の検証条件は保持した。

署名付きreleaseの確認では、非同期で骨格が読み込まれた後、静止したままだと対象ボタンが出ないケースを検出した。読み込み済みの対象をComposeの状態へ反映して更新するよう修正し、全100展示に「詳細図鑑を開く」ボタンの表示検証を追加した。

全100展示の巡回時の11回の計測で、全体PSSの最大値は306,806 KiB、100番目で303,666 KiBだった。5.004秒に118フレームを描画し、骨格キャッシュは20件だった。[100番目のメモリ標本](memory-100.txt) と [描画計測](performance.txt) は、ソフトウェア描画のエミュレーターでの測定である。全機種のフレームレート、実機のピークメモリ、長時間の発熱・電池消費を保証する値ではない。

## 配布物の署名

最終ファイルのサイズとSHA-256は [build-artifacts.json](../../build-artifacts.json)。APKの[署名](apk-signature.txt)と[16 KB zipalign](zipalign.txt)、AABの[署名](aab-signature.txt)と[bundletool 1.18.3検証](bundle-validation.txt)が成功。

証明書SHA-256：`179a7b6d654bfd424e2b4c9da7e737fab60c1b383a5beb46b8efe98b3ffa74cb`。1.5.0と同一。jarsignerの自己署名・タイムスタンプ未付与の通知は残る。

## 最終署名付きreleaseの更新・表示

エミュレーターに署名付き1.5.0をインストールし、UIからティラノサウルスをお気に入りへ登録してから、最終1.6.0へ `install --no-incremental -r` で更新した。シアターの初期表示、館内マップ、出典・ライセンス、Triceratops・Tyrannosaurus・Spinosaurus・Patagotitan・Eoraptorへの移動と一致する詳細図鑑、展示への復帰、Triceratopsの精細観察と資料、お気に入り保持、既存メニューへの往復、横画面、入口へのリセットを確認した。[最終release記録](release-smoke.txt)。

最終APKのSHA-256は `4d28eebc10e4cdacf0fc4ad7fe2eb4675a15829cb5f15c8f8c7384cc4d877a41`。未加工画面は `screenshots/release-1.6-theater-*.png`。[横画面](../../screenshots/release-1.6-theater-landscape.png)、[館内マップ](../../screenshots/release-1.6-theater-map.png)、[精細観察](../../screenshots/release-1.6-theater-individual-skeleton.png)。

## 物理端末

Pixel 10a（API 37）を1.5.0 / code 6から1.6.0 / code 7へ `install --no-incremental -r` で更新した。上記で検証した最終署名付きAPKと端末内APKのSHA-256一致、コールド起動（MainActivity）、現在プロセスの継続、最前面のActivity、現在プロセスのクラッシュ記録が空であることを確認した。[端末更新記録](device-install.txt)。

物理端末のデータ消去・アンインストール、ジェスチャーやキー入力の注入は行っていない。更新時のお気に入り保持と各操作はエミュレーターで検証した。実機での手触りや長時間の散策による発熱は、今回の確認範囲に含まない。

## 確認範囲の限界

館内の骨格は展示台に合わせた縮尺で、実寸比較用ではない。細部の確認には各種の「3D骨格」と「モデルと実寸資料」を使用する。全100種類の実物スキャン・解剖学的監修・全機種のGPU確認は実施していない。建物は独自の仮想空間であり、実在館の正確な再現や公認を意味しない。

Google Playへの提出・審査・公開、およびGitHubへのcommit・pushは本作業では行っていない。
