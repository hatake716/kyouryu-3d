> Google Playへ今回登録する縦横8枚は [docs/store/README.md](../store/README.md) にまとめています。このフォルダーは過去の検証記録です。

# 動作画面・素材の帰属

## 1.3.0・以前の版

API 35のエミュレーターで取得した実際のアプリ画面です。署名付き1.3.0の画面は `release-cretaceous.png`、`release-details-swipe.png`、`release-name-origin.png`、`release-discovery-story.png`、`release-research-notes.png`、`release-after-down-swipe.png` です。白亜紀地図の外側の余白、追加本文・出典、詳細図鑑を閉じる操作を確認しました。その他の画像は1.2.0の検証記録として残しています。

背景画はアプリの画面でも元画像の権利を維持します。再配布時には [ARTWORK_CREDITS.md](../ARTWORK_CREDITS.md) の該当行と [artwork-provenance.json](../artwork-provenance.json) の作者・出典・ライセンスを併記してください。表示時の拡大、トリミング、明度調整、UIと3D骨格の重ね合わせがあります。CC BY-SA画像を含む表示の改変部分についても、その画像の継承条件に従います。

- 各属の学名をファイル名とした画像：同名の背景画像の帰属を参照。
- `tyrannosaurus-*`、`release-tyrannosaurus.png`、`release-landscape.png`、`background-*`：Tyrannosaurusの背景（Steveoc 86、CC BY-SA 4.0）。`background-off.png` は濃さ0の確認画面です。
- `release-titanosaurus.png` と `titanosaurus.png`：本アプリ用に生成したTitanosaurusの仮説イメージ。`saurophaganax.png` もAI生成です。[生成記録と提供条件](../GENERATED_ARTWORK.md)。
- `00-collection.png`、`period-cretaceous.png`、`release-cretaceous.png`：白亜紀100 Maの地図。`period-jurassic.png`：ジュラ紀155 Ma。`period-triassic.png`：三畳紀225 Ma。地図はいずれもScotese, Vérard, Burgener, Elling & Kocsis / PALEOMAP、CC BY 4.0。[原本・作者・配布条件](../PALEOMAPS.md)。
- `collection-artwork.png`：ジュラ紀の地図とAllosaurus、Stegosaurus、Archaeopteryx、Brachiosaurusの背景画。各画像の帰属を上記一覧で参照してください。
- `earth-exhibit.png`、`details-from-swipe.png`：Allosaurusの背景画。`release-details-swipe.png`、`release-name-origin.png`、`release-discovery-story.png`、`release-research-notes.png`、`release-after-down-swipe.png`：Tyrannosaurusの背景画（Steveoc 86、CC BY-SA 4.0）。追加した図鑑本文もCC BY-SA 4.0です。
- `licenses-about.png`：説明UI。背後に見える白亜紀の地図も上記のCC BY 4.0です。独自のUI、アイコン、骨格描画はApache 2.0。画面中のWikipedia由来の説明はCC BY-SA 4.0です。

第三者画像やWikipedia由来の文章に、プログラム全体のApache 2.0を一律に適用しないでください。個別条件は [CONTENT_LICENSE.md](../CONTENT_LICENSE.md) を参照してください。

## 1.4.0 骨格と表面

署名付き1.4.0をAPI 35エミュレーター（1080 × 2400）で動作させた未加工キャプチャです。

- `release-specimen-triceratops.png`：Smithsonianの公開標本、前後幅5.89 m、m単位の床格子。
- `release-specimen-surface.png`：公開標本の色・陰影・凹凸を拡大。
- `release-specimen-source.png`：資料の反映範囲と帰属。
- `release-specimen-orbit.png`：別の視点。
- `release-measured-tyrannosaurus.png` と `release-bone-measurements.png`：復元形状と標本の骨長。
- `release-saurolophus.png` と `release-torosaurus.png`：名称の似た項目と取り違えずに開いた追加確認。

背景画の個別ライセンスは `../ARTWORK_CREDITS.md`、3D素材の帰属は `../MODEL_ACCURACY.md` に記載しています。博物館による提携・監修を示す画像ではありません。

## 1.5.0 詳細メッシュと復元形状

`release-1.5-*.png` は署名付き1.5.0のAPI 35エミュレーターでの未加工画面です。

- `tyrannosaurus`：Rigstersの展示レプリカの公開スキャンと色テクスチャ（CC BY 4.0）。
- `carnotaurus`：Magoriusの公開復元モデルと色テクスチャ（CC BY 4.0）。
- `patagotitan`：MebaLab / lautaro.cbの公開復元を部材ごとに軽量化し、アプリの材質・陰影を適用したモデル（CC BY-SA 4.0）。変換後メッシュとその改変の表現にも継承条件を適用。
- `spinosaurus`：アプリ独自の模式復元の形状・表面改善。
- `triceratops`：継続使用するSmithsonianのCC0公開標本。

