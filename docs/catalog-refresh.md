# 図鑑データの更新

通常の生成・検査はネットワーク不要です。

```sh
python3 tools/build_catalog.py
python3 tools/catalog/write_profiles.py
python3 tools/check_catalog.py
```

`tools/catalog/extra-entries.json` はレビュー対象となる追加87項目の凍結入力です。基本カードの編集元は `facts.tsv`、模型の設定は `profiles.py`。元の13項目は `tools/build_catalog.py` にあります。本文はCC BY-SA 4.0です。

新しい期間へ更新するときは `rank.py` の期間を明示的に変更し、候補記事・別名・対象群を再確認します。`research.py` は各記事の固定版とCommonsの画像候補を作業キャッシュへ収集します。`prepare_catalog.py` は収集後の抄録とカードを凍結入力へ書き出します。候補に出たことだけで採用せず、画像が実際に対象の生体復元か、作者と商用再配布条件、本文とカードの年代・分類が一致するかを確認してください。

収集用の追加Python依存はlxmlです。ファイルはUTF-8、作業キャッシュは無視対象の `artifacts/research/`。HTTP制限には待機・再試行で対応し、欠損を0と置き換えません。旧キャッシュを別の期間に流用しないでください。

Commons画像は `artworks.py` がライセンス情報とダウンロード後のハッシュを保存します。例外の2点は新規AI生成画像で、`GENERATED_ARTWORK.md` を参照してください。全100画像を配布するため、出典だけでなく画像ファイルもコミットします。
