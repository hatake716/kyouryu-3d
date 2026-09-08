#!/usr/bin/env python3
"""Import Rigsters' CC BY 4.0 scan from the public Objaverse archive.
Keeps all archived triangles and the original color image, applies only rigid
orientation plus reversible display normalization. Source scale is unverified.
Requires numpy. Source author, transformations and checksums are recorded.
"""
import argparse,gzip,hashlib,json,struct
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[2]
UID='77e21c4e5d65468bbe11780ae0557b95'
URL='https://huggingface.co/datasets/allenai/objaverse/resolve/main/glbs/000-033/'+UID+'.glb'
def main():
 ap=argparse.ArgumentParser();ap.add_argument('glb',type=Path);a=ap.parse_args();raw=a.glb.read_bytes()
 assert hashlib.sha256(raw).hexdigest()=='23d5c3b6d5d5ee2c2287c726165c8b43eb314e210beb628f519865b704aebc30'
 assert raw[:4]==b'glTF';size=struct.unpack_from('<I',raw,12)[0];j=json.loads(raw[20:20+size]);buf=raw[28+size:]
 def acc(i):
  a=j['accessors'][i];v=j['bufferViews'][a['bufferView']]
  width={'VEC3':3,'VEC2':2,'SCALAR':1}[a['type']]
  dtype=np.dtype({5126:'<f4',5125:'<u4',5123:'<u2'}[a['componentType']])
  return np.ndarray((a['count'],width),dtype=dtype,buffer=buf,offset=v.get('byteOffset',0)+a.get('byteOffset',0),strides=(v.get('byteStride',width*dtype.itemsize),dtype.itemsize))
 data=[]
 def node(i,parent):
  n=j['nodes'][i];assert not any(k in n for k in ['translation','rotation','scale','skin'])
  mat=parent@np.array(n.get('matrix',np.eye(4).ravel()),dtype=float).reshape(4,4).T
  if 'mesh' in n:
   for primitive in j['meshes'][n['mesh']]['primitives']:
    attr=primitive['attributes'];ix=acc(primitive['indices']).ravel();pos=acc(attr['POSITION'])[ix]@mat[:3,:3].T+mat[:3,3]
    normal=acc(attr['NORMAL'])[ix]@np.linalg.inv(mat[:3,:3]);normal/=np.linalg.norm(normal,axis=1)[:,None]
    data.append((pos,normal,acc(attr['TEXCOORD_0'])[ix]))
  for c in n.get('children',[]):node(c,mat)
 for i in j['scenes'][j['scene']]['nodes']:node(i,np.eye(4))
 p=np.concatenate([d[0] for d in data]);n=np.concatenate([d[1] for d in data]);uv=np.concatenate([d[2] for d in data])
 angle=np.deg2rad(32);rotation=np.array([[np.sin(angle),0,np.cos(angle)],[0,1,0],[-np.cos(angle),0,np.sin(angle)]])
 p=p@rotation.T;n=n@rotation.T;lo=p.min(0);hi=p.max(0);scale=8/float((hi-lo).max());p=(p-(lo+hi)*.5)*scale
 x,y,z=p.reshape(-1,3,3).mean(1).T
 group=np.full(len(x),2,np.float32)
 group[(x>-.55)&(x<1.9)&(y<.78)&(y>-.30)]=3
 group[(x>-.70)&(x<.55)&(y>.0)&(y<.73)]=4
 group[(x>-.9)&(x<1.55)&(y<-.27)]=6
 group[(x>.95)&(x<2.15)&(y<.32)&(y>-.30)]=5
 group[x>2.05]=1
 mesh=np.column_stack((p,n,np.repeat(group,3),uv)).astype('<f4');radius=float(np.linalg.norm(p,axis=1).max())
 out=ROOT/'app/src/main/assets/specimens';out.mkdir(exist_ok=True)
 name='tyrannosaurus.ksmz';(out/name).write_bytes(gzip.compress(b'KSM1'+struct.pack('<Iff',len(mesh),0,radius)+mesh.tobytes(),mtime=0,compresslevel=9))
 v=j['bufferViews'][j['images'][0]['bufferView']];start=v.get('byteOffset',0);tex='tyrannosaurus-color.jpg';(out/tex).write_bytes(buf[start:start+v['byteLength']])
 manifest={'id':'tyrannosaurus','taxon':'Tyrannosaurus rex','author':'Rigsters','title':'Tyrannosaurus Rex skeleton',
 'sourceUrl':URL,'objectUrl':'https://sketchfab.com/3d-models/tyrannosaurus-rex-skeleton-'+UID,
 'license':'CC-BY-4.0','licenseUrl':'https://creativecommons.org/licenses/by/4.0/',
 'sourceSha256':hashlib.sha256(raw).hexdigest(),'reviewed':'2026-09-07',
 'description':'Photogrammetry of a mounted replica at Knuthenborg Safaripark, Denmark; 133 photographs. Supplied by Rigsters under CC BY 4.0. Source archive may differ from the current Sketchfab version. Surfaces hidden from photographs may be incomplete.',
 'triangles':len(mesh)//3,'metresPerDisplayUnit':None,'sourceUnits':'unverified; glTF coordinate units are not evidence of physical scale',
 'changes':'All archived triangle geometry, UVs and color image retained. Node transforms applied; rigid rotation, centering and display normalization. Approximate spatial region IDs added. No measured lengths from another specimen applied.',
 'rotationRows':rotation.tolist(),'displayScale':scale,'orientedSourceBounds':[lo.tolist(),hi.tolist()],
 'outputs':{f:{'bytes':(out/f).stat().st_size,'sha256':hashlib.sha256((out/f).read_bytes()).hexdigest()} for f in [name,tex]}}
 (ROOT/'docs/tyrannosaurus-provenance.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
 print('Imported',len(mesh)//3,'triangles; groups',np.unique(group),'radius',radius)
if __name__=='__main__':main()
