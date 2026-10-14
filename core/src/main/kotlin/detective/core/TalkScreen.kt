package detective.core

/**
 * A conversation. The character speaks French; the player answers by picking
 * a French reply, building one from word tiles, handing something over,
 * paying with coins, choosing a route on a map, writing down a fact, sorting
 * things into two or three groups, or putting steps in order. A
 * wrong answer never ends the scene: the character reacts, then asks again
 * more slowly. After two misses the hint appears.
 */
class TalkScreen(g: Game, talkId: String, private val onExit: () -> Unit, private val onDone: () -> Unit) : Screen(g) {
    private companion object {
        const val SPEAK = 0
        const val ASK = 1
        const val HERO = 2
        const val REACT = 3
        const val NOTED = 4
        const val INTRO = 5
        const val WIN = 6
        // steps of a duel
        const val D_BUSY = 0
        const val D_MOVES = 1
        const val D_SPEAK = 2
        const val D_LISTEN = 3
    }

    private val talk = g.unit.talks[talkId] ?: throw IllegalArgumentException("unit ${g.unit.id} has no conversation \"$talkId\"")
    private var node: Node = talk.nodes[talk.start] ?: throw IllegalArgumentException("conversation $talkId has no start node \"${talk.start}\"")
    private val faceId = g.unit.id + "/" + talk.id
    private val tapMode = g.unit.english == "tap"
    private var line: Line? = null
    private var mood = "neutral"
    private var narr: String? = null
    private var phase = SPEAK
    private var misses = 0
    private val tried = ArrayList<Int>()
    private val order = ArrayList<Int>()
    private var speakUntil = 0f
    private var picked = -1
    private var factHint = false
    private var showHelp = false
    /** A picture held up during a reaction to a wrong answer. */
    private var reactShow: String? = null

    /** Sorting: which item is up, and what has gone into each bin so far. */
    private var sortIdx = 0
    private val sorted = ArrayList<Int>()
    /** Ordering: the items as shown (shuffled), and the order she has tapped them in. */
    private val orderShown = ArrayList<Int>()
    private val orderPicked = ArrayList<Int>()
    private var orderLocked = 0
    /** Rooms she has marked on a plan. They stay marked for the rest of the conversation. */
    private val planMarks = HashSet<String>()
    private var planFlash = 0f

    private val tray = ArrayList<String>()
    private val answer = ArrayList<Int>()
    private var locked = 0
    private val paid = ArrayList<Int>()

    override fun enter() { enterNode(talk.start) }

    // ---------- a spoken battle: a "duel" node ----------

    /** One step of what happens after a move: a line the speaker says, or a short message from the game. */
    private class DMsg(val line: Line?, val fr: String, val en: String, val fx: (() -> Unit)?)

    private var dStep = D_BUSY
    private var doubt = 100
    private var barShown = 100f
    private var turn = 1
    private var dMood = "froid"
    private var polite = false
    private var scolded = false
    private var won = false
    private var earsBroken = false
    /** Her last phrase did not suit the mood, so the next menu gives a hint. */
    private var offMood = false
    private var dMove: DuelMove? = null
    private var dTries = 0
    private var lastDmg = 0
    private var dmgT = -9f
    private var missT = -9f
    private var introT = 0f
    private var winT = -9f
    private val said = IntArray(3)
    private var status = ""
    private var statusEn = ""
    private var sysFr = ""
    private var sysEn = ""
    private val dMsgs = ArrayList<DMsg>()

    /** The battle layout is up: during a duel, its opening and its win. */
    private fun arena(): Boolean = node.ask == "duel" || phase == INTRO || phase == WIN

    private fun earsOk(): Boolean = g.ears.ready() && !earsBroken

    private fun startDuel() {
        val d = node.duel ?: return
        doubt = 100
        barShown = 100f
        turn = 1
        dMood = "froid"
        // a duel in the middle of a conversation has no hello to say
        polite = d.moves.none { it.kind == "greet" }
        scolded = false
        won = false
        offMood = false
        for (i in said.indices) said[i] = 0
        sysFr = ""
        status = ""
        dStep = D_BUSY
        phase = INTRO
        introT = t
        line = null
        rebuild()
        g.audio.sfx("battle")
        after(2.2f) { speak(node.npc, "busy", false) { showMoves() } }
    }

    private fun moodNow(): DuelMood? = node.duel?.moods?.firstOrNull { it.id == dMood }

    private fun showMoves() {
        dStep = D_MOVES
        dMove = null
        phase = ASK
        sysFr = ""
        // early on, and after a phrase that did not suit, the storyteller says which kind of phrase suits this mood
        val mo = moodNow()
        narr = if (mo != null && (g.unit.number <= 4 || offMood))
            (if (node.duel?.she == true) "She" else "He") + " is " + mo.en + ": " + Duel.tag(mo.best) + " works best."
        else node.narr
        rebuild()
    }

    private fun pickMove(m: DuelMove) {
        if (dStep != D_MOVES) return
        dMove = m
        dTries = 0
        dStep = D_SPEAK
        phase = ASK
        // the speech box now says what to do
        line = null
        if (earsOk()) { status = "Écoutez, puis parlez !"; statusEn = "Listen, then speak!" }
        else { status = "Dites-le à voix haute !"; statusEn = "Say it out loud, then tap." }
        rebuild()
        g.voice.say("hero", m.line.say)
    }

    private fun talkNow() {
        val m = dMove ?: return
        if (dStep != D_SPEAK) return
        if (!earsOk()) {
            // no microphone: she says it aloud and taps, and that counts as said well
            g.ears.log(logLine(m, "(said aloud, no microphone)", Speech.GOOD))
            resolveMove(Speech.GOOD)
            return
        }
        g.audio.stopVoice()
        dStep = D_LISTEN
        phase = HERO
        status = "J'écoute…"
        statusEn = "Listening… say it now."
        rebuild()
        g.ears.listen(m.fr, Speech.words(m.fr)) { h -> heard(h) }
    }

    private fun heard(h: Heard) {
        val m = dMove ?: return
        if (dStep != D_LISTEN) return
        when (h.error) {
            "permission" -> {
                dStep = D_SPEAK; phase = ASK
                status = "Le micro ?"; statusEn = "Allow the microphone, then tap PARLER again."
                rebuild(); return
            }
            "none" -> {
                earsBroken = true
                dStep = D_SPEAK; phase = ASK
                status = "Dites-le à voix haute !"; statusEn = "The microphone is not working. Say it out loud, then tap."
                rebuild(); return
            }
        }
        dTries++
        var tier = Speech.tier(m.fr, h)
        if (tier == null && dTries >= 2) tier = Speech.WEAK
        g.ears.log(logLine(m, h.text() + "\t" + h.words.joinToString(" ") { (Math.round(it.conf * 100f) / 100f).toString() }, tier ?: "again"))
        if (tier == null) {
            g.audio.sfx("miss")
            missT = t
            dStep = D_SPEAK; phase = ASK
            status = "Encore une fois ?"; statusEn = "One more time?"
            rebuild()
            return
        }
        resolveMove(tier)
    }

    private fun logLine(m: DuelMove, heard: String, tier: String): String =
        g.unit.id + "\t" + m.fr + "\t" + heard + "\ttry " + dTries + "\t" + tier

    private fun tierWords(tier: String): Pair<String, String> = when (tier) {
        Speech.STRONG -> Pair("Très bien !", "Very well said!")
        Speech.GOOD -> Pair("Bien !", "Well said!")
        else -> Pair("Pas mal.", "Not bad. Keep trying!")
    }

    /** Work out what her move does, then play it out: a message, the hit, the speaker's answer, his next mood. */
    private fun resolveMove(tier: String) {
        val d = node.duel ?: return
        val m = dMove ?: return
        dStep = D_BUSY
        phase = SPEAK
        status = ""
        said[when (tier) { Speech.STRONG -> 0; Speech.GOOD -> 1; else -> 2 }]++
        dMsgs.clear()
        val tw = tierWords(tier)
        dMsgs.add(DMsg(null, tw.first, tw.second, null))
        val power = when (tier) { Speech.STRONG -> 1f; Speech.GOOD -> 0.65f; else -> 0.35f }
        var dmg = 0
        if (m.kind == "greet") {
            if (polite) dMsgs.add(DMsg(null, "Vous avez déjà dit bonjour.", "You already said hello. It does nothing now.", null))
            else {
                dmg = 12
                dMsgs.add(DMsg(null, "C'est poli !", "That's polite! Now your other words work fully.") { hit(12) })
                polite = true
                dMsgs.add(DMsg(d.greetReply, "", "", null))
            }
        } else {
            val best = polite && moodNow()?.best == m.kind
            offMood = polite && moodNow() != null && !best
            var mul = if (best) 1.5f else 0.7f
            if (!polite) mul *= 0.5f
            dmg = Math.round(30f * power * mul)
            val hitNow = dmg
            if (best) dMsgs.add(DMsg(null, "C'est super efficace !", "It's super effective!") { hit(hitNow) })
            else dMsgs.add(DMsg(null, "Ce n'est pas très efficace…", "It's not very effective…") { hit(hitNow) })
            if (!polite && !scolded) { scolded = true; dMsgs.add(DMsg(d.scold, "", "", null)) }
            if (doubt - dmg > 0) dMsgs.add(DMsg(d.react[tier], "", "", null))
        }
        val left = doubt - dmg
        turn++
        if (left <= 0 || turn > d.turns) {
            runMsgs { duelWin(left > 0) }
            return
        }
        if (polite) {
            // his mood changes every turn, and his line gives it away
            val others = d.moods.filter { it.id != dMood }
            val next = others[g.rng.next(others.size)]
            dMsgs.add(DMsg(next.line, "", "") { dMood = next.id })
        } else dMsgs.add(DMsg(node.npc, "", "", null))
        runMsgs { showMoves() }
    }

