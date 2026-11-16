"""Fur ("shell") textures for the cat ears and the cat tail.

The numbers (region sizes, layer counts...) must match FurLayout.java. load_layout() reads them from
that file and stops if they differ from the ones in LAYOUT below, so the two cannot drift apart.

Every part is drawn as LAYERS boxes. Layer 0 is solid. Layers 1..SHELLS only keep the texels of the
hairs that reach that far; each texel gets one random "length" that is the same in all layers, so a
hair is a column of texels through the layers.
"""
import json
import random
import re
from pathlib import Path

from PIL import Image

SHELLS = 10
LAYERS = SHELLS + 1

# name -> value, checked against FurLayout.java
LAYOUT = dict(
    SHELLS=10, TEXELS=4,
    TAIL_TEX_WIDTH=256, TAIL_TEX_HEIGHT=128, TAIL_BOX_WIDTH=8, TAIL_BOX_HEIGHT=8,
    TAIL_REGION_WIDTH=32, TAIL_REGION_HEIGHT=16, TAIL_COLUMNS=8,
    TAIL_VARIANT_FUR=0, TAIL_VARIANT_STRIPE=1, TAIL_VARIANT_TIP=2,
    EAR_TEX_WIDTH=256, EAR_TEX_HEIGHT=64, EAR_PIECES=3,
    EAR_BOX=[[12, 6, 8], [8, 6, 8], [4, 4, 4]], EAR_COLUMNS=[6, 8, 16], EAR_TOP=[0, 28, 56],
)


def load_layout(root):
    """Check LAYOUT against FurLayout.java and return it."""
    src = (Path(root) / "src/main/java/io/github/napuruhusu/nekomimi/client/FurLayout.java").read_text()
    found = {}
    for m in re.finditer(r"public static final int((?:\[\])*) (\w+) = ([^;]+);", src):
        text = m.group(3).strip()
        if not m.group(1) and not re.fullmatch(r"-?\d+", text):
            continue  # a computed value such as SHELLS + 1
        found[m.group(2)] = json.loads(text.replace("{", "[").replace("}", "]")) if m.group(1) else int(text)
    for name, value in LAYOUT.items():
        if name not in found:
            raise SystemExit(f"FurLayout.java has no constant {name}")
        if found[name] != value:
            raise SystemExit(f"FurLayout.{name} = {found[name]} but fur_texture.py expects {value}")
    return LAYOUT


# ---- layout helpers (same formulas as FurLayout.java) ----

