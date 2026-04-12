---
group: "advanced"
order: 1
name: "GLES 3.0 Syntax"
desc: "The same touch ripple shader ported to modern #version 300 es."
video: "media/sample-gles3.webm"
poster: "media/sample-gles3.webp"
links:
  - label: "Khronos — OpenGL ES 3.0 spec"
    href: "https://registry.khronos.org/OpenGL/specs/es/3.0/GLSL_ES_Specification_3.00.pdf"
  - label: "Android — OpenGL ES versions"
    href: "https://developer.android.com/develop/ui/views/graphics/opengl/about-opengl"
code: |-
  #version 300 es
  #ifdef GL_FRAGMENT_PRECISION_HIGH
  precision highp float;
  #else
  precision mediump float;
  #endif
  
  out vec4 fragColor;
  
  uniform float time;
  uniform int pointerCount;
  uniform vec3 pointers[10];
  uniform vec2 resolution;
  
  void main() {
  	float mx = max(resolution.x, resolution.y);
  	vec2 uv = gl_FragCoord.xy / mx;
  	vec3 color = vec3(uv, 0.25 + 0.5 * sin(time));
  
  	for (int n = 0; n < pointerCount; ++n) {
  		vec3 hole = vec3(sin(1.5 - distance(
  			uv, pointers[n].xy / mx) * 8.0));
  		color = mix(color, hole, -0.5);
  	}
  	fragColor = vec4(color, 1.0);
  }
---
GLES 3.0 (OpenGL ES 3.0) is supported on most Android devices from 2013 onward. ShaderEditor detects the `#version 300 es` directive and automatically creates a GLES 3.0 rendering context.

**Key syntax differences from GLES 2.0:**
- `#version 300 es` must be the **very first line** — no comments or blank lines before it.
- `gl_FragColor` is removed — you declare your own output: `out vec4 fragColor;`
- `texture2D()` becomes `texture()`.
- You gain integer textures, bitwise operators, flat interpolation, and `layout` qualifiers.

This example is intentionally the same Touch Ripples shader, rewritten for GLES 3.0, so you can compare the two side by side. The logic is identical — only the boilerplate changes.

**Try this:** Once in GLES 3.0, try integer operations: `int px = int(gl_FragCoord.x); if ((px & 1) == 0) fragColor = vec4(0);` creates a scanline effect impossible in GLES 2.0.