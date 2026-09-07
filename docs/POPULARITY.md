# 100項目の選定方法

ユーザーの指定に従い、Wikipedia閲覧数を人気の目安として採用しました。実際の投票結果や世界共通の「最も人気のある100種」を称するものではありません。アプリの「種類」は属単位を基本とする図鑑の項目です。有効性に議論がある歴史的な属名も含みます。

- 候補1,468記事：英語版Wikipediaの [恐竜の属の一覧](https://en.wikipedia.org/wiki/List_of_dinosaur_genera) から抽出した1,419名と、翼竜・中生代の海生／水辺の爬虫類を補った集合。補足名は `tools/catalog/rank.py`、抽出済み候補名は `tools/catalog/candidate-names.txt`。
- 期間：2026-06-01〜2026-08-31。直近の完了した3か月。
- API：en.wikipedia / all-access / user / monthly。人による閲覧を対象とし、自動化エージェントの閲覧は含めません。[Wikimediaの指標定義](https://doc.wikimedia.org/generated-data-platform/aqs/analytics-api/reference/page-views.html)。
- 各記事の月別値を合計。言語間・別名リダイレクト・属内の種別記事は合算しません。新しい研究・ニュースによる一時的な関心を含みます。
- APIが返した合計値で候補を降順に並べ、今回の対象外である陸生のワニ系主竜類Postosuchusを除いた100項目。元の13項目はすべて自然に100項目内へ入り、順位への加点や優先枠はありません。
- 全1,468候補のAPI応答を取得でき、採用100記事はすべて3か月分あります。圏外の7記事は1〜2か月分しか返らず、その月だけの合計です。未返却月を0件と偽って記録していません。したがって候補全体の完全な履歴を保証する順位ではありません。

全候補の取得URL・月別値・応答ハッシュ：[popularity-candidates.json](popularity-candidates.json)。採用順と合計値：[selected-taxa.json](selected-taxa.json)。取得・選定日：2026-09-07。収集時のHTTP 429は間隔を空けて再取得しました。アプリで自動更新はしません。

APIが3か月分を返さなかった圏外の記事：Uragasaurus（2か月）, Plesiolophus（2か月）, Dinevenator（1か月）, Ceratocaudia（2か月）, Musango（2か月）, Nipponopelta（1か月）, Mesetasaurus（2か月）。

## 収録項目

|順序|日本語名|属名|閲覧数（3か月）|分類|
|---:|---|---|---:|---|
|1|ティラノサウルス|Tyrannosaurus|249,988|獣脚類|
|2|スピノサウルス|Spinosaurus|113,996|獣脚類|
|3|ヴェロキラプトル|Velociraptor|106,223|獣脚類|
|4|アロサウルス|Allosaurus|102,921|獣脚類|
|5|ケツァルコアトルス|Quetzalcoatlus|101,030|翼竜類|
|6|トリケラトプス|Triceratops|92,183|角竜類|
|7|ステゴサウルス|Stegosaurus|89,186|剣竜類|
|8|アーケオプテリクス|Archaeopteryx|77,790|鳥類に近い恐竜|
|9|モササウルス|Mosasaurus|74,126|海生爬虫類|
|10|ギガノトサウルス|Giganotosaurus|69,327|獣脚類|
|11|ブラキオサウルス|Brachiosaurus|66,235|竜脚類|
|12|アンキロサウルス|Ankylosaurus|65,038|鎧竜類|
|13|ディロフォサウルス|Dilophosaurus|64,598|獣脚類|
|14|テリジノサウルス|Therizinosaurus|62,468|獣脚類|
|15|デイノニクス|Deinonychus|58,637|獣脚類|
|16|パラサウロロフス|Parasaurolophus|55,770|鳥脚類|
|17|ユタラプトル|Utahraptor|53,301|獣脚類|
|18|カルノタウルス|Carnotaurus|53,094|獣脚類|
|19|パキケファロサウルス|Pachycephalosaurus|52,436|堅頭竜類|
|20|ブロントサウルス|Brontosaurus|52,199|竜脚類|
|21|アルゼンチノサウルス|Argentinosaurus|50,418|竜脚類|
|22|ディプロドクス|Diplodocus|46,986|竜脚類|
|23|プテロダクティルス|Pterodactylus|46,533|翼竜類|
|24|プテラノドン|Pteranodon|43,076|翼竜類|
|25|イグアノドン|Iguanodon|39,760|鳥脚類|
|26|スコミムス|Suchomimus|39,019|獣脚類|
|27|ニジェールサウルス|Nigersaurus|36,597|竜脚類|
|28|アパトサウルス|Apatosaurus|34,106|竜脚類|
|29|エドモントサウルス|Edmontosaurus|33,109|鳥脚類|
|30|カルカロドントサウルス|Carcharodontosaurus|32,868|獣脚類|
|31|メガロサウルス|Megalosaurus|32,735|獣脚類|
|32|ボレアロペルタ|Borealopelta|31,100|鎧竜類|
|33|ヘレラサウルス|Herrerasaurus|30,858|初期の恐竜|
|34|バリオニクス|Baryonyx|30,848|獣脚類|
|35|コンプソグナトゥス|Compsognathus|30,680|獣脚類|
|36|ケラトサウルス|Ceratosaurus|28,869|獣脚類|
|37|ティタノサウルス|Titanosaurus|28,684|竜脚類|
|38|パタゴティタン|Patagotitan|28,510|竜脚類|
|39|ユウティラヌス|Yutyrannus|27,059|獣脚類|
|40|ハツェゴプテリクス|Hatzegopteryx|26,303|翼竜類|
|41|トロオドン|Troodon|25,288|獣脚類|
|42|ティロサウルス|Tylosaurus|24,731|海生爬虫類|
|43|リオプレウロドン|Liopleurodon|24,466|海生爬虫類|
|44|アクロカントサウルス|Acrocanthosaurus|24,460|獣脚類|
|45|ミクロラプトル|Microraptor|22,555|鳥類に近い恐竜|
|46|ナノティラヌス|Nanotyrannus|22,003|獣脚類|
|47|タルボサウルス|Tarbosaurus|21,709|獣脚類|
|48|カマラサウルス|Camarasaurus|21,284|竜脚類|
|49|プレシオサウルス|Plesiosaurus|21,011|海生爬虫類|
|50|スティラコサウルス|Styracosaurus|20,420|角竜類|
|51|アウストラロヴェナトル|Australovenator|20,225|獣脚類|
|52|コエロフィシス|Coelophysis|20,134|獣脚類|
|53|アルバートサウルス|Albertosaurus|20,115|獣脚類|
|54|ブルハトカヨサウルス|Bruhathkayosaurus|20,097|竜脚類|
|55|デイノケイルス|Deinocheirus|19,658|獣脚類|
|56|クリスタトゥサウルス|Cristatusaurus|19,350|獣脚類|
|57|ドレッドノータス|Dreadnoughtus|19,184|竜脚類|
|58|プロトケラトプス|Protoceratops|18,673|角竜類|
|59|プシッタコサウルス|Psittacosaurus|18,097|角竜類|
|60|エラスモサウルス|Elasmosaurus|17,914|海生爬虫類|
|61|アーケロン|Archelon|17,607|海生爬虫類|
|62|プリオサウルス|Pliosaurus|17,440|海生爬虫類|
|63|イリタトル|Irritator|17,216|獣脚類|
|64|ゴルゴサウルス|Gorgosaurus|16,866|獣脚類|
|65|ガリミムス|Gallimimus|16,561|獣脚類|
|66|トルヴォサウルス|Torvosaurus|16,042|獣脚類|
|67|マジュンガサウルス|Majungasaurus|15,922|獣脚類|
|68|マイプ|Maip|15,921|獣脚類|
|69|クロノサウルス|Kronosaurus|15,920|海生爬虫類|
|70|シノサウロプテリクス|Sinosauropteryx|15,170|獣脚類|
|71|トロペオグナトゥス|Tropeognathus|15,118|翼竜類|
|72|マイアサウラ|Maiasaura|14,899|鳥脚類|
|73|オヴィラプトル|Oviraptor|14,771|獣脚類|
|74|アマルガサウルス|Amargasaurus|14,705|竜脚類|
|75|アクイロプス|Aquilops|14,540|角竜類|
|76|アラモサウルス|Alamosaurus|14,278|竜脚類|
|77|クリョロフォサウルス|Cryolophosaurus|14,191|獣脚類|
|78|ピロラプトル|Pyroraptor|14,157|獣脚類|
|79|サウロポセイドン|Sauroposeidon|14,043|竜脚類|
|80|ナヌークサウルス|Nanuqsaurus|13,904|獣脚類|
|81|ダスプレトサウルス|Daspletosaurus|13,533|獣脚類|
|82|マメンチサウルス|Mamenchisaurus|13,513|竜脚類|
|83|イクチオサウルス|Ichthyosaurus|13,504|海生爬虫類|
|84|サウロロフス|Saurolophus|13,389|鳥脚類|
|85|プラテオサウルス|Plateosaurus|13,334|竜脚形類|
|86|サウロファガナクス|Saurophaganax|12,914|竜脚類|
|87|アンキオルニス|Anchiornis|12,696|鳥類に近い恐竜|
|88|パキリノサウルス|Pachyrhinosaurus|12,652|角竜類|
|89|コンカヴェナトル|Concavenator|12,528|獣脚類|
|90|ランフォリンクス|Rhamphorhynchus|12,360|翼竜類|
|91|タニストロフェウス|Tanystropheus|12,212|水辺の爬虫類|
|92|マプサウルス|Mapusaurus|12,192|獣脚類|
|93|ラジャサウルス|Rajasaurus|12,075|獣脚類|
|94|ケントロサウルス|Kentrosaurus|11,750|剣竜類|
|95|スキピオニクス|Scipionyx|11,727|獣脚類|
|96|トロサウルス|Torosaurus|11,589|角竜類|
|97|ハドロサウルス|Hadrosaurus|11,530|鳥脚類|
|98|ドリプトサウルス|Dryptosaurus|11,524|獣脚類|
|99|ディモルフォドン|Dimorphodon|11,478|翼竜類|
|100|エオラプトル|Eoraptor|11,294|竜脚形類|
