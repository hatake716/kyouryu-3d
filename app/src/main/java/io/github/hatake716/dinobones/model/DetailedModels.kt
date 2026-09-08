package io.github.hatake716.dinobones.model

/** Reviewed, bundled meshes. The author's model license is independent of the app license. */
data class DetailedModel(val label:String,val source:String,val note:String,val textured:Boolean=true)
object DetailedModels {
    val entries=mapOf(
        "triceratops" to DetailedModel("公開3D標本 · 実寸単位を保持", "https://3d.si.edu/object/3d/triceratops-horridus-marsh-1889:d8c623be-4ebc-11ea-b77f-2e728ce88125", "Smithsonian Institution / National Museum of Natural History の公開3D標本（CC0）。約15万三角形の形状と原資料の色・凹凸テクスチャを使用し、メートル単位を保持しています。公開メッシュの前後方向の幅は約5.89 mです。複数個体や補作を含む組み立て復元で、生きていた1個体の完全な骨格ではありません。部位選択は座標による簡易的な領域分けで、骨ごとの厳密な分離ではありません。"),
        "tyrannosaurus" to DetailedModel("公開3Dスキャン · 展示レプリカ", "https://sketchfab.com/3d-models/tyrannosaurus-rex-skeleton-77e21c4e5d65468bbe11780ae0557b95", "Rigsters制作『Tyrannosaurus Rex skeleton』（CC BY 4.0）。デンマークのKnuthenborg Safariparkに展示されたレプリカを133枚の写真から作成した公開スキャンです。Objaverse公開アーカイブに含まれる約25万三角形の形状と、撮影に由来する色テクスチャを使用しています。向き・中心・画面内の大きさを調整し、座標による簡易的な部位分けを追加しました。写真に写らなかった面には欠損や歪みが残ります。原資料の実寸換算を確認できないため、m単位の目盛りは表示しません。別個体CM 9380の骨長はこの標本へ適用していません。"),
        "carnotaurus" to DetailedModel("詳細3D復元 · 作者公開モデル", "https://sketchfab.com/3d-models/carnotaurus-sastrei-skeleton-04c8d6e0e1794091aeae0d2f54cca43b", "Magorius制作『Carnotaurus sastrei (skeleton)』（CC BY 4.0）の公開復元モデルです。約26万三角形の形状と元の色テクスチャを使用しています。左右の角、頭骨の開口部、下顎、歯列、椎骨、肩帯、指まで立体で表現されています。表示の向き・中心・大きさと陰影を調整し、座標による簡易的な部位分けを追加しました。実物を計測したスキャンという裏付けや実寸換算は確認できないため、標本の厳密な計測再現とは表示せず、m単位の格子は使用しません。"),
        "patagotitan" to DetailedModel("詳細3D復元 · 作者公開モデル", "https://sketchfab.com/3d-models/patagotitan-mayorum-by-mebalab-a48cde44e8a045d2968fca28ce62c7ae", "MebaLab / lautaro.cb制作『Patagotitan mayorum by MebaLab』（CC BY-SA 4.0）の公開復元モデルです。元の約84万三角形を、細い骨が消えないよう部材ごとに軽量化し、約35万三角形で表示します。長い首と尾、椎骨の突起、肋骨、肩帯、四肢の形状を含みます。表示の向き・中心・大きさを調整し、簡易的な部位分けと本アプリの骨の質感・陰影を適用しました。実測標本や実寸換算の裏付けは確認できないため、研究用の計測モデルとは扱わず、m単位の格子は使用しません。変換後の骨格データにもCC BY-SA 4.0を適用しています。", textured=false),
    )
}
