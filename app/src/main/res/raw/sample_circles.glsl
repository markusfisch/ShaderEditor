#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform vec2 resolution;
uniform float time;

void main(void) {
	float mx = max(resolution.x, resolution.y);
	vec2 uv = gl_FragCoord.xy / mx;
	vec2 center = resolution / mx * 0.5;
	float t = time * 10.0;

	gl_FragColor = vec4(
		vec3(sin(t - distance(uv, center) * 255.0)) * 0.2,
		1.0);
}
