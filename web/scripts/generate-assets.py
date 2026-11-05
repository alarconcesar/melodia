#!/usr/bin/env python3
"""Genera dos recursos originales sin texto ni interfaces; conserva procedencia."""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import json
import subprocess
import shutil
from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'public' / 'images'
OUT.mkdir(parents=True, exist_ok=True)
JOBS = [
    ('caratula', '1:1', 'Original fine art album cover photography, no text, no lettering, no logos, no interface. A solitary sharp volcanic sea stack emerging from dark calm Pacific ocean, the entire rock and its reflection in frame, rich cold forest green monochrome atmosphere with a small warm ivory full moon, subtle grain like a 35mm analog photograph, editorial music art direction, very dark nearly black corners, cinematic high contrast, minimal poetic composition with lots of negative space, premium indie album artwork, square composition.'),
    ('escucha', '16:9', 'Premium editorial music lifestyle photograph for an independent music player brand, landscape horizontal composition. Candid side profile and upper torso of a young adult Latina woman wearing elegant black over ear headphones and a dark charcoal jacket, eyes closed, feeling music, standing at a concrete rooftop overlooking a quiet Lima urban skyline and the Pacific coast at blue hour. Subject on the right third, huge soft negative space on left with pale green-grey overcast sky. Direct flash mixed with natural twilight, 35mm film grain, rich graphite blacks, cool desaturated green grey palette with natural skin color, sophisticated fashion magazine campaign, authentic not stock photo, no text, no logos, no phone screen, no UI.')
]

def run(job):
    name, aspect, prompt = job
    proc = subprocess.run(['/home/ubuntu/.hermes/hermes-agent/venv/bin/python', '/home/ubuntu/emma/bin/img_codex.py', prompt, '--aspect', aspect, '--model', 'gpt-image-2-medium', '--json'], text=True, capture_output=True, timeout=260)
    log = {'name': name, 'aspect': aspect, 'prompt': prompt, 'exit_code': proc.returncode}
    try:
        result = json.loads(proc.stdout)
        log['result'] = result
        if proc.returncode != 0 or not result.get('success'):
            raise RuntimeError(str(result.get('error', 'generation failed')))
        source = result.get('image', '')
        if source.startswith('file://'):
            source = source[7:]
        if not Path(source).is_file():
            raise RuntimeError(f'No existe la imagen reportada: {source}')
        im = Image.open(source).convert('RGB')
        if name == 'caratula':
            im = ImageOps.fit(im, (960,960), method=Image.Resampling.LANCZOS)
            im.save(OUT / f'{name}.webp', 'WEBP', quality=86, method=6)
            im.resize((480,480), Image.Resampling.LANCZOS).save(OUT / f'{name}-480.webp', 'WEBP', quality=83, method=6)
        else:
            im = ImageOps.fit(im, (1600,900), method=Image.Resampling.LANCZOS)
            im.save(OUT / f'{name}.webp', 'WEBP', quality=84, method=6)
            im.resize((800,450), Image.Resampling.LANCZOS).save(OUT / f'{name}-800.webp', 'WEBP', quality=82, method=6)
        log['output'] = str(OUT / f'{name}.webp')
        log['size'] = im.size
        print(name, log['output'], flush=True)
    except Exception as error:
        log['error'] = str(error)
        log['stderr'] = proc.stderr[-2000:]
        print(name, 'ERROR', error, flush=True)
    return log

if __name__ == '__main__':
    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(run, JOBS))
    (ROOT / 'assets-provenance.json').write_text(json.dumps(results, ensure_ascii=False, indent=2))
    if any(r.get('error') for r in results):
        raise SystemExit(1)
