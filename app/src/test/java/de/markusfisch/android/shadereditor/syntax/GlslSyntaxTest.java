package de.markusfisch.android.shadereditor.syntax;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// Completion from the caret's line offers what the old editor offers from the
// tokens of the whole text.
public class GlslSyntaxTest {
	private static final List<String> BRACKETS = Arrays.asList("{", "}", "(",
			")", "[", "]");

	@Test
	public void completesAsTheOldEditorOnEveryBundledShader()
			throws IOException {
		for (Map.Entry<String, String> shader :
				BundledShaders.read().entrySet()) {
			assertSameEverywhere(shader.getKey(), shader.getValue());
		}
	}

	@Test
	public void completesAsTheOldEditorAroundComments() {
		for (String text : new String[]{
				"/* a\nb */ fl", "// a \\\nflo", "/* a */\nfl", "/**/x",
				"/*/ a\n*/", "a /* b */ c", "#def", "#define F flo",
				"x = .5;\n\ty"}) {
			assertSameEverywhere("", text);
		}
	}

	private static void assertSameEverywhere(String where, String text) {
		Tokens tokens = GlslLexer.lex(text);
		List<String> lines = Arrays.asList(text.split("\n", -1));
		LineTokens scratch = new LineTokens();
		int state = Syntax.TEXT_START;
		int lineStart = 0;
		for (int n = 0; n < lines.size(); ++n) {
			String s = lines.get(n);
			boolean breakFollows = n < lines.size() - 1;
			LexedLine line = LexedLine.of(GlslSyntax.INSTANCE, s,
					breakFollows, state, scratch);
			for (int column = 0; column <= s.length(); ++column) {
				List<Object> offered = new ArrayList<>();
				GlslSyntax.INSTANCE.complete(line, column,
						(words, position) -> {
							offered.add(words);
							offered.add(position);
						});
				assertEquals(where + " " + n + ":" + column,
						oldEditor(tokens, text, lineStart + column),
						offered);
			}
			state = GlslLexer.lexLine(s, breakFollows, state, scratch);
			lineStart += s.length() + 1;
		}
	}

	// As ShaderEditor.provideCompletions.
	private static List<Object> oldEditor(Tokens tokens, String text,
			int cursor) {
		int index = GlslCompletion.tokenBefore(tokens, cursor);
		if (index < 0) {
			return Arrays.asList(BRACKETS, 0);
		}
		if (GlslCompletion.isInComment(tokens, text, index, cursor)) {
			return Arrays.asList(Collections.emptyList(), 0);
		}
		String prefix = text.substring(tokens.start(index), cursor);
		List<String> words = GlslCompletion.words(prefix,
				tokens.inDirective(index));
		if (words.isEmpty() ||
				(words.size() == 1 && words.get(0).equals(prefix))) {
			return Arrays.asList(BRACKETS, 0);
		}
		return Arrays.asList(words, cursor - tokens.start(index));
	}
}
