package de.markusfisch.android.shadereditor.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.app.ShaderEditorApp
import de.markusfisch.android.shadereditor.view.SystemBarMetrics

abstract class AbstractSubsequentActivity : AppCompatActivity() {
    private lateinit var fm: FragmentManager

    companion object {
        @JvmStatic
        fun addFragment(fm: FragmentManager, fragment: Fragment) {
            getReplaceFragmentTransaction(fm, fragment).addToBackStack(null).commit()
        }

        @JvmStatic
        fun setFragment(fm: FragmentManager, fragment: Fragment) {
            getReplaceFragmentTransaction(fm, fragment).commit()
        }

        @JvmStatic
        fun initToolbar(activity: AppCompatActivity) {
            val toolbar = activity.findViewById<Toolbar>(R.id.toolbar)
            activity.setSupportActionBar(toolbar)

            activity.supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
            }
        }

        @JvmStatic
        fun initSystemBars(activity: AppCompatActivity) {
            SystemBarMetrics.setSystemBarColor(
                activity.window, ShaderEditorApp.preferences.systemBarColor, false
            )
        }

        @JvmStatic
        @SuppressLint("CommitTransaction")
        private fun getReplaceFragmentTransaction(
            fm: FragmentManager,
            fragment: Fragment
        ): FragmentTransaction {
            return fm.beginTransaction().replace(R.id.content_frame, fragment)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (fm.backStackEntryCount > 0) {
            fm.popBackStack()
        } else {
            finish()
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fm = supportFragmentManager
    }

    protected fun setFragmentForIntent(fragment: Fragment?, intent: Intent?) {
        if (fragment == null || intent == null) {
            finish()
            return
        }
        fragment.arguments = intent.extras
        setFragment(supportFragmentManager, fragment)
    }
}