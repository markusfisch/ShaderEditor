package de.markusfisch.android.shadereditor.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

final class ShaderRenderPipeline {
	private static final int THUMBNAIL_WIDTH = 144;
	private static final int THUMBNAIL_HEIGHT = 144;
	private static final String SURFACE_FRAME = "frame";

	private final float[] thumbnailResolution =
			new float[]{THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT};
	private final float[] drawResolution = new float[2];
	private final TextureParameters thumbnailTextureParameters =
			new TextureParameters(
					GLES20.GL_LINEAR,
					GLES20.GL_LINEAR,
					GLES20.GL_CLAMP_TO_EDGE,
					GLES20.GL_CLAMP_TO_EDGE);
	@NonNull
	private final GlDevice device;
	@NonNull
	private final Mesh fullScreenQuadMesh;
	private final GlFramebuffer[] framebuffers = new GlFramebuffer[2];
	private final GlTexture2D[] targetTextures = new GlTexture2D[2];
	private int frontTarget;
	private int backTarget = 1;
	@Nullable
	private GlFramebuffer thumbnailFramebuffer;
	@Nullable
	private GlTexture2D thumbnailTexture;

	ShaderRenderPipeline(@NonNull GlDevice device, @NonNull Mesh fullScreenQuadMesh) {
		this.device = device;
		this.fullScreenQuadMesh = fullScreenQuadMesh;
	}

