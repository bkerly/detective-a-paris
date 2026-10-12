package detective.core

/**
 * The police sketch. The detective describes someone she saw, one French
 * sentence at a time, and the officer's drawing changes with each word. A
 * wrong word gives a wrong face; she fixes it until the drawing matches what
 * she saw. The people, sentences and words all come from the unit file.
 */
class SketchScreen(g: Game, sketchId: String, private val onExit: () -> Unit, private val onDone: () -> Unit) : Screen(g) {
    private companion object {
        const val TIP = 0
        const val TALK = 1     // the officer is speaking
        const val PICK = 2     // she is choosing words
        const val HERO = 3     // she is saying a sentence
    }

    private val sk = g.unit.sketches[sketchId] ?: throw IllegalArgumentException("unit ${g.unit.id} has no sketch \"$sketchId\"")
    private val tapMode = g.unit.english == "tap"
    private var person = 0
    private var phase = TIP
    private var sel = 0
    /** The option chosen for each sentence, or -1. */
    private var chosen = IntArray(0)
    private val bad = HashSet<Int>()
    private var line: Line? = null
    private var showHelp = false
    private var speakUntil = 0f
    var checks = 0; private set

    override fun enter() {
        phase = if (sk.tip.isEmpty()) TALK else TIP
        if (phase == TALK) startPerson() else rebuild()
    }

    private fun cur(): Suspect = sk.people[Math.min(person, sk.people.size - 1)]

    private fun startPerson() {
        if (person >= sk.people.size) { onDone(); return }
        val s = cur()
        chosen = IntArray(s.features.size) { -1 }
        bad.clear()
        sel = 0
        say(s.intro) { phase = PICK; rebuild() }
    }

    private fun say(ln: Line?, then: () -> Unit) {
        clearTimers()
        phase = TALK
        line = ln
        showHelp = false
        rebuild()
        val d = if (ln != null) g.say(ln) else 0.2f
        speakUntil = t + d
        after(d + 0.3f) { then() }
    }

    /** A face from the starting description plus the words chosen so far (or, for the truth, the right words). */
    private fun spec(truth: Boolean): LinkedHashMap<String, Any?> {
        val s = cur()
        val m = LinkedHashMap<String, Any?>()
        (s.base.v as? Map<*, *>)?.let { for (e in it.entries) m[e.key.toString()] = e.value }
        for ((i, f) in s.features.withIndex()) {
            var opt: FeatOpt? = null
            if (truth) { for (o in f.options) if (o.fr == f.answer) opt = o }
            else if (i < chosen.size && chosen[i] >= 0) opt = f.options[chosen[i]]
            (opt?.set?.v as? Map<*, *>)?.let { for (e in it.entries) m[e.key.toString()] = e.value }
        }
        return m
    }

    private fun sentence(f: Feature, i: Int): String {
        val word = if (i < chosen.size && chosen[i] >= 0) f.options[chosen[i]].fr else "___"
        return Text.french(f.fr.replace("{x}", word))
    }

    private fun allChosen(): Boolean { for (c in chosen) if (c < 0) return false; return chosen.isNotEmpty() }

    private fun pick(o: Int) {
        if (phase != PICK) return
        val s = cur()
        val f = s.features[sel]
        chosen[sel] = o
        bad.remove(sel)
        phase = HERO
        rebuild()
        val d = g.voice.say("hero", f.say.replace("{x}", f.options[o].raw))
        after(d + 0.25f) {
            phase = PICK
            // move on to the next sentence that still needs a word
            var nx = -1
            for (i in s.features.indices) if (chosen[i] < 0 || bad.contains(i)) { nx = i; break }
            if (nx >= 0) sel = nx
            rebuild()
        }
    }

    private fun check() {
        if (phase != PICK || !allChosen()) return
        checks++
        val s = cur()
        bad.clear()
        for ((i, f) in s.features.withIndex()) if (f.options[chosen[i]].fr != f.answer) bad.add(i)
        if (bad.isEmpty()) {
            g.audio.sfx("stamp")
            say(sk.right) {
                person++
                if (person >= sk.people.size) onDone() else startPerson()
            }
        } else {
            g.audio.sfx("no")
            for (i in s.features.indices) if (bad.contains(i)) { sel = i; break }
            say(sk.wrong) { phase = PICK; rebuild() }
        }
    }

    private fun rebuild() {
        buttons.clear()
        add(Button("exit", 6f, 3f, 28f, 22f, "", Style.DARK) { g.audio.stopVoice(); onExit() }).icon = "back"
        add(Button("book", 366f, 3f, 28f, 22f, "", Style.DARK) { g.openOverlay(NotebookScreen(g)) }).icon = "book"
        if (phase == TIP) {
            val b = add(Button("next", 156f, 170f, 88f, 30f, "Go!", Style.PRIMARY) { startPerson() })
            b.font = Font.DISPLAY
            b.size = 15f
            return
        }
        val s = cur()
        val n = s.features.size
        val rh = Math.min(24f, 118f / Math.max(1, n) - 3f)
        for ((i, f) in s.features.withIndex()) {
            val style = if (bad.contains(i)) Style.BAD else if (i == sel && phase != TALK) Style.PRIMARY else Style.PLAIN
            val b = add(Button("row:$i", 202f, 76f + i * (rh + 3f), 190f, rh, sentence(f, i), style) {
                if (phase == PICK) { sel = i; rebuild() }
            })
            b.size = 11.5f
            b.align = Align.LEFT
            b.enabled = phase == PICK
        }
        if (phase == PICK || phase == HERO) {
            val f = s.features[sel]
            val m = f.options.size
            val w = Math.min(92f, (300f - (m - 1) * 4f) / m)
            for ((o, op) in f.options.withIndex()) {
                val b = add(Button("word:$o", 8f + o * (w + 4f), 204f, w, 30f, op.fr, if (chosen[sel] == o) Style.DARK else Style.PLAIN) { pick(o) })
                b.size = 12f
                b.enabled = phase == PICK
            }
            val ck = add(Button("check", 316f, 202f, 76f, 34f, "Done", Style.GOOD) { check() })
            ck.font = Font.DISPLAY
            ck.size = 15f
            ck.enabled = phase == PICK && allChosen()
        }
        val ln = line
        if (ln != null) add(Button("again", 262f, 34f, 28f, 22f, "", Style.DARK) { speakUntil = t + g.say(ln) }).icon = "speaker"
    }

    override fun tapUp(x: Float, y: Float) {
        if (line != null && x > 60f && x < 300f && y > 28f && y < 72f) showHelp = !showHelp
    }

    private fun drawPerson(p: Painter, m: LinkedHashMap<String, Any?>, x: Float, y: Float, w: Float, h: Float, label: String, id: String) {
        p.fillRoundRect(x + 1f, y + 2f, w, h, 3f, C.alpha(C.BLACK, 0.2f))
        p.fillRoundRect(x, y, w, h, 3f, C.WHITE)
        // a height chart behind the head
        var ly = y + 12f
        while (ly < y + h - 14f) { p.fillRect(x + 3f, ly, w - 6f, 0.7f, C.GREY_L); ly += 14f }
        val tall = (m["tall"] as? Boolean) ?: true
        val ph = if (tall) h - 18f else h - 36f
        val pw = ph * Faces.W / Faces.H
        val img = Faces.get(id + Json.write(m), FaceSpec.from(J(m)), "neutral")
        p.pushClip(x, y, w, h - 12f)
        p.image(img, x + w / 2f - pw / 2f, y + h - 12f - ph, pw, ph, false, 1f)
        p.popClip()
        p.fillRect(x, y + h - 12f, w, 12f, C.INK)
        Text.line(p, label, x + w / 2f, y + h - 6f, 7.5f, C.WHITE, Font.BOLD, Align.CENTER)
    }

    override fun draw(p: Painter) {
        p.fillRect(0f, 0f, Game.W, Game.H, 0xFFDCE4F0.toInt())
        p.fillRect(0f, 198f, Game.W, 42f, C.PAPER)
        if (phase == TIP) {
            Draw.card(p, 60f, 50f, 280f, 160f, C.PAPER)
            Icons.draw(p, "sketch", 76f, 66f, 50f)
            Text.block(p, sk.tip, 136f, 64f, 190f, 12f, C.INK, Font.BOLD, Align.LEFT, 1.3f)
            drawButtons(p)
            return
        }
        val s = cur()
        // the officer and what he says
        var mood = "neutral"
        if (line != null && t < speakUntil && (t * 7f).toInt() % 2 == 0) mood = "talk"
        val face = Faces.get(g.unit.id + "/sketch/" + sk.id, sk.face, mood)
        p.fillRoundRect(8f, 30f, 44f, 42f, 4f, 0xFFC9D6EA.toInt())
        p.pushClip(8f, 30f, 44f, 42f)
        p.image(face, 10f, 31f, 40f, 43.6f, false, 1f)
        p.popClip()
        val ln = line
        if (ln != null) {
            Draw.bubble(p, 62f, 30f, 232f, 40f, 50f)
            var size = 12.5f
            while (size > 9f && p.textWidth(ln.fr, size, Font.BOLD) > 190f) size -= 0.5f
            Text.line(p, ln.fr, 70f, 43f, size, C.INK, Font.BOLD)
            val help = if (ln.en.isNotEmpty()) ln.en else ln.gloss.joinToString("   ")
            if (!tapMode || showHelp) Text.line(p, help, 70f, 59f, 9.5f, C.GREY, Font.TEXT)
            else if (help.isNotEmpty()) Text.line(p, "tap for help", 70f, 59f, 8.5f, C.GREY_L, Font.TEXT)
        }
        Text.line(p, s.title, 348f, 44f, 12f, C.INK, Font.DISPLAY, Align.CENTER)
        Text.line(p, "${person + 1} of ${sk.people.size}", 348f, 60f, 9f, C.GREY, Font.TEXT, Align.CENTER)

        // what she saw, and the drawing so far
        drawPerson(p, spec(true), 8f, 76f, 92f, 118f, "YOU SAW", "t")
        drawPerson(p, spec(false), 104f, 76f, 92f, 118f, "THE DRAWING", "d")
        drawButtons(p)
    }

    override fun back(): Boolean { g.audio.stopVoice(); onExit(); return true }

    override fun describe() = "Sketch person=${person + 1}/${sk.people.size} phase=$phase sel=$sel"

    // ---------- for the scripted playthrough ----------

    fun tipShowing(): Boolean = phase == TIP
    fun waiting(): Boolean = phase == PICK
    /** The next taps towards a right drawing: a row, then its right word; or "check" when all are right. */
    fun rightTaps(): List<String> {
        val s = cur()
        for ((i, f) in s.features.withIndex()) {
            val want = f.options.indexOfFirst { it.fr == f.answer }
            if (chosen[i] != want) return listOf("row:$i", "word:$want")
        }
        return listOf("check")
    }
    /** Taps that put in one wrong word. */
    fun wrongTaps(): List<String> {
        val s = cur()
        val f = s.features[0]
        val bad = f.options.indexOfFirst { it.fr != f.answer }
        return listOf("row:0", "word:$bad")
    }
}