def tail_origin(variant, layer):
    i = variant * LAYERS + layer
    return (i % LAYOUT["TAIL_COLUMNS"]) * LAYOUT["TAIL_REGION_WIDTH"], \
           (i // LAYOUT["TAIL_COLUMNS"]) * LAYOUT["TAIL_REGION_HEIGHT"]


def ear_origin(piece, layer):
    w, h, d = LAYOUT["EAR_BOX"][piece]
    cols = LAYOUT["EAR_COLUMNS"][piece]
    return (layer % cols) * (2 * d + 2 * w), LAYOUT["EAR_TOP"][piece] + (layer // cols) * (d + h)


def faces(w, h, d):
    """The six faces of a box UV layout, as name -> (x, y, width, height) relative to the region."""
    return {
        "top": (d, 0, w, d),
        "bottom": (d + w, 0, w, d),
        "right": (0, d, d, h),
        "front": (d, d, w, h),
        "left": (d + w, d, d, h),
        "back": (2 * d + w, d, w, h),
    }


# ---- colors ----

def hex_rgb(value):
    v = value.lstrip("#")
    return tuple(int(v[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(a[i] * (1 - t) + b[i] * t for i in range(3))


def shade(c, t):
    """t < 0 darkens toward black, t > 0 lightens toward white."""
    return mix(c, (255, 255, 255), t) if t > 0 else mix(c, (0, 0, 0), -t)


def to_px(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c) + (255,)


# ---- strand lengths ----

def visible_fraction(layer):
    """Share of the hairs that reach layer `layer` (1..SHELLS)."""
    return (1.0 - layer / (SHELLS + 1.0)) ** 1.3


class Strands:
    """One random hair length (rank 0..1, small = long) and one color jitter per texel of a box."""

    def __init__(self, seed, w, h, d):
        rng = random.Random(seed)
        self.rects = faces(w, h, d)
        raw = {}
        for name, (fx, fy, fw, fh) in self.rects.items():
            # Clumps: hairs next to each other are a bit alike, like locks of fur.
            cw, ch = fw // 2 + 2, fh // 2 + 2
            clump = [[rng.random() for _ in range(cw)] for _ in range(ch)]
            for y in range(fh):
                for x in range(fw):
                    raw[(name, x, y)] = 0.72 * rng.random() + 0.28 * clump[y // 2][x // 2]
        order = sorted(raw, key=raw.get)
        self.rank = {key: i / len(order) for i, key in enumerate(order)}
        self.jitter = {key: rng.random() - 0.5 for key in raw}


def paint_region(img, origin, strands, layer, color_at):
    """Draw one layer of one box. color_at(face, x, y, rank, jitter, layer) -> (r, g, b) or None for 'no hair here'."""
    ox, oy = origin
    px = img.load()
    for name, (fx, fy, fw, fh) in strands.rects.items():
        for y in range(fh):
            for x in range(fw):
                key = (name, x, y)
                color = color_at(name, x, y, strands.rank[key], strands.jitter[key], layer)
                if color is not None:
                    px[ox + fx + x, oy + fy + y] = to_px(color)


def fur_color(base, tip, depth, jitter):
    """Root is darker and a little richer, the tip lighter; every hair is slightly different."""
    root = shade(base, -0.28)
    c = mix(root, mix(base, tip, 0.35), depth ** 0.9)
    c = shade(c, jitter * 0.16 + 0.04 * depth)
    return c


def fur_rule(base, tip, reach=1.0):
    def color_at(face, x, y, rank, jitter, layer):
        if layer > 0 and rank >= visible_fraction(layer) * reach:
            return None
        return fur_color(base, tip, layer / SHELLS, jitter)
    return color_at


# ---- tail ----

def tail_texture(palette):
    img = Image.new("RGBA", (LAYOUT["TAIL_TEX_WIDTH"], LAYOUT["TAIL_TEX_HEIGHT"]), (0, 0, 0, 0))
    base = hex_rgb(palette["base"])
    stripe = hex_rgb(palette["stripe"]) if palette["stripe"] else base
    tip = hex_rgb(palette["tip"])
    variants = {
        LAYOUT["TAIL_VARIANT_FUR"]: (base, shade(base, 0.22)),
        LAYOUT["TAIL_VARIANT_STRIPE"]: (stripe, shade(stripe, 0.22)),
        LAYOUT["TAIL_VARIANT_TIP"]: (tip, shade(tip, 0.3)),
    }
    w, h = LAYOUT["TAIL_BOX_WIDTH"], LAYOUT["TAIL_BOX_HEIGHT"]
    for variant, (body, highlight) in variants.items():
        strands = Strands(f"tail-{variant}-{palette['base']}", w, h, w)
        for layer in range(LAYERS):
            paint_region(img, tail_origin(variant, layer), strands, layer, fur_rule(body, highlight))
    return img


# ---- ears ----

# Pink patch on the front face of each piece, as (x0, x1, y0, y1) in texels; together they make a triangle.
EAR_INNER = {0: (3, 9, 0, 6), 1: (2, 6, 0, 6)}


def ear_texture(palette):
    img = Image.new("RGBA", (LAYOUT["EAR_TEX_WIDTH"], LAYOUT["EAR_TEX_HEIGHT"]), (0, 0, 0, 0))
    base = hex_rgb(palette["base"])
    highlight = shade(base, 0.22)
    inner = hex_rgb(palette["inner"])
    # The soft hair inside the ear is paler and shorter.
    inner_tip = shade(inner, 0.45)
    plain = fur_rule(base, highlight)
    soft = fur_rule(inner, inner_tip, reach=0.7)

    for piece, (w, h, d) in enumerate(LAYOUT["EAR_BOX"]):
        patch = EAR_INNER.get(piece)

        def color_at(face, x, y, rank, jitter, layer, patch=patch):
            if patch and face == "front" and patch[0] <= x < patch[1] and patch[2] <= y < patch[3]:
                return soft(face, x, y, rank, jitter, layer)
            return plain(face, x, y, rank, jitter, layer)

        strands = Strands(f"ear-{piece}-{palette['base']}", w, h, d)
        for layer in range(LAYERS):
            paint_region(img, ear_origin(piece, layer), strands, layer, color_at)
    return img
