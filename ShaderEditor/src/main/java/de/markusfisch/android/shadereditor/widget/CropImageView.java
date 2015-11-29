package de.markusfisch.android.shadereditor.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

public class CropImageView extends ScalingImageView
{
	private final Paint paint = new Paint( Paint.ANTI_ALIAS_FLAG );

	private int padding;

	public CropImageView( Context context, AttributeSet attr )
	{
		super( context, attr );

		padding = Math.round(
			context
				.getResources()
				.getDisplayMetrics()
				.density*24f );

		paint.setColor( 0x88ffffff );
		paint.setStyle( Paint.Style.STROKE );

		setScaleType( ScalingImageView.ScaleType.CENTER_CROP );
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

		int width = right-left;
		int height = bottom-top;
		int size = width < height ?
			width-padding*2 :
			height-padding*2;
		int hpad = (width-size)/2;
		int vpad = (height-size)/2;

		setBounds(
			left+hpad,
			top+vpad,
			right-hpad,
			bottom-vpad );

		center( getBounds() );
	}

	@Override
	protected void onDraw( Canvas canvas )
	{
		super.onDraw( canvas );

		canvas.drawRect( getBounds(), paint );
	}
}
