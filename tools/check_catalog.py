"""Check distributable content, attribution and frozen ranking as one artifact."""
import hashlib,json
from pathlib import Path
from urllib.parse import urlparse
ROOT=Path(__file__).resolve().parents[1]
def read(p):return json.loads((ROOT/p).read_text())
rows=read('app/src/main/assets/dinosaurs.json');arts=read('docs/artwork-provenance.json');selection=read('docs/selected-taxa.json');sources=read('docs/source-provenance.json');metrics=read('docs/popularity-candidates.json')['candidates']
assert len(rows)==len(arts)==len(selection)==len(sources)==100
assert len({r['id'] for r in rows})==100
assert len(metrics)==1468
assert all(1<=len(m['months'])<=3 and sum(x['views'] for x in m['months'])==m['views'] for m in metrics)
expected=[m for m in metrics if m['title']!='Postosuchus'][:100]
assert all(len(m['months'])==3 for m in expected)
assert [r['title'] for r in selection]==[r['title'] for r in expected]
assert [r['id'] for r in rows]==[r['title'].lower() for r in selection]
assert {r['id'] for r in sources}=={r['id'] for r in rows}
for rank,r in enumerate(rows,1):
 assert r['rank']==rank and r['pageviews']==expected[rank-1]['views'],r['id']
 a=r['artwork'];asset=Path('app/src/main/assets')/a['asset'];p=ROOT/asset
 assert p.resolve().is_relative_to((ROOT/'app/src/main/assets/backgrounds').resolve())
 assert hashlib.sha256(p.read_bytes()).hexdigest()==a['sha256'],r['id']
 assert a['artist'] and a['license'] and a['source'],r['id']
 assert a in arts
 for url in [a['source'],a['licenseUrl']]+[s['url'] for s in r['sources']+r['videos']]:
  assert urlparse(url).scheme=='https' and urlparse(url).netloc,url
 assert r['sources'] and any('oldid=' in s['url'] for s in r['sources'])
assert {p.name for p in (ROOT/'app/src/main/assets/backgrounds').iterdir()}=={Path(a['asset']).name for a in arts}
assert sum(bool(a.get('generated')) for a in arts)==2
print('Verified 100 entries, 100 image hashes and attributions, 100 source records and 1,468 returned ranking records (all selected 100 have three months).')
