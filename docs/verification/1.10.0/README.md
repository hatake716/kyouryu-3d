# 検証記録 — 1.10.0 / versionCode 11

確認日：2026-09-08（JST）。パッケージ：`io.github.hatake716.dinobones`。前版は [1.9.0](../../verification/1.9.0/README.md)。

## 変更範囲

独自復元96種類と表面画像を持たないパタゴティタンの計97モデルに、種類別の色調・鉱物斑・風化・細いひび・付着土・粗面の表現を追加しました。個別3Dと館内で同じ表面設定を使い、一覧の骨格サムネイルにも色調を反映します。トリケラトプス・ティラノサウルス・カルノタウルスの元の表面画像・館内頂点色を保持します。

「モデルと実寸資料」、アプリ内案内、READMEと配布説明を更新。表面は独自の観察用表現で、特定標本・産地の保存色の測定結果ではないことを明示しています。[仕様と適用範囲](../../FOSSIL_SURFACES.md)。第三者の写真・テクスチャは追加していません。パタゴティタンの元モデルの帰属とCC BY-SA 4.0条件を継続します。

## ビルド・データ・配布形式

[release APK/AABのビルド](../../verification/1.10.0/build-release.txt)が成功。JDK 17、Gradle 8.14.3、SDK 36を使用しました。

- ユニットテスト16件成功。モデル8件・館内5件・カメラ1件に、種別表面とプレビュー整合性の2件を追加。97種の色・模様の違い、再取得時の安定性、3種の元テクスチャの扱い、計測値・形状・初期フレーミングを確認。
- [最終Lint](../../verification/1.10.0/lint-build.txt)：エラー0、警告33。[結果XML](../../verification/1.10.0/lint-results.xml)。
- [APK/AAB内容検証](../../verification/1.10.0/package-content.txt)：各222 assetsが作業ツリーとバイト一致、100館内メッシュを同梱。ネイティブライブラリ4点は1.5.0以降と同一。
- [原資料の比較](../../verification/1.10.0/source-assets-unchanged.txt)：213点のモデル・館内メッシュ・画像assetsが1.9.0とバイト一致。館内の実寸換算と展示配置は変更していません。
- [カタログ検証](../../verification/1.10.0/catalog-check.txt)：100項目・100背景画・3地図・100出典記録と選定データの整合性を確認。
- [APK署名](../../verification/1.10.0/apk-signature.txt)、[16 KB zipalign](../../verification/1.10.0/zipalign.txt)、[AAB署名](../../verification/1.10.0/aab-signature.txt)、[bundletool](../../verification/1.10.0/bundle-validation.txt)が成功。署名証明書SHA-256は `179a7b6d654bfd424e2b4c9da7e737fab60c1b383a5beb46b8efe98b3ffa74cb`。jarsignerの自己署名・タイムスタンプ等の通知をログに保持。
- [ファイルのサイズとSHA-256](../../build-artifacts.json)。

## エミュレーターでの描画と操作

API 35、1080 × 2400 / 2400 × 1080、SwiftShaderを使用。8件の異なるinstrumentationテストが成功しました。

[新規試験3件](../../verification/1.10.0/new-ui-tests.txt)は130.863秒で成功しました。

1. 独立したEGL面で共通シェーダーをコンパイル・描画。97種類の画像ハッシュが別々で、同じ種を再描画すると画素が一致することを確認。微分拡張を使う経路と使わないES 2.0フォールバックの両方で検査しました。
2. Spinosaurus、Velociraptor、Brachiosaurus、Stegosaurus、Ankylosaurus、Archaeopteryx、Mosasaurus、Patagotitan、Triceratops、Tyrannosaurus、Carnotaurusの11種類を個別描画。種別の設定、元の表面画像の読み込み、拡大、回転、Activity再生成後のカメラと材質保持を確認しました。
3. 館内全100展示へ移動し、97種の新表面と3種の元資料の表面を確認。全展示でOpenGLエラー0、読み込み失敗なし。[全100展示の記録](../../verification/1.10.0/fossil-tour.tsv)。CPU描画提出時間はSwiftShader上の参考値で、実機のFPSではありません。

