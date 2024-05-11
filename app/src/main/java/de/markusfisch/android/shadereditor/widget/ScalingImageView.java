package de.markusfisch.android.shadereditor.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

public class ScalingImageView extends AppCompatImageView {
	private final SparseArray<PointF> initialPoint = new SparseArray<>();
	private final Matrix initialMatrix = new Matrix();
	private final Matrix transformMatrix = new Matrix();
	private final Tapeline initialTapeline = new Tapeline();
	private final Tapeline transformTapeline = new Tapeline();
	private final RectF drawableRect = new RectF();
	private final RectF bounds = new RectF();

	private GestureDetector gestureDetector;
	private ImageView.ScaleType scaleType = ImageView.ScaleType.CENTER_INSIDE;
	private float magnifyScale = 4f;
	private float minWidth;
	private float rotation;

	public ScalingImageView(Context context) {
		super(context);
		init(context);
	}

	public ScalingImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public ScalingImageView(
			Context context,
			AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	@Override
	public void setImageMatrix(Matrix matrix) {
		transformMatrix.set(matrix);
		setMinWidth(bounds, new Matrix());
		fitTranslate(transformMatrix, getDrawableRect(), bounds);
		super.setImageMatrix(transformMatrix);
	}

	@Override
	public void setScaleType(ImageView.ScaleType scaleType) {
		if (scaleType != ImageView.ScaleType.CENTER &&
				scaleType != ImageView.ScaleType.CENTER_CROP &&
				scaleType != ImageView.ScaleType.CENTER_INSIDE) {
			throw new UnsupportedOperationException();
		}

		this.scaleType = scaleType;
		center(bounds);
		invalidate();
	}

	@Override
	public ScaleType getScaleType() {
		return scaleType;
	}

	// Click handling is correct.
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (gestureDetector.onTouchEvent(event)) {
			return true;
		}

		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				initTransform(event, -1);
				return true;
			case MotionEvent.ACTION_POINTER_DOWN:
				initTransform(event, -1);
				return true;
			case MotionEvent.ACTION_MOVE:
				transform(event);
				return true;
			case MotionEvent.ACTION_POINTER_UP:
				initTransform(event,
						// Ignore the pointer that has gone up.
						event.getActionIndex());
				return true;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				return true;
		}

