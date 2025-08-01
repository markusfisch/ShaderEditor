package de.markusfisch.android.shadereditor.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;

public class ShaderSpinnerAdapter
		extends ShaderAdapter
		implements SpinnerAdapter {
	/**
	 * Constructs a new adapter for a spinner.
	 *
	 * @param context The current context.
	 */
	public ShaderSpinnerAdapter(Context context) {
		// Call the parent constructor, which only requires a Context.
		super(context);
	}

	/**
	 * This method is required by the SpinnerAdapter interface to display
	 * the items in the dropdown list.
	 *
	 * @param position    The position of the item within the adapter's data set.
	 * @param convertView The old view to reuse, if possible.
	 * @param parent      The parent that this view will eventually be attached to.
	 * @return A View that displays the data at the specified position.
	 */
	@Override
	public View getDropDownView(
			int position,
			View convertView,
			ViewGroup parent) {
		// We can reuse the parent's getView() implementation because the
		// layout for a selected item and a dropdown item is the same.
		return getView(position, convertView, parent);
	}
}