    private fun hit(dmg: Int) {
        doubt = Math.max(0, doubt - dmg)
        lastDmg = dmg
        dmgT = t
        if (dmg > 0) g.audio.sfx("hit")
    }

    private fun runMsgs(then: () -> Unit) {
        if (dMsgs.isEmpty()) { then(); return }
        val m = dMsgs.removeAt(0)
        m.fx?.invoke()
        val ln = m.line
        if (ln != null) {
            sysFr = ""
            speak(ln, "neutral", false) { runMsgs(then) }
        } else {
            clearTimers()
            g.audio.stopVoice()
            line = null
            sysFr = m.fr
            sysEn = m.en
            phase = SPEAK
            rebuild()
            after(1.5f) { runMsgs(then) }
        }
    }

    private fun duelWin(tired: Boolean) {
        val d = node.duel ?: return
        won = true
        doubt = 0
        dMsgs.clear()
        dMsgs.add(DMsg(if (tired) d.tired else d.win, "", "", null))
        runMsgs {
            phase = WIN
            winT = t
            sysFr = ""
            rebuild()
            g.audio.sfx("victory")
            after(2.8f) { enterNode(node.next) }
        }
    }

    private fun duelButtons() {
        val d = node.duel ?: return
        if (dStep == D_MOVES) {
            val n = d.moves.size
            val rows = (n + 1) / 2
            val h = Math.min(36f, (76f - (rows - 1) * 4f) / rows)
            for ((i, m) in d.moves.withIndex()) {
                val x = if (i % 2 == 0) 8f else 202f
                val y = 162f + (i / 2) * (h + 4f)
                val b = add(Button("move:${m.kind}", x, y, 190f, h, "", Style.PLAIN) { pickMove(m) })
                b.tag = i
            }
            return
        }
        val m = dMove ?: return
        val hb = add(Button("hear", 10f, 204f, 100f, 30f, "Écouter", Style.PLAIN) { g.voice.say("hero", m.line.say) })
        hb.icon = "speaker"; hb.size = 11f
        hb.enabled = dStep == D_SPEAK
        if (earsOk()) {
            val tb = add(Button("talk", 116f, 204f, 116f, 30f, if (dStep == D_LISTEN) "J'écoute…" else "Parler", Style.PRIMARY) { talkNow() })
            tb.icon = "mic"; tb.font = Font.DISPLAY; tb.size = 14f
            tb.enabled = dStep == D_SPEAK
        } else {
            val sb = add(Button("said", 116f, 204f, 116f, 30f, "J'ai dit !", Style.GOOD) { talkNow() })
            sb.font = Font.DISPLAY; sb.size = 14f
            sb.enabled = dStep == D_SPEAK
        }
        val ob = add(Button("other", 238f, 204f, 64f, 30f, "Autre", Style.PLAIN) { showMoves() })
        ob.size = 10f
        ob.enabled = dStep == D_SPEAK
    }

    private fun enterNode(id: String?) {
        val n = if (id == null) null else talk.nodes[id]
        if (n == null) { onDone(); return }
        node = n
        misses = 0
        tried.clear()
        paid.clear()
        factHint = false
        picked = -1
        reactShow = null
        narr = n.narr
        order.clear()
        for (i in n.options.indices) order.add(i)
        g.rng.shuffle(order)
        val b = n.build
        if (b != null) {
            tray.clear()
            for (tl in b.tiles) tray.add(g.fill(tl))
            for (tl in b.extra) tray.add(g.fill(tl))
            g.rng.shuffle(tray)
            answer.clear()
            locked = 0
            if (narr == null) narr = b.en
        }
        if (n.ask == "give" && narr == null) narr = "Tap the right thing."
        if (n.ask == "pay" && narr == null) narr = "Tap coins and notes to make the right amount."
        sortIdx = 0
        sorted.clear()
        orderShown.clear()
        orderPicked.clear()
        orderLocked = 0
        val oq = n.order
        if (oq != null) {
            for (i in oq.items.indices) orderShown.add(i)
            g.rng.shuffle(orderShown)
            if (orderShown.size > 1 && orderShown == (0 until orderShown.size).toList()) orderShown.reverse()
        }
        if (n.ask == "duel") { startDuel(); return }
        speak(n.npc, n.mood, false) { ask() }
    }

    /** The speaker's line about the item that is up for sorting, if it has one. */
    private fun sortItemLine(): Line? {
        val q = node.sort ?: return null
        return if (sortIdx < q.items.size) q.items[sortIdx].line else null
    }

    private fun speak(ln: Line?, defMood: String, slow: Boolean, then: () -> Unit) {
        clearTimers()
        phase = SPEAK
        line = ln
        showHelp = false
        mood = ln?.mood ?: defMood
        rebuild()
        val d = if (ln != null) g.say(ln, slow) else 0.4f
        speakUntil = t + d
        after(d + 0.25f) { then() }
    }

    private fun ask() {
        val il = sortItemLine()
        if (node.ask == "sort" && il != null && line !== il) { speak(il, node.mood, false) { ask() }; return }
        phase = ASK
        rebuild()
        if (node.ask == "end") after(0.8f) { onDone() }
    }

    /** On the phone the words show only once she has missed twice (or there is nothing to answer). */
    private fun phoneHidden(ln: Line): Boolean =
        talk.phone && misses < 2 && node.ask != "auto" && node.ask != "end" && phase != NOTED && !(node.ask == "fact" && factHint)

    /** The help for a line: its English, or a short list of its new words. */
    private fun helpFor(ln: Line): String = if (ln.en.isNotEmpty()) ln.en else ln.gloss.joinToString("   ")

    private fun helpShown(ln: Line): Boolean {
        if (ln.test && misses < 2) return false
        if (ln.test) return true
        return !tapMode || showHelp
    }

    private fun modal(): Boolean = (node.ask == "fact" || node.ask == "route" || node.ask == "order" || node.ask == "plan") &&
        (phase == ASK || phase == NOTED || ((node.ask == "order" || node.ask == "plan") && phase == HERO))

    // ---------- buttons ----------

    private fun rebuild() {
        buttons.clear()
        if (modal()) {
            // the card covers the screen, so only its own buttons are live
            if (phase == ASK || phase == HERO) when (node.ask) {
                "fact" -> if (phase == ASK) factButtons()
                "route" -> if (phase == ASK) routeButtons()
                "order" -> orderButtons()
                "plan" -> planButtons()
            }
            return
        }
        add(Button("exit", 6f, 3f, 28f, 22f, "", Style.DARK) { g.audio.stopVoice(); onExit() }).icon = "back"
        add(Button("book", 366f, 3f, 28f, 22f, "", Style.DARK) { g.openOverlay(NotebookScreen(g)) }).icon = "book"
        val ln = line
        if (ln != null && phase != NOTED) {
            val ax = if (arena()) 66f else 262f
            add(Button("again", ax, 131f, 64f, 24f, "Again", Style.DARK) { speakUntil = t + g.say(ln) }).also { it.icon = "speaker"; it.size = 10f }
            add(Button("slow", ax + 68f, 131f, 62f, 24f, "Slow", Style.DARK) { speakUntil = t + g.say(ln, true) }).also { it.icon = "slow"; it.size = 10f }
        }
        if (phase == ASK || phase == HERO) {
            when (node.ask) {
                "choice" -> choiceButtons()
                "build" -> buildButtons()
                "give" -> giveButtons()
                "pay" -> payButtons()
                "sort" -> sortButtons()
                "duel" -> duelButtons()
                "auto" -> {
                    val b = add(Button("next", 304f, 200f, 88f, 30f, "Next", Style.PRIMARY) { enterNode(node.next) })
                    b.font = Font.DISPLAY
                    b.size = 15f
                }
            }
        }
    }

    override fun tapUp(x: Float, y: Float) {
        // in "tap" units, a tap on the speech bubble shows the help for that line
        val inBubble = if (arena()) x > 66f && x < 300f && y > 64f && y < 130f else x > 128f && y > 32f && y < 128f
        if (line != null && !modal() && inBubble) showHelp = !showHelp
    }

    private fun choiceButtons() {
        val n = order.size
        val bh = if (n > 3) 17f else 22f
        val gap = if (n > 3) 2f else 4f
        for ((row, i) in order.withIndex()) {
            val o = node.options[i]
            val y = 162f + row * (bh + gap)
            val b = add(Button("opt:$i", 40f, y, 352f, bh, g.fill(o.fr), if (picked == i) Style.PRIMARY else Style.PLAIN) { choose(i) })
            b.size = if (n > 3) 11.5f else 13f
            b.enabled = phase == ASK && !tried.contains(i) || picked == i
            val h = add(Button("hear:$i", 8f, y, 28f, bh, "", Style.DARK) { g.voice.say("hero", o.say) })
            h.icon = "speaker"
            h.enabled = phase == ASK
        }
    }

    private fun choose(i: Int) {
        if (phase != ASK) return
        val o = node.options[i]
        picked = i
        phase = HERO
        rebuild()
        val d = g.voice.say("hero", o.say)
        after(d + 0.3f) {
            picked = -1
            if (o.next != null) {
                g.audio.sfx("ok")
                enterNode(o.next)
            } else {
                g.audio.sfx("no")
                misses++
                tried.add(i)
                val tip = node.tip
                narr = if (misses >= 2 && tip != null) tip else (o.narr ?: narr)
                reactShow = o.show
                react(o.react)
            }
        }
    }

    /** The character reacts to a wrong answer, then asks the question again. */
    private fun react(ln: Line?) {
        val again = {
            reactShow = null
            val base = node.npc
            if (base != null) speak(base, node.mood, !base.lost) { phase = ASK; rebuild() }
            else { line = null; mood = node.mood; phase = ASK; rebuild() }
        }
        if (ln == null) { again(); return }
        clearTimers()
        phase = REACT
        line = ln
        showHelp = false
        mood = ln.mood
        rebuild()
        val d = g.say(ln)
        speakUntil = t + d
        after(d + 1.3f) { again() }
    }

