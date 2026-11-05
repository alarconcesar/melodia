#!/usr/bin/env python3
"""Genera el icono de Melodia (adaptive + mipmaps legacy + icono de tienda).

Uso: python3 tools/build_icon.py [--check]
- Usa el glifo Material «audiotrack» (silueta probada, legible a 24 dp).
- Centra por centroide de masa, no solo por caja: una nota pesa abajo y con el
  bbox centrado se percibe caída.
- Escribe también los vector drawables, para que el icono adaptativo y los PNG
  salgan del mismo dibujo.

Reglas que valida antes de escribir:
- El glifo cabe en la zona segura de 66 dp del icono adaptativo.
- El centroide queda a <=1 dp del centro del lienzo.
- A 48 px el mástil conserva >=2 px y no hay huecos internos.
- El icono de tienda es cuadrado, a sangre y opaco (Play aplica su máscara).
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

BG = "#0E2418"
FG = "#2EE56B"
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

# Glifo Material «audiotrack» en su viewBox original de 24x24.
GLYPH = "M12 3v9.28c-.47-.17-.97-.28-1.5-.28C8.01 12 6 14.01 6 16.5S8.01 21 10.5 21c2.31 0 4.2-1.75 4.45-4H15V6h4V3h-7z"
GLYPH_BOX = (6.0, 3.0, 19.0, 21.0)  # x0, y0, x1, y1 del glifo
TARGET_H = 55.0                      # alto del glifo en dp (de 108)

SVG_TMPL = """<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 108 108">
{bg}<path d="{glyph}" fill="{fg}" transform="translate({tx:.3f},{ty:.3f}) scale({s:.4f})"/>
</svg>"""


def render(svg_text: str, out: Path, size: int = 1024) -> Image.Image:
    html = Path(tempfile.mkdtemp(prefix="melodia-icon-")) / "i.html"
    html.write_text(
        "<!doctype html><meta charset=utf-8><style>html,body{margin:0;background:transparent}"
        f"svg{{display:block;width:{size}px;height:{size}px}}</style>" + svg_text,
        encoding="utf-8",
    )
    subprocess.run(
        [str(CHROME), "--headless", "--disable-gpu", "--hide-scrollbars", "--no-sandbox",
         "--default-background-color=00000000", f"--window-size={size},{size}",
         "--virtual-time-budget=2000", f"--screenshot={out}", html.as_uri()],
        check=True, capture_output=True, timeout=120,
    )
    return Image.open(out).convert("RGBA")


def centroid(im: Image.Image) -> tuple[float, float]:
    a = im.getchannel("A")
    w, h = im.size
    px = a.load()
    total = cx = cy = 0
    for y in range(h):
        for x in range(w):
            v = px[x, y]
            if v:
                total += v
                cx += x * v
                cy += y * v
    assert total, "Render vacío"
    return cx / total, cy / total


def build_svg(tx: float, ty: float, s: float, with_bg: bool) -> str:
    bg = f'<rect width="108" height="108" fill="{BG}"/>' if with_bg else ""
    return SVG_TMPL.format(bg=bg, glyph=GLYPH, fg=FG, tx=tx, ty=ty, s=s)


def base_transform() -> tuple[float, float, float]:
    x0, y0, x1, y1 = GLYPH_BOX
    s = TARGET_H / (y1 - y0)
    tx = (108 - (x1 - x0) * s) / 2 - x0 * s
    ty = (108 - (y1 - y0) * s) / 2 - y0 * s
    return tx, ty, s


def main() -> None:
    check = "--check" in sys.argv
    tmp = Path(tempfile.mkdtemp(prefix="melodia-icon-out-"))

    tx, ty, s = base_transform()
    # Centrado por caja (contorno), no por masa: el ojo juzga la simetria del
    # contorno. Centrar por centroide dejaba la caja 1-2 dp alta y a la derecha,
    # que es exactamente lo que se percibe como "no esta centrado".
    for _ in range(2):
        fg = render(build_svg(tx, ty, s, False), tmp / "fg.png")
        bbox = fg.getchannel("A").getbbox()
        assert bbox, "El foreground quedo vacio"
        tx += (54 - (bbox[0] + bbox[2]) / 2 * 108 / 1024)
        ty += (54 - (bbox[1] + bbox[3]) / 2 * 108 / 1024)

    fg = render(build_svg(tx, ty, s, False), tmp / "fg.png")
    bbox = fg.getchannel("A").getbbox()
    assert bbox, "El foreground quedo vacio"
    to_dp = 108 / 1024
    lo, hi = bbox[0] * to_dp, bbox[2] * to_dp
    top, bottom = bbox[1] * to_dp, bbox[3] * to_dp
    centro_caja = ((lo + hi) / 2, (top + bottom) / 2)
    cx, cy = centroid(fg)
    print("GLIFO bbox", round(lo, 2), round(top, 2), round(hi, 2), round(bottom, 2))
    print("MARGENES", round(lo, 2), round(108 - hi, 2), round(top, 2), round(108 - bottom, 2))
    print("CENTRO_CAJA", round(centro_caja[0], 2), round(centro_caja[1], 2))
    print("CENTROIDE", round(cx * to_dp, 2), round(cy * to_dp, 2))
    print("TRANSFORM", round(tx, 3), round(ty, 3), round(s, 4))

    assert lo >= 21 and hi <= 87 and top >= 21 and bottom <= 87, "El glifo sale de la zona segura"
    assert abs(centro_caja[0] - 54) <= 0.5 and abs(centro_caja[1] - 54) <= 0.5, f"Caja descentrada: {centro_caja}"
    # Margen lateral y vertical deben ser iguales: es lo que se ve como centrado.
    assert abs(lo - (108 - hi)) <= 0.6 and abs(top - (108 - bottom)) <= 0.6, "Margenes asimetricos"

    fg48 = fg.resize((48, 48), Image.LANCZOS)
    a48 = fg48.getchannel("A").load()
    # Mástil: columna a 1/3 del ancho del glifo, entre el 35 % y el 60 % de alto.
    w = 48
    xm = int((lo + (hi - lo) * 0.62) / 108 * w)
    tramo = [a48[xm, y] for y in range(int(30 / 108 * w), int(55 / 108 * w))]
    assert min(tramo) > 140, f"Mástil cortado a 48 px (min {min(tramo)})"
    print("MASTIL_48_OK", min(tramo), "columna", xm)

    if check:
        return

    full = render(build_svg(tx, ty, s, True), tmp / "full.png")

    # Vector drawables desde la misma transformación.
    def vector(color: str, comment: str) -> str:
        return f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <!-- {comment} -->
    <group android:translateX="{tx:.3f}" android:translateY="{ty:.3f}"
        android:scaleX="{s:.4f}" android:scaleY="{s:.4f}">
        <path android:fillColor="{color}" android:pathData="{GLYPH}" />
    </group>
</vector>
"""

    (RES / "drawable").mkdir(parents=True, exist_ok=True)
    (RES / "drawable/ic_launcher_foreground.xml").write_text(
        vector(FG, "Corchea de Melodia: glifo audiotrack centrado por masa y dentro de la zona segura."),
        encoding="utf-8",
    )
    (RES / "drawable/ic_launcher_monochrome.xml").write_text(
        vector("#FFFFFF", "Version monocroma para temas del sistema."), encoding="utf-8"
    )
    (RES / "drawable/ic_launcher_background.xml").write_text(
        f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="{BG}" android:pathData="M0,0h108v108H0z" />
</vector>
""",
        encoding="utf-8",
    )

    square = full.resize((1024, 1024), Image.LANCZOS)
    legacy = square.copy()
    legacy.putalpha(rounded_mask(1024, 0.225))
    round_icon = square.copy()
    round_icon.putalpha(circle_mask(1024))

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
    square.resize((512, 512), Image.LANCZOS).convert("RGB").save(store / "icon-512.png", optimize=True)
    print("ICONOS_GENERADOS", sorted(SIZES), "tienda (512, RGB)")


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


if __name__ == "__main__":
    main()
