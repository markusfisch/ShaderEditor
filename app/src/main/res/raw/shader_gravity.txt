#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform vec2 resolution;
uniform vec3 gravity;

void main( void )
{
	vec2 uv = gl_FragCoord.xy/resolution.xy;

	gl_FragColor = vec4(
		vec3( uv, 1.0 )*9.80665/abs( gravity - 4.90332 ),
		1.0 );
}
