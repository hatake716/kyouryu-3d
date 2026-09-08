# 館内用骨格データの再生成

通常のAndroidビルドはコミット対象の `app/src/main/assets/theater/` をそのまま同梱します。生成は明示的に実行し、単体テストがソース資産を上書きすることはありません。

```sh
./gradlew :app:exportTheaterSources
python3 -m venv artifacts/theater-1.6/venv
artifacts/theater-1.6/venv/bin/pip install -r tools/theater/requirements.txt
artifacts/theater-1.6/venv/bin/python tools/theater/build_meshes.py
./gradlew :app:testDebugUnitTest
```

96種類の原稿は `artifacts/theater-1.6/sources/` のKSM1、公開メッシュ4種類は既存の `assets/specimens/` を使用します。入力データ自体はこのスクリプトで取得しません。先に元データの権利とprovenanceを確認してください。

TMS1はgzip圧縮した独自形式で、圧縮の時刻を0に固定しています。4バイトmagic、little-endian UInt32のパート数、各パートの頂点数・インデックス数に続き、xyz・法線・RGBの9個のFloat32、UInt16のインデックスを格納します。1パートは60,000頂点以内とし、GLES2の標準インデックス形式で描画できます。

`.tmsz` を使うのは、Androidの資産梱包で `.gz` が特別扱いされるのを避けるためです。アプリ内manifestとdocsのprovenanceは同一内容を生成します。使用する幾何ライブラリの違いで簡略化結果が変わる場合があるため、記録したバージョンとハッシュを確認します。

原本の帰属・派生メッシュのライセンス・縮尺の限界は [THEATER.md](../../docs/THEATER.md) を参照してください。


## 1.7.0のm単位への換算

`build_meshes.py` の最後に `calibrate_scale.py` を呼び、展示台へ一律に縮小する換算を置き換えます。元のKSM1ヘッダーのm単位がある19種はその倍率を保持し、他は収録図鑑の全長・翼開長の範囲と、スクリプトに記載した博物館資料を採用します。5種は確かな寸法がないため未確定と表示します。

骨格メッシュを再生成せず、寸法情報だけを更新する場合：

```sh
./gradlew :app:exportTheaterSources
python3 tools/theater/calibrate_scale.py
./gradlew :app:testDebugUnitTest
```

この操作は既存の `.tmsz` の頂点・色を変更しません。`manifest.json` と `docs/theater-mesh-provenance.json`、`ExhibitScales.kt`、アプリ内資料 `theater/scale-notes.txt` を一緒に生成します。元のKSM1を参照するので、新しい作業環境では先にエクスポートしてください。寸法値を変更すると、Kotlin側が展示台・通路・部屋の配置を再計算します。収録データと典拠は同時に更新し、全展示の到達・選択・骨格が室内に収まることを検証します。
