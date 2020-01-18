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
	// a cell is (or becomes) alive if it has 3 neighbors
	// or if it has 2 neighbors *and* was alive already
	return vec3(has3 + has2 * get(0.0, 0.0));
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

	float tap = min(resolution.x, resolution.y) * 0.05;
	for (int n = 0; n < pointerCount; ++n) {
		if (distance(pointers[n].xy, gl_FragCoord.xy) < tap) {
			sum = 3.0;
			break;
		}
	}

	gl_FragColor = vec4(evaluate(sum), 1.0);
}
