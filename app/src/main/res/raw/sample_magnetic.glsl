#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform vec3 magnetic;

void main(void) {
	gl_FragColor = vec4(abs(magnetic) / 31.869, 1.0);
}
