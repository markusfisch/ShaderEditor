---
group: "input"
order: 4
name: "Orientation Grid"
desc: "A tiled pattern that rotates with the device orientation."
video: "media/sample-orientation.webm"
poster: "media/sample-orientation.webp"
links:
  - label: "Android — Position sensors"
    href: "https://developer.android.com/develop/sensors-and-location/sensors/sensors_position"
  - label: "The Book of Shaders — 2D Matrices"
    href: "https://thebookofshaders.com/08/"
code: |-
  #ifdef GL_FRAGMENT_PRECISION_HIGH
  precision highp float;
  #else
  precision mediump float;
  #endif
  
  uniform vec2 resolution;
  uniform vec3 orientation;
  
  void main(void) {
  	vec2 uv = gl_FragCoord.xy /
  		min(resolution.x, resolution.y);
  
  	uv = uv * 2.0 - 1.0;
  
  	vec3 uv3 = vec3(uv, 0.0);
  	float a = orientation.z;
  	float ac = cos(a);
  	float as = sin(a);
  	uv3 *= mat3(
  		ac, as, 0.0,
  		-as, ac, 0.0,
  		0.0, 0.0, 1.0);
  	uv.x = uv3.x;
  	uv.y = uv3.y;
  
  	uv = mod(uv, 0.2) * 5.0;
  
  	gl_FragColor = vec4(uv, 1.0, 1.0);
  }
---
The `orientation` uniform is a `vec3` containing azimuth, pitch, and roll in radians. This shader uses `orientation.z` (roll) to build a 3×3 rotation matrix that spins the UV coordinate space.

The UVs are first centered (`uv * 2.0 - 1.0`), then rotated using `mat3(cos, sin, 0, -sin, cos, 0, 0, 0, 1)` — a standard Z-axis rotation. After rotation, `mod(uv, 0.2) * 5.0` tiles the space into a repeating 5×5 grid and maps tile-local coordinates to color. The result is a checkered pattern that tilts with your device.

**Uniforms used:** `orientation` (vec3, radians) and `resolution`. Dividing by `min(resolution.x, resolution.y)` ensures uniform scaling in both directions.

**Try this:** Use `orientation.x` (azimuth) instead of `.z` to react to compass heading. Change `0.2` to `0.1` for a finer grid. Add a time component: `float a = orientation.z + time * 0.5;` for auto-spin.