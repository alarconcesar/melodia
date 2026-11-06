#!/usr/bin/env python3
"""QA real con Chromium: layout, recursos, audio, teclado, reduced motion y móvil."""
from pathlib import Path
import hashlib
import json
import os
import sys
from playwright.sync_api import sync_playwright

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'qa'
OUT.mkdir(exist_ok=True)
URL=os.environ.get('MELODIA_QA_URL','http://127.0.0.1:8942/melodia/')
CHROME=sorted(Path.home().glob('.cache/ms-playwright/chromium_headless_shell-*/chrome-linux/headless_shell'))[-1]
RESULT={'url':URL,'viewports':[],'checks':[],'failures':[]}

def record(name,value,detail=None):
    RESULT['checks'].append({'name':name,'passed':bool(value),'detail':detail})
    if not value:RESULT['failures'].append(name)

def walk(pg):
    pg.evaluate("document.documentElement.style.scrollBehavior='auto'")
    height=pg.evaluate('document.documentElement.scrollHeight')
    for y in range(0,height,600):
        pg.evaluate('(y)=>window.scrollTo(0,y)',y)
        pg.wait_for_timeout(100)
    pg.wait_for_timeout(1500)

with sync_playwright() as pw:
    browser=pw.chromium.launch(executable_path=str(CHROME),args=['--no-sandbox','--disable-dev-shm-usage','--autoplay-policy=user-gesture-required'])
    for width in [320,360,390,430,768,1024,1440,1920]:
        errors=[];bad=[]
        context=browser.new_context(viewport={'width':width,'height':900 if width>=768 else 844},is_mobile=width<768,has_touch=width<768,device_scale_factor=1)
        page=context.new_page()
        page.on('pageerror',lambda error:errors.append(str(error)))
        page.on('response',lambda response:bad.append({'url':response.url,'status':response.status}) if response.status>=400 else None)
        response=page.goto(URL,wait_until='networkidle')
        page.evaluate('document.fonts.ready')
        page.wait_for_timeout(1300)
        metrics=page.evaluate("""()=>{const h=document.querySelector('h1'),cta=document.querySelector('.hero-cta'),stage=document.querySelector('.hero-visual');return {width:innerWidth,height:innerHeight,overflow:document.documentElement.scrollWidth-innerWidth,fontReady:document.fonts.check('600 48px "Manrope Variable"','Tu música. A tu manera.'),fonts:[...document.fonts].filter(f=>f.status==='loaded').map(f=>f.family),heroTitle:{size:getComputedStyle(h).fontSize,height:h.getBoundingClientRect().height},ctaBottom:cta.getBoundingClientRect().bottom,visualTop:stage.getBoundingClientRect().top,pageHeight:document.documentElement.scrollHeight};}""")
        record(f'{width}:http',response is not None and response.status==200)
        record(f'{width}:viewport-real',metrics['width']==width,metrics['width'])
        record(f'{width}:overflow',metrics['overflow']==0,metrics['overflow'])
        record(f'{width}:font-loaded',metrics['fontReady'] and bool(metrics['fonts']),metrics['fonts'])
        record(f'{width}:hero-cta-in-fold',metrics['ctaBottom']<metrics['height'],metrics['ctaBottom'])
        if width in [390,1440]:
            page.screenshot(path=str(OUT/f'hero-{width}.png'))
        walk(page)
        images=page.evaluate("[...document.images].map(i=>({src:i.getAttribute('src'),ok:i.complete&&i.naturalWidth>0}))")
        record(f'{width}:images',all(i['ok'] for i in images),images)
        invisible=page.evaluate("[...document.querySelectorAll('.reveal')].filter(e=>getComputedStyle(e).opacity<.99).length")
        record(f'{width}:reveals',invisible==0,invisible)
        record(f'{width}:page-errors',not errors,errors)
        record(f'{width}:http-assets',not bad,bad)
        metrics['errors']=errors;metrics['badAssets']=bad
        RESULT['viewports'].append(metrics)
        if width in [390,1440]:
            for sel,name in [('#tu-sonido','sonido'),('.listening-section','editorial'),('#descargar','descarga')]:
                page.locator(sel).scroll_into_view_if_needed()
                page.wait_for_timeout(1000)
                page.screenshot(path=str(OUT/f'{name}-{width}.png'))
        if width==390:
            page.evaluate('scrollTo(0,0)')
            page.locator('.menu-trigger').click()
            page.wait_for_timeout(500)
            record('mobile:menu-open',page.locator('#mobile-menu').evaluate('(e)=>e.open'))
            record('mobile:menu-expanded',page.locator('.menu-trigger').get_attribute('aria-expanded')=='true')
            page.screenshot(path=str(OUT/'menu-390.png'))
            page.keyboard.press('Escape')
            record('mobile:menu-escape',not page.locator('#mobile-menu').evaluate('(e)=>e.open'))
            record('mobile:menu-focus-restored',page.locator('.menu-trigger').evaluate('(e)=>document.activeElement===e'))
            page.locator('.menu-trigger').click()
            page.locator('#mobile-menu a[href="#tu-sonido"]').click()
            record('mobile:menu-anchor-close',not page.locator('#mobile-menu').evaluate('(e)=>e.open'))
        if width==1440:
            page.locator('#demo-play').click()
            page.wait_for_timeout(900)
            state=page.locator('#demo-audio').evaluate('(e)=>({paused:e.paused,time:e.currentTime,duration:e.duration,error:e.error?.message})')
            record('demo:real-playback',not state['paused'] and state['time']>.1,state)
            page.locator('#demo-play').click()
            record('demo:pause',page.locator('#demo-audio').evaluate('(e)=>e.paused'))
            page.locator('.demo-like').click()
            record('demo:favorite',page.locator('.demo-like').get_attribute('aria-pressed')=='true')
            page.locator('#demo-next').click()
            record('demo:next-title',page.locator('#demo-title').inner_text()=='Fuera de hora')
            page.locator('#demo-play').click()
            page.wait_for_timeout(600)
            record('demo:second-audio',page.locator('#demo-audio').evaluate('(e)=>!e.paused&&e.currentTime>0'))
            page.locator('#demo-play').click()
            page.locator('[data-preset="graves"]').click()
            vals=page.locator('.eq-sliders input').evaluate_all('(es)=>es.map(e=>Number(e.value))')
            record('demo:eq-graves',vals==[8,5,0,-1,0],vals)
            page.locator('.eq-sliders input').first.fill('5')
            record('demo:eq-manual-clears-presets',page.locator('[data-preset][aria-pressed="true"]').count()==0)
            page.locator('[data-preset="plano"]').click()
            record('demo:eq-flat',page.locator('.eq-sliders input').evaluate_all('(es)=>es.every(e=>Number(e.value)===0)'))
            page.locator('.faq-list summary').first.click()
            record('faq:opens',page.locator('.faq-list details').first.get_attribute('open') is not None)
            page.locator('.faq-list summary').first.click()
            record('faq:closes',page.locator('.faq-list details').first.get_attribute('open') is None)
            with page.expect_download() as download:
                page.locator('.download-link').click()
            item=download.value
            data=Path(item.path()).read_bytes()
            manifest=page.request.get(URL+'descargas/version.json').json()
            record('download:sha256',hashlib.sha256(data).hexdigest()==manifest['sha256'])
            record('download:size',len(data)==manifest['bytes'],len(data))
            # El nombre lo decide el servidor cuando el APK es cross-origin (Releases).
            record('download:name',item.suggested_filename.endswith('.apk'),item.suggested_filename)
        context.close()
    # Movimiento reducido, sin JavaScript y tamaño de texto aumentado.
    for mode in ['reduced','no-js','large-text']:
        context=browser.new_context(viewport={'width':390,'height':844},is_mobile=True,has_touch=True,reduced_motion='reduce' if mode=='reduced' else 'no-preference',java_script_enabled=mode!='no-js')
        page=context.new_page();page.goto(URL,wait_until='networkidle')
        if mode=='large-text': page.add_style_tag(content='html{font-size:20px}')
        page.wait_for_timeout(1300)
        record(f'{mode}:overflow',page.evaluate('document.documentElement.scrollWidth<=innerWidth'))
        record(f'{mode}:headline-visible',page.locator('h1').evaluate('(e)=>Number(getComputedStyle(e).opacity)>0.99'))
        href=page.locator('.download-link').get_attribute('href') or ''
        record(f'{mode}:download-link',href.endswith('melodia-7.33.apk'),href)
        if mode=='reduced': record('reduced:frame-transform',page.locator('.demo-frame').evaluate('(e)=>getComputedStyle(e).transform')=='none')
        if mode=='no-js':record('no-js:demo-honest',page.locator('.no-js-demo').is_visible())
        context.close()
    browser.close()

(OUT/'report.json').write_text(json.dumps(RESULT,ensure_ascii=False,indent=2))
print(json.dumps({'checks':len(RESULT['checks']),'failures':RESULT['failures'],'viewports':RESULT['viewports'],'report':str(OUT/'report.json')},ensure_ascii=False,indent=2))
sys.exit(1 if RESULT['failures'] else 0)
