package de.markusfisch.android.shadereditor.widget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.adapter.ErrorAdapter;
import de.markusfisch.android.shadereditor.opengl.ShaderError;

public class ErrorListModal extends BottomSheetDialogFragment {
	public static final String TAG = "ErrorListModal";
	@NonNull
	private final ErrorAdapter.OnItemClickListener onItemClickListener;

	@NonNull
	final List<ShaderError> errors;

	public ErrorListModal(@NonNull List<ShaderError> errors,
			@NonNull ErrorAdapter.OnItemClickListener onItemClickListener) {
		this.errors = errors;
		this.onItemClickListener = (lineNumber) -> {
			onItemClickListener.onItemClick(lineNumber);
			dismiss();
		};
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		View view =inflater.inflate(R.layout.error_list_bottom_sheet_content,
						container, false);
		ListView errorList = view.findViewById(R.id.error_list);
		errorList.setAdapter(new ErrorAdapter(requireContext(), errors, onItemClickListener));
		return view;
	}
}
