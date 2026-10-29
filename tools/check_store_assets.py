#!/usr/bin/env python3
"""Check the versioned Play listing, screenshot hashes and PNG upload constraints."""
from pathlib import Path
import hashlib, json, re, struct, zlib
ROOT = Path(__file__).resolve().parents[1]
STORE = ROOT / 'docs/store'

def png_info(path):
    data = path.read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n', f'{path}: not PNG'
    position = 8
    header = None
    while position < len(data):
        length = struct.unpack('>I', data[position:position+4])[0]
        kind = data[position+4:position+8]
        payload = data[position+8:position+8+length]
        expected_crc = struct.unpack('>I', data[position+8+length:position+12+length])[0]
        assert zlib.crc32(kind+payload) & 0xffffffff == expected_crc, f'{path}: corrupt PNG'
        if kind == b'IHDR':
            header = struct.unpack('>IIBBBBB', payload)
        assert kind != b'tRNS', f'{path}: transparent palette not supported'
        position += 12 + length
        if kind == b'IEND':
            break
    assert header and position == len(data), f'{path}: incomplete PNG'
    return header[:4]

def main():
    lengths = {}
    for name, maximum in [('title',30), ('short-description',80), ('full-description',4000), ('release-notes',500)]:
        text = (STORE/'ja-JP'/f'{name}.txt').read_text().strip()
        assert 0 < len(text) <= maximum, (name, len(text), maximum)
        assert not any(s in text for s in ['TODO', 'YOUR_', '要設定']), f'{name}: placeholder'
        lengths[name] = len(text)
    width,height,depth,color = png_info(STORE/'icon-512.png')
    assert (width,height,depth) == (512,512,8) and color in (2,6)
    assert (STORE/'icon-512.png').stat().st_size <= 1024*1024
    assert png_info(STORE/'feature-1024x500.png') == (1024,500,8,2)
    manifest = json.loads((STORE/'screenshot-manifest.json').read_text())
    screenshots = manifest['screenshots']
    assert len(screenshots) == 8
    files = {p.relative_to(STORE).as_posix() for p in (STORE/'screenshots').rglob('*.png')}
    assert {s['file'] for s in screenshots} == files
    assert len([s for s in screenshots if s['width'] < s['height']]) == 4
    assert sorted(s['order'] for s in screenshots) == list(range(1,9))
    for s in screenshots:
        path = STORE / s['file']
        assert png_info(path) == (s['width'],s['height'],8,2), s['file']
        assert sorted((s['width'],s['height'])) == [1080,1920]
        assert 0 < len(s['altJa']) <= 140, s['file']
        assert hashlib.sha256(path.read_bytes()).hexdigest() == s['sha256'], s['file']
        assert s['rgbPixelsUnchanged']
    gradle = (ROOT/'app/build.gradle.kts').read_text()
    version = re.search(r'versionName = "([^"]+)"', gradle).group(1)
    code = int(re.search(r'versionCode = (\d+)', gradle).group(1))
    capture_artifacts = ROOT/'docs/build-artifacts.json'
    if version != manifest['appVersion'] or code != manifest['versionCode']:
        # Reusing screenshots requires an explicit review for this release, while
        # retaining the capture version and original binary's provenance.
        review = json.loads((STORE/'release-compatibility.json').read_text())
        assert (review['appVersion'],review['versionCode']) == (version,code)
        assert (review['screenshotAppVersion'],review['screenshotVersionCode']) == (manifest['appVersion'],manifest['versionCode'])
        assert manifest['versionCode'] < code and review['reason'].strip()
        capture_artifacts = ROOT/'docs/verification'/manifest['appVersion']/'build-artifacts.json'
    artifacts = json.loads(capture_artifacts.read_text())
    capture_name = f"kyouryu-3d-{manifest['appVersion']}.apk"
    assert next(a['sha256'] for a in artifacts if a['file']==capture_name) == manifest['apkSha256']
    for page in ['index.html','privacy/index.html','parents/index.html','credits/index.html']:
        text = (ROOT/'docs'/page).read_text()
        assert '<html lang="ja">' in text and 'acesmash@gmail.com' in text, page
        assert '<script' not in text.lower() and '<iframe' not in text.lower(), page
    print(json.dumps(dict(result='passed', listingCharacters=lengths, screenshots=8,
                          portrait='4 x 1080x1920 RGB PNG', landscape='4 x 1920x1080 RGB PNG',
                          feature='1024x500 RGB PNG', icon='512x512 PNG',
                          releaseVersion=version, screenshotVersion=manifest['appVersion'], captureApkSha256=manifest['apkSha256']),ensure_ascii=False,indent=2))

if __name__ == '__main__':
    main()
