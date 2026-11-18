#!/usr/bin/env python3
"""Generate textures, item models, recipes and language files for the Nekomimi mod.

Usage (from the repository root):  python3 tools/generate_assets.py
Requires Pillow.
"""
import json
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import fur_texture  # noqa: E402

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


# Metal versions: surround the cosmetic item with 8 of the ingot (gem for diamond) in a crafting table.
# They look the same when worn and only gain the armor stats of that metal's helmet / leggings.
METALS = {
    "iron": dict(ingot="minecraft:iron_ingot", ja="鉄", en="Iron"),
    "gold": dict(ingot="minecraft:gold_ingot", ja="金", en="Gold"),
    "diamond": dict(ingot="minecraft:diamond", ja="ダイヤモンド", en="Diamond"),
    "netherite": dict(ingot="minecraft:netherite_ingot", ja="ネザライト", en="Netherite"),
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
    """Worn texture: layered fur, see fur_texture.py (the layout is shared with FurLayout.java)."""
    return fur_texture.ear_texture(p) if kind == "nekomimi" else fur_texture.tail_texture(p)


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
    fur_texture.load_layout(ROOT)
    lang_ja = {"itemGroup.nekomimi": "猫耳としっぽ"}
    lang_en = {"itemGroup.nekomimi": "Nekomimi"}

    # Controls, messages and the emotion menu (cat mode).
    lang_ja.update({
        "key.categories.nekomimi": "猫耳としっぽ",
        "key.nekomimi.emotion_menu": "感情メニューを開く（猫モード）",
        "key.nekomimi.pose_menu": "ポーズメニューを開く（猫モード）",
        "key.nekomimi.toggle_mode": "モード切り替え（Alt＋右クリックでも可）",
        "message.nekomimi.mode.cat": "猫モード",
        "message.nekomimi.mode.normal": "通常モード",
        "message.nekomimi.no_space": "ここでは立ち上がれません",
        "message.nekomimi.leap_distance": "ロングジャンプ：約%sブロック先に着地",
        "message.nekomimi.leap_far": "ロングジャンプ：着地点が遠すぎます",
        "message.nekomimi.need_cat_mode": "感情とポーズは猫モードで選べます（Alt＋右クリックで切り替え）",
        "nekomimi.emotion.title": "今の気持ち",
        "nekomimi.emotion.none": "ふつう",
        "nekomimi.emotion.happy": "嬉しい",
        "nekomimi.emotion.relaxed": "リラックス",
        "nekomimi.emotion.annoyed": "イライラ",
        "nekomimi.emotion.scared": "怖い",
        "nekomimi.emotion.surprised": "驚き・威嚇",
        "nekomimi.emotion.interested": "興味",
        "nekomimi.emotion.affectionate": "甘えたい",
        "nekomimi.pose.title": "ポーズ",
        "nekomimi.pose.none": "立つ",
        "nekomimi.pose.sit": "座る",
        "nekomimi.pose.loaf": "香箱座り",
        "nekomimi.pose.stretch": "伸び",
        "nekomimi.pose.groom": "毛づくろい",
        "nekomimi.pose.sleep": "丸まって寝る",
    })
    lang_en.update({
        "key.categories.nekomimi": "Nekomimi",
        "key.nekomimi.emotion_menu": "Open emotion menu (cat mode)",
        "key.nekomimi.pose_menu": "Open pose menu (cat mode)",
        "key.nekomimi.toggle_mode": "Switch mode (also Alt + right click)",
        "message.nekomimi.mode.cat": "Cat mode",
        "message.nekomimi.mode.normal": "Normal mode",
        "message.nekomimi.no_space": "There is no room to stand up here",
        "message.nekomimi.leap_distance": "Long jump: lands about %s blocks ahead",
        "message.nekomimi.leap_far": "Long jump: the landing is too far to show",
        "message.nekomimi.need_cat_mode": "Emotions and poses are picked in cat mode (Alt + right click to switch)",
        "nekomimi.emotion.title": "Mood",
        "nekomimi.emotion.none": "Normal",
        "nekomimi.emotion.happy": "Happy",
        "nekomimi.emotion.relaxed": "Relaxed",
        "nekomimi.emotion.annoyed": "Annoyed",
        "nekomimi.emotion.scared": "Scared",
        "nekomimi.emotion.surprised": "Startled",
        "nekomimi.emotion.interested": "Curious",
        "nekomimi.emotion.affectionate": "Affectionate",
        "nekomimi.pose.title": "Pose",
        "nekomimi.pose.none": "Stand",
        "nekomimi.pose.sit": "Sit",
        "nekomimi.pose.loaf": "Loaf",
        "nekomimi.pose.stretch": "Stretch",
        "nekomimi.pose.groom": "Groom",
        "nekomimi.pose.sleep": "Curl up and sleep",
    })

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

    for color_id, p in COLORS.items():
        for kind in ("nekomimi", "neko_tail"):
            base_id = f"{kind}_{color_id}"
            for metal_id, m in METALS.items():
                item_id = f"{base_id}_{metal_id}"

                # same icon as the cosmetic version
                write_json(ASSETS / f"models/item/{item_id}.json", {
                    "parent": "minecraft:item/generated",
                    "textures": {"layer0": f"{MODID}:item/{base_id}"},
                })

                # ring recipe: cosmetic item in the middle, 8 ingots around it
                write_json(DATA / f"recipes/{item_id}.json", {
                    "type": "minecraft:crafting_shaped",
                    "category": "equipment",
                    "pattern": ["III", "IXI", "III"],
                    "key": {
                        "I": {"item": m["ingot"]},
                        "X": {"item": f"{MODID}:{base_id}"},
                    },
                    "result": {"item": f"{MODID}:{item_id}"},
                })

                if kind == "nekomimi":
                    lang_ja[f"item.{MODID}.{item_id}"] = f"{p['ja']}猫耳（{m['ja']}）"
                    lang_en[f"item.{MODID}.{item_id}"] = f"{p['en']} Cat Ears ({m['en']})"
                else:
                    lang_ja[f"item.{MODID}.{item_id}"] = f"{p['ja']}猫のしっぽ（{m['ja']}）"
                    lang_en[f"item.{MODID}.{item_id}"] = f"{p['en']} Cat Tail ({m['en']})"

    write_json(ASSETS / "lang/ja_jp.json", lang_ja)
    write_json(ASSETS / "lang/en_us.json", lang_en)
    print("generated assets for", ", ".join(COLORS))


if __name__ == "__main__":
    main()
