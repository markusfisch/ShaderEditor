package de.markusfisch.android.shadereditor.opengl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

final class GlProgramBuildResult {
	@Nullable
	private final GlProgram program;
	@NonNull
	private final List<ShaderError> infoLog;

	private GlProgramBuildResult(
			@Nullable GlProgram program,
			@NonNull List<ShaderError> infoLog) {
		this.program = program;
		this.infoLog = infoLog;
	}

	static GlProgramBuildResult success(@NonNull GlProgram program) {
		return new GlProgramBuildResult(program, Collections.emptyList());
	}

	static GlProgramBuildResult failure(@NonNull List<ShaderError> infoLog) {
		return new GlProgramBuildResult(null, infoLog);
	}

	boolean succeeded() {
		return program != null;
	}

	@Nullable
	GlProgram getProgram() {
		return program;
	}

	@NonNull
	List<ShaderError> getInfoLog() {
		return infoLog;
	}
}