    private fun buildButtons() {
        var size = 14f
        fun tw(s: String) = Math.max(30f, s.length * size * 0.52f + 16f)
        fun rowW(): Float { var w = 0f; for (s in tray) w += tw(s) + 5f; return w }
        while (size > 9f && rowW() > 380f) size -= 1f
        var ax = 12f
        for ((slot, k) in answer.withIndex()) {
            val w = tw(tray[k])
            val b = add(Button("ans:$slot", ax, 164f, w, 28f, tray[k], Style.PRIMARY) {
                if (phase == ASK && slot >= locked) { answer.removeAt(slot); rebuild() }
            })
            b.size = size
            ax += w + 5f
        }
        var total = 0f
        for (i in tray.indices) if (!answer.contains(i)) total += tw(tray[i]) + 5f
        var tx = 200f - total / 2f
        for (i in tray.indices) {
            if (answer.contains(i)) continue
            val w = tw(tray[i])
            val b = add(Button("tile:$i", tx, 204f, w, 28f, tray[i], Style.PLAIN) {
                if (phase == ASK) { answer.add(i); rebuild() }
            })
            b.size = size
            tx += w + 5f
        }
        val ck = add(Button("check", 318f, 164f, 74f, 28f, "Say it", Style.GOOD) { checkBuild() })
        ck.font = Font.DISPLAY
        ck.size = 14f
        ck.enabled = phase == ASK && answer.isNotEmpty()
    }

    private fun checkBuild() {
        val b = node.build ?: return
        var ok = answer.size == b.tiles.size
        if (ok) for (i in b.tiles.indices) if (tray[answer[i]] != g.fill(b.tiles[i])) ok = false
        phase = HERO
        rebuild()
        val d = g.voice.say(b.who, b.say)
        after(d + 0.5f) {
            if (ok) {
                g.audio.sfx("ok")
                enterNode(node.next)
            } else {
                g.audio.sfx("no")
                misses++
                answer.clear()
                locked = 0
                if (misses >= 2) {
                    val first = tray.indexOf(g.fill(b.tiles[0]))
                    if (first >= 0) { answer.add(first); locked = 1 }
                }
                react(node.wrong)
            }
        }
    }

    private fun giveButtons() {
        val n = node.items.size
        val w = Math.min(86f, (384f - (n - 1) * 8f) / n)
        val gap = 8f
        val x0 = 200f - (n * w + (n - 1) * gap) / 2f
        for (i in 0 until n) {
            val item = node.items[i]
            val b = add(Button("item:$item", x0 + i * (w + gap), 164f, w, 68f, "", if (picked == i) Style.PRIMARY else Style.PLAIN) { give(i) })
            b.icon = item
            b.iconSize = Math.min(52f, w - 14f)
            b.enabled = phase == ASK && !tried.contains(i) || picked == i
        }
    }

    private fun give(i: Int) {
        if (phase != ASK) return
        picked = i
        phase = HERO
        rebuild()
        val ok = node.items[i] == node.answer
        after(0.45f) {
            picked = -1
            if (ok) {
                g.audio.sfx("ok")
                enterNode(node.next)
            } else {
                g.audio.sfx("no")
                misses++
                tried.add(i)
                react(node.wrong)
            }
        }
    }

    private fun sortButtons() {
        val q = node.sort ?: return
        val n = q.bins.size
        val gap = 8f
        val w = (244f - (n - 1) * gap) / n
        for (i in 0 until n) {
            val b = add(Button("bin:$i", 148f + i * (w + gap), 162f, w, 72f, "", if (picked == i) Style.PRIMARY else Style.PLAIN) { sortInto(i) })
            b.enabled = phase == ASK || picked == i
        }
    }

    private fun sortInto(bin: Int) {
        val q = node.sort ?: return
        if (phase != ASK || sortIdx >= q.items.size) return
        picked = bin
        phase = HERO
        rebuild()
        val d = g.voice.say("hero", q.binSay[bin])
        after(d + 0.3f) {
            picked = -1
            if (q.items[sortIdx].bin == bin) {
                g.audio.sfx("ok")
                sorted.add(bin)
                sortIdx++
                if (sortIdx >= q.items.size) enterNode(node.next)
                else { misses = 0; ask() }
            } else {
                g.audio.sfx("no")
                misses++
                react(node.wrong)
            }
        }
    }

    private fun orderButtons() {
        val q = node.order ?: return
        val n = q.items.size
        val h = Math.min(34f, (178f - (n - 1) * 4f) / n)
        // what she has put in order so far; tap one to take it back
        for ((slot, k) in orderPicked.withIndex()) {
            val b = add(Button("slot:$slot", 70f, 52f + slot * (h + 4f), 138f, h, "", Style.PRIMARY) {
                if (phase == ASK && slot >= orderLocked) { while (orderPicked.size > slot) orderPicked.removeAt(orderPicked.size - 1); rebuild() }
            })
            b.enabled = phase == ASK && slot >= orderLocked
            b.tag = k
        }
        for ((row, k) in orderShown.withIndex()) {
            if (orderPicked.contains(k)) continue
            val b = add(Button("step:$k", 218f, 52f + row * (h + 4f), 174f, h, "", Style.PLAIN) { pickStep(k) })
            b.enabled = phase == ASK
            b.tag = k
        }
        val ln = line
        if (ln != null) {
            add(Button("again", 262f, 6f, 64f, 22f, "Again", Style.GHOST) { speakUntil = t + g.say(ln) }).also { it.icon = "speaker"; it.size = 10f }
            add(Button("slow", 330f, 6f, 62f, 22f, "Slow", Style.GHOST) { speakUntil = t + g.say(ln, true) }).also { it.icon = "slow"; it.size = 10f }
        }
    }

    private fun pickStep(k: Int) {
        val q = node.order ?: return
        if (phase != ASK) return
        orderPicked.add(k)
        g.voice.say("hero", q.items[k].say)
        if (orderPicked.size < q.items.size) { rebuild(); return }
        phase = HERO
        rebuild()
        after(1.4f) {
            var ok = true
            for (i in orderPicked.indices) if (orderPicked[i] != i) ok = false
            if (ok) {
                g.audio.sfx("ok")
                enterNode(node.next)
            } else {
                g.audio.sfx("no")
                misses++
                factHint = true
                orderPicked.clear()
                orderLocked = 0
                // after two misses the first step goes in by itself
                if (misses >= 2) { orderPicked.add(0); orderLocked = 1 }
                phase = ASK
                rebuild()
            }
        }
    }

    // ---------- a plan of a place: tap the room the speaker describes ----------

    private val planX = 14f
    private val planY = 14f
    private val planW = 236f
    private val planH = 212f

    private fun planButtons() {
        val q = node.plan ?: return
        for (r in q.rooms) {
            val b = add(Button("room:${r.id}", planX + r.x / 100f * planW, planY + r.y / 100f * planH, r.w / 100f * planW, r.h / 100f * planH, "", Style.GHOST) { tapRoom(r.id) })
            b.enabled = phase == ASK && !r.fixed && !planMarks.contains(r.id)
        }
        val ln = line
        if (ln != null) {
            add(Button("again", 262f, 102f, 60f, 24f, "Again", Style.GHOST) { speakUntil = t + g.say(ln) }).also { it.icon = "speaker"; it.size = 10f }
            add(Button("slow", 326f, 102f, 60f, 24f, "Slow", Style.GHOST) { speakUntil = t + g.say(ln, true) }).also { it.icon = "slow"; it.size = 10f }
        }
    }

    private fun tapRoom(id: String) {
        val q = node.plan ?: return
        if (phase != ASK) return
        if (id == q.answer) {
            planMarks.add(id)
            planFlash = 1f
            g.audio.sfx("stamp")
            phase = HERO
            rebuild()
            after(1.4f) { enterNode(node.next) }
        } else {
            g.audio.sfx("no")
            misses++
            factHint = true
            button("room:$id")?.shake = 1f
        }
    }

