---
group: "backbuffer"
order: 2
name: "Cloudy Conway"
desc: "Game of Life variant with colorful afterglow trails when cells die."
video: "media/sample-cloudy.webm"
poster: "media/sample-cloudy.webp"
links:
  - label: "Vexorian — Cloudy Conway"
    href: "http://www.vexorian.com/2015/05/cloudy-conway.html"
code: |-
  #ifdef GL_FRAGMENT_PRECISION_HIGH
  precision highp float;
  #else
  precision mediump float;
  #endif
  
  /* Cloudy Conway
   * http://www.vexorian.com/2015/05/cloudy-conway.html
   */
  
  uniform vec2 resolution;
  uniform int pointerCount;
  uniform vec3 pointers[10];
  uniform sampler2D backbuffer;
  
  float oneIfZero(float value) {
  	return step(abs(value), 0.1);
  }
  
  vec4 get4(float x, float y) {
  	return texture2D(backbuffer,
  		(gl_FragCoord.xy + vec2(x, y)) / resolution);
  }
  
  vec4 evaluate(float sum) {
  	vec4 cell = get4(0.0, 0.0);
  	float wasAlive = cell[3];
  	float has3 = oneIfZero(sum - 3.0);
  	float has2 = oneIfZero(sum - 2.0);
  	float isAlive = min(1.0, has3 + has2 * wasAlive);
  	float justDied = (1.0 - isAlive) * wasAlive;
  	float afterglow = cell[2];
  	return vec4(
  		afterglow * isAlive,
  		afterglow * (1.0 - isAlive),
  		max(afterglow * 0.99, justDied),
  		isAlive);
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
This builds on the Game of Life by using **all four RGBA channels** to track cell state over time, not just alive/dead. The alpha channel stores whether the cell is alive. The blue channel stores "afterglow" — a value that jumps to 1.0 when a cell dies, then slowly decays (`afterglow * 0.99` each frame).

The `evaluate()` function routes afterglow through red and green based on alive/dead status, creating a two-tone color trail: living cells glow one color, recently-dead cells fade through another. The `0.99` decay factor means trails persist for hundreds of frames.

**vec4 channel mapping:** R = afterglow × alive (living cell color), G = afterglow × dead (trail color), B = max(decay, justDied) (afterglow tracker), A = alive flag.

**Try this:** Change `0.99` to `0.95` for faster-fading trails, or `0.999` for very long persistence. Swap the red and green assignments in the return statement to change the color scheme.