package de.markusfisch.android.shadereditor.opengl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

final class ProgramBindings {
	static final int TYPE_INT = 1;
	static final int TYPE_FLOAT = 2;
	static final int TYPE_FLOAT2 = 3;
	static final int TYPE_FLOAT3 = 4;
	static final int TYPE_FLOAT4 = 5;
	static final int TYPE_MATRIX2 = 6;
	static final int TYPE_MATRIX3 = 7;

	static final class UniformValue {
		final String name;
		int type;
		boolean active;
		boolean transpose;
		int count = 1;
		int intValue;
		float floatValue;
		@Nullable
		float[] values;

		UniformValue(String name, int type) {
			this.name = name;
			this.type = type;
		}
	}

	static final class TextureValue {
		final String name;
		boolean active;
		@Nullable
		GlTexture texture;

		TextureValue(String name) {
			this.name = name;
		}
	}

	@NonNull
	private final GlProgram program;
	@NonNull
	private final Map<String, UniformValue> uniforms = new LinkedHashMap<>();
	@NonNull
	private final Map<String, TextureValue> textures = new LinkedHashMap<>();

	ProgramBindings(@NonNull GlProgram program) {
		this.program = program;
	}

	@NonNull
	GlProgram getProgram() {
		return program;
	}

	@NonNull
	Iterable<UniformValue> getUniforms() {
		return uniforms.values();
	}

	@NonNull
	Iterable<TextureValue> getTextures() {
		return textures.values();
	}

	void clear() {
		for (var uniform : uniforms.values()) {
			uniform.active = false;
		}
		for (var texture : textures.values()) {
			texture.active = false;
			texture.texture = null;
		}
	}

	void setInt(@NonNull String name, int value) {
		var uniform = getOrCreateUniform(name, TYPE_INT);
		uniform.count = 1;
		uniform.intValue = value;
	}

	void setFloat(@NonNull String name, float value) {
		var uniform = getOrCreateUniform(name, TYPE_FLOAT);
		uniform.count = 1;
		uniform.floatValue = value;
	}

	void setFloat2(@NonNull String name, @NonNull float[] values) {
		setFloat2(name, 1, values);
	}

	void setFloat2(@NonNull String name, int count, @NonNull float[] values) {
		var uniform = getOrCreateUniform(name, TYPE_FLOAT2);
		uniform.count = count;
		uniform.values = values;
	}

	void setFloat3(@NonNull String name, @NonNull float[] values) {
		setFloat3(name, 1, values);
	}

	void setFloat3(@NonNull String name, int count, @NonNull float[] values) {
		var uniform = getOrCreateUniform(name, TYPE_FLOAT3);
		uniform.count = count;
		uniform.values = values;
	}

	void setFloat4(@NonNull String name, @NonNull float[] values) {
		setFloat4(name, 1, values);
	}

	void setFloat4(@NonNull String name, int count, @NonNull float[] values) {
		var uniform = getOrCreateUniform(name, TYPE_FLOAT4);
		uniform.count = count;
		uniform.values = values;
	}

	void setMatrix2(@NonNull String name,
			boolean transpose,
			@NonNull float[] values) {
		var uniform = getOrCreateUniform(name, TYPE_MATRIX2);
		uniform.transpose = transpose;
		uniform.count = 1;
		uniform.values = values;
	}

	void setMatrix3(@NonNull String name,
			boolean transpose,
			@NonNull float[] values) {
		var uniform = getOrCreateUniform(name, TYPE_MATRIX3);
		uniform.transpose = transpose;
		uniform.count = 1;
		uniform.values = values;
	}

	void setTexture(@NonNull String name, @Nullable GlTexture texture) {
		var value = textures.get(name);
		if (value == null) {
			value = new TextureValue(name);
			textures.put(name, value);
		}
		value.active = texture != null;
		value.texture = texture;
	}

	@NonNull
	private UniformValue getOrCreateUniform(@NonNull String name, int type) {
		var uniform = uniforms.get(name);
		if (uniform == null) {
			uniform = new UniformValue(name, type);
			uniforms.put(name, uniform);
		}
		uniform.type = type;
		uniform.active = true;
		return uniform;
	}
}
