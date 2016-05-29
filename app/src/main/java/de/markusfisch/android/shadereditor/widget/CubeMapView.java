package de.markusfisch.android.shadereditor.widget;

import de.markusfisch.android.shadereditor.graphics.BitmapEditor;
import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.preference.Preferences;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class CubeMapView extends ScalingImageView
{
	private static final int PREVIEW_SIZE = 256;
	private static final int LABEL_ID[] = new int[]{
		R.string.cube_map_negative_x,
		R.string.cube_map_negative_z,
		R.string.cube_map_positive_y,
		R.string.cube_map_negative_y,
		R.string.cube_map_positive_z,
		R.string.cube_map_positive_x };

	public static class Face implements Parcelable
	{
		public static final Parcelable.Creator<Face> CREATOR =
			new Parcelable.Creator<Face>()
			{
				@Override
				public Face createFromParcel( Parcel in )
				{
					return new Face( in );
				}

				@Override
				public Face[] newArray( int size )
				{
					return new Face[size];
				}
			};

		private final RectF bounds = new RectF();
		private final String label;

		private Uri uri;
		private RectF clip;
		private float rotation = 0;
		private Bitmap bitmap;
		private Matrix matrix;

		public Uri getUri()
		{
			return uri;
		}

		public RectF getClip()
		{
			return clip;
		}

		public float getRotation()
		{
			return rotation;
		}

		@Override
		public int describeContents()
		{
			return 0;
		}

		@Override
		public void writeToParcel( Parcel out, int flags )
		{
			out.writeParcelable( uri, flags );
			out.writeParcelable( clip, flags );
			out.writeFloat( rotation );
		}

		private Face( String label )
		{
			this.label = label;
		}

		private Face( Parcel in )
		{
			label = null;
			uri = in.readParcelable( Uri.class.getClassLoader() );
			clip = in.readParcelable( RectF.class.getClassLoader() );
			rotation = in.readFloat();
		}
	}

	private final Face faces[] = new Face[6];
	private final Paint selectedPaint = new Paint( Paint.ANTI_ALIAS_FLAG );
	private final Paint unselectedPaint = new Paint( Paint.ANTI_ALIAS_FLAG );
	private final Paint textPaint = new Paint( Paint.ANTI_ALIAS_FLAG );
	private final Point navigationBarSize = new Point( 0, 0 );

	private int mapPadding;
	private int textPadding;
	private int toolAndStatusBarHeight;
	private int tapTimeout = ViewConfiguration.getTapTimeout();
	private Bitmap selectedBitmap;
	private int selectedFace = 0;
	private long touchDownTime = 0;

	public CubeMapView( Context context, AttributeSet attr )
	{
		super( context, attr );

		Resources res = context.getResources();
		float dp = res.getDisplayMetrics().density;

		initFaces( context );
		initPaints( context, dp );
		initMetrics( context, dp, res );

		setScaleType( ScalingImageView.ScaleType.CENTER_CROP );
	}

	public Face[] getFaces()
	{
		saveSelectedFace();
		return faces.clone();
	}

	public void setSelectedFaceImage( Uri imageUri )
	{
		faces[selectedFace].uri = imageUri;
		setImageRotation( 0 );
		setImageFromUri( imageUri );
	}

	@Override
	public boolean onTouchEvent( MotionEvent event )
	{
		long now = System.currentTimeMillis();

		switch( event.getActionMasked() )
		{
			default:
				break;
			case MotionEvent.ACTION_DOWN:
				touchDownTime = now;
				break;
			case MotionEvent.ACTION_UP:
				if( now-touchDownTime <= tapTimeout )
				{
					touchDownTime = 0;

					if( selectFaceAt(
						event.getX(),
						event.getY() ) )
					{
						performClick();
						return true;
					}
				}
				break;
		}

		return super.onTouchEvent( event );
	}

	@Override
	protected void onDraw( Canvas canvas )
	{
		super.onDraw( canvas );

		RectF selectedBounds = null;

		for( int n = faces.length; n-- > 0; )
		{
			Face face = faces[n];
			RectF bounds = face.bounds;
			Bitmap bitmap = face.bitmap;

			if( n == selectedFace )
				selectedBounds = bounds;
			else if( bitmap != null )
				canvas.drawBitmap(
					bitmap,
					null,
					bounds,
					null );

			canvas.drawRect( bounds, unselectedPaint );
			canvas.drawText(
				face.label,
				bounds.left+textPadding,
				bounds.bottom-textPadding,
				textPaint );
		}

		if( selectedBounds != null )
			canvas.drawRect( selectedBounds, selectedPaint );
	}

	@Override
	protected Parcelable onSaveInstanceState()
	{
		saveSelectedFace();

		return new SavedState(
			super.onSaveInstanceState(),
			faces,
			selectedFace );
	}

	@Override
	protected void onRestoreInstanceState( Parcelable state )
	{
		if( state instanceof SavedState )
		{
			SavedState savedState = (SavedState)state;
			selectedFace = savedState.savedSelectedFace;

			for( int n = faces.length; n-- > 0; )
			{
				Face face = savedState.savedFaces[n];

				if( face.uri == null )
					continue;

				restoreFace( n, face );

				if( n == selectedFace )
				{
					setImageRotation( face.rotation );
					setImageFromUri( face.uri );
				}
			}

			state = savedState.getSuperState();
		}

		super.onRestoreInstanceState( state );
	}

	@Override
	protected void layoutImage(
		boolean changed,
		int left,
		int top,
		int right,
		int bottom )
	{
		if( changed )
			calculateFaceRects(
				left,
				top+toolAndStatusBarHeight,
				right-navigationBarSize.x,
				bottom-navigationBarSize.y );

		Face face = faces[selectedFace];
		setBounds( face.bounds );
		setImageMatrixForFace( face );
	}

	private void setImageMatrixForFace( Face face )
	{
		if( selectedBitmap == null )
			return;

		if( face.rotation != getImageRotation() ||
			face.clip == null )
		{
			center( getBounds() );
			saveSelectedFace();

			// matrix is already set in center()
			return;
		}

		if( face.matrix == null )
		{
			setImageRotation( face.rotation );
			setMatrixFromClip( face );
		}

		setImageMatrix( face.matrix );
	}

	private void initFaces( Context context )
	{
		for( int n = faces.length; n-- > 0; )
			faces[n] = new Face( context.getString( LABEL_ID[n] ) );
	}

	private void initPaints( Context context, float dp )
	{
		selectedPaint.setColor( ContextCompat.getColor(
			context,
			R.color.cube_face_selected ) );
		selectedPaint.setStyle( Paint.Style.STROKE );
		selectedPaint.setStrokeWidth( dp*2f );

		unselectedPaint.setColor( ContextCompat.getColor(
			context,
			R.color.crop_bound ) );
		unselectedPaint.setStyle( Paint.Style.STROKE );
		unselectedPaint.setPathEffect(
			new DashPathEffect( new float[]{
				10f*dp,
				10f*dp }, 0 ) );

		textPaint.setColor( ContextCompat.getColor(
			context,
			R.color.cube_face_text ) );
		textPaint.setTextSize( 14f*dp );
	}

	private void initMetrics( Context context, float dp, Resources res )
	{
		mapPadding = Math.round( 24f*dp );
		textPadding = Math.round( 8f*dp );
		toolAndStatusBarHeight =
			Preferences.getStatusAndToolBarHeight( context );
		Preferences.getNavigationBarHeight( res, navigationBarSize );
	}

	private void restoreFace( int idx, Face from )
	{
		if( from.uri == null ||
			from.clip == null )
			return;

		Bitmap bitmap = BitmapEditor.getBitmapFromUri(
			getContext(),
			from.uri,
			PREVIEW_SIZE );

		if( bitmap == null )
			return;

		Face face = faces[idx];

		face.bitmap = BitmapEditor.crop(
			bitmap,
			from.clip,
			from.rotation );

		face.uri = from.uri;
		face.clip = from.clip;
		face.rotation = from.rotation;
	}

	private void setMatrixFromClip( Face face )
	{
		face.matrix = getMatrixFromClip(
			selectedBitmap.getWidth(),
			selectedBitmap.getHeight(),
			face.bounds,
			face.clip,
			face.rotation );
	}

	private static Matrix getMatrixFromClip(
		int width,
		int height,
		RectF bounds,
		RectF normalizedClip,
		float rotation )
	{
		Matrix matrix = new Matrix();
		RectF dstRect = new RectF();

		matrix.setTranslate( width*-.5f, height*-.5f );
		matrix.postRotate( rotation );
		matrix.mapRect( dstRect, new RectF( 0f, 0f, width, height ) );

		float dw = dstRect.width();
		float dh = dstRect.height();
		float scale = bounds.width()/(normalizedClip.width()*dw);
		matrix.postScale( scale, scale );
		matrix.postTranslate(
			bounds.centerX()-(normalizedClip.centerX()-.5f)*dw*scale,
			bounds.centerY()-(normalizedClip.centerY()-.5f)*dh*scale );

		return matrix;
	}

	private void calculateFaceRects(
		int left,
		int top,
		int right,
		int bottom )
	{
		left += mapPadding;
		top += mapPadding;
		right -= mapPadding;
		bottom -= mapPadding;

		float viewWidth = right-left;
		float viewHeight = bottom-top;

		int cols = 2;
		int rows = 3;

		// prefer 3 by 2 for landscape layouts
		if( viewWidth > viewHeight )
		{
			cols = 3;
			rows = 2;
		}

		float mapWidth = cols;
		float mapHeight = rows;

		// calculate a map rectangle that fits inside the
		// view rectangle
		if( viewWidth*mapHeight > viewHeight*mapWidth )
		{
			mapWidth = mapWidth*viewHeight/mapHeight;
			mapHeight = viewHeight;
		}
		else
		{
			mapHeight = mapHeight*viewWidth/mapWidth;
			mapWidth = viewWidth;
		}

		// center map inside view
		{
			int h = Math.round( (viewWidth-mapWidth)/2f );
			int v = Math.round( (viewHeight-mapHeight)/2f );

			left += h;
			top += v;
			right -= h;
			// bottom is never used below this point
		}

		// lay out face bounds
		int faceSize = Math.round( mapWidth/cols );
		int x = left;
		int y = top;

		for( Face face : faces )
		{
			face.bounds.set(
				x,
				y,
				x+faceSize,
				y+faceSize );

			if( (x += faceSize) >= right )
			{
				x = left;
				y += faceSize;
			}
		}
	}

	private boolean selectFaceAt( float x, float y )
	{
		for( int n = 0, l = faces.length; n < l; ++n )
			if( faces[n].bounds.contains( x, y ) )
			{
				selectFace( n );
				return true;
			}

		return false;
	}

	private void selectFace( int n )
	{
		saveSelectedFace();

		if( selectedBitmap != null )
		{
			Face face = faces[selectedFace];
			face.bitmap = BitmapEditor.crop(
				selectedBitmap,
				face.clip,
				face.rotation );
		}

		selectedFace = n;
		Face face = faces[n];

		setBounds( face.bounds );
		setImageRotation( face.rotation );
		setImageFromUri( face.uri );

		if( face.matrix == null &&
			selectedBitmap != null )
			setMatrixFromClip( face );

		setImageMatrix( face.matrix );
		invalidate();
	}

	private void saveSelectedFace()
	{
		Face face = faces[selectedFace];

		if( selectedBitmap == null )
		{
			face.matrix = null;
			face.clip = null;
			face.rotation = 0;
			return;
		}

		face.matrix = new Matrix( getImageMatrix() );
		face.clip = getNormalizedRectInBounds();
		face.rotation = getImageRotation();
	}

	private void setImageFromUri( Uri uri )
	{
		selectedBitmap = uri != null ?
			BitmapEditor.getBitmapFromUri(
				getContext(),
				uri,
				PREVIEW_SIZE ) :
			null;

		setImageBitmap( selectedBitmap );
	}

	private static class SavedState extends BaseSavedState
	{
		private int savedSelectedFace;
		private Face savedFaces[];

		public static final Parcelable.Creator<SavedState> CREATOR =
			new Parcelable.Creator<SavedState>()
			{
				@Override
				public SavedState createFromParcel( Parcel in )
				{
					return new SavedState( in );
				}

				@Override
				public SavedState[] newArray( int size )
				{
					return new SavedState[size];
				}
			};

		@Override
		public void writeToParcel( Parcel out, int flags )
		{
			super.writeToParcel( out, flags );

			out.writeInt( savedSelectedFace );
			out.writeTypedArray( savedFaces, flags );
		}

		private SavedState(
			Parcelable superState,
			Face faces[],
			int selectedFace )
		{
			super( superState );

			savedSelectedFace = selectedFace;
			savedFaces = faces;
		}

		private SavedState( Parcel in )
		{
			super( in );

			savedSelectedFace = in.readInt();
			savedFaces = in.createTypedArray( Face.CREATOR );
		}
	}
}
