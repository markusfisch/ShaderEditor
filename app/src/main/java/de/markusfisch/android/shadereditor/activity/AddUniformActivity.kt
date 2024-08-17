package de.markusfisch.android.shadereditor.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuInflater
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.fragment.TextureViewFragment
import de.markusfisch.android.shadereditor.fragment.UniformPagesFragment
import de.markusfisch.android.shadereditor.widget.SearchMenu

class AddUniformActivity : AbstractContentActivity() {
    companion object {
        const val STATEMENT = "statement"

        @JvmStatic
        fun setAddUniformResult(activity: Activity, name: String) {
            val bundle = Bundle().apply {
                putString(STATEMENT, name)
            }

            val data = Intent().apply {
                putExtras(bundle)
            }

            activity.setResult(Activity.RESULT_OK, data)
        }
    }

    private var onSearchListener: SearchMenu.OnSearchListener? = null

    private var currentSearchQuery: String? = null

    fun getCurrentSearchQuery(): String? = currentSearchQuery

    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var cropImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickTextureLauncher: ActivityResultLauncher<Intent>

    fun setSearchListener(onSearchListener: SearchMenu.OnSearchListener) {
        this.onSearchListener = onSearchListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: android.view.Menu, menuInflater: MenuInflater) {
                SearchMenu.addSearchMenu(menu, menuInflater) { value ->
                    currentSearchQuery = value
                    onSearchListener?.filter(value)
                }
            }

            override fun onMenuItemSelected(menuItem: android.view.MenuItem) = false
        }, this, Lifecycle.State.RESUMED)

        // Register the ActivityResultLaunchers
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { imageUri ->
                    val cropIntent = CropImageActivity.getIntentForImage(this, imageUri)
                    cropImageLauncher.launch(cropIntent)
                }
            }
        }

        cropImageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK, result.data)
                finish()
            }
        }

        pickTextureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK, result.data)
                finish()
            }
        }

        // Handle any intents passed to this activity
        startActivityForIntent(intent)
    }

    override fun defaultFragment(): Fragment {
        return UniformPagesFragment()
    }

    private fun startActivityForIntent(intent: Intent?) {
        intent ?: return

        val type = intent.type
        if (Intent.ACTION_SEND == intent.action && type?.startsWith("image/") == true) {

            val imageUri =
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            imageUri?.let {
                val cropIntent = CropImageActivity.getIntentForImage(this, it)
                cropImageLauncher.launch(cropIntent)
            }
        }
    }

    fun startPickImage() {
        val pickImageIntent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        pickImageLauncher.launch(
            Intent.createChooser(
                pickImageIntent, getString(R.string.choose_image)
            )
        )
    }

    fun startPickTexture(id: Long, samplerType: String) {
        val pickTextureIntent = Intent(this, TextureViewActivity::class.java).apply {
            putExtra(TextureViewFragment.TEXTURE_ID, id)
            putExtra(TextureViewFragment.SAMPLER_TYPE, samplerType)
        }
        pickTextureLauncher.launch(pickTextureIntent)
    }
}