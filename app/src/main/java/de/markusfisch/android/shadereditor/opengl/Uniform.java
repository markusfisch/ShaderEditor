package de.markusfisch.android.shadereditor.opengl;

/**
 * The contract for a pluggable, self-managing shader uniform.
 * Each implementation is responsible for its own GL calls.
 */
public interface Uniform {
	/**
	 * The uniform's name in GLSL (e.g., "u_time").
	 */
	String getName();

	/**
	 * Finds the uniform's location in a new shader program.
	 */
	void locate(int programId);

	/**
	 * Updates the uniform's value on each frame by making a glUniform* call.
	 */
	void update();
}