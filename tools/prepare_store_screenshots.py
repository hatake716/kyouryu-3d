#!/usr/bin/env python3
"""Export real adb captures as Play-compatible RGB PNGs without changing RGB pixels.

Requires Pillow. Run from any directory, with the raw capture directory as argument.
Never creates or synthesizes app UI. Captures must be reviewed before publishing.
"""
from pathlib import Path
import argparse, hashlib, json
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SCENES = [
    ('01-portrait-museum', '博物館の入口。森林と天窓に囲まれた展示室で、トリケラトプスなどの骨格を一人称視点で観察。'),
    ('02-portrait-collection', '白亜紀の世界地図とコレクション。時代を選び、ティラノサウルスやスピノサウルスのカードから探せます。'),
    ('03-portrait-skeleton', 'ブラキオサウルスの学習用骨格を縦の全画面で表示。首・脚・尾と種類ごとの化石の質感を観察できます。'),
    ('04-portrait-encyclopedia', 'ティラノサウルスの日本語図鑑。年代や産地に加え、名前の由来と命名の歴史を出典付きで紹介。'),
    ('05-landscape-museum', '横画面の博物館。トリケラトプスの公開標本を大きく眺め、名前札や復元イラストから詳しく調べられます。'),
    ('06-landscape-skeleton', 'ティラノサウルスの展示レプリカの公開スキャン。横画面で全身を観察し、右のカードから視点や部位を選択。'),
    ('07-landscape-illustration', 'トリケラトプスの復元イラストを横画面で表示。2本指で拡大・移動でき、作者と出典も確認できます。'),
    ('08-landscape-spinosaurus', 'スピノサウルスの学習用骨格。長い吻と背中の棘を、復元イラストを背景にさまざまな角度から観察。'),
]

def sha(data):
    return hashlib.sha256(data).hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('raw_directory', type=Path)
    args = parser.parse_args()
    records = []
    for order, (name, alt) in enumerate(SCENES, 1):
        source = args.raw_directory / (name + '.png')
        raw = source.read_bytes()
        orientation = 'portrait' if 'portrait' in name else 'landscape'
        size = (1080, 1920) if orientation == 'portrait' else (1920, 1080)
        with Image.open(source) as capture:
            assert capture.size == size, f'{source}: unexpected dimensions {capture.size}'
            assert capture.mode in ('RGB', 'RGBA'), f'{source}: expected RGB(A)'
            if capture.mode == 'RGBA':
                assert capture.getchannel('A').getextrema() == (255, 255), f'{source}: non-opaque pixels'
            rgb = capture.convert('RGB')
            destination = ROOT / 'docs/store/screenshots' / orientation / (name + '.png')
            destination.parent.mkdir(parents=True, exist_ok=True)
            rgb.save(destination, format='PNG', optimize=True)
            with Image.open(destination) as exported:
                assert exported.tobytes() == rgb.tobytes(), 'RGB pixels changed'
            records.append(dict(order=order, file=destination.relative_to(ROOT / 'docs/store').as_posix(),
                                width=size[0], height=size[1], mode='RGB', altJa=alt,
                                rawSha256=sha(raw), sha256=sha(destination.read_bytes()),
                                rgbPixelsSha256=sha(rgb.tobytes()), rgbPixelsUnchanged=True))
    manifest = dict(capturedDate='2026-09-08', appVersion='1.10.1', versionCode=12,
                    apkSha256='49b92c8aa66a7e322c351cded57be703ac5a41f7bc00e87c24f8c0ffdc302f73',
                    device='Android Emulator, API 35, SwiftShader', densityDpi=420,
                    capture='adb exec-out screencap -p; real signed release APK; UI navigation only',
                    systemUi='Android System UI demo mode: 10:00, battery 100%, notifications hidden',
                    postprocess='PNG RGBA to RGB only; opaque alpha removed, RGB pixels preserved; no resize, crop, overlays or AI edits',
                    screenshots=records)
    (ROOT/'docs/store/screenshot-manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+'\n')
    print(f'Exported {len(records)} real screenshots; all RGB pixels unchanged')

if __name__ == '__main__':
    main()
