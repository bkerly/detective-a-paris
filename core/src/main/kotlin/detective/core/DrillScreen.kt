package detective.core

/**
 * The notebook: learn each word, then prove you know it.
 *
 * A page teaches its words a few at a time. What happens to each word is set
 * by the page's "tasks" list in the unit file:
 *   learn    a card with the word, its meaning and its sound
 *   picture  hear the word, tap the right picture
 *   listen   hear the word, tap the right French text
 *   meaning  see the French, tap the English
 *   reverse  see the English, tap the French
 * After the words come a tip, a question on the tip, and word-tile sentences.
 * A wrong answer comes round again.
 */
class DrillScreen(g: Game, range: String, private val onDone: () -> Unit) : Screen(g) {
    private companion object {
        const val LEARN = 0
        const val LISTEN = 1
        const val MEANING = 2
        const val REVERSE = 3
        const val BUILD = 4
        const val TIP = 5
        const val QUIZ = 6
        const val PICTURE = 7
        const val STRIP = 8

        const val ASK = 0
        const val RIGHT = 1
        const val WRONG = 2
        const val PAGE_DONE = 3

        val NAMES = arrayOf("learn", "listen", "meaning", "reverse", "build", "tip", "quiz", "picture", "strip")
    }

    private class Task(
        val kind: Int, val word: Word? = null, val build: Build? = null, val tip: Tip? = null,
        val quiz: Quiz? = null, val group: List<Word>? = null
    ) {
        var misses = 0
    }

    private val unitId = g.unit.id
    private val pages = g.unit.pages
    /** The pages this notebook step covers, counted from 0. "all" or "1-3" (counted from 1) in the unit file. */
    private val first: Int
    private val last: Int
    init {
        val r = pageRange(range, pages.size)
        first = r[0]
        last = r[1]
    }
    private var pageIndex = if (g.save.page(unitId) in first..last) g.save.page(unitId) else first
    private val queue = ArrayList<Task>()
    private var total = 1
    private var finished = 0
    private var cur: Task? = null
    private var phase = ASK
    private var phaseT = 0f
    /** Goes up by one each time a new task is shown. */
    var serial = 0; private set

    // multiple choice
    private val options = ArrayList<String>()
    private val optionIcons = ArrayList<String>()
    private var correct = 0
    private var picked = -1
    /** Words the player has seen a card for, used to pick wrong answers. */
    private val known = ArrayList<Word>()

    // word tiles
    private val tray = ArrayList<String>()
    private val answer = ArrayList<Int>()
    private var locked = 0

    override fun enter() {
        if (pages.isEmpty() || first > last) { onDone(); return }
        for (i in 0 until pageIndex) for (id in pages[i].words) g.unit.word(id)?.let { if (!known.contains(it)) known.add(it) }
        startPage()
    }

    private fun startPage() {
        queue.clear()
        val page = pages[pageIndex]
        val tasks = page.tasks
        val ws = ArrayList<Word>()
        for (id in page.words) g.unit.word(id)?.let { ws.add(it) }
        // teach in small groups: five to a card on a "strip" page, otherwise three or four
        var i = 0
        while (i < ws.size) {
            val left = ws.size - i
            val n = if (page.strip) Math.min(5, left)
            else if (left == 4 || left == 7 || left >= 8) 4 else if (left <= 5 && left != 5) left else 3
            val chunk = ArrayList<Word>(ws.subList(i, Math.min(ws.size, i + n)))
            i += chunk.size
            if (tasks.contains("learn")) {
                if (page.strip) queue.add(Task(STRIP, group = chunk)) else for (w in chunk) queue.add(Task(LEARN, w))
            } else for (w in chunk) if (!known.contains(w)) known.add(w)
            for (kind in intArrayOf(PICTURE, LISTEN, MEANING)) {
                if (!tasks.contains(NAMES[kind])) continue
                val a = ArrayList<Word>(chunk)
                g.rng.shuffle(a)
                for (w in a) queue.add(Task(kind, w))
            }
        }
        for (id in page.tips) g.unit.tip(id)?.let { queue.add(Task(TIP, tip = it)) }
        if (page.quiz != null) queue.add(Task(QUIZ, quiz = page.quiz))

        val rev = ArrayList<Word>()
        if (tasks.contains("reverse")) rev.addAll(ws)
        if (page.review) for (k in 0 until pageIndex) for (id in pages[k].words) g.unit.word(id)?.let { rev.add(it) }
        // old words named by the page, and the shakiest words from earlier units
        val old = ArrayList<Word>()
        for (id in page.bringBack) g.findWord(id)?.let { if (!old.contains(it)) old.add(it) }
        if (page.refresh > 0) {
            val cands = ArrayList<Word>()
            for (id in g.save.met) {
                if (g.unit.word(id) != null) continue
                val w = g.findWord(id) ?: continue
                if (!old.contains(w)) cands.add(w)
            }
            g.rng.shuffle(cands)
            cands.sortByDescending { g.save.trouble(it.id) }
            for (w in cands) if (old.size < page.bringBack.size + page.refresh) old.add(w)
        }
        for (w in old) if (!known.contains(w)) known.add(w)
        rev.addAll(old)
        g.rng.shuffle(rev)
        for ((k, w) in rev.withIndex()) {
            // old words alternate between "say it in French" and "what does it mean"
            queue.add(Task(if (old.contains(w) && k % 2 == 1) MEANING else REVERSE, w))
        }
        for (bd in page.builds) queue.add(Task(BUILD, build = bd))
        total = Math.max(1, queue.size)
        finished = 0
        next()
    }

