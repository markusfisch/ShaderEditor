import { defineConfig } from 'astro/config';

export default defineConfig({
  site: 'https://markusfisch.github.io',
  base: '/ShaderEditor/',
  build: {
    assets: '_assets',
  },
});
