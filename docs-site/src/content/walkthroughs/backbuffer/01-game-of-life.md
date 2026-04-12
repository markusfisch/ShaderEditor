---
group: "backbuffer"
order: 1
name: "Game of Life"
desc: "Conway's cellular automaton running on the GPU — touch to seed cells."
video: "media/sample-gol.webm"
poster: "media/sample-gol.webp"
links:
  - label: "Wikipedia — Conway's Game of Life"
    href: "https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life"
  - label: "The Book of Shaders — Cellular patterns"
    href: "https://thebookofshaders.com/12/"
code: |-
  #ifdef GL_FRAGMENT_PRECISION_HIGH
  precision highp float;
  #else
  precision mediump float;
  #endif
  
  uniform vec2 resolution;
  uniform int pointerCount;
  uniform vec3 pointers[10];
  uniform sampler2D backbuffer;
  
  float get(float x, float y) {
  	return texture2D(backbuffer,
  		(gl_FragCoord.xy + vec2(x, y)) / resolution).r;
  }
  
  float oneIfZero(float value) {
  	return step(abs(value), 0.1);
  }
  
  vec3 evaluate(float sum) {
  	float has3 = oneIfZero(sum - 3.0);
  	float has2 = oneIfZero(sum - 2.0);
  	// alive if 3 neighbors, or 2 + was alive
  	return vec3(has3 + has2 * get(0.0, 0.0));
  }
  
  void main() {
  	float sum =
  		get(-1.0, -1.0) + get(-1.0, 0.0) + get(-1.0, 1.0) +
  		get( 0.0, -1.0) +                   get( 0.0, 1.0) +
  		get( 1.0, -1.0) + get( 1.0, 0.0) + get( 1.0, 1.0);
  
  	float tap = min(resolution.x, resolution.y) * 0.05;
  	for (int n = 0; n < pointerCount; ++n) {
  		if (distance(pointers[n].xy, gl_FragCoord.xy) < tap) {
  			sum = 3.0;
  			break;
  		}
  	}
  
  	gl_FragColor = vec4(evaluate(sum), 1.0);
  }
---
The `backbuffer` uniform is a `sampler2D` that contains the **previous frame's output**. This lets each frame build on the last — essential for simulations, trails, and feedback effects.

Each pixel samples its 8 neighbors from the backbuffer and sums their red channel values. Conway's rules are encoded using `step()`: the `oneIfZero()` helper returns 1.0 only when the input equals zero (within a 0.1 tolerance). A cell is born if it has exactly 3 neighbors (`has3`), or survives if it has 2 and was already alive (`has2 * get(0,0)`).

Touch input acts as a "brush" — when any pointer is within `tap` distance, the neighbor sum is forced to 3.0, birthing new cells.

**Uniforms used:** `backbuffer` (sampler2D), `pointerCount`, `pointers[10]`, `resolution`.

**Try this:** Map alive-time to color by storing age in the green channel. Change the `0.05` multiplier for a bigger or smaller touch brush. Add `* 0.99` to the output to make cells fade and create trails.