    private fun drawPlan(p: Painter) {
        val q = node.plan ?: return
        p.fillRect(0f, 0f, Game.W, Game.H, C.NIGHT)
        // the paper plan, with walls between rooms
        p.fillRoundRect(planX - 4f, planY - 4f, planW + 8f, planH + 8f, 4f, 0xFFF4EEDC.toInt())
        for (i in 0..10) p.fillRect(planX + i * planW / 10f, planY, 0.6f, planH, 0xFFE2DAC2.toInt())
        for (i in 0..10) p.fillRect(planX, planY + i * planH / 10f, planW, 0.6f, 0xFFE2DAC2.toInt())
        for (r in q.rooms) {
            val x = planX + r.x / 100f * planW
            val y = planY + r.y / 100f * planH
            val w = r.w / 100f * planW
            val h = r.h / 100f * planH
            val marked = planMarks.contains(r.id)
            val bg = when {
                r.fixed -> 0xFFE9D9B4.toInt()
                marked -> 0xFFCFE8C9.toInt()
                else -> 0xFFFFFFFF.toInt()
            }
            p.fillRect(x, y, w, h, bg)
            p.strokeRoundRect(x, y, w, h, 1f, 2f, C.INK2)
            val b = button("room:${r.id}")
            if (b != null && b.shake > 0f) p.fillRect(x, y, w, h, C.alpha(C.RED, 0.25f))
            if (r.fixed || marked) {
                val s = Math.min(Math.min(w, h) - 14f, 34f)
                if (r.icon.isNotEmpty() && s > 10f) Icons.draw(p, r.icon, x + (w - s) / 2f, y + (h - s) / 2f - 5f, s)
                var fs = 9.5f
                while (fs > 6.5f && p.textWidth(r.label, fs, Font.BOLD) > w - 4f) fs -= 0.5f
                Text.line(p, r.label, x + w / 2f, y + h - 7f, fs, C.INK, Font.BOLD, Align.CENTER)
                if (marked && !r.fixed) Icons.draw(p, "check", x + w - 14f, y + 3f, 11f, C.GREEN_D)
            }
        }
        if (q.hereX >= 0f) {
            val hx = planX + q.hereX / 100f * planW
            val hy = planY + q.hereY / 100f * planH
            val bob = Math.sin(t * 5.0).toFloat() * 1.5f
            p.fillOval(hx - 6f, hy - 6f + bob, 12f, 12f, C.RED)
            p.strokeRoundRect(hx - 6f, hy - 6f + bob, 12f, 12f, 6f, 1.5f, C.WHITE)
            val lw = p.textWidth("Vous êtes ici", 8f, Font.BOLD) + 8f
            p.fillRoundRect(hx + 8f, hy - 6f + bob, lw, 12f, 3f, C.alpha(C.WHITE, 0.9f))
            Text.line(p, "Vous êtes ici", hx + 12f, hy + bob, 8f, C.RED_D, Font.BOLD)
        }
        val ln = line
        if (ln != null) {
            var size = 12f
            while (size > 9f && Text.wrap(p, g.fill(ln.fr), size, Font.BOLD, 128f).size > 5) size -= 0.5f
            val bottom = Text.block(p, g.fill(ln.fr), 262f, 16f, 128f, size, C.WHITE, Font.BOLD, Align.LEFT, 1.22f)
            if (bottom > 98f) { /* lines are kept short in the unit files */ }
        }
        Text.block(p, q.prompt, 262f, 134f, 128f, 10.5f, C.MUSTARD, Font.BOLD, Align.LEFT, 1.25f)
        if (factHint && phase == ASK) Text.block(p, node.tip ?: "Not that room. Listen again.", 262f, 186f, 128f, 9.5f, C.MUSTARD, Font.TEXT, Align.LEFT, 1.2f)
        for (b in buttons) if (!b.id.startsWith("room:")) b.draw(p)
    }

    private fun payTotal(): Int { var s = 0; for (c in paid) s += c; return s }

    private fun payButtons() {
        val pay = node.pay ?: return
        val n = pay.coins.size
        val w = Math.min(52f, (290f - (n - 1) * 6f) / n)
        for (i in 0 until n) {
            val v = pay.coins[i]
            val b = add(Button("coin:$v", 8f + i * (w + 6f), 162f, w, 40f, "", Style.PLAIN) {
                if (phase == ASK && paid.size < 8) { paid.add(v); g.audio.sfx("tap"); rebuild() }
            })
            b.icon = "coin:$v"
            b.iconSize = Math.min(34f, w - 8f)
            b.enabled = phase == ASK
        }
        val cl = add(Button("clear", 8f, 208f, 70f, 26f, "Take back", Style.PLAIN) { paid.clear(); rebuild() })
        cl.size = 10f
        cl.enabled = phase == ASK && paid.isNotEmpty()
        val pb = add(Button("pay", 312f, 176f, 80f, 44f, "Pay", Style.GOOD) { checkPay() })
        pb.font = Font.DISPLAY
        pb.size = 17f
        pb.enabled = phase == ASK && paid.isNotEmpty()
    }

    private fun checkPay() {
        val pay = node.pay ?: return
        val ok = payTotal() == pay.amount
        phase = HERO
        rebuild()
        after(0.4f) {
            if (ok) {
                g.audio.sfx("ok")
                enterNode(node.next)
            } else {
                g.audio.sfx("no")
                misses++
                paid.clear()
                react(node.wrong)
            }
        }
    }

    private fun factButtons() {
        val f = node.fact ?: return
        val ln = line
        if (f.card == "hear" && ln != null) {
            // nothing to read the answer from: she has to listen
            add(Button("again", 128f, 96f, 68f, 26f, "Again", Style.GHOST) { g.say(ln) }).also { it.icon = "speaker"; it.size = 10f }
            add(Button("slow", 204f, 96f, 68f, 26f, "Slow", Style.GHOST) { g.say(ln, true) }).also { it.icon = "slow"; it.size = 10f }
        }
        val n = f.options.size
        val gap = 10f
        val w = Math.min(110f, (364f - (n - 1) * gap) / n)
        val x0 = 200f - (n * w + (n - 1) * gap) / 2f
        var long = false
        for (o in f.options) if (o.length > 4) long = true
        for (i in 0 until n) {
            val v = f.options[i]
            val b = add(Button("fact:$v", x0 + i * (w + gap), 170f, w, 34f, v, Style.PLAIN) {
                if (v == f.answer) {
                    g.save.facts[g.unit.id + "/" + f.key] = (f.label + " " + v).trim()
                    g.save.commit()
                    g.audio.sfx("stamp")
                    phase = NOTED
                    rebuild()
                    after(2.0f) { enterNode(node.next) }
                } else {
                    g.audio.sfx("no")
                    factHint = true
                    button("fact:$v")?.shake = 1f
                }
            })
            if (long) { b.size = 11f } else { b.font = Font.DISPLAY; b.size = 18f }
        }
    }

    private fun routeButtons() {
        val r = node.route ?: return
        val ln = line
        if (ln != null) add(Button("again", 262f, 86f, 60f, 24f, "Again", Style.GHOST) { g.say(ln) }).also { it.icon = "speaker"; it.size = 10f }
        if (ln != null) add(Button("slow", 326f, 86f, 60f, 24f, "Slow", Style.GHOST) { g.say(ln, true) }).also { it.icon = "slow"; it.size = 10f }
        for (i in r.ids.indices) {
            val id = r.ids[i]
            val b = add(Button("route:$id", 262f + i * 43f, 150f, 38f, 34f, id, Style.PLAIN) {
                if (id == r.answer) {
                    g.audio.sfx("ok")
                    enterNode(node.next)
                } else {
                    g.audio.sfx("no")
                    misses++
                    factHint = true
                    button("route:$id")?.shake = 1f
                }
            })
            b.font = Font.DISPLAY
            b.size = 18f
        }
    }

    // ---------- drawing ----------

    override fun draw(p: Painter) {
        p.fillRect(0f, 0f, Game.W, Game.H, 0xFFE4ECF6.toInt())
        p.fillRect(0f, 158f, Game.W, 82f, C.PAPER)
        p.fillRect(0f, 158f, Game.W, 1f, C.GREY_L)
        if (arena()) drawArena(p) else drawStage(p)

        // the storyteller's line
        val nr = narr
        if (nr != null) {
            p.fillRoundRect(40f, 2f, 320f, 26f, 6f, C.INK)
            var ns = 9.5f
            while (ns > 8f && Text.wrap(p, nr, ns, Font.TEXT, 306f).size > 2) ns -= 0.5f
            val lines = Text.wrap(p, nr, ns, Font.TEXT, 306f)
            var y = 15f - (lines.size - 1) * ns * 0.61f
            for (s in lines) { Text.line(p, s, 200f, y, ns, C.WHITE, Font.TEXT, Align.CENTER); y += ns * 1.22f }
        }

        if (phase == ASK && node.ask == "build") {
            p.fillRoundRect(8f, 161f, 306f, 34f, 6f, C.alpha(C.WHITE, 0.8f))
            p.strokeRoundRect(8f, 161f, 306f, 34f, 6f, 1f, C.GREY_L)
            if (answer.isEmpty()) Text.line(p, "Tap the words to build your answer", 160f, 178f, 10f, C.GREY, Font.TEXT, Align.CENTER)
        }
        if ((phase == ASK || phase == HERO || phase == SPEAK || phase == REACT) && node.ask == "sort") drawSort(p)
        if ((phase == ASK || phase == HERO) && node.ask == "pay") {
            // what she has put down so far
            var x = 88f
            for (c in paid) { Icons.draw(p, "coin:$c", x, 208f, 24f); x += 26f }
            if (paid.isNotEmpty()) Text.line(p, "= ${payTotal()} €", Math.min(x + 4f, 262f), 221f, 14f, C.INK, Font.DISPLAY)
        }

        if (modal()) {
            when (node.ask) {
                "fact" -> drawFact(p)
                "route" -> drawRoute(p)
                "order" -> drawOrder(p)
                "plan" -> drawPlan(p)
            }
            return
        }
        drawButtons(p)
        if (node.ask == "sort") drawBins(p)
        if (node.ask == "duel") drawDuelPanel(p)
    }

    override fun update(dt: Float) {
        super.update(dt)
        val target = doubt.toFloat()
        if (barShown > target) barShown = Math.max(target, barShown - dt * 70f)
        if (barShown < target) barShown = target
    }

    /** A colour turned grey, then lightened by [lift] (0 to 1). */
    private fun grey(c: Int, lift: Float): Int {
        val v = 0.3f * ((c shr 16) and 255) + 0.59f * ((c shr 8) and 255) + 0.11f * (c and 255)
        val k = (v + (255f - v) * lift).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (k shl 16) or (k shl 8) or k
    }

    private fun faceMood(): String = if (won || phase == WIN) "smile" else moodNow()?.face ?: "busy"

    private fun moodLabel(): Pair<String, String> {
        if (won || phase == WIN) return Pair(if (node.duel?.she == true) "convaincue" else "convaincu", "won over")
        val m = moodNow() ?: return Pair(if (node.duel?.she == true) "froide" else "froid", "cold")
        return Pair(m.label, m.en)
    }

