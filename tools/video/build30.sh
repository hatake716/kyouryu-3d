#!/usr/bin/env bash
# 恐竜の骨格３D プロモ動画（30fps版）
# 素材は 1コマ = 1フレーム で撮影済みなので、fps変換も補間も不要。
# そのまま 30fps で並べるだけで滑らかに再生される。
set -euo pipefail
SP=${PROMO_WORK:-/tmp/kyouryu-promo}
F=$SP/frames30
C=$SP/clips30
OUT=$SP/out
mkdir -p $C $OUT
FONT="/nix/store/6qk3fb6fa5wjagx5s5kdhgyqgcx8p8g7-noto-fonts-cjk-sans-2.004/share/fonts/opentype/noto-cjk/NotoSansCJK-VF.otf.ttc"

echo "== クリップ生成（1コマ=1フレーム、30fps）=="
for name in 01_entrance 02_walk 03_lookup 04_model3d 05_parts 06_encyclopedia 07_collection 08_grid; do
  ffmpeg -y -v error -framerate 30 -i "$F/$name/%04d.png" \
    -vf "scale=1920:1080:flags=lanczos" \
    -c:v libx264 -preset slow -crf 17 -pix_fmt yuv420p -r 30 \
    "$C/$name.mp4"
  printf "  %-16s %s枚 -> %ss\n" "$name" "$(ls $F/$name | wc -l)" \
    "$(ffprobe -v error -show_entries format=duration -of csv=p=0 $C/$name.mp4)"
done

printf "file '%s'\n" \
  $C/01_entrance.mp4 $C/02_walk.mp4 $C/03_lookup.mp4 $C/04_model3d.mp4 \
  $C/05_parts.mp4 $C/06_encyclopedia.mp4 $C/07_collection.mp4 $C/08_grid.mp4 \
  > $OUT/list30.txt
ffmpeg -y -v error -f concat -safe 0 -i $OUT/list30.txt -c copy $OUT/raw30.mp4
echo "連結: $(ffprobe -v error -show_entries format=duration -of csv=p=0 $OUT/raw30.mp4)s"

# クリップ境界（秒）
#  0.00  4.03  7.36 10.86 14.76 18.26 21.89 25.06 28.46
draw() { # text start end fontsize xexpr
  local txt=$1 st=$2 en=$3 size=$4 xx=$5
  printf "drawtext=fontfile='%s':text='%s':fontcolor=0xF3E3C5:fontsize=%s:x=%s:y=h-190:box=1:boxcolor=0x241B16@0.82:boxborderw=24:borderw=3:bordercolor=0x140E0A@0.9:enable='between(t\\,%s\\,%s)':alpha='if(lt(t-%s\\,0.35),(t-%s)/0.35,if(lt(%s-t\\,0.35),(%s-t)/0.35,1))'," \
    "$FONT" "$txt" "$size" "$xx" "$st" "$en" "$st" "$st" "$en" "$en"
}
CENTER='(w-tw)/2'
LEFT='110'

FILTER=$(
  draw "恐竜の骨格３D"                      0.35  3.70  70 "$CENTER"
  draw "100種類の骨格が並ぶ、歩ける博物館"   4.35  7.05  54 "$CENTER"
  draw "見上げるほどの大きさを、原寸のまま"  7.70 10.55  54 "$CENTER"
  draw "指で回して、あらゆる角度から"       11.20 14.45  54 "$LEFT"
  draw "部位ごとに、骨のつくりを観察"       15.10 17.95  54 "$LEFT"
  draw "日本語の解説と、出典つきの図鑑"     18.60 21.60  54 "$LEFT"
  draw "三畳紀・ジュラ紀・白亜紀で切り替え"  22.25 24.75  54 "$CENTER"
  draw "広告なし・オフラインで使える"       25.40 28.15  54 "$CENTER"
)
FILTER=${FILTER%,}

ffmpeg -y -v error -i $OUT/raw30.mp4 -vf "$FILTER" \
  -c:v libx264 -preset slow -crf 18 -pix_fmt yuv420p -movflags +faststart -r 30 \
  $OUT/kyouryu-3d-promo-1920x1080.mp4

echo "完成: $OUT/kyouryu-3d-promo-1920x1080.mp4"
ffprobe -v error -select_streams v:0 -show_entries stream=width,height,r_frame_rate,nb_frames \
  -show_entries format=duration,size -of default=nw=1 $OUT/kyouryu-3d-promo-1920x1080.mp4
