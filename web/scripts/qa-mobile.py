#!/usr/bin/env python3
"""Auditoría móvil reproducible: tamaños reales, taps, tipografía y descarga.

No sustituye pruebas físicas en Android/iOS. MELODIA_QA_URL admite Pages o local.
"""
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import sys
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
OUT = Path(os.environ.get('MELODIA_QA_OUTPUT', str(ROOT / 'qa')))
OUT.mkdir(parents=True, exist_ok=True)
URL = os.environ.get('MELODIA_QA_URL', 'http://127.0.0.1:8951/melodia/')
CHROME = sorted(Path.home().glob('.cache/ms-playwright/chromium_headless_shell-*/chrome-linux/headless_shell'))[-1]
VIEWPORTS = [(320, 568), (360, 640), (375, 667), (390, 844), (414, 896), (430, 932),
             (480, 800), (600, 960), (667, 375), (844, 390), (768, 1024),
             (1024, 768), (1440, 900), (1920, 1080)]
REPORT = {'url': URL, 'at': datetime.now(timezone.utc).isoformat(), 'viewports': [], 'checks': [], 'failures': []}

def record(name, passed, detail=None):
    REPORT['checks'].append({'name': name, 'passed': bool(passed), 'detail': detail})
    if not passed:
        REPORT['failures'].append(name)


def walk(page, height):
    page.evaluate("document.documentElement.style.scrollBehavior='auto'")
    total = page.evaluate('document.documentElement.scrollHeight')
    for y in range(0, total, max(220, int(height * .65))):
        page.evaluate('(y)=>scrollTo(0,y)', y)
        page.wait_for_timeout(70)
    page.wait_for_timeout(900)


