#!/usr/bin/env python3
"""Escribe robots.txt y sitemap.xml para el origen que se esté publicando.

El mismo build sirve al preview del VPS (noindex por cabecera) y a GitHub Pages.
Sin argumento usa el origen del preview; el workflow de Pages pasa SITE_ORIGIN.
"""
from __future__ import annotations

import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PUBLIC = ROOT / "public"
ORIGIN = os.environ.get("SITE_ORIGIN", "https://vnic-cesar.tailb6ee43.ts.net").rstrip("/")

robots = f"""User-agent: *
Allow: /melodia/

Sitemap: {ORIGIN}/melodia/sitemap.xml
"""

sitemap = f"""<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url><loc>{ORIGIN}/melodia/</loc></url>
  <url><loc>{ORIGIN}/melodia/condiciones/</loc></url>
  <url><loc>{ORIGIN}/melodia/privacidad/</loc></url>
</urlset>
"""

(PUBLIC / "robots.txt").write_text(robots, encoding="utf-8")
(PUBLIC / "sitemap.xml").write_text(sitemap, encoding="utf-8")
print("SITE_ORIGIN", ORIGIN)
