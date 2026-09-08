#!/usr/bin/env python3
"""Build licensed, indexed gallery LODs. Original observation meshes remain unchanged.
Run :app:exportTheaterSources first. Dependencies: numpy, Pillow, trimesh, scipy,
fast-simplification (versions are recorded with the source assets).
"""
from pathlib import Path
import gzip,struct,json,hashlib
import numpy as np
from PIL import Image
import trimesh
from scipy.spatial import cKDTree
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'app/src/main/assets/theater';OUT.mkdir(exist_ok=True)
DETAIL={'triceratops':75000,'tyrannosaurus':85000,'carnotaurus':80000,'patagotitan':100000}
manifest=[]
for d in json.loads((ROOT/'app/src/main/assets/dinosaurs.json').read_text()):
 id=d['id'];scan=id in DETAIL
 src=ROOT/('app/src/main/assets/specimens' if scan else 'artifacts/theater-1.6/sources')/(id+'.ksmz')
 raw=gzip.decompress(src.read_bytes());magic,count,metres,radius=struct.unpack('<4sIff',raw[:16]);assert magic==b'KSM1'
 a=np.frombuffer(raw, dtype='<f4',offset=16).reshape(count,9).copy();original_count=count//3
 colorfile=next(iter((ROOT/'app/src/main/assets/specimens').glob(id+'-color.*')),None) if scan else None
 if colorfile:
  with Image.open(colorfile) as im:im.thumbnail((1024,1024));pixels=np.asarray(im.convert('RGB'));h,w=pixels.shape[:2]
  # Imported UV v=0 matches the first uploaded bitmap row, just as in the observation renderer.
  colors=pixels[(a[:,8].clip(0,1)*(h-1)).astype(int),(a[:,7].clip(0,1)*(w-1)).astype(int)].astype(np.float32)/255
 else:
  grain=(np.sin(a[:,0]*11.2+a[:,1]*4.7)*np.sin(a[:,2]*15.3+a[:,1]*7.1))*.055
  colors=np.array([.72,.59,.40],dtype=np.float32)[None,:]+grain[:,None]
 if scan:
  p,inv=np.unique(a[:,:3],axis=0,return_inverse=True)
  mesh=trimesh.Trimesh(p,inv.reshape(-1,3),process=False)
  chunks=mesh.split(only_watertight=False);reduced=[]
  ratio=DETAIL[id]/original_count
  for m in chunks:
   target=max(16,int(len(m.faces)*ratio))
   if len(m.faces)>target:m=m.simplify_quadric_decimation(face_count=target,aggression=3)
   if len(m.faces):reduced.append(m)
  merged=trimesh.util.concatenate(reduced)
  _,nearest=cKDTree(a[:,:3]).query(merged.vertices,workers=2)
  normal=merged.vertex_normals.copy();bad=np.linalg.norm(normal,axis=1)<.1;normal[bad]=[0,1,0]
  v=np.column_stack((merged.vertices,normal,colors[nearest])).astype('<f4');faces=merged.faces
 else:
  joined=np.column_stack((a[:,:6],colors)).astype('<f4')
  v,inv=np.unique(joined,axis=0,return_inverse=True);faces=inv.reshape(-1,3)
 length=np.linalg.norm(v[:,3:6],axis=1);bad=length<1e-8;v[bad,3:6]=[0,1,0];length[bad]=1
 v[:,3:6]/=length[:,None]
 # Partition for core GLES2 unsigned-short indices on every supported device.
 parts=[];mapping={};verts=[];indices=[]
 for face in faces:
  if len(verts)+3>60000:
   parts.append((v[np.array(verts)],np.array(indices,dtype='<u2')));mapping={};verts=[];indices=[]
  for old in face:
   old=int(old)
   if old not in mapping:mapping[old]=len(verts);verts.append(old)
   indices.append(mapping[old])
 if verts:parts.append((v[np.array(verts)],np.array(indices,dtype='<u2')))
 payload=bytearray(struct.pack('<4sI',b'TMS1',len(parts)))
 for vertex,index in parts:payload+=struct.pack('<II',len(vertex),len(index))+vertex.astype('<f4').tobytes()+index.tobytes()
 path=OUT/(id+'.tmsz');path.write_bytes(gzip.compress(payload,compresslevel=9,mtime=0))
 lo=v[:,:3].min(0);hi=v[:,:3].max(0)
 # Fits each original virtual display island; no unverified true-size claims.
 scale=min(1.0,12.4/float((hi-lo)[0]),6.2/float((hi-lo)[2]))
 license='Apache-2.0';source='Original app reconstruction'
 if scan:
  p=json.loads((ROOT/'docs'/('specimen-provenance.json' if id=='triceratops' else id+'-provenance.json')).read_text());license=p['license'];source=p['objectUrl']
 manifest.append(dict(id=id,source=source,license=license,sourceSha256=hashlib.sha256(src.read_bytes()).hexdigest(),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),bytes=path.stat().st_size,triangles=len(faces),parts=len(parts),min=lo.tolist(),max=hi.tolist(),scale=scale,change=('Components simplified separately; source color sampled into vertex colors; ' if scan else 'Original generator with lower radial/longitudinal sampling; all anatomical components retained; ')+'indexed and split for GLES2; exhibition scale, not a measurement model'))
 print(id,len(faces),path.stat().st_size,flush=True)
(OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
(ROOT/'docs/theater-mesh-provenance.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
print('TOTAL',sum(m['bytes'] for m in manifest),'bytes',sum(m['triangles'] for m in manifest),'triangles')

# Restore shared metre calibration after every mesh regeneration.
from calibrate_scale import calibrate
calibrate()
