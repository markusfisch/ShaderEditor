package de.markusfisch.android.shadereditor.fragment

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.app.ShaderEditorApp
import de.markusfisch.android.shadereditor.opengl.ShaderError
import de.markusfisch.android.shadereditor.view.SoftKeyboard
import de.markusfisch.android.shadereditor.view.UndoRedo
import de.markusfisch.android.shadereditor.widget.ErrorListModal
import de.markusfisch.android.shadereditor.widget.ShaderEditor

class EditorFragment : Fragment() {

    companion object {
        const val TAG = "EditorFragment"
    }

    private lateinit var editorContainer: View
    private lateinit var shaderEditor: ShaderEditor
    private lateinit var undoRedo: UndoRedo

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_editor, container, false)
        editorContainer = view.findViewById(R.id.editor_container)
        shaderEditor = view.findViewById(R.id.editor)

        // Set up preferences and listeners
        setShowLineNumbers(ShaderEditorApp.preferences.showLineNumbers())
        undoRedo = UndoRedo(shaderEditor, ShaderEditorApp.editHistory)

        val activity = requireActivity()
        checkActivityImplementsListeners(activity)

        return view
    }

    private fun checkActivityImplementsListeners(activity: Activity) {
        if (activity is ShaderEditor.OnTextChangedListener) {
            shaderEditor.setOnTextChangedListener(activity)
            shaderEditor.setOnCompletionsListener(activity as ShaderEditor.CodeCompletionListener)
        } else {
            throw ClassCastException("$activity must implement ShaderEditor.OnTextChangedListener")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        undoRedo.detachListener()
    }

    override fun onResume() {
        super.onResume()
        updateToPreferences()
        undoRedo.listenForChanges() // Start listening for changes after content is restored
    }

    fun undo() = undoRedo.undo()

    fun canUndo() = undoRedo.canUndo()

    fun redo() = undoRedo.redo()

    fun canRedo() = undoRedo.canRedo()

    fun hasErrors() = shaderEditor.hasErrors()

    fun clearError() {
        shaderEditor.errors = emptyList()
    }

    fun updateHighlighting() = shaderEditor.updateHighlighting()

    fun highlightErrors() = shaderEditor.updateErrorHighlighting()

    var errors: List<ShaderError>
        get() = shaderEditor.errors
        set(value) {
            shaderEditor.errors = value
            highlightErrors()
        }

    fun showErrors() {
        val errors = shaderEditor.errors
        ErrorListModal(errors, this::navigateToLine).show(parentFragmentManager, ErrorListModal.TAG)
    }

    private fun navigateToLine(lineNumber: Int) = shaderEditor.navigateToLine(lineNumber)

    val isModified: Boolean
        get() = shaderEditor.isModified

    var text: String
        get() = shaderEditor.cleanText
        set(value) {
            clearError()
            undoRedo.clearHistory()
            undoRedo.stopListeningForChanges()
            shaderEditor.setTextHighlighted(value)
            undoRedo.listenForChanges()
        }


    fun insert(text: CharSequence) = shaderEditor.insert(text)

    fun addUniform(name: String) = shaderEditor.addUniform(name)

    val isCodeVisible: Boolean
        get() = editorContainer.visibility == View.VISIBLE

    fun toggleCode(): Boolean {
        val visible = isCodeVisible
        editorContainer.visibility = if (visible) View.GONE else View.VISIBLE
        if (visible) {
            SoftKeyboard.hide(requireActivity(), shaderEditor)
        }
        return visible
    }

    private fun updateToPreferences() {
        val preferences = ShaderEditorApp.preferences
        shaderEditor.apply {
            setUpdateDelay(preferences.updateDelay)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, preferences.textSize.toFloat())
            val font = preferences.font
            typeface = font
            val features = fontFeatureSettings
            if (font != Typeface.MONOSPACE || features != null) {
                fontFeatureSettings =
                    if (font == Typeface.MONOSPACE) null else if (preferences.useLigatures()) "normal" else "calt off"
            }
        }
    }

    fun setShowLineNumbers(showLineNumbers: Boolean) =
        shaderEditor.setShowLineNumbers(showLineNumbers)
}