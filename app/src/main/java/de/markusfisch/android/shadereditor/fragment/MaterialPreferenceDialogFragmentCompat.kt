package de.markusfisch.android.shadereditor.fragment

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.preference.PreferenceDialogFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

abstract class MaterialPreferenceDialogFragmentCompat : PreferenceDialogFragmentCompat() {

    private var whichButtonClicked: Int = DialogInterface.BUTTON_NEGATIVE
    private var dialogClosedFromDismiss = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        whichButtonClicked = DialogInterface.BUTTON_NEGATIVE
        val preference = preference

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(preference.dialogTitle)
            .setIcon(preference.dialogIcon)
            .setPositiveButton(preference.positiveButtonText, this)
            .setNegativeButton(preference.negativeButtonText, this)

        val contentView = onCreateDialogView(requireContext())
        if (contentView != null) {
            onBindDialogView(contentView)
            builder.setView(contentView)
        } else {
            builder.setMessage(preference.dialogMessage)
        }

        onPrepareDialogBuilder(builder)
        return builder.create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        whichButtonClicked = which
    }

    override fun onDismiss(dialog: DialogInterface) {
        dialogClosedFromDismiss = true
        super.onDismiss(dialog)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (dialogClosedFromDismiss) {
            dialogClosedFromDismiss = false
            onMaterialDialogClosed(whichButtonClicked == DialogInterface.BUTTON_POSITIVE)
        } else {
            onMaterialDialogClosed(positiveResult)
        }
    }

    abstract fun onMaterialDialogClosed(positiveResult: Boolean)
}