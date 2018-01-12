package de.markusfisch.android.shadereditor.widget;

import de.markusfisch.android.shadereditor.opengl.ShaderRenderer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.EGLContextFactory;
import android.util.AttributeSet;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

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

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		renderer.touchAt(event);
		return true;
	}

	public void setFragmentShader(String src, float quality) {
		onPause();
		renderer.setFragmentShader(src, quality);
		onResume();
	}

	public ShaderRenderer getRenderer() {
		return renderer;
	}

	private void init(Context context, int renderMode) {
		renderer = new ShaderRenderer(context);

		// on some devices it's important to setEGLContextClientVersion()
		// even if the docs say it's not used when setEGLContextFactory()
		// is called; not doing so will crash the app (e.g. on the FP1)
		setEGLContextClientVersion(2);
		setEGLContextFactory(new ContextFactory(renderer));
		setRenderer(renderer);
		setRenderMode(renderMode);
	}

	private static class ContextFactory
			implements GLSurfaceView.EGLContextFactory {
		private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
		private ShaderRenderer renderer;

		public ContextFactory(ShaderRenderer renderer) {
			this.renderer = renderer;
		}

		@Override
		public EGLContext createContext(EGL10 egl, EGLDisplay display,
				EGLConfig eglConfig) {
			EGLContext context = egl.eglCreateContext(display, eglConfig,
					EGL10.EGL_NO_CONTEXT, new int[] {
				EGL_CONTEXT_CLIENT_VERSION, 3,
				EGL10.EGL_NONE
			});
			if (context != null && context != EGL10.EGL_NO_CONTEXT &&
					context.getGL() != null) {
				renderer.setVersion(3);
				return context;
			}
			return egl.eglCreateContext(display, eglConfig,
					EGL10.EGL_NO_CONTEXT, new int[] {
				EGL_CONTEXT_CLIENT_VERSION, 2,
				EGL10.EGL_NONE
			});
		}

		@Override
		public void destroyContext(EGL10 egl, EGLDisplay display,
				EGLContext context) {
			egl.eglDestroyContext(display, context);
		}
	}
}