    private fun next() {
        serial++
        clearTimers()
        g.audio.stopVoice()
        buttons.clear()
        addExit()
        if (queue.isEmpty()) { pageDone(); return }
        val task = queue.removeAt(0)
        cur = task
        phase = ASK
        phaseT = 0f
        picked = -1
        when (task.kind) {
            LEARN -> setupLearn(task.word!!)
            STRIP -> setupStrip(task.group!!)
            LISTEN, MEANING, REVERSE, PICTURE -> setupChoice(task)
            QUIZ -> setupQuiz(task.quiz!!)
            TIP -> setupTip(task.tip!!)
            BUILD -> setupBuild(task.build!!)
        }
    }

    private fun addExit() {
        add(Button("exit", 366f, 4f, 28f, 22f, "", Style.PLAIN) { g.go(HubScreen(g)) }).icon = "cross"
    }

    private fun hear(w: Word, slow: Boolean = false): Float = g.voice.say("teacher", w.say, slow)

    // ---------- word cards ----------

    private fun setupLearn(w: Word) {
        if (!known.contains(w)) known.add(w)
        g.save.meet(w.id)
        add(Button("again", 150f, 148f, 66f, 24f, "Again", Style.DARK) { hear(w) }).also { it.icon = "speaker"; it.size = 10f }
        add(Button("slow", 222f, 148f, 62f, 24f, "Slow", Style.DARK) { hear(w, true) }).also { it.icon = "slow"; it.size = 10f }
        val nx = add(Button("next", 300f, 194f, 88f, 32f, "Got it", Style.PRIMARY) { finished++; next() })
        nx.font = Font.DISPLAY
        nx.size = 15f
        after(0.35f) { hear(w) }
    }

    /** Several short words on one card, for sets like numbers and days. Each row plays its word. */
    private fun setupStrip(group: List<Word>) {
        var delay = 0.35f
        for ((i, w) in group.withIndex()) {
            if (!known.contains(w)) known.add(w)
            g.save.meet(w.id)
            add(Button("hear:$i", 44f, 52f + i * 27f, 30f, 23f, "", Style.DARK) { clearTimers(); hear(w) }).icon = "speaker"
            val d = delay
            after(d) { hear(w) }
            delay += 1.5f
        }
        val nx = add(Button("next", 300f, 194f, 88f, 32f, "Got it", Style.PRIMARY) { finished++; next() })
        nx.font = Font.DISPLAY
        nx.size = 15f
    }

    // ---------- four kinds of question about one word ----------

    private fun generic(icon: String) = icon == "talk" || icon.isEmpty()

