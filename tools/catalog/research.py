"""Read Wikipedia articles and collect licensed restoration candidates, with evidence.
Requires lxml. Downloaded article bodies stay in ignored artifacts/research.
"""
import concurrent.futures, hashlib, json, re, ssl, time, sys
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.parse import quote, unquote, urlencode
from lxml import html
ROOT=Path(__file__).resolve().parents[2]
CACHE=ROOT/'artifacts/research';CACHE.mkdir(parents=True,exist_ok=True)
UA='Kyoryu3D/1.1 (https://github.com/hatake716/kyouryu-3d; attribution research)'
CTX=ssl.create_default_context(cafile='/etc/ssl/certs/ca-certificates.crt') if Path('/etc/ssl/certs/ca-certificates.crt').exists() else ssl.create_default_context()
def fetch(url):
    for attempt in range(4):
        try:
            with urlopen(Request(url,headers={'User-Agent':UA}),context=CTX,timeout=35) as r:return r.read()
        except Exception:
            if attempt==3:raise
            time.sleep(2**attempt)
def clean(s):return re.sub(r'\s+',' ',s).strip()
def article(title,lang='en'):
    p=CACHE/f'{lang}-{title}.html'
    if not p.exists():p.write_bytes(fetch(f'https://{lang}.wikipedia.org/wiki/'+quote(title.replace(' ','_'),safe='')))
    data=p.read_bytes();root=html.fromstring(data)
    revision=re.search(r'"wgRevisionId"\s*:\s*(\d+)',data.decode()) or re.search(r'Special:Redirect/revision/(\d+)',data.decode())
    ja=root.xpath('//a[@lang="ja"]/@href')
    figures=[]
    for f in root.xpath('//figure|//td[contains(@class,"infobox-image")]'):
        links=f.xpath('.//a[contains(@href,"File:")]/@href')
        if links:
            title=unquote(links[0].split('/wiki/')[-1]).replace('_',' ')
            figures.append({'file':title,'caption':clean(f.text_content())})
    paragraphs=[clean(p.text_content()) for p in root.xpath('//p') if len(clean(p.text_content()))>60]
    return {'revision':revision.group(1) if revision else None,'sha256':hashlib.sha256(data).hexdigest(),'japanese':unquote(ja[0].split('/wiki/')[-1]) if ja else None,'paragraphs':paragraphs,'figures':figures}
def score(fig,taxon):
    s=(fig['file']+' '+fig['caption']).lower()
    good=sum(w in s for w in ['life restoration','restoration of','reconstruction','artist','restoration'])
    bad=sum(w in s for w in ['skelet','skull','size comparison','diagram','outdated','historical','1905','1900','1897','statue','sculpture','skeleton','museum','jaw','comparison','scale','silhouette'])
    return good*10-bad*12+(4 if taxon.lower() in s else 0)
def metadata(file):
    url='https://commons.wikimedia.org/w/api.php?'+urlencode({'action':'query','titles':file,'prop':'imageinfo','iiprop':'url|extmetadata|size|sha1','iiurlwidth':1280,'format':'json'})
    result=json.loads(fetch(url));pages=result.get('query',{}).get('pages',{})
    for p in pages.values():
        if p.get('imageinfo'):
            info=p['imageinfo'][0];meta=info.get('extmetadata',{})
            def value(k):return clean(html.fromstring('<div>'+meta.get(k,{}).get('value','')+'</div>').text_content())
            license=value('LicenseShortName');licenseurl=meta.get('LicenseUrl',{}).get('value','')
            allowed=bool(re.fullmatch(r'CC BY(?:-SA)? (?:2\.0|2\.5|3\.0|4\.0)',license) or license in ['CC0','Public domain'])
            if not allowed:return None
            return {'fileTitle':p['title'],'source':info['descriptionurl'],'url':info.get('thumburl',info['url']),'originalUrl':info['url'],'artist':value('Artist'),'credit':value('Credit'),'license':license,'licenseUrl':licenseurl or ('https://creativecommons.org/publicdomain/zero/1.0/' if license=='CC0' else 'https://creativecommons.org/publicdomain/mark/1.0/'),'description':value('ImageDescription'),'width':info.get('thumbwidth',info['width']),'height':info.get('thumbheight',info['height']),'originalSha1':info.get('sha1'),'attributionRequired':value('AttributionRequired'),'restrictions':value('Restrictions')}
    return None
def research(taxon):
    out=CACHE/f'{taxon}-research.json'
    if out.exists():return json.loads(out.read_text())
    a=article(taxon);candidates=[]
    for fig in sorted(a['figures'],key=lambda f:score(f,taxon),reverse=True):
        if score(fig,taxon)<8 or len(candidates)>=3:break
        try:
            info=metadata(fig['file'])
            if info and info['width']>=600 and info['height']>=300 and not info['restrictions']:candidates.append(dict(info,caption=fig['caption'],score=score(fig,taxon)))
        except Exception as e:print(taxon,'image error',str(e),flush=True)
    a['candidates']=candidates;a['title']=taxon
    if a['japanese']:
        try:a['jaArticle']=article(a['japanese'],'ja')
        except Exception as e:a['jaError']=str(e)
    out.write_text(json.dumps(a,ensure_ascii=False,indent=2));print(taxon,'images',len(candidates),'JA',a['japanese'],flush=True);return a
if __name__=='__main__':
    names=sys.argv[1:] or [r['id'].capitalize() for r in json.loads((ROOT/'app/src/main/assets/dinosaurs.json').read_text())]
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as ex:
        for r in ex.map(research,names):pass
