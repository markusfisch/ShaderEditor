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
	vec2 coord = gl_FragCoord.xy / resolution.xy;
	vec2 dCoord;
	for(int i=0;i<pointerCount;i++){
	    vec2 pc = pointers[i].xy / resolution.xy;
	    vec2 uv = coord-pc;
	    float angle=360./180.*3.1415927*1./(exp(20.*length(uv))+1.)*sin(time*10.) ;
	    float s=sin(angle);
	    float c=cos(angle)-1.;
	    mat2 rot=mat2(c, s, -s, c);
	    dCoord=dCoord+rot*uv;
	}

	gl_FragColor = vec4(
		texture2D(noise, coord+dCoord).rgb,
		1.0
	);
}
