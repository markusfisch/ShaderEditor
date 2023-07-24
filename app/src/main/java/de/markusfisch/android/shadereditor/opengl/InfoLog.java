package de.markusfisch.android.shadereditor.opengl;

public class InfoLog {
	private static String message;
	private static int errorLine;
	private static int silentlyAddedExtraLines;

	public static String getMessage() {
		return message;
	}

	public static int getErrorLine() {
		return errorLine - silentlyAddedExtraLines;
	}

	public static void resetSilentlyAddedExtraLines() {
		silentlyAddedExtraLines = 0;
	}

	public static void addSilentlyAddedExtraLine() {
		++silentlyAddedExtraLines;
	}

	public static void parse(String infoLog) {
		message = null;
		errorLine = 0;

		if (infoLog == null) {
			return;
		}

		int from;
		if ((from = infoLog.indexOf("ERROR: 0:")) > -1) {
			from += 9;
		} else if ((from = infoLog.indexOf("0:")) > -1) {
			from += 2;
		}

		if (from > -1) {
			int to;
			if ((to = infoLog.indexOf(":", from)) > -1) {
				try {
					errorLine = Integer.parseInt(
							infoLog.substring(from, to).trim());
				} catch (NumberFormatException | NullPointerException e) {
					// Can't do anything about it.
				}

				from = ++to;
			}

			if ((to = infoLog.indexOf("\n", from)) < 0) {
				to = infoLog.length();
			}

			infoLog = infoLog.substring(from, to).trim();
		}

		message = infoLog;
	}
}
