#!/usr/bin/env python3
"""Bundle only allowlisted Play documents/images; verify the existing signed binaries.

Does not build, sign, upload, include keystores, or publish a release.
"""
from pathlib import Path
import hashlib, json, re, subprocess, zipfile
ROOT = Path(__file__).resolve().parents[1]

def sha(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024*1024), b''):
            h.update(chunk)
    return h.hexdigest()

def main():
    subprocess.run(['python3',str(ROOT/'tools/check_store_assets.py')],check=True)
    out = ROOT/'artifacts/release'
    artifacts = json.loads((ROOT/'docs/build-artifacts.json').read_text())
    for artifact in artifacts:
        path = out/artifact['file']
        assert path.stat().st_size == artifact['bytes'] and sha(path) == artifact['sha256'], path
    version = re.search(r'versionName = "([^"]+)"', (ROOT/'app/build.gradle.kts').read_text()).group(1)
    files = {ROOT/'LICENSE', ROOT/'docs/site.css', ROOT/'docs/index.html'}
    files.update(p for p in (ROOT/'docs/store').iterdir() if p.is_file())
    for folder in ['docs/store/ja-JP','docs/store/screenshots','docs/privacy','docs/parents','docs/credits','docs/verification/play-preparation','docs/verification/1.10.1',f'docs/verification/{version}']:
        files.update(p for p in (ROOT/folder).rglob('*') if p.is_file())
    for name in ['PLAY_STORE','PLAY_CONSOLE','RELEASE','CONTENT_LICENSE','ARTWORK_CREDITS','MODEL_ACCURACY','PALEOMAPS','RESEARCH','GENERATED_ARTWORK','FOSSIL_SURFACES','THEATER','POPULARITY']:
        files.add(ROOT/'docs'/f'{name}.md')
    files.update((ROOT/'docs').glob('*.json'))
    files.add(ROOT/'app/src/main/assets/licenses.txt')
    files.add(ROOT/'app/src/main/assets/privacy.txt')
    for name in ['check_store_assets.py','package_play_release.py','prepare_store_screenshots.py']:
        files.add(ROOT/'tools'/name)
    forbidden = ['.jks','.keystore','.properties','.apk','.aab']
    assert all(p.suffix not in forbidden and not p.is_symlink() for p in files)
    kit = out/f'kyouryu-3d-{version}-play-kit.zip'
    intro = '''恐竜の骨格３D — Google Play公開準備キット

最初に docs/PLAY_STORE.md と docs/PLAY_CONSOLE.md を確認してください。
スマートフォン用の画像は docs/store/screenshots/ 内の縦4枚・横4枚です。
権利表示は docs/store/CREDITS.md と docs/CONTENT_LICENSE.md を参照してください。
APK/AABはZIPに入れず、同じリリース出力フォルダーへ個別に配置しています。
Google Playへのアップロード、審査、公開は未実施です。

このキットは公開準備に必要な資料の抜粋です。過去の検証画像やアプリの
完全なソースは https://github.com/hatake716/kyouryu-3d を参照してください。
公開サイト: https://hatake716.github.io/kyouryu-3d/
サポート: acesmash@gmail.com
'''
    with zipfile.ZipFile(kit,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as archive:
        archive.writestr('START-HERE.txt',intro)
        for path in sorted(files):
            archive.write(path,path.relative_to(ROOT))
    assert zipfile.ZipFile(kit).testzip() is None
    sums = [f'{sha(out/a["file"])}  {a["file"]}' for a in artifacts]
    sums.append(f'{sha(kit)}  {kit.name}')
    (out/f'SHA256SUMS-{version}.txt').write_text('\n'.join(sums)+'\n')
    print(json.dumps({'kit':kit.name,'files':len(files)+1,'bytes':kit.stat().st_size,'sha256':sha(kit)},indent=2))

if __name__ == '__main__':
    main()
