---
group: "camera"
order: 2
name: "Battery Gauge"
desc: "A horizontal bar showing real-time battery level, color-coded from red to green."
video: "media/sample-battery.webm"
poster: "media/sample-battery.webp"
links:
  - label: "Khronos — step() reference"
    href: "https://registry.khronos.org/OpenGL-Refpages/gl4/html/step.xhtml"
  - label: "Khronos — smoothstep() reference"
    href: "https://registry.khronos.org/OpenGL-Refpages/gl4/html/smoothstep.xhtml"
code: |-
  #ifdef GL_FRAGMENT_PRECISION_HIGH
  precision highp float;
  #else
  precision mediump float;
  #endif
  
  uniform vec2 resolution;
  uniform float battery;
  
  void main(void) {
  	vec2 uv = gl_FragCoord.xy / resolution;
  	vec3 bar = vec3(step(uv.x, battery));
  	bar *= mix(vec3(1.0, 0.2, 0.2), vec3(0.2, 1.0, 0.3), battery);
  	gl_FragColor = vec4(bar, 1.0);
  }
---
This shader turns the screen into a live battery indicator. The `battery` uniform is a float from 0.0 (empty) to 1.0 (full), updated in real time by the system.

`step(uv.x, battery)` is the key: it returns 1.0 for all pixels whose x-coordinate is less than the battery level, and 0.0 for the rest. This creates a hard-edged bar whose width matches the charge percentage.

`mix(vec3(1, 0.2, 0.2), vec3(0.2, 1, 0.3), battery)` interpolates the bar's color from red (low) to green (full) based on the same battery value. The bar gets greener as you charge.

**Uniforms used:** `battery` (float, 0–1) and `resolution`. Also available: `powerConnected` (bool, true when charging) and `nightMode` (bool, system dark mode).

**Try this:** Replace `step()` with `smoothstep(battery - 0.01, battery + 0.01, uv.x)` for a soft edge. Add a glow: `float glow = 0.005 / abs(uv.x - battery);` for a bright line at the battery edge.