    /**
     * The battle layout. The place is drawn in greys so the two people stand out:
     * the speaker on a pad at the right, with a box for name, mood and doubt;
     * the detective from behind at the bottom left; the speech box between them.
     */
    private fun drawArena(p: Painter) {
        val d = node.duel
        val title = d?.title ?: talk.name
        val intro = phase == INTRO
        val it0 = t - introT
        val e = if (intro) Math.min(1f, it0 / 0.9f) else 1f
        val ease = 1f - (1f - e) * (1f - e)
        val wall = grey(if (talk.sky) C.SKY else talk.wall, 0.45f)
        p.fillRect(0f, 30f, Game.W, 128f, wall)
        p.fillRect(0f, 104f, Game.W, 54f, grey(wall, 0.35f))
        p.fillRect(0f, 104f, Game.W, 1f, 0xFFB8B8B8.toInt())
        p.fillRect(0f, 0f, Game.W, 30f, 0xFFE2E2E2.toInt())
        p.fillRect(0f, 159f, Game.W, 81f, 0xFFEDEDED.toInt())
        val oxOff = (1f - ease) * 200f
        val hxOff = -(1f - ease) * 140f
        pad(p, 292f + oxOff, 116f, 104f, 26f)
        pad(p, 0f + hxOff, 138f, 82f, 20f)

        // the speaker, who flickers and shakes when a phrase lands
        var m = faceMood()
        val ln = line
        if (ln != null && t < speakUntil && phase != HERO && ((t * 7f).toInt() % 2 == 0)) m = "talk"
        val hd = t - dmgT
        val hit = hd < 0.5f && lastDmg > 0
        val visible = !hit || ((hd * 12f).toInt() % 2 == 0)
        val shakeX = if (hit) (Math.sin(hd * 50.0) * 4.0 * (1f - hd * 2f)).toFloat() else 0f
        if (visible) {
            if (talk.phone) Icons.draw(p, "phone", 312f + oxOff + shakeX, 52f, 70f)
            else p.image(Faces.get(faceId, talk.face, m), 306f + oxOff + shakeX, 36f, 82f, 89.4f, false, 1f)
        }
        if (talk.phone && ln != null && t < speakUntil) for (k in 0 until 3) {
            val a = ((t * 3f + k / 3f) % 1f)
            p.strokeRoundRect(347f - 12f - a * 24f, 76f - a * 10f, 24f + a * 48f, 34f + a * 20f, 18f + a * 24f, 1.2f, C.alpha(C.INK, 1f - a))
        }

        // the detective, from behind: she steps forward when a phrase lands, and wobbles when she must try again
        val lunge = if (hd < 0.3f && lastDmg > 0) Math.sin(Math.PI * hd / 0.3).toFloat() else 0f
        val md = t - missT
        val wob = if (md < 0.5f) (Math.sin(md * 40.0) * 2.5 * (1f - md * 2f)).toFloat() else 0f
        p.image(Looks.hero(g, 1, 0), 16f + hxOff + lunge * 12f + wob, 84f - lunge * 4f, 48f, 63f, false, 1f)

        if (intro) {
            if (e >= 1f) {
                textBox(p, 66f, 66f, 236f, 52f)
                var ts = 14f
                val msg = "$title vous défie !"
                while (ts > 10f && p.textWidth(msg, ts, Font.BOLD) > 216f) ts -= 0.5f
                Text.line(p, msg, 78f, 86f, ts, C.INK, Font.BOLD)
                Text.line(p, "défie = challenges", 78f, 106f, 9f, C.GREY, Font.TEXT)
            }
            // a quick dark flash as the battle starts
            if (it0 < 0.35f) p.fillRect(0f, 0f, Game.W, Game.H, C.alpha(C.NIGHT, 1f - it0 / 0.35f))
            return
        }

        // the speaker's box: name, level, mood and doubt
        if (phase == WIN) {
            p.fillRoundRect(8f, 30f, 228f, 34f, 6f, C.MUSTARD)
            p.strokeRoundRect(8f, 30f, 228f, 34f, 6f, 1.5f, C.INK)
            Text.line(p, "VICTOIRE !", 122f, 47f, 18f, C.INK, Font.DISPLAY, Align.CENTER)
        } else {
            p.fillRoundRect(8f, 30f, 228f, 34f, 6f, C.WHITE)
            p.strokeRoundRect(8f, 30f, 228f, 34f, 6f, 1.5f, C.INK)
            var ns = 10.5f
            while (ns > 7.5f && p.textWidth(title, ns, Font.BOLD) > 170f) ns -= 0.5f
            Text.line(p, title, 15f, 39f, ns, C.INK, Font.BOLD)
            Text.line(p, "Nv. ${g.unit.number}", 229f, 39f, 8.5f, C.INK2, Font.BOLD, Align.RIGHT)
            val ml = moodLabel()
            val cw = p.textWidth(ml.first, 8f, Font.BOLD) + 10f
            p.fillRoundRect(14f, 48f, cw, 12f, 6f, C.INK)
            Text.line(p, ml.first, 14f + cw / 2f, 54f, 8f, C.MUSTARD, Font.BOLD, Align.CENTER)
            Text.line(p, ml.second, 18f + cw, 54f, 7.5f, C.GREY, Font.TEXT)
            Text.line(p, "DOUTE", 150f, 54f, 7f, C.RED_D, Font.BOLD, Align.RIGHT)
            p.fillRoundRect(154f, 51f, 50f, 7f, 3.5f, C.INK)
            val f = barShown / 100f
            val col = when { f > 0.5f -> C.RED; f > 0.2f -> C.MUSTARD; else -> C.GREEN }
            if (f > 0.01f) p.fillRoundRect(155f, 52f, 48f * f, 5f, 2.5f, col)
            Text.line(p, "${Math.round(barShown)}", 229f, 54.5f, 10f, C.INK, Font.DISPLAY, Align.RIGHT)
        }

        // what is being said: the speaker's line, a message from the game, or what to do now
        if (ln != null) speech(p, ln, 66f, 68f, 232f, 60f)
        else if (sysFr.isNotEmpty()) boxText(p, sysFr, sysEn, 15f)
        else if (node.ask == "duel" && dStep == D_MOVES) {
            // early on, and after a phrase that did not suit, the box says which kind of phrase suits this mood
            boxText(p, "Que dites-vous ?", "What do you say? Pick a phrase.", 14f)
        }
        else if (node.ask == "duel" && (dStep == D_SPEAK || dStep == D_LISTEN) && status.isNotEmpty()) boxText(p, status, statusEn, 14f)

        // her box: her name and the turn
        p.fillRoundRect(202f, 131f, 96f, 24f, 6f, C.WHITE)
        p.strokeRoundRect(202f, 131f, 96f, 24f, 6f, 1.5f, C.INK)
        val who = g.fill("{name}")
        var ws = 9f
        while (ws > 6.5f && p.textWidth(who, ws, Font.BOLD) > 46f) ws -= 0.5f
        Text.line(p, who, 208f, 143f, ws, C.INK, Font.BOLD)
        Text.line(p, "TOUR ${if (phase == WIN) turn - 1 else Math.min(turn, d?.turns ?: turn)}", 292f, 143f, 8.5f, C.INK2, Font.BOLD, Align.RIGHT)

        // how much doubt the phrase took away floats up from the speaker
        if (hd < 1.4f && lastDmg > 0) {
            val a = if (hd < 1f) 1f else (1.4f - hd) / 0.4f
            val fy = 72f - hd * 22f
            val s = "−$lastDmg"
            for (o in arrayOf(floatArrayOf(-1.5f, 0f), floatArrayOf(1.5f, 0f), floatArrayOf(0f, -1.5f), floatArrayOf(0f, 1.5f)))
                Text.line(p, s, 347f + o[0], fy + o[1], 18f, C.alpha(C.INK, a), Font.DISPLAY, Align.CENTER)
            Text.line(p, s, 347f, fy, 18f, C.alpha(C.MUSTARD, a), Font.DISPLAY, Align.CENTER)
        }

        // on a win, stars rain gently, clear of the boxes
        if (phase == WIN) {
            val wd = t - winT
            for (k in 0 until 14) {
                val sx = (k * 53f + 17f) % 392f
                val sy = 30f + ((wd * (40f + (k % 4) * 12f) + k * 23f) % 120f)
                if (sx > 56f && sx < 306f && sy > 60f && sy < 132f) continue
                if (sx < 240f && sy < 68f) continue
                if (sx > 190f && sx < 300f && sy > 124f) continue
                Icons.draw(p, "star", sx, sy, 10f + (k % 3) * 3f, if (k % 2 == 0) C.MUSTARD else C.MUSTARD_D)
            }
        }
    }

    /** A message in the speech box: French in bold, English small under it. */
    private fun boxText(p: Painter, fr: String, en: String, size: Float) {
        textBox(p, 66f, 68f, 232f, 60f)
        var fs = size
        while (fs > 10f && Text.wrap(p, fr, fs, Font.BOLD, 212f).size > 2) fs -= 0.5f
        val bottom = Text.block(p, fr, 77f, 76f, 212f, fs, C.INK, Font.BOLD, Align.LEFT, 1.2f)
        if (en.isNotEmpty()) Text.block(p, en, 77f, bottom + 2f, 212f, 9f, C.GREY, Font.TEXT, Align.LEFT, 1.15f)
    }

