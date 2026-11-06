#!/usr/bin/env python3
"""Tarjeta social de marca desde los mismos assets, sin capturas ficticias."""
from pathlib import Path
from playwright.sync_api import sync_playwright
ROOT=Path(__file__).resolve().parents[1]
CHROME=sorted(Path.home().glob('.cache/ms-playwright/chromium_headless_shell-*/chrome-linux/headless_shell'))[-1]
BASE='http://127.0.0.1:8942/melodia/'
HTML='''<!doctype html><html lang="es"><meta charset="utf-8"><style>
@font-face{font-family:Manrope;src:url(BASEfonts/manrope-latin-variable.woff2);font-weight:200 800}*{box-sizing:border-box}html,body{margin:0;width:1200px;height:630px;background:#000;color:#f4f5f3;font-family:Manrope,sans-serif}body{display:flex;align-items:center;justify-content:space-between;padding:64px 80px}.brand{position:absolute;left:80px;top:48px;display:flex;gap:8px;align-items:center;font-size:30px;font-weight:700;letter-spacing:-2px}.brand img{width:32px;height:40px}.copy{width:650px;padding-top:64px;z-index:2}h1{font-size:82px;line-height:1;letter-spacing:-5px;font-weight:600;margin:0}h1 span{color:#2ee56b}p{font-size:20px;color:#b9bcb5;line-height:28px;margin:32px 0;max-width:480px}.cta{display:inline-flex;align-items:center;gap:32px;background:#2ee56b;color:#000;padding:16px 24px;border-radius:100px;font-size:16px;font-weight:700}.art{width:370px;height:420px;position:relative;transform:rotate(6deg);margin-top:32px}.record{position:absolute;width:340px;height:340px;border-radius:50%;background:#181818;border:1px solid #444;left:64px;top:48px;box-shadow:inset 0 0 0 24px #181818,inset 0 0 0 25px #313131,inset 0 0 0 32px #181818,inset 0 0 0 33px #313131}.cover{position:absolute;width:340px;height:340px;left:-48px;top:0;border-radius:16px;overflow:hidden;transform:rotate(-12deg)}.cover img{width:100%;height:100%;object-fit:cover}.cover span{position:absolute;left:24px;bottom:24px;font-size:24px;font-weight:600;line-height:28px;letter-spacing:-1px}.foot{position:absolute;bottom:40px;left:80px;color:#a9aaa7;font-size:14px}.dot{color:#2ee56b}
</style><div class="brand"><img src="BASEimages/mark.svg"><span>melodia<span class="dot">.</span></span></div><div class="copy"><h1>Descarga música<br><span>gratis.</span></h1><p>Guárdala en tu Android.<br>Escúchala sin conexión.</p><div class="cta">Conoce Melodia <span>↗</span></div></div><div class="art"><div class="record"></div><div class="cover"><img src="BASEimages/caratula.webp"><span>Sesiones<br>Melodia</span></div></div><div class="foot">Gratis y de código abierto · Para contenido propio o autorizado.</div></html>'''.replace('BASE',BASE)
with sync_playwright() as p:
 b=p.chromium.launch(executable_path=str(CHROME),args=['--no-sandbox'])
 page=b.new_page(viewport={'width':1200,'height':630},device_scale_factor=1)
 page.set_content(HTML,wait_until='networkidle')
 page.evaluate('document.fonts.ready')
 assert page.evaluate('[...document.images].every(i=>i.complete&&i.naturalWidth>0)')
 page.screenshot(path=str(ROOT/'public/images/og-melodia.jpg'),type='jpeg',quality=88)
 b.close()
print('OG',ROOT/'public/images/og-melodia.jpg')
