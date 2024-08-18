package de.markusfisch.android.shadereditor.fragment

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.activity.AbstractSubsequentActivity
import de.markusfisch.android.shadereditor.app.ShaderEditorApp
import de.markusfisch.android.shadereditor.database.Database
import de.markusfisch.android.shadereditor.widget.ScalingImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TextureViewFragment : Fragment() {

    interface ScalingImageViewProvider {
        fun getScalingImageView(): ScalingImageView
    }

    companion object {
        const val TEXTURE_ID = "texture_id"
        const val SAMPLER_TYPE = "sampler_type"
    }

    private lateinit var imageView: ScalingImageView
    private var textureId: Long = 0L
    private lateinit var textureName: String
    private lateinit var samplerType: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val activity = requireActivity()

        imageView = (activity as? ScalingImageViewProvider)?.getScalingImageView()
            ?: throw IllegalArgumentException("$activity must implement ScalingImageViewProvider")

        val args = arguments ?: return finishFragmentWithError(activity)
        textureId = args.getLong(TEXTURE_ID, -1)
        val samplerType = args.getString(SAMPLER_TYPE)

        if (textureId < 1 || samplerType == null) {
            return finishFragmentWithError(activity)
        }
        this.samplerType = samplerType

        val cursor = ShaderEditorApp.db.getTexture(textureId)
        if (Database.closeIfEmpty(cursor)) {
            removeInvalidTexture(activity)
            cursor.close()
            return finishFragmentWithError(activity)
        }

        setupImageView(cursor)
        cursor.close()

        val view = inflater.inflate(R.layout.fragment_view_texture, container, false)
        view.findViewById<View>(R.id.insert_code).setOnClickListener {
            insertUniformSamplerStatement()
        }

        activity.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_view_texture, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.remove_texture) {
                    askToRemoveTexture(textureId)
                    return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return view
    }

    private fun finishFragmentWithError(context: Context): View? {
        Toast.makeText(context, R.string.removed_invalid_texture, Toast.LENGTH_LONG).show()
        requireActivity().finish()
        return null
    }

    private fun setupImageView(cursor: Cursor) {
        textureName = runCatching {
            imageView.apply {
                visibility = View.VISIBLE
                setImageBitmap(ShaderEditorApp.db.getTextureBitmap(cursor))
            }
            Database.getString(cursor, Database.TEXTURES_NAME)
        }.getOrNull() ?: getString(R.string.image_too_big)

        requireActivity().title = textureName
    }

    private fun removeInvalidTexture(activity: Activity) {
        ShaderEditorApp.db.removeTexture(textureId)
        Toast.makeText(activity, R.string.removed_invalid_texture, Toast.LENGTH_LONG).show()
    }

    private fun askToRemoveTexture(id: Long) {
        MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.remove_texture)
            .setMessage(R.string.sure_remove_texture)
            .setPositiveButton(android.R.string.ok) { _, _ -> removeTextureAsync(id) }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun removeTextureAsync(id: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            ShaderEditorApp.db.removeTexture(id)
            withContext(Dispatchers.Main) {
                requireActivity().finish()
            }
        }
    }

    private fun insertUniformSamplerStatement() {
        imageView.visibility = View.GONE
        AbstractSubsequentActivity.addFragment(
            parentFragmentManager, TextureParametersFragment.newInstance(samplerType, textureName)
        )
    }
}