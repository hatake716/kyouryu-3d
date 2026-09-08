# Google Play 掲載準備 — 1.10.1

最終確認日：2026-09-08。サポート連絡先は **acesmash@gmail.com**、対象は **子どもを含む全年齢**。開発者の指定に基づく設定です。IARCの年齢レーティングはPlay Consoleで質問票に回答して取得します。

## コピーして入力する文章

| 項目 | ファイル | 上限 |
|---|---|---:|
| アプリ名 | [title.txt](store/ja-JP/title.txt) | 30文字 |
| 短い説明 | [short-description.txt](store/ja-JP/short-description.txt) | 80文字 |
| 詳しい説明 | [full-description.txt](store/ja-JP/full-description.txt) | 4,000文字 |
| このリリースの更新情報 | [release-notes.txt](store/ja-JP/release-notes.txt) | 500文字 |

テキストファイルには見出しや注釈を含めず、そのまま貼り付けられる形にしています。説明と画像は1.10.1の実装に合わせました。100種類すべての実物スキャン、博物館との提携、専門家の監修、常時最新の学説、IARC取得済みなどの表示はしていません。

## ストアの画像

| 素材 | 配置 | 仕様 |
|---|---|---|
| アイコン | [icon-512.png](store/icon-512.png) | 512×512 PNG |
| フィーチャーグラフィック | [feature-1024x500.png](store/feature-1024x500.png) | 1024×500 RGB PNG、透過なし |
| スマートフォン縦画面 | [portrait](store/screenshots/portrait/) | 1080×1920、4枚 |
| スマートフォン横画面 | [landscape](store/screenshots/landscape/) | 1920×1080、4枚 |

[画像一覧・掲載順・代替テキスト](store/README.md)と[帰属一覧](store/CREDITS.md)を用意しています。8枚は同じ「スマートフォン」の枠に登録できます。縦横で別の端末カテゴリにはしません。タブレット専用画面は未撮影のため、スマートフォン画像をタブレットで撮影したものとして登録しません。

スクリーンショットは署名済み1.10.1の実際の操作画面です。広告文や端末枠の合成、画像の切り抜き・拡大、AIによる補正はしていません。従来の `docs/screenshots/` は過去の検証用画像で、今回のアップロード対象は `docs/store/screenshots/` です。

## 基本入力

| Play Consoleの項目 | 入力内容・方針 |
|---|---|
| アプリ / ゲーム | アプリ |
| メインの言語 | 日本語（ja-JP） |
| カテゴリー | 教育 |
| サポートメール | acesmash@gmail.com |
| ウェブサイト | https://hatake716.github.io/kyouryu-3d/ |
| プライバシーポリシー | https://hatake716.github.io/kyouryu-3d/privacy/ |
| アプリへのアクセス | ログイン・資格情報・地域制限なし。すべての機能を利用可能 |
| 広告 | アプリ内に広告なし。外部サイトの広告は当該サービスによる |
| アプリ内購入 | なし |
| 対象年齢 | 全年齢。下記のConsole申告ガイドを参照 |
| 販売価格 / 配信国 | 開発者がConsoleで決定。無料・有料の価格設定は未実施 |

[データセーフティ・対象年齢・審査用操作説明](PLAY_CONSOLE.md)と[リリース手順](RELEASE.md)に、入力案と残るConsole作業をまとめています。

## 確認した公式要件

- [画像・動画・短い説明の要件](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en)：画像寸法、形式、枚数、実際の体験を示す画像、代替テキスト。
- [ストアの掲載情報](https://support.google.com/googleplay/android-developer/answer/9859152?hl=en)：アプリ名と詳しい説明の文字数。
- [子どもを対象に含む場合のFamiliesポリシー](https://support.google.com/googleplay/android-developer/answer/9893335?hl=en)：内容と申告の整合。

これらは確認時点の公式資料です。実際の提出時にはConsoleの表示と審査上の指示も確認してください。
