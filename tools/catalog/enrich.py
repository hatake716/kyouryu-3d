"""Apply reviewed, offline editorial inputs to the reproducible catalogue."""
import json
from collections import defaultdict

TOPICS = (
    '名前の由来と命名', '生態と暮らし', '体の特徴と観察のポイント',
    '化石発見のエピソード', 'まだ解けていない謎',
)


def blocks(path):
    header = None
    lines = []
    for line in path.read_text().splitlines():
        if line.startswith('@'):
            if header is not None:
                yield header, lines
            header, lines = line[1:], []
        elif line.strip():
            assert header is not None, path
            lines.append(line)
    if header is not None:
        yield header, lines


def enrich(rows, root):
    folder = root / 'tools/catalog/enrichment'
    reading_blocks = list(blocks(folder / 'reading-notes.txt'))
    reading = dict(reading_blocks)
    assert len(reading_blocks) == len(reading) == len(rows) == 100
    assert set(reading) == {row['id'] for row in rows}
    references = json.loads((folder / 'references.json').read_text())
    provenance = {p['id']: p for p in json.loads((root / 'docs/source-provenance.json').read_text())}
    research = defaultdict(list)
    for header, paragraphs in blocks(folder / 'research-notes.txt'):
        taxon, title, keys = header.split('|')
        assert taxon in reading and paragraphs
        research[taxon].append(dict(title=title, text='\n\n'.join(paragraphs), references=[
            {k: references[key][k] for k in ('title', 'url')} for key in keys.split(',')
        ]))

    # Rock-age constraints are not first/last occurrences of the genus.
    for row in rows:
        taxon = row['id']
        if taxon in ('giganotosaurus', 'argentinosaurus', 'mapusaurus'):
            age, error, formation = (111.4, 1.1, 'カンデレロス層') if taxon == 'giganotosaurus' else (106.8, 2.2, 'ウインクル層')
            row.update(period='白亜紀（産出層の年代再検討中）',
                       age=f'産出層の年代制約：約{age}±{error}百万年前',
                       startMa=age, endMa=age)
            if taxon == 'argentinosaurus':
                row['summary'] = 'アルゼンチンのウインクル層から知られる巨大な植物食のティタノサウルス類です。部分骨格から全身を推定するため、大きさには幅があります。産出層の年代は2026年の研究で再検討されています。'
            if taxon == 'mapusaurus':
                row['summary'] = 'アルゼンチンのウインクル層から知られる大型のカルカロドントサウルス科の獣脚類です。複数個体の骨が集まった産地が有名ですが、協力して狩ったことの直接証拠ではありません。産出層の年代は再検討されています。'
            for section in row['sections']:
                if section['title'] in ('地層年代の更新', '時代・年代の再検討'):
                    section['text'] = f'{formation}のジルコン年代として約{age}±{error}百万年前が報告され、従来の白亜紀後期という位置付けが再検討されています。基本カードの数値は産出層の年代制約で、属の出現・絶滅年代ではありません。以下の過去の記述に残る旧年代と区別してください。'
                    section['references'] = [{k: references['dating26'][k] for k in ('title', 'url')}]

        paragraphs = reading[taxon]
        assert len(paragraphs) == len(TOPICS) and all(len(p) >= 60 for p in paragraphs), taxon
        source = provenance[taxon]
        fixed = {'title': 'Wikipedia contributors · 確認した固定版', 'url': source['permalink']}
        additions = [dict(title=title, text=paragraph, references=[fixed]) for title, paragraph in zip(TOPICS, paragraphs)]
        # Preserve older detail and attribution while making its historical context explicit.
        legacy = row['sections']
        for section in legacy:
            section['title'] = '補足：' + section['title']
        notice = dict(title='補足資料の読み方', text='ここからは従来の図鑑本文とWikipedia固定版に基づく補足です。古い年代・分類・行動の解釈も研究史として含まれます。新しい研究との違いは上の読みもの・研究ノートを参照してください。出典の固定版と編集履歴は末尾から開けます。')
        row['sections'] = additions + research[taxon] + [notice] + legacy
        known = {s['url'] for s in row['sources']}
        for section in research[taxon]:
            for source in section['references']:
                if source['url'] not in known:
                    row['sources'].append(source.copy())
                    known.add(source['url'])
