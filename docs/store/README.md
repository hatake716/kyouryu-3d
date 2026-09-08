# Google Play用の画像と掲載文

1.10.1 / code 12、2026-09-08撮影。署名済みリリースAPKをAPI 35エミュレーターで実行した画面です。全8枚はスマートフォン用で、縦横を混在させて登録します。

| 掲載順 | ファイル | 内容 |
|---:|---|---|
| 1 | [縦・博物館](screenshots/portrait/01-portrait-museum.png) | 100種類が並ぶ仮想博物館の入口 |
| 2 | [縦・コレクション](screenshots/portrait/02-portrait-collection.png) | 時代の世界地図と種類別カード |
| 3 | [縦・骨格観察](screenshots/portrait/03-portrait-skeleton.png) | ブラキオサウルスの学習用骨格 |
| 4 | [縦・詳細図鑑](screenshots/portrait/04-portrait-encyclopedia.png) | 名前の由来と命名の歴史 |
| 5 | [横・博物館](screenshots/landscape/05-landscape-museum.png) | トリケラトプスの公開標本 |
| 6 | [横・骨格観察](screenshots/landscape/06-landscape-skeleton.png) | ティラノサウルスの展示レプリカのスキャン |
| 7 | [横・復元イラスト](screenshots/landscape/07-landscape-illustration.png) | トリケラトプスの復元画 |
| 8 | [横・骨格観察](screenshots/landscape/08-landscape-spinosaurus.png) | スピノサウルスの学習用骨格 |

縦1080×1920、横1920×1080。24bit RGB PNG、透過なし。Androidのデモモードで時計を10:00、電池を100%、通知を非表示に統一しました。アプリ内の操作で表示・視点・スクロール位置を選び、`adb exec-out screencap -p`で撮影しています。画像のリサイズ・トリミング・UI合成・AI補正は行っていません。完全不透明なアルファチャンネルだけを除去し、RGB画素の一致を検査しました。

各画像の日本語代替テキスト（140文字以内）、寸法、APK・原キャプチャ・PNG・RGB画素のSHA-256は [screenshot-manifest.json](screenshot-manifest.json)。画素変換は `tools/prepare_store_screenshots.py RAW_DIRECTORY` で再現できます（Python + Pillow）。検証用の過去画像 `docs/screenshots/` は今回のストア登録に使用しません。

[アイコン](icon-512.png)は既存のランチャーと共通の独自頭骨シンボルです。[フィーチャーグラフィック](feature-1024x500.png)は [編集可能なSVG](feature-1024x500.svg) から描画した、アプリの独自ブランド画像です。実際のアプリ画面を示すスクリーンショットとは別の素材です。フォントは実行環境のNoto Sans CJK JPで描画しています。フォントファイルは再配布しません。

素材の再利用には [CREDITS.md](CREDITS.md) と個別ライセンスを確認してください。ストア画像も第三者の復元画・公開モデル・地図・図鑑本文を含みます。ライセンスの帰属を落としたまま一律Apache 2.0として配布しないでください。

登録用の文章は [ja-JP](ja-JP/)、入力方法は [PLAY_STORE.md](../PLAY_STORE.md)。プレビュー画像の検査は `python3 tools/check_store_assets.py` で実行できます。