	@NonNull
	List<ShaderError> createContextResources() {
		ArrayList<ShaderError> errors = new ArrayList<>();
		thumbnailTexture = device.createTexture2D();
		device.applyTextureParameters(thumbnailTexture, thumbnailTextureParameters);
		device.allocateTexture2D(
				thumbnailTexture,
				THUMBNAIL_WIDTH,
				THUMBNAIL_HEIGHT);
		thumbnailFramebuffer = device.createFramebuffer();
		device.attachColor(thumbnailFramebuffer, thumbnailTexture);
		int status = device.checkFramebufferStatus(thumbnailFramebuffer);
		if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
			errors.add(ShaderError.createGeneral(
					"Thumbnail framebuffer incomplete: 0x" +
							Integer.toHexString(status)));
		}
		device.bindFramebuffer(null);
		return errors;
	}

	void discardContextResources() {
		thumbnailFramebuffer = null;
		thumbnailTexture = null;
		discardTargets();
	}

	void releaseTargets() {
		for (int i = 0; i < framebuffers.length; ++i) {
			device.deleteFramebuffer(framebuffers[i]);
			framebuffers[i] = null;
			device.deleteTexture(targetTextures[i]);
			targetTextures[i] = null;
		}
		frontTarget = 0;
		backTarget = 1;
	}

	boolean hasTargets() {
		return framebuffers[0] != null && targetTextures[0] != null;
	}

	@NonNull
	List<ShaderError> ensureTargets(
			@NonNull Context context,
			int width,
			int height,
			@NonNull BackBufferParameters parameters) {
		ArrayList<ShaderError> errors = new ArrayList<>();
		if (hasTargets()) {
			return errors;
		}

		releaseTargets();
		createTarget(context, frontTarget, width, height, parameters, errors);
		createTarget(context, backTarget, width, height, parameters, errors);
		device.bindFramebuffer(null);
		return errors;
	}

	void renderMainPass(
			@NonNull ProgramBindings bindings,
			@NonNull GlProgram program) {
		GlFramebuffer framebuffer = framebuffers[frontTarget];
		GlTexture2D targetTexture = targetTextures[frontTarget];
		if (framebuffer == null || targetTexture == null) {
			return;
		}

		device.bindFramebuffer(framebuffer);
		device.setViewport(0, 0, targetTexture.getWidth(), targetTexture.getHeight());
		device.applyBindings(bindings);
		device.draw(fullScreenQuadMesh, program);
	}

	void renderSurfacePass(
			@NonNull ProgramBindings surfaceBindings,
			@NonNull GlProgram surfaceProgram,
			int surfaceWidth,
			int surfaceHeight) {
		drawSurface(
				targetTextures[frontTarget],
				surfaceWidth,
				surfaceHeight,
				null,
				surfaceBindings,
				surfaceProgram);
	}

	@Nullable
	GlTexture2D getBackTexture() {
		return targetTextures[backTarget];
	}

	void swapTargets() {
		int target = frontTarget;
		frontTarget = backTarget;
		backTarget = target;
	}

	@Nullable
	byte[] captureThumbnail(
			@NonNull ProgramBindings surfaceBindings,
			@NonNull GlProgram surfaceProgram) {
		if (thumbnailFramebuffer == null || targetTextures[frontTarget] == null) {
			return null;
		}

		drawSurface(
				targetTextures[frontTarget],
				(int) thumbnailResolution[0],
				(int) thumbnailResolution[1],
				thumbnailFramebuffer,
				surfaceBindings,
				surfaceProgram);

		final int pixels = THUMBNAIL_WIDTH * THUMBNAIL_HEIGHT;
		final int[] rgba = new int[pixels];
		final IntBuffer buffer = IntBuffer.wrap(rgba);
		device.readPixels(
				0,
				0,
				THUMBNAIL_WIDTH,
				THUMBNAIL_HEIGHT,
				buffer);
		device.bindFramebuffer(null);

		int[] argb = new int[pixels];
		for (int y = 0; y < THUMBNAIL_HEIGHT; ++y) {
			for (int x = 0; x < THUMBNAIL_WIDTH; ++x) {
				int srcIdx = y * THUMBNAIL_WIDTH + x;
				int destIdx = (THUMBNAIL_HEIGHT - y - 1) * THUMBNAIL_WIDTH + x;
				int pixel = rgba[srcIdx];
				argb[destIdx] = 0xff000000
						| ((pixel << 16) & 0x00ff0000)
						| (pixel & 0x0000ff00)
						| ((pixel >> 16) & 0x000000ff);
			}
		}

		try (var out = new ByteArrayOutputStream()) {
			Bitmap.createBitmap(
					argb,
					THUMBNAIL_WIDTH,
					THUMBNAIL_HEIGHT,
					Bitmap.Config.ARGB_8888).compress(
					Bitmap.CompressFormat.PNG,
					100,
					out);
			return out.toByteArray();
		} catch (OutOfMemoryError | IllegalArgumentException | IOException e) {
			return null;
		}
	}

	private void drawSurface(
			@Nullable GlTexture2D sourceTexture,
			int drawWidth,
			int drawHeight,
			@Nullable GlFramebuffer targetFramebuffer,
			@NonNull ProgramBindings surfaceBindings,
			@NonNull GlProgram surfaceProgram) {
		if (sourceTexture == null) {
			return;
		}

		device.bindFramebuffer(targetFramebuffer);
		device.setViewport(0, 0, drawWidth, drawHeight);
		drawResolution[0] = drawWidth;
		drawResolution[1] = drawHeight;
		surfaceBindings.clear();
		surfaceBindings.setFloat2(
				ShaderRenderer.UNIFORM_RESOLUTION,
				drawResolution);
		surfaceBindings.setTexture(SURFACE_FRAME, sourceTexture);
		device.applyBindings(surfaceBindings);
		device.clear(GLES20.GL_COLOR_BUFFER_BIT);
		device.draw(fullScreenQuadMesh, surfaceProgram);
	}

	private void discardTargets() {
		framebuffers[0] = null;
		framebuffers[1] = null;
		targetTextures[0] = null;
		targetTextures[1] = null;
		frontTarget = 0;
		backTarget = 1;
	}

	private void createTarget(
			@NonNull Context context,
			int index,
			int width,
			int height,
			@NonNull BackBufferParameters parameters,
			@NonNull List<ShaderError> errors) {
		GlTexture2D texture = device.createTexture2D();
		targetTextures[index] = texture;

		Bitmap bitmap = parameters.getPresetBitmap(context, width, height);
		if (bitmap != null) {
			String message = device.uploadTexture2D(texture, bitmap, true);
			if (message != null) {
				errors.add(ShaderError.createGeneral(message));
			}
			bitmap.recycle();
		} else {
			device.allocateTexture2D(texture, width, height);
		}

		device.applyTextureParameters(texture, parameters);
		device.generateMipmap(texture);

		GlFramebuffer framebuffer = device.createFramebuffer();
		framebuffers[index] = framebuffer;
		device.attachColor(framebuffer, texture);
		int status = device.checkFramebufferStatus(framebuffer);
		if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
			errors.add(ShaderError.createGeneral(
					"Framebuffer incomplete: 0x" + Integer.toHexString(status)));
		}

		if (bitmap == null) {
			device.bindFramebuffer(framebuffer);
			device.clear(GLES20.GL_COLOR_BUFFER_BIT |
					GLES20.GL_DEPTH_BUFFER_BIT);
		}
	}
}