各名前の後の `-close` は拡大、`-source` はモデルの資料・帰属表示です。3Dモデルの原本リンクと変更内容は [MODEL_ACCURACY.md](../MODEL_ACCURACY.md)、背景画の作者・配布条件は [ARTWORK_CREDITS.md](../ARTWORK_CREDITS.md) を参照してください。1.4.0の `release-measured-tyrannosaurus.png` は以前のモデルの記録で、現在は別のレプリカの公開スキャンに変わっています。

`../verification/1.5.0/all-models-1.jpg`〜`all-models-4.jpg` は最終debugビルドの100種類を25種類ずつ並べた確認用一覧です。各画面の3D表示領域を切り出し、縮小と学名ラベルを追加しています。背景画の各作者・ライセンスと、公開メッシュ4種の帰属・変更条件は上記と同じです。原寸の未加工PNGはローカルの `artifacts/model-upgrade-1.5/final-evidence/evidence/` に保持しています。

## 1.6.0 シアターモード

`release-1.6-theater-*.png` はシアターモードの未加工キャプチャです。建物・展示台・照明・案内板・UIはhatake716の独自制作（Apache 2.0）。ロイヤル・ティレル古生物学博物館の写真や建築図面を合成した画像ではありません。

館内に写るTriceratopsの形状・原色はSmithsonian Institution / National Museum of Natural History（CC0 1.0）。TyrannosaurusはRigsters、CarnotaurusはMagorius（ともにCC BY 4.0）。PatagotitanはMebaLab / lautaro.cb（CC BY-SA 4.0）で、館内用の派生モデルも同条件です。96種の復元は独自制作。元データのURLと変更内容は [館内の資料](../THEATER.md)、[モデル資料](../MODEL_ACCURACY.md) および [全100点の変換台帳](../theater-mesh-provenance.json) を参照してください。実在館の模型写真・建物写真として紹介しないでください。

`release-1.6-theater-individual-skeleton.png` と `release-1.6-theater-collection.png` に含まれる復元背景画の作者・元ファイル・配布条件は [ARTWORK_CREDITS.md](../ARTWORK_CREDITS.md) と [artwork-provenance.json](../artwork-provenance.json) を参照してください。館内の画像と同様に、画面キャプチャ内の第三者素材にも元の条件が適用されます。

`release-1.6-theater-collection.png` の白亜紀100 Maの地図はScotese, Vérard, Burgener, Elling & Kocsis / PALEOMAP（CC BY 4.0）。楕円の外側をマスクしてUIへ合成しています。[原本・変更内容](../PALEOMAPS.md)。`release-1.6-theater-eoraptor-details.png` のWikipedia由来の図鑑本文はWikipedia contributors（CC BY-SA 4.0）。[本文の帰属と変更](../CONTENT_LICENSE.md)。

## 1.7.0 実寸・推定寸法を反映した展示室

`release-1.7-theater-*.png` は署名済み1.7.0をAPI 35エミュレーターで動作させた未加工キャプチャです。`entrance-landscape` は入口の横画面、`patagotitan-landscape` は全長の目安37.5 mに換算したPatagotitanを見上げた画面、`archaeopteryx` は全長の目安0.5 mの小型復元を載せた観察台です。各値は採用した推定値であり、模型全体の実測を意味しません。`scale-notes` は各模型の寸法の根拠・出典を確認する画面です。

天窓・植栽・化石レリーフ・建物・家具・比較用シルエット・照明は独自のプログラムで生成したもの（Apache 2.0）です。特定の博物館の写真や建築図面を使用していません。骨格の公開データ4点と独自復元96点は1.6.0と同じ元データを使い、寸法と館内配置を変更しています。SmithsonianのTriceratopsはCC0、RigstersのTyrannosaurusとMagoriusのCarnotaurusはCC BY 4.0、MebaLab / lautaro.cbのPatagotitanおよびその派生モデルはCC BY-SA 4.0です。画面内のこれらの表現にも元の条件が適用されます。[各データの帰属・元URL・寸法変更](../THEATER.md)。

`individual-skeleton` と `collection` に写る復元背景画は [ARTWORK_CREDITS.md](../ARTWORK_CREDITS.md)、白亜紀の地図はScoteseほか / PALEOMAP（CC BY 4.0、[変更・帰属](../PALEOMAPS.md)）を参照してください。`*-details` のWikipedia由来の本文はWikipedia contributors（CC BY-SA 4.0）、[本文の帰属](../CONTENT_LICENSE.md)を継続します。

