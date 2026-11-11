package de.markusfisch.android.shadereditor.highlighter;

import static de.markusfisch.android.shadereditor.highlighter.Styles.assertStyles;

import org.junit.Test;

/**
 * The colour of every character as the editor shows it. See {@link Styles}
 * for the letters.
 */
public class LexerStylesTest {
	@Test
	public void declaration() {
		assertStyles(
				"float x = 1.0; // hi",
				"ppppp.....nnn..ccccc");
	}

	@Test
	public void vectorConstructor() {
		assertStyles(
				"vec3 color = vec3(0.0, 0.5, 1.0);",
				"vvvv.........vvvv.nnn..nnn..nnn..");
	}

	@Test
	public void matrices() {
		assertStyles(
				"mat4 m; mat2x3 n; dmat3 d;",
				"mmmm....mmmmmm....mmmmm...");
	}

	@Test
	public void uniforms() {
		assertStyles(
				"uniform sampler2D tex; uniform vec2 resolution;",
				"qqqqqqq.sssssssss......qqqqqqq.vvvv............");
	}

	@Test
	public void inAndOut() {
		assertStyles(
				"in vec4 v; out vec4 fragColor; inout int k;",
				"qq.vvvv....qqq.vvvv............qqqqq.ppp...");
	}

	@Test
	public void primitives() {
		assertStyles(
				"const int N = 4; bool b; uint u; double d;",
				"ppppp.ppp.....n..pppp....pppp....pppppp...");
	}

	@Test
	public void samplerImageAndAtomicTypes() {
		assertStyles(
				"samplerCube c; image2D img; atomic_uint a;",
				"sssssssssss....iiiiiii......aaaaaaaaaaa...");
	}

	@Test
	public void precision() {
		assertStyles(
				"precision mediump float; highp vec2 p; lowp int q;",
				"rrrrrrrrr.rrrrrrr.ppppp..rrrrr.vvvv....rrrr.ppp...");
	}

	@Test
	public void numbers() {
		assertStyles(
				"1 1u 0x1F 017 019 0 1. .5 1e3 1.5e-3 2.0f 1E+2",
				"n.nn.nnnn.nnn.xxx.n.nn.nn.nnn.nnnnnn.nnnn.nnnn");
	}

	@Test
	public void exponentWithoutDigits() {
		assertStyles(
				"1e x;",
				"xx...");
	}

	@Test
	public void operators() {
		assertStyles(
				"a += b << 2; c = a && b || !c; i++; --j; x = y ? z : w;",
				"..........n............................................");
	}

	@Test
	public void unexpectedCharacters() {
		assertStyles(
				"@ $ `",
				"x.x.x");
	}

	@Test
	public void blockComments() {
		assertStyles(
				"/* block */ int a; /* multi\nline */",
				"ccccccccccc.ppp....cccccccccccccccc");
	}

	@Test
	public void unterminatedBlockComment() {
		assertStyles(
				"int a; /* unterminated\nint b;",
				"ppp....cccccccccccccccccccccc");
	}

	@Test
	public void lineComment() {
		assertStyles(
				"// line\nint a;",
				"ccccccc.ppp...");
	}

	@Test
	public void version() {
		assertStyles(
				"#version 300 es\nprecision highp float;",
				"dddddddd.nnn....rrrrrrrrr.rrrrr.ppppp.");
	}

	@Test
	public void conditionalPrecision() {
		assertStyles(
				"#ifdef GL_FRAGMENT_PRECISION_HIGH\n" +
						"precision highp float;\n" +
						"#else\n" +
						"precision mediump float;\n" +
						"#endif",
				"dddddd............................" +
						"rrrrrrrrr.rrrrr.ppppp.." +
						"ddddd." +
						"rrrrrrrrr.rrrrrrr.ppppp.." +
						"dddddd");
	}

	@Test
	public void define() {
		assertStyles(
				"#define PI 3.14159\nfloat a = PI;",
				"ddddddd....nnnnnnn.ppppp........");
	}

	@Test
	public void indentedDirective() {
		assertStyles(
				"  #define X 1",
				"..ddddddd...n");
	}

	@Test
	public void hashInsideALine() {
		assertStyles(
				"x # y",
				"..x..");
	}

	@Test
	public void struct() {
		assertStyles(
				"struct Ray { vec3 o; vec3 d; };",
				"kkkkkk.ttt...vvvv....vvvv......");
	}

	@Test
	public void fieldSelection() {
		assertStyles(
				"p.xy = gl_FragCoord.xy / resolution.xy;",
				"..ff...bbbbbbbbbbbb.ff..............ff.");
	}

	@Test
	public void builtinFunctions() {
		assertStyles(
				"c.rgb = texture(tex, uv).rgb * sin(time) + mix(a, b, 0.5);",
				"..fff...bbbbbbb..........fff...bbb.........bbb.......nnn..");
	}

	@Test
	public void controlFlow() {
		assertStyles(
				"for (int i = 0; i < 4; ++i) { if (x) break; else continue; }\n" +
						"return; discard;",
				"ooo..ppp.....n......n.........oo.....ooooo..oooo.oooooooo...." +
						"oooooo..ooooooo.");
	}

	@Test
	public void loopsAndSwitch() {
		assertStyles(
				"while (x) { do { switch (n) { case 1: default: } } while (y); }",
				"ooooo.......oo...oooooo.......oooo.n..ooooooo......ooooo.......");
	}

	@Test
	public void mainImage() {
		assertStyles(
				"void mainImage(out vec4 fragColor, in vec2 fragCoord) {}",
				"kkkk...........qqq.vvvv............qq.vvvv..............");
	}

	@Test
	public void lineContinuationInsideAWord() {
		assertStyles(
				"fl\\\noat x;",
				"ppppppp...");
	}

	@Test
	public void crlf() {
		assertStyles(
				"float a;\r\nfloat b;",
				"ppppp.....ppppp...");
	}

	@Test
	public void emojiInCodeAndComment() {
		assertStyles(
				"// 😀\nx = 😀;",
				"ccccc.....xx.");
	}
}
