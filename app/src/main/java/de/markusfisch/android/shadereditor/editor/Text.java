package de.markusfisch.android.shadereditor.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// An immutable balanced tree of lines, CodeMirror 6's document tree: a leaf
// holds up to 32 lines. An edit shares every part it did not touch, so a text
// can be kept for undo or read on another thread as it is.
public abstract class Text {
	public static final Text EMPTY = new Leaf(new String[]{""});

	private static final int SHIFT = 5;
	private static final int BRANCH = 1 << SHIFT;
	// The first part continues the last line of the parts before it.
	private static final int OPEN_START = 1;
	// The last line of the last part continues in the parts after it.
	private static final int OPEN_END = 2;

	private final int length;
	private final int lines;
	private final int longestLine;

	private Text(int length, int lines, int longestLine) {
		this.length = length;
		this.lines = lines;
		this.longestLine = longestLine;
	}

	// Lines end at "\r\n", '\r' or '\n', and a no-break space becomes a space.
	public static Text of(CharSequence s) {
		String string = s.toString();
		if (string.indexOf('\r') > -1) {
			string = string.replace("\r\n", "\n").replace('\r', '\n');
		}
		string = string.replace('\u00a0', ' ');
		List<String> lines = new ArrayList<>();
		int start = 0;
		for (int end; (end = string.indexOf('\n', start)) > -1;
				start = end + 1) {
			lines.add(string.substring(start, end));
		}
		lines.add(string.substring(start));
		return ofLines(lines);
	}

	public final int length() {
		return length;
	}

	// At least 1.
	public final int lineCount() {
		return lines;
	}

	public final int longestLineLength() {
		return longestLine;
	}

	// Without its line break.
	public final String line(int n) {
		checkIndex(n, lines - 1);
		return lineAt(n);
	}

	public final int lineStart(int n) {
		checkIndex(n, lines - 1);
		return startOf(n);
	}

	// A line break belongs to the line it ends.
	public final int lineOf(int offset) {
		checkIndex(offset, length);
		return indexAt(offset);
	}

	public final Text replace(int from, int to, Text text) {
		checkRange(from, to);
		return from == to && text.length == 0
				? this
				: splice(from, to, text);
	}

	public final Text slice(int from, int to) {
		checkRange(from, to);
		if (from == to) {
			return EMPTY;
		}
		List<Text> parts = new ArrayList<>();
		decompose(from, to, parts, 0);
		return join(parts);
	}

	public final String substring(int from, int to) {
		checkRange(from, to);
		StringBuilder sb = new StringBuilder(to - from);
		appendTo(sb, from, to);
		return sb.toString();
	}

	@Override
	public final String toString() {
		return substring(0, length);
	}

	abstract String lineAt(int n);

	abstract int startOf(int n);

	abstract int indexAt(int offset);

	// The range may reach beyond this text.
	abstract void decompose(int from, int to, List<Text> target, int open);

	abstract void appendTo(StringBuilder sb, int from, int to);

	abstract void addLines(List<String> target);

	Text splice(int from, int to, Text text) {
		List<Text> parts = new ArrayList<>();
		decompose(0, from, parts, OPEN_END);
		if (text.length > 0) {
			text.decompose(0, text.length, parts, OPEN_START | OPEN_END);
		}
		decompose(to, length, parts, OPEN_START);
		return join(parts);
	}

	private void checkRange(int from, int to) {
		checkIndex(to, length);
		checkIndex(from, to);
	}

	private static void checkIndex(int index, int max) {
		if (index < 0 || index > max) {
			throw new IndexOutOfBoundsException(
					index + " is not in [0, " + max + "]");
		}
	}

	private static Text ofLines(List<String> lines) {
		List<Text> leaves = new ArrayList<>();
		for (int i = 0, size = lines.size(); i < size; i += BRANCH) {
			leaves.add(new Leaf(lines.subList(i, Math.min(size, i + BRANCH))));
		}
		return join(leaves);
	}

	private static Text join(List<Text> parts) {
		if (parts.size() == 1) {
			return parts.get(0);
		}
		int lines = 0;
		for (Text part : parts) {
			lines += part.lines;
		}
		if (lines < BRANCH) {
			List<String> flat = new ArrayList<>(lines);
			for (Text part : parts) {
				part.addLines(flat);
			}
			return new Leaf(flat);
		}
		Joiner joiner = new Joiner(lines);
		for (Text part : parts) {
			joiner.add(part);
		}
		joiner.flush();
		List<Text> chunks = joiner.chunks;
		return chunks.size() == 1 ? chunks.get(0) : Node.of(chunks);
	}

