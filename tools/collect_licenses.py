"""Run after Gradle legalExport. Preserve notices in the shipped runtime jars/aars."""
import hashlib,io,json,zipfile,re
from pathlib import Path
root=Path(__file__).resolve().parents[1]
manifest=[];notices={}
def inspect(raw):
    result=[]
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        for n in z.namelist():
            if re.search(r'(^|/)(LICENSE[^/]*|NOTICE[^/]*|[^/]*\.LICENSE[^/]*)$',n,re.I):
                try:result.append((n,z.read(n).decode('utf-8')))
                except UnicodeDecodeError:pass
            if n=='classes.jar': result.extend(inspect(z.read(n)))
    return result
for line in (root/'artifacts/legal-artifacts.txt').read_text().splitlines():
    coordinate,path=line.split('\t');p=Path(path);body=p.read_bytes();embedded=inspect(body)
    manifest.append(dict(coordinate=coordinate,artifact=p.name,sha256=hashlib.sha256(body).hexdigest(),embedded_notice_paths=[n for n,t in embedded]))
    for name,text in embedded:
        if len(text.strip())>30:
            notices.setdefault(text,[]).append(coordinate+' / '+name)
asset=root/'app/src/main/assets/licenses.txt';base=asset.read_text().split('\n\nRUNTIME ARTIFACT NOTICES\n')[0]
append='\n\nRUNTIME ARTIFACT NOTICES\n\n'+ '\n'.join(m['coordinate'] for m in manifest)+'\n'
for text,coordinates in notices.items():append+='\n\n'+'\n'.join(coordinates)+'\n\n'+text
asset.write_text(base+append)
(root/'docs/runtime-dependencies.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
print(len(manifest),'runtime artifacts;',len(notices),'distinct embedded notices; bundled text',len(base+append),'characters')