try:
    with sync_playwright() as pw:
        browser = pw.chromium.launch(executable_path=str(CHROME), args=['--no-sandbox', '--disable-dev-shm-usage'])
        for width, height in VIEWPORTS:
            key = f'{width}x{height}'
            errors = []
            context = browser.new_context(viewport={'width': width, 'height': height},
                is_mobile=width < 1024, has_touch=width < 1024, device_scale_factor=1)
            page = context.new_page()
            page.on('pageerror', lambda error: errors.append(str(error)))
            try:
                response = page.goto(URL, wait_until='networkidle')
                page.evaluate('document.fonts.ready')
                page.wait_for_timeout(1300)
                m = page.evaluate('''()=>{
                    const h=document.querySelector('h1'), cta=document.querySelector('.hero-cta');
                    const style=getComputedStyle(h), r=h.getBoundingClientRect();
                    return {width:innerWidth,clientWidth:document.documentElement.clientWidth,height:innerHeight,overflow:document.documentElement.scrollWidth-innerWidth,
                      heading:h.innerText,headingLines:r.height/parseFloat(style.lineHeight),
                      headingSize:style.fontSize,ctaBottom:cta.getBoundingClientRect().bottom,
                      manrope:document.fonts.check('600 48px "Manrope Variable"','Descarga música gratis.'),
                      loaded:[...document.fonts].filter(f=>f.status==='loaded').map(f=>f.family)};
                }''')
                record(f'{key}:http', response is not None and response.status == 200)
                record(f'{key}:real-viewport', m['width'] == width and m['clientWidth'] == width and m['height'] == height, m)
                record(f'{key}:free-download-headline', 'Descarga música' in m['heading'] and 'gratis.' in m['heading'])
                record(f'{key}:two-line-headline', m['headingLines'] <= 2.1, m['headingLines'])
                record(f'{key}:font-loaded', m['manrope'] and any('Manrope' in f for f in m['loaded']))
                if width < 768 and height > width:
                    record(f'{key}:cta-above-fold', m['ctaBottom'] < height - 8, m['ctaBottom'])
                if width in (320, 390, 1440):
                    page.screenshot(path=str(OUT / f'mobile-audit-hero-{width}.png'))
                walk(page, height)
                record(f'{key}:overflow', page.evaluate('document.documentElement.scrollWidth===innerWidth'))
                text_overflow = page.evaluate('''()=>[...document.querySelectorAll('.hero-copy h1,.hero-description,.hero-terms,.offline-panel h2,.offline-panel h3,.offline-panel p')].flatMap(e=>{
                    const walker=document.createTreeWalker(e,NodeFilter.SHOW_TEXT);let n,bad=[];
                    while(n=walker.nextNode()){if(!n.textContent.trim())continue;const r=document.createRange();r.selectNodeContents(n);
                      for(const x of r.getClientRects())if(x.left<-.5||x.right>innerWidth+.5)bad.push(n.textContent.trim());}
                    return bad;
                })''')
                record(f'{key}:text-not-clipped', not text_overflow, text_overflow)
                tiny = page.evaluate('''()=>[...document.querySelectorAll('a,button,summary,input[type=range]')].filter(e=>{
                    if(e.closest('dialog:not([open])')||e.classList.contains('skip-link'))return false;
                    const r=e.getBoundingClientRect(),s=getComputedStyle(e);
                    return r.width>0&&r.height>0&&s.visibility!=='hidden'&&(r.width<43.5||r.height<43.5);
                }).map(e=>({text:e.getAttribute('aria-label')||e.innerText||e.id,
                     width:e.getBoundingClientRect().width,height:e.getBoundingClientRect().height}))''')
                record(f'{key}:touch-targets-44px', not tiny, tiny)
                record(f'{key}:three-download-steps', page.locator('.offline-flow li').count() == 3)
                record(f'{key}:images-loaded', page.evaluate('[...document.images].every(i=>i.complete&&i.naturalWidth>0)'))
                for i in range(page.locator('.faq-list details').count()):
                    d = page.locator('.faq-list details').nth(i)
                    d.locator('summary').click()
                    record(f'{key}:faq-{i+1}-open', d.evaluate('(e)=>e.open'))
                    d.locator('summary').click()
                if width < 768:
                    page.evaluate('scrollTo(0,0)')
                    page.locator('.menu-trigger').tap()
                    page.wait_for_timeout(400)
                    record(f'{key}:menu-open', page.locator('#mobile-menu').evaluate('(e)=>e.open'))
                    menu_metrics = page.locator('#mobile-menu').evaluate('(e)=>({top:e.getBoundingClientRect().top,bottom:e.getBoundingClientRect().bottom,client:e.clientHeight,scroll:e.scrollHeight})')
                    record(f'{key}:menu-fits-height', menu_metrics['top'] >= 0 and menu_metrics['bottom'] <= height + 1, menu_metrics)
                    page.keyboard.press('Escape')
                    record(f'{key}:menu-escape', not page.locator('#mobile-menu').evaluate('(e)=>e.open'))
                    record(f'{key}:menu-focus', page.locator('.menu-trigger').evaluate('(e)=>document.activeElement===e'))
                    page.locator('.menu-trigger').tap()
                    page.locator('#mobile-menu a[href="#descargas-gratis"]').tap()
                    page.wait_for_timeout(250)
                    record(f'{key}:menu-navigates', page.evaluate("location.hash==='#descargas-gratis'") and not page.locator('#mobile-menu').evaluate('(e)=>e.open'))
                    box = page.locator('#descargas-gratis').bounding_box()
                    assert box is not None, 'La sección de descargas debe ser visible'
                    top = box['y']
                    record(f'{key}:section-not-under-header', top >= 88, top)
                if width == 390:
                    page.locator('#descargas-gratis').scroll_into_view_if_needed()
                    page.wait_for_timeout(700)
                    page.locator('#descargas-gratis').screenshot(path=str(OUT / 'mobile-audit-downloads-390.png'))
                    page.locator('#demo-play').tap()
                    page.wait_for_timeout(800)
                    record('touch:audio-plays', page.locator('#demo-audio').evaluate('(e)=>!e.paused&&e.currentTime>0'))
                    page.locator('#demo-play').tap()
                    page.locator('.demo-repeat').tap()
                    record('touch:repeat', page.locator('.demo-repeat').get_attribute('aria-pressed') == 'true')
                    page.locator('.demo-shuffle').tap()
                    record('touch:shuffle', page.locator('.demo-shuffle').get_attribute('aria-pressed') == 'true')
                    with page.expect_download() as dl:
                        page.locator('.download-link').tap()
                    download = dl.value
                    data = Path(download.path()).read_bytes()
                    expected = page.request.get(URL + 'descargas/version.json').json()
                    record('touch:apk-sha256', hashlib.sha256(data).hexdigest() == expected['sha256'])
                    record('touch:apk-size', len(data) == expected['bytes'], len(data))
                    page.evaluate('scrollTo(0,0)')
                    page.locator('.menu-trigger').tap()
                    page.set_viewport_size({'width': 1024, 'height': 768})
                    page.wait_for_timeout(250)
                    record('mobile:resize-closes-menu', not page.locator('#mobile-menu').evaluate('(e)=>e.open'))
                    record('mobile:resize-unlocks-scroll', page.evaluate("document.body.style.overflow!== 'hidden'"))
                record(f'{key}:no-js-errors', not errors, errors)
                REPORT['viewports'].append(m)
            except Exception as error:
                record(f'{key}:exception', False, str(error))
            finally:
                context.close()
        browser.close()
finally:
    path = OUT / 'mobile-report.json'
    path.write_text(json.dumps(REPORT, ensure_ascii=False, indent=2))
    print(json.dumps({'url':URL,'viewports':len(REPORT['viewports']),'checks':len(REPORT['checks']),
                      'failures':REPORT['failures'],'report':str(path)}, ensure_ascii=False))
sys.exit(bool(REPORT['failures']) or len(REPORT['viewports']) != len(VIEWPORTS))
