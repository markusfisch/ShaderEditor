package de.markusfisch.android.shadereditor.opengl;

import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import javax.microedition.khronos.opengles.GL11ExtensionPack;

import de.markusfisch.android.shadereditor.graphics.BitmapEditor;

final class GlDevice {
	private static final int[] CUBE_MAP_TARGETS = {
			GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_NEGATIVE_X,
			GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z,
			GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_POSITIVE_Y,
			GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y,
			GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_POSITIVE_Z,
			GL11ExtensionPack.GL_TEXTURE_CUBE_MAP_POSITIVE_X};

	private final GlStateCache stateCache;
	private final int maxTextureUnits;

	private record ShaderCompileResult(
			@Nullable GlShader shader,
			@NonNull List<ShaderError> infoLog) {
	}

	GlDevice(int maxTextureUnits) {
		this.maxTextureUnits = maxTextureUnits;
		stateCache = new GlStateCache(maxTextureUnits);
	}

	void resetState() {
		stateCache.reset();
	}

	int getMaxTextureUnits() {
		return maxTextureUnits;
	}

	void disable(int cap) {
		GLES20.glDisable(cap);
	}

	void setClearColor(float red, float green, float blue, float alpha) {
		GLES20.glClearColor(red, green, blue, alpha);
	}

	void clear(int mask) {
		GLES20.glClear(mask);
	}

	void setViewport(int x, int y, int width, int height) {
		if (stateCache.viewportX == x &&
				stateCache.viewportY == y &&
				stateCache.viewportWidth == width &&
				stateCache.viewportHeight == height) {
			return;
		}
		GLES20.glViewport(x, y, width, height);
		stateCache.viewportX = x;
		stateCache.viewportY = y;
		stateCache.viewportWidth = width;
		stateCache.viewportHeight = height;
	}

	@NonNull
	GlTexture2D createTexture2D() {
		int[] textures = new int[1];
		GLES20.glGenTextures(1, textures, 0);
		return new GlTexture2D(textures[0]);
	}

	@NonNull
	GlCubemap createCubemap() {
		int[] textures = new int[1];
		GLES20.glGenTextures(1, textures, 0);
		return new GlCubemap(textures[0]);
	}

	@NonNull
	GlExternalTexture createExternalTexture() {
		int[] textures = new int[1];
		GLES20.glGenTextures(1, textures, 0);
		return new GlExternalTexture(textures[0]);
	}

	void deleteTexture(@Nullable GlTexture texture) {
		if (texture == null || !texture.isValid()) {
			return;
		}
		int id = texture.getId();
		GLES20.glDeleteTextures(1, new int[]{id}, 0);
		stateCache.clearTextureId(id);
		texture.invalidate();
	}

