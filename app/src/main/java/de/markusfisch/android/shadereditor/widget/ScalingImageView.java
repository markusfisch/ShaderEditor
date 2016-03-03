package de.markusfisch.android.shadereditor.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.widget.ImageView;

public class ScalingImageView extends ImageView
{
	private final SparseArray<Float> originX = new SparseArray<Float>();
	private final SparseArray<Float> originY = new SparseArray<Float>();
	private final Matrix originMatrix = new Matrix();
	private final Matrix transformMatrix = new Matrix();
	private final Gesture originGesture = new Gesture();
	private final Gesture transformGesture = new Gesture();
	private final RectF srcRect = new RectF();
	private final RectF dstRect = new RectF();
	private final RectF bounds = new RectF();

	private float minWidth;
	private float rotation = 0f;
	private ImageView.ScaleType scaleType =
		ImageView.ScaleType.CENTER_INSIDE;

	public ScalingImageView( Context context )
	{
		super( context );
		init();
	}

	public ScalingImageView( Context context, AttributeSet attrs )
	{
		super( context, attrs );
		init();
	}

	public ScalingImageView(
		Context context,
		AttributeSet attrs,
		int defStyleAttr )
	{
		super( context, attrs, defStyleAttr );
		init();
	}

	@TargetApi( 21 )
	public ScalingImageView(
		Context context,
		AttributeSet attrs,
		int defStyleAttr,
		int defStyleRes )
	{
		super( context, attrs, defStyleAttr, defStyleRes );
		init();
	}

	@Override
	public void setScaleType( ImageView.ScaleType scaleType )
	{
		if( scaleType != ImageView.ScaleType.CENTER &&
			scaleType != ImageView.ScaleType.CENTER_CROP &&
			scaleType != ImageView.ScaleType.CENTER_INSIDE )
			throw new UnsupportedOperationException();

		this.scaleType = scaleType;
		center( bounds );
	}

	@Override
	public ScaleType getScaleType()
	{
		return scaleType;
	}

	@Override
	public boolean onTouchEvent( MotionEvent event )
	{
		final int pointerCount = event.getPointerCount();
		int ignoreIndex = -1;

		switch( event.getActionMasked() )
		{
			// the number of pointers changed so
			// (re)initialize the transformation
			case MotionEvent.ACTION_POINTER_UP:
				// ignore the pointer that has gone up
				ignoreIndex = event.getActionIndex();
				// fall through
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				initTransform(
					event,
					pointerCount,
					ignoreIndex );
				return true;
			// the position of the pointer(s) changed
			// so transform accordingly
			case MotionEvent.ACTION_MOVE:
				transform( event, pointerCount );
				return true;
			// end of transformation
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				return true;
		}

		return onTouchEvent( event );
	}

	@Override
	protected void onLayout(
		boolean changed,
		int left,
		int top,
		int right,
		int bottom )
	{
		super.onLayout(
			changed,
			left,
			top,
			right,
			bottom );

		setBounds( left, top, right, bottom );
		center( bounds );
	}

	public void setImageRotation( float degrees )
	{
		rotation = degrees;
		center( bounds );
	}

	public float getImageRotation()
	{
		return rotation;
	}

	public Rect getRectInBounds()
	{
		transformMatrix.mapRect( dstRect, srcRect );

		float scale = dstRect.width()/srcRect.width();

		return new Rect(
			Math.round( (bounds.left-dstRect.left)/scale ),
			Math.round( (bounds.top-dstRect.top)/scale ),
			Math.round( (bounds.right-dstRect.left)/scale ),
			Math.round( (bounds.bottom-dstRect.top)/scale ) );
	}

	public RectF getNormalizedRectInBounds()
	{
		transformMatrix.mapRect( dstRect, srcRect );

		float w = dstRect.width();
		float h = dstRect.height();

		return new RectF(
			(bounds.left-dstRect.left)/w,
			(bounds.top-dstRect.top)/h,
			(bounds.right-dstRect.left)/w,
			(bounds.bottom-dstRect.top)/h );
	}

	protected void setBounds(
		float left,
		float top,
		float right,
		float bottom )
	{
		bounds.set( left, top, right, bottom );
	}

	protected void setBounds( RectF rect )
	{
		bounds.set( rect );
	}

	protected RectF getBounds()
	{
		return bounds;
	}

