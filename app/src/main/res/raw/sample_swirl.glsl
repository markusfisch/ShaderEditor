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
