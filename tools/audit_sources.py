"""Capture attribution evidence without redistributing source article bodies."""
from urllib.request import Request,urlopen
from urllib.parse import quote
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime,timezone
from pathlib import Path
import json,re,hashlib
root=Path(__file__).resolve().parents[1]
rows=json.loads((root/'app/src/main/assets/dinosaurs.json').read_text())
def audit(row):
    name=row['id'].capitalize(); url='https://en.wikipedia.org/wiki/'+name
    request=Request(url,headers={'User-Agent':'Kyoryu3D/1.0 (source attribution audit; https://github.com/hatake716/kyouryu-3d)'})
    with urlopen(request,timeout=30) as r:body=r.read();status=r.status;final=r.url
    text=body.decode('utf-8');revision=re.search(r'"wgRevisionId"\s*:\s*(\d+)',text) or re.search(r'Special:Redirect/revision/(\d+)',text) or re.search(r'oldid=(\d+)',text)
    ja='https://ja.wikipedia.org/wiki/'+quote(row['wikiTitle'])
    try:
        with urlopen(Request(ja,headers={'User-Agent':'Kyoryu3D/1.0'}),timeout=25) as j:ja_result={'url':ja,'status':j.status,'resolved_url':j.url}
    except Exception as e:ja_result={'url':ja,'error':str(e)}
    return dict(id=row['id'],title=name,author='Wikipedia contributors',url=url,history='https://en.wikipedia.org/w/index.php?title='+name+'&action=history',revision_id=revision.group(1) if revision else None,permalink='https://en.wikipedia.org/w/index.php?oldid='+revision.group(1) if revision else None,license='CC BY-SA 4.0',license_url='https://creativecommons.org/licenses/by-sa/4.0/',modification='Japanese summary, restructuring and supplemental factual notes',retrieved_utc=datetime.now(timezone.utc).isoformat(),status=status,sha256_source_html=hashlib.sha256(body).hexdigest(),japanese_link=ja_result)
results=list(ThreadPoolExecutor(max_workers=3).map(audit,rows))
(root/'docs/source-provenance.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
for r in results:print(r['id'],r['status'],r['revision_id'],r['japanese_link'].get('status',r['japanese_link'].get('error')))
