#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

/* Cloudy Conway
 * http://www.vexorian.com/2015/05/cloudy-conway.html?m=1
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
	// a cell is (or becomes) alive if it has 3 neighbors
	// or if it has 2 neighbors *and* was alive already
	float isAlive = min(1.0, has3 + has2 * wasAlive);
	float justDied = (1.0 - isAlive) * wasAlive;
	float afterglow = cell[2];
	return vec4(
		afterglow * isAlive,
		afterglow * (1.0 - isAlive),
		max(afterglow * 0.99, justDied),
		isAlive
	);
}

float get(float x, float y) {
	return get4(x, y).a;
}

void main() {
	float sum =
		get(-1.0, -1.0) +
		get(-1.0, 0.0) +
		get(-1.0, 1.0) +
		get(0.0, -1.0) +
		get(0.0, 1.0) +
		get(1.0, -1.0) +
		get(1.0, 0.0) +
		get(1.0, 1.0);

	float tapSize = min(resolution.x, resolution.y) * 0.05;
	for (int n = 0; n < pointerCount; ++n) {
		if (distance(pointers[n].xy, gl_FragCoord.xy) < tapSize) {
			sum = 3.0;
			break;
		}
	}

	gl_FragColor = evaluate(sum);
}