## 1.8.0 森林の壁画と明るい横画面

`release-1.8-theater-*.png` は署名済み1.8.0のAPI 35エミュレーターで撮影した未加工画面です。`entrance-landscape` が横画面の入口、`entrance-portrait` が縦画面、`patagotitan-landscape` が大型骨格の展示です。

壁面の森林は本アプリ用に組み込みのOpenAI image_genで新規生成した創作画です。特定の年代・地域の植生を確定した科学的復元ではありません。第三者の壁画や写真を参照せず、hatake716が権利を有する範囲でApache 2.0として提供します。[元画像・生成プロンプト・利用条件](../forest-mural-provenance.json)。天井・天窓・植物・家具・案内表示は独自制作（Apache 2.0）です。

骨格の公開モデル4点は従来のCC0 / CC BY 4.0 / CC BY-SA 4.0条件を継続します。SmithsonianのTriceratopsはCC0、RigstersのTyrannosaurusとMagoriusのCarnotaurusはCC BY 4.0、MebaLab / lautaro.cbのPatagotitanとその派生表現はCC BY-SA 4.0です。[モデルの帰属](../MODEL_ACCURACY.md)。`individual-skeleton` と `collection` の背景画は [ARTWORK_CREDITS.md](../ARTWORK_CREDITS.md)、白亜紀の地図はScoteseほか / PALEOMAP（CC BY 4.0、[変更・帰属](../PALEOMAPS.md)）を参照してください。


## 1.9.0 復元イラストの立て札と横画面

`release-1.9-*` は署名済み1.9.0をAPI 35エミュレーターで実行した未加工のキャプチャです。縦画面は1080 × 2400、横画面は2400 × 1080です。

- `theater-entrance-*`：各展示の名前札の左隣に復元イラスト。見える画像・骨格は [ARTWORK_CREDITS.md](../ARTWORK_CREDITS.md)、[MODEL_ACCURACY.md](../MODEL_ACCURACY.md)、[theater-mesh-provenance.json](../theater-mesh-provenance.json) の該当種の帰属・ライセンスを参照してください。森林壁画は本アプリ用AI生成画で、[生成記録](../forest-mural-provenance.json)の条件です。
- `illustration-landscape` / `illustration-source`：Triceratopsの同梱復元画を画像全体で表示し、作者・出典を確認する画面。画像の作者と配布条件は上記のTriceratopsの項目を参照。余白・UIを組み合わせた表示です。
- `skeleton-landscape-triceratops` / `patagotitan` / `pteranodon`：横画面の操作欄を右側へ移し、初期カメラを調整した3D画面。同名の骨格資料・復元画それぞれの帰属と配布条件を維持します。

画像のライセンスをアプリ全体のライセンスへ変更するものではありません。各画面の第三者素材の再配布では該当する作者・出典・条件を併記してください。

## 1.10.0 種類ごとの化石表面

`release-1.10-*.png` は署名済み1.10.0のAPI 35エミュレーターの未加工画面です。`hall` は館内、`skeleton-landscape` は横画面の初期視点、`surface-close` は拡大表示、`surface-note` はモデルの資料欄です。骨格の新しい色・ひび・土は独自の表現で、発掘標本の写真・測定色ではありません。

Spinosaurus・Velociraptor・Brachiosaurusの骨格形状と表面生成は独自制作。PatagotitanはMebaLab / lautaro.cbのモデルへ独自表面を適用した派生表現（CC BY-SA 4.0）、TyrannosaurusはRigstersの公開スキャン（CC BY 4.0）、TriceratopsはSmithsonian（CC0）です。背景・立て札の復元画の作者・出典・条件は [ARTWORK_CREDITS.md](../ARTWORK_CREDITS.md)、骨格の詳細は [MODEL_ACCURACY.md](../MODEL_ACCURACY.md) を参照してください。森林壁画は既存のアプリ用生成画です。

## 1.10.1 全画面表示と透過タイトル

`release-1.10.1-*.png` は最終署名済み1.10.1をAPI 35エミュレーターで撮影した未加工キャプチャです。横画面2400 × 1080では通知バーとナビゲーションバーを隠し、`theater-portrait`（1080 × 2400）では復帰した両バーを確認できます。

森林壁画・建築・UI・骨格の素材条件は上記1.10.0と同じです。`illustration` と `illustration-source` に写る復元画はNobu TamuraのTriceratops BW.jpg（CC BY 2.5）。[原本・作者と帰属](../ARTWORK_CREDITS.md)、[骨格モデルの条件](../MODEL_ACCURACY.md)を参照してください。スクリーンショット中の第三者素材にも元のライセンスが適用されます。
