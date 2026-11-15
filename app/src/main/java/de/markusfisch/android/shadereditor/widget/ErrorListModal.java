package de.markusfisch.android.shadereditor.widget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.markusfisch.android.shadereditor.R;
import de.markusfisch.android.shadereditor.adapter.ErrorAdapter;
import de.markusfisch.android.shadereditor.opengl.ShaderError;

public class ErrorListModal extends BottomSheetDialogFragment {
	public static final String TAG = "ErrorListModal";
	public static final String RESULT = "shader_error_line";
	public static final String LINE = "line";
	private static final String ERRORS = "errors";

	public static ErrorListModal newInstance(@NonNull List<ShaderError> errors) {
		Bundle args = new Bundle();
		args.putSerializable(ERRORS, new ArrayList<>(errors));
		ErrorListModal modal = new ErrorListModal();
		modal.setArguments(args);
		return modal;
	}

	@SuppressWarnings("deprecation")
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
		ErrorAdapter adapter = new ErrorAdapter(lineNumber -> {
			Bundle result = new Bundle();
			result.putInt(LINE, lineNumber);
			getParentFragmentManager().setFragmentResult(RESULT, result);
			dismiss();
		});
		@SuppressWarnings("unchecked")
		List<ShaderError> errors = (List<ShaderError>) requireArguments().getSerializable(ERRORS);
		adapter.submitList(errors);
		errorList.setAdapter(adapter);
		return view;
	}
}