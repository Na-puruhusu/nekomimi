#!/usr/bin/env python3
"""Generate textures, item models, recipes and language files for the Nekomimi mod.

Usage (from the repository root):  python3 tools/generate_assets.py
Requires Pillow.
"""
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/nekomimi"
DATA = ROOT / "src/main/resources/data/nekomimi"
MODID = "nekomimi"

# id -> palette. wool is the vanilla wool used in the recipe.
COLORS = {
    "white": dict(base="#f2f2f2", tip="#d9d9de", inner="#f4a6b9", stripe=None,
                  outline="#8a8a94", wool="minecraft:white_wool", ja="白い", en="White"),
    "black": dict(base="#2a2a30", tip="#55555e", inner="#c7788c", stripe=None,
                  outline="#0c0c10", wool="minecraft:black_wool", ja="黒い", en="Black"),
    "orange": dict(base="#e58a34", tip="#fff1dd", inner="#f4a6b9", stripe="#b85f17",
                   outline="#6b3510", wool="minecraft:orange_wool", ja="オレンジの", en="Orange"),
    "gray": dict(base="#8d9199", tip="#d6d9df", inner="#f4a6b9", stripe=None,
                 outline="#4a4d54", wool="minecraft:gray_wool", ja="灰色の", en="Gray"),
}


def rgb(hex_color):
    h = hex_color.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def mix(color, other, amount):
    return tuple(int(color[i] * (1 - amount) + other[i] * amount) for i in range(3)) + (255,)


def lighten(color, amount=0.12):
    return mix(color, (255, 255, 255, 255), amount)


def darken(color, amount=0.14):
    return mix(color, (0, 0, 0, 255), amount)


def rect(img, x, y, w, h, color):
    ImageDraw.Draw(img).rectangle([x, y, x + w - 1, y + h - 1], fill=color)


def paint_cube(img, u, v, w, h, d, base, top=None, bottom=None, side=None):
    """Fill the six faces of a Minecraft box UV layout at texOffs(u, v)."""
    rect(img, u + d, v, w, d, top or lighten(base))            # top
    rect(img, u + d + w, v, w, d, bottom or darken(base))      # bottom
    rect(img, u, v + d, d, h, side or darken(base, 0.07))      # right (-x)
    rect(img, u + d, v + d, w, h, base)                        # front (-z)
    rect(img, u + d + w, v + d, d, h, side or darken(base, 0.07))  # left (+x)
    rect(img, u + 2 * d + w, v + d, w, h, base)                # back (+z)


def armor_texture(kind, p):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    base, tip, inner = rgb(p["base"]), rgb(p["tip"]), rgb(p["inner"])
    if kind == "nekomimi":
        # ear base 3x2x2 at (0,0): pink inner ear on the front face
        paint_cube(img, 0, 0, 3, 2, 2, base)
        rect(img, 2 + 1, 2, 1, 2, inner)       # middle column of the front face
        # ear tip 1x1x2 at (10,0)
        paint_cube(img, 10, 0, 1, 1, 2, base)
        rect(img, 10 + 2, 2, 1, 1, inner)
    else:
        # tail base 2x6x2 at (0,8), tip 2x4x2 at (8,8)
        paint_cube(img, 0, 8, 2, 6, 2, base)
        paint_cube(img, 8, 8, 2, 4, 2, tip, top=lighten(tip), bottom=darken(tip), side=darken(tip, 0.07))
        if p["stripe"]:
            stripe = rgb(p["stripe"])
            for row in (1, 3):
                y = 8 + 2 + row
                rect(img, 0, y, 8, 1, stripe)   # right, front, left, back faces of the base
    return img


def icon_outline(canvas, outline):
    px = canvas.load()
    w, h = canvas.size
    fur = {(x, y) for x in range(w) for y in range(h) if px[x, y][3] > 0}
    for (x, y) in fur:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in fur:
                    px[nx, ny] = outline


