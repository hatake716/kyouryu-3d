#!/usr/bin/env python3
"""Import reviewed CC-licensed reconstructions; never assign an unverified metre scale.
trimesh 5.1.0, numpy 2.5.3, fast-simplification 0.2.0.
Each output retains the original author's asset license (including ShareAlike).
"""
import argparse,gzip,hashlib,json,struct
from pathlib import Path
import numpy as np
import trimesh
ROOT=Path(__file__).resolve().parents[2]
CONFIG={
 'patagotitan':dict(uid='a48cde44e8a045d2968fca28ce62c7ae',shard='000-118',sha='b762040a14743d4de82a3d10314de036d78480f6aec82d238dea43c979d33c5c',author='MebaLab / lautaro.cb',title='Patagotitan mayorum by MebaLab',license='CC-BY-SA-4.0',licenseUrl='https://creativecommons.org/licenses/by-sa/4.0/',triangles=230000,color=False,flip=False,axis='z'),
 'carnotaurus':dict(uid='04c8d6e0e1794091aeae0d2f54cca43b',shard='000-057',sha='60cef552003deebe6f19e7ecde294564e8549e5be7680b2134a81c27a4dca471',author='Magorius',title='Carnotaurus sastrei (skeleton)',license='CC-BY-4.0',licenseUrl='https://creativecommons.org/licenses/by/4.0/',triangles=260854,color=True,flip=False),
}
def main():
 ap=argparse.ArgumentParser();ap.add_argument('id',choices=CONFIG);ap.add_argument('glb',type=Path);args=ap.parse_args();c=CONFIG[args.id]
 raw=args.glb.read_bytes();assert hashlib.sha256(raw).hexdigest()==c['sha']
 scene=trimesh.load(args.glb,force='scene',process=False);parts=list(scene.dump())
 source_count=sum(len(m.faces) for m in parts)
 if not c['color']:
  reduced=[]
  for mesh in parts:
   mesh.merge_vertices();mesh.update_faces(mesh.unique_faces());mesh.update_faces(mesh.nondegenerate_faces())
   budget=max(350,int(len(mesh.faces)*c['triangles']/source_count))
   if len(mesh.faces)>budget:mesh=mesh.simplify_quadric_decimation(face_count=budget,aggression=3)
   reduced.append(mesh)
  parts=reduced
 pos=[];norm=[];tex=[]
 for m in parts:
  pos.append(m.vertices[m.faces].reshape(-1,3));normals=m.vertex_normals[m.faces].reshape(-1,3).copy();bad=np.linalg.norm(normals,axis=1)<.5
  normals[bad]=np.repeat(m.face_normals,3,axis=0)[bad];bad=np.linalg.norm(normals,axis=1)<.5;normals[bad]=[0,1,0];normals/=np.linalg.norm(normals,axis=1)[:,None];norm.append(normals)
  uv=m.visual.uv.copy() if c['color'] else np.zeros((len(m.vertices),2))
  if c['color']:uv[:,1]=1-uv[:,1] # undo trimesh's texture-coordinate conversion
  tex.append(uv[m.faces].reshape(-1,2))
 p=np.concatenate(pos);n=np.concatenate(norm);uv=np.concatenate(tex)
 if c.get('axis')=='z':p=p[:,[2,1,0]]*np.array([1,1,-1]);n=n[:,[2,1,0]]*np.array([1,1,-1])
 if c['flip']:p[:,[0,2]]*=-1;n[:,[0,2]]*=-1
 lo=p.min(0);hi=p.max(0);scale=8/float((hi-lo).max());p=(p-(lo+hi)*.5)*scale
 x,y,z=p.reshape(-1,3,3).mean(1).T;group=np.full(len(x),2,np.float32)
 group[(x>-.6)&(x<2.5)&(y>-.35)&(y<.92)]=3
 group[(x>-.9)&(x<.40)&(y>.17)]=4
 group[(x>-.85)&(x<1.5)&(y<-.35)]=6
 group[(x>1.5)&(x<2.5)&(y<.30)]=5
 group[x>2.7]=1
 if args.id=='patagotitan':
  group[:]=2;group[(x>-.25)&(x<1.2)&(y>-.10)&(y<.60)]=3
  group[(x>-.55)&(x<1.3)&(y>.1)&(y<.80)&(abs(z)>.18)]=4
  group[(x>-.65)&(x<.6)&(y<.10)]=6;group[(x>.6)&(x<1.65)&(y<.10)]=5;group[x>3.2]=1
 mesh=np.column_stack((p,n,np.repeat(group,3),uv)).astype('<f4');radius=float(np.linalg.norm(p,axis=1).max())
 out=ROOT/'app/src/main/assets/specimens';name=args.id+'.ksmz';(out/name).write_bytes(gzip.compress(b'KSM1'+struct.pack('<Iff',len(mesh),0,radius)+mesh.tobytes(),mtime=0,compresslevel=9));files=[name]
 if c['color']:
  size=struct.unpack_from('<I',raw,12)[0];j=json.loads(raw[20:20+size]);buf=raw[28+size:];index=j['materials'][0]['pbrMetallicRoughness']['baseColorTexture']['index'];image=j['images'][j['textures'][index]['source']];assert image['mimeType'] in ['image/jpeg','image/png']
  v=j['bufferViews'][image['bufferView']];start=v.get('byteOffset',0);name=args.id+'-color.'+('jpg' if image['mimeType']=='image/jpeg' else 'png');(out/name).write_bytes(buf[start:start+v['byteLength']]);files.append(name)
 manifest={k:v for k,v in c.items() if k in ['uid','author','title','license','licenseUrl']}
 manifest.update(id=args.id,sourceUrl=f"https://huggingface.co/datasets/allenai/objaverse/resolve/main/glbs/{c['shard']}/{c['uid']}.glb",objectUrl=f"https://sketchfab.com/3d-models/{c['uid']}",sourceSha256=c['sha'],reviewed='2026-09-08',sourceTriangles=source_count,triangles=len(mesh)//3,metresPerDisplayUnit=None,
  changes='Authored reconstruction, not a validated fossil scan. Node transforms applied, centered and normalized for display. Approximate coordinate regions assigned. Source physical units unverified. '+('All archived triangles and original color image retained; material uses app matte lighting.' if c['color'] else 'Vertices welded and duplicate/degenerate faces removed within each node; each bone mesh simplified separately with quadric error decimation; normals recalculated; opaque procedural fossil material replaces original material.'),
  outputs={f:dict(bytes=(out/f).stat().st_size,sha256=hashlib.sha256((out/f).read_bytes()).hexdigest(),license=c['license']) for f in files})
 (ROOT/'docs'/f'{args.id}-provenance.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
 print(args.id,'triangles',len(mesh)//3,'groups',np.unique(group),'radius',radius)
if __name__=='__main__':main()
