package de.markusfisch.android.shadereditor.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import de.markusfisch.android.shadereditor.R

abstract class AbstractContentActivity : AbstractSubsequentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subsequent)

        initSystemBars(this)
        initToolbar(this)

        if (savedInstanceState == null) {
            setFragment(
                supportFragmentManager, defaultFragment()
            )
        }
    }

    protected abstract fun defaultFragment(): Fragment
}