    private fun setupChoice(task: Task) {
        val w = task.word!!
        // wrong answers come from words already taught, this page first
        val pool = ArrayList<Word>()
        for (k in known) {
            if (k === w || k.en == w.en || k.fr == w.fr) continue
            if (task.kind == PICTURE && (generic(k.icon) || k.icon == w.icon)) continue
            pool.add(k)
        }
        val page = pages[pageIndex].words
        val near = ArrayList<Word>()
        val far = ArrayList<Word>()
        for (k in pool) if (page.contains(k.id)) near.add(k) else far.add(k)
        g.rng.shuffle(near)
        g.rng.shuffle(far)
        val picks = ArrayList<Word>()
        fun fits(k: Word): Boolean {
            // never offer two answers that look the same
            for (pk in picks) if (pk.en == k.en || pk.fr == k.fr || (task.kind == PICTURE && pk.icon == k.icon)) return false
            return true
        }
        for (k in near) if (picks.size < 3 && fits(k)) picks.add(k)
        for (k in far) if (picks.size < 3 && fits(k)) picks.add(k)
        picks.add(w)
        g.rng.shuffle(picks)
        options.clear()
        optionIcons.clear()
        for (k in picks) {
            options.add(if (task.kind == MEANING) k.en else if (task.kind == PICTURE) "" else k.fr)
            optionIcons.add(if (task.kind == PICTURE) k.icon else "")
        }
        correct = picks.indexOf(w)
        layoutOptions(13f)
        if (task.kind == LISTEN || task.kind == PICTURE) {
            add(Button("again", 164f, 58f, 64f, 46f, "", Style.DARK) { hear(w) }).also { it.icon = "speaker"; it.iconSize = 30f }
            add(Button("slow", 236f, 66f, 58f, 30f, "Slow", Style.DARK) { hear(w, true) }).also { it.icon = "slow"; it.size = 10f }
            after(0.35f) { hear(w) }
        } else if (task.kind == MEANING) {
            add(Button("again", 340f, 62f, 40f, 32f, "", Style.DARK) { hear(w) }).icon = "speaker"
            after(0.35f) { hear(w) }
        }
    }

    private fun setupQuiz(q: Quiz) {
        val order = ArrayList<Int>()
        for (i in q.options.indices) order.add(i)
        g.rng.shuffle(order)
        options.clear()
        optionIcons.clear()
        for (i in order) { options.add(q.options[i]); optionIcons.add("") }
        correct = order.indexOf(q.answer)
        layoutOptions(12f)
    }

    private fun layoutOptions(size: Float) {
        val n = options.size
        val w = 176f
        val h = 46f
        for (i in 0 until n) {
            val col = i % 2
            val row = i / 2
            var x = 30f + col * (w + 8f)
            if (n % 2 == 1 && i == n - 1) x = 30f + (w + 8f) / 2f
            val b = add(Button("opt:$i", x, 124f + row * (h + 8f), w, h, options[i], Style.PLAIN) { choose(i) })
            b.size = size
            if (optionIcons[i].isNotEmpty()) { b.icon = optionIcons[i]; b.iconSize = 40f }
        }
    }

    private fun choose(i: Int) {
        if (phase != ASK) return
        val task = cur ?: return
        picked = i
        phaseT = 0f
        val ok = i == correct
        task.word?.let { g.save.mark(it.id, ok) }
        for (b in buttons) if (b.id.startsWith("opt:")) b.enabled = false
        button("opt:$correct")?.let { it.style = Style.GOOD; it.enabled = true }
        var wait = 1.0f
        if (ok) {
            phase = RIGHT
            g.audio.sfx("ok")
            finished++
            if (task.kind == REVERSE || task.kind == MEANING) wait = Math.max(wait, hear(task.word!!) + 0.4f)
            if (task.kind == QUIZ && task.quiz!!.speak) wait = Math.max(wait, g.voice.say("teacher", task.quiz.says[task.quiz.answer]) + 0.4f)
        } else {
            phase = WRONG
            g.audio.sfx("no")
            button("opt:$i")?.let { it.style = Style.BAD; it.enabled = true; it.shake = 1f }
            task.misses++
            queue.add(Math.min(queue.size, 3), task)
            wait = 2.8f
            task.word?.let { w -> after(0.5f) { hear(w) } }
        }
        after(wait) { next() }
    }

    // ---------- tip card ----------

    private fun setupTip(tip: Tip) {
        if (!g.save.tips.contains(tip.id)) g.save.tips.add(tip.id)
        val nx = add(Button("next", 300f, 194f, 88f, 32f, "Got it", Style.PRIMARY) { finished++; next() })
        nx.font = Font.DISPLAY
        nx.size = 15f
    }

    // ---------- put the words in order ----------

    private fun setupBuild(b: Build) {
        tray.clear()
        for (tl in b.tiles) tray.add(g.fill(tl))
        for (tl in b.extra) tray.add(g.fill(tl))
        // shuffle until the tiles are not already in the right order
        var tries = 0
        do { g.rng.shuffle(tray); tries++ } while (tries < 8 && startsInOrder(b))
        answer.clear()
        locked = 0
        layoutBuild()
    }

