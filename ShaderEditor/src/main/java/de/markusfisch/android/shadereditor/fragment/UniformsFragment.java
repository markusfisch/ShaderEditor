package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.activity.UniformsActivity;
import de.markusfisch.android.shadereditor.adapter.UniformsAdapter;
import de.markusfisch.android.shadereditor.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class UniformsFragment extends Fragment
{
	private UniformsAdapter uniformsAdapter;
	private ListView listView;

	@Override
	public View onCreateView(
		LayoutInflater inflater,
		ViewGroup container,
		Bundle state )
	{
		Activity activity;

		if( (activity = getActivity()) == null )
			return null;

		View view;

		if( (view = inflater.inflate(
				R.layout.fragment_uniforms,
				container,
				false )) == null ||
			(listView = (ListView)view.findViewById(
				R.id.uniforms )) == null )
		{
			activity.finish();
			return null;
		}

		initListView( activity );

		return view;
	}

	private void initListView( Context context )
	{
		uniformsAdapter = new UniformsAdapter( context );

		listView.setAdapter( uniformsAdapter );
		listView.setOnItemClickListener(
			new AdapterView.OnItemClickListener()
			{
				@Override
				public void onItemClick(
					AdapterView<?> parent,
					View view,
					int position,
					long id )
				{
					Activity activity = getActivity();

					if( activity == null )
						return;

					UniformsActivity.setAddUniformResult(
						activity,
						uniformsAdapter.getItem( position ) );

					activity.finish();
				}
			} );
	}
}
