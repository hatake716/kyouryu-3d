# 本アプリ用に生成した背景画

2026-09-07、組み込みのOpenAI `image_gen` を使用。参照画像・第三者の作品・特定作家や映画のスタイルを入力せず、新規に生成しました。CLIや独自APIスクリプトは使用していません。以下のPNGを未加工でアプリへ同梱し、表示時に拡大・トリミング・明度調整を行います。C2PA情報を削除していません。

- `app/src/main/assets/backgrounds/titanosaurus.png`
- `app/src/main/assets/backgrounds/saurophaganax.png`

両方とも、全身を確定できない属に対する**強い仮定を含むイメージ**です。アプリの3D画面にAI生成と仮説の表示を付けています。現実の化石を計測・スキャンした復元画ではなく、爪・筋肉・軟組織・色などの細部を科学的資料として扱いません。AIによる出力に独占的な著作権が成立することを保証せず、権利を有する範囲でApache 2.0として提供します。

## Titanosaurus prompt

```text
Use case: scientific-educational. Asset type: original full-screen portrait background illustration for a Japanese dinosaur skeleton Android app. Primary request: a speculative Titanosaurus indicus life impression, clearly an illustrative generic titanosaur body plan because its diagnostic fossils are fragmentary. Subject: one long-necked, long-tailed, four-legged titanosaur sauropod with small head, broad torso, columnar legs, neutral grey-green scaly skin, walking through a Late Cretaceous Indian river floodplain with ferns and conifers. Portrait 1024x1536 composition; three-quarter view with entire animal including head, tail and four feet inside frame and occupying the central lower two thirds. Natural soft dusk light, painterly museum educational paleoart, calm realistic environment. No existing film creature designs, no logos, no signature, no text, no skeletal overlay, no horns or fantastical anatomy. The app separately labels uncertainty and AI origin.
```

## Saurophaganax prompt

```text
Use case: scientific-educational. Asset type: original portrait background image for a dinosaur encyclopedia Android app. Primary request: an explicitly speculative diplodocid sauropod impression for the disputed Saurophaganax holotype under the sauropod interpretation proposed in 2024; its full animal is unknown. Depict one generic diplodocid, not an allosaur: long relatively horizontal neck, tiny elongated head, barrel torso, four columnar legs, exceptionally long tapering whip tail held clear of ground, grey earthy scaly skin, no armor. Original calm museum paleoart painting, late Jurassic North American seasonal floodplain with conifers and ferns, natural light. Portrait 1024x1536, three-quarter view with the animal fitting entirely within the frame, neck and tail curving gently within composition, readable center silhouette with head around upper third and four feet near bottom. No film imagery, no existing artwork references, no logo, no text, no signature, no skeletal overlay. The app labels this as an AI-generated highly speculative interpretation rather than a verified life restoration.
```

全画像のハッシュと出典は [artwork-provenance.json](artwork-provenance.json)。Commons由来の98画像にこのライセンスを適用しないでください。

## 1.8.0 館内の森林壁画

2026-09-08、組み込みの `image_gen` で新規生成。参照画像は使用せず、CLI・外部のAPIスクリプトも使用していません。巨大な針葉樹や木生シダを思わせる創作の景観です。特定の年代・産地の植生を確定した科学的復元ではありません。

同梱先：`app/src/main/assets/theater/mesozoic-forest.png`（1774 × 887）。生成されたPNGの全バイトとC2PA情報を保持。実行時に2048 × 1024へ補間してGPUへ転送し、左右を交互に反転して壁面を連続表示します。壁の遠近や薄い霞による表示上の変換があります。hatake716が権利を有する範囲でApache 2.0として提供し、AI出力の独占的な著作権成立を保証しません。既存の第三者モデル・図鑑背景画の利用条件をこの壁画へ置き換えません。

[生成履歴・プロンプト・SHA-256](forest-mural-provenance.json)。

```text
Use case: historical-scene. Asset type: panoramic forest mural texture for the inside walls of a first-person 3D paleontology museum Android app, original artwork. Create a highly detailed, naturalistic environmental matte painting evoking a vast Mesozoic forest. Colossal tall araucaria-like conifers rising far above dense tree ferns, cycad-like plants and fern understory; layered forest receding into luminous blue-green atmospheric mist, glimpses of pale blue sky through high canopies. View horizontally from human eye height, inviting daylight, beautifully nuanced bark, feathery green foliage. Bright, airy upper half with sunlit tree crowns and sky; readable medium-light greens and warm earth tones below, no dark vignette, no black canopy ceiling. Broad continuous forest vista, forest covers entire image edge to edge, no large foreground tree blocking the view, no distinct central focal point. Paint as one flat unframed panoramic image, landscape aspect ratio 2:1, ideally 2048 x 1024 or higher at that ratio. The left and right edges should be visually compatible to tile horizontally at equal ground/sky levels, with fine distant foliage near both edges. This is evocative prehistoric concept art rather than an exact scientific reconstruction. No dinosaurs or other animals, no humans, no skeletons, no buildings, no museum interior, no lettering, no logos, no border, no watermarks. Original composition, not a recreation of any existing mural or museum artwork.
```