EARS_ICON = [
    "..F........F..",
    ".FFF......FFF.",
    ".FPFF....FFPF.",
    ".FPPFF..FFPPF.",
    ".FFFFFFFFFFFF.",
    "..FFFFFFFFFF..",
    "...FFFFFFFF...",
]


def ears_icon(p):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    base, inner = rgb(p["base"]), rgb(p["inner"])
    px = img.load()
    for ry, row in enumerate(EARS_ICON):
        for rx, ch in enumerate(row):
            if ch == "F":
                px[rx + 1, ry + 4] = base
            elif ch == "P":
                px[rx + 1, ry + 4] = inner
    icon_outline(img, rgb(p["outline"]))
    return img


def tail_icon(p):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    base, tip = rgb(p["base"]), rgb(p["tip"])
    stripe = rgb(p["stripe"]) if p["stripe"] else None
    px = img.load()
    p0, p1, p2, p3 = (3.0, 13.5), (2.0, 6.0), (10.0, 12.0), (12.0, 3.0)
    steps = 120
    for i in range(steps + 1):
        t = i / steps
        x = (1 - t) ** 3 * p0[0] + 3 * (1 - t) ** 2 * t * p1[0] + 3 * (1 - t) * t ** 2 * p2[0] + t ** 3 * p3[0]
        y = (1 - t) ** 3 * p0[1] + 3 * (1 - t) ** 2 * t * p1[1] + 3 * (1 - t) * t ** 2 * p2[1] + t ** 3 * p3[1]
        color = tip if t > 0.85 else (stripe if stripe and int(t * 10) % 3 == 1 else base)
        for ox in range(-2, 3):
            for oy in range(-2, 3):
                cx, cy = int(round(x)) + ox, int(round(y)) + oy
                if 1 <= cx <= 14 and 1 <= cy <= 14 and math.hypot(cx - x, cy - y) <= 1.3:
                    px[cx, cy] = color
    icon_outline(img, rgb(p["outline"]))
    return img


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main():
    lang_ja = {"itemGroup.nekomimi": "猫耳としっぽ"}
    lang_en = {"itemGroup.nekomimi": "Nekomimi"}

    for color_id, p in COLORS.items():
        for kind in ("nekomimi", "neko_tail"):
            item_id = f"{kind}_{color_id}"

            # armor (worn) texture
            tex_dir = ASSETS / "textures/models/armor"
            tex_dir.mkdir(parents=True, exist_ok=True)
            armor_texture(kind, p).save(tex_dir / f"{item_id}.png")

            # inventory icon
            icon_dir = ASSETS / "textures/item"
            icon_dir.mkdir(parents=True, exist_ok=True)
            (ears_icon(p) if kind == "nekomimi" else tail_icon(p)).save(icon_dir / f"{item_id}.png")

            # item model
            write_json(ASSETS / f"models/item/{item_id}.json", {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"{MODID}:item/{item_id}"},
            })

            # recipe
            if kind == "nekomimi":
                pattern = ["W W", "WLW"]
            else:
                pattern = ["  W", " W ", "LW "]
            write_json(DATA / f"recipes/{item_id}.json", {
                "type": "minecraft:crafting_shaped",
                "category": "equipment",
                "pattern": pattern,
                "key": {"W": {"item": p["wool"]}, "L": {"item": "minecraft:leather"}},
                "result": {"item": f"{MODID}:{item_id}"},
            })

            if kind == "nekomimi":
                lang_ja[f"item.{MODID}.{item_id}"] = f"{p['ja']}猫耳"
                lang_en[f"item.{MODID}.{item_id}"] = f"{p['en']} Cat Ears"
            else:
                lang_ja[f"item.{MODID}.{item_id}"] = f"{p['ja']}猫のしっぽ"
                lang_en[f"item.{MODID}.{item_id}"] = f"{p['en']} Cat Tail"

    write_json(ASSETS / "lang/ja_jp.json", lang_ja)
    write_json(ASSETS / "lang/en_us.json", lang_en)
    print("generated assets for", ", ".join(COLORS))


if __name__ == "__main__":
    main()
