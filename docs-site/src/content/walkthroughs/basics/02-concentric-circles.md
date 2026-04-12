---
group: "basics"
order: 2
name: "Concentric Circles"
desc: "Expanding rings from the screen center using distance functions."
video: "media/sample-circles.webm"
poster: "media/sample-circles.webp"
links:
  - label: "The Book of Shaders — Shapes"
    href: "https://thebookofshaders.com/07/"
  - label: "Inigo Quilez — 2D Distance Functions"
    href: "https://iquilezles.org/articles/distfunctions2d/"
code: |-
  #ifdef GL_FRAGMENT_PRECISION_HIGH
  precision highp float;
  #else
  precision mediump float;
  #endif
  
  uniform vec2 resolution;
  uniform float time;
  
  void main(void) {
  	float mx = max(resolution.x, resolution.y);
  	vec2 uv = gl_FragCoord.xy / mx;
  	vec2 center = resolution / mx * 0.5;
  	float t = time * 10.0;
  
  	gl_FragColor = vec4(
  		vec3(sin(t - distance(uv, center) * 255.0)) * 0.2,
  		1.0);
  }
---
This shader computes the `distance()` from every pixel to the center of the screen, then feeds it into `sin()` to create concentric rings. Multiplying the distance by `255.0` packs many rings tightly together.

**Aspect-ratio handling:** Instead of dividing by `resolution` directly (which would stretch the circles on non-square screens), both the UV and the center are divided by `max(resolution.x, resolution.y)`. This keeps circles perfectly round regardless of screen shape.

**Uniforms used:** `resolution` (vec2) and `time` (float). Time is multiplied by 10 to animate the rings outward.

**Try this:** Replace `sin(...) * 0.2` with `step(0.0, sin(...))` for sharp black-and-white rings. Add color by using separate frequencies per channel: `vec3(sin(t - d*200.), sin(t - d*255.), sin(t - d*300.))`.