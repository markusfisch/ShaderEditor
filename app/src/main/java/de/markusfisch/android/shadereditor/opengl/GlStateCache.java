package de.markusfisch.android.shadereditor.opengl;

import java.util.Arrays;

final class GlStateCache {
	private final int[] texture2dBindings;
	private final int[] cubemapBindings;
	private final int[] externalTextureBindings;
	int currentProgramId = -1;
	int currentFramebufferId = -1;
	int activeTextureUnit = -1;
	int viewportX = Integer.MIN_VALUE;
	int viewportY = Integer.MIN_VALUE;
	int viewportWidth = Integer.MIN_VALUE;
	int viewportHeight = Integer.MIN_VALUE;

	GlStateCache(int maxTextureUnits) {
		texture2dBindings = new int[maxTextureUnits];
		cubemapBindings = new int[maxTextureUnits];
		externalTextureBindings = new int[maxTextureUnits];
		reset();
	}

	void reset() {
		Arrays.fill(texture2dBindings, 0);
		Arrays.fill(cubemapBindings, 0);
		Arrays.fill(externalTextureBindings, 0);
		currentProgramId = -1;
		currentFramebufferId = -1;
		activeTextureUnit = -1;
		viewportX = Integer.MIN_VALUE;
		viewportY = Integer.MIN_VALUE;
		viewportWidth = Integer.MIN_VALUE;
		viewportHeight = Integer.MIN_VALUE;
	}

	int[] getTextureBindingsForTarget(int target) {
		switch (target) {
			case android.opengl.GLES20.GL_TEXTURE_2D:
				return texture2dBindings;
			case android.opengl.GLES20.GL_TEXTURE_CUBE_MAP:
				return cubemapBindings;
			case android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES:
				return externalTextureBindings;
			default:
				throw new IllegalArgumentException("Unsupported texture target: " + target);
		}
	}

	void clearTextureId(int textureId) {
		clearTextureId(texture2dBindings, textureId);
		clearTextureId(cubemapBindings, textureId);
		clearTextureId(externalTextureBindings, textureId);
	}

	private static void clearTextureId(int[] bindings, int textureId) {
		for (int i = 0; i < bindings.length; ++i) {
			if (bindings[i] == textureId) {
				bindings[i] = 0;
			}
		}
	}
}
