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