		return super.onTouchEvent(event);
	}

	public void setMagnifyScale(float scale) {
		magnifyScale = scale;
	}

	public float getMagnifyScale() {
		return magnifyScale;
	}

	public void setImageRotation(float degrees) {
		if (degrees == rotation) {
			return;
		}

		rotation = degrees;
		requestLayout();
	}

	public float getImageRotation() {
		return rotation;
	}

	public Rect getRectInBounds() {
		RectF srcRect = getDrawableRect();
		RectF dstRect = new RectF();
		transformMatrix.mapRect(dstRect, srcRect);

		float scale = dstRect.width() / srcRect.width();
		return new Rect(
				Math.round((bounds.left - dstRect.left) / scale),
				Math.round((bounds.top - dstRect.top) / scale),
				Math.round((bounds.right - dstRect.left) / scale),
				Math.round((bounds.bottom - dstRect.top) / scale));
	}

	public RectF getNormalizedRectInBounds() {
		RectF dstRect = getMappedRect();
		float w = dstRect.width();
		float h = dstRect.height();
		return new RectF(
				(bounds.left - dstRect.left) / w,
				(bounds.top - dstRect.top) / h,
				(bounds.right - dstRect.left) / w,
				(bounds.bottom - dstRect.top) / h);
	}

	@Override
	protected void onLayout(
			boolean changed,
			int left,
			int top,
			int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		// Use a separate method to layout the image so it's possible
		// to override this behaviour without skipping super.onLayout().
		layoutImage(changed, left, top, right, bottom);
	}

	protected void layoutImage(
			boolean changed,
			int left,
			int top,
			int right,
			int bottom) {
		if (changed) {
			setBounds(left, top, right, bottom);
		}

		center(bounds);
	}

	protected void setBounds(
			float left,
			float top,
			float right,
			float bottom) {
		bounds.set(left, top, right, bottom);
	}

	protected void setBounds(RectF rect) {
		bounds.set(rect);
	}

	protected RectF getBounds() {
		return bounds;
	}

	protected void center(RectF rect) {
		setMinWidth(rect, transformMatrix);
		super.setImageMatrix(transformMatrix);
	}

	protected boolean inBounds() {
		return getMappedRect().width() <= minWidth;
	}

	protected void setMinWidth(RectF rect, Matrix matrix) {
		// Don't try to store the drawable dimensions by overriding
		// setImageDrawable() since it is called in the ImageView's
		// constructor and no referenced member of this object will
		// have been initialized yet. So it's best to simply request
		// the dimensions when they are required only.
		RectF srcRect = new RectF(getDrawableRect());

		if (rect == null || matrix == null) {
			return;
		}

		float dw = srcRect.width();
		float dh = srcRect.height();
		float rw = rect.width();
		float rh = rect.height();

		if (dw < 1 || dh < 1 || rw < 1 || rh < 1) {
			return;
		}

		RectF dstRect = new RectF();
		matrix.setTranslate(dw * -.5f, dh * -.5f);
		matrix.postRotate(rotation);
		matrix.mapRect(dstRect, srcRect);

		float xr = rw / dstRect.width();
		float yr = rh / dstRect.height();
		float scale;

		if (scaleType == ImageView.ScaleType.CENTER) {
			scale = 1f;
		} else if (scaleType == ImageView.ScaleType.CENTER_INSIDE) {
			scale = Math.min(xr, yr);
		} else if (scaleType == ImageView.ScaleType.CENTER_CROP) {
			scale = Math.max(xr, yr);
		} else {
			throw new UnsupportedOperationException();
		}

		matrix.postScale(scale, scale);
		matrix.postTranslate(
				Math.round(rect.left + rw * .5f),
				Math.round(rect.top + rh * .5f));

		matrix.mapRect(dstRect, srcRect);

		minWidth = dstRect.width();
	}

	private void init(Context context) {
		super.setScaleType(ImageView.ScaleType.MATRIX);
		gestureDetector = new GestureDetector(
				context,
				new GestureDetector.SimpleOnGestureListener() {
					@Override
					public boolean onDoubleTap(@NonNull MotionEvent event) {
						magnify(event.getX(), event.getY(), magnifyScale);
						initTransform(event, -1);
						return true;
					}
				});
	}

	private RectF getDrawableRect() {
		Drawable drawable;
		int w = 0;
		int h = 0;

		if ((drawable = getDrawable()) != null) {
			w = drawable.getIntrinsicWidth();
			h = drawable.getIntrinsicHeight();
		}

		return new RectF(0, 0, w, h);
	}

	private RectF getMappedRect() {
		RectF dstRect = new RectF();
		transformMatrix.mapRect(dstRect, getDrawableRect());
		return dstRect;
	}

	private void initTransform(MotionEvent event, int ignoreIndex) {
		initialMatrix.set(transformMatrix);
		drawableRect.set(getDrawableRect());

		// Try to find two pointers that are down.
		// Event may contain a pointer that has
		// gone up and must be ignored.
		int p1 = 0xffff;
		int p2 = 0xffff;

		for (int i = 0, l = event.getPointerCount(); i < l; ++i) {
			initialPoint.put(event.getPointerId(i), new PointF(
					event.getX(i),
					event.getY(i)));

			if (i == ignoreIndex) {
				continue;
			} else if (p1 == 0xffff) {
				p1 = i;
			} else {
				p2 = i;
				break;
			}
		}

		if (p2 != 0xffff) {
			initialTapeline.set(event, p1, p2);
		}
	}

	private void transform(MotionEvent event) {
		transformMatrix.set(initialMatrix);

		int pointerCount = event.getPointerCount();
		if (pointerCount == 1) {
			int i = event.getActionIndex();
			PointF point = initialPoint.get(event.getPointerId(i));
			if (point != null) {
				transformMatrix.postTranslate(
						event.getX(i) - point.x,
						event.getY(i) - point.y);
			}
		} else if (pointerCount > 1) {
			transformTapeline.set(event, 0, 1);

			float scale = fitScale(
					initialMatrix,
					drawableRect,
					transformTapeline.length / initialTapeline.length);

			transformMatrix.postScale(
					scale,
					scale,
					initialTapeline.pivotX,
					initialTapeline.pivotY);

			transformMatrix.postTranslate(
					transformTapeline.pivotX - initialTapeline.pivotX,
					transformTapeline.pivotY - initialTapeline.pivotY);
		}

		if (fitTranslate(transformMatrix, drawableRect, bounds)) {
			initTransform(event, -1);
		}

		super.setImageMatrix(transformMatrix);
	}

	private float fitScale(
			Matrix matrix,
			RectF rect,
			float scale) {
		RectF dstRect = new RectF();
		matrix.mapRect(dstRect, rect);

		float w = dstRect.width();
		return w * scale < minWidth ? minWidth / w : scale;
	}

	private static boolean fitTranslate(
			Matrix matrix,
			RectF rect,
			RectF frame) {
		RectF dstRect = new RectF();
		matrix.mapRect(dstRect, rect);

		float x = dstRect.left;
		float y = dstRect.top;
		float w = dstRect.width();
		float h = dstRect.height();
		float fw = frame.width();
		float fh = frame.height();
		float minX = frame.right - w;
		float minY = frame.bottom - h;
		float dx = w > fw
				? Math.max(minX - x, Math.min(frame.left - x, 0))
				: (frame.left + Math.round((fw - w) * .5f)) - x;
		float dy = h > fh
				? Math.max(minY - y, Math.min(frame.top - y, 0))
				: (frame.top + Math.round((fh - h) * .5f)) - y;

		if (dx != 0 || dy != 0) {
			matrix.postTranslate(dx, dy);
			return true;
		}

		return false;
	}

	private void magnify(float x, float y, float scale) {
		if (inBounds()) {
			transformMatrix.postScale(scale, scale, x, y);
		} else {
			setMinWidth(bounds, transformMatrix);
		}
		super.setImageMatrix(transformMatrix);
	}

	private static class Tapeline {
		private float length;
		private float pivotX;
		private float pivotY;

		private void set(MotionEvent event, int p1, int p2) {
			float x1 = event.getX(p1);
			float y1 = event.getY(p1);
			float x2 = event.getX(p2);
			float y2 = event.getY(p2);
			float dx = x2 - x1;
			float dy = y2 - y1;

			length = (float) Math.sqrt(dx * dx + dy * dy);
			pivotX = (x1 + x2) * .5f;
			pivotY = (y1 + y2) * .5f;
		}
	}
}