    private fun startsInOrder(b: Build): Boolean {
        for (i in b.tiles.indices) if (tray[i] != g.fill(b.tiles[i])) return false
        return true
    }

    private fun layoutBuild() {
        buttons.clear()
        addExit()
        // measuring needs a painter, so widths are estimated from the letter count
        var size = 14f
        fun tw(s: String) = Math.max(30f, s.length * size * 0.53f + 16f)
        fun rowW(): Float { var w = 0f; for (s in tray) w += tw(s) + 6f; return w }
        while (size > 9f && rowW() > 340f) size -= 1f
        var totalA = 0f
        for (k in answer) totalA += tw(tray[k]) + 6f
        var ax = 212f - totalA / 2f
        for ((slot, k) in answer.withIndex()) {
            val w = tw(tray[k])
            val bt = add(Button("ans:$slot", ax, 84f, w, 30f, tray[k], Style.PRIMARY) {
                if (phase == ASK && slot >= locked) { answer.removeAt(slot); layoutBuild() }
            })
            bt.size = size
            ax += w + 6f
        }
        var totalT = 0f
        for (i in tray.indices) if (!answer.contains(i)) totalT += tw(tray[i]) + 6f
        var tx = 212f - totalT / 2f
        for (i in tray.indices) {
            if (answer.contains(i)) continue
            val w = tw(tray[i])
            val bt = add(Button("tile:$i", tx, 140f, w, 30f, tray[i], Style.PLAIN) {
                if (phase == ASK) { answer.add(i); layoutBuild() }
            })
            bt.size = size
            tx += w + 6f
        }
        val ck = add(Button("check", 162f, 192f, 100f, 34f, "Check", Style.GOOD) { checkBuild() })
        ck.font = Font.DISPLAY
        ck.size = 15f
        ck.enabled = answer.isNotEmpty() && phase == ASK
    }

    private fun checkBuild() {
        val task = cur ?: return
        val b = task.build ?: return
        var ok = answer.size == b.tiles.size
        if (ok) for (i in b.tiles.indices) if (tray[answer[i]] != g.fill(b.tiles[i])) ok = false
        phaseT = 0f
        if (ok) {
            phase = RIGHT
            g.audio.sfx("ok")
            finished++
            for (bt in buttons) if (bt.id.startsWith("ans:")) bt.style = Style.GOOD
            button("check")?.enabled = false
            val d = g.voice.say(b.who, b.say)
            after(Math.max(1.1f, d + 0.5f)) { next() }
        } else {
            phase = WRONG
            g.audio.sfx("no")
            task.misses++
            for (bt in buttons) if (bt.id.startsWith("ans:")) { bt.style = Style.BAD; bt.shake = 1f }
            button("check")?.enabled = false
            after(1.0f) {
                phase = ASK
                answer.clear()
                // after two misses, put the first word in place as a hint
                locked = 0
                if (task.misses >= 2) {
                    val first = tray.indexOf(g.fill(b.tiles[0]))
                    if (first >= 0) { answer.add(first); locked = 1 }
                }
                layoutBuild()
            }
        }
    }

    // ---------- end of a page ----------

    private fun pageDone() {
        cur = null
        phase = PAGE_DONE
        phaseT = 0f
        if (pageIndex + 1 > g.save.page(unitId)) g.save.pages[unitId] = pageIndex + 1
        g.save.commit()
        g.audio.sfx("win")
        if (pageIndex >= last) {
            // the next notebook step starts after this one; after the last page a replay starts again from page one
            g.save.pages[unitId] = if (last + 1 >= pages.size) 0 else last + 1
            g.save.commit()
            val b = add(Button("go", 152f, 190f, 120f, 34f, "Done!", Style.PRIMARY) { onDone() })
            b.font = Font.DISPLAY
            b.size = 15f
        } else {
            val b = add(Button("go", 212f, 190f, 150f, 34f, "Next page", Style.PRIMARY) { pageIndex++; startPage() })
            b.font = Font.DISPLAY
            b.size = 15f
            add(Button("stop", 62f, 190f, 140f, 34f, "Stop for now", Style.PLAIN) { g.go(HubScreen(g)) })
        }
    }

    override fun update(dt: Float) {
        super.update(dt)
        phaseT += dt
    }

    override fun tapUp(x: Float, y: Float) {
        // a tap anywhere moves on once the answer has been shown for a moment
        if ((phase == RIGHT || phase == WRONG) && phaseT > 0.6f && cur?.kind != BUILD) next()
    }

    // ---------- drawing ----------

