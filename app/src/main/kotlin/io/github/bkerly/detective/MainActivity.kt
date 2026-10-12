package io.github.bkerly.detective

import android.app.Activity
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager

/**
 * The only screen of the Android app. It shows one full-screen view; all of
 * the game is in the :core module, which knows nothing about Android.
 */
class MainActivity : Activity() {
    private lateinit var view: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        view = GameView(this)
        setContentView(view)
        hideBars()
    }

    /** Hide the status bar and the back/home bar so the game fills the tablet. */
    @Suppress("DEPRECATION")
    private fun hideBars() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideBars()
    }

    override fun onResume() {
        super.onResume()
        view.resume()
    }

    override fun onPause() {
        view.pause()
        super.onPause()
    }

    override fun onDestroy() {
        view.release()
        super.onDestroy()
    }

    @Deprecated("Still the simplest way to catch Back on the old tablets this app is for.")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (!view.back()) super.onBackPressed()
    }
}
