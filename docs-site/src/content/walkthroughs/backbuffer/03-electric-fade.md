---
group: "backbuffer"
order: 3
name: "Electric Fade"
desc: "Cloudy Conway with a noise-seeded backbuffer for instant patterns."
video: "media/sample-electric.webm"
poster: "media/sample-electric.webp"
links:
  - label: "Vexorian — Cloudy Conway"
    href: "http://www.vexorian.com/2015/05/cloudy-conway.html"
  - label: "Wikipedia — Cellular automaton"
    href: "https://en.wikipedia.org/wiki/Cellular_automaton"
code: |-
  #ifdef GL_FRAGMENT_PRECISION_HIGH
  precision highp float;
  #else
  precision mediump float;
  #endif
  
  /* Based on Cloudy Conway
   * http://www.vexorian.com/2015/05/cloudy-conway.html
   */
  
  uniform vec2 resolution;
  uniform int pointerCount;
  uniform vec3 pointers[10];
  uniform sampler2D backbuffer;///p:noise
  
  float oneIfZero(float value) {
  	return step(abs(value), 0.1);
  }
  
  vec4 get4(float x, float y) {
  	return texture2D(backbuffer,
  		(gl_FragCoord.xy + vec2(x, y)) / resolution);
  }
  
  vec4 evaluate(float sum) {
  	vec4 cell = get4(0.0, 0.0);
  	float wasAlive = cell[0];
  	float has3 = oneIfZero(sum - 3.0);
  	float has2 = oneIfZero(sum - 2.0);
  	float isAlive = min(1.0, has3 + has2 * wasAlive);
  	float justDied = (1.0 - isAlive) * wasAlive;
  	float afterglow = cell[2];
  	return vec4(
  		isAlive,
  		afterglow * isAlive,
  		max(afterglow * 0.99, justDied),
  		afterglow * (1.0 - isAlive));
  }
  
  float get(float x, float y) {
  	return get4(x, y).a;
  }
  
  void main() {
  	float sum =
  		get(-1.0, -1.0) + get(-1.0, 0.0) + get(-1.0, 1.0) +
  		get( 0.0, -1.0) +                   get( 0.0, 1.0) +
  		get( 1.0, -1.0) + get( 1.0, 0.0) + get( 1.0, 1.0);
  
  	float tapSize = min(resolution.x, resolution.y) * 0.05;
  	for (int n = 0; n < pointerCount; ++n) {
  		if (distance(pointers[n].xy, gl_FragCoord.xy) < tapSize) {
  			sum = 3.0;
  			break;
  		}
  	}
  
  	gl_FragColor = evaluate(sum);
  }
---
This is a Cloudy Conway variant with one crucial difference: the `backbuffer` declaration includes `///p:noise` — a ShaderEditor directive that **pre-fills the backbuffer with a noise texture** instead of starting blank.

Because the noise provides random initial alive/dead states, the simulation ignites immediately with complex patterns instead of requiring you to touch-seed cells manually. The color channel mapping is also swapped (alive → red, afterglow routing → green/alpha), producing an electric blue-to-cyan palette.

**The `///p:` syntax:** When you declare `uniform sampler2D backbuffer;///p:noise`, ShaderEditor initializes the backbuffer's first frame from the named preset texture. This is a ShaderEditor-specific extension — it won't work in other GLSL environments.

**Try this:** Change `///p:noise` to just `backbuffer;` (no preset) and compare — you'll get a blank start that requires touch seeding. Try other presets by importing your own textures through "Add uniform".