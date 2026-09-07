import json
from pathlib import Path
from profiles import PROFILES
ROOT=Path(__file__).resolve().parents[2]
TYPES=dict(base='String',neck='Float',neckRise='Float',head='Float',headHeight='Float',headWidth='Float',body='Float',arm='Float',claw='Float',tail='Float',sail='Float',crest='String',teeth='Boolean?',shoulder='Float',quadruped='Boolean?',fingers='Int',club='Boolean',brow='Float',nose='Float',frillSpikes='Int',frill='Float',fenestrae='Boolean',flipper='Float')
DEFAULTS=dict(base='"allosaurus"',neck='1f',neckRise='1f',head='1f',headHeight='1f',headWidth='1f',body='1f',arm='1f',claw='1f',tail='1f',sail='-1f',crest='"default"',teeth='null',shoulder='0f',quadruped='null',fingers='0',club='true',brow='1f',nose='1f',frillSpikes='0',frill='1f',fenestrae='false',flipper='1f')
def kt(k,v):
 if TYPES[k]=='Float':return str(float(v))+'f'
 return json.dumps(v,ensure_ascii=False)
s='package io.github.hatake716.dinobones.model\n\n// Generated from authored conceptual profiles; these are not fossil measurements.\n'
s+='data class ModelProfile('+','.join(f'val {k}:{t}={DEFAULTS[k]}' for k,t in TYPES.items())+')\n'
s+='object ModelProfiles { val extras=mapOf(\n'
selected={r['title'].lower() for r in json.loads((ROOT/'docs/selected-taxa.json').read_text())}
for name,p in PROFILES.items():
 if name in selected:s+=f'    "{name}" to ModelProfile('+','.join(f'{k}={kt(k,v)}' for k,v in p.items())+'),\n'
s+=') }\n'
(ROOT/'app/src/main/java/io/github/hatake716/dinobones/model/ModelProfiles.kt').write_text(s)
