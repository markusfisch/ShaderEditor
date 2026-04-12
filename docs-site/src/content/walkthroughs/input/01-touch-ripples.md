---
group: "input"
order: 1
name: "Touch Ripples"
desc: "Multi-touch creates rippling waves across a gradient background."
video: "media/sample-touch.webm"
poster: "media/sample-touch.webp"
links:
  - label: "The Book of Shaders — Shapes"
    href: "https://thebookofshaders.com/07/"
  - label: "Khronos — mix() reference"
    href: "https://registry.khronos.org/OpenGL-Refpages/gl4/html/mix.xhtml"
code: |-
  #ifdef GL_FRAGMENT_PRECISION_HIGH
  precision highp float;
  #else
  precision mediump float;
  #endif
  
  uniform float time;
  uniform int pointerCount;
  uniform vec3 pointers[10];
  uniform vec2 resolution;
  
  void main(void) {
  	float mx = max(resolution.x, resolution.y);
  	vec2 uv = gl_FragCoord.xy / mx;
  	vec3 color = vec3(uv, 0.25 + 0.5 * sin(time));
  
  	for (int n = 0; n < pointerCount; ++n) {
  		vec3 hole = vec3(sin(1.5 - distance(
  			uv, pointers[n].xy / mx) * 8.0));
  		color = mix(color, hole, -0.5);
  	}
  	gl_FragColor = vec4(color, 1.0);
  }
---
ShaderEditor provides up to 10 simultaneous touch points through the `pointers[10]` array (each a `vec3` with x, y in pixels, z = pressure) and `pointerCount` (how many are active).

The background is a smooth UV gradient animated by `time`. For each active pointer, the shader computes the `distance()` from the current pixel to the touch position, wraps it through `sin()` to create rings, then blends it into the background using `mix()` with a **negative factor** (`-0.5`). Negative mix factors extrapolate rather than interpolate, creating the characteristic dark ripple inversion.

**Uniforms used:** `pointerCount` (int), `pointers[10]` (vec3 array), `time`, `resolution`.

**Try this:** Change `8.0` to `30.0` for tighter rings. Change `-0.5` to `0.5` for a bright glow instead of dark ripples. Add `+ time * 5.0` inside the `sin()` to animate the rings outward from each finger.