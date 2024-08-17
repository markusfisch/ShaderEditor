package de.markusfisch.android.shadereditor.activity

import androidx.fragment.app.Fragment
import de.markusfisch.android.shadereditor.fragment.PreferencesFragment

class PreferencesActivity : AbstractContentActivity() {
    override fun defaultFragment(): Fragment {
        return PreferencesFragment()
    }
}
