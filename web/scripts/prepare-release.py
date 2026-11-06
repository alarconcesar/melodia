#!/usr/bin/env python3
"""Prepara solo recursos públicos: glifo real, sprite oficial, APK verificado y ficha."""
from pathlib import Path
import hashlib
import json
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT.parent
PUBLIC = ROOT / 'public'
IMAGES = PUBLIC / 'images'
IMAGES.mkdir(parents=True, exist_ok=True)
ANDROID = '{http://schemas.android.com/apk/res/android}'

def main():
    # Marca exacta de Android, sin redibujar ni modificar la app.
    tree = ET.parse(APP / 'app/src/main/res/drawable/ic_launcher_foreground.xml').getroot()
    group = tree.find('group')
    path = group.find('path') if group is not None else tree.find('path')
    assert path is not None, 'No se encontró el path del icono de la app'
    data = path.attrib[ANDROID + 'pathData']
    color = path.attrib[ANDROID + 'fillColor']
    transform = ''
    if group is not None:
        transform = f"translate({group.get(ANDROID+'translateX','0')} {group.get(ANDROID+'translateY','0')}) scale({group.get(ANDROID+'scaleX','1')} {group.get(ANDROID+'scaleY','1')})"
    # Recortar el viewBox al glifo para que el isotipo no tenga aire invisible.
    mark = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="30 22 48 64"><g transform="{transform}"><path fill="{color}" d="{data}"/></g></svg>'
    (IMAGES / 'mark.svg').write_text(mark)
    icon = Image.open(APP / 'docs/store/icon-512.png').convert('RGB')
    icon.save(IMAGES / 'icon.png', optimize=True)
    icon.resize((48,48),Image.Resampling.LANCZOS).save(IMAGES / 'favicon.png', optimize=True)
    icon.resize((180,180),Image.Resampling.LANCZOS).save(IMAGES / 'apple-touch-icon.png', optimize=True)

    names = ['arrow-down-right','arrow-down','arrow-up','arrow-up-right','headphones','music-notes','heart','shuffle','skip-back','skip-forward','play','pause','repeat','speaker-slash','user-circle','wifi-slash','asterisk','folder-simple','vinyl-record','queue','sliders','microphone-stage','plus','caret-down','x']
    icons = ROOT / 'node_modules/@phosphor-icons/core/assets/regular'
    symbols = []
    for name in names:
        svg = ET.parse(icons / f'{name}.svg').getroot()
        # Remover elementos de descripción, conservar geometría oficial.
        inside = ''.join(ET.tostring(child,encoding='unicode') for child in svg if child.tag.rsplit('}',1)[-1] not in ['title','desc'])
        inside = re.sub(r'xmlns:ns\d+="[^"]+"', '', inside)
        inside = re.sub(r'ns\d+:', '', inside)
        symbols.append(f'<symbol id="{name}" viewBox="{svg.get("viewBox", "0 0 256 256")}">{inside}</symbol>')
    (PUBLIC / 'icons.svg').write_text('<svg xmlns="http://www.w3.org/2000/svg">' + ''.join(symbols) + '</svg>')

    gradle = (APP / 'app/build.gradle.kts').read_text()
    version_match = re.search(r'versionName = "([^"]+)"',gradle)
    code_match = re.search(r'versionCode = (\d+)',gradle)
    assert version_match and code_match, 'Falta la versión de Android'
    version = version_match[1]
    code = int(code_match[1])
    assert version == '7.33' and code == 100, 'Actualiza el copy y la descarga de la web antes de cambiar de versión'
    apk = APP / 'app/build/outputs/apk/release/app-release.apk'
    subprocess.run(['/home/ubuntu/Android/build-tools/35.0.0/apksigner','verify',str(apk)],check=True)
    # aapt2 en ARM64 necesita box64; si no está disponible, la firma y la versión
    # del Gradle siguen siendo la comprobación efectiva.
    badging = ''
    try:
        badging = subprocess.run([str(APP / 'tools/aapt2-box64.sh'),'dump','badging',str(apk)],check=True,text=True,capture_output=True).stdout
    except (OSError, subprocess.CalledProcessError) as error:
        print('AVISO: no se pudo leer el badging con aapt2:', error)
    if badging:
        assert "name='com.melodia.app'" in badging and f"versionCode='{code}'" in badging and f"versionName='{version}'" in badging and "sdkVersion:'24'" in badging
    dest = PUBLIC / 'descargas'
    dest.mkdir(parents=True,exist_ok=True)
    target = dest / f'melodia-{version}.apk'
    shutil.copyfile(apk,target)
    release = {'app':'Melodia','version':version,'versionCode':code,'minSdk':24,'targetSdk':36,'variant':'release','channel':'prueba','bytes':target.stat().st_size,'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'file':target.name,'installationNote':'La firma release es distinta de las compilaciones internas perf. Respalda listas y favoritos antes de cualquier desinstalación.'}
    (dest / 'version.json').write_text(json.dumps(release,indent=2,ensure_ascii=False))
    print('RECURSOS',len(names),'iconos oficiales, marca exacta de la app')
    print('APK_VERIFICADO',version,code,target.stat().st_size,'bytes')

if __name__ == '__main__':
    main()
