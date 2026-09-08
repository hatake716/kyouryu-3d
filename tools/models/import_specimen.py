#!/usr/bin/env python3
"""Reproducible GLB -> GLES mesh. Source is CC0 Smithsonian Open Access.
No anatomical edits or extra decimation. Rigid axis permutation only; keep metre scale.
Textures are extracted byte-for-byte. Region labels are approximate, per triangle.
Run with Python + numpy. Download URL and hashes are recorded in the manifest.
"""
import argparse, gzip, hashlib, json, struct
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[2]
SOURCE='Triceratops_horridus_Marsh_1889-150k-4096.glb'
BASE='https://smithsonian-open-access.s3-us-west-2.amazonaws.com/3d/d8c623be-4ebc-11ea-b77f-2e728ce88125/'
def main():
    parser=argparse.ArgumentParser();parser.add_argument('glb',type=Path);args=parser.parse_args()
    raw=args.glb.read_bytes();assert hashlib.sha256(raw).hexdigest()=='cd8525c7371d14876bcdefe3f579d216bf597db892c2de6ed597da86bf1a98b3', 'Unreviewed input GLB'
    assert raw[:4]==b'glTF'
    size=struct.unpack_from('<I',raw,12)[0];j=json.loads(raw[20:20+size]);buf=raw[28+size:]
    def accessor(n):
        a=j['accessors'][n];v=j['bufferViews'][a['bufferView']]
        assert 'byteStride' not in v
        width={'VEC3':3,'VEC2':2,'SCALAR':1}[a['type']]
        return np.frombuffer(buf,dtype={5126:'<f4',5125:'<u4'}[a['componentType']],offset=v.get('byteOffset',0)+a.get('byteOffset',0),count=a['count']*width).reshape(a['count'],width)
    primitive=j['meshes'][0]['primitives'][0];attr=primitive['attributes'];idx=accessor(primitive['indices']).ravel()
    # Source GLB: +Z towards head, +X up, +Y lateral. Proper rigid rotation (det = +1).
    p=accessor(attr['POSITION'])[:,[2,0,1]];normal=accessor(attr['NORMAL'])[:,[2,0,1]]
    lo=p.min(0);hi=p.max(0);scale=8/float((hi-lo).max());center=(hi+lo)*.5
    tri=p[idx].reshape(-1,3,3);c=tri.mean(1);x,y,z=c.T
    group=np.full(len(c),2,dtype=np.float32)
    group[(x>-.95)&(x<.65)&(abs(z)>.15)&(y<.6)]=3
    group[((x<-.8)&(x>-1.6)&(y>-.35))|((x>.25)&(x<.85)&(y>-.35)&(y<.7)&(abs(z)>.25))]=4
    group[(x<-.6)&(x>-1.9)&(y<-.35)]=6
    group[(x>-.3)&(x<1.25)&(y<-.45)]=5
    group[x>1.1]=1
    # Frill and horn cores remain in the skull region: anatomical segmentation is not supplied by SI.
    mesh=np.column_stack(((p[idx]-center)*scale,normal[idx],np.repeat(group,3),accessor(attr['TEXCOORD_0'])[idx])).astype('<f4')
    radius=float(np.linalg.norm(mesh[:,:3],axis=1).max())
    out=ROOT/'app/src/main/assets/specimens';out.mkdir(exist_ok=True)
    payload=b'KSM1'+struct.pack('<Iff',len(mesh),1/scale,radius)+mesh.tobytes()
    (out/'triceratops.ksmz').write_bytes(gzip.compress(payload,compresslevel=9,mtime=0))
    paths=['triceratops.ksmz']
    for i,name in enumerate(['color','occlusion','normal']):
        view=j['bufferViews'][j['images'][i]['bufferView']];start=view.get('byteOffset',0)
        filename=f'triceratops-{name}.jpg';(out/filename).write_bytes(buf[start:start+view['byteLength']]);paths.append(filename)
    manifest={
        'taxon':'Triceratops horridus','institution':'Smithsonian Institution, National Museum of Natural History',
        'objectUrl':'https://3d.si.edu/object/3d/triceratops-horridus-marsh-1889:d8c623be-4ebc-11ea-b77f-2e728ce88125',
        'sourceUrl':BASE+'resources/'+SOURCE,'metadataUrl':BASE+'scene.svx.json',
        'sourceSha256':hashlib.sha256(raw).hexdigest(),'license':'CC0-1.0','licenseUrl':'https://creativecommons.org/publicdomain/zero/1.0/',
        'rightsEvidence':['https://www.si.edu/openaccess/faq','https://commons.wikimedia.org/w/index.php?title=File:Triceratops-full_resolution-obj_(Smithsonian_Institute).stl&oldid=881096487'],
        'reviewed':'2026-09-07','sourceUnits':'m','unitEvidence':'scene.svx.json: models[0].units and scenes[0].units',
        'triangles':len(idx)//3,'sourceBoundsMetres':{'min':lo.tolist(),'max':hi.tolist()},'axisPermutation':[2,0,1],
        'metresPerDisplayUnit':1/scale,'changes':'Rigid axis rotation, centering and reversible display normalization; no further decimation. Original triangle positions, UVs and texture bytes retained. Approximate spatial region IDs added; not bone segmentation. Composite/restored museum skeleton, not a complete single individual.',
        'outputs':{name:{'sha256':hashlib.sha256((out/name).read_bytes()).hexdigest(),'bytes':(out/name).stat().st_size} for name in paths}
    }
    (ROOT/'docs/specimen-provenance.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({'triangles':manifest['triangles'],'boundsMetres':(hi-lo).tolist(),'outputs':manifest['outputs']},indent=2))
if __name__=='__main__':main()
