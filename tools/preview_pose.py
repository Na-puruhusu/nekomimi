#!/usr/bin/env python3
"""Rough preview of the cat poses (no Minecraft needed).

Compiles CatPoses.java (it has no game classes), prints the part placements of every pose and draws the
parts as colored boxes from three sides, with the ground and the head's face marked.
Usage: [POSE_PITCH=<radians, negative = looking up>] python3 tools/preview_pose.py <out.png> [pose ...]
(default: all poses; also CROUCH, LONG_RISE, LONG_FALL)
"""
import json
import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src/main/java/io/github/napuruhusu/nekomimi"
PARTS = ["head", "body", "front_right", "front_left", "hind_right", "hind_left"]
# box corners relative to the pivot: (x0, y0, z0, x1, y1, z1)
BOXES = {
    "head": (-4, -8, -4, 4, 0, 4),
    "body": (-4, 0, -2, 4, 12, 2),
    "front_right": (-3, -2, -2, 1, 10, 2),
    "front_left": (-1, -2, -2, 3, 10, 2),
    "hind_right": (-2, 0, -2, 2, 12, 2),
    "hind_left": (-2, 0, -2, 2, 12, 2),
}
COLORS = {
    "head": (232, 190, 150), "body": (70, 110, 190), "front_right": (200, 70, 70), "front_left": (230, 150, 80),
    "hind_right": (70, 150, 90), "hind_left": (120, 190, 110),
}


def dump_poses(pitch=0.0):
    with tempfile.TemporaryDirectory() as tmp:
        files = [SRC / "client/CatPoses.java", SRC / "state/NekoPose.java", SRC / "state/NekoLeap.java",
                 ROOT / "tools/pose_dump/PoseDump.java"]
        subprocess.run(["javac", "-d", tmp] + [str(f) for f in files], check=True,
                       stderr=subprocess.DEVNULL)
        out = subprocess.run(["java", "-cp", tmp, "io.github.napuruhusu.nekomimi.client.PoseDump", str(pitch)],
                             check=True, capture_output=True, text=True).stdout
    return json.loads(out)


def rot(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = np.cos(rx), np.sin(rx), np.cos(ry), np.sin(ry), np.cos(rz), np.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


FACES = {  # corner numbers (bits: x = 4, y = 2, z = 1; 0 = the smaller coordinate) and outward normals
    "front": ((0, 2, 6, 4), (0, 0, -1)), "back": ((1, 3, 7, 5), (0, 0, 1)),
    "left": ((0, 1, 3, 2), (-1, 0, 0)), "right": ((4, 5, 7, 6), (1, 0, 0)),
    "top": ((0, 1, 5, 4), (0, -1, 0)), "bottom": ((2, 3, 7, 6), (0, 1, 0)),
}


def part_faces(name, values, view):
    x, y, z, rx, ry, rz, sx, sy, sz = values
    x0, y0, z0, x1, y1, z1 = BOXES[name]
    corners = np.array([[(x0, x1)[i >> 2 & 1], (y0, y1)[i >> 1 & 1], (z0, z1)[i & 1]] for i in range(8)], float)
    corners *= (sx, sy, sz)
    R = rot(rx, ry, rz)
    world = corners @ R.T + (x, y, z)
    cam = world @ view.T
    base = np.array(COLORS[name], float)
    result = []
    for fname, (idx, normal) in FACES.items():
        n = view @ (R @ np.array(normal, float))
        shade = 0.55 + 0.45 * max(0.0, -n[2] * 0.7 + -n[1] * 0.3)
        color = base * shade
        if name == "head" and fname == "front":
            color = np.array([255, 235, 120])
        result.append((cam[list(idx)], color, n[2]))
    return result


def render(pose_values, yaw, pitch=-12, size=420, scale=10.0):
    view = np.array([[1, 0, 0], [0, np.cos(np.radians(pitch)), -np.sin(np.radians(pitch))],
                     [0, np.sin(np.radians(pitch)), np.cos(np.radians(pitch))]]) @ rot(0, np.radians(yaw), 0)
    img = Image.new("RGB", (size, size), (150, 190, 230))
    d = ImageDraw.Draw(img)
    # ground
    g = np.array([[-20, 24, -20], [20, 24, -20], [20, 24, 20], [-20, 24, 20]], float) @ view.T
    d.polygon([(size / 2 + p[0] * scale, size / 2 + (p[1] - 12) * scale) for p in g], fill=(95, 150, 85))
    polys = []
    for i, name in enumerate(PARTS):
        values = pose_values[i * 9:(i + 1) * 9]
        for cam, color, nz in part_faces(name, values, view):
            polys.append((cam[:, 2].mean(), cam, color))
    for _, cam, color in sorted(polys, key=lambda t: -t[0]):
        pts = [(size / 2 + p[0] * scale, size / 2 + (p[1] - 12) * scale) for p in cam]
        d.polygon(pts, fill=tuple(int(c) for c in color), outline=(20, 20, 20))
    return img


def main():
    out = sys.argv[1]
    import os
    poses = dump_poses(float(os.environ.get("POSE_PITCH", "0")))
    names = sys.argv[2:] or list(poses)
    views = (90, 35, 215)
    sheet = Image.new("RGB", (420 * len(views), 420 * len(names)))
    for r, name in enumerate(names):
        for c, yaw in enumerate(views):
            img = render(poses[name.upper()], yaw)
            ImageDraw.Draw(img).text((8, 8), f"{name} (view {yaw})", fill=(0, 0, 0))
            sheet.paste(img, (420 * c, 420 * r))
    sheet.save(out)


if __name__ == "__main__":
    main()
