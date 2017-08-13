#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform float light;

void main(void) {
	gl_FragColor = vec4(vec3(light) / 4.0, 1.0);
}
