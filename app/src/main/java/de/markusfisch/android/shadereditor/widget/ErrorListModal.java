package de.markusfisch.android.shadereditor.widget;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;
import java.util.Objects;

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
		View view = inflater.inflate(R.layout.error_list_bottom_sheet_content,
				container, false);
		RecyclerView errorList = view.findViewById(R.id.error_list);
		errorList.setLayoutManager(new LinearLayoutManager(requireContext()));
		DividerItemDecoration divider = new DividerItemDecoration(errorList.getContext(),
				DividerItemDecoration.VERTICAL);
		divider.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(requireContext(),
				R.drawable.divider_with_padding_horizontal)));
		errorList.addItemDecoration(divider);
		ErrorAdapter adapter = new ErrorAdapter(onItemClickListener);
		adapter.submitList(errors);
		errorList.setAdapter(adapter);
		return view;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		Objects.requireNonNull(dialog.getWindow()).addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
		return dialog;
	}
}
