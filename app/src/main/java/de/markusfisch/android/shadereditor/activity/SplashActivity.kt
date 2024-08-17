package de.markusfisch.android.shadereditor.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // It's important _not_ to inflate a layout file here
        // because that would happen after the app is fully initialized what is too late.

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
