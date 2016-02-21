package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.activity.TexturesActivity;
import de.markusfisch.android.shadereditor.app.ShaderEditorApplication;
import de.markusfisch.android.shadereditor.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TexturePropertiesFragment extends Fragment
{
	public static final String TEXTURE_NAME = "[a-zA-Z0-9_]+";

	private static final String IMAGE_URI = "image_uri";
	private static final String CROP_RECT = "crop_rect";
	private static final String ROTATION = "rotation";
	private static final Pattern NAME_PATTERN = Pattern.compile(
		"^"+TEXTURE_NAME+"$" );

	private static boolean inProgress = false;

	private InputMethodManager imm;
	private SeekBar sizeBarView;
	private TextView sizeView;
	private EditText nameView;
	private CheckBox addUniformView;
	private View progressView;
	private Uri imageUri;
	private RectF cropRect;
	private float imageRotation;

	public static Fragment newInstance(
		Uri uri,
		RectF rect,
		float rotation )
	{
		Bundle args = new Bundle();
		args.putParcelable( IMAGE_URI, uri );
		args.putParcelable( CROP_RECT, rect );
		args.putFloat( ROTATION, rotation );

		TexturePropertiesFragment fragment =
			new TexturePropertiesFragment();
		fragment.setArguments( args );

		return fragment;
	}

	@Override
	public void onCreate( Bundle state )
	{
		super.onCreate( state );

		setHasOptionsMenu( true );
	}

	@Override
	public View onCreateView(
		LayoutInflater inflater,
		ViewGroup container,
		Bundle state )
	{
		Activity activity;

		if( (activity = getActivity()) == null )
			return null;

		activity.setTitle( R.string.texture_properties );

		Bundle args;
		View view;

		if( (args = getArguments()) == null ||
			(imageUri = (Uri)args.getParcelable(
				IMAGE_URI )) == null ||
			(cropRect = (RectF)args.getParcelable(
				CROP_RECT )) == null ||
			(view = inflater.inflate(
				R.layout.fragment_texture_properties,
				container,
				false )) == null ||
			(sizeBarView = (SeekBar)view.findViewById(
				R.id.size_bar )) == null ||
			(sizeView = (TextView)view.findViewById(
				R.id.size )) == null ||
			(nameView = (EditText)view.findViewById(
				R.id.name )) == null ||
			(addUniformView = (CheckBox)view.findViewById(
				R.id.should_add_uniform )) == null ||
			(progressView = view.findViewById(
				R.id.progress_view )) == null )
		{
			activity.finish();
			return null;
		}

		imageRotation = args.getFloat( ROTATION );

		imm = (InputMethodManager)activity.getSystemService(
			Context.INPUT_METHOD_SERVICE );

		if( activity.getCallingActivity() == null )
		{
			addUniformView.setVisibility( View.GONE );
			addUniformView.setChecked( false );
		}

		initSizeView();
		initNameView();

		return view;
	}

	@Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
	{
		inflater.inflate(
			R.menu.fragment_texture_properties,
			menu );
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		switch( item.getItemId() )
		{
			case R.id.save:
				saveTextureAsync();
				return true;
			default:
				return super.onOptionsItemSelected( item );
		}
	}

	private void initSizeView()
	{
		setSizeView( sizeBarView.getProgress() );
		sizeBarView.setOnSeekBarChangeListener(
			new SeekBar.OnSeekBarChangeListener()
			{
				int progress = 0;

				@Override
				public void onProgressChanged(
					SeekBar seekBar,
					int progresValue,
					boolean fromUser )
				{
					setSizeView( progresValue );
				}

				@Override
				public void onStartTrackingTouch(
					SeekBar seekBar )
				{
				}

				@Override
				public void onStopTrackingTouch(
					SeekBar seekBar )
				{
				}
			} );
	}

	private void setSizeView( int power )
	{
		int size = getPower( power );

		sizeView.setText( String.format(
			"%d x %d",
			size,
			size ) );
	}

	private void initNameView()
	{
		nameView.setFilters( new InputFilter[]{
			new InputFilter()
			{
				@Override
				public CharSequence filter(
					CharSequence source,
					int start,
					int end,
					Spanned dest,
					int dstart,
					int dend )
				{
					return NAME_PATTERN
						.matcher( source )
						.find() ? null : "";
				}
			} } );
	}

	private void saveTextureAsync()
	{
		final Context context = getActivity();

		if( context == null ||
			inProgress )
			return;

		final String name = nameView.getText().toString();

		if( name == null ||
			name.trim().length() < 1 )
		{
			Toast.makeText(
				context,
				R.string.missing_name,
				Toast.LENGTH_SHORT ).show();

			return;
		}
		else if(
			!name.matches( TEXTURE_NAME ) ||
			name.equals( "backbuffer" ) )
		{
			Toast.makeText(
				context,
				R.string.invalid_texture_name,
				Toast.LENGTH_SHORT ).show();

			return;
		}

		imm.hideSoftInputFromWindow(
			nameView.getWindowToken(),
			0 );

		final int size = getPower( sizeBarView.getProgress() );

		inProgress = true;
		progressView.setVisibility( View.VISIBLE );

		new AsyncTask<Void, Void, Integer>()
		{
			@Override
			protected Integer doInBackground( Void... nothings )
			{
				return saveTexture(
					// try to get a bigger source image in
					// case the cut out is quite small
					CropImageFragment.getBitmapFromUri(
						context,
						imageUri,
						// which doesn't work for some devices;
						// 2048 is too much => out of memory
						1024 ),
					cropRect,
					imageRotation,
					name,
					size );
			}

			@Override
			protected void onPostExecute( Integer messageId )
			{
				inProgress = false;
				progressView.setVisibility( View.GONE );

				Activity activity = getActivity();

				if( activity == null )
					return;

				if( messageId > 0 )
				{
					Toast.makeText(
						activity,
						messageId,
						Toast.LENGTH_SHORT ).show();

					return;
				}

				if( addUniformView.isChecked() )
					TexturesActivity.setAddUniformResult(
						activity,
						name );

				activity.finish();
			}
		}.execute();
	}

	private int saveTexture(
		Bitmap bitmap,
		RectF rect,
		float rotation,
		String name,
		int size )
	{
		if( bitmap == null )
			return 0;

		System.gc();

		try
		{
			if( rotation % 360 != 0 )
			{
				Matrix matrix = new Matrix();
				matrix.setRotate( rotation );

				bitmap = Bitmap.createBitmap(
					bitmap,
					0,
					0,
					bitmap.getWidth(),
					bitmap.getHeight(),
					matrix,
					true );
			}

			float w = bitmap.getWidth();
			float h = bitmap.getHeight();

			bitmap = Bitmap.createBitmap(
				bitmap,
				Math.round( rect.left*w ),
				Math.round( rect.top*h ),
				Math.round( rect.width()*w ),
				Math.round( rect.height()*h ) );
		}
		catch( IllegalArgumentException e )
		{
			return R.string.illegal_rectangle;
		}
		catch( OutOfMemoryError e )
		{
			return R.string.out_of_memory;
		}

		if( ShaderEditorApplication
			.dataSource
			.insertTexture(
				name,
				Bitmap.createScaledBitmap(
					bitmap,
					size,
					size,
					true ) ) < 1 )
			return R.string.name_already_taken;

		return 0;
	}

	private static int getPower( int power )
	{
		return 1 << (power+1);
	}
}
