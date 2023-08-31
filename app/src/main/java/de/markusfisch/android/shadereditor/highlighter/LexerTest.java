package de.markusfisch.android.shadereditor.highlighter;

import java.util.stream.Collectors;

import de.markusfisch.android.shadereditor.util.OneTimeIterable;

public class LexerTest {
	public static void main(String[] args) {
		System.out.println("🍺".chars().mapToObj(v->Integer.toString(v,16)).collect(Collectors.toList()));
		String source = "" +
			"/* test */\n" +
			"int hello;vec2🥃 hello;\n" +
			"vec3 hello(vec2 🥱morning) {\n" +
			"	vec4 b;\n" +
			"	// 🥃😉🤔🍰🤙😊\n" +
			"\n" +
			"};\n";
		Lexer lexer = new Lexer(
			source
			, 2);
		int i = 0;
		for (Token token : new OneTimeIterable<>(new TokenByLineIterator(source, lexer.iterator()))) {
			System.out.println(token);
			System.out.println("\"" + source.substring(token.startOffset(), token.startOffset() + (source.length() >(token.startOffset() + 4) ? 4 : 0)) +
				"\" (" + token + ")");
			if (++i > 200) break;
		}
	}
}
