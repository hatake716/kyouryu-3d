# Model data generation

Run from the repository root with Python 3; `import_specimen.py` also needs numpy. No conversion dependency is shipped in the app. Download inputs to the ignored `artifacts/research-models/` directory:

- `Triceratops_horridus_Marsh_1889-150k-4096.glb`: https://smithsonian-open-access.s3-us-west-2.amazonaws.com/3d/d8c623be-4ebc-11ea-b77f-2e728ce88125/resources/Triceratops_horridus_Marsh_1889-150k-4096.glb
- `cursorial.xml`: https://www.ebi.ac.uk/europepmc/webservices/rest/PMC4728391/fullTextXML
- `stego-xml.txt`: https://journals.plos.org/plosone/article/file?id=10.1371/journal.pone.0138352&type=manuscript

The scripts write checked-in assets, provenance and generated Kotlin. Sources, licenses, scope and hashes are in `docs/MODEL_ACCURACY.md`, `docs/specimen-provenance.json` and `docs/bone-measurements.json`. Never silently replace the source with a differently scaled model or a noncommercial licensed scan.

KSM1: little endian magic `KSM1`, uint32 vertex count, float32 metres per display unit, float32 bounding radius, then 9 float32 per expanded vertex (position XYZ, normal XYZ, group, UV). Entire file is gzip compressed with mtime 0. Triangle positions, normals, UVs and extracted JPEG bytes remain traceable to the original GLB. Grid labels reverse the display normalization.


## 1.5.0の追加モデル

各入力のURL・SHA-256は `docs/tyrannosaurus-provenance.json`、`docs/carnotaurus-provenance.json`、`docs/patagotitan-provenance.json` に記録する。別のファイルを誤って取り込まないよう、変換器で入力ハッシュを照合する。

```sh
python tools/models/import_tyrannosaurus.py path/to/77e21c4e5d65468bbe11780ae0557b95.glb
python tools/models/import_authored_models.py carnotaurus path/to/04c8d6e0e1794091aeae0d2f54cca43b.glb
python tools/models/import_authored_models.py patagotitan path/to/a48cde44e8a045d2968fca28ce62c7ae.glb
```

作者復元の変換は numpy 2.5.3、trimesh 5.1.0、scipy 1.18.1、fast-simplification 0.2.0で検証。テクスチャのないメッシュはノードごとに整理・軽量化する。パタゴティタンの出力データはCC BY-SA 4.0、他2種の出力はCC BY 4.0。実寸未確認はKSM1のメートル係数に0を記録し、ランタイムでnullとして扱う。PRCとU3Dは異なる形式であり、Sophieの3D PDFから抽出したPRCをU3Dとしてインポートしない。
