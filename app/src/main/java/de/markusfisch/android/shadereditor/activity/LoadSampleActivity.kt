package de.markusfisch.android.shadereditor.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import de.markusfisch.android.shadereditor.fragment.LoadSampleFragment

class LoadSampleActivity : AbstractContentActivity() {

    companion object {
        const val NAME = "name"
        const val RESOURCE_ID = "resource_id"
        const val THUMBNAIL_ID = "thumbnail_id"
        const val QUALITY = "quality"

        @JvmStatic
        fun setSampleResult(
            activity: Activity, name: String, resId: Int, thumbId: Int, quality: Float
        ) {
            val bundle = Bundle().apply {
                putString(NAME, name)
                putInt(RESOURCE_ID, resId)
                putInt(THUMBNAIL_ID, thumbId)
                putFloat(QUALITY, quality)
            }

            val data = Intent().apply {
                putExtras(bundle)
            }

            activity.setResult(Activity.RESULT_OK, data)
        }
    }

    override fun defaultFragment(): Fragment {
        return LoadSampleFragment()
    }
}