一部の初回自動撮影が描画待ちの表示を含んだため、撮影手順を「読み込み表示の終了と次のGLフレームを待つ」方式へ修正しました。[再確認と回帰試験5件](../../verification/1.10.0/regression-tests.txt)は191.244秒で成功。上記の11種類を再確認し、以下4件も成功しています。

- 7種類の縦横表示、横画面で隠れない表示領域、全画面往復、回転・拡大・リセット、画面再生成、詳細図鑑の上下スワイプ。
- トリケラトプスの原表面・部位単独表示・資料、ステゴサウルスの868 mmの計測資料。
- 背景の有無・濃さ・画像全体表示と視点の保持。
- 館内を6回再生成した後のネイティブ確保量の増分が24 MiB未満であること。[記録](../../verification/1.10.0/recreation-memory.txt)。連続instrumentation試験内の値で、独立起動時や実機の絶対使用量とは比較しません。

## 個別3D画面の資源解放

最初の署名済み版を連続操作した確認ではネイティブ確保量が増えたため、個別3DのViewを閉じる際にGPUバッファ、背景・表面画像、陰影用FBO、シェーダーを明示的に解放しました。既に一時停止中の場合は同じEGLコンテキストへ小さな作業面を接続して削除し、描画スレッド終了前に完了を待ちます。

[追加の往復試験](../../verification/1.10.0/viewer-memory-test.txt)で通常表示と全画面表示を10回往復し、古い画面の資源解放・カメラ・材質保持を確認しました。[確保量の記録](../../verification/1.10.0/viewer-recreation-memory.txt)は1回目109,987,408 bytes、2回目107,636,320 bytes、10回目108,409,744 bytes。2回目からの増加は約0.74 MiBでした。SwiftShaderで同じスピノサウルスを往復した測定であり、端末全体のメモリ削減率ではありません。

[修正後の再検証](../../verification/1.10.0/cleanup-regression-tests.txt)でも11種類の材質・7種類の縦横表示とジェスチャー・元テクスチャと部位分離・背景設定の4試験を再実行し、174.332秒で成功しました。表面の見た目やモデルデータは変えていません。

## 最終署名済みAPK

[1.9.0で登録したお気に入り](../../verification/1.10.0/release-baseline.txt)を保持したまま1.10.0へ上書き更新しました。[release操作記録](../../verification/1.10.0/release-smoke.txt)。立て札の画面上のタップからトリケラトプスの復元イラストと作者・出典を開き、画像の縦横表示と館内復帰を確認しました。

Spinosaurus、Velociraptor、Brachiosaurus、Patagotitan、Tyrannosaurusの5種類で館内・個別3Dの縦横表示・館内復帰を確認。新表面の4種類では資料欄と全画面での拡大も確認し、従来の図鑑一覧への往復も成功しました。取得した横画面PNGは幅が高さを超えることを検査しています。[現在プロセスのクラッシュ記録](../../verification/1.10.0/release-crash-buffer.txt)は空でした。[署名済み版の起動後メモリ](../../verification/1.10.0/release-memory-start.txt)と[操作後メモリ](../../verification/1.10.0/release-memory-end.txt)を別途保存しています。

[変更前のスピノサウルス](../../verification/1.10.0/before-hall-spinosaurus.png)、[変更後の館内](../../screenshots/release-1.10-hall-spinosaurus.png)、[スピノサウルスの拡大](../../screenshots/release-1.10-surface-close-spinosaurus.png)、[ヴェロキラプトルの拡大](../../screenshots/release-1.10-surface-close-velociraptor.png)、[ブラキオサウルスの拡大](../../screenshots/release-1.10-surface-close-brachiosaurus.png)。未加工の画面です。素材の帰属は [画面説明](../../screenshots/README.md) を参照してください。

## 端末への更新

接続中のPixel 10a（API 37）を1.9.0 / code 10から1.10.0 / code 11へ `install --no-incremental -r` で更新しました。端末内APKのSHA-256がエミュレーターで試験した配布物と一致し、通常起動がStatus: ok、MainActivityが最前面、現在プロセスのクラッシュ記録が空であることを確認しています。[実機更新記録](../../verification/1.10.0/device-install.txt)。アプリデータの削除・アンインストール・実機へのタップやキー・ジェスチャー注入は行っていません。見た目と操作はエミュレーターで検証し、実機では更新・同一APK・通常起動を確認しました。
