import './styles.css';

const root = document.documentElement;
root.classList.add('js');
const reduce = window.matchMedia('(prefers-reduced-motion: reduce)');
const $ = (selector, parent = document) => parent.querySelector(selector);
const $$ = (selector, parent = document) => [...parent.querySelectorAll(selector)];
const iconHref = (name) => `${import.meta.env.BASE_URL}icons.svg#${name}`;
const base = import.meta.env.BASE_URL;

// Las entradas son mejora progresiva: si falla JS, la página y la descarga se ven.
const reveals = $$('.reveal');
if (!reduce.matches && 'IntersectionObserver' in window) {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      entry.target.classList.add('is-visible');
      observer.unobserve(entry.target);
    });
  }, { threshold: 0.12, rootMargin: '0px 0px -24px 0px' });
  reveals.forEach((node) => { node.classList.add('motion-ready'); observer.observe(node); });
  // Ningún fallo de observación puede dejar contenido permanentemente oculto.
  window.setTimeout(() => reveals.forEach((node) => node.classList.add('is-visible')), 12000);
}

const manifesto = $('.word-reveal');
if (manifesto && !reduce.matches && 'IntersectionObserver' in window) {
  let order = 0;
  [...manifesto.childNodes].forEach((node) => {
    if (node.nodeType !== Node.TEXT_NODE) return;
    const fragment = document.createDocumentFragment();
    node.textContent.split(/(\s+)/).forEach((word) => {
      if (!word.trim()) { fragment.append(document.createTextNode(word)); return; }
      const span = document.createElement('span');
      span.className = 'word';
      span.textContent = word;
      span.dataset.order = String(order++);
      fragment.append(span);
    });
    node.replaceWith(fragment);
  });
  const wordObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      const delay = Number(entry.target.dataset.order) * 65;
      setTimeout(() => entry.target.classList.add('is-lit'), delay);
      wordObserver.unobserve(entry.target);
    });
  }, { rootMargin: '0px 0px -24% 0px', threshold: 0.5 });
  $$('.word', manifesto).forEach((word) => wordObserver.observe(word));
}

if ('IntersectionObserver' in window) {
  const sentinel = $('.top-sentinel');
  new IntersectionObserver(([entry]) => root.classList.toggle('scrolled', !entry.isIntersecting)).observe(sentinel);
  const navObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      $$('[data-nav]').forEach((link) => {
        if (link.hash === `#${entry.target.id}`) link.setAttribute('aria-current', 'location');
        else link.removeAttribute('aria-current');
      });
    });
  }, { rootMargin: '-15% 0px -55% 0px' });
  ['inicio', 'descargas-gratis', 'a-tu-manera', 'tu-sonido', 'preguntas', 'descargar'].forEach((id) => {
    const target = document.getElementById(id);
    if (target) navObserver.observe(target);
  });
}

// Diálogo nativo: foco atrapado por el navegador, Escape y retorno al disparador.
const menu = $('#mobile-menu');
const menuTrigger = $('.menu-trigger');
const menuClose = $('.menu-close');
let savedScroll = 0;
function openMenu() {
  if (menu.open) return;
  savedScroll = window.scrollY;
  menuTrigger.setAttribute('aria-expanded', 'true');
  menuTrigger.setAttribute('aria-label', 'Cerrar menú');
  menu.showModal();
  document.body.style.overflow = 'hidden';
}
function closeMenu() {
  if (menu.open) menu.close();
}
menuTrigger.addEventListener('click', () => menu.open ? closeMenu() : openMenu());
menuClose.addEventListener('click', closeMenu);
menu.addEventListener('close', () => {
  document.body.style.overflow = '';
  menuTrigger.setAttribute('aria-expanded', 'false');
  menuTrigger.setAttribute('aria-label', 'Abrir menú');
});
$$('a', menu).forEach((link) => link.addEventListener('click', closeMenu));
window.matchMedia('(min-width: 768px)').addEventListener('change', (event) => {
  if (event.matches) closeMenu();
});

const toast = $('.toast');
let toastTimer;
function notify(message) {
  toast.textContent = message;
  toast.classList.add('visible');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('visible'), 4200);
}

