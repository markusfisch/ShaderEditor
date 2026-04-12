---
group: "basics"
order: 3
name: "Texture Sampling"
desc: "Load an image into the shader and animate it with time."
video: "media/sample-texture.webm"
poster: "media/sample-texture.webp"
links:
  - label: "The Book of Shaders — Textures"
    href: "https://thebookofshaders.com/12/"
  - label: "Khronos — texture2D reference"
    href: "https://registry.khronos.org/OpenGL-Refpages/es3.0/html/texture.xhtml"
code: |-
  #ifdef GL_FRAGMENT_PRECISION_HIGH
  precision highp float;
  #else
  precision mediump float;
  #endif
  
  uniform float time;
  uniform vec2 resolution;
  uniform sampler2D noise;
  
  void main(void) {
  	vec2 uv = gl_FragCoord.xy / max(resolution.x, resolution.y);
  
  	gl_FragColor = vec4(
  		texture2D(noise, uv + sin(time)).rgb,
  		1.0);
  }
---
ShaderEditor lets you import images as `sampler2D` uniforms. To add a texture, tap the menu → "Add uniform" → choose a 2D texture or pick one of the built-in presets like `noise`.

The shader reads pixel colors from the texture with `texture2D(sampler, uv)`. Adding `sin(time)` to the UV coordinates shifts the sampling position over time, creating a panning animation through the texture.

**Aspect-ratio note:** Dividing by `max(resolution.x, resolution.y)` instead of `resolution` keeps the texture's aspect ratio — dividing by `resolution` directly would stretch it to fill the screen.

**Try this:** Distort the texture: replace `uv + sin(time)` with `uv + 0.05 * vec2(sin(time + uv.y * 10.0), cos(time + uv.x * 10.0))` for a wavy water effect.