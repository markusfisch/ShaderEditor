package de.markusfisch.android.shadereditor.opengl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * The central engine. It implements the Renderer interface and orchestrates
 * the rendering process by managing Uniform plugins and dispatching draw
 * calls to a ShaderHelper.
 */
public class ShaderEngine implements GLSurfaceView.Renderer {

	private final List<Uniform> uniforms = new ArrayList<>();
	@NonNull
	private final FloatBuffer vertexBuffer;
	private int programId;

	public ShaderEngine() {
		// Initialize vertex buffer for a full-screen quad
		final float[] vertices = {-1, -1, 1, -1, -1, 1, 1, 1};
		vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		vertexBuffer.put(vertices).position(0);
	}

	/**
	 * Registers a uniform plugin to be managed by the engine.
	 */
	public void register(Uniform uniform) {
		uniforms.add(uniform);
	}

	/**
	 * Compiles a new shader and locates all registered uniforms within it.
	 */
	public void setFragmentShader(String fragmentShader) {
		int newProgramId = ShaderHelper.linkProgram(fragmentShader);
		if (newProgramId > 0) {
			programId = newProgramId;
			// After linking, tell all uniforms to find their new locations.
			for (Uniform uniform : uniforms) {
				uniform.locate(programId);
			}
		}
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		if (programId == 0) return;

		// 1. Set the program to be used.
		GLES20.glUseProgram(programId);

		// 2. Dispatch to all plugins to update their data.
		for (Uniform uniform : uniforms) {
			uniform.update();
		}

		// 3. Dispatch to the headless renderer to execute the draw call.
		ShaderHelper.drawQuad(programId, vertexBuffer);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
	}
}