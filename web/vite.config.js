import { defineConfig } from 'vite';

export default defineConfig({
  base: '/melodia/',
  build: {
    target: 'es2020',
    outDir: 'dist',
    emptyOutDir: true,
    cssMinify: true,
    minify: 'esbuild',
  },
  preview: { allowedHosts: ['.trycloudflare.com', 'vnic-cesar.tailb6ee43.ts.net'] },
});
