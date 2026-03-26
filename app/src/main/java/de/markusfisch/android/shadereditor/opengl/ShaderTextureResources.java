package de.markusfisch.android.shadereditor.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.shadereditor.database.Database;

final class ShaderTextureResources {
	static final class SamplerTextureBinding {
		@NonNull
		private final DiscoveredSampler sampler;
		@Nullable
		private GlTexture texture;

		SamplerTextureBinding(@NonNull DiscoveredSampler sampler) {
			this.sampler = sampler;
		}

		@NonNull
		DiscoveredSampler sampler() {
			return sampler;
		}

		@Nullable
		GlTexture texture() {
			return texture;
		}

		void setTexture(@Nullable GlTexture texture) {
			this.texture = texture;
		}
	}

	@NonNull
	private final List<SamplerTextureBinding> bindings;

	private ShaderTextureResources(@NonNull List<SamplerTextureBinding> bindings) {
		this.bindings = bindings;
	}

	@NonNull
	static ShaderTextureResources create(@NonNull List<DiscoveredSampler> samplers) {
		var bindings = new ArrayList<SamplerTextureBinding>(samplers.size());
		for (var sampler : samplers) {
			bindings.add(new SamplerTextureBinding(sampler));
		}
		return new ShaderTextureResources(bindings);
	}

	@NonNull
	static ShaderTextureResources empty() {
		return new ShaderTextureResources(List.of());
	}

	void discard() {
		for (var binding : bindings) {
			binding.setTexture(null);
		}
	}

	void release(@NonNull GlDevice device) {
		for (var binding : bindings) {
			device.deleteTexture(binding.texture());
			binding.setTexture(null);
		}
	}

	@NonNull
	List<ShaderError> load(@NonNull Context context, @NonNull GlDevice device) {
		release(device);

		var errors = new ArrayList<ShaderError>();
		var textureDao = Database.getInstance(context).getDataSource().texture;
		for (var binding : bindings) {
			var sampler = binding.sampler();
			if (sampler.target() == GLES11Ext.GL_TEXTURE_EXTERNAL_OES) {
				loadExternalTexture(device, binding);
				continue;
			}

			var bitmap = textureDao.getTextureBitmap(sampler.name());
			if (bitmap == null) {
				continue;
			}

			try {
				switch (sampler.target()) {
					case GLES20.GL_TEXTURE_2D -> loadTexture2D(
							device,
							binding,
							errors,
							bitmap);
					case GLES20.GL_TEXTURE_CUBE_MAP -> loadCubemap(
							device,
							binding,
							bitmap,
							errors);
					default -> {
					}
				}
			} finally {
				bitmap.recycle();
			}
		}

		return errors;
	}

	void applyTo(@NonNull ProgramBindings bindings) {
		for (var binding : this.bindings) {
			if (binding.texture() != null) {
				bindings.setTexture(binding.sampler().name(), binding.texture());
			}
		}
	}

	@Nullable
	SamplerTextureBinding getFirstBinding(@NonNull String... samplerNames) {
		for (var binding : bindings) {
			for (String samplerName : samplerNames) {
				if (binding.sampler().name().equals(samplerName)) {
					return binding;
				}
			}
		}
		return null;
	}

	private static void loadExternalTexture(
			@NonNull GlDevice device,
			@NonNull SamplerTextureBinding binding) {
		var sampler = binding.sampler();
		var texture = device.createExternalTexture();
		device.applyTextureParameters(texture, sampler.parameters());
		binding.setTexture(texture);
	}

	private static void loadTexture2D(
			@NonNull GlDevice device,
			@NonNull SamplerTextureBinding binding,
			@NonNull List<ShaderError> errors,
			@NonNull Bitmap bitmap) {
		var sampler = binding.sampler();
		var texture = device.createTexture2D();
		device.applyTextureParameters(texture, sampler.parameters());
		var message = device.uploadTexture2D(texture, bitmap, true);
		if (message != null) {
			errors.add(ShaderError.createGeneral(message));
			device.deleteTexture(texture);
			return;
		}
		device.generateMipmap(texture);
		binding.setTexture(texture);
	}

	private static void loadCubemap(
			@NonNull GlDevice device,
			@NonNull SamplerTextureBinding binding,
			@NonNull Bitmap bitmap,
			@NonNull List<ShaderError> errors) {
		var sampler = binding.sampler();
		var texture = device.createCubemap();
		device.applyTextureParameters(texture, sampler.parameters());
		var message = device.uploadCubemapFromAtlas(texture, bitmap);
		if (message != null) {
			errors.add(ShaderError.createGeneral(message));
			device.deleteTexture(texture);
			return;
		}
		device.generateMipmap(texture);
		binding.setTexture(texture);
	}
}
