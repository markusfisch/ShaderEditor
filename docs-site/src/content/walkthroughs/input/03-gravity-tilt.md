---
group: "input"
order: 3
name: "Gravity Tilt"
desc: "Colors respond to real device tilt using the gravity sensor."
video: "media/sample-gravity.webm"
poster: "media/sample-gravity.webp"
links:
  - label: "Android — Sensor types"
    href: "https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion"
code: |-
  #ifdef GL_FRAGMENT_PRECISION_HIGH
  precision highp float;
  #else
  precision mediump float;
  #endif
  
  uniform vec2 resolution;
  uniform vec3 gravity;
  
  void main(void) {
  	vec2 uv = gl_FragCoord.xy / resolution.xy;
  
  	gl_FragColor = vec4(
  		vec3(uv, 1.0) * 9.80665 / abs(gravity - 4.90332),
  		1.0);
  }
---
The `gravity` uniform is a `vec3` reporting the device's gravity vector in m/s². When the phone is flat on a table, gravity ≈ `(0, 0, 9.81)`. Tilting shifts values into the x and y components.

The expression `9.80665 / abs(gravity - 4.90332)` creates a non-linear color response: when a gravity component is near 4.9 (half of Earth gravity), the division approaches infinity and the color channel saturates. This produces vivid color pops as you tilt through specific angles.

**Uniforms used:** `gravity` (vec3, m/s²) and `resolution` (vec2). The gravity sensor must be present on the device — most Android phones have one.

**Try this:** For a smoother response, replace the formula with `vec3(uv, 1.0) * gravity * 0.1`. To make a tilt-controlled spotlight, compute `vec2 tilt = gravity.xy / 9.81` and use it as a center point for a radial gradient.