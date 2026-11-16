#!/usr/bin/env python3
"""Rough software preview of the fur shells (no Minecraft needed).

Draws the tail chain and the ears from the generated textures the way the models build them:
layered boxes, grown by the fur length, with only the opaque texels kept.
Usage: python3 tools/preview_fur.py <color> <out.png> [yaw_degrees]
This is only a rough check of the look: it does not copy Minecraft's lighting or its exact face orientation.
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import fur_texture as ft  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
ARMOR = ROOT / "src/main/resources/assets/nekomimi/textures/models/armor"
ft.load_layout(ROOT)
L = ft.LAYOUT
SHELLS, LAYERS, PART_SCALE = L["SHELLS"], L["SHELLS"] + 1, 0.25
TAIL_FUR, EAR_FUR = 1.1, 0.4


def rot_x(a):
    c, s = np.cos(a), np.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def rot_y(a):
    c, s = np.cos(a), np.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


class Scene:
    def __init__(self, size=(600, 600), scale=14.0, yaw=35.0, pitch=15.0, center=(0, 0, 0)):
        self.w, self.h = size
        self.scale = scale
        self.view = rot_x(np.radians(pitch)) @ rot_y(np.radians(yaw))
        self.center = np.array(center, float)
        self.color = np.zeros((self.h, self.w, 3))
        self.color[:] = (0.55, 0.72, 0.9)
        self.depth = np.full((self.h, self.w), 1e9)

    def box(self, tex, origin, size3, grow3, pos, rot, scale3, density=3.0):
        """One shell box. size3 = (w, h, d) in texels, box spans [-w/2, w/2] x [0 or -h/2...]: see callers."""
        ox, oy = origin
        w, h, d = size3
        faces = ft.faces(w, h, d)
        gx, gy, gz = grow3
        lo = np.array(size3[:0] or [0, 0, 0], float)
        # box corners in local texels, given by caller through self._lo
        lo = np.array(self._lo, float) - (gx, gy, gz)
        hi = np.array(self._lo, float) + (w, h, d) + (gx, gy, gz)
        # face definitions: name -> (origin corner, u axis, v axis, normal)
        x0, y0, z0 = lo
        x1, y1, z1 = hi
        defs = {
            "front": ((x0, y0, z0), (1, 0, 0), (0, 1, 0), (0, 0, -1)),
            "back": ((x1, y0, z1), (-1, 0, 0), (0, 1, 0), (0, 0, 1)),
            "right": ((x0, y0, z1), (0, 0, -1), (0, 1, 0), (-1, 0, 0)),
            "left": ((x1, y0, z0), (0, 0, 1), (0, 1, 0), (1, 0, 0)),
            "top": ((x0, y0, z1), (1, 0, 0), (0, 0, -1), (0, -1, 0)),
            "bottom": ((x0, y1, z0), (1, 0, 0), (0, 0, 1), (0, 1, 0)),
        }
        for name, (o, u, v, n) in defs.items():
            fx, fy, fw, fh = faces[name]
            o = np.array(o, float)
            ua, va = np.array(u, float), np.array(v, float)
            ulen = np.dot(hi - lo, np.abs(ua))
            vlen = np.dot(hi - lo, np.abs(va))
            nu, nv = int(fw * density), int(fh * density)
            su = (np.arange(nu) + 0.5) / nu
            sv = (np.arange(nv) + 0.5) / nv
            tx = np.minimum((su * fw).astype(int), fw - 1)
            ty = np.minimum((sv * fh).astype(int), fh - 1)
            rgba = tex[oy + fy + ty[:, None], ox + fx + tx[None, :]]
            mask = rgba[..., 3] > 0
            if not mask.any():
                continue
            S, T = np.meshgrid(su, sv)
            pts = o + S[..., None] * ulen * ua + T[..., None] * vlen * va
            pts = pts[mask]
            cols = rgba[mask][:, :3] / 255.0
            local = pts * np.array(scale3)
            world = pos + local @ rot.T
            nrm = rot @ (np.array(n, float) * np.array([1, 1, 1]))
            cam = (world - self.center) @ self.view.T
            ncam = self.view @ nrm
            light = 0.62 + 0.38 * max(0.0, float(np.dot(ncam, [-0.3, -0.5, -0.8])) * -1.0 + 0.2)
            light = float(np.clip(light, 0.55, 1.0))
            px = (self.w / 2 + cam[:, 0] * self.scale).astype(int)
            py = (self.h / 2 + cam[:, 1] * self.scale).astype(int)
            ok = (px >= 0) & (px < self.w) & (py >= 0) & (py < self.h)
            px, py, z, cols = px[ok], py[ok], cam[ok, 2], cols[ok]
            order = np.argsort(-z)
            for i in order:
                if z[i] < self.depth[py[i], px[i]]:
                    self.depth[py[i], px[i]] = z[i]
                    self.color[py[i], px[i]] = cols[i] * light

    def save(self, path, zoom=1):
        img = Image.fromarray((np.clip(self.color, 0, 1) * 255).astype(np.uint8))
        img.save(path)


def tail_scene(tex, yaw, pitches, widths, sc):
    rot_acc = np.eye(3)
    pos = np.array([0.0, 0.0, 0.0])
    variants = [0, 1, 0, 1, 2, 2]
    for i in range(6):
        rot_acc = rot_acc @ rot_x(pitches[i])
        thick = (0.1 - 0.04 * i) * 4
        for k in range(LAYERS):
            grow = k * TAIL_FUR * 4 / SHELLS
            org = (ft.tail_origin(variants[i], k))
            sc._lo = (-4, 0, -4)
            sx = PART_SCALE * widths
            sc.box(tex, org, (8, 8, 8), (thick + grow, 0.8 + 0.5 * grow, thick + grow), pos, rot_acc,
                   (sx, PART_SCALE, sx), density=6.0)
        pos = pos + rot_acc @ np.array([0, 2.0, 0])


def ears_scene(tex, sc):
    centers = {"base": [(2.5, -9.0, -2.0), (-2.5, -9.0, -2.0)], "tip": [(2.5, -10.5, -2.0), (-2.5, -10.5, -2.0)]}
    for side in range(2):
        for kind, size, org in (("base", (12, 8, 8), ft.ear_base_origin), ("tip", (4, 4, 8), ft.ear_tip_origin)):
            for k in range(LAYERS):
                grow = k * EAR_FUR * 4 / SHELLS
                sc._lo = (-size[0] / 2, -size[1] / 2, -size[2] / 2)
                sc.box(tex, org(k), size, (grow, grow, grow), np.array(centers[kind][side]), np.eye(3),
                       (PART_SCALE,) * 3, density=10.0)


def main():
    color = sys.argv[1]
    out = sys.argv[2]
    yaw = float(sys.argv[3]) if len(sys.argv) > 3 else 35.0
    tail_tex = np.array(Image.open(ARMOR / f"neko_tail_{color}.png").convert("RGBA"))
    ear_tex = np.array(Image.open(ARMOR / f"nekomimi_{color}.png").convert("RGBA"))
    a = Scene(size=(520, 520), scale=22.0, yaw=yaw, pitch=-10, center=(0, 6, 0))
    # tail hanging and curling up at the tip: pitch is the angle turned away from straight down
    pitches = [0.45, 0.20, 0.20, 0.25, 0.35, 0.45]
    tail_scene(tail_tex, yaw, pitches, 1.0, a)
    b = Scene(size=(520, 520), scale=40.0, yaw=yaw, pitch=-15, center=(0, -9, -2))
    ears_scene(ear_tex, b)
    out_img = Image.new("RGB", (1040, 520))
    for i, s in enumerate((a, b)):
        out_img.paste(Image.fromarray((np.clip(s.color, 0, 1) * 255).astype(np.uint8)), (520 * i, 0))
    out_img.save(out)


if __name__ == "__main__":
    main()
