"""Download licensed, unmodified Wikimedia thumbnails; preserve authors and evidence."""
import json, hashlib, time, re
from pathlib import Path
from urllib.parse import unquote
from research import ROOT,CACHE,metadata,fetch
OVERRIDES={
 'Nanotyrannus':'File:Nanotyrannus lancensis.png',
 'Tarbosaurus':'File:Tarbosaurus TD.png',
 'Bruhathkayosaurus':'File:Bruhathkayosaurus matleyi updated.png',
 'Maiasaura':'File:Maiasaura peeblesorum.png',
 'Saurolophus':'File:Saurolophus osborni.png',
 'Rhamphorhynchus':'File:Rhamphorhynchus Flock.jpg',
 'Tanystropheus':'File:Tanystropheus NT small.jpg',
 'Utahraptor':'File:Utahraptor Restoration.png',
 'Oviraptor':'File:Oviraptor Restoration.png',
 'Archelon':'File:Archelon BW.jpg',
 'Spinosaurus':'File:Spinosaurus life restoration with Onchopristis.jpg',
 'Tyrannosaurus':'File:Tyrannosaurus-rex-Profile-steveoc86.png',
 'Triceratops':'File:Triceratops BW.jpg',
 'Velociraptor':'File:Velociraptor mongoliensis.jpg',
 'Allosaurus':'File:Allosaurus Revised.jpg',
 'Archaeopteryx':'File:Archaeopteryx lithographica by durbed.jpg',
}
def choose(r):
    name=r['title']
    if name in OVERRIDES:return metadata(OVERRIDES[name])
    candidates=r['candidates']
    # Reject related genera, comparisons and historic poses; reviewers inspect the remaining selection.
    candidates=[a for a in candidates if name.lower() in a['fileTitle'].lower() and not re.search(r'juvenile|comparison|outdated|historical|1901|relative|multiple|various',a['caption'],re.I)]
    return candidates[0] if candidates else None
if __name__=='__main__':
    target=ROOT/'app/src/main/assets/backgrounds';target.mkdir(exist_ok=True)
    out=ROOT/'docs/artwork-provenance.json'
    existing={a['id']:a for a in json.loads(out.read_text())} if out.exists() else {}
    for selected in json.loads((ROOT/'docs/selected-taxa.json').read_text()):
        name=selected['title'];id=name.lower();p=CACHE/f'{name}-research.json'
        if not p.exists():continue
        old=existing.get(id)
        if old and (name not in OVERRIDES or old['fileTitle']==OVERRIDES[name]):continue
        try:
            a=choose(json.loads(p.read_text()))
            if not a:print('MISSING',name,flush=True);continue
            if a['restrictions']:print('RESTRICTED',name,flush=True);continue
            if a['license']=='Public domain':a['licenseUrl']='https://creativecommons.org/publicdomain/mark/1.0/'
            ext=Path(unquote(a['url']).split('?')[0]).suffix.lower()
            if ext not in ['.png','.jpg','.jpeg','.webp']:print('FORMAT',name,ext,flush=True);continue
            asset=f'backgrounds/{id}{ext}';path=ROOT/'app/src/main/assets'/asset
            if not path.exists() or (old and old['fileTitle']!=a['fileTitle']):path.write_bytes(fetch(a['url']))
            a.update(id=id,asset=asset,sha256=hashlib.sha256(path.read_bytes()).hexdigest(),retrieved='2026-09-07',modifications='Wikimedia thumbnail downloaded unchanged; displayed cropped and dimmed in app.')
            existing[id]=a;out.write_text(json.dumps(list(existing.values()),ensure_ascii=False,indent=2)+'\n')
            print('OK',name,a['fileTitle'],a['license'],flush=True);time.sleep(.5)
        except Exception as e:print('ERROR',name,str(e),flush=True)
