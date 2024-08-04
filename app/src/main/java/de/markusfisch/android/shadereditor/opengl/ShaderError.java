package de.markusfisch.android.shadereditor.opengl;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShaderError {
	private static final Pattern ERROR_PATTERN = Pattern.compile(
			"^.*(\\d+):(\\d+):\\s+(.*)$");
	private static int silentlyAddedExtraLines;

	private final int sourceStringNumber;
	private final int errorLine;
	@NonNull
	private final String message;

	private ShaderError(int sourceStringNumber, int errorLine, @NonNull String message) {
		this.sourceStringNumber = sourceStringNumber;
		this.errorLine = errorLine;
		this.message = message;
	}

	public static ShaderError createGeneral(@NonNull String message) {
		return new ShaderError(0, -1, message);
	}

	public int getSourceStringNumber() {
		return sourceStringNumber;
	}
	public boolean hasLine() {
		return errorLine > 0;
	}

	public int getLine() {
		return errorLine - silentlyAddedExtraLines;
	}

	@NonNull
	public String getMessage() {
		return message;
	}

	@NonNull
	public static List<ShaderError> parseAll(@NonNull String infoLog) {
		String[] messages = infoLog.split("\n");
		List<ShaderError> shaderErrors = new ArrayList<>(messages.length);
		for (String message : messages) {
			shaderErrors.add(parse(message));
		}
		return shaderErrors;
	}

	@NonNull
	public static ShaderError parse(@NonNull String message) {
		int sourceStringNumber = 0;
		int errorLine = -1;
		String infoLog = message;
		Matcher matcher = ERROR_PATTERN.matcher(message);
		if (!matcher.matches()) {
			return new ShaderError(sourceStringNumber, errorLine, infoLog);
		}
		sourceStringNumber = Integer.parseInt(matcher.group(1));
		errorLine = Integer.parseInt(matcher.group(2));
		infoLog = Objects.requireNonNull(matcher.group(3));
		return new ShaderError(sourceStringNumber, errorLine, infoLog);

	}

	public static void resetSilentlyAddedExtraLines() {
		silentlyAddedExtraLines = 0;
	}

	public static void addSilentlyAddedExtraLine() {
		++silentlyAddedExtraLines;
	}

	@Override
	public final boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ShaderError)) return false;

		ShaderError that = (ShaderError) o;
		return sourceStringNumber == that.sourceStringNumber && errorLine == that.errorLine && message.equals(that.message);
	}

	@Override
	public int hashCode() {
		int result = sourceStringNumber;
		result = 31 * result + errorLine;
		result = 31 * result + message.hashCode();
		return result;
	}
}