    override fun draw(p: Painter) {
        Draw.notebookPage(p)
        val page = pages[pageIndex]
        Text.line(p, page.title, 30f, 15f, 14f, C.INK, Font.DISPLAY)
        // progress
        p.fillRoundRect(190f, 11f, 166f, 8f, 4f, C.PAPER2)
        val f = if (phase == PAGE_DONE) 1f else finished / total.toFloat()
        if (f > 0f) p.fillRoundRect(190f, 11f, Math.max(8f, 166f * Math.min(1f, f)), 8f, 4f, C.GREEN)

        val task = cur
        if (phase == PAGE_DONE || task == null) {
            drawPageDone(p)
        } else {
            when (task.kind) {
                LEARN -> drawLearn(p, task.word!!)
                STRIP -> drawStrip(p, task.group!!)
                LISTEN -> head(p, "Listen. Which one did you hear?")
                PICTURE -> head(p, "Listen. Tap the right picture.")
                MEANING -> {
                    head(p, "What does it mean?")
                    val w = task.word!!
                    val size = fit(p, w.fr, 26f, 290f, Font.BOLD)
                    Text.line(p, w.fr, 190f, 78f, size, C.INK, Font.BOLD, Align.CENTER)
                }
                REVERSE -> {
                    head(p, "How do you say it in French?")
                    val w = task.word!!
                    Icons.draw(p, w.icon, 84f, 52f, 54f)
                    val size = fit(p, w.en, 20f, 230f, Font.BOLD)
                    Text.line(p, w.en, 150f, 80f, size, C.INK, Font.BOLD)
                }
                QUIZ -> {
                    head(p, "A question for the detective")
                    Text.block(p, task.quiz!!.en, 40f, 58f, 344f, 15f, C.INK, Font.BOLD, Align.CENTER)
                }
                TIP -> drawTip(p, task.tip!!)
                BUILD -> drawBuild(p, task.build!!)
            }
            if ((task.kind == LISTEN || task.kind == MEANING || task.kind == REVERSE || task.kind == QUIZ || task.kind == PICTURE) && phase != ASK) {
                val ok = phase == RIGHT
                val w = task.word
                val msg = when {
                    (task.kind == LISTEN || task.kind == PICTURE) && w != null -> (if (ok) "Yes! " else "It was: ") + w.fr + " = " + w.en
                    ok -> "Yes!"
                    else -> "Not this time. Look at the green one."
                }
                Text.line(p, msg, 212f, 114f, 10.5f, if (ok) C.GREEN_D else C.RED_D, Font.BOLD, Align.CENTER)
            }
        }
        drawButtons(p)
    }

    private fun head(p: Painter, s: String) {
        Text.line(p, s, 212f, 40f, 12f, C.INK2, Font.TEXT, Align.CENTER)
    }

    private fun fit(p: Painter, s: String, size: Float, maxW: Float, font: Int): Float {
        var sz = size
        while (sz > 10f && p.textWidth(s, sz, font) > maxW) sz -= 1f
        return sz
    }

    private fun drawLearn(p: Painter, w: Word) {
        head(p, "New word")
        Draw.card(p, 36f, 50f, 352f, 132f)
        Icons.draw(p, w.icon, 52f, 70f, 80f)
        val size = fit(p, w.fr, 26f, 226f, Font.BOLD)
        Text.line(p, w.fr, 150f, 76f, size, C.INK, Font.BOLD)
        val es = fit(p, w.en, 14f, 226f, Font.BOLD)
        Text.line(p, w.en, 150f, 100f, es, C.GREEN_D, Font.BOLD)
        if (w.note.isNotEmpty()) Text.block(p, w.note, 150f, 112f, 226f, 10f, C.GREY, Font.TEXT, Align.LEFT, 1.25f)
    }

    private fun drawStrip(p: Painter, group: List<Word>) {
        head(p, "New words. Tap one to hear it again.")
        Draw.card(p, 36f, 48f, 352f, group.size * 27f + 6f)
        for ((i, w) in group.withIndex()) {
            val y = 52f + i * 27f
            Icons.draw(p, w.icon, 82f, y, 23f)
            val size = fit(p, w.fr, 16f, 130f, Font.BOLD)
            Text.line(p, w.fr, 114f, y + 11.5f, size, C.INK, Font.BOLD)
            val es = fit(p, w.en, 12f, 128f, Font.TEXT)
            Text.line(p, w.en, 250f, y + 11.5f, es, C.GREEN_D, Font.BOLD)
        }
    }

