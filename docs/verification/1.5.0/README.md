# 検証記録 — 1.5.0 / versionCode 6

確認日：2026-09-08（JST）。パッケージ：`io.github.hatake716.dinobones`。

## 変更と確認範囲

既存のトリケラトプスに加え、ティラノサウルス、カルノタウルス、パタゴティタンの詳細な公開メッシュを同梱。残る96種類では頭骨の開口部・厚み、上下の歯列、骨端の隆起と溝、頸椎、血道弓、骨板、表面と接触陰影を改良した。コレクションも同じ三角形メッシュから描画する。

実寸換算を確認した公開標本はトリケラトプス。他3種類のメッシュは換算未確認。生成96種類のうち18種類には論文の骨長を反映し、78種類は実測によらない模式復元。全100種類の実物スキャンや、各種の同じ解剖学的精度を意味しない。詳細な帰属・反映範囲は [MODEL_ACCURACY.md](MODEL_ACCURACY.md)。以前の検証記録は [1.4.0](verification/1.4.0/README.md) と `verification/1.3.0/` に保存した。

## ビルド・梱包・形状

JDK 17 / Gradle 8.14.3 / SDK 36。debug・AndroidTest・R8最適化release APK・AABをビルド。[ビルド記録](verification/1.5.0/build.txt)、[release記録](verification/1.5.0/release-build.txt)。

- ユニットテスト8件成功。100モデルの有限座標・部位・境界・識別性、図鑑と出典、カメラ、選択した実測骨長、4公開メッシュの三角形数・法線・UV・実寸換算の有無を検証。[結果](verification/1.5.0/unit-tests.xml)。
- 描画三角形数：Triceratops 149,998、Tyrannosaurus 250,240、Carnotaurus 260,854、Patagotitan 350,179。Tyrannosaurusはアーカイブ版の全三角形を使用。Patagotitanは骨の部材ごとに軽量化した。
- 計測表19行のうち、CM 9380の値は別のTyrannosaurusレプリカに適用しない。現在の計測反映は18種類。Triceratopsと合わせ19種類でm単位の格子を表示。
- Lintはエラー0、警告27。依存更新・SDK・KTXなどの提案。[記録](verification/1.5.0/lint.txt)。
- 100項目・背景画像・3時代の地図・出典を検査。[図鑑検証](verification/1.5.0/catalog-validation.txt)。
- 署名済みAPK/AABの全assetsと作業ツリーのバイト一致を検証。[梱包検証](verification/1.5.0/packaged-assets.txt)。原資料・変換後データのSHA-256とライセンスは各provenance JSONに記録。

## メモリ不足の修正

初回の連続表示でPatagotitanのgzip展開がJavaヒープ上限192 MBに達した。[失敗記録](verification/1.5.0/initial-memory-failure.txt)。全展開データを複製する読み込みを64 KB単位のストリーム展開に変更し、メッシュキャッシュを32 MB以内に制限、次の生成前に16 MBまで縮小する。大きな骨格がまだ画面やサムネイルに保持されている場合は弱参照で共有する。GPUへは256 KB単位で転送し、サムネイルの重い描画処理を直列化した。全頂点から最下点・部位を調べる際の巨大なボックス化リストも除去。`largeHeap`は使用していない。

その後、48番目のCamarasaurus生成でも容量を拡張した配列と完成配列の重複によるヒープ不足を確認した。[記録](verification/1.5.0/initial-builder-memory-failure.txt)。生成処理を頂点数の事前計数と正確な容量の一度だけの確保に変更し、完成時の巨大配列コピーを廃止した。頂点や骨の部材数は削減していない。

背景テストは別のGL Surfaceとダイアログの合成完了を待つよう修正し、背景OFFの画素が元の土色と一致するという厳密な条件は保持した。実測資料テストのステゴサウルス検索失敗は、画面再生成後のpagerの一時的なページ復元を実際の移動と扱い、「全時代」を解除することが原因だった。絞り込み解除をユーザーの地図ドラッグまたは時代ボタン操作だけに限定し、画面復元で保存状態が変わらないように修正した。[失敗時の記録](verification/1.5.0/filter-restoration-failure.txt)。

## エミュレーター

