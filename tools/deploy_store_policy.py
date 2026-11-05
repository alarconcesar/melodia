#!/usr/bin/env python3
"""Despliega SOLO la política pública en el nginx existente; ejecutar con sudo."""
from pathlib import Path
import hashlib
import os
import pwd
import shutil
import stat
import subprocess
import time
import requests

ROOT = Path(__file__).resolve().parents[1]
CONFIG = Path('/etc/nginx/sites-available/vps-hub')
BACKUP = Path('/home/ubuntu/.hermes/profiles/emma/cache/scratch/vps-hub-pre-melodia-20261105.conf')
DEST = Path('/var/www/html/melodia-public/privacidad')
BLOCK = '''    # --- Melodia: privacidad pública (solo HTML e icono; no APK ni documentos internos) ---
    location = /melodia/privacidad {
        auth_basic off;
        return 308 /melodia/privacidad/;
    }
    location ^~ /melodia/privacidad/ {
        auth_basic off;
        alias /var/www/html/melodia-public/privacidad/;
        index index.html;
        charset utf-8;
        limit_except GET HEAD { deny all; }
        add_header X-Robots-Tag "noindex, nofollow, noarchive" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header Referrer-Policy "no-referrer" always;
        add_header Content-Security-Policy "default-src 'none'; img-src 'self'; style-src 'unsafe-inline'; base-uri 'none'; frame-ancestors 'none'; form-action 'none'" always;
    }

'''

def main() -> None:
    if os.geteuid() != 0:
        raise SystemExit('Se requiere sudo para modificar únicamente este vhost y el directorio público.')
    source = ROOT / 'app/build/store-site'
    assert (source / 'index.html').is_file(), 'Genera primero la página con tools/store_site.py'
    text = CONFIG.read_text()
    if not BACKUP.exists():
        BACKUP.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(CONFIG, BACKUP)
        os.chmod(BACKUP, 0o600)
        user = pwd.getpwnam('ubuntu')
        os.chown(BACKUP, user.pw_uid, user.pw_gid)
    DEST.mkdir(parents=True, exist_ok=True)
    os.chmod(DEST, 0o755)
    for name in ['index.html', 'icon.png']:
        shutil.copyfile(source / name, DEST / name)
        os.chmod(DEST / name, 0o644)
    if '# --- Melodia: privacidad pública' not in text:
        anchor = '    # --- Previews de sitios: públicos, sin auth del panel ---'
        assert text.count(anchor) == 1, 'Anchor ambiguo: no se modifica nginx'
        previous = CONFIG.stat()
        staging = CONFIG.with_name(CONFIG.name + '.melodia-staging')
        staging.write_text(text.replace(anchor, BLOCK + anchor, 1))
        os.chmod(staging, stat.S_IMODE(previous.st_mode))
        os.chown(staging, previous.st_uid, previous.st_gid)
        os.replace(staging, CONFIG)
    check = subprocess.run(['nginx', '-t'], capture_output=True, text=True)
    if check.returncode:
        shutil.copyfile(BACKUP, CONFIG)
        raise SystemExit('nginx -t falló; se restauró la configuración anterior. No hubo reload.')
    subprocess.run(['nginx', '-s', 'reload'], check=True)
    time.sleep(2)
    headers = {'Host': 'vnic-cesar.tailb6ee43.ts.net'}
    for path, expected in [('/', 401), ('/melodia/privacidad/', 200),
                           ('/melodia/privacidad/icon.png', 200),
                           ('/melodia/privacidad/keystore.properties', 404)]:
        response = requests.get('http://127.0.0.1' + path, headers=headers, timeout=15)
        print('LOCAL_HTTP', path, response.status_code)
        assert response.status_code == expected, (path, response.status_code)
        if path == '/melodia/privacidad/':
            assert 'WWW-Authenticate' not in response.headers
            assert hashlib.sha256(response.content).hexdigest() == hashlib.sha256((source / 'index.html').read_bytes()).hexdigest()
    print('LOCAL_POLICY_DEPLOY_VERIFIED; el auth del hub se mantiene')

if __name__ == '__main__':
    main()
