package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.activity.AddUniformActivity;
import de.markusfisch.android.shadereditor.adapter.PresetUniformAdapter;
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

public class UniformPresetPageFragment extends Fragment
{
	private PresetUniformAdapter uniformsAdapter;
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
				R.layout.fragment_uniform_preset_page,
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
		uniformsAdapter = new PresetUniformAdapter( context );

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

					AddUniformActivity.setAddUniformResult(
						activity,
						uniformsAdapter.getItem( position ) );

					activity.finish();
				}
			} );
	}
}