// Tres fragmentos instrumentales originales: reproducidos de verdad, sin autoplay.
const tracks = [
  { title: 'Costa adentro', file: 'costa-adentro.ogg', hue: 0, number: '01' },
  { title: 'Fuera de hora', file: 'fuera-de-hora.ogg', hue: 40, number: '02' },
  { title: 'Lado B', file: 'lado-b.ogg', hue: -30, number: '03' },
];
const audio = $('#demo-audio');
const play = $('#demo-play');
const seek = $('#demo-seek');
const frame = $('.hero-visual');
const favorite = $('.demo-like');
const elapsed = $('#demo-elapsed');
const duration = $('#demo-duration');
const favorites = new Set();
let current = 0;
let shuffle = false;
let repeat = false;
let isSeeking = false;
let loadGeneration = 0;
const formatTime = (seconds) => {
  if (!Number.isFinite(seconds)) return '0:00';
  const whole = Math.max(0, Math.floor(seconds));
  return `${Math.floor(whole / 60)}:${String(whole % 60).padStart(2, '0')}`;
};
function reflectPlayback() {
  const playing = !audio.paused && !audio.ended;
  frame.classList.toggle('is-playing', playing);
  play.setAttribute('aria-label', playing ? 'Pausar fragmento original' : 'Escuchar fragmento original');
  play.setAttribute('aria-pressed', String(playing));
  $('use', play).setAttribute('href', iconHref(playing ? 'pause' : 'play'));
}
function reflectFavorite() {
  const on = favorites.has(current);
  favorite.setAttribute('aria-pressed', String(on));
  favorite.setAttribute('aria-label', `${on ? 'Quitar' : 'Marcar'} ${tracks[current].title} ${on ? 'de favoritos' : 'como favorito'}`);
}
async function startAudio() {
  const generation = loadGeneration;
  play.setAttribute('aria-busy', 'true');
  try {
    await audio.play();
  } catch (error) {
    if (generation !== loadGeneration || error.name === 'AbortError') return;
    notify('No se pudo cargar el fragmento. Revisa tu conexión y vuelve a darle play.');
  } finally {
    if (generation === loadGeneration) {
      play.removeAttribute('aria-busy');
      reflectPlayback();
    }
  }
}
play.addEventListener('click', () => {
  if (audio.paused) startAudio();
  else audio.pause();
});
function setTrack(index, shouldPlay = !audio.paused) {
  loadGeneration++;
  audio.pause();
  current = (index + tracks.length) % tracks.length;
  const track = tracks[current];
  $('#demo-title').textContent = track.title;
  $('.cover-number').textContent = track.number;
  $('#demo-cover').style.filter = `hue-rotate(${track.hue}deg)`;
  audio.src = `${base}audio/${track.file}`;
  audio.loop = repeat;
  seek.value = 0;
  elapsed.textContent = '0:00';
  reflectFavorite();
  reflectPlayback();
  play.removeAttribute('aria-busy');
  if (!reduce.matches) {
    $('.demo-art').animate([{ opacity: .4, transform: 'translateY(4px)' }, { opacity: 1, transform: 'translateY(0)' }], { duration: 250, easing: 'cubic-bezier(.23,1,.32,1)' });
  }
  if (shouldPlay) startAudio();
}
$('#demo-prev').addEventListener('click', () => setTrack(current - 1));
$('#demo-next').addEventListener('click', () => {
  const increment = shuffle ? 1 + Math.floor(Math.random() * (tracks.length - 1)) : 1;
  setTrack(current + increment);
});
favorite.addEventListener('click', () => {
  if (favorites.has(current)) favorites.delete(current);
  else favorites.add(current);
  reflectFavorite();
});
$('.demo-shuffle').addEventListener('click', (event) => {
  shuffle = !shuffle;
  event.currentTarget.setAttribute('aria-pressed', String(shuffle));
  event.currentTarget.setAttribute('aria-label', `${shuffle ? 'Desactivar' : 'Activar'} reproducción aleatoria`);
});
$('.demo-repeat').addEventListener('click', (event) => {
  repeat = !repeat;
  audio.loop = repeat;
  event.currentTarget.setAttribute('aria-pressed', String(repeat));
  event.currentTarget.setAttribute('aria-label', `${repeat ? 'Desactivar' : 'Activar'} repetición del fragmento`);
});
['play','pause','ended'].forEach((name) => audio.addEventListener(name, reflectPlayback));
audio.addEventListener('ended', () => { if (!repeat) setTrack(current + 1, true); });
audio.addEventListener('loadedmetadata', () => {
  if (Number.isFinite(audio.duration)) {
    seek.max = String(audio.duration);
    duration.textContent = formatTime(audio.duration);
  }
});
audio.addEventListener('timeupdate', () => {
  if (!isSeeking) seek.value = String(audio.currentTime);
  elapsed.textContent = formatTime(audio.currentTime);
  seek.setAttribute('aria-valuetext', `${formatTime(audio.currentTime)} de ${formatTime(audio.duration || 32)}`);
});
seek.addEventListener('input', () => {
  isSeeking = true;
  const value = Number(seek.value);
  elapsed.textContent = formatTime(value);
  if (audio.readyState >= 1) audio.currentTime = value;
});
seek.addEventListener('change', () => { isSeeking = false; });
seek.addEventListener('blur', () => { isSeeking = false; });
document.addEventListener('visibilitychange', () => { if (document.hidden) audio.pause(); });

// Ecualizador expresamente visual; no atribuye procesamiento de audio inexistente.
const eqSliders = $$('.eq-sliders input');
const presetButtons = $$('[data-preset]');
const presets = { propio: [3,1,-2,2,4], graves: [8,5,0,-1,0], plano: [0,0,0,0,0] };
presetButtons.forEach((button) => {
  button.addEventListener('click', () => {
    const values = presets[button.dataset.preset];
    eqSliders.forEach((slider, index) => { slider.value = String(values[index]); });
    presetButtons.forEach((item) => item.setAttribute('aria-pressed', String(item === button)));
  });
});
eqSliders.forEach((slider) => slider.addEventListener('input', () => {
  presetButtons.forEach((button) => button.setAttribute('aria-pressed', 'false'));
}));

// Metadatos del mismo archivo que se sirve, sin IDs de analítica de otros proyectos.
fetch(`${base}descargas/version.json`, { cache: 'no-cache' })
  .then((response) => { if (!response.ok) throw new Error('release'); return response.json(); })
  .then((release) => {
    if (release.version !== '7.33' || !/^[a-f0-9]{64}$/.test(release.sha256)) throw new Error('release');
    $('#apk-sha').textContent = release.sha256;
    $('#apk-size').textContent = `${(release.bytes / 1048576).toLocaleString('es-PE', { maximumFractionDigits: 1 })} MB · APK firmado`;
  })
  .catch(() => { $('#apk-sha').textContent = 'Abre la ficha de la versión para verificar el archivo.'; });

// Señal local para futuras mediciones, no envía datos ni finge instalaciones.
$('.download-link').addEventListener('click', () => {
  document.dispatchEvent(new CustomEvent('melodia:download-click', { detail: { version: '7.33', location: 'cierre' } }));
});
