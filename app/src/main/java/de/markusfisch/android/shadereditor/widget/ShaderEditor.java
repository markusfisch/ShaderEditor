package de.markusfisch.android.shadereditor.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LineHeightSpan;
import android.text.style.ReplacementSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.app.ShaderEditorApp;
import de.markusfisch.android.shadereditor.highlighter.Highlight;
import de.markusfisch.android.shadereditor.highlighter.Lexer;
import de.markusfisch.android.shadereditor.highlighter.Token;

public class ShaderEditor extends LineNumberEditText {
	@FunctionalInterface
	public interface OnTextChangedListener {
		void onTextChanged(String text);
	}

	@FunctionalInterface
	public interface CodeCompletionListener {
		void onCodeCompletions(@NonNull List<String> completions, int position);
	}

	private static final Pattern PATTERN_TRAILING_WHITE_SPACE = Pattern.compile(
			"[\\t ]+$",
			Pattern.MULTILINE);
	private static final Pattern PATTERN_INSERT_UNIFORM = Pattern.compile(
			"^([ \t]*uniform.+)$",
			Pattern.MULTILINE);
	private static final Pattern PATTERN_ENDIF = Pattern.compile(
			"(#endif)\\b");
	private static final Pattern PATTERN_SHADER_TOY = Pattern.compile(
			".*void\\s+mainImage\\s*\\(.*");
	private static final Pattern PATTERN_MAIN = Pattern.compile(
			".*void\\s+main\\s*\\(.*");
	private static final Pattern PATTERN_NO_BREAK_SPACE = Pattern.compile(
			"\\xA0");
	private static final ArrayList<String> DEFAULT_COMPLETIONS = new ArrayList<>();

	static {
		DEFAULT_COMPLETIONS.add("{");
		DEFAULT_COMPLETIONS.add("}");
		DEFAULT_COMPLETIONS.add("(");
		DEFAULT_COMPLETIONS.add(")");
		DEFAULT_COMPLETIONS.add("[");
		DEFAULT_COMPLETIONS.add("]");
	}

	private final Handler updateHandler = new Handler();
	private final Runnable updateRunnable = new Runnable() {
		@Override
		public void run() {
			Editable e = getText();

			if (e != null) {
				if (onTextChangedListener != null) {
					onTextChangedListener.onTextChanged(e.toString());
				}

				highlightWithoutChange(e);
			}

		}
	};
	private final int[] colors = new int[Highlight.values().length];

	private OnTextChangedListener onTextChangedListener;
	@Nullable
	private CodeCompletionListener codeCompletionListener;
	private int updateDelay = 1000;
	private int errorLine = 0;
	private boolean dirty = false;
	private boolean modified = true;
	private int colorError;
	private int textColor;
	private int tabWidthInCharacters = 0;
	private int tabWidth = 0;
	private List<Token> tokens = new ArrayList<>();
	private int revision = 0;
	private final TokenListUpdater tokenListUpdater =
			new TokenListUpdater(this::provideCompletions);
	private boolean editing = false;

	public ShaderEditor(Context context) {
		super(context);
		init(context);
	}

