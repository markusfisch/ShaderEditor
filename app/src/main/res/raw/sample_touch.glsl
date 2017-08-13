#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform int pointerCount;
uniform vec3 pointers[10];
uniform vec2 resolution;

void main(void) {
	float mx = max(resolution.x, resolution.y);
	vec2 uv = gl_FragCoord.xy / mx;
	vec3 color = vec3(0.0);

	for (int n = 0; n < pointerCount; ++n) {
		color = max(color, smoothstep(
			0.085,
			0.08,
			distance(uv, pointers[n].xy / mx)));
	}

	gl_FragColor = vec4(color, 1.0);
}
