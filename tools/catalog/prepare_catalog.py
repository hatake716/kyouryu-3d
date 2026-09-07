"""Freeze reviewed article excerpts and factual cards. Network-free after research caches exist."""
import json,re,hashlib
from pathlib import Path
from urllib.parse import quote
from lxml import html
from research import ROOT,CACHE,clean
from extract_text import sections
from profiles import PROFILES
facts={r[0]:r[1:] for r in [line.split('\t') for line in (ROOT/'tools/catalog/facts.tsv').read_text().splitlines()]}
original=set('spinosaurus tyrannosaurus triceratops mosasaurus velociraptor stegosaurus ankylosaurus brachiosaurus parasaurolophus giganotosaurus allosaurus pteranodon archaeopteryx'.split())
AGES={'Therizinosaurus':(70,66),'Borealopelta':(110,110),'Tarbosaurus':(70,66),'Ceratosaurus':(154,149),'Deinocheirus':(70,66),'Gallimimus':(70,66),'Maip':(72,66),'Saurophaganax':(157,149),'Tanystropheus':(247,232)}
CATEGORIES={'brachiosaurus':'竜脚類','pteranodon':'翼竜類','mosasaurus':'海生爬虫類','triceratops':'角竜類','stegosaurus':'剣竜類','ankylosaurus':'鎧竜類','parasaurolophus':'鳥脚類','archaeopteryx':'鳥類に近い恐竜'}
UPDATES={
'Nanotyrannus':('2025年の分類研究','2025年の新しい標本・骨格形態・成長の解析は、ナノティラヌスをティラノサウルスの幼体とは異なる属として支持し、N. lancensis と N. lethaeus の2種を扱っています。古い文献には幼体説が多く現れます。本項の歴史的な記述は研究当時の見解として読み分けてください。','https://doi.org/10.1038/s41586-025-09801-6'),
'Saurophaganax':('分類の再検討と表示している姿','2024年の再検討は、従来サウロファガナクスにまとめられた標本に、複数の恐竜の骨が混ざっている可能性を示しました。基準標本の神経弓は竜脚類かもしれず、獣脚類の材料には Allosaurus anax という新種が提案されています。この属の全身像は確定できません。本アプリの骨格と背景は「竜脚類だった場合」の強い仮定を含むイメージで、以前の大型肉食恐竜の復元とは異なります。','https://scholars.okstate.edu/en/publications/chimerism-of-specimens-referred-to-isaurophaganax-maximusi-reveal/'),
'Titanosaurus':('属の有効性と復元の限界','模式種はインドで発見された断片的な骨に基づき、他の竜脚類と区別する特徴が十分かどうかが問題となっています。かつて本属に含まれた多くの種は別属へ移されました。本アプリの全身像はティタノサウルス類に共通する体型を示す仮説的な模式図です。ティタノサウルス類全体と、この属を同一視することはできません。',None),
'Bruhathkayosaurus':('標本の状態と大きさの不確実性','元の骨は現在直接再調査できず、残された写真と記録が重要です。巨大な竜脚類だった可能性が論じられていますが、全身の比率や最大サイズの推定には極めて大きな不確実性があります。背景と3D骨格は、ティタノサウルス類と仮定した体型イメージです。',None),
'Troodon':('名前と全身像の対応','属名の基準は歯です。多くの全身復元は、かつてこの属へまとめられた標本や近縁のトロオドン類を参考にしています。分類によって Stenonychosaurus などを分けるため、表示する全身骨格を模式標本の確定復元とは扱えません。',None),
'Cristatusaurus':('分類と補完部分','吻などの部分的な骨に基づくスピノサウルス類で、同じ地域のスコミムスとの区別や属の有効性には議論があります。首・胴・尾・後肢などの全身像は近縁群からの補完を大きく含みます。',None),
'Argentinosaurus':('地層年代の更新','従来は白亜紀後期と紹介されていましたが、2026年の年代研究を受け、参照した英語版では産出層を白亜紀前期アルビアン期の約1億620万年前としています。本項の基本カードはこの更新値を採用します。以下の日本語版抄録には従来の年代の説明が含まれます。','https://doi.org/10.1016/j.gr.2026.06.020'),
'Mapusaurus':('地層年代の更新','産出するウインクル層の年代は再検討されています。基本カードは、2026年の年代研究を反映した英語版の約1億620万年前を採用します。日本語版の抄録や過去の資料にある約9700万〜9300万年前という紹介とは異なります。','https://doi.org/10.1016/j.gr.2026.06.020'),
}
rows=[];prov=[]
for selected in json.loads((ROOT/'docs/selected-taxa.json').read_text()):
 name=selected['title'];id=name.lower()
 if id in original:continue
 r=json.loads((CACHE/f'{name}-research.json').read_text());ja=r['japanese'];assert ja,name
 secs=sections(name,ja);assert secs,name
 region,size,subtitle=facts[name]
 root=html.fromstring((CACHE/f'en-{name}.html').read_bytes());box=clean(root.xpath('//table[contains(@class,"infobox")]')[0].text_content()).split('PreꞒ')[0]
 nums=re.search(r'(\d+(?:\.\d+)?)\s*[–−-]\s*(\d+(?:\.\d+)?)\s*Ma',box)
 one=re.search(r'(\d+(?:\.\d+)?)\s*Ma',box)
 start,end=tuple(map(float,nums.groups())) if nums else ((float(one[1]),float(one[1])) if one else AGES[name])
 # Show approximate numeric dates directly; a single estimate is not invented into a range.
 age=f'約{start:g}〜{end:g}百万年前' if start!=end else f'約{start:g}百万年前'
 if name in AGES:age+='（地層・時代の概算）'
 period='三畳紀' if start>=201.4 else 'ジュラ紀' if start>=143.1 else '白亜紀'
 for en,jp in [('Late Triassic','三畳紀後期'),('Early Jurassic','ジュラ紀前期'),('Middle Jurassic','ジュラ紀中期'),('Late Jurassic','ジュラ紀後期'),('Early Cretaceous','白亜紀前期'),('Late Cretaceous','白亜紀後期')]:
  if en in box:period=jp;break
 if 'Jurassic' in box and ('Early Cretaceous' in box or 'Lower Cretaceous' in box):period='ジュラ紀後期〜白亜紀前期'
 if 'Early Cretaceous' in box and 'Late Cretaceous' in box:period='白亜紀前期〜後期'
 if ('Middle Jurassic' in box and 'Late Jurassic' in box) or 'Middle - Late Jurassic' in box:period='ジュラ紀中期〜後期'
 profile=PROFILES[id];category=CATEGORIES.get(profile['base'],'獣脚類')
 if name=='Pachycephalosaurus':category='堅頭竜類'
 if name in ['Plateosaurus','Eoraptor']:category='竜脚形類'
 if name=='Herrerasaurus':category='初期の恐竜'
 if name=='Tanystropheus':category='水辺の爬虫類'
 if name=='Archelon':category='海生爬虫類'
 diet='植物食' if category in ['竜脚類','角竜類','剣竜類','鎧竜類','鳥脚類','堅頭竜類','竜脚形類'] else '肉食'
 if name=='Therizinosaurus':diet='植物食（推定）'
 if name in ['Gallimimus','Deinocheirus','Oviraptor','Eoraptor']:diet='雑食など複数説'
 if name in ['Suchomimus','Baryonyx','Cristatusaurus','Irritator']:diet='魚食・肉食（推定）'
 if category in ['海生爬虫類','翼竜類','水辺の爬虫類']:diet='魚や小動物など（推定）'
 enurl=f'https://en.wikipedia.org/w/index.php?title={name}&oldid={r["revision"]}'
 jurl=f'https://ja.wikipedia.org/w/index.php?title={quote(ja)}&oldid={r["jaArticle"]["revision"]}'
 assert r['revision'] and r['jaArticle']['revision'],name
 refs=[{'title':f'Wikipedia contributors · {ja}（日本語・抄録元の固定版）','url':jurl},{'title':'Wikipedia 日本語版の編集履歴・執筆者','url':f'https://ja.wikipedia.org/w/index.php?title={quote(ja)}&action=history'},{'title':f'Wikipedia contributors · {name}（英語・基本情報の参照版）','url':enurl},{'title':'Wikipedia 英語版の編集履歴・執筆者','url':f'https://en.wikipedia.org/w/index.php?title={name}&action=history'}]
 summary=secs[0]['text'].split('\n\n')[0]
 notice={'title':'この解説の読み方','text':'基本カードは確認した英語版などから整理し、以下には日本語版Wikipediaの本文を抄録しています。引用番号・外部リンク・表や一部の節を省略し、節を分割しています。資料によって年代・体格・分類に違いがあります。各説の詳細な根拠や参考文献は末尾の固定版から確認できます。'}
 if name in UPDATES:
  title,text,url=UPDATES[name];secs.insert(0,{'title':title,'text':text})
  if url:refs.append({'title':'分類・年代の更新に関する研究','url':url})
  if name in ['Saurophaganax','Nanotyrannus','Titanosaurus','Bruhathkayosaurus','Cristatusaurus']:summary=text
 modelNote=f'{category}の体型構造を共有し、首・頭・尾・四肢や特徴的な突起を模式化した独自モデルです。比率は図示のための設定で、化石の計測値ではありません。属内の全種、全個体の違いは表現していません。'
 if name in ['Saurophaganax','Troodon','Titanosaurus','Bruhathkayosaurus','Cristatusaurus']:modelNote+=' 本属は標本・分類に強い不確実性があり、全身像は近縁群に基づく仮説です。'
 rows.append(dict(id=id,name=ja,latin=name,subtitle=subtitle,category=category,period=period,age=age,startMa=start,endMa=end,region=region,size=size,diet=diet,summary=summary,sections=[notice]+secs,sources=refs,modelNote=modelNote,wikiTitle=ja,videos=[],contentOrigin='ja-excerpt'))
 prov.append(dict(id=id,englishTitle=name,revision_id=r['revision'],permalink=enurl,html_sha256=r['sha256'],japaneseTitle=ja,japaneseRevision=r['jaArticle']['revision'],japanesePermalink=jurl,japaneseHtmlSha256=r['jaArticle']['sha256'],reviewDate='2026-09-07',adaptation='Japanese Wikipedia narrative excerpt; citations, images, tables and external sections omitted. English Wikipedia factual cards, original Japanese summaries and explicit update notes.',license='CC BY-SA 4.0',authors='Wikipedia contributors'))
(ROOT/'tools/catalog/extra-entries.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n')
old=[p for p in json.loads((ROOT/'docs/source-provenance.json').read_text()) if p['id'] in original]
(ROOT/'docs/source-provenance.json').write_text(json.dumps(old+prov,ensure_ascii=False,indent=2)+'\n')
print('Extra entries',len(rows),'sections',sum(len(r['sections']) for r in rows))
