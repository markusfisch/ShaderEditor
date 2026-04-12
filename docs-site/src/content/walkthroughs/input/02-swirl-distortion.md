---
group: "input"
order: 2
name: "Swirl Distortion"
desc: "Touch a texture to warp it with vortex distortions."
video: "media/sample-swirl.webm"
poster: "media/sample-swirl.webp"
links:
  - label: "The Book of Shaders — 2D Matrices"
    href: "https://thebookofshaders.com/08/"
  - label: "Wikipedia — Rotation matrix"
    href: "https://en.wikipedia.org/wiki/Rotation_matrix"
code: |-
  #ifdef GL_FRAGMENT_PRECISION_HIGH
  precision highp float;
  #else
  precision mediump float;
  #endif
  
  uniform float time;
  uniform vec2 resolution;
  uniform vec3 pointers[10];
  uniform sampler2D noise;
  uniform int pointerCount;
  
  void main(void) {
  	vec2 uv = gl_FragCoord.xy / resolution.xy;
  	vec2 distortion;
  
  	for (int i = 0; i < pointerCount; i++) {
  		vec2 pc = pointers[i].xy / resolution.xy;
  		vec2 st = uv - pc;
  		float angle = 6.2831 /
  			(exp(20. * length(st)) + 1.) *
  			sin(time * 10.);
  		float s = sin(angle);
  		float c = cos(angle) - 1.;
  		distortion += mat2(c, s, -s, c) * st;
  	}
  
  	gl_FragColor = vec4(
  		texture2D(noise, uv + distortion).rgb,
  		1.0
  	);
  }
---
This shader applies a **rotation matrix** to UV coordinates based on distance from each touch point. Near a finger, the rotation angle is large (creating a swirl); far away, `exp(20.0 * length(st))` makes the denominator huge, so the angle drops to zero — no distortion.

The `mat2(c, s, -s, c)` is a 2D rotation matrix. Subtracting 1 from cosine (`cos(angle) - 1.0`) makes the distortion additive — the identity matrix gives zero displacement. Each touch point contributes its own swirl and they **accumulate**.

**How to use:** This shader requires a texture uniform. Tap menu → "Add uniform" → pick any 2D texture or use the `noise` preset. Then touch the screen to create swirls.

**Try this:** Change `20.` to `5.` for wider, softer swirls. Remove `sin(time * 10.)` to make the swirl direction constant instead of oscillating.