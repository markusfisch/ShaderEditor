---
group: "basics"
order: 1
name: "Rainbow Gradient"
desc: "Animated color gradient — the \"hello world\" of fragment shaders."
video: "media/sample-rainbow.webm"
poster: "media/sample-rainbow.webp"
links:
  - label: "Inigo Quilez — Color palettes"
    href: "https://iquilezles.org/articles/palettes/"
  - label: "The Book of Shaders — Colors"
    href: "https://thebookofshaders.com/06/"
code: |-
  #ifdef GL_FRAGMENT_PRECISION_HIGH
  precision highp float;
  #else
  precision mediump float;
  #endif
  
  uniform float time;
  uniform vec2 resolution;
  
  void main(void) {
  	vec2 uv = gl_FragCoord.xy / resolution;
  	vec3 col = 0.5 + 0.5 * cos(time + uv.xyx + vec3(0, 2, 4));
  	gl_FragColor = vec4(col, 1.0);
  }
---
Every fragment shader runs once per pixel, every frame. `gl_FragCoord.xy` gives the pixel's position, and dividing by `resolution` normalizes it to 0–1 — this is called **UV coordinates**.

The color formula `0.5 + 0.5 * cos(time + uv.xyx + vec3(0, 2, 4))` is a classic trick by Inigo Quilez: it produces smooth, cycling rainbow hues by phase-shifting cosine waves across the RGB channels. The `vec3(0, 2, 4)` offsets red, green, and blue so they peak at different UV positions.

**Uniforms used:** `time` (float, seconds since shader started) and `resolution` (vec2, viewport size in pixels). These are the two most fundamental ShaderEditor uniforms — almost every shader needs them.

**Try this:** Change `vec3(0, 2, 4)` to `vec3(0, 0.5, 1)` for a warmer palette, or multiply `uv.xyx` by `3.0` for more color bands.