	public ShaderEditor(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public void setOnTextChangedListener(OnTextChangedListener listener) {
		onTextChangedListener = listener;
	}

	public void setOnCompletionsListener(@NonNull CodeCompletionListener listener) {
		codeCompletionListener = listener;
	}

	public void setUpdateDelay(int ms) {
		updateDelay = ms;
	}

	public void setTabWidth(int characters) {
		if (tabWidthInCharacters == characters) {
			return;
		}

		tabWidthInCharacters = characters;
		tabWidth = Math.round(getPaint().measureText("m") * characters);
	}

	public boolean hasErrorLine() {
		return errorLine > 0;
	}

	public void setErrorLine(int line) {
		errorLine = line;
	}

	public void updateHighlighting() {
		Editable text = getText();
		if (text != null) {
			highlightWithoutChange(text);
		}
	}

	private void clearError(@Nullable Spannable e) {
		if (e == null) return;
		clearSpans(e, 0, e.length(), BackgroundColorSpan.class);
	}

	private void highlightError(int errorLine) {
		Spannable e = getText();
		clearError(e);
		if (e == null || e.length() == 0 || errorLine <= 0) {
			return;
		}
		int line = errorLine - 1;
		Layout layout = getLayout();
		e.setSpan(
				new BackgroundColorSpan(colorError),
				layout.getLineStart(line),
				layout.getLineEnd(line),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	public void updateErrorHighlighting() {
		Spannable e = getText();
		clearError(e);
		if (errorLine > 0) {
			highlightError(errorLine);
		}
	}

	public boolean isModified() {
		return dirty;
	}

	public void setTextHighlighted(CharSequence text) {
		if (text == null) {
			text = "";
		}

		cancelUpdate();

		errorLine = 0;
		dirty = false;

		modified = false;

		tokenListUpdater.update(text, ++revision); // `setText` can't be overridden
		setText(highlight(new SpannableStringBuilder(text), true));
		modified = true;

		Editable e = getText();
		if (e != null && onTextChangedListener != null) {
			onTextChangedListener.onTextChanged(e.toString());
		}
	}

	public String getCleanText() {
		return PATTERN_TRAILING_WHITE_SPACE
				.matcher(getText())
				.replaceAll("");
	}

	public void insert(@NonNull CharSequence text) {
		int start = getSelectionStart();
		int end = getSelectionEnd();
		Editable e = getText();
		if (e == null) {
			return;
		}
		e.replace(Math.min(start, end),
				Math.max(start, end),
				text,
				0,
				text.length());
	}

	public void addUniform(String statement) {
		if (statement == null) {
			return;
		}

		Editable e = getText();
		removeUniform(e, statement);

		Matcher m = PATTERN_INSERT_UNIFORM.matcher(e);
		int start = -1;

		while (m.find()) {
			start = m.end();
		}

		if (start > -1) {
			// Add line break before statement because it's
			// inserted before the last line-break.
			statement = "\n" + statement;
		} else {
			// Add a line break after statement if there's no
			// uniform already.
			statement += "\n";

			// Add an empty line between the last #endif
			// and the now following uniform.
			if ((start = endIndexOfLastEndIf(e)) > -1) {
				statement = "\n" + statement;
			}

			// Move index past line break or to the start
			// of the text when no #endif was found.
			++start;
		}

		if (e != null) {
			e.insert(start, statement);
		}
	}

	/**
	 * Define functions required for AutoFill API
	 */
	@RequiresApi(api = Build.VERSION_CODES.O)
	@Override
	public int getAutofillType() {
		return AUTOFILL_TYPE_NONE;
	}

	@Nullable
	@Override
	public InputConnection onCreateInputConnection(@NonNull EditorInfo outAttrs) {
		InputConnection connection = super.onCreateInputConnection(outAttrs);
		if (ShaderEditorApp.preferences.hideNativeSuggestions()) {
			outAttrs.inputType = InputType.TYPE_NULL;
		}
		return connection;
	}

	@Override
	protected void onSelectionChanged(int selStart, int selEnd) {
		super.onSelectionChanged(selStart, selEnd);
		Editable text = getText();
		CodeCompletionListener listener = codeCompletionListener;
		if (text == null || listener == null) {
			return;
		}
		int start = getSelectionStart();
		int end = getSelectionEnd();
		if (start != end) {
			return;
		}
		if (!editing && tokenListUpdater.isDone()) {
			provideCompletions(tokens, text);
		}
	}

	private void removeUniform(Editable e, String statement) {
		if (statement == null) {
			return;
		}

		String regex = "^(" + statement.replace(" ", "[ \\t]+");
		int p = regex.indexOf(";");
		if (p > -1) {
			regex = regex.substring(0, p);
		}
		regex += ".*\\n)$";

		Matcher m = Pattern.compile(regex, Pattern.MULTILINE).matcher(e);
		if (m.find()) {
			e.delete(m.start(), m.end());
		}
	}

	private int endIndexOfLastEndIf(Editable e) {
		Matcher m = PATTERN_ENDIF.matcher(e);
		int idx = -1;

		while (m.find()) {
			idx = m.end();
		}

		return idx;
	}

	private void init(@NonNull Context context) {
		// Setting this through XML does not work
		setHorizontallyScrolling(true);

		setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> {
			if (modified &&
					end - start == 1 &&
					start < source.length() &&
					dstart < dest.length()) {
				char c = source.charAt(start);

				if (c == '\n') {
					return autoIndent(source, dest, dstart, dend);
				}
			}

			return source;
		}});

		addTextChangedListener(new TextWatcher() {
			private int start = 0;
			private int count = 0;

			@Override
			public void onTextChanged(
					CharSequence s,
					int start,
					int before,
					int count) {
				this.start = start;
				this.count = count;
			}

			@Override
			public void beforeTextChanged(
					CharSequence s,
					int start,
					int count,
					int after) {
				editing = true;
			}

			@Override
			public void afterTextChanged(Editable e) {
				cancelUpdate();
				convertTabs(e, start, count);

				String converted = convertShaderToySource(e.toString());
				if (converted != null) {
					setTextHighlighted(converted);
				}

				if (!modified) {
					editing = false;
					return;
				}

				editing = false;
				dirty = true;
				tokenListUpdater.update(e, ++revision);
				updateHandler.postDelayed(updateRunnable, updateDelay);
			}
		});

		setSyntaxColors(context);
		setUpdateDelay(ShaderEditorApp.preferences.getUpdateDelay());
		setTabWidth(ShaderEditorApp.preferences.getTabWidth());

		setOnKeyListener((v, keyCode, event) -> {
			if (ShaderEditorApp.preferences.useTabForIndent() &&
					event.getAction() == KeyEvent.ACTION_DOWN &&
					keyCode == KeyEvent.KEYCODE_TAB) {
				// Insert a tab character instead of doing focus
				// navigation.
				insert("\t");
				return true;
			}
			return false;
		});
	}

	private void setSyntaxColors(Context context) {
		for (Highlight highlight : Highlight.values()) {
			colors[highlight.ordinal()] = ContextCompat.getColor(context, highlight.id());
		}
		textColor = ContextCompat.getColor(context, R.color.editor_text);
		colorError = ContextCompat.getColor(
				context,
				R.color.syntax_error);
	}

	private void cancelUpdate() {
		updateHandler.removeCallbacks(updateRunnable);
	}

	private void highlightWithoutChange(@NonNull Editable e) {
		modified = false;
		highlight(e, false);
		modified = true;
	}

	private Editable highlight(@NonNull Editable e, boolean complete) {
		int length = e.length();

		clearError(e);

		if (length == 0) {
			tokens = new ArrayList<>();
			return e;
		}

		// When pasting text from other apps, e.g. Github Mobile code
		// viewer, the text can be tainted with No-Break Space (U+00A0)
		// characters.
		for (Matcher m = PATTERN_NO_BREAK_SPACE.matcher(e); m.find(); ) {
			e.replace(m.start(), m.end(), " ");
		}

		if (ShaderEditorApp.preferences.disableHighlighting() &&
				length > 4096) {
			clearSpans(e, 0, length, ForegroundColorSpan.class);
			return e;
		}
		List<Token> newTokens = tokenListUpdater.ensureUpdated(e, revision);

		if (complete) {
			clearSpans(e, 0, length, ForegroundColorSpan.class);
			for (Token token : newTokens) {
				@ColorInt int color = colors[Highlight.from(token.type()).ordinal()];
				if (color != textColor) {
					e.setSpan(
							new ForegroundColorSpan(color),
							token.startOffset(), token.endOffset(),
							Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}
		} else {
			Lexer.Diff diff = Lexer.diff(tokens, newTokens);
			if (diff.start <= diff.deleteEnd) {
				int startOffset = newTokens.get(diff.start).startOffset();
				int endOffset = newTokens.get(diff.insertEnd).endOffset();
				clearSpans(e, startOffset, endOffset, ForegroundColorSpan.class);
			}
			for (int i = diff.start; i <= diff.insertEnd; ++i) {
				Token token = newTokens.get(i);
				e.setSpan(
						new ForegroundColorSpan(colors[Highlight.from(token.type()).ordinal()]),
						token.startOffset(), token.endOffset(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}

		tokens = newTokens;

		return e;
	}

	private void provideCompletions(@NonNull List<Token> tokens, @NonNull CharSequence text) {
		CodeCompletionListener listener = codeCompletionListener;
		if (listener == null) {
			return;
		}
		int start = getSelectionStart();
		Token tok = Lexer.findToken(tokens, start);
		if (tok == null || tok.endOffset() >= text.length()) {
			listener.onCodeCompletions(DEFAULT_COMPLETIONS, 0);
			return;
		}
		int positionInToken = start - tok.startOffset();
		listener.onCodeCompletions(
				Lexer.completeKeyword(
						Lexer.tokenSource(tok, text).subSequence(0, positionInToken).toString(),
						tok.category()
				),
				positionInToken);
	}

	private static <T> void clearSpans(Spannable e, int start, int end, Class<T> clazz) {
		// Remove foreground color spans.
		T[] spans = e.getSpans(
				start,
				end,
				clazz);
		for (T span : spans) {
			e.removeSpan(span);
		}
	}

	private CharSequence autoIndent(
			CharSequence source,
			Spanned dest,
			int dstart,
			int dend) {
		String indent = "";
		int istart = dstart - 1;

		// Find start of this line.
		boolean dataBefore = false;
		int pt = 0;

		for (; istart > -1; --istart) {
			char c = dest.charAt(istart);

			if (c == '\n') {
				break;
			}

			if (c != ' ' && c != '\t') {
				if (!dataBefore) {
					// Indent always after those characters.
					if (c == '{' ||
							c == '+' ||
							c == '-' ||
							c == '*' ||
							c == '/' ||
							c == '%' ||
							c == '^' ||
							c == '=') {
						--pt;
					}

					dataBefore = true;
				}

				// Parenthesis counter.
				if (c == '(') {
					--pt;
				} else if (c == ')') {
					++pt;
				}
			}
		}

		// Copy indent of this line into the next.
		if (istart > -1) {
			char charAtCursor = dest.charAt(dstart);
			int iend;

			for (iend = ++istart; iend < dend; ++iend) {
				char c = dest.charAt(iend);

				// Auto expand comments.
				if (charAtCursor != '\n' &&
						c == '/' &&
						iend + 1 < dend &&
						dest.charAt(iend) == c) {
					iend += 2;
					break;
				}

				if (c != ' ' && c != '\t') {
					break;
				}
			}

			indent += dest.subSequence(istart, iend);
		}

		// Add new indent.
		if (pt < 0) {
			indent += "\t";
		}

		// Append white space of previous line and new indent.
		return source + indent;
	}

	private void convertTabs(Editable e, int start, int count) {
		clearSpans(e, start, count, TabWidthSpan.class);
		if (tabWidth < 1) {
			return;
		}

		String s = e.toString();

		for (int stop = start + count;
				(start = s.indexOf("\t", start)) > -1 && start < stop;
				++start) {
			e.setSpan(
					new TabWidthSpan(tabWidth),
					start,
					start + 1,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
	}

	private static String convertShaderToySource(String src) {
		if (!PATTERN_SHADER_TOY.matcher(src).find() ||
				PATTERN_MAIN.matcher(src).find()) {
			return null;
		}
		// Only include and translate uniforms that have an equivalent.
		return "#ifdef GL_FRAGMENT_PRECISION_HIGH\n" +
				"precision highp float;\n" +
				"#else\n" +
				"precision mediump float;\n" +
				"#endif\n\n" +
				"uniform vec2 resolution;\n" +
				"uniform float time;\n" +
				"uniform vec4 mouse;\n" +
				"uniform vec4 date;\n\n" +
				src.replaceAll("iResolution", "resolution")
						.replaceAll("iGlobalTime", "time")
						.replaceAll("iMouse", "mouse")
						.replaceAll("iDate", "date") +
				"\n\nvoid main() {\n" +
				"\tvec4 fragment_color;\n" +
				"\tmainImage(fragment_color, gl_FragCoord.xy);\n" +
				"\tgl_FragColor = fragment_color;\n" +
				"}\n";
	}

	private static class TabWidthSpan
			extends ReplacementSpan
			implements LineHeightSpan.WithDensity {
		private final int width;

		private TabWidthSpan(int width) {
			this.width = width;
		}

		@Override
		public int getSize(
				@NonNull Paint paint,
				CharSequence text,
				int start,
				int end,
				Paint.FontMetricsInt fm) {
			return width;
		}

		@Override
		public void draw(
				@NonNull Canvas canvas,
				CharSequence text,
				int start,
				int end,
				float x,
				int top,
				int y,
				int bottom,
				@NonNull Paint paint) {
		}

		@Override
		public void chooseHeight(CharSequence text, int start, int end,
				int spanstartv, int lineHeight,
				Paint.FontMetricsInt fm, TextPaint paint) {
			paint.getFontMetricsInt(fm);
		}

		@Override
		public void chooseHeight(CharSequence text, int start, int end,
				int spanstartv, int lineHeight, Paint.FontMetricsInt fm) {
		}
	}

	@FunctionalInterface
	interface OnTokenized {
		void onTokens(@NonNull List<Token> tokens, @NonNull CharSequence text);
	}

	static class TokenizeCalculation implements Callable<List<Token>> {
		private final String text;
		@NonNull
		private final OnTokenized onTokenized;

		public TokenizeCalculation(@NonNull String text, @NonNull OnTokenized onTokenized) {
			this.text = text;
			this.onTokenized = onTokenized;
		}

		@Override
		public List<Token> call() {
			Lexer lexer = new Lexer(text);
			List<Token> tokens = new ArrayList<>();
			for (Token token : lexer) {
				tokens.add(token);
				if (Thread.currentThread().isInterrupted()) {
					break;
				}
			}
			onTokenized.onTokens(tokens, text);
			return tokens;
		}
	}

	static class TokenListUpdater {
		@NonNull
		private final OnTokenized onTokenized;
		@NonNull
		private final ExecutorService executor = Executors.newSingleThreadExecutor();
		@Nullable
		private FutureTask<List<Token>> task;
		private int revision = -1;

		public TokenListUpdater(@NonNull OnTokenized onTokenized) {
			this.onTokenized = onTokenized;
		}

		@NonNull
		public List<Token> ensureUpdated(@NonNull CharSequence text, int revision) {
			if (task != null && revision == this.revision) {
				try {
					return task.get();
				} catch (ExecutionException | InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			update(text, revision);
			try {
				return task.get();
			} catch (ExecutionException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		public void update(@NonNull CharSequence text, int revision) {
			if (revision == this.revision) {
				Log.d("TEST", "Same revision: " + revision);
				return;
			} else if (task != null) {
				task.cancel(false);
			}
			Log.d("TEST", "Different revision: " + revision + " != " + this.revision);

			this.revision = revision;
			task = new FutureTask<>(new TokenizeCalculation(text.toString(), onTokenized));
			executor.submit(task);
		}

		public boolean isDone() {
			return task != null && task.isDone();
		}
	}
}
