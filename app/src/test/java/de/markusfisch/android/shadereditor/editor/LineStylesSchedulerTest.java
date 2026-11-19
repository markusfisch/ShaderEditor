package de.markusfisch.android.shadereditor.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import de.markusfisch.android.shadereditor.syntax.GlslSyntax;
import de.markusfisch.android.shadereditor.syntax.Style;
import de.markusfisch.android.shadereditor.syntax.Syntax;

public class LineStylesSchedulerTest {
	private static final Syntax GLSL = GlslSyntax.INSTANCE;

	@Test
	public void makesEveryLineCurrentInBoundedSteps() {
		Document document = new Document(Text.of(
				"float a;\n".repeat(20_000)));
		LineStyles styles = new LineStyles(document, GLSL, 200);
		Main main = new Main();
		Background background = new Background();
		int[] applied = new int[1];
		new LineStylesScheduler(document, styles, background, main,
				() -> ++applied[0]);
		settle(main, background);
		assertNull(styles.continuation());
		document.replace(0, 0, Text.of("/*"));
		assertNotNull(styles.continuation());
		applied[0] = 0;
		settle(main, background);
		assertNull(styles.continuation());
		assertEquals(Style.COMMENT, styles.runs(19_999).style(0));
		// At most 4096 states per call, one call per message.
		assertTrue(applied[0] >= 20_000 / 4096);
	}

	@Test
	public void runsNothingInTheBackgroundForOrdinaryTyping() {
		Document document = new Document(Text.of(
				"float a;\n".repeat(20_000)));
		LineStyles styles = new LineStyles(document, GLSL, 200);
		Main main = new Main();
		Background background = new Background();
		new LineStylesScheduler(document, styles, background, main, () -> {});
		settle(main, background);
		int offset = document.text().lineStart(10_000);
		for (char c : "float x = 1.0; // y\n".toCharArray()) {
			document.type(offset, offset, Text.of(String.valueOf(c)));
			++offset;
			assertEquals(List.of(), background.tasks);
		}
	}

	@Test
	public void appliesNothingAfterClose() {
		Document document = new Document(Text.of(
				"float a;\n".repeat(1000)));
		LineStyles styles = new LineStyles(document, GLSL, 200);
		Main main = new Main();
		Background background = new Background();
		List<String> applied = new ArrayList<>();
		LineStylesScheduler scheduler = new LineStylesScheduler(document,
				styles, background, main, () -> applied.add("applied"));
		scheduler.close();
		settle(main, background);
		document.replace(0, 0, Text.of("x"));
		settle(main, background);
		assertEquals(List.of(), applied);
		assertNotNull(styles.continuation());
	}

	private static void settle(Main main, Background background) {
		while (!main.tasks.isEmpty() || !background.tasks.isEmpty()) {
			background.runAll();
			main.runAll();
		}
	}

	private static final class Main implements Executor {
		private final List<Runnable> tasks = new ArrayList<>();

		@Override
		public void execute(Runnable task) {
			tasks.add(task);
		}

		// One message at a time, as a looper runs them.
		void runAll() {
			while (!tasks.isEmpty()) {
				tasks.remove(0).run();
			}
		}
	}

	private static final class Background implements Executor {
		private final List<Runnable> tasks = new ArrayList<>();

		@Override
		public void execute(Runnable task) {
			tasks.add(task);
		}

		void runAll() {
			List<Runnable> run = new ArrayList<>(tasks);
			tasks.clear();
			run.forEach(Runnable::run);
		}
	}
}
