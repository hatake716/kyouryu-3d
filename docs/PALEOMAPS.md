# 時代の世界地図

三畳紀・ジュラ紀・白亜紀の古地理復元図を左右にスライドすると、コレクションの時代が切り替わります。初期表示は白亜紀です。時代名のボタン・左右の矢印でも選択でき、「全時代」で100項目を横断して検索できます。検索語、分類、お気に入りと時代の条件を組み合わせます。骨格画面から一覧へ戻ったとき、また画面を再生成したときも選択とスクロール位置を保持します。

地図は各時代の**代表時点**であり、その時点に生存していた生物だけの一覧ではありません。図鑑の時代ラベルを基に、その時代全体の生きものを表示します。複数の時代にまたがる属は両方に表示します。

|時代|地図の時点|収録項目数|
|---|---|---:|
|三畳紀|2億2500万年前（225 Ma）|5|
|ジュラ紀|1億5500万年前（155 Ma）|25|
|白亜紀|1億年前（100 Ma）|72|

マメンチサウルスとプリオサウルスがジュラ紀・白亜紀の両方に含まれるため、上記の合計はユニークな100項目より多くなります。イクチオサウルスの時代ラベルは、基本カードの数値年代と属レベルの資料に一致する「ジュラ紀前期」に修正しました。元の日本語版抄録の範囲表記との差は項目内の注記に説明しています。[NHMの魚竜解説](https://www.nhm.ac.uk/discover/what-is-an-ichthyosaur.html)。

## 地図の帰属とライセンス

全3点の作者：**Scotese, Christopher R.; Vérard, Christian; Burgener, Landon; Elling, Reece P.; Kocsis, Ádám T.**

出典：PALEOMAP project, *Phanerozoic-scope supplementary material to “The Cretaceous World: Plate Tectonics, Paleogeography, and Paleoclimate”* (2024)。[研究資料・DOI 10.5281/zenodo.10659112](https://doi.org/10.5281/zenodo.10659112)、[論文 DOI 10.1144/SP544-2024-28](https://doi.org/10.1144/SP544-2024-28)。

ライセンス：**[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)**。プログラムのApache 2.0をこれらの画像に適用しません。アプリの「地図の出典」から作者、ライセンス、原本、研究資料を参照できます。再配布時にも帰属・出典・ライセンス・変更内容を保持してください。

- 三畳紀：[Mollweide Paleographic Map of Earth, 225 Ma (Norian Age).png](https://commons.wikimedia.org/wiki/File:Mollweide_Paleographic_Map_of_Earth,_225_Ma_%28Norian_Age%29.png)
- ジュラ紀：[Mollweide Paleographic Map of Earth, 155 Ma (Oxfordian Age).png](https://commons.wikimedia.org/wiki/File:Mollweide_Paleographic_Map_of_Earth,_155_Ma_%28Oxfordian_Age%29.png)
- 白亜紀：[100Ma Mollweide Paleographic Map of Earth.jpg](https://commons.wikimedia.org/wiki/File:100Ma_Mollweide_Paleographic_Map_of_Earth.jpg)

Wikimediaの配信用縮小画像を未加工で同梱し、表示領域に収まるよう縮小して表示します。画像内の黒い線は、位置の目安として重ねた**現在の国境**であり、当時の国境ではありません。海岸線や大陸の位置には復元上の不確実性があります。

取得日、Commons固定版、配信用URL、元画像SHA-1、同梱ファイルSHA-256は [paleomap-provenance.json](paleomap-provenance.json)。同じ記録をアプリの `assets/maps.json` に収録し、`tools/check_catalog.py` が画像と帰属の対応を検証します。通常のビルド・アプリ動作に外部取得は不要です。再取得する場合のみ `python3 tools/fetch_paleomaps.py` を使用してください。

## 時代区分

時代区分の境界年代は [ICS国際年代層序表 v2026/06](https://stratigraphy.org/chart/) に基づく概数です。表そのものの画像は同梱していません。

- 三畳紀：約251.9〜201.4 Ma
- ジュラ紀：約201.4〜143.1 Ma
- 白亜紀：約143.1〜66.0 Ma

参照：Cohen, Harper, Gibbard & Car (2025), *The ICS international chronostratigraphic chart this decade*, Episodes 48:105–115, [DOI 10.18814/epiiugs/2025/025001](https://doi.org/10.18814/epiiugs/2025/025001)。境界の数値年代と個々の化石の推定年代には誤差や更新があります。