    /** The bottom of the screen in a duel: the phrases to choose from, or the one she is about to say. */
    private fun drawDuelPanel(p: Painter) {
        val d = node.duel ?: return
        if (dStep == D_MOVES) {
            for (b in buttons) {
                if (!b.id.startsWith("move:")) continue
                val mv = d.moves[b.tag]
                Text.line(p, Duel.tag(mv.kind), b.x + 8f, b.y + 8f, 7f, C.MUSTARD_D, Font.BOLD)
                val showEn = !tapMode && mv.en.isNotEmpty()
                var fs = 12f
                while (fs > 8.5f && p.textWidth(mv.fr, fs, Font.BOLD) > b.w - 14f) fs -= 0.5f
                Text.line(p, mv.fr, b.x + 8f, b.y + if (showEn) 19f else 22f, fs, C.INK, Font.BOLD)
                if (showEn) Text.line(p, mv.en, b.x + 8f, b.y + 30f, 7.5f, C.GREY, Font.TEXT)
            }
            return
        }
        val mv = dMove ?: return
        if (dStep == D_SPEAK || dStep == D_LISTEN) {
            Text.line(p, Duel.tag(mv.kind), 12f, 168f, 7.5f, C.MUSTARD_D, Font.BOLD)
            var fs = 17f
            while (fs > 10f && p.textWidth(mv.fr, fs, Font.BOLD) > 376f) fs -= 0.5f
            Text.line(p, mv.fr, 12f, 183f, fs, C.INK, Font.BOLD)
            if (mv.en.isNotEmpty()) Text.line(p, mv.en, 12f, 197f, 8.5f, C.GREY, Font.TEXT)
            if (dStep == D_LISTEN) {
                // the microphone is on: rings pulse round the button
                val b = button("talk")
                if (b != null) for (k in 0 until 2) {
                    val a = ((t * 1.6f + k * 0.5f) % 1f)
                    p.strokeRoundRect(b.x - a * 8f, b.y - a * 6f, b.w + a * 16f, b.h + a * 12f, 8f + a * 4f, 2f, C.alpha(C.RED, 1f - a))
                }
            }
        }
    }

    /** An oval for someone to stand on, in grey like the rest of the place. */
    private fun pad(p: Painter, x: Float, y: Float, w: Float, h: Float) {
        p.fillOval(x, y, w, h, 0xFFB4B4B4.toInt())
        p.fillOval(x + 4f, y + 2f, w - 8f, h - 7f, 0xFFCDCDCD.toInt())
    }

    /** A white box with a double border, like the text box of an old game. */
    private fun textBox(p: Painter, x: Float, y: Float, w: Float, h: Float) {
        p.fillRoundRect(x + 1f, y + 2f, w, h, 6f, C.alpha(C.BLACK, 0.18f))
        p.fillRoundRect(x, y, w, h, 6f, C.WHITE)
        p.strokeRoundRect(x, y, w, h, 6f, 2f, C.INK)
        p.strokeRoundRect(x + 3f, y + 3f, w - 6f, h - 6f, 4f, 0.8f, C.INK2)
    }

    /** The speech bubble in a duel, with its tail pointing right at the speaker. */
    private fun speech(p: Painter, ln: Line, x: Float, y: Float, w: Float, h: Float) {
        p.fillRoundRect(x + 1f, y + 2f, w, h, 8f, C.alpha(C.BLACK, 0.18f))
        p.fillRoundRect(x, y, w, h, 8f, C.WHITE)
        val ty = y + 22f
        p.fillPoly(floatArrayOf(x + w - 1f, ty - 6f, x + w + 10f, ty + 2f, x + w - 1f, ty + 6f), C.WHITE)
        p.strokeRoundRect(x, y, w, h, 8f, 1f, C.INK)
        p.fillPoly(floatArrayOf(x + w - 0.6f, ty - 5f, x + w + 8.5f, ty + 1.6f, x + w - 0.6f, ty + 5f), C.WHITE)
        p.line(x + w, ty - 6f, x + w + 10f, ty + 2f, 1f, C.INK)
        p.line(x + w + 10f, ty + 2f, x + w, ty + 6f, 1f, C.INK)
        val tw = w - 18f
        val fr = g.fill(ln.fr)
        val hidden = phoneHidden(ln)
        if (hidden) {
            Text.line(p, ". . .", x + 9f, y + 14f, 15f, C.GREY, Font.BOLD)
            Text.block(p, "On the phone you only hear. Listen, then answer.", x + 9f, y + 26f, tw, 9f, C.GREY, Font.TEXT, Align.LEFT, 1.2f)
            return
        }
        val help = helpFor(ln)
        val showH = help.isNotEmpty() && !ln.lost && !(ln.test && misses < 2) && helpShown(ln)
        var size = 13f
        var hs = 9.5f
        fun need(): Float {
            var hh = Text.wrap(p, fr, size, Font.BOLD, tw).size * size * 1.22f
            hh += if (showH) Text.wrap(p, help, hs, Font.TEXT, tw).size * hs * 1.18f + 2f else 12f
            return hh
        }
        while (size > 9.5f && need() > h - 10f) { size -= 0.5f; if (hs > 8f) hs -= 0.25f }
        val bottom = Text.block(p, fr, x + 9f, y + 6f, tw, size, C.INK, Font.BOLD, Align.LEFT, 1.22f)
        if (ln.lost) Text.line(p, "? ? ?", x + 9f, bottom + 8f, 11f, C.RED, Font.DISPLAY)
        else if (ln.test && misses < 2) Text.line(p, "Listen and read carefully.", x + 9f, bottom + 8f, 9f, C.GREY, Font.TEXT)
        else if (showH) Text.block(p, help, x + 9f, bottom + 2f, tw, hs, C.GREY, Font.TEXT, Align.LEFT, 1.18f)
        else if (help.isNotEmpty()) Text.line(p, "tap here for help", x + 9f, bottom + 8f, 8.5f, C.GREY_L, Font.TEXT)
    }

    /** The usual layout: the speaker on the left, the speech bubble on the right. */
    private fun drawStage(p: Painter) {
        // the character
        p.fillRoundRect(8f, 32f, 114f, 126f, 6f, if (talk.sky) C.SKY else talk.wall)
        if (talk.sky) {
            p.image(g.art.get("cloud"), 12f, 58f, 40f, 17f, false, 1f)
            p.image(g.art.get("cloud"), 84f, 78f, 34f, 14f, false, 1f)
            p.fillRect(8f, 112f, 114f, 46f, talk.wall)
        }
        if (talk.board.isNotEmpty()) {
            val yellow = talk.boardStyle == "yellow"
            p.fillRoundRect(14f, 35f, 102f, 14f, 3f, if (yellow) C.MUSTARD else C.INK)
            var bs = 9f
            while (bs > 6f && p.textWidth(talk.board, bs, Font.DISPLAY) > 96f) bs -= 0.5f
            Text.line(p, talk.board, 65f, 42f, bs, if (yellow) C.INK else C.MUSTARD, Font.DISPLAY, Align.CENTER)
        }
        var m = mood
        val ln = line
        if (ln != null && t < speakUntil && phase != HERO && ((t * 7f).toInt() % 2 == 0)) m = "talk"
        if (talk.phone) {
            // a telephone, with sound coming out of it while the voice speaks
            Icons.draw(p, "phone", 25f, 62f, 80f)
            if (ln != null && t < speakUntil) for (k in 0 until 3) {
                val a = ((t * 3f + k / 3f) % 1f)
                p.strokeRoundRect(65f - 14f - a * 30f, 70f - a * 12f, 28f + a * 60f, 40f + a * 24f, 20f + a * 30f, 1.2f, C.alpha(C.INK, 1f - a))
            }
        } else {
            val face = Faces.get(faceId, talk.face, m)
            p.image(face, 17f, 52f, 96f, 104.7f, false, 1f)
        }
        if (talk.counter) {
            p.fillRect(8f, 144f, 114f, 14f, C.BROWN)
            p.fillRect(8f, 144f, 114f, 2.5f, 0xFFC58B4E.toInt())
        }
        val sh = reactShow ?: node.show
        if (sh != null) {
            p.fillOval(74f, 100f, 46f, 46f, C.WHITE)
            p.strokeRoundRect(74f, 100f, 46f, 46f, 23f, 1f, C.INK)
            Icons.draw(p, sh, 79f, 105f, 36f)
        }

        // what they say
        if (ln != null) {
            Draw.bubble(p, 132f, 34f, 260f, 92f, 84f)
            var size = 14f
            while (size > 11f && Text.wrap(p, g.fill(ln.fr), size, Font.BOLD, 240f).size > 3) size -= 1f
            val hidden = phoneHidden(ln)
            val bottom = if (hidden) {
                Text.line(p, ". . .", 142f, 50f, 16f, C.GREY, Font.BOLD)
                Text.block(p, "On the phone you only hear. Listen, then answer. The words come after two tries.", 142f, 64f, 240f, 10f, C.GREY, Font.TEXT, Align.LEFT, 1.2f)
            } else Text.block(p, g.fill(ln.fr), 142f, 41f, 240f, size, C.INK, Font.BOLD, Align.LEFT, 1.25f)
            val help = helpFor(ln)
            if (hidden) { }
            else if (ln.lost) Text.line(p, "? ? ?", 142f, bottom + 9f, 12f, C.RED, Font.DISPLAY)
            else if (ln.test && misses < 2) Text.line(p, "Listen and read carefully.", 142f, bottom + 9f, 9.5f, C.GREY, Font.TEXT)
            else if (help.isNotEmpty() && helpShown(ln)) Text.block(p, help, 142f, bottom + 3f, 240f, 10.5f, C.GREY, Font.TEXT, Align.LEFT, 1.2f)
            else if (help.isNotEmpty()) Text.line(p, "tap here for help", 142f, bottom + 9f, 9f, C.GREY_L, Font.TEXT)
        }

        if (ln == null && mood == "busy") {
            // nobody is talking: she is busy at her keyboard
            p.fillRoundRect(132f, 70f, 92f, 26f, 13f, C.WHITE)
            p.strokeRoundRect(132f, 70f, 92f, 26f, 13f, 1f, C.GREY_L)
            val dots = (t * 3f).toInt() % 4
            Text.line(p, "tac, tac, tac" + ".".repeat(dots), 142f, 83f, 10.5f, C.GREY, Font.HAND)
        }
    }

