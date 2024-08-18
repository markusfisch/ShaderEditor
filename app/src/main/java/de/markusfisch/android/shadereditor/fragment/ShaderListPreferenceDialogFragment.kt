package de.markusfisch.android.shadereditor.fragment

import android.database.Cursor
import android.database.MatrixCursor
import android.database.MergeCursor
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.adapter.ShaderSpinnerAdapter
import de.markusfisch.android.shadereditor.app.ShaderEditorApp
import de.markusfisch.android.shadereditor.database.Database
import de.markusfisch.android.shadereditor.preference.Preferences

class ShaderListPreferenceDialogFragment : MaterialPreferenceDialogFragmentCompat() {

    private var adapter: ShaderSpinnerAdapter? = null

    companion object {
        @JvmStatic
        fun newInstance(key: String) = ShaderListPreferenceDialogFragment().apply {
            arguments = bundleOf(ARG_KEY to key)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        closeCursor()
    }

    override fun onMaterialDialogClosed(positiveResult: Boolean) {
        closeCursor()
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        val key = preference.key
        var cursor = ShaderEditorApp.db.shaders

        if (Preferences.DEFAULT_NEW_SHADER == key) {
            cursor = addEmptyItem(cursor)
        }

        adapter = ShaderSpinnerAdapter(requireContext(), cursor)

        builder.setSingleChoiceItems(adapter, 0) { dialog, which ->
            when (key) {
                Preferences.WALLPAPER_SHADER -> {
                    ShaderEditorApp.preferences.setWallpaperShader(adapter?.getItemId(which) ?: 0L)
                }

                Preferences.DEFAULT_NEW_SHADER -> {
                    ShaderEditorApp.preferences.setDefaultNewShader(adapter?.getItemId(which) ?: 0L)
                }
            }

            onClick(dialog, AlertDialog.BUTTON_POSITIVE)
            dialog.dismiss()
        }

        builder.setPositiveButton(null, null)
    }

    private fun closeCursor() {
        adapter?.changeCursor(null)
        adapter = null
    }

    private fun addEmptyItem(cursor: Cursor): Cursor {
        val matrixCursor = MatrixCursor(
            arrayOf(
                Database.SHADERS_ID,
                Database.SHADERS_THUMB,
                Database.SHADERS_NAME,
                Database.SHADERS_MODIFIED
            )
        )

        matrixCursor.addRow(arrayOf(0, null, getString(R.string.no_shader_selected), null))

        return MergeCursor(arrayOf(matrixCursor, cursor))
    }
}