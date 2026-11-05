#!/usr/bin/env python3
"""Genera el icono de Melodia (adaptive + mipmaps legacy + icono de tienda).

Uso: python3 tools/build_icon.py [--check]
- Renderiza el SVG con Chromium headless y deriva todos los tamaños con LANCZOS.
- --check solo informa geometría del render, sin escribir recursos.
"""
from __future__ import annotations

import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
CHROME = sorted(Path.home().glob(".cache/ms-playwright/chromium_headless_shell-*/chrome-linux/headless_shell"))[-1]

BG = "#0A1510"
FG = "#2EE56B"
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

# Corchea centrada en el viewport 108x108: bbox x 29..79, y 24.5..83.5 (centro 54,54).
NOTE = [
    # Cabeza: elipse inclinada ~-20 grados.
    "M40.5,74.5 m-11.5,0 a11.5,8.8 -20 1,0 23,0 a11.5,8.8 -20 1,0 -23,0",
    # Mástil.
    "M51,24.5 h7.5 v50 h-7.5 z",
    # Bandera curva que cae hacia la derecha.
    "M58.5,24.5 C66.5,24.5 75.5,30 78.5,42.5 C74.5,35.5 67.5,31.5 58.5,31.5 Z",
]

SVG_FULL = """<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 108 108">
<rect width="108" height="108" fill="{bg}"/>
{paths}
</svg>"""

SVG_FG = """<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 108 108">
{paths}
</svg>"""


def svg(paths: list[str], background: str | None) -> str:
    body = "\n".join(f'<path d="{d}" fill="{FG}"/>' for d in paths)
    if background:
        return SVG_FULL.format(bg=background, paths=body)
    return SVG_FG.format(paths=body)


def render(svg_text: str, out: Path) -> Image.Image:
    html = Path(tempfile.mkdtemp(prefix="melodia-icon-")) / "i.html"
    html.write_text(
        "<!doctype html><meta charset=utf-8><style>html,body{margin:0;background:transparent}"
        "svg{display:block}</style>" + svg_text,
        encoding="utf-8",
    )
    subprocess.run(
        [str(CHROME), "--headless", "--disable-gpu", "--hide-scrollbars", "--no-sandbox",
         "--default-background-color=00000000", "--window-size=1024,1024",
         "--virtual-time-budget=2000", f"--screenshot={out}", html.as_uri()],
        check=True, capture_output=True, timeout=120,
    )
    return Image.open(out).convert("RGBA")


def rounded_mask(size: int, radius_ratio: float) -> Image.Image:
    scale = 4
    big = Image.new("L", (size * scale, size * scale), 0)
    ImageDraw.Draw(big).rounded_rectangle(
        (0, 0, size * scale - 1, size * scale - 1), radius=int(size * scale * radius_ratio), fill=255
    )
    return big.resize((size, size), Image.LANCZOS)


def circle_mask(size: int) -> Image.Image:
    scale = 4
    big = Image.new("L", (size * scale, size * scale), 0)
    ImageDraw.Draw(big).ellipse((0, 0, size * scale - 1, size * scale - 1), fill=255)
    return big.resize((size, size), Image.LANCZOS)


def mask_of(im: Image.Image, mask: Image.Image) -> Image.Image:
    out = im.copy()
    out.putalpha(mask)
    return out


def geometry(im: Image.Image) -> dict:
    alpha = im.getchannel("A")
    bbox = alpha.getbbox()
    w, h = im.size
    total = sum(alpha.getdata())
    cx = cy = 0.0
    if total:
        px = alpha.load()
        for y in range(h):
            for x in range(w):
                a = px[x, y]
                if a:
                    cx += x * a
                    cy += y * a
    return {
        "bbox": bbox,
        "bbox_ratio": None if not bbox else (
            round((bbox[0]) / w, 3), round(bbox[1] / h, 3),
            round((w - bbox[2]) / w, 3), round((h - bbox[3]) / h, 3),
        ),
        "centroid_ratio": (round(cx / total / w, 4), round(cy / total / h, 4)) if total else None,
        "coverage": round(total / (255 * w * h), 4),
    }


def main() -> None:
    check = "--check" in sys.argv
    tmp = Path(tempfile.mkdtemp(prefix="melodia-icon-out-"))
    full = render(svg(NOTE, BG), tmp / "full.png")
    fg = render(svg(NOTE, None), tmp / "fg.png")

    print("FULL", geometry(full))
    print("FOREGROUND", geometry(fg))

    # Zona segura del icono adaptativo: círculo de 66/108 centrado.
    bbox = fg.getchannel("A").getbbox()
    safe_lo, safe_hi = 108 * (1 - 66 / 108) / 2, 108 - 108 * (1 - 66 / 108) / 2
    assert bbox, "El foreground quedó vacío"
    lo, hi = bbox[0] / 1024 * 108, bbox[2] / 1024 * 108
    top, bottom = bbox[1] / 1024 * 108, bbox[3] / 1024 * 108
    print("SAFE_ZONE", round(safe_lo, 2), round(safe_hi, 2), "content", round(lo, 2), round(hi, 2),
          round(top, 2), round(bottom, 2))
    assert lo >= safe_lo and hi <= safe_hi and top >= safe_lo and bottom <= safe_hi, "La nota sale de la zona segura"

    if check:
        return

    square = full.resize((1024, 1024), Image.LANCZOS)
    legacy = mask_of(square, rounded_mask(1024, 0.225))
    round_icon = mask_of(square, circle_mask(1024))

    for density, size in SIZES.items():
        d = RES / f"mipmap-{density}"
        d.mkdir(parents=True, exist_ok=True)
        legacy.resize((size, size), Image.LANCZOS).save(d / "ic_launcher.png", optimize=True)
        round_icon.resize((size, size), Image.LANCZOS).save(d / "ic_launcher_round.png", optimize=True)

    store = ROOT / "docs/store"
    previous = store / "sources/icon-512-anterior.png"
    if (store / "icon-512.png").exists() and not previous.exists():
        previous.parent.mkdir(parents=True, exist_ok=True)
        previous.write_bytes((store / "icon-512.png").read_bytes())
    legacy.resize((512, 512), Image.LANCZOS).save(store / "icon-512.png", optimize=True)
    print("ICONOS_GENERADOS", sorted(SIZES), "tienda", store / "icon-512.png")


if __name__ == "__main__":
    main()