    /** The thing that is up for sorting, on a card at the bottom left. */
    private fun drawSort(p: Painter) {
        val q = node.sort ?: return
        if (sortIdx >= q.items.size) return
        val it = q.items[sortIdx]
        Draw.card(p, 8f, 162f, 132f, 72f, C.WHITE)
        val fc = it.face
        if (fc != null) {
            val img = Faces.get("sort/" + it.name, fc, "neutral")
            p.image(img, 14f, 166f, 40f, 43.6f, false, 1f)
        } else if (it.icon.isNotEmpty()) Icons.draw(p, it.icon, 14f, 168f, 40f)
        Text.block(p, it.name, 58f, 172f, 78f, 11f, C.INK, Font.BOLD, Align.LEFT, 1.15f)
        Text.line(p, "${sortIdx + 1} / ${q.items.size}", 74f, 224f, 9f, C.GREY, Font.TEXT, Align.CENTER)
    }

    /** Each bin: its French word, and small marks for what is in it already. */
    private fun drawBins(p: Painter) {
        val q = node.sort ?: return
        for (i in q.bins.indices) {
            val b = button("bin:$i") ?: continue
            var fs = 18f
            while (fs > 10f && p.textWidth(q.bins[i], fs, Font.DISPLAY) > b.w - 12f) fs -= 1f
            Text.line(p, q.bins[i], b.x + b.w / 2f, b.y + 20f, fs, if (picked == i) C.INK else C.INK, Font.DISPLAY, Align.CENTER)
            var k = 0
            for (j in sorted.indices) if (sorted[j] == i) {
                val item = q.items[j]
                val fc = item.face
                val ix = b.x + 8f + (k % 6) * 19f
                val iy = b.y + 34f + (k / 6) * 19f
                if (fc != null) p.image(Faces.get("sort/" + item.name, fc, "neutral"), ix, iy, 17f, 18.5f, false, 1f)
                else Icons.draw(p, item.icon, ix, iy, 17f)
                k++
            }
        }
    }

    /** Putting steps in order: slots on the left, the steps to choose from on the right. */
    private fun drawOrder(p: Painter) {
        val q = node.order ?: return
        p.fillRect(0f, 0f, Game.W, Game.H, C.NIGHT)
        val ln = line
        if (ln != null) {
            var size = 11f
            while (size > 8.5f && Text.wrap(p, g.fill(ln.fr), size, Font.BOLD, 246f).size > 3) size -= 0.5f
            Text.block(p, g.fill(ln.fr), 10f, 6f, 246f, size, C.WHITE, Font.BOLD, Align.LEFT, 1.2f)
        }
        val n = q.items.size
        val h = Math.min(34f, (178f - (n - 1) * 4f) / n)
        for (i in 0 until n) {
            val y = 52f + i * (h + 4f)
            val label = if (i < q.labels.size) q.labels[i] else "${i + 1}."
            Text.line(p, label, 64f, y + h / 2f, 11f, C.MUSTARD, Font.DISPLAY, Align.RIGHT)
            if (i >= orderPicked.size) {
                p.strokeRoundRect(70f, y, 138f, h, 6f, 1f, C.alpha(C.WHITE, 0.4f))
            }
        }
        for (b in buttons) {
            b.draw(p)
            if (b.id.startsWith("slot:") || b.id.startsWith("step:")) {
                val it = q.items[b.tag]
                Icons.draw(p, it.icon, b.x + 4f, b.y + (b.h - Math.min(26f, b.h - 6f)) / 2f, Math.min(26f, b.h - 6f))
                var fs = 10f
                while (fs > 7.5f && Text.wrap(p, it.fr, fs, Font.BOLD, b.w - 40f).size > 2) fs -= 0.5f
                val lines = Text.wrap(p, it.fr, fs, Font.BOLD, b.w - 40f)
                var ty = b.y + b.h / 2f - (lines.size - 1) * fs * 0.6f
                for (s in lines) { Text.line(p, s, b.x + 34f, ty, fs, C.INK, Font.BOLD); ty += fs * 1.2f }
            }
        }
        // the hint sits top right, under Again and Slow, clear of the five rows
        if (factHint) Text.block(p, node.tip ?: "Not quite. Listen again, and try another order.", 262f, 32f, 130f, 8.5f, C.MUSTARD, Font.BOLD, Align.LEFT, 1.15f)
        else Text.block(p, "Tap the steps in the order you hear them.", 262f, 32f, 130f, 8.5f, C.alpha(C.WHITE, 0.7f), Font.TEXT, Align.LEFT, 1.15f)
    }

    private fun drawFact(p: Painter) {
        val f = node.fact ?: return
        p.fillRect(0f, 0f, Game.W, Game.H, C.alpha(C.NIGHT, 0.97f))
        val tk = g.unit.ticket
        if (f.card == "ticket" && tk != null) TicketView.draw(p, tk, 60f, 28f, 280f, 100f)
        else if (f.card == "note") NoteView.draw(p, f.note, 70f, 22f, 260f, 108f)
        else if (f.card == "hear") {
            val ln = line
            if (ln != null && phoneHidden(ln)) {
                Draw.bubble(p, 50f, 20f, 300f, 66f, 60f)
                Text.line(p, "Listen to the voice.", 200f, 52f, 14f, C.GREY, Font.BOLD, Align.CENTER)
            } else if (ln != null) {
                Draw.bubble(p, 50f, 20f, 300f, 66f, 60f)
                var size = 15f
                while (size > 11f && Text.wrap(p, g.fill(ln.fr), size, Font.BOLD, 280f).size > 3) size -= 1f
                Text.block(p, g.fill(ln.fr), 60f, 28f, 280f, size, C.INK, Font.BOLD, Align.CENTER, 1.25f)
            }
        }
        if (phase == NOTED) {
            Draw.card(p, 90f, 140f, 220f, 78f, C.PAPER)
            Text.line(p, "In your notebook:", 200f, 156f, 11f, C.INK2, Font.TEXT, Align.CENTER)
            val txt = (f.label + " " + f.answer).trim()
            var hs = 24f
            while (hs > 9f && p.textWidth(txt, hs, Font.HAND) > 150f) hs -= 1f
            Text.line(p, txt, 176f, 186f, hs, C.INK, Font.HAND, Align.CENTER)
            Draw.stamp(p, 278f, 184f, 20f, "OK", C.RED)
        } else {
            Text.block(p, f.prompt, 40f, 138f, 320f, 12f, C.WHITE, Font.BOLD, Align.CENTER)
            if (factHint) Text.block(p, f.hint, 30f, 210f, 340f, 10.5f, C.MUSTARD, Font.BOLD, Align.CENTER)
        }
        for (b in buttons) b.draw(p)
    }

    private fun drawRoute(p: Painter) {
        val r = node.route ?: return
        p.fillRect(0f, 0f, Game.W, Game.H, C.NIGHT)
        val mx = 14f
        val my = 14f
        val mw = 236f
        val mh = 212f
        Maps.draw(p, r.map, mx, my, mw, mh)
        val colors = intArrayOf(C.RED, 0xFF2F6FD0.toInt(), C.GREEN_D, C.MUSTARD_D)
        for (i in r.paths.indices) {
            val pts = r.paths[i]
            val col = colors[i % colors.size]
            // each route is nudged a little sideways so shared streets still show both colours
            val off = (i - (r.paths.size - 1) / 2f) * 3f
            var k = 0
            while (k + 3 < pts.size) {
                p.line(mx + pts[k] / 100f * mw + off, my + pts[k + 1] / 100f * mh + off, mx + pts[k + 2] / 100f * mw + off, my + pts[k + 3] / 100f * mh + off, 3f, col)
                k += 2
            }
            val n = pts.size
            if (n >= 4) {
                // the letter sits in the middle of the route's second stretch (routes often share their first)
                val a0 = if (n >= 6) 2 else 0
                val lx = mx + (pts[a0] + pts[a0 + 2]) / 2f / 100f * mw + off
                val ly = my + (pts[a0 + 1] + pts[a0 + 3]) / 2f / 100f * mh + off
                p.fillOval(lx - 8f, ly - 8f, 16f, 16f, col)
                Text.line(p, r.ids[i], lx, ly, 11f, C.WHITE, Font.DISPLAY, Align.CENTER)
            }
        }
        val ln = line
        if (ln != null) {
            val bottom = Text.block(p, ln.fr, 262f, 16f, 128f, 11.5f, C.WHITE, Font.BOLD, Align.LEFT, 1.25f)
            if (bottom > 84f) { /* long lines push into the buttons; they are kept short in the unit files */ }
        }
        Text.block(p, r.prompt, 262f, 116f, 128f, 10.5f, C.MUSTARD, Font.BOLD, Align.LEFT, 1.25f)
        if (factHint) Text.block(p, "Not that one. Listen again.", 262f, 192f, 128f, 10f, C.MUSTARD, Font.TEXT, Align.LEFT, 1.2f)
        for (b in buttons) b.draw(p)
        // colour each route button like its line
        for (i in r.ids.indices) {
            val b = button("route:${r.ids[i]}") ?: continue
            p.fillRoundRect(b.x + 4f, b.y + b.h - 7f, b.w - 8f, 3f, 1.5f, colors[i % colors.size])
        }
    }

    override fun back(): Boolean { g.audio.stopVoice(); onExit(); return true }

    override fun describe(): String =
        "Talk ${talk.id} node=${node.id} ask=${node.ask} phase=$phase misses=$misses line=${line?.fr ?: "-"}"

    // ---------- for the scripted playthrough ----------

