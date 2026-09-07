"""Freeze a transparent popularity proxy, never a claimed universal popularity poll."""
import concurrent.futures, hashlib, json, re, time
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.parse import quote
ROOT=Path(__file__).resolve().parents[2]
CACHE=ROOT/'artifacts/research/pageviews'
CACHE.mkdir(parents=True,exist_ok=True)
UA='Kyoryu3D/1.1 (https://github.com/hatake716/kyouryu-3d; catalog research)'
START='2026060100';END='2026083100'
names=set((ROOT/'tools/catalog/candidate-names.txt').read_text().splitlines())
names.update('Archaeopteryx Pteranodon Quetzalcoatlus Hatzegopteryx Pterodactylus Rhamphorhynchus Dimorphodon Dorygnathus Dsungaripterus Tapejara Tupandactylus Tropeognathus Anhanguera Ornithocheirus Nyctosaurus Sordes Anurognathus Darwinopterus Zhejiangopterus Thalassodromeus Sinopterus Kunpengopterus Mosasaurus Tylosaurus Prognathodon Platecarpus Clidastes Plotosaurus Plesiosaurus Elasmosaurus Liopleurodon Kronosaurus Pliosaurus Dolichorhynchops Cryptoclidus Styxosaurus Albertonectes Aristonectes Ichthyosaurus Shonisaurus Shastasaurus Temnodontosaurus Ophthalmosaurus Stenopterygius Eurhinosaurus Cymbospondylus Nothosaurus Placodus Tanystropheus Dakosaurus Metriorhynchus Archelon'.split())
def fetch(name):
    p=CACHE/(name+'.json')
    url=f'https://wikimedia.org/api/rest_v1/metrics/pageviews/per-article/en.wikipedia/all-access/user/{quote(name,safe="")}/monthly/{START}/{END}'
    if p.exists():
        cached=json.loads(p.read_text())
        if cached.get('url')==url and (cached.get('views') is not None or '404' in cached.get('error','')):return cached
    error=None
    for attempt in range(4):
        try:
            with urlopen(Request(url,headers={'User-Agent':UA}),timeout=25) as r:raw=r.read()
            response=json.loads(raw);items=response['items']
            result={'title':name,'views':sum(x['views'] for x in items),'months':items,'url':url,'sha256':hashlib.sha256(raw).hexdigest()}
            p.write_text(json.dumps(result,ensure_ascii=False));time.sleep(1.2);return result
        except Exception as e:
            error=str(e)
            if '404' in error:break
            time.sleep(2**attempt)
    result={'title':name,'views':None,'error':error,'url':url}
    p.write_text(json.dumps(result));return result
results=[]
with concurrent.futures.ThreadPoolExecutor(max_workers=1) as ex:
    for i,r in enumerate(ex.map(fetch,sorted(names))):
        results.append(r)
        if (i+1)%50==0:print(f'{i+1}/{len(names)}',flush=True)
results.sort(key=lambda x:x['views'] if x['views'] is not None else -1,reverse=True)
(ROOT/'docs/popularity-candidates.json').write_text(json.dumps({'project':'en.wikipedia','agent':'user','access':'all-access','start':START,'end':END,'candidateList':'https://en.wikipedia.org/wiki/List_of_dinosaur_genera','supplement':'named pterosaurs and Mesozoic marine reptiles; see script','candidates':results},ensure_ascii=False,indent=2)+'\n')
for r in results[:135]:print(r['title'],r['views'],flush=True)