	// Groups parts into chunks of about a 32nd of all lines, and keeps
	// parts of about that size whole.
	private static final class Joiner {
		private final List<Text> chunks = new ArrayList<>();
		private final List<Text> current = new ArrayList<>();
		private final int chunk;
		private int currentLines;

		private Joiner(int lines) {
			chunk = Math.max(BRANCH, lines >> SHIFT);
		}

		private void add(Text part) {
			int last = current.size() - 1;
			if (part.lines > chunk << 1 && part instanceof Node) {
				for (Text child : ((Node) part).children) {
					add(child);
				}
			} else if (part.lines > chunk >> 1 &&
					(currentLines > chunk >> 1 || currentLines == 0)) {
				flush();
				chunks.add(part);
			} else if (part instanceof Leaf && last > -1 &&
					current.get(last) instanceof Leaf &&
					current.get(last).lines + part.lines <= BRANCH) {
				List<String> lines = new ArrayList<>();
				current.get(last).addLines(lines);
				part.addLines(lines);
				current.set(last, new Leaf(lines));
				currentLines += part.lines;
			} else {
				if (currentLines + part.lines > chunk) {
					flush();
				}
				current.add(part);
				currentLines += part.lines;
			}
		}

		private void flush() {
			if (!current.isEmpty()) {
				chunks.add(join(new ArrayList<>(current)));
				current.clear();
				currentLines = 0;
			}
		}
	}

	// Adds the range [from, to] of lines to target; the first piece
	// continues the last line of target.
	private static void appendLines(String[] lines, List<String> target,
			int from, int to) {
		boolean first = true;
		for (int pos = 0, i = 0; i < lines.length && pos <= to; ++i) {
			String line = lines[i];
			int end = pos + line.length();
			if (end >= from) {
				if (end > to) {
					line = line.substring(0, to - pos);
				}
				if (pos < from) {
					line = line.substring(from - pos);
				}
				if (first) {
					int last = target.size() - 1;
					String head = target.get(last);
					// Keep the very string of a line that did not change.
					target.set(last, head.isEmpty() ? line : head.concat(line));
					first = false;
				} else {
					target.add(line);
				}
			}
			pos = end + 1;
		}
	}

	private static final class Leaf extends Text {
		private final String[] text;

		private Leaf(String[] text) {
			super(lengthOf(text), text.length, longestOf(text));
			this.text = text;
		}

		private Leaf(List<String> lines) {
			this(lines.toArray(new String[0]));
		}

		private static int lengthOf(String[] lines) {
			int length = lines.length - 1;
			for (String line : lines) {
				length += line.length();
			}
			return length;
		}

		private static int longestOf(String[] lines) {
			int longest = 0;
			for (String line : lines) {
				longest = Math.max(longest, line.length());
			}
			return longest;
		}

		@Override
		String lineAt(int n) {
			return text[n];
		}

		@Override
		int startOf(int n) {
			int start = 0;
			for (int i = 0; i < n; ++i) {
				start += text[i].length() + 1;
			}
			return start;
		}

		@Override
		int indexAt(int offset) {
			int i = 0;
			for (int end = text[0].length(); end < offset;
					end += text[i].length() + 1) {
				++i;
			}
			return i;
		}

		@Override
		Text splice(int from, int to, Text insert) {
			if (!(insert instanceof Leaf)) {
				return super.splice(from, to, insert);
			}
			List<String> lines = new ArrayList<>(text.length + insert.lines);
			lines.add("");
			appendLines(text, lines, 0, from);
			appendLines(((Leaf) insert).text, lines, 0, insert.length);
			appendLines(text, lines, to, length());
			return ofLines(lines);
		}