    fun waiting(): Boolean = phase == ASK
    fun askKind(): String = node.ask
    fun nodeId(): String = node.id
    fun reacting(): Boolean = phase == REACT
    /** In a duel: "intro", "moves", "speak", "listen", "busy" or "win". Empty when there is no duel. */
    fun duelStep(): String = if (!arena()) "" else when {
        phase == INTRO -> "intro"
        phase == WIN -> "win"
        dStep == D_MOVES -> "moves"
        dStep == D_SPEAK -> "speak"
        dStep == D_LISTEN -> "listen"
        else -> "busy"
    }
    fun duelDoubt(): Int = doubt
    fun duelBest(): String? = moodNow()?.best
    fun duelPolite(): Boolean = polite
    fun duelTurn(): Int = turn
    fun duelKinds(): List<String> = node.duel?.moves?.map { it.kind } ?: emptyList()
    fun duelStatus(): String = status
    fun noted(): Boolean = phase == NOTED
    /** How far through a sorting question she is, so a player can see it move on item by item. */
    fun progress(): Int = sortIdx
    /** Button ids that answer the current question correctly, in the order to tap them. */
    fun rightTaps(): List<String> {
        val out = ArrayList<String>()
        when (node.ask) {
            "choice" -> for (i in node.options.indices) if (node.options[i].next != null) { out.add("opt:$i"); break }
            "give" -> out.add("item:${node.answer}")
            "fact" -> node.fact?.let { out.add("fact:${it.answer}") }
            "route" -> node.route?.let { out.add("route:${it.answer}") }
            "plan" -> node.plan?.let { out.add("room:${it.answer}") }
            "auto" -> out.add("next")
            "sort" -> node.sort?.let { q -> if (sortIdx < q.items.size) out.add("bin:${q.items[sortIdx].bin}") }
            "order" -> node.order?.let { q ->
                // anything already put in the wrong place is taken back first
                var k = 0
                while (k < orderPicked.size && orderPicked[k] == k) k++
                if (k < orderPicked.size) out.add("slot:$k")
                for (i in k until q.items.size) out.add("step:$i")
            }
            "pay" -> {
                val pay = node.pay
                if (pay != null) {
                    var left = pay.amount - payTotal()
                    val coins = ArrayList<Int>(pay.coins)
                    coins.sortDescending()
                    for (c in coins) while (left >= c) { out.add("coin:$c"); left -= c }
                    out.add("pay")
                }
            }
            "build" -> {
                val b = node.build
                if (b != null) {
                    val used = ArrayList<Int>(answer)
                    for (k in locked until b.tiles.size) {
                        val want = g.fill(b.tiles[k])
                        for (i in tray.indices) if (tray[i] == want && !used.contains(i)) { used.add(i); out.add("tile:$i"); break }
                    }
                    out.add("check")
                }
            }
        }
        return out
    }
    /** Taps that give a wrong answer, to test that mistakes are handled. Empty if there is no wrong answer. */
    fun wrongTaps(): List<String> {
        val out = ArrayList<String>()
        when (node.ask) {
            "choice" -> for (i in node.options.indices) if (node.options[i].next == null && !tried.contains(i)) { out.add("opt:$i"); break }
            "give" -> for (i in node.items.indices) if (node.items[i] != node.answer && !tried.contains(i)) { out.add("item:${node.items[i]}"); break }
            "fact" -> node.fact?.let { f -> for (o in f.options) if (o != f.answer) { out.add("fact:$o"); break } }
            "route" -> node.route?.let { r -> for (o in r.ids) if (o != r.answer) { out.add("route:$o"); break } }
            "plan" -> node.plan?.let { q -> for (r in q.rooms) if (r.id != q.answer && !r.fixed && !planMarks.contains(r.id)) { out.add("room:${r.id}"); break } }
            "sort" -> node.sort?.let { q -> if (sortIdx < q.items.size) for (i in q.bins.indices) if (i != q.items[sortIdx].bin) { out.add("bin:$i"); break } }
            "order" -> node.order?.let { q -> if (q.items.size >= 2 && orderPicked.isEmpty()) { out.add("step:1"); out.add("step:0"); for (i in 2 until q.items.size) out.add("step:$i") } }
            "pay" -> node.pay?.let { pay -> if (pay.coins.isNotEmpty() && pay.coins[0] != pay.amount) { out.add("coin:${pay.coins[0]}"); out.add("pay") } }
            "build" -> {
                val right = rightTaps()
                if (right.size >= 3) { out.add(right[1]); out.add(right[0]); out.add("check") }
            }
        }
        return out
    }
}

/** A handwritten note, shown when a fact has to be read off it. */
object NoteView {
    fun draw(p: Painter, lines: List<String>, x: Float, y: Float, w: Float, h: Float) {
        p.fillRoundRect(x + 2f, y + 3f, w, h, 3f, C.alpha(C.BLACK, 0.3f))
        p.fillRoundRect(x, y, w, h, 3f, C.PAPER)
        var ly = y + 22f
        while (ly < y + h - 4f) { p.fillRect(x + 8f, ly, w - 16f, 0.6f, C.RULE); ly += 18f }
        var ty = y + 8f
        for (s in lines) {
            var size = 16f
            while (size > 10f && p.textWidth(Text.french(s), size, Font.HAND) > w - 28f) size -= 1f
            Text.line(p, Text.french(s), x + 14f, ty + 8f, size, C.INK, Font.HAND)
            ty += 18f
        }
    }
}

/** Small street maps for "which way?" questions. Routes are drawn on top by the conversation. */
object Maps {
    fun draw(p: Painter, name: String, x: Float, y: Float, w: Float, h: Float) {
        p.pushClip(x, y, w, h)
        val land = 0xFFEFE6CF.toInt()
        val street = 0xFFFFFFFF.toInt()
        val block = 0xFFD9CDB0.toInt()
        val water = 0xFF8EC5E8.toInt()
        p.fillRect(x, y, w, h, land)
        fun X(v: Float) = x + v / 100f * w
        fun Y(v: Float) = y + v / 100f * h
        fun rect(a: Float, b: Float, c: Float, d: Float, col: Int) = p.fillRect(X(a), Y(b), c / 100f * w, d / 100f * h, col)
        when (name) {
            "island" -> {
                // city blocks north and south of the river
                for (bx in floatArrayOf(4f, 28f, 52f, 76f)) {
                    rect(bx, 4f, 20f, 12f, block); rect(bx, 20f, 20f, 10f, block)
                    rect(bx, 72f, 20f, 10f, block); rect(bx, 86f, 20f, 10f, block)
                }
                // the river and its island
                rect(0f, 34f, 100f, 34f, water)
                p.fillRoundRect(X(22f), Y(42f), 56f / 100f * w, 18f / 100f * h, 10f, land)
                // bridges
                rect(34f, 60f, 5f, 12f, street); rect(62f, 60f, 5f, 12f, street)
                rect(34f, 30f, 5f, 12f, street); rect(62f, 30f, 5f, 12f, street)
                // the cathedral on the island
                Icons.draw(p, "cathedral", X(44f), Y(40f), 22f / 100f * h)
                Text.line(p, "la Seine", X(8f), Y(64f), 7.5f, 0xFF3F6DB5.toInt(), Font.TEXT)
                // you are here
                p.fillOval(X(50f) - 4f, Y(92f) - 4f, 8f, 8f, C.INK)
                Text.line(p, "Vous êtes ici", X(54f), Y(92f), 7.5f, C.INK, Font.BOLD)
            }
            else -> {
                for (bx in floatArrayOf(6f, 38f, 70f)) for (by in floatArrayOf(6f, 38f, 70f)) rect(bx, by, 24f, 24f, block)
            }
        }
        p.popClip()
        p.strokeRoundRect(x, y, w, h, 2f, 1.5f, C.INK)
    }
}

/** The boarding pass, shown in the conversation and again in the action scene. */
object TicketView {
    fun draw(p: Painter, tk: Ticket, x: Float, y: Float, w: Float, h: Float) {
        val cream = 0xFFFFF4C8.toInt()
        p.fillRoundRect(x + 2f, y + 3f, w, h, 6f, C.alpha(C.BLACK, 0.3f))
        p.fillRoundRect(x, y, w, h, 6f, cream)
        val band = h * 0.24f
        p.fillRoundRect(x, y, w, band + 6f, 6f, C.INK2)
        p.fillRect(x, y + band, w, 6.5f, cream)
        Text.line(p, tk.airline, x + 12f, y + band / 2f, band * 0.56f, C.WHITE, Font.DISPLAY)
        Icons.draw(p, "plane", x + w - band - 8f, y + 1f, band - 2f)
        // tear line and stub
        val stub = x + w * 0.80f
        var dy = y + band + 4f
        while (dy < y + h - 4f) { p.fillRect(stub, dy, 1f, 3f, C.GREY); dy += 6f }
        var bx = stub + 8f
        var k = 0
        while (bx < x + w - 8f) {
            val bw = if (k % 3 == 0) 2.4f else 1.2f
            p.fillRect(bx, y + band + 10f, bw, h - band - 20f, C.INK)
            bx += bw + 1.8f
            k++
        }
        // where to (sizes follow the height, so a small ticket stays tidy)
        val u = h / 100f
        Text.line(p, "DESTINATION", x + 12f * u, y + band + 14f * u, 6.5f * u, C.GREY, Font.BOLD)
        Text.line(p, tk.to, x + 12f * u, y + band + 36f * u, h * 0.26f, C.INK, Font.DISPLAY)
        // the numbered fields
        val n = tk.labels.size
        val left = x + w * 0.36f
        val colW = (stub - left - 4f) / Math.max(1, n)
        for (i in 0 until n) {
            val cx = left + colW * i + colW / 2f
            p.fillRoundRect(cx - colW / 2f + 2f, y + band + 8f * u, colW - 4f, h - band - 16f * u, 4f, C.alpha(C.WHITE, 0.7f))
            Text.line(p, tk.labels[i], cx, y + band + 19f * u, 9f * u, C.INK2, Font.BOLD, Align.CENTER)
            Text.line(p, tk.values[i], cx, y + band + 42f * u, h * 0.27f, C.INK, Font.DISPLAY, Align.CENTER)
        }
        p.strokeRoundRect(x, y, w, h, 6f, 1f, C.INK)
    }
}
