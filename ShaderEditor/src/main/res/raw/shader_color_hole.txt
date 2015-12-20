#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform float time;
uniform int pointerCount;
uniform vec3 pointers[10];
uniform vec2 resolution;

void main( void )
{
	float mx = max( resolution.x, resolution.y );
	vec2 uv = gl_FragCoord.xy/mx;
	vec3 color = vec3(
		uv,
		0.25 + 0.5*sin( time ) );

	for( int n = 0; n < pointerCount; ++n )
	{
		vec3 hole = vec3(
			sin( 1.5 - distance(
				uv,
				pointers[n].xy/mx )*8.0 ) );

		color = mix( color, hole, -0.5 );
	}

	gl_FragColor = vec4( color, 1.0 );
}
