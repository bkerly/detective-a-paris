package detective.core

/**
 * Listening to the player. The Android app listens with an offline French
 * recogniser that is told to expect only the phrase she is trying to say. The
 * desktop has no ears, so the game falls back to "say it, then tap" there,
 * and the scripted check plugs in pretend ears.
 */

/** One word the recogniser heard, with how sure it was (0 to 1). */
class HeardWord(val word: String, val conf: Float)

/**
 * What came back from one try. [error] is null when listening worked (even if
 * nothing was heard); "permission" when the microphone is not allowed yet;
 * "none" when there is no recogniser at all. [cannot] lists words of the
 * phrase that the recogniser does not know, so they are not counted against her.
 */
class Heard(val words: List<HeardWord>, val error: String? = null, val cannot: List<String> = emptyList()) {
    fun text(): String = words.joinToString(" ") { it.word }
}

interface Ears {
    /** True when the device can listen now. If false, the game asks her to say it aloud and tap. */
    fun ready(): Boolean
    /** Listen once for [phrase]. [words] are its words, lower case, as [Speech.words] gives them. Call [done] on the game's thread. */
    fun listen(phrase: String, words: List<String>, done: (Heard) -> Unit)
    fun stop()
    /** Keep one line of the speaking log, so the cut-offs can be set from her real voice. */
    fun log(line: String)
}

object NoEars : Ears {
    override fun ready() = false
    override fun listen(phrase: String, words: List<String>, done: (Heard) -> Unit) { done(Heard(emptyList(), "none")) }
    override fun stop() {}
    override fun log(line: String) {}
}

object Speech {
    const val STRONG = "strong"
    const val GOOD = "good"
    const val WEAK = "weak"

    /** Every word of the phrase must be this sure for "said very well". To be tuned from the log. */
    const val SURE = 0.80f
    /** This share of the words must be heard for "said well". */
    const val ENOUGH = 0.6f

    /** The words of a phrase, lower case, as a recogniser writes them: "S'il vous plaît !" gives s'il, vous, plaît. */
    fun words(s: String): List<String> {
        val out = ArrayList<String>()
        val clean = StringBuilder()
        for (ch in s.lowercase()) {
            clean.append(
                when {
                    ch == '’' -> '\''
                    ch.isLetter() || ch == '\'' || ch == '-' -> ch
                    else -> ' '
                }
            )
        }
        for (w in clean.toString().split(' ')) {
            val t = w.trim('\'', '-')
            if (t.isNotEmpty()) out.add(t)
        }
        return out
    }

    /** Small pieces for matching: no accents, and split at apostrophes and hyphens, so "aidez-moi" and "aidez moi" agree. */
    fun bits(words: List<String>): List<String> {
        val out = ArrayList<String>()
        for (w in words) for (b in plain(w).split('\'', '-')) if (b.isNotEmpty()) out.add(b)
        return out
    }

    private fun plain(s: String): String {
        val sb = StringBuilder()
        for (ch in s) sb.append(
            when (ch) {
                'à', 'â', 'ä' -> 'a'
                'é', 'è', 'ê', 'ë' -> 'e'
                'î', 'ï' -> 'i'
                'ô', 'ö' -> 'o'
                'ù', 'û', 'ü' -> 'u'
                'ç' -> 'c'
                'œ' -> 'o'
                else -> ch
            }
        )
        return sb.toString()
    }

    /**
     * How well she said [phrase]: STRONG, GOOD, or null for "try again".
     * Words the recogniser cannot know are left out of the count.
     */
    fun tier(phrase: String, heard: Heard): String? {
        val skip = HashSet(bits(heard.cannot))
        val want = bits(words(phrase)).filter { !skip.contains(it) }
        if (want.isEmpty()) return GOOD
        // each heard word, split the same way, keeps its confidence
        val pool = ArrayList<Pair<String, Float>>()
        for (w in heard.words) for (b in bits(listOf(w.word))) pool.add(Pair(b, w.conf))
        var hit = 0
        var sure = true
        for (w in want) {
            var k = -1
            for (i in pool.indices) if (pool[i].first == w) { k = i; break }
            if (k >= 0) {
                hit++
                if (pool[k].second < SURE) sure = false
                pool.removeAt(k)
            }
        }
        if (hit == want.size && sure) return STRONG
        if (hit >= Math.ceil((want.size * ENOUGH).toDouble()).toInt()) return GOOD
        return null
    }
}
