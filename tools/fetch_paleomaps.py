#!/usr/bin/env python3
"""Fetch the three reviewed CC BY 4.0 PALEOMAP images and retain their provenance.

Optional research task, not part of the offline app build. Standard Python only.
"""
from pathlib import Path
import datetime
import hashlib
import html
import json
import re
import ssl
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
FILES = [
    ("triassic", 225, "File:Mollweide Paleographic Map of Earth, 225 Ma (Norian Age).png"),
    ("jurassic", 155, "File:Mollweide Paleographic Map of Earth, 155 Ma (Oxfordian Age).png"),
    ("cretaceous", 100, "File:100Ma Mollweide Paleographic Map of Earth.jpg"),
]
CTX = ssl.create_default_context()
HEADERS = {"User-Agent": "Kyoryu3D/1.2 (https://github.com/hatake716/kyouryu-3d; offline educational map assets)"}


def fetch(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=HEADERS), context=CTX, timeout=60) as response:
        return response.read()


def plain(value):
    return html.unescape(re.sub(r"<[^>]*>", "", value)).strip()


if __name__ == "__main__":
    records = []
    for identity, age, title in FILES:
        api = "https://commons.wikimedia.org/w/api.php?" + urllib.parse.urlencode({
            "action": "query", "format": "json", "titles": title,
            "prop": "imageinfo|revisions", "rvprop": "ids", "iiprop": "url|extmetadata|sha1|size", "iiurlwidth": 1280,
        })
        response = fetch(api)
        page = next(iter(json.loads(response)["query"]["pages"].values()))
        info = page["imageinfo"][0]
        meta = info["extmetadata"]
        assert plain(meta["LicenseShortName"]["value"]) == "CC BY 4.0", title
        url = info["thumburl"]
        data = fetch(url)
        asset = f"maps/{identity}." + ("jpg" if title.endswith(".jpg") else "png")
        target = ROOT / "app/src/main/assets" / asset
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
        records.append({
            "id": identity, "mapAgeMa": age, "asset": asset, "fileTitle": title,
            "artist": plain(meta["Artist"]["value"]), "credit": plain(meta["Credit"]["value"]),
            "license": "CC BY 4.0", "licenseUrl": "https://creativecommons.org/licenses/by/4.0/",
            "source": info["descriptionurl"], "revision": page["revisions"][0]["revid"],
            "dataset": "https://doi.org/10.5281/zenodo.10659112", "url": url,
            "originalUrl": info["url"], "originalSha1": info["sha1"],
            "sha256": hashlib.sha256(data).hexdigest(), "apiResponseSha256": hashlib.sha256(response).hexdigest(),
            "width": info["thumbwidth"], "height": info["thumbheight"],
            "retrieved": datetime.date.today().isoformat(),
            "modifications": "Wikimedia thumbnail downloaded unchanged. Display scaled to fit; no recoloring or cropping.",
        })
        print(identity, len(data), "bytes")
    text = json.dumps(records, ensure_ascii=False, indent=2) + "\n"
    (ROOT / "docs/paleomap-provenance.json").write_text(text)
    (ROOT / "app/src/main/assets/maps.json").write_text(text)
