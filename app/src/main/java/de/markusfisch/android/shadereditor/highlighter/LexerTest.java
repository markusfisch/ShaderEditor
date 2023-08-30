package de.markusfisch.android.shadereditor.highlighter;

import de.markusfisch.android.shadereditor.util.OneTimeIterable;

public class LexerTest {
	public static void main(String[] args) {
		String source = "if (false) {\n" + // 1
				"  // Test\n" + // 2
				"} else {\n" + // 3
				"  /*\n" + // 4
				"   * Multiline!\n" + // 5
				"   */\n" + // 6
				"}\n"; // 7 & 8
		Lexer lexer = new Lexer(
				source
				, 2);
		int i = 0;
		for (Token token : new OneTimeIterable<>(new TokenByLineIterator(source, lexer.iterator()))) {
			System.out.println("\"" + source.substring(token.getStartOffset(), token.getEnd()) + "\" ("+ token + ")");
			if (++i > 200) break;
		}
	}
}