		@Override
		void decompose(int from, int to, List<Text> target, int open) {
			boolean continues = (open & OPEN_START) != 0;
			if (!continues && from <= 0 && to >= length()) {
				target.add(this);
				return;
			}
			List<String> lines = new ArrayList<>();
			if (continues) {
				Leaf last = (Leaf) target.remove(target.size() - 1);
				Collections.addAll(lines, last.text);
			} else {
				lines.add("");
			}
			appendLines(text, lines, from, to);
			int size = lines.size();
			int mid = size > BRANCH ? size >> 1 : 0;
			if (mid > 0) {
				target.add(new Leaf(lines.subList(0, mid)));
			}
			target.add(new Leaf(lines.subList(mid, size)));
		}

		@Override
		void appendTo(StringBuilder sb, int from, int to) {
			for (int pos = 0, i = 0; i < text.length && pos <= to; ++i) {
				String line = text[i];
				int end = pos + line.length();
				if (i > 0 && pos > from) {
					sb.append('\n');
				}
				if (from < end && to > pos) {
					sb.append(line, Math.max(0, from - pos),
							Math.min(line.length(), to - pos));
				}
				pos = end + 1;
			}
		}

		@Override
		void addLines(List<String> target) {
			Collections.addAll(target, text);
		}
	}

	private static final class Node extends Text {
		private final Text[] children;

		private Node(Text[] children, int length, int lines) {
			super(length, lines, longestOf(children));
			this.children = children;
		}

		private static int longestOf(Text[] children) {
			int longest = 0;
			for (Text child : children) {
				longest = Math.max(longest, child.longestLine);
			}
			return longest;
		}

		private static Node of(List<Text> children) {
			int length = -1;
			int lines = 0;
			for (Text child : children) {
				length += child.length + 1;
				lines += child.lines;
			}
			return new Node(children.toArray(new Text[0]), length, lines);
		}

		@Override
		String lineAt(int n) {
			int i = 0;
			for (; n >= children[i].lines; ++i) {
				n -= children[i].lines;
			}
			return children[i].lineAt(n);
		}

		@Override
		int startOf(int n) {
			int start = 0;
			int i = 0;
			for (; n >= children[i].lines; ++i) {
				n -= children[i].lines;
				start += children[i].length + 1;
			}
			return start + children[i].startOf(n);
		}

		@Override
		int indexAt(int offset) {
			int line = 0;
			int i = 0;
			for (; offset > children[i].length; ++i) {
				offset -= children[i].length + 1;
				line += children[i].lines;
			}
			return line + children[i].indexAt(offset);
		}

		@Override
		Text splice(int from, int to, Text text) {
			if (text.lines >= lineCount()) {
				return super.splice(from, to, text);
			}
			int i = 0;
			int pos = 0;
			for (; from > pos + children[i].length; ++i) {
				pos += children[i].length + 1;
			}
			Text child = children[i];
			int end = pos + child.length;
			if (to > end) {
				return super.splice(from, to, text);
			}
			// The range lies in one child: replace that child alone
			// while its size stays in proportion.
			Text updated = child.splice(from - pos, to - pos, text);
			int lines = lineCount() - child.lines + updated.lines;
			if (updated.lines >= lines >> (SHIFT - 1) ||
					updated.lines <= lines >> (SHIFT + 1)) {
				return super.splice(pos, end, updated);
			}
			Text[] copy = children.clone();
			copy[i] = updated;
			return new Node(copy, length() - (to - from) + text.length, lines);
		}

		@Override
		void decompose(int from, int to, List<Text> target, int open) {
			for (int i = 0, pos = 0; i < children.length && pos <= to; ++i) {
				Text child = children[i];
				int end = pos + child.length;
				if (from <= end) {
					int childOpen = open & ((pos <= from ? OPEN_START : 0) |
							(end >= to ? OPEN_END : 0));
					if (pos >= from && end <= to && childOpen == 0) {
						target.add(child);
					} else {
						child.decompose(from - pos, to - pos, target,
								childOpen);
					}
				}
				pos = end + 1;
			}
		}

		@Override
		void appendTo(StringBuilder sb, int from, int to) {
			for (int i = 0, pos = 0; i < children.length && pos <= to; ++i) {
				Text child = children[i];
				int end = pos + child.length;
				if (i > 0 && pos > from) {
					sb.append('\n');
				}
				if (from < end && to > pos) {
					child.appendTo(sb, from - pos, to - pos);
				}
				pos = end + 1;
			}
		}

		@Override
		void addLines(List<String> target) {
			for (Text child : children) {
				child.addLines(target);
			}
		}
	}
}
