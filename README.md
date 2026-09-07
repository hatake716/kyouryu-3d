# 恐竜の骨格３D

100種類の古生物を、独自制作の3D骨格と日本語図鑑で観察するAndroidアプリです。生体の復元イメージを背景いっぱいに表示し、骨格を回転・拡大縮小して見られます。

**3Dは学習用の模式復元です。実物標本のスキャン、計測用模型、学術監修済みモデルではありません。** 骨数、関節面、欠損部分、個体差は簡略化し、種類ごとの限界をアプリ内に表示しています。

| 100種類の一覧 | 背景付きの3D展示室 | 背景の調整・出典 |
|---|---|---|
| ![一覧](docs/screenshots/00-collection.png) | ![3D展示室](docs/screenshots/release-titanosaurus.png) | ![背景と出典](docs/screenshots/background-attribution.png) |

## 機能

- 100点の背景画。全画面への拡大／画像全体の表示、濃さ調整、作者・出典・ライセンスの確認
- 1本指の回転、2本指のピンチ拡大縮小・平行移動
- 側面・正面・背面・上面・下面、自動回転、全画面、リセット
- 頭骨／背骨・尾／肋骨／肩・骨盤／前肢・翼／後肢／装飾・装甲の強調と単独表示
- 100種類、注記を含む計693節の日本語解説。分類、時代、生息環境、発見地、研究史、特徴、食性、推定値、未解明の点
- 名前・時代・地域による検索、分類フィルター、端末内のお気に入り
- Wikipedia記事、全100種類のYouTube関連動画検索、選定した動画・動画掲載ページ、参考資料への外部リンク
- 骨格・背景画像・図鑑はオフライン。広告・解析・アカウント・通信権限なし

元の13種類：スピノサウルス、ティラノサウルス、トリケラトプス、モササウルス、ヴェロキラプトル、ステゴサウルス、アンキロサウルス、ブラキオサウルス、パラサウロロフス、ギガノトサウルス、アロサウルス、プテラノドン、アーケオプテリクス。

さらにケツァルコアトルス、ディロフォサウルス、テリジノサウルス、イグアノドンなどを含む100項目です。[全収録項目と選定根拠](docs/POPULARITY.md)をご覧ください。英語版Wikipediaの2026年6〜8月の人による閲覧数を目安に、候補中の上位100項目を選びました。世界共通の人気投票ではありません。

モササウルスは海生有鱗類、プテラノドンは翼竜です。アーケオプテリクスは鳥類に近い恐竜として紹介しています。

## ビルド

JDK 17、Android SDK Platform 36、ネットワーク接続（初回のGradle依存解決）が必要です。Android 8.0（API 26）以上、OpenGL ES 2.0対応端末向け。targetSdkは36です。

```sh
# local.properties に自身のSDKを指定
printf 'sdk.dir=/path/to/Android/Sdk\n' > local.properties
./gradlew assembleDebug testDebugUnitTest lintDebug

# エミュレーターを指定。日常利用端末には入力注入しない
ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest

# release署名設定後
./gradlew assembleRelease bundleRelease
```

リリース署名、Play Console、プライバシーポリシー公開については [docs/RELEASE.md](docs/RELEASE.md)、説明文は [docs/PLAY_STORE.md](docs/PLAY_STORE.md)、検証記録は [docs/VERIFICATION.md](docs/VERIFICATION.md) を参照してください。

## 出典と権利

プログラム・独自骨格生成・独自アイコン：Apache License 2.0。第三者の3Dモデルや動画は同梱していません。追加87モデルは共通の体型構造に属ごとの模式的な設定を加えています。骨格は `model/Skeleton.kt` の独自座標から生成します。

図鑑の日本語解説はWikipedia各記事の抄録・翻案で、**CC BY-SA 4.0**です。Wikipedia contributorsへの帰属、原文・編集履歴・ライセンスへのリンク、翻案であることをアプリ内と [docs/CONTENT_LICENSE.md](docs/CONTENT_LICENSE.md) に記載しています。基本情報は英語版などから整理し、追加分は日本語版の本文を抄録しています。固定版と更新の注記を併記します。

背景98点はWikimedia CommonsのCC BY／CC BY-SA等の条件に従って同梱し、2点は本アプリ用のAI生成画像です。プログラムのApache 2.0を第三者画像に適用しません。個別の帰属・ライセンス・ファイルハッシュは [artwork-provenance.json](docs/artwork-provenance.json)、生成プロンプトは [GENERATED_ARTWORK.md](docs/GENERATED_ARTWORK.md)。表示時にトリミング・明度調整を行います。

確認日：2026-09-07。自動更新やすべての既知情報の網羅を保証しません。大きさ・年代は推定で、研究・標本・分類により変わります。ギガノトサウルスの産地年代の再検討、スピノサウルスの2026年新種報告などは注記付きで扱います。

本アプリは各博物館、Wikimedia Foundation、YouTube、Googleの公式・提携アプリではありません。第三者の素材を追加するときは、別途ライセンス・帰属・商用再配布条件を確認してください。

## 構成

- `app/src/main/java/.../model/` — 独自メッシュ、OpenGL ES描画、カメラ、ジェスチャー
- `app/src/main/java/.../ui/` — Composeの一覧・展示室・図鑑・権利表示
- `tools/build_catalog.py` — 元の13項目と凍結した追加データからJSONを生成
- `tools/catalog/` — 候補・閲覧数の収集、本文抽出、図鑑の追加入力、模型の設定
- `tools/check_catalog.py` — 100画像のハッシュ・帰属と閲覧数／収録項目の整合確認
- `app/src/main/assets/` — オフライン図鑑、プライバシー、ライセンス
- `docs/privacy/index.html` — 公開用プライバシーページ
- `app/src/androidTest/` — 100種類の描画と実操作、状態保持を検証するエミュレーターE2E

図鑑を編集したら `python3 tools/build_catalog.py` を実行し、出典と確認日、アプリ内の説明も同時に更新してください。

モデル設定を編集した場合は `python3 tools/catalog/write_profiles.py` も実行します。通常のビルドはネットワークなしで図鑑・画像を利用します。Wikipedia/Commonsの再取得は `tools/catalog/` の研究用スクリプト（Python 3とlxml）で行いますが、追加前に本文、分類、画像の内容、個別の権利表示を確認してください。取得時の作業キャッシュはGit対象外です。