`emulator-5554` / API 35 / 1080 × 2400 / Javaヒープ上限192 MB。

修正後の重点3テストが成功（128.41秒）。[記録](verification/1.5.0/focused-instrumentation.txt)。

- 背景OFFの画素、濃度・画像全体/拡大表示、視点と背景設定のActivity再生成後の保持。
- Triceratopsの初回表示、部位分離、別視点、テクスチャ再アップロードと実寸資料、Stegosaurusの868 mmの資料。
- Tyrannosaurus、Carnotaurus、Patagotitan、Camarasaurus、Spinosaurus、Velociraptor、Stegosaurus、Brachiosaurus、Ankylosaurus、Pteranodon、Mosasaurusの連続表示と拡大。テクスチャ、接触陰影、頂点数、出典UI、OpenGLエラー0を確認。PatagotitanはActivity再生成とEGLコンテキスト破棄・再作成後の接触陰影・描画・GLエラー0も検証。

100種類の原寸画面を保存し、25種類ずつの確認用一覧で全体を目視確認した。[1〜25](verification/1.5.0/all-models-1.jpg)、[26〜50](verification/1.5.0/all-models-2.jpg)、[51〜75](verification/1.5.0/all-models-3.jpg)、[76〜100](verification/1.5.0/all-models-4.jpg)。画像・モデルの個別ライセンスは [画面の帰属案内](screenshots/README.md) を参照。

全100項目の連続表示、地図・図鑑の上下スワイプ2件、外部リンク、検索の5テストが成功（624.68秒）。[記録](verification/1.5.0/catalog-instrumentation.txt)。各カードの日本語名を完全一致で選択し、表示中の学名も一致することを確認した。左右回転、ピンチ拡大、2本指移動、部位選択、全画面、お気に入りと視点の保持、Activity再生成、全時代フィルターの保持を含む。Camarasaurusを含め100種類が同じプロセスでメモリ不足なく表示された。

## 配布物の署名

[ファイル名・サイズ・SHA-256](build-artifacts.json)。APKの[署名検証](verification/1.5.0/apk-signature.txt)と[16 KB zipalign](verification/1.5.0/zipalign.txt)、AABの[署名検証](verification/1.5.0/aab-signature.txt)と[bundletool 1.18.3検証](verification/1.5.0/bundle-validation.txt)が成功。

証明書SHA-256：`179a7b6d654bfd424e2b4c9da7e737fab60c1b383a5beb46b8efe98b3ffa74cb`。前版と同一。AABのjarsigner通知には自己署名証明書・タイムスタンプ未付与などを含む。

## 署名付きreleaseの更新・表示

エミュレーターの署名付き1.4.0でお気に入りを登録し、最終1.5.0へ `install --no-incremental -r` で更新。お気に入り保持、コールド起動、追加3モデル・Spinosaurus・Triceratopsの初回表示と拡大、作者の資料画面、回転、図鑑からの下スワイプ復帰を確認した。[記録](verification/1.5.0/release-smoke.txt)。

最終APKのSHA-256は `589cf037349ccb51e6c8eaa04080c7565722c549198c6744fadcb9624d9398f1`。画面の未加工キャプチャは `screenshots/release-1.5-*.png`。

## 物理端末

Pixel 10a（API 37）を1.4.0 / code 5から1.5.0 / code 6へ `install --no-incremental -r` で更新した。上記エミュレーターで検証した最終APKと端末内APKのSHA-256一致、最前面のMainActivity、現在プロセスの致命的例外がないことを確認。[端末更新記録](verification/1.5.0/device-install.txt)。

端末のデータ消去・アンインストール、ジェスチャーやキー入力の注入は行っていない。実際の操作感はユーザーの端末操作による確認範囲で、こちらのジェスチャー・描画検証はエミュレーターで実施した。

## 残る限界

全100種類の精密な標本スキャン、専門家による解剖学的監修、全GPU/全機種の試験、長時間の発熱・電力測定は実施していない。実測骨長の一致は全身の解剖学的正確さを保証しない。未対応GPUでは法線マップ・接触陰影にフォールバックがあるが、その非対応端末での実機検証は未実施。

Google Playへの提出・審査・公開は行っていない。AABはローカルの提出用成果物。
