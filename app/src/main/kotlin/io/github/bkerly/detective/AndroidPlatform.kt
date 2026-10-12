package io.github.bkerly.detective

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import detective.core.Assets
import detective.core.AudioOut
import detective.core.Store
import java.io.IOException

/** Reads text files from the app's assets folder. */
class AndroidAssets(private val context: Context) : Assets {
    override fun text(path: String): String? = try {
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
    } catch (e: IOException) {
        null
    }
}

/** Keeps the saved game in the app's private settings. */
class AndroidStore(context: Context) : Store {
    private val prefs = context.getSharedPreferences("detective", Context.MODE_PRIVATE)
    override fun load(key: String): String? = prefs.getString(key, null)
    override fun save(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}

/**
 * Voice clips play one at a time through a MediaPlayer; a new clip stops the
 * one before. Short sound effects are loaded once into a SoundPool.
 */
class AndroidAudio(private val context: Context) : AudioOut {
    private var player: MediaPlayer? = null
    private val pool: SoundPool
    private val sounds = HashMap<String, Int>()

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
        for (name in arrayOf("tap", "ok", "no", "bump", "stamp", "win")) {
            try {
                val fd = context.assets.openFd("sfx/$name.ogg")
                sounds[name] = pool.load(fd, 1)
                fd.close()
            } catch (e: IOException) {
                // a missing effect is silent, not fatal
            }
        }
    }

    override fun voice(path: String) {
        stopVoice()
        try {
            val fd = context.assets.openFd("$path.ogg")
            val mp = MediaPlayer()
            mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            fd.close()
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { done ->
                done.release()
                if (player === done) player = null
            }
            mp.setOnErrorListener { bad, _, _ ->
                bad.release()
                if (player === bad) player = null
                true
            }
            player = mp
            mp.prepareAsync()
        } catch (e: Exception) {
            // a missing clip is silent; the desktop check reports missing clips
            player = null
        }
    }

    override fun stopVoice() {
        val p = player ?: return
        player = null
        try {
            p.reset()
        } catch (e: IllegalStateException) {
            // already finished
        }
        p.release()
    }

    override fun sfx(name: String) {
        val id = sounds[name] ?: return
        pool.play(id, 0.6f, 0.6f, 1, 0, 1f)
    }

    fun release() {
        stopVoice()
        pool.release()
    }
}
