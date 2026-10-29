#!/usr/bin/env python3
"""kyouryu-3d プロモ動画の 30fps コマ撮り。

前版は1シーン15〜28枚しか撮らず、1枚が5〜7フレーム持続してカクついた。
今回は秒あたり30枚を撮る。

`input swipe` は短距離だとタップ判定になり視点が動かない(展示の図鑑が開いてしまう)。
そこで `input motionevent` で DOWN → 細かい MOVE の連続 → UP を送る。
1回のジェスチャーの中で少しずつ動かすので、touchSlop を最初に一度越えれば
以降は 5px 単位でも確実に動き、コマ間の変化量を自由に決められる。
"""
import subprocess, sys, os, time

DEV = os.environ.get("PROMO_DEVICE", "emulator-5584")
ROOT = os.environ.get("PROMO_WORK", "/tmp/kyouryu-promo") + "/frames30"
PKG = "io.github.hatake716.dinobones"
SLOP = 40   # 最初のMOVEで消費される距離。これを越えてから実際の移動が始まる

def adb(*args, timeout=120):
    return subprocess.run(["adb", "-s", DEV, *args], capture_output=True, timeout=timeout)

def sh(cmd, timeout=120):
    return adb("shell", cmd, timeout=timeout)

def shot(path):
    for _ in range(5):
        r = adb("exec-out", "screencap", "-p", timeout=120)
        if r.stdout[:8] == b"\x89PNG\r\n\x1a\n" and len(r.stdout) > 80000:
            open(path, "wb").write(r.stdout)
            return True
        time.sleep(.4)
    print(f"  !! capture failed {path}", file=sys.stderr)
    return False

class Scene:
    def __init__(self, name):
        self.name = name
        self.dir = os.path.join(ROOT, name)
        os.makedirs(self.dir, exist_ok=True)
        for f in os.listdir(self.dir):
            os.remove(os.path.join(self.dir, f))
        self.n = 0
        self.t0 = time.time()
        print(f"[scene] {name}", flush=True)

    def frame(self):
        self.n += 1
        shot(os.path.join(self.dir, f"{self.n:04d}.png"))

    def hold(self, count):
        """静止コマは複製で水増し。静止中は絵が変わらないので画質に影響しない。"""
        if self.n == 0:
            self.frame()
        data = open(os.path.join(self.dir, f"{self.n:04d}.png"), "rb").read()
        for _ in range(count):
            self.n += 1
            open(os.path.join(self.dir, f"{self.n:04d}.png"), "wb").write(data)

    def gesture(self, x, y, step_x, step_y, frames):
        """DOWN → 1コマにつき (step_x, step_y) だけ MOVE → 各コマを撮る → UP。
        最初のMOVEで touchSlop を越えさせ、以降を等速の細かい移動にする。"""
        sh(f"input motionevent DOWN {x} {y}")
        cx, cy = x, y
        # slop を一気に消費する(この分は絵が動かないので撮らない)
        sx = SLOP if step_x > 0 else (-SLOP if step_x < 0 else 0)
        sy = SLOP if step_y > 0 else (-SLOP if step_y < 0 else 0)
        cx += sx; cy += sy
        sh(f"input motionevent MOVE {cx} {cy}")
        for _ in range(frames):
            cx += step_x; cy += step_y
            sh(f"input motionevent MOVE {int(cx)} {int(cy)}")
            self.frame()
        sh(f"input motionevent UP {int(cx)} {int(cy)}")

    def look(self, step_x, step_y, frames):
        """館内の視点操作。画面中央付近を起点にする。"""
        self.gesture(1150, 540, step_x, step_y, frames)

    def rotate(self, step_x, frames):
        """個別3Dビューで骨格を回す。左側の3D領域が起点。"""
        self.gesture(700, 560, step_x, 0, frames)

    def scroll(self, step_y, frames):
        self.gesture(960, 760, 0, step_y, frames)

    def walk(self, frames, power=0.22):
        """左下スティックを前へ倒して前進する。

        TheaterRenderer は 1フレームの移動量を実時間 dt から決める
        (TheaterWorld.move: min(dt,0.1) * MOVE_SPEED 8m/s = 最大0.8m/frame)。
        コマ撮りは1コマ約0.75秒かかるため、スティックを最大まで倒すと
        毎コマ上限の0.8m進み、数コマで壁や展示台に着いてしまう。
        power で倒す量を絞り、1コマあたりの前進量を小さくする。

        座標は TheaterView.Controls の実装より、density 420 で
        中心 (84dp, height-104dp) = (220, 807)、半径 54dp = 142。
        forward = (stickY - y)/radius なので、中心から radius*power だけ
        上へ倒すと forward = power になる。"""
        cx, cy = 220, 807
        ty = int(cy - 142 * power)
        sh(f"input motionevent DOWN {cx} {cy}")
        for i in range(frames):
            sh(f"input motionevent MOVE {cx + (i % 2)} {ty}")
            self.frame()
        sh(f"input motionevent UP {cx} {ty}")

    def tap(self, x, y, wait=1.2):
        sh(f"input tap {x} {y}")
        time.sleep(wait)

    def done(self):
        print(f"  -> {self.n}枚 ({time.time()-self.t0:.0f}秒)", flush=True)

def restart():
    sh(f"am force-stop {PKG}")
    time.sleep(1.5)
    sh("settings put system user_rotation 1")
    sh(f"am start -n {PKG}/.MainActivity")
    time.sleep(28)
