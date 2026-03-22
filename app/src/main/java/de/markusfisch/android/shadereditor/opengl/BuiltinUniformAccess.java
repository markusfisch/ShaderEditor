package de.markusfisch.android.shadereditor.opengl;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

final class BuiltinUniformAccess {
	@NonNull
	private final GlDevice device;
	@NonNull
	private final GlProgram program;
	@NonNull
	private final Map<String, Boolean> cachedPresence = new HashMap<>();

	BuiltinUniformAccess(
			@NonNull GlDevice device,
			@NonNull GlProgram program) {
		this.device = device;
		this.program = program;
	}

	boolean has(@NonNull String uniformName) {
		Boolean present = cachedPresence.get(uniformName);
		if (present != null) {
			return present;
		}
		boolean resolved = device.hasUniform(program, uniformName);
		cachedPresence.put(uniformName, resolved);
		return resolved;
	}

	boolean hasAny(@NonNull String... uniformNames) {
		for (String uniformName : uniformNames) {
			if (has(uniformName)) {
				return true;
			}
		}
		return false;
	}
}