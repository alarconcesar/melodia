#!/usr/bin/env python3
"""Compone tres bucles instrumentales originales para la demo, sin muestras ajenas."""
from pathlib import Path
import json
import subprocess
import tempfile
import wave
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'public/audio'
OUT.mkdir(parents=True,exist_ok=True)
SR=24000
DURATION=32
N=SR*DURATION

def make(name,roots,bpm,seed):
    rng=np.random.default_rng(seed)
    left=np.zeros(N,dtype=np.float64)
    right=np.zeros(N,dtype=np.float64)
    beat=60/bpm
    def note(at,midi,length,gain,pan: float=0.0):
        length=min(length,DURATION-at)
        if length<=0:return
        t=np.arange(int(length*SR))/SR
        freq=440*2**((midi-69)/12)
        envelope=(1-np.exp(-t*45))*np.exp(-t*1.8)*np.minimum(1,(length-t)*8)
        tone=(np.sin(2*np.pi*freq*t)+.2*np.sin(2*np.pi*freq*2*t)+.07*np.sin(2*np.pi*freq*3*t))*envelope*gain
        i=int(at*SR)
        length=min(len(tone),N-i)
        if length<=0:return
        left[i:i+length]+=tone[:length]*(1-pan*.35)
        right[i:i+length]+=tone[:length]*(1+pan*.35)
        delay=int(.31*SR)
        if i+delay+length<N:
            left[i+delay:i+delay+length]+=tone[:length]*.12
            right[i+delay:i+delay+length]+=tone[:length]*.18
    step=0
    at=0
    while at<DURATION:
        root=roots[(step//8)%len(roots)]
        chord=[root+24,root+27,root+31,root+34]
        melodic=[0,2,1,3,2,1,3,2][step%8]
        note(at,chord[melodic],2.0,.095,(-1 if step%2 else 1)*.5)
        if step%4==0:
            note(at,root,beat*1.8,.13)
            for value in [root+12,root+15,root+19]:note(at,value,beat*3.2,.022)
        if step%2==0:
            t=np.arange(int(.32*SR))/SR
            kick=np.sin(2*np.pi*(48*t+38*(1-np.exp(-t*30))/30))*np.exp(-t*20)*.11
            i=int(at*SR);size=min(len(kick),N-i)
            left[i:i+size]+=kick[:size];right[i:i+size]+=kick[:size]
        if step%4==2:
            t=np.arange(int(.12*SR))/SR
            noise=rng.standard_normal(len(t))
            snare=np.diff(noise,prepend=0)*np.exp(-t*38)*.014
            i=int(at*SR);size=min(len(snare),N-i)
            left[i:i+size]+=snare[:size];right[i:i+size]+=snare[:size]
        t=np.arange(int(.045*SR))/SR
        noise=rng.standard_normal(len(t))
        hat=np.diff(noise,prepend=0)*np.exp(-t*100)*.005
        i=int(at*SR);size=min(len(hat),N-i)
        left[i:i+size]+=hat[:size];right[i:i+size]+=hat[:size]
        step+=1;at+=beat/2
    fade=np.ones(N)
    fin=int(.3*SR);fout=int(1.4*SR)
    fade[:fin]=np.linspace(0,1,fin)
    fade[-fout:]=np.linspace(1,0,fout)
    data=np.stack([left*fade,right*fade],axis=1)
    peak=np.abs(data).max()
    data=np.tanh(data/peak*.65)*.7
    wav=Path(tempfile.mkdtemp(prefix='melodia-audio-'))/f'{name}.wav'
    with wave.open(str(wav),'wb') as w:
        w.setnchannels(2);w.setsampwidth(2);w.setframerate(SR)
        w.writeframes((data*32767).astype('<i2').tobytes())
    target=OUT/f'{name}.ogg'
    subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','3','-metadata','title='+name,'-metadata','artist=Sesiones Melodia','-metadata','comment=Composición instrumental original para la demostración web de Melodia',str(target)],check=True)
    probe=json.loads(subprocess.run(['ffprobe','-v','error','-show_entries','format=duration:stream=codec_name,sample_rate,channels','-of','json',str(target)],check=True,text=True,capture_output=True).stdout)
    wav.unlink()
    print(name,probe['format']['duration'],target.stat().st_size,'bytes')
    assert abs(float(probe['format']['duration'])-DURATION)<.1
    return {'file':target.name,'duration':DURATION,'bytes':target.stat().st_size,'bpm':bpm,'composition':'Original, síntesis numérica, sin muestras de terceros'}

if __name__=='__main__':
    result=[make('costa-adentro',[45,41,48,43],88,33),make('fuera-de-hora',[42,38,45,40],80,72),make('lado-b',[47,43,50,45],96,100)]
    (OUT/'credits.json').write_text(json.dumps(result,indent=2,ensure_ascii=False))
