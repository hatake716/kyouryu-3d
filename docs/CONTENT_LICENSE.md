# 出典・権利の記録（1.10.1）

## コードと骨格

独自の座標・立体生成処理・表面シェーダー・アイコンはApache License 2.0です。

トリケラトプスのメッシュと3点の表面テクスチャは **Smithsonian Institution / National Museum of Natural History、CC0 1.0**。原GLBを回転・中心化・表示縮尺変換し、部位の簡易領域を追加しました。テクスチャの同梱ファイルは未改変です。

18種類の表示モデルに使う骨長は **Persons・Currie（2016）表1、およびMaidment・Brassey・Barrett（2015）表3、CC BY 4.0** の数値を選択・単位換算して立体に反映しています。元論文の著者、題名、DOI、ライセンス、変更をアプリにも記載します。

[MODEL_ACCURACY.md](MODEL_ACCURACY.md) に帰属・原資料・適用範囲、[specimen-provenance.json](specimen-provenance.json) と [bone-measurements.json](bone-measurements.json) にハッシュ・標本番号を保存しています。残る78種類は教育用の模式復元です。100個の実物標本を精密に再現した模型ではありません。

### 1.5.0で追加した詳細3Dモデル

- Rigsters, *Tyrannosaurus Rex skeleton*, [作者の公開ページ](https://sketchfab.com/3d-models/tyrannosaurus-rex-skeleton-77e21c4e5d65468bbe11780ae0557b95), CC BY 4.0。展示レプリカの撮影データ。三角形と原色画像を保持し、表示用変換と簡易部位を追加。[取得と変換の記録](tyrannosaurus-provenance.json)。
- Magorius, *Carnotaurus sastrei (skeleton)*, [作者の公開ページ](https://sketchfab.com/3d-models/carnotaurus-sastrei-skeleton-04c8d6e0e1794091aeae0d2f54cca43b), CC BY 4.0。作者の復元。形状・UV・原色画像を使い、法線・表示縮尺・簡易部位・陰影を調整。[取得と変換の記録](carnotaurus-provenance.json)。
- MebaLab / lautaro.cb, *Patagotitan mayorum by MebaLab*, [作者の公開ページ](https://sketchfab.com/3d-models/patagotitan-mayorum-by-mebalab-a48cde44e8a045d2968fca28ce62c7ae), **CC BY-SA 4.0**。部材ごとの軽量化、法線・向き・表示縮尺・簡易部位を調整し、不透明な骨の質感へ変更。**変換後の `app/src/main/assets/specimens/patagotitan.ksmz` も CC BY-SA 4.0**。独自コードのApache 2.0をこのメッシュに適用しない。[取得と変換の記録](patagotitan-provenance.json)。

3点は [AllenAIのObjaverse公開アーカイブ](https://huggingface.co/datasets/allenai/objaverse)から取得。現行の作者側Sketchfab APIで素材ごとのライセンスも確認した。データセット全体のライセンスを素材の許諾の代わりにしていない。原本と変更内容、作者・配布条件はアプリのライセンスにも記載。実寸未確認の3点にメートル目盛りを付けない。

## 図鑑の本文

100項目、注記を含む1,327節。Wikipedia contributorsに帰属する**CC BY-SA 4.0**の抄録・翻案を含みます。元の13項目は英語版からの日本語翻案、追加87項目は主に日本語版の本文抄録と英語版から整理した基本カードです。画像、表、引用番号、外部リンク節などを省略し、節の整理・基本カード・更新の注記を加えています。参考文献や各説の詳細は原文の固定版へ案内します。

全記事の固定版ID、取得したHTMLのハッシュ、編集履歴への情報は [source-provenance.json](source-provenance.json)。各項目のアプリ画面にも記事名・執筆者の履歴・固定版・変更の説明・[CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/deed.ja)へのリンクを表示します。抄録・翻案を再利用する場合は表示・継承条件に従ってください。

今回の独自増補も含め、図鑑本文全体をCC BY-SA 4.0で提供します。独自増補の帰属はhatake716です。[500節の読みものと34節の研究ノート](RESEARCH.md)に確認範囲と書誌を記録します。

博物館資料や論文は科学的事実の確認・補足に使います。その本文や図版を、Wikipedia由来の文章や背景画像の条件と混同しません。新しい分類・年代についての注記は、個別に研究へのリンクを持ちます。

## 背景画像

[画像100点のクレジット一覧](ARTWORK_CREDITS.md)から、人が読みやすい表でも確認できます。

- **98点：Wikimedia Commonsの個別ファイルで商用再配布できる条件を確認した復元画。** CC BY、CC BY-SA、Public domain等。ファイルごとの作者、原本、ライセンスURL、取得日、画像ハッシュ、元の説明を [artwork-provenance.json](artwork-provenance.json) に保存しています。
- **2点：本アプリ用にOpenAI image_genで生成した画像。** ティタノサウルスとサウロファガナクス。仮説的な全身像である旨を常時画面に表示します。生成プロンプト・対象ファイルは [GENERATED_ARTWORK.md](GENERATED_ARTWORK.md)。権利を有する範囲でApache 2.0として提供します。独占権や科学的な正確性を保証する表現はしません。

Commons由来の画像はサーバーが提供するサムネイルをそのまま保存しています。画面表示時に拡大・トリミング・明度調整を行います。コレクションの骨格カードにも同じ画像を表示します。画像ごとの「背景と出典」画面で作者と配布条件、原本を確認できます。CC BY-SA画像とその改変物を再配布する場合は、それぞれの継承条件に従ってください。コードのApache 2.0をこれらの第三者画像に適用しません。

Commons全体が一律の画像ライセンスなのではありません。[Commonsの再利用案内](https://commons.wikimedia.org/wiki/Commons:Reusing_content_outside_Wikimedia)に基づき、各ファイルの情報を確認しています。作者はアップロード者とは別に記録します。写真に写った第三者の彫刻・模型の権利が不明なものや、商用利用・再配布を許可しない素材は採用していません。

## 古地理の世界地図

三畳紀225 Ma、ジュラ紀155 Ma、白亜紀100 Maの3点は、Scotese, Christopher R.; Vérard, Christian; Burgener, Landon; Elling, Reece P.; Kocsis, Ádám T. によるPALEOMAP資料（2024）、**CC BY 4.0**です。[帰属・原本・研究資料](PALEOMAPS.md)と[paleomap-provenance.json](paleomap-provenance.json)に固定版とハッシュを保存しています。画像は縮小版を未加工で同梱し、画面では縦横比を保って縮小し、モルワイデ投影の楕円の外側をマスクします。白亜紀JPEGの白い四隅を隠す表示上の変更で、同梱ファイルと地理の色は変更していません。

地図の復元には不確実性があります。黒線は目安として重ねられた現在の国境です。年代区分はICS国際年代層序表の数値を参照していますが、その表の画像を転載していません。

## 閲覧数・選定

[POPULARITY.md](POPULARITY.md) と [popularity-candidates.json](popularity-candidates.json) に対象・期間・取得URL・月別値を記録します。閲覧数は人気の目安で、普遍的な順位ではありません。候補の名前の一覧はWikipediaから抽出し、説明文の無許諾コピーには使っていません。

## 動画・外部リンク

全100項目にWikipediaとYouTube検索へのHTTPSリンクがあります。[video-links.json](video-links.json) の選定動画・動画掲載ページも引き続き掲載します。動画・音声・サムネイルを保存・同梱・再配信しません。外部アプリまたはブラウザーの公開ページで視聴します。検索結果や動画の内容・将来の再生可否を保証しません。

## ライブラリと表示

既存のAndroidX、Compose、Kotlin等の依存関係は変更していません。[runtime-dependencies.json](runtime-dependencies.json) とアプリ内のライセンスに通知を保持します。画像や解説の追加によるデータ収集、解析SDK、通信権限の追加はありません。

本アプリは各博物館、Wikimedia Foundation、YouTube、Googleの公式・提携アプリではありません。確認日：2026-09-07〜08。権利情報は保存した取得時点の証拠であり、法的問題が一切起きないという保証ではありません。権利情報の訂正があった場合は対象素材を再確認してください。

## シアターモード（1.6.0）

建物・照明・展示台は独自制作。実在館の写真・ロゴ・図面・壁画を転用せず、展示体験の参考資料を記載しています。館内用の骨格100種は既存データから派生し、公開メッシュ4種の元のCC0 / CC BY 4.0 / CC BY-SA 4.0条件を継続します。特にpatagotitan.tmszにもCC BY-SA 4.0を適用します。各作者・原本の帰属は既存の表示を保持し、部材ごとの簡略化、色の変換、正規化、描画用分割、展示用縮尺への変更を明記しました。[説明と資料](THEATER.md)、[全変換データの台帳](theater-mesh-provenance.json)。


1.7.0の館内建物・植物・天窓・採光・家具・アンモナイト風レリーフは独自制作（Apache 2.0）。既存の館内骨格の頂点・色は保持し、共通のm単位への倍率と配置を変更しました。派生メッシュの元のCC0 / CC BY / CC BY-SA条件を継続します。追加の寸法参照元と換算範囲は [THEATER.md](THEATER.md) およびアプリ内「館内の縮尺」に記載し、実測と推定を区別します。

## 1.8.0 森林の壁画

`app/src/main/assets/theater/mesozoic-forest.png` は2026-09-08に組み込みのOpenAI image_genで新規生成した本アプリ用の森林イメージです。第三者の壁画や写真は参照していません。生成時のPNGを未加工で同梱し、表示時に補間、左右反転・繰り返し、透視投影、遠景の霞を適用します。hatake716が権利を有する範囲でApache License 2.0として提供します。AI生成物の独占的な著作権成立を保証しません。[プロンプト・ファイルハッシュと表現範囲](forest-mural-provenance.json)、[生成記録](GENERATED_ARTWORK.md)。既存の第三者モデル・写真・図鑑本文の条件は継続します。


1.9.0 復元イラストの立て札
館内の復元イラストは、同梱の背景画100点を縮小して配置したものです。原画像の作者・原本URL・ライセンス・クレジットは各画像を開いた「作者・出典」、背景画の出典画面、および上記の権利情報に保持しています。立て札では余白と「復元イラスト」の見出しを付け、描画時に遠近・縮小を適用します。CC BY-SAの画像には引き続き各画像の同じライセンスを適用します。絵の閲覧は画像全体の表示から始まり、利用者の操作で拡大・移動できます。立て札の枠・支柱・配置を行う独自プログラムはApache License 2.0です。

## 1.10.0の化石表面

色合いと付着土は本アプリ独自の観察用表現で、実際の標本・産地の色を測定したものではありません。トリケラトプス、ティラノサウルス、カルノタウルスは元の公開資料の表面表現を使用します。 表面生成コードは独自実装（Apache 2.0）で、追加の第三者テクスチャを含みません。パタゴティタンの原資料への帰属・変更表示・CC BY-SA 4.0条件を継続します。[表面の実装と適用範囲](FOSSIL_SURFACES.md)。

## Google Play 掲載素材

公開用のアイコンとベクターのフィーチャーグラフィックは独自制作。実際のアプリ画面を撮影した縦横8枚のスクリーンショットには、既存の第三者素材が含まれます。[素材ごとの帰属・改変・配布条件](store/CREDITS.md)、[撮影・形式変換と画素ハッシュ](store/screenshot-manifest.json)を併記しています。ストア説明には[公開の出典ページ](https://hatake716.github.io/kyouryu-3d/credits/)を記載し、画像をコードと同じ条件で一括扱いしません。
