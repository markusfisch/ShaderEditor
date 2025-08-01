package de.markusfisch.android.shadereditor.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;

public class TextureSpinnerAdapter
		extends TextureAdapter
		implements SpinnerAdapter {

	/**
	 * Creates a new adapter for a texture spinner.
	 *
	 * @param context The current context.
	 */
	public TextureSpinnerAdapter(Context context) {
		// The parent constructor only needs the context.
		super(context);
	}

	/**
	 * Provides the view for an item in the spinner's dropdown list.
	 *
	 * @param position    The position of the item in the data set.
	 * @param convertView The old view to reuse, if possible.
	 *                    M @param parent      The parent view that this view will eventually be
	 *                    attached to.
	 * @return A View corresponding to the data at the specified position.
	 */
	@Override
	public View getDropDownView(
			int position,
			View convertView,
			ViewGroup parent) {
		// We can reuse the parent's getView() implementation because the
		// layout is the same for the selected item and the dropdown items.
		return getView(position, convertView, parent);
	}
}