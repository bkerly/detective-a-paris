package io.github.bkerly.detective

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import detective.core.Ears
import detective.core.Heard
import detective.core.HeardWord
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * The microphone, with Vosk: an offline French recogniser. It is told to listen
 * only for the phrase she is trying to say (plus "[unk]" for anything else), which
 * makes it far kinder to a child's voice and an English accent.
 *
 * The model lives in assets/model-fr (fetch it with tools/get_speech_model.R).
 * On first launch it is copied out of the app in the background; until then, or
 * if it is missing, the game asks her to say each phrase aloud and tap.
 *
 * Each try is written to speech-log.tsv in the app's files folder
 * (Android/data/io.github.bkerly.detective/files/ over USB), so the cut-offs in
 * core's Speech object can be set from her real voice.
 */
class AndroidEars(private val activity: Activity) : Ears {
    private var model: Model? = null
    private var vocab: HashSet<String>? = null
    private var service: SpeechService? = null
    private var recognizer: Recognizer? = null
    private var pending: ((Heard) -> Unit)? = null
    private var cannot: List<String> = emptyList()
    private var broken = false

    init {
        try {
            activity.assets.open("model-fr/uuid").close()
            StorageService.unpack(activity, "model-fr", "model",
                { m: Model ->
                    model = m
                    vocab = readVocab()
                },
                { _: IOException -> broken = true })
        } catch (e: IOException) {
            broken = true
        }
    }

    /** The model's word list, if it has one, so words it cannot know are left out. */
    private fun readVocab(): HashSet<String>? {
        val dir = File(activity.getExternalFilesDir(null), "model/model-fr")
        val f = File(dir, "graph/words.txt")
        if (!f.exists()) return null
        val out = HashSet<String>()
        try {
            f.forEachLine { ln ->
                val w = ln.substringBefore(' ')
                if (w.isNotEmpty()) out.add(w)
            }
        } catch (e: IOException) {
            return null
        }
        return out
    }

    override fun ready(): Boolean = model != null && !broken

    override fun listen(phrase: String, words: List<String>, done: (Heard) -> Unit) {
        val m = model
        if (m == null || broken) { done(Heard(emptyList(), "none")); return }
        if (Build.VERSION.SDK_INT >= 23 &&
            activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 7)
            done(Heard(emptyList(), "permission"))
            return
        }
        stop()
        // the phrase as written, and again split at apostrophes and hyphens, in case the model spells words apart
        val v = vocab
        val joined = ArrayList<String>()
        val split = ArrayList<String>()
        val missing = ArrayList<String>()
        for (w in words) {
            val parts = w.split('\'', '-').filter { it.isNotEmpty() }
            if (v == null) { joined.add(w); split.addAll(parts); continue }
            val okParts = parts.filter { v.contains(it) }
            if (v.contains(w)) joined.add(w) else joined.addAll(okParts)
            split.addAll(okParts)
            if (!v.contains(w) && okParts.isEmpty()) missing.add(w)
        }
        cannot = missing
        val grammar = JSONArray()
        if (joined.isNotEmpty()) grammar.put(joined.joinToString(" "))
        if (split.isNotEmpty() && split != joined) grammar.put(split.joinToString(" "))
        grammar.put("[unk]")
        try {
            val rec = Recognizer(m, 16000f, grammar.toString())
            rec.setWords(true)
            recognizer = rec
            val s = SpeechService(rec, 16000f)
            service = s
            pending = done
            s.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String) {}
                // a pause with nothing in it does not end her try; the time limit does
                override fun onResult(hypothesis: String) { finish(hypothesis, false) }
                override fun onFinalResult(hypothesis: String) { finish(hypothesis, true) }
                override fun onError(exception: Exception) { finishEmpty() }
                override fun onTimeout() { finish(recognizer?.finalResult ?: "{}", true) }
            }, 6000)
        } catch (e: IOException) {
            pending = null
            done(Heard(emptyList(), "none"))
        }
    }

    /** The first result with words in it, after she stops talking, is her try. */
    private fun finish(json: String, last: Boolean) {
        val cb = pending ?: return
        val out = ArrayList<HeardWord>()
        try {
            val res = JSONObject(json).optJSONArray("result")
            if (res != null) for (i in 0 until res.length()) {
                val o = res.getJSONObject(i)
                val w = o.optString("word")
                if (w.isNotEmpty() && w != "[unk]") out.add(HeardWord(w, o.optDouble("conf", 0.0).toFloat()))
            }
        } catch (e: Exception) {
            // an empty or odd result counts as nothing heard
        }
        if (out.isEmpty() && !last) return
        pending = null
        stop()
        cb(Heard(out, null, cannot))
    }

    private fun finishEmpty() {
        val cb = pending ?: return
        pending = null
        stop()
        cb(Heard(emptyList(), null, cannot))
    }

    override fun stop() {
        pending = null
        service?.let { it.stop(); it.shutdown() }
        service = null
        recognizer?.close()
        recognizer = null
    }

    override fun log(line: String) {
        try {
            val dir = activity.getExternalFilesDir(null) ?: return
            FileWriter(File(dir, "speech-log.tsv"), true).use { it.write(System.currentTimeMillis().toString() + "\t" + line + "\n") }
        } catch (e: IOException) {
            // the log is a help, not a need
        }
    }

    fun release() {
        stop()
        model?.close()
        model = null
    }
}
