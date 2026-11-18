package de.markusfisch.android.shadereditor.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Replays a keyboard session recorded on a device: every line is the
// milliseconds since it started, who acted (a connection's number or app), a
// name and its arguments. The keyboard's calls and the app's actions are made
// again to new TextInputs and documents, on a clock set to the time of each
// line, and the answers of the calls, what the keyboard is told and the text
// and selection a connection closed on must be the session's, times aside.
// Lines the recorder kept for itself, changed and selected, are not compared.
final class KeyboardReplay {
	interface Handler {
		void perform(Document document, String name, List<String> args);
	}

	private record Quoted(String text) {
	}

	private static final Set<String> NOT_COMPARED = Set.of("changed",
			"selected");
	private static final Set<String> TOLD = Set.of("updateSelection",
			"updateExtractedText");

	private final String source;
	private final Handler handler;
	private final List<String> expected = new ArrayList<>();
	private final List<Integer> lineNumbers = new ArrayList<>();
	private final List<String> written = new ArrayList<>();
	private final List<TextInput> inputs = new ArrayList<>();
	private final long[] now = {0};
	private Document document;

	private KeyboardReplay(String source, Handler handler) {
		this.source = source;
		this.handler = handler;
	}

	static void replay(String source, List<String> lines, Handler handler) {
		new KeyboardReplay(source, handler).run(lines);
	}

	private void run(List<String> lines) {
		List<List<Object>> made = new ArrayList<>();
		for (int i = 0; i < lines.size(); ++i) {
			String line = lines.get(i);
			if (line.isBlank() || line.startsWith("#")) {
				continue;
			}
			List<Object> tokens = tokenize(line);
			String name = (String) tokens.get(2);
			if ("truncated".equals(name)) {
				break;
			}
			if (NOT_COMPARED.contains(name)) {
				continue;
			}
			if (!"app".equals(tokens.get(1)) && !"open".equals(name)) {
				expected.add(line.substring(line.indexOf(' ') + 1));
				lineNumbers.add(i + 1);
			}
			if (!TOLD.contains(name)) {
				made.add(tokens);
			}
		}
		for (List<Object> tokens : made) {
			now[0] = Long.parseLong((String) tokens.get(0));
			apply(tokens);
			compare();
		}
		if (written.size() < expected.size()) {
			fail(at(written.size()) + "expected\n  " +
					expected.get(written.size()) +
					"\nbut the editor wrote nothing more");
		}
	}

	private void apply(List<Object> t) {
		String who = (String) t.get(1);
		String name = (String) t.get(2);
		if ("app".equals(who)) {
			perform(name, t.subList(3, t.size()));
			return;
		}
		int connection = Integer.parseInt(who);
		if ("open".equals(name)) {
			String text = string(t, 5);
			if (text != null) {
				document = new Document(Text.of(text), () -> now[0]);
				document.select(number(t, 3), number(t, 4));
			}
			inputs.add(new TextInput(document, new TextInput.Listener() {
				@Override
				public void onUpdateSelection(int selStart, int selEnd,
						int composingStart, int composingEnd) {
					write(connection, "updateSelection", selStart, selEnd,
							composingStart, composingEnd);
				}

				@Override
				public void onUpdateExtractedText(int token,
						TextInput.Window window) {
					write(connection, "updateExtractedText", token, window);
				}
			}));
			assertEquals(source + ": connection", inputs.size(), connection);
			return;
		}
		TextInput input = inputs.get(connection - 1);
		// The answer comes before what the keyboard is told, the close
		// after it.
		int at = written.size();
		Object answer = call(input, name, t);
		List<Object> args = new ArrayList<>(t.subList(3, t.size()));
		int arrow = args.indexOf("->");
		if (arrow >= 0) {
			args.subList(arrow, args.size()).clear();
		}
		if ("close".equals(name)) {
			Document.Selection selection = document.selection();
			written.add(connection + " close -> " + selection.anchor() +
					" " + selection.head() + " " +
					quote(document.text().toString()));
			return;
		}
		StringBuilder line = new StringBuilder();
		line.append(connection).append(' ').append(name);
		for (Object arg : args) {
			line.append(' ').append(arg instanceof Quoted q
					? quote(q.text())
					: String.valueOf(arg));
		}
		if ("endBatchEdit".equals(name)) {
			// It tells the other connections before it answers.
			while (at < written.size() &&
					!written.get(at).startsWith(connection + " ")) {
				++at;
			}
		}
		written.add(at, line.append(" -> ").append(value(answer)).toString());
	}

