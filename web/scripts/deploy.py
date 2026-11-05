#!/usr/bin/env python3
"""Despliega SOLO /melodia/; conserva /melodia/privacidad/ y el auth del hub.

Ejecutar con sudo después del build y QA. Copia únicamente dist/. Cualquier
fallo restaura config y symlink anteriores. El rollback temporal se elimina
al verificar; el script y su bloque nginx son la receta versionada.
"""
from pathlib import Path
import hashlib
import os
import pwd
import shutil
import stat
import subprocess
import tempfile
import time
import urllib.request
import urllib.error

ROOT=Path(__file__).resolve().parents[1]
DIST=ROOT/'dist'
CONFIG=Path('/etc/nginx/sites-available/vps-hub')
ENABLED=Path('/etc/nginx/sites-enabled/vps-hub')
PARENT=Path('/var/www/html/melodia-public')
DEST=PARENT/'landing'
START='    # --- Melodia landing: BEGIN (web/scripts/deploy.py) ---'
END='    # --- Melodia landing: END ---'
BLOCK='''    # --- Melodia landing: BEGIN (web/scripts/deploy.py) ---
    location = /melodia {
        auth_basic off;
        return 308 /melodia/;
    }
    location ^~ /melodia/ {
        auth_basic off;
        alias /var/www/html/melodia-public/landing/;
        index index.html;
        charset utf-8;
        include /etc/nginx/mime.types;
        types { font/woff2 woff2; application/vnd.android.package-archive apk; }
        error_page 404 =404 /melodia/404.html;
        limit_except GET HEAD { deny all; }
        add_header Cache-Control "no-cache, must-revalidate" always;
        add_header X-Robots-Tag "noindex, nofollow, noarchive" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-Frame-Options "DENY" always;
        add_header Referrer-Policy "same-origin" always;
        add_header Content-Security-Policy "default-src 'self'; img-src 'self' data:; media-src 'self'; font-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'none'" always;
        location ~ /\\. { deny all; return 404; }
    }
    # --- Melodia landing: END ---

'''
HOST='vnic-cesar.tailb6ee43.ts.net'

def fetch(path):
    request=urllib.request.Request('http://127.0.0.1'+path,headers={'Host':HOST})
    try:
        response=urllib.request.urlopen(request,timeout=20)
    except urllib.error.HTTPError as response:
        return response.code,dict(response.headers),response.read()
    with response:
        return response.status,dict(response.headers),response.read()

def put_config(data):
    current=CONFIG.stat()
    staging=CONFIG.with_name(CONFIG.name+'.melodia-stage')
    staging.write_bytes(data)
    os.chmod(staging,stat.S_IMODE(current.st_mode))
    os.chown(staging,current.st_uid,current.st_gid)
    os.replace(staging,CONFIG)

def set_link(target):
    link=DEST.with_name('landing.next')
    if link.is_symlink():link.unlink()
    link.symlink_to(target)
    os.replace(link,DEST)

def main():
    assert os.geteuid()==0, 'Usa sudo para escribir solo el vhost y la carpeta pública'
    assert ENABLED.resolve()==CONFIG.resolve(), 'El vhost enabled no es el archivo esperado'
    assert (DIST/'index.html').is_file() and (DIST/'images/og-melodia.jpg').is_file()
    assert (ROOT/'qa/report.json').is_file(), 'Ejecuta QA antes del despliegue'
    import json
    assert not json.loads((ROOT/'qa/report.json').read_text())['failures'], 'El QA contiene fallos'
    files=sorted(p for p in DIST.rglob('*') if p.is_file())
    assert all(not any(part.startswith('.') for part in p.relative_to(DIST).parts) for p in files), 'No se publican dotfiles'
    digest=hashlib.sha256(b''.join(str(p.relative_to(DIST)).encode()+hashlib.sha256(p.read_bytes()).digest() for p in files)).hexdigest()[:12]
    release=PARENT/f'landing-build-{digest}'
    original=CONFIG.read_bytes()
    text=original.decode()
    if START in text:
        a=text.index(START);b=text.index(END,a)+len(END)
        text=text[:a]+BLOCK.rstrip()+text[b:]
    else:
        anchor='    # --- Melodia: privacidad pública'
        assert text.count(anchor)==1, 'Ancla ambigua: no se modifica nginx'
        text=text.replace(anchor,BLOCK+anchor,1)
    status,headers,privacy=fetch('/melodia/privacidad/')
    assert status==200, 'La privacidad pública debe estar sana antes de cambiar nada'
    old_target=DEST.resolve() if DEST.is_symlink() else None
    assert not DEST.exists() or DEST.is_symlink(), 'No se sobrescribe una carpeta ajena'
    if not release.exists():
        shutil.copytree(DIST,release)
        user=pwd.getpwnam('www-data')
        for p in [release,*release.rglob('*')]:
            os.chmod(p,0o755 if p.is_dir() else 0o644)
            os.chown(p,user.pw_uid,user.pw_gid)
    try:
        set_link(release)
        put_config(text.encode())
        check=subprocess.run(['nginx','-t'],capture_output=True,text=True)
        if check.returncode:raise RuntimeError('nginx -t falló: '+check.stderr)
        subprocess.run(['nginx','-s','reload'],check=True)
        time.sleep(2)
        for path,expected in [('/',401),('/melodia/',200),('/melodia/condiciones/',200),('/melodia/privacidad/',200),('/melodia/descargas/version.json',200),('/melodia/keystore.properties',404),('/melodia/.git/config',404),('/melodia/no-existe',404),('/melodia/images/og-melodia.jpg',200)]:
            status,headers,body=fetch(path)
            print('HTTP',path,status)
            assert status==expected,(path,status,expected)
            if path=='/melodia/':assert hashlib.sha256(body).digest()==hashlib.sha256((DIST/'index.html').read_bytes()).digest()
            if path=='/melodia/privacidad/':assert body==privacy,'La política cambió indebidamente'
            if path=='/melodia/no-existe':assert b'Esta pista' in body
        print('NGINX_VALIDO; PRIVACIDAD_IDENTICA; AUTH_DEL_HUB_INTACTO')
    except Exception:
        put_config(original)
        if old_target is not None:set_link(old_target)
        elif DEST.is_symlink():DEST.unlink()
        subprocess.run(['nginx','-t'],check=True)
        subprocess.run(['nginx','-s','reload'],check=True)
        raise
    # Los builds reproducibles anteriores no se acumulan en el VPS.
    for old in PARENT.glob('landing-build-*'):
        if old!=release and old.is_dir():shutil.rmtree(old)
    print('DEPLOY',digest,len(files),'archivos',DEST)

if __name__=='__main__':main()