	protected void center( RectF rect )
	{
		Drawable drawable;

		if( rect == null ||
			(drawable = getDrawable()) == null )
			return;

		float dw = drawable.getIntrinsicWidth();
		float dh = drawable.getIntrinsicHeight();
		srcRect.set( 0f, 0f, dw, dh );

		transformMatrix.setTranslate( dw*-.5f, dh*-.5f );
		transformMatrix.postRotate( rotation );
		transformMatrix.mapRect( dstRect, srcRect );

		float rw = rect.width();
		float rh = rect.height();
		float xr = rw/dstRect.width();
		float yr = rh/dstRect.height();
		float scale;

		if( scaleType == ImageView.ScaleType.CENTER )
			scale = 1f;
		else if( scaleType == ImageView.ScaleType.CENTER_INSIDE )
			scale = Math.min( xr, yr );
		else if( scaleType == ImageView.ScaleType.CENTER_CROP )
			scale = Math.max( xr, yr );
		else
			throw new UnsupportedOperationException();

		transformMatrix.postScale( scale, scale );
		transformMatrix.postTranslate(
			Math.round( rect.left+rw*.5f ),
			Math.round( rect.top+rh*.5f ) );

		transformMatrix.mapRect( dstRect, srcRect );
		minWidth = dstRect.width();

		setImageMatrix( transformMatrix );
	}

	private void init()
	{
		super.setScaleType( ImageView.ScaleType.MATRIX );
	}

	private void initTransform(
		MotionEvent event,
		int pointerCount,
		int ignoreIndex )
	{
		originMatrix.set( transformMatrix );

		// try to find two pointers that are down;
		// pointerCount may include a pointer that
		// has gone up (ignoreIndex)
		int p1 = 0xffff;
		int p2 = 0xffff;

		for( int n = 0; n < pointerCount; ++n )
		{
			int id = event.getPointerId( n );

			originX.put( id, event.getX( n ) );
			originY.put( id, event.getY( n ) );

			if( n == ignoreIndex ||
				p2 != 0xffff )
				continue;

			if( p1 == 0xffff )
				p1 = n;
			else if( p2 == 0xffff )
				p2 = n;
		}

		if( p2 != 0xffff )
			originGesture.set( event, p1, p2 );
	}

	private void transform( MotionEvent event, int pointerCount )
	{
		transformMatrix.set( originMatrix );

		if( pointerCount == 1 )
		{
			int id = event.getPointerId( 0 );

			transformMatrix.postTranslate(
				event.getX( 0 )-originX.get( id ),
				event.getY( 0 )-originY.get( id ) );
		}
		else if( pointerCount > 1 )
		{
			transformGesture.set( event, 0, 1 );

			float scale = fitScale(
				originMatrix,
				transformGesture.length/originGesture.length );

			transformMatrix.postScale(
				scale,
				scale,
				originGesture.pivotX,
				originGesture.pivotY );

			transformMatrix.postTranslate(
				transformGesture.pivotX-originGesture.pivotX,
				transformGesture.pivotY-originGesture.pivotY );
		}

		if( fitRect( transformMatrix ) )
			initTransform( event, pointerCount, -1 );

		setImageMatrix( transformMatrix );
	}

	private float fitScale( Matrix matrix, float scale )
	{
		matrix.mapRect( dstRect, srcRect );
		float w = dstRect.width();

		return w*scale < minWidth ?
			minWidth/w :
			scale;
	}

	private boolean fitRect( Matrix matrix )
	{
		matrix.mapRect( dstRect, srcRect );

		float x = dstRect.left;
		float y = dstRect.top;
		float w = dstRect.width();
		float h = dstRect.height();
		float bw = bounds.width();
		float bh = bounds.height();
		float minX = bounds.right-w;
		float minY = bounds.bottom-h;
		float dx = w > bw ?
			Math.max( minX-x, Math.min( bounds.left-x, 0 ) ) :
			(bounds.left+Math.round( (bw-w)*.5f ))-x;
		float dy = h > bh ?
			Math.max( minY-y, Math.min( bounds.top-y, 0 ) ) :
			(bounds.top+Math.round( (bh-h)*.5f ))-y;

		if( dx != 0 || dy != 0 )
		{
			matrix.postTranslate( dx, dy );
			return true;
		}

		return false;
	}

	private static class Gesture
	{
		public float length;
		public float pivotX;
		public float pivotY;

		public void set( MotionEvent event, int p1, int p2 )
		{
			float x1 = event.getX( p1 );
			float y1 = event.getY( p1 );
			float x2 = event.getX( p2 );
			float y2 = event.getY( p2 );
			float dx = x2-x1;
			float dy = y2-y1;

			length = (float)Math.sqrt( dx*dx + dy*dy );
			pivotX = (x1+x2)*.5f;
			pivotY = (y1+y2)*.5f;
		}
	}
}
