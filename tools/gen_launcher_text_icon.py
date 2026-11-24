#!/usr/bin/env python3
"""生成 Material3Tim 的文字启动图标（M3 / TIM 两行居中）。

- 字体：Google Sans Flex（SIL OFL 1.1，google/fonts 仓库），实例化 wght=600 / opsz=96；
- 输出：res/drawable/ic_launcher_foreground.xml、ic_launcher_monochrome.xml（主题图标用）、
  ic_launcher_splash.xml（Android 12 启动页图标，288dp viewport）；
- 适配层：res/mipmap-anydpi/ic_launcher.xml（background + foreground + monochrome）。

用法：
    python3 tools/gen_launcher_text_icon.py            # 自动下载字体到 /tmp 后生成
    python3 tools/gen_launcher_text_icon.py --font path/to/GoogleSansFlex.ttf

依赖：pip install fonttools
"""
import argparse
import os
import re
import subprocess
import tempfile

from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.misc.transform import Transform
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

FONT_URL = (
    "https://raw.githubusercontent.com/google/fonts/main/ofl/googlesansflex/"
    "GoogleSansFlex%5BROND%2Copsz%2Cslnt%2Cwdth%2Cwght%5D.ttf"
)
REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "app/src/main/res/drawable")

# 108dp 画布上的设计参数（可根据预览微调）。
# 注意：自适应图标 108dp 里真正可见的通常只有中心 ~72dp（启动器会按圆角/圆形遮罩裁掉四周），
# 安全内容建议控制在中心 66dp 圆形内。之前的 58x58 内容在四角会超出圆形遮罩被截断，
# 因此缩到 42x46 左右（四角都在 r≈31 < 33 内）。
LINE1 = "M3"
LINE2 = "TIM"
TARGET_H1 = 22.0   # 第一行视觉高度
TARGET_H2 = 17.0   # 第二行视觉高度
GAP = 5.0          # 两行间隙
MAX_WIDTH = 42.0   # 内容最大宽度（控制在中心安全圆内）


def load_font(path):
    font = TTFont(path)
    return instantiateVariableFont(
        font, {"wght": 600, "opsz": 96, "ROND": 0, "wdth": 100, "slnt": 0}
    )


def measure(font, text):
    glyph_set = font.getGlyphSet()
    cmap = font.getBestCmap()
    total = 0
    x = 0
    xmin = ymin = 1e9
    xmax = ymax = -1e9
    for ch in text:
        gname = cmap[ord(ch)]
        glyph = glyph_set[gname]
        pen = BoundsPen(glyph_set)
        glyph.draw(pen)
        if pen.bounds:
            gx0, gy0, gx1, gy1 = pen.bounds
            xmin = min(xmin, x + gx0)
            xmax = max(xmax, x + gx1)
            ymin = min(ymin, gy0)
            ymax = max(ymax, gy1)
        x += glyph.width
        total += glyph.width
    return glyph_set, cmap, xmin, ymin, xmax, ymax


def line_paths(font, text, scale, center_x, center_y):
    glyph_set, cmap, xmin, ymin, xmax, ymax = measure(font, text)
    tx = center_x - ((xmin + xmax) / 2) * scale
    ty = center_y + ((ymin + ymax) / 2) * scale
    tf = Transform(scale, 0, 0, -scale, tx, ty)
    paths = []
    x = 0
    for ch in text:
        gname = cmap[ord(ch)]
        glyph = glyph_set[gname]
        spen = SVGPathPen(glyph_set)
        glyph.draw(TransformPen(spen, tf.translate(x, 0)))
        commands = spen.getCommands()
        if commands.strip():
            paths.append(commands)
        x += glyph.width
    return paths, (xmax - xmin) * scale, (ymax - ymin) * scale


def make_body(font, viewport, fill):
    target_h1 = TARGET_H1 / 108 * viewport
    target_h2 = TARGET_H2 / 108 * viewport
    gap = GAP / 108 * viewport
    max_width = MAX_WIDTH / 108 * viewport
    _, w1, h1 = line_paths(font, LINE1, 1.0, 0, 0)
    _, w2, h2 = line_paths(font, LINE2, 1.0, 0, 0)
    s1 = target_h1 / h1
    s2 = target_h2 / h2
    if w1 * s1 > max_width:
        s1 = max_width / w1
    if w2 * s2 > max_width:
        s2 = max_width / w2
    h1 *= s1
    h2 *= s2
    total = h1 + gap + h2
    top = viewport / 2 - total / 2
    c1 = top + h1 / 2
    c2 = top + h1 + gap + h2 / 2
    p1, _, _ = line_paths(font, LINE1, s1, viewport / 2, c1)
    p2, _, _ = line_paths(font, LINE2, s2, viewport / 2, c2)
    body = ""
    for d in p1 + p2:
        body += (
            "    <path\n"
            f'        android:fillColor="{fill}"\n'
            f'        android:pathData="{d}" />\n'
        )
    return body


def write_vector(path, width_dp, viewport, body):
    xml = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{width_dp}dp"\n'
        f'    android:height="{width_dp}dp"\n'
        f'    android:viewportWidth="{viewport}"\n'
        f'    android:viewportHeight="{viewport}">\n'
        f"{body}"
        "</vector>\n"
    )
    with open(path, "w", encoding="utf-8") as f:
        f.write(xml)
    print("wrote", os.path.relpath(path, REPO))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--font", help="Google Sans Flex 变量字体路径（缺省自动下载）")
    args = parser.parse_args()
    font_path = args.font
    if not font_path:
        font_path = os.path.join(tempfile.gettempdir(), "GoogleSansFlex.ttf")
        if not os.path.exists(font_path):
            print("downloading font ->", font_path)
            subprocess.run(["curl", "-sL", "--max-time", "120", "-o", font_path, FONT_URL], check=True)
    font = load_font(font_path)
    body108 = make_body(font, 108, "#FFFFFFFF")
    body288 = make_body(font, 288, "#FFFFFFFF")
    write_vector(os.path.join(RES, "ic_launcher_foreground.xml"), 108, 108, body108)
    write_vector(os.path.join(RES, "ic_launcher_monochrome.xml"), 108, 108, body108)
    write_vector(os.path.join(RES, "ic_launcher_splash.xml"), 288, 288, body288)


if __name__ == "__main__":
    main()