	private static Object call(TextInput input, String name, List<Object> t) {
		return switch (name) {
			case "close" -> {
				input.closeConnection();
				yield null;
			}
			case "commitText" -> input.commitText(string(t, 3), number(t, 4));
			case "setComposingText" ->
					input.setComposingText(string(t, 3), number(t, 4));
			case "setComposingRegion" ->
					input.setComposingRegion(number(t, 3), number(t, 4));
			case "finishComposingText" -> input.finishComposingText();
			case "deleteSurroundingText" ->
					input.deleteSurroundingText(number(t, 3), number(t, 4));
			case "deleteSurroundingTextInCodePoints" ->
					input.deleteSurroundingTextInCodePoints(number(t, 3),
							number(t, 4));
			case "setSelection" ->
					input.setSelection(number(t, 3), number(t, 4));
			case "getTextBeforeCursor" ->
					input.getTextBeforeCursor(number(t, 3));
			case "getTextAfterCursor" ->
					input.getTextAfterCursor(number(t, 3));
			case "getSelectedText" -> input.getSelectedText();
			case "getSurroundingText" ->
					input.getSurroundingText(number(t, 3), number(t, 4));
			case "getExtractedText" -> input.getExtractedText(number(t, 3),
					Boolean.parseBoolean((String) t.get(4)));
			case "beginBatchEdit" -> input.beginBatchEdit();
			case "endBatchEdit" -> input.endBatchEdit();
			default -> throw new AssertionError("unknown call " + name);
		};
	}

	private void perform(String name, List<Object> tokens) {
		List<String> args = new ArrayList<>();
		for (Object token : tokens) {
			args.add(token instanceof Quoted q ? q.text() : (String) token);
		}
		switch (name) {
			case "undo" -> document.undo();
			case "redo" -> document.redo();
			case "select" -> document.select(Integer.parseInt(args.get(0)),
					Integer.parseInt(args.get(1)));
			default -> handler.perform(document, name, args);
		}
	}

	private void write(int connection, String name, Object... args) {
		StringBuilder line = new StringBuilder();
		line.append(connection).append(' ').append(name);
		for (Object arg : args) {
			line.append(' ').append(value(arg));
		}
		written.add(line.toString());
	}

	private void compare() {
		for (int i = 0; i < written.size(); ++i) {
			if (i >= expected.size()) {
				fail(source + ": the editor wrote more:\n  " + written.get(i));
			}
			if (!written.get(i).equals(expected.get(i))) {
				fail(at(i) + "expected\n  " + expected.get(i) +
						"\nbut the editor wrote\n  " + written.get(i));
			}
		}
	}

	private String at(int index) {
		return source + ":" + lineNumbers.get(index) + ": ";
	}

	static List<Object> tokenize(String line) {
		List<Object> tokens = new ArrayList<>();
		int i = 0;
		while (i < line.length()) {
			if (line.charAt(i) == ' ') {
				++i;
			} else if (line.charAt(i) == '"') {
				StringBuilder sb = new StringBuilder();
				for (++i; line.charAt(i) != '"'; ++i) {
					char c = line.charAt(i);
					if (c == '\\') {
						c = line.charAt(++i);
						switch (c) {
							case 'n' -> c = '\n';
							case 't' -> c = '\t';
							case 'u' -> {
								c = (char) Integer.parseInt(
										line.substring(i + 1, i + 5), 16);
								i += 4;
							}
							default -> {
							}
						}
					}
					sb.append(c);
				}
				++i;
				tokens.add(new Quoted(sb.toString()));
			} else {
				int end = line.indexOf(' ', i);
				end = end < 0 ? line.length() : end;
				String word = line.substring(i, end);
				tokens.add("null".equals(word) ? null : word);
				i = end;
			}
		}
		return tokens;
	}

	private static String value(Object value) {
		if (value instanceof CharSequence s) {
			return quote(s);
		} else if (value instanceof TextInput.Window w) {
			return w.offset() + " " + w.selectionStart() + " " +
					w.selectionEnd() + " " + quote(w.text());
		}
		return String.valueOf(value);
	}

	private static String quote(CharSequence s) {
		StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
		for (int i = 0, length = s.length(); i < length; ++i) {
			char c = s.charAt(i);
			if (c == '"' || c == '\\') {
				sb.append('\\').append(c);
			} else if (c == '\n') {
				sb.append("\\n");
			} else if (c == '\t') {
				sb.append("\\t");
			} else if (c < ' ' || c > '~') {
				sb.append("\\u");
				for (int shift = 12; shift >= 0; shift -= 4) {
					sb.append(Character.forDigit((c >> shift) & 15, 16));
				}
			} else {
				sb.append(c);
			}
		}
		return sb.append('"').toString();
	}

	private static String string(List<Object> tokens, int index) {
		Object token = tokens.get(index);
		return token == null ? null : ((Quoted) token).text();
	}

	private static int number(List<Object> tokens, int index) {
		return Integer.parseInt((String) tokens.get(index));
	}
}
