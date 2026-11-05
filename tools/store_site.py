#!/usr/bin/env python3
"""Genera la página pública desde PRIVACY.md; no publica notas internas ni credenciales."""
from pathlib import Path
from html import escape
import argparse
import re
import shutil

ROOT = Path(__file__).resolve().parents[1]

def inline(value: str) -> str:
    value = escape(value)
    value = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", value)
    # Solo enlaces HTTPS o anclas del documento, sin HTML arbitrario.
    return re.sub(r"\[([^\]]+)\]\((https://[^\s)]+|#[^\s)]+)\)",
                  r'<a href="\2" rel="noreferrer">\1</a>', value)


def render(source: str) -> str:
    result = []
    paragraph = []
    in_list = False
    in_table = False
    def flush() -> None:
        if paragraph:
            result.append("<p>" + inline(" ".join(paragraph)) + "</p>")
            paragraph.clear()
    for line in source.splitlines():
        stripped = line.strip()
        if stripped.startswith("|"):
            flush()
            if in_list:
                result.append("</ul>")
                in_list = False
            cells = [cell.strip() for cell in stripped.strip("|").split("|")]
            if all(cell and all(c in "-: " for c in cell) for cell in cells):
                continue
            if not in_table:
                result.append('<div class="table-wrap"><table><thead><tr>' +
                              ''.join('<th scope="col">' + inline(cell) + '</th>' for cell in cells) +
                              '</tr></thead><tbody>')
                in_table = True
            else:
                result.append('<tr>' + ''.join('<td>' + inline(cell) + '</td>' for cell in cells) + '</tr>')
            continue
        if in_table:
            result.append('</tbody></table></div>')
            in_table = False
        if stripped.startswith("#"):
            flush()
            if in_list:
                result.append("</ul>")
                in_list = False
            level = min(len(stripped) - len(stripped.lstrip("#")), 6)
            result.append(f"<h{level}>" + inline(stripped[level:].strip()) + f"</h{level}>")
        elif stripped.startswith("- "):
            flush()
            if not in_list:
                result.append("<ul>")
                in_list = True
            result.append("<li>" + inline(stripped[2:]) + "</li>")
        elif not stripped:
            flush()
            if in_list:
                result.append("</ul>")
                in_list = False
        else:
            if in_list:
                result.append("</ul>")
                in_list = False
            paragraph.append(stripped)
    flush()
    if in_list:
        result.append("</ul>")
    if in_table:
        result.append('</tbody></table></div>')
    return "\n".join(result)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    source = ROOT / "docs/store/PRIVACY.md"
    text = source.read_text(encoding="utf-8")
    if "[tu correo" in text.lower() or "mailto:" in text.lower():
        raise SystemExit("No se publica un correo sin autorización.")
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    page = """<!doctype html>
<html lang="es"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="robots" content="noindex,nofollow,noarchive">
<meta name="description" content="Información sobre privacidad y tratamiento de datos de Melodia.">
<title>Privacidad · Melodia</title>
<style>
:root{color-scheme:dark light;font:16px/1.7 system-ui,sans-serif;background:#0a0b0a;color:#f2f4f2}
body{max-width:760px;margin:auto;padding:32px 22px 72px}header{display:flex;align-items:center;gap:12px;margin-bottom:32px}
header img{width:48px;height:48px;border-radius:12px}header span{font-size:20px;font-weight:650}
h1{font-size:clamp(26px,5vw,36px);line-height:1.2;letter-spacing:-.7px}h2{font-size:22px;margin-top:30px;line-height:1.3}
h3{font-size:18px;margin-top:24px}p,li{color:#c6cbc7}strong{color:#f2f4f2}li{margin:8px 0}ul{padding-left:22px}
a{color:#70e995;text-underline-offset:3px;overflow-wrap:anywhere}main{overflow-wrap:anywhere}
.table-wrap{max-width:100%;overflow:auto}table{border-collapse:collapse;width:100%;font-size:14px}
th,td{padding:12px;text-align:left;vertical-align:top;border-bottom:1px solid #46554a;min-width:140px}
footer{margin-top:40px;font-size:14px;color:#a0a5a0}
@media(prefers-color-scheme:light){:root{background:#f4f6f4;color:#10140f}p,li,footer{color:#46554a}strong{color:#10140f}a{color:#087a3e}}
</style></head><body><header><img src="icon.png" alt=""><span>Melodia</span></header><main>
""" + render(text) + """
</main><footer>Este documento describe el tratamiento de datos de la aplicación. No implica que la app esté publicada en Google Play.</footer>
</body></html>
"""
    (output / "index.html").write_text(page, encoding="utf-8")
    shutil.copy2(ROOT / "docs/store/icon-512.png", output / "icon.png")
    # Nunca copiar docs/store completo: contiene guiones y decisiones internas.
    print(f"Página generada: {output / 'index.html'}")

if __name__ == "__main__":
    main()
