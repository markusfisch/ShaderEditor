#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform float time;
uniform vec2 resolution;
uniform vec2 offset;
uniform sampler2D backbuffer;

void main( void )
{
	float mx = max( resolution.x, resolution.y );
	vec2 uv = (gl_FragCoord.xy - resolution.xy*0.5)/mx;

	uv += offset*0.1;

	float r = 0.7;
	uv *= mat2(
		r, -r,
		r, r );

	float y = uv.y*mx*0.05 + time;
	float f = fract( y );
	f = (max( 0.4, min( f, 1.0 - f ) ) - 0.4)*10.0;

	vec3 color =
		vec3(
			mod( y, 6.0 )*f,
			mod( y, 2.0 )*f,
			mod( y, 0.9 )*f )*
		abs( sin( mod(
			30.0 + uv.x,
			uv.y + 1.0 ) ) );

	color = mix(
		texture2D(
			backbuffer,
			gl_FragCoord.xy/mx ).rgb,
		color,
		0.5 );

	gl_FragColor = vec4( color, 1.0 );
}
