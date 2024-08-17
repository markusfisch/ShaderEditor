package de.markusfisch.android.shadereditor.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

import de.markusfisch.android.shadereditor.opengl.ShaderRenderer;

public class ShaderView extends GLSurfaceView {
	private ShaderRenderer renderer;

	public ShaderView(Context context, int renderMode) {
		super(context);
		init(context, renderMode);
	}

	public ShaderView(Context context) {
		super(context);
		init(context, GLSurfaceView.RENDERMODE_CONTINUOUSLY);
	}

	public ShaderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, GLSurfaceView.RENDERMODE_CONTINUOUSLY);
	}

	@Override
	public void onPause() {
		super.onPause();
		renderer.unregisterListeners();
	}

	// Click handling is implemented in renderer.
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event) {
		renderer.touchAt(event);
		return true;
	}

	public void setFragmentShader(String src, float quality) {
		onPause();
		// When pasting text from other apps, e.g. Gmail, the
		// text is sometimes tainted with useless non-ascii
		// characters that can raise an exception in the shader
		// compiler. To still allow UTF-8 characters in comments,
		// the source is cleaned up here.
		renderer.setFragmentShader(removeNonAscii(src), quality);
		onResume();
	}

	public ShaderRenderer getRenderer() {
		return renderer;
	}

	private void init(Context context, int renderMode) {
		renderer = new ShaderRenderer(context);

		// On some devices it's important to setEGLContextClientVersion()
		// even if the docs say it's not used when setEGLContextFactory()
		// is called. Not doing so will crash the app (e.g. on the FP1).
		setEGLContextClientVersion(2);
		setEGLContextFactory(new ContextFactory(renderer));
		setRenderer(renderer);
		setRenderMode(renderMode);
	}

	private static String removeNonAscii(@Nullable String text) {
		return text == null
				? null
				: text.replaceAll("[^\\x0A\\x09\\x20-\\x7E]", "");
	}

	private static class ContextFactory
			implements GLSurfaceView.EGLContextFactory {
		private final ShaderRenderer renderer;

		private ContextFactory(ShaderRenderer renderer) {
			this.renderer = renderer;
		}

		@Override
		public EGLContext createContext(@NonNull EGL10 egl, EGLDisplay display,
				EGLConfig eglConfig) {
			int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
			EGLContext context = egl.eglCreateContext(display, eglConfig,
					EGL10.EGL_NO_CONTEXT, new int[]{
							EGL_CONTEXT_CLIENT_VERSION,
							3,
							EGL10.EGL_NONE
					});
			if (context != null && context != EGL10.EGL_NO_CONTEXT &&
					context.getGL() != null) {
				renderer.setVersion(3);
				return context;
			}
			return egl.eglCreateContext(display, eglConfig,
					EGL10.EGL_NO_CONTEXT, new int[]{
							EGL_CONTEXT_CLIENT_VERSION,
							2,
							EGL10.EGL_NONE
					});
		}

		@Override
		public void destroyContext(@NonNull EGL10 egl, EGLDisplay display,
				EGLContext context) {
			egl.eglDestroyContext(display, context);
		}
	}
}
