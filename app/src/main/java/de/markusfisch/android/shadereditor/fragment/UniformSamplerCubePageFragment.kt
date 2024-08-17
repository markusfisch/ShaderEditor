package de.markusfisch.android.shadereditor.fragment

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import de.markusfisch.android.shadereditor.activity.CubeMapActivity
import de.markusfisch.android.shadereditor.app.ShaderEditorApp

class UniformSamplerCubePageFragment : UniformSampler2dPageFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSamplerType(AbstractSamplerPropertiesFragment.SAMPLER_CUBE)
    }

    override fun addTexture() {
        val activity = activity ?: return
        startActivity(Intent(activity, CubeMapActivity::class.java))
    }

    override fun loadTextures(): Cursor {
        return ShaderEditorApp.db.getSamplerCubeTextures(searchQuery)
    }
}