    private fun drawTip(p: Painter, tip: Tip) {
        head(p, "Detective tip")
        Draw.card(p, 36f, 50f, 352f, 132f, 0xFFFFF4C8.toInt())
        Icons.draw(p, "loupe", 48f, 62f, 44f)
        Text.line(p, tip.title, 100f, 76f, 18f, C.INK, Font.DISPLAY)
        var size = 12f
        while (size > 9.5f && Text.height(p, Text.french(tip.text), 276f, size, Font.TEXT, 1.32f) > 84f) size -= 0.5f
        Text.block(p, Text.french(tip.text), 100f, 92f, 276f, size, C.INK, Font.TEXT, Align.LEFT, 1.32f)
    }

    private fun drawBuild(p: Painter, b: Build) {
        head(p, "Put the words in order")
        val en = g.fill(b.en)
        Text.line(p, en, 212f, 62f, fit(p, en, 16f, 340f, Font.BOLD), C.INK, Font.BOLD, Align.CENTER)
        p.fillRoundRect(40f, 80f, 344f, 38f, 8f, C.alpha(C.WHITE, 0.7f))
        p.strokeRoundRect(40f, 80f, 344f, 38f, 8f, 1f, C.GREY_L)
        if (answer.isEmpty()) Text.line(p, "Tap the words below", 212f, 99f, 10f, C.GREY, Font.TEXT, Align.CENTER)
        if (phase == RIGHT) Text.line(p, "Yes!", 212f, 128f, 11f, C.GREEN_D, Font.BOLD, Align.CENTER)
        if (phase == WRONG) Text.line(p, "Not quite. Try again.", 212f, 128f, 11f, C.RED_D, Font.BOLD, Align.CENTER)
    }

    private fun drawPageDone(p: Painter) {
        val grow = Math.min(1f, phaseT * 4f)
        Draw.stamp(p, 212f, 72f, 32f * (1.6f - 0.6f * grow), "OK", C.RED)
        Text.line(p, "Page ${pageIndex - first + 1} of ${last - first + 1} done!", 212f, 126f, 20f, C.INK, Font.DISPLAY, Align.CENTER)
        val n = pages[pageIndex].words.size
        val msg = if (n > 0) "$n new words are in your notebook." else "Your old words are fresh again."
        Text.block(p, msg, 60f, 142f, 304f, 12f, C.INK2, Font.TEXT, Align.CENTER)
    }

    override fun back(): Boolean { g.go(HubScreen(g)); return true }

    override fun describe(): String {
        val task = cur ?: return "Drill page=${pageIndex + 1} pageDone"
        val what = task.word?.id ?: task.build?.say ?: task.tip?.id ?: (if (task.group != null) "group" else "quiz")
        return "Drill page=${pageIndex + 1} ${NAMES[task.kind]} $what phase=$phase correct=$correct left=${queue.size}"
    }

    // ---------- for the scripted playthrough ----------

    fun pageDoneShowing(): Boolean = phase == PAGE_DONE
    fun kindName(): String = cur?.let { NAMES[it.kind] } ?: "none"

    /** The id of the button that is the right answer now, or null if any "next" will do. */
    fun rightButton(): String? {
        val task = cur ?: return null
        return when (task.kind) {
            LISTEN, MEANING, REVERSE, QUIZ, PICTURE -> "opt:$correct"
            else -> null
        }
    }

    /** For a tile task, the tray indices in the right order. */
    fun rightTiles(): List<Int> {
        val b = cur?.build ?: return emptyList()
        val out = ArrayList<Int>()
        for (k in 0 until locked) out.add(answer[k])
        for (k in locked until b.tiles.size) {
            val want = g.fill(b.tiles[k])
            for (i in tray.indices) if (tray[i] == want && !out.contains(i)) { out.add(i); break }
        }
        return out.subList(locked, out.size)
    }

    fun isBuild(): Boolean = cur?.kind == BUILD
}

/** Reads a notebook step's pages: "all", "2" or "1-3" (counted from 1). Returns the first and last page counted from 0. */
fun pageRange(range: String, count: Int): IntArray {
    val r = range.trim()
    if (r.isEmpty() || r == "all") return intArrayOf(0, count - 1)
    val parts = r.split('-')
    val a = parts[0].trim().toIntOrNull() ?: 1
    val b = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: a else a
    return intArrayOf(Math.max(0, a - 1), Math.min(count - 1, b - 1))
}
