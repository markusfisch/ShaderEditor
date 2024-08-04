package de.markusfisch.android.shadereditor.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public abstract class MaterialPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {
	private int mWhichButtonClicked = 0;
	private boolean onDialogClosedWasCalledFromOnDismiss = false;

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE;
		final DialogPreference preference = getPreference();
		final AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireContext())
				.setTitle(preference.getDialogTitle())
				.setIcon(preference.getDialogIcon())
				.setPositiveButton(preference.getPositiveButtonText(), this)
				.setNegativeButton(preference.getNegativeButtonText(), this);

		View contentView = onCreateDialogView(requireContext());
		if (contentView != null) {
			onBindDialogView(contentView);
			builder.setView(contentView);
		} else {
			builder.setMessage(preference.getDialogMessage());
		}

		onPrepareDialogBuilder(builder);

		// inputMethod handling removed
		return builder.create();
	}

	@Override
	public void onClick(@NonNull DialogInterface dialog, int which) {
		mWhichButtonClicked = which;
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		onDialogClosedWasCalledFromOnDismiss = true;
		super.onDismiss(dialog);
	}

	@Override
	public void onDialogClosed(boolean positiveResult) {
		if (onDialogClosedWasCalledFromOnDismiss) {
			onDialogClosedWasCalledFromOnDismiss = false;
			onMaterialDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE);
		} else {
			onMaterialDialogClosed(positiveResult);
		}
	}

	abstract void onMaterialDialogClosed(boolean positiveResult);
}
