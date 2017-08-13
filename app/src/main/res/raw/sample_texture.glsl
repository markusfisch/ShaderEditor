#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform float time;
uniform vec2 resolution;
uniform sampler2D noise;

void main(void) {
	vec2 uv = gl_FragCoord.xy / max(resolution.x, resolution.y);

	gl_FragColor = vec4(
		texture2D(noise, uv + sin(time)).rgb,
		1.0);
}