	void bindTexture(int unit, @Nullable GlTexture texture) {
		if (texture == null || !texture.isValid() || unit < 0 || unit >= maxTextureUnits) {
			return;
		}
		if (stateCache.activeTextureUnit != unit) {
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + unit);
			stateCache.activeTextureUnit = unit;
		}
		int target = texture.getTarget();
		int[] bindings = stateCache.getTextureBindingsForTarget(target);
		int textureId = texture.getId();
		boolean forceBind = texture.consumeBindingDirty();
		if (forceBind || bindings[unit] != textureId) {
			GLES20.glBindTexture(target, textureId);
			bindings[unit] = textureId;
		}
	}

	void applyTextureParameters(@NonNull GlTexture texture,
			@NonNull TextureParameters parameters) {
		bindTexture(0, texture);
		int target = texture.getTarget();
		int minFilter = parameters.getMinFilter();
		int wrapS = parameters.getWrapS();
		int wrapT = parameters.getWrapT();
		if (target == GLES11Ext.GL_TEXTURE_EXTERNAL_OES) {
			minFilter = switch (minFilter) {
				case GLES20.GL_NEAREST, GLES20.GL_LINEAR -> minFilter;
				default -> GLES20.GL_LINEAR;
			};
			wrapS = GLES20.GL_CLAMP_TO_EDGE;
			wrapT = GLES20.GL_CLAMP_TO_EDGE;
		}
		GLES20.glTexParameteri(
				target,
				GLES20.GL_TEXTURE_MIN_FILTER,
				minFilter);
		GLES20.glTexParameteri(
				target,
				GLES20.GL_TEXTURE_MAG_FILTER,
				parameters.getMagFilter());
		GLES20.glTexParameteri(
				target,
				GLES20.GL_TEXTURE_WRAP_S,
				wrapS);
		GLES20.glTexParameteri(
				target,
				GLES20.GL_TEXTURE_WRAP_T,
				wrapT);
	}

	void allocateTexture2D(@NonNull GlTexture2D texture, int width, int height) {
		bindTexture(0, texture);
		GLES20.glTexImage2D(
				GLES20.GL_TEXTURE_2D,
				0,
				GLES20.GL_RGBA,
				width,
				height,
				0,
				GLES20.GL_RGBA,
				GLES20.GL_UNSIGNED_BYTE,
				null);
		texture.setSize(width, height);
	}

	@Nullable
	String uploadTexture2D(@NonNull GlTexture2D texture,
			@NonNull Bitmap bitmap,
			boolean flipY) {
		bindTexture(0, texture);
		String message = uploadBitmapInternal(
				GLES20.GL_TEXTURE_2D,
				bitmap,
				flipY);
		if (message == null) {
			texture.setSize(bitmap.getWidth(), bitmap.getHeight());
		}
		return message;
	}

	@Nullable
	String uploadCubemapFromAtlas(@NonNull GlCubemap cubemap,
			@NonNull Bitmap bitmap) {
		bindTexture(0, cubemap);

		int bitmapWidth = bitmap.getWidth();
		int bitmapHeight = bitmap.getHeight();
		int sideWidth = (int) Math.ceil(bitmapWidth / 2f);
		int sideHeight = Math.round(bitmapHeight / 3f);
		int sideLength = Math.min(sideWidth, sideHeight);
		int x = 0;
		int y = 0;
		String message = null;

		for (int target : CUBE_MAP_TARGETS) {
			int[] sidePixels = new int[sideLength * sideLength];
			bitmap.getPixels(sidePixels, 0, sideLength, x, y, sideLength, sideLength);

			Bitmap side = Bitmap.createBitmap(
					sideLength,
					sideLength,
					Bitmap.Config.ARGB_8888);
			side.setPremultiplied(false);
			side.setPixels(sidePixels, 0, sideLength, 0, 0, sideLength, sideLength);

			message = uploadBitmapInternal(target, side, false);
			side.recycle();

			if (message != null) {
				return message;
			}

			if ((x += sideWidth) >= bitmapWidth) {
				x = 0;
				y += sideHeight;
			}
		}

		cubemap.setSideLength(sideLength);
		return null;
	}

	void generateMipmap(@NonNull GlTexture texture) {
		bindTexture(0, texture);
		if (texture.getTarget() == GLES11Ext.GL_TEXTURE_EXTERNAL_OES) {
			return;
		}
		GLES20.glGenerateMipmap(texture.getTarget());
	}

	@NonNull
	GlFramebuffer createFramebuffer() {
		int[] framebuffers = new int[1];
		GLES20.glGenFramebuffers(1, framebuffers, 0);
		return new GlFramebuffer(framebuffers[0]);
	}

	void deleteFramebuffer(@Nullable GlFramebuffer framebuffer) {
		if (framebuffer == null || !framebuffer.isValid()) {
			return;
		}
		int id = framebuffer.getId();
		GLES20.glDeleteFramebuffers(1, new int[]{id}, 0);
		if (stateCache.currentFramebufferId == id) {
			stateCache.currentFramebufferId = 0;
		}
		framebuffer.invalidate();
	}

	void bindFramebuffer(@Nullable GlFramebuffer framebuffer) {
		int framebufferId = framebuffer != null && framebuffer.isValid()
				? framebuffer.getId()
				: 0;
		if (stateCache.currentFramebufferId == framebufferId) {
			return;
		}
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferId);
		stateCache.currentFramebufferId = framebufferId;
	}

	void attachColor(@NonNull GlFramebuffer framebuffer,
			@NonNull GlTexture2D texture) {
		bindFramebuffer(framebuffer);
		GLES20.glFramebufferTexture2D(
				GLES20.GL_FRAMEBUFFER,
				GLES20.GL_COLOR_ATTACHMENT0,
				GLES20.GL_TEXTURE_2D,
				texture.getId(),
				0);
		framebuffer.setColorAttachment(texture);
	}

	int checkFramebufferStatus(@NonNull GlFramebuffer framebuffer) {
		bindFramebuffer(framebuffer);
		return GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
	}

	@NonNull
	GlProgramBuildResult createProgram(
			@NonNull String vertexSource,
			@NonNull String fragmentSource,
			int fragmentExtraLines) {
		ShaderCompileResult vertexCompile = compileShader(
				GLES20.GL_VERTEX_SHADER,
				vertexSource,
				0);
		if (vertexCompile.shader == null) {
			return GlProgramBuildResult.failure(vertexCompile.infoLog);
		}

		ShaderCompileResult fragmentCompile = compileShader(
				GLES20.GL_FRAGMENT_SHADER,
				fragmentSource,
				fragmentExtraLines);
		if (fragmentCompile.shader == null) {
			deleteShader(vertexCompile.shader);
			return GlProgramBuildResult.failure(fragmentCompile.infoLog);
		}

		int programId = GLES20.glCreateProgram();
		if (programId == 0) {
			deleteShader(vertexCompile.shader);
			deleteShader(fragmentCompile.shader);
			return GlProgramBuildResult.failure(
					List.of(ShaderError.createGeneral("Cannot create program")));
		}

		GLES20.glAttachShader(programId, vertexCompile.shader.getId());
		GLES20.glAttachShader(programId, fragmentCompile.shader.getId());
		GLES20.glLinkProgram(programId);

		deleteShader(vertexCompile.shader);
		deleteShader(fragmentCompile.shader);

		int[] linkStatus = new int[1];
		GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0);
		if (linkStatus[0] != GLES20.GL_TRUE) {
			List<ShaderError> infoLog = ShaderError.parseAll(
					GLES20.glGetProgramInfoLog(programId),
					fragmentExtraLines);
			GLES20.glDeleteProgram(programId);
			return GlProgramBuildResult.failure(infoLog);
		}

		return GlProgramBuildResult.success(new GlProgram(programId));
	}

	void deleteProgram(@Nullable GlProgram program) {
		if (program == null || !program.isValid()) {
			return;
		}
		int programId = program.getId();
		GLES20.glDeleteProgram(programId);
		if (stateCache.currentProgramId == programId) {
			stateCache.currentProgramId = 0;
		}
		program.invalidate();
	}

	void useProgram(@Nullable GlProgram program) {
		int programId = program != null && program.isValid()
				? program.getId()
				: 0;
		if (stateCache.currentProgramId == programId) {
			return;
		}
		GLES20.glUseProgram(programId);
		stateCache.currentProgramId = programId;
	}

	int getUniformLocation(@NonNull GlProgram program, @NonNull String name) {
		Integer location = program.getUniformLocations().get(name);
		if (location != null) {
			return location;
		}
		int uniformLocation = GLES20.glGetUniformLocation(program.getId(), name);
		program.getUniformLocations().put(name, uniformLocation);
		return uniformLocation;
	}

	boolean hasUniform(@NonNull GlProgram program, @NonNull String name) {
		return getUniformLocation(program, name) > -1;
	}

	int getAttribLocation(@NonNull GlProgram program, @NonNull String name) {
		Integer location = program.getAttribLocations().get(name);
		if (location != null) {
			return location;
		}
		int attribLocation = GLES20.glGetAttribLocation(program.getId(), name);
		program.getAttribLocations().put(name, attribLocation);
		return attribLocation;
	}

	void uniform1i(int location, int value) {
		if (location > -1) {
			GLES20.glUniform1i(location, value);
		}
	}

	void uniform1f(int location, float value) {
		if (location > -1) {
			GLES20.glUniform1f(location, value);
		}
	}

	void uniform2fv(int location, int count, @NonNull float[] values) {
		if (location > -1 && count > 0) {
			GLES20.glUniform2fv(location, count, values, 0);
		}
	}

	void uniform3fv(int location, int count, @NonNull float[] values) {
		if (location > -1 && count > 0) {
			GLES20.glUniform3fv(location, count, values, 0);
		}
	}

	void uniform4fv(int location, int count, @NonNull float[] values) {
		if (location > -1 && count > 0) {
			GLES20.glUniform4fv(location, count, values, 0);
		}
	}

	void uniformMatrix2fv(int location,
			boolean transpose,
			@NonNull float[] values) {
		if (location > -1) {
			GLES20.glUniformMatrix2fv(location, 1, transpose, values, 0);
		}
	}

	void uniformMatrix3fv(int location,
			boolean transpose,
			@NonNull float[] values) {
		if (location > -1) {
			GLES20.glUniformMatrix3fv(location, 1, transpose, values, 0);
		}
	}

	void applyBindings(@NonNull ProgramBindings bindings) {
		GlProgram program = bindings.getProgram();
		useProgram(program);

		for (ProgramBindings.UniformValue uniform : bindings.getUniforms()) {
			if (!uniform.active) {
				continue;
			}
			int location = getUniformLocation(program, uniform.name);
			switch (uniform.type) {
				case ProgramBindings.TYPE_INT:
					uniform1i(location, uniform.intValue);
					break;
				case ProgramBindings.TYPE_FLOAT:
					uniform1f(location, uniform.floatValue);
					break;
				case ProgramBindings.TYPE_FLOAT2:
					if (uniform.values != null) {
						uniform2fv(location, uniform.count, uniform.values);
					}
					break;
				case ProgramBindings.TYPE_FLOAT3:
					if (uniform.values != null) {
						uniform3fv(location, uniform.count, uniform.values);
					}
					break;
				case ProgramBindings.TYPE_FLOAT4:
					if (uniform.values != null) {
						uniform4fv(location, uniform.count, uniform.values);
					}
					break;
				case ProgramBindings.TYPE_MATRIX2:
					if (uniform.values != null) {
						uniformMatrix2fv(location, uniform.transpose, uniform.values);
					}
					break;
				case ProgramBindings.TYPE_MATRIX3:
					if (uniform.values != null) {
						uniformMatrix3fv(location, uniform.transpose, uniform.values);
					}
					break;
				default:
					break;
			}
		}

		int textureUnit = 0;
		for (ProgramBindings.TextureValue texture : bindings.getTextures()) {
			if (!texture.active || texture.texture == null) {
				continue;
			}
			if (textureUnit >= maxTextureUnits) {
				break;
			}
			int location = getUniformLocation(program, texture.name);
			uniform1i(location, textureUnit);
			bindTexture(textureUnit, texture.texture);
			++textureUnit;
		}
	}

	void draw(@NonNull Mesh mesh, @NonNull GlProgram program) {
		Geometry geometry = mesh.getGeometry();
		for (VertexAttribute attribute : geometry.getAttributes()) {
			int location = getAttribLocation(program, attribute.getName());
			if (location < 0) {
				continue;
			}
			GLES20.glEnableVertexAttribArray(location);
			GLES20.glVertexAttribPointer(
					location,
					attribute.getComponentCount(),
					attribute.getType(),
					attribute.isNormalized(),
					attribute.getStride(),
					positionedBuffer(geometry.getVertexBuffer(), attribute.getByteOffset()));
		}
		GLES20.glDrawArrays(
				geometry.getDrawMode(),
				0,
				geometry.getVertexCount());
	}

	void readPixels(int x, int y, int width, int height, @NonNull Buffer buffer) {
		GLES20.glReadPixels(
				x,
				y,
				width,
				height,
				GLES20.GL_RGBA,
				GLES20.GL_UNSIGNED_BYTE,
				buffer);
	}

	@Nullable
	private ShaderCompileResult compileShader(int type,
			@NonNull String source,
			int extraLines) {
		int shaderId = GLES20.glCreateShader(type);
		if (shaderId == 0) {
			return new ShaderCompileResult(
					null,
					List.of(ShaderError.createGeneral("Cannot create shader")));
		}

		GLES20.glShaderSource(shaderId, source);
		GLES20.glCompileShader(shaderId);

		int[] compiled = new int[1];
		GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, compiled, 0);
		if (compiled[0] == 0) {
			List<ShaderError> infoLog = ShaderError.parseAll(
					GLES20.glGetShaderInfoLog(shaderId),
					extraLines);
			GLES20.glDeleteShader(shaderId);
			return new ShaderCompileResult(null, infoLog);
		}

		return new ShaderCompileResult(
				new GlShader(shaderId, type, source),
				java.util.Collections.emptyList());
	}

	private void deleteShader(@Nullable GlShader shader) {
		if (shader == null || !shader.isValid()) {
			return;
		}
		GLES20.glDeleteShader(shader.getId());
		shader.invalidate();
	}

	@Nullable
	private static String uploadBitmapInternal(int target,
			@NonNull Bitmap bitmap,
			boolean flipY) {
		clearGlErrors();
		GLES20.glTexImage2D(
				target,
				0,
				GLES20.GL_RGBA,
				bitmap.getWidth(),
				bitmap.getHeight(),
				0,
				GLES20.GL_RGBA,
				GLES20.GL_UNSIGNED_BYTE,
				BitmapEditor.createRgbaBuffer(bitmap, flipY));
		int error = getLastGlError();
		if (error != GLES20.GL_NO_ERROR) {
			return "glTexImage2D failed with GL error 0x" + Integer.toHexString(error);
		}
		return null;
	}

	private static void clearGlErrors() {
		while (GLES20.glGetError() != GLES20.GL_NO_ERROR) {
			// Drain stale GL errors so uploads only report their own failures.
		}
	}

	private static int getLastGlError() {
		int lastError = GLES20.GL_NO_ERROR;
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			lastError = error;
		}
		return lastError;
	}

	@NonNull
	private static Buffer positionedBuffer(@NonNull Buffer buffer, int byteOffset) {
		if (buffer instanceof ByteBuffer byteBuffer) {
			ByteBuffer duplicate = byteBuffer.duplicate();
			duplicate.position(byteOffset);
			return duplicate;
		}
		if (buffer instanceof FloatBuffer floatBuffer) {
			FloatBuffer duplicate = floatBuffer.duplicate();
			duplicate.position(byteOffset / 4);
			return duplicate;
		}
		if (buffer instanceof IntBuffer intBuffer) {
			IntBuffer duplicate = intBuffer.duplicate();
			duplicate.position(byteOffset / 4);
			return duplicate;
		}
		throw new IllegalArgumentException("Unsupported vertex buffer type: " +
				buffer.getClass().getSimpleName());
	}
}
