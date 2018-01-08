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
		setEGLContextFactory(new ContextFactory());
		setRenderer((renderer = new ShaderRenderer(context)));
		setRenderMode(renderMode);
	}

	private static class ContextFactory
			implements GLSurfaceView.EGLContextFactory {
		@Override
		public EGLContext createContext(EGL10 egl, EGLDisplay display,
				EGLConfig eglConfig) {
			EGLContext context = egl.eglCreateContext(display, eglConfig,
					EGL10.EGL_NO_CONTEXT, new int[] {
				0x3098,
				(int) 3,
				EGL10.EGL_NONE
			});
			GL10 gl;
			if (context != null &&
					(gl = (GL10) context.getGL()) != null &&
					gl.glGetString(GL10.GL_VERSION) != null) {
				return context;
			}
			return egl.eglCreateContext(display, eglConfig,
					EGL10.EGL_NO_CONTEXT, new int[] {
				0x3098,
				(int) 2,
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
