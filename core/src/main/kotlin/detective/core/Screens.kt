package detective.core

/**
 * Moves the player through a unit. Each stage of a unit is a list of steps in
 * the unit file (a story, a conversation, a walking scene, a taxi ride, a
 * police sketch, the notebook); this runs them in order.
 */
object Flow {
    /** Open a unit: show its opening story the first time, then its case-file page. */
    fun openUnit(g: Game, id: String) {
        g.select(id)
        val u = g.unit
        val key = u.intro
        val cards = if (key == null) null else u.story[key]
        if (cards != null && !g.save.intros.contains(u.id)) {
            g.go(StoryScreen(g, cards) {
                g.save.intros.add(u.id)
                g.save.commit()
                g.go(HubScreen(g))
            })
        } else g.go(HubScreen(g))
    }

    fun start(g: Game, stage: Int) { run(g, stage, 0) }

    fun run(g: Game, stage: Int, step: Int) {
        val u = g.unit
        val st = u.stages[stage]
        if (step >= st.steps.size) { done(g, stage); return }
        val s = st.steps[step]
        val next = { run(g, stage, step + 1) }
        val exit = { g.go(HubScreen(g)) }
        when (s.kind) {
            "drill" -> g.go(DrillScreen(g, s.ref, next))
            "story" -> g.go(StoryScreen(g, u.story[s.ref] ?: emptyList(), next))
            "talk" -> g.go(TalkScreen(g, s.ref, exit, next))
            "walk" -> g.go(WalkScreen(g, s.ref, exit, next))
            "ride" -> g.go(RideScreen(g, s.ref, exit, next))
            "sketch" -> g.go(SketchScreen(g, s.ref, exit, next))
            "game" -> g.go(Games.open(g, s.ref, exit, next))
            else -> throw IllegalArgumentException("unit ${u.id}: unknown step \"${s.kind}\"")
        }
    }

    fun done(g: Game, stage: Int) {
        g.save.setStage(g.unit.id, stage + 1)
        g.go(StageDoneScreen(g, stage))
    }
}

class TitleScreen(g: Game) : Screen(g) {
    private var confirm = false
    private var secretTaps = 0

    override fun tapUp(x: Float, y: Float) {
        // five taps on the name of the game open the secret code screen, for a parent whose save was lost
        if (!confirm && y > 30f && y < 90f) {
            secretTaps++
            if (secretTaps >= 5) g.go(CodeScreen(g))
        }
    }

    override fun enter() { build() }

    private fun build() {
        buttons.clear()
        if (confirm) {
            add(Button("reset_yes", 96f, 168f, 100f, 32f, "Yes, erase it", Style.BAD) { g.save.reset(); confirm = false; build() })
            add(Button("reset_no", 204f, 168f, 100f, 32f, "No, keep it", Style.PRIMARY) { confirm = false; build() })
            return
        }
        val has = g.save.name.isNotEmpty()
        val b = add(Button("start", 125f, 150f, 150f, 38f, if (has) "Continue" else "Start", Style.PRIMARY) {
            if (!has) g.go(NameScreen(g)) else Flow.openUnit(g, g.unit.id)
        })
        b.font = Font.DISPLAY
        b.size = 18f
        if (has) add(Button("reset", 160f, 200f, 80f, 24f, "Start over", Style.GHOST) { confirm = true; build() }).size = 10f
    }

    override fun draw(p: Painter) {
        Draw.nightSky(p, t)
        Draw.skyline(p, 240f, 0xFF0C1124.toInt())
        // lit windows
        var seed = 99
        for (i in 0 until 46) {
            seed = seed * 1103515245 + 12345
            val wx = ((seed ushr 8) and 0xFFFF) % 392
            seed = seed * 1103515245 + 12345
            val wy = 212 + ((seed ushr 8) and 0xFFFF) % 26
            p.fillRect(wx.toFloat(), wy.toFloat(), 2f, 3f, C.alpha(C.MUSTARD, 0.8f))
        }
        Text.line(p, "Détective à Paris", 200f + 1.5f, 62f + 2f, 38f, C.alpha(C.BLACK, 0.5f), Font.DISPLAY, Align.CENTER)
        Text.line(p, "Détective à Paris", 200f, 62f, 38f, C.MUSTARD, Font.DISPLAY, Align.CENTER)
        if (confirm) {
            Draw.card(p, 70f, 100f, 260f, 112f)
            Text.block(p, "Erase the case file of ${g.save.name} and start again?", 86f, 114f, 228f, 13f, C.INK, Font.BOLD, Align.CENTER)
        } else {
            val sub = if (g.save.name.isNotEmpty()) "Case file of ${g.save.name}" else "A French mystery"
            Text.line(p, sub, 200f, 104f, 14f, C.WHITE, Font.TEXT, Align.CENTER)
        }
        drawButtons(p)
    }

    override fun describe() = "Title confirm=$confirm"
}

/** Big letter keys, shared by the name screen and the secret code screen. */
object Keyboard {
    fun add(s: Screen, type: (Char) -> Unit, del: () -> Unit) {
        val rows = arrayOf("ABCDEFGHI", "JKLMNOPQR", "STUVWXYZ")
        val kw = 38f
        val kh = 28f
        val gap = 4f
        val x0 = (Game.W - (9 * kw + 8 * gap)) / 2f
        for (r in rows.indices) {
            for (i in 0 until rows[r].length) {
                val ch = rows[r][i]
                val b = s.add(Button("key:$ch", x0 + i * (kw + gap), 100f + r * (kh + gap), kw, kh, ch.toString(), Style.PLAIN) { type(ch) })
                b.size = 15f
            }
        }
        s.add(Button("del", x0 + 8 * (kw + gap), 100f + 2 * (kh + gap), kw, kh, "", Style.DARK) { del() }).icon = "back"
    }
}

/**
 * The secret code screen, opened by five taps on the title. Each unit has a
 * code word. Typing it marks every earlier unit finished, puts their words
 * back in the notebook, and opens that unit. It never takes progress away.
 */
class CodeScreen(g: Game) : Screen(g) {
    private var code = ""
    private val HELLO = "Type the code for a unit."
    private var message = HELLO
    private var shake = 0f

    override fun enter() {
        buttons.clear()
        Keyboard.add(this, { ch -> if (code.length < 12) { code += ch; message = HELLO; refresh() } },
            { if (code.isNotEmpty()) code = code.substring(0, code.length - 1); message = HELLO; refresh() })
        add(Button("cancel", 52f, 198f, 120f, 32f, "Cancel", Style.PLAIN) { g.go(TitleScreen(g)) })
        val ok = add(Button("ok", 228f, 198f, 144f, 32f, "Use code", Style.PRIMARY) { use() })
        ok.font = Font.DISPLAY
        ok.size = 15f
        refresh()
    }

    private fun refresh() { button("ok")?.enabled = code.isNotEmpty() }

    private fun use() {
        val i = Codes.find(g, code)
        if (i < 0) {
            g.audio.sfx("no")
            shake = 0.4f
            message = "That code does not open anything."
            code = ""
            refresh()
            return
        }
        Codes.apply(g, i)
        g.audio.sfx("stamp")
        val id = g.unitList[i].id
        if (g.save.name.isEmpty()) g.go(NameScreen(g, id)) else Flow.openUnit(g, id)
    }

    override fun update(dt: Float) { super.update(dt); if (shake > 0f) shake -= dt }

    override fun draw(p: Painter) {
        Draw.notebookPage(p)
        Text.line(p, "Secret code", 212f, 20f, 17f, C.INK, Font.DISPLAY, Align.CENTER)
        Text.line(p, message, 212f, 40f, 11f, C.INK2, Font.TEXT, Align.CENTER)
        val dx = if (shake > 0f) Math.sin((shake * 60f).toDouble()).toFloat() * 4f else 0f
        p.fillRoundRect(92f + dx, 56f, 240f, 34f, 8f, C.WHITE)
        p.strokeRoundRect(92f + dx, 56f, 240f, 34f, 8f, 1.5f, C.INK)
        Text.line(p, code, 212f + dx, 73f, 20f, C.INK, Font.BOLD, Align.CENTER)
        drawButtons(p)
    }

    override fun back(): Boolean { g.go(TitleScreen(g)); return true }
    override fun describe() = "Code \"$code\""
}

/** Finding and using the units' secret codes. */
object Codes {
    /** The place in the unit list of the unit whose code this is, or -1. */
    fun find(g: Game, typed: String): Int {
        val c = typed.trim().uppercase()
        if (c.isEmpty()) return -1
        for (i in g.unitList.indices) if (g.unitById(g.unitList[i].id).code == c) return i
        return -1
    }

    /** Mark every unit before unit [index] finished, as if it had been played, and open unit [index]. */
    fun apply(g: Game, index: Int) {
        val s = g.save
        for (k in 0 until index) {
            val u = g.unitById(g.unitList[k].id)
            s.setStage(u.id, u.stages.size)
            s.pages[u.id] = 0
            if (!s.intros.contains(u.id)) s.intros.add(u.id)
            for (w in u.words) s.meet(w.id)
            for (t in u.tips) if (!s.tips.contains(t.id)) s.tips.add(t.id)
            // the facts she wrote down in the unit's conversations go back in the notebook too
            for (tk in u.talks.values) for (n in tk.nodes.values) {
                val f = n.fact ?: continue
                val key = u.id + "/" + f.key
                if (!s.facts.containsKey(key)) s.facts[key] = (f.label + " " + f.answer).trim()
            }
        }
        s.current = g.unitList[index].id
        s.commit()
    }
}

/** The player types the detective's name on big keys. */
class NameScreen(g: Game, private val then: String? = null) : Screen(g) {
    private var name = ""
    private val maxLen = 12

    override fun enter() {
        buttons.clear()
        Keyboard.add(this, { ch -> type(ch) }, { if (name.isNotEmpty()) name = name.substring(0, name.length - 1); refresh() })
        val kw = 38f
        val gap = 4f
        val x0 = (Game.W - (9 * kw + 8 * gap)) / 2f
        add(Button("space", x0, 198f, 4 * kw + 3 * gap, 32f, "space", Style.PLAIN) { type(' ') }).size = 11f
        val ok = add(Button("ok", x0 + 5 * (kw + gap), 198f, 4 * kw + 3 * gap, 32f, "That's me!", Style.PRIMARY) {
            g.save.name = name.trim()
            g.save.commit()
            Flow.openUnit(g, then ?: g.unitList[0].id)
        })
        ok.font = Font.DISPLAY
        ok.size = 15f
        refresh()
    }

    private fun type(ch: Char) {
        if (name.length >= maxLen) return
        if (ch == ' ') {
            if (name.isEmpty() || name.endsWith(" ") || name.contains(" ")) return
            name += " "
        } else {
            val upper = name.isEmpty() || name.endsWith(" ")
            name += if (upper) ch.uppercaseChar() else ch.lowercaseChar()
        }
        refresh()
    }

    private fun refresh() {
        button("ok")?.enabled = name.trim().isNotEmpty()
    }

    override fun draw(p: Painter) {
        Draw.notebookPage(p)
        Text.line(p, "Every detective needs a name.", 212f, 20f, 17f, C.INK, Font.DISPLAY, Align.CENTER)
        Text.line(p, "What is yours?", 212f, 40f, 12f, C.INK2, Font.TEXT, Align.CENTER)
        p.fillRoundRect(92f, 56f, 240f, 34f, 8f, C.WHITE)
        p.strokeRoundRect(92f, 56f, 240f, 34f, 8f, 1.5f, C.INK)
        val w = p.textWidth(name, 22f, Font.HAND)
        Text.line(p, name, 212f, 73f, 22f, C.INK, Font.HAND, Align.CENTER)
        if ((t * 2f).toInt() % 2 == 0) p.fillRect(212f + w / 2f + 2f, 63f, 1.5f, 20f, C.INK)
        drawButtons(p)
    }

    override fun back(): Boolean { g.go(TitleScreen(g)); return true }
    override fun describe() = "Name \"$name\""
}

/** Story cards: a picture, a few plain sentences, and sometimes one French line. */
class StoryScreen(g: Game, private val cards: List<StoryCard>, private val onDone: () -> Unit) : Screen(g) {
    private var index = 0
    private var showEnglish = false

    override fun enter() {
        if (cards.isEmpty()) { onDone(); return }
        show()
    }

    private fun show() {
        buttons.clear()
        clearTimers()
        showEnglish = false
        val last = index == cards.size - 1
        val nx = add(Button("next", 304f, 204f, 88f, 30f, if (last) "Go!" else "Next", Style.PRIMARY) {
            if (index < cards.size - 1) { index++; g.audio.stopVoice(); show() } else onDone()
        })
        nx.font = Font.DISPLAY
        nx.size = 15f
        if (index > 0) add(Button("prev", 8f, 204f, 44f, 30f, "", Style.PLAIN) { index--; g.audio.stopVoice(); show() }).icon = "back"
        val ln = cards[index].line
        if (ln != null) {
            after(0.5f) { g.say(ln) }
            val sp = add(Button("again", 64f, 14f, 30f, 24f, "", Style.DARK) { g.say(ln) })
            sp.icon = "speaker"
        }
    }

    override fun tapUp(x: Float, y: Float) {
        // in "tap" units the English under a French line shows only when the bubble is tapped
        if (cards[index].line != null && x > 150f && x < 340f && y > 12f && y < 90f) showEnglish = !showEnglish
    }

    override fun draw(p: Painter) {
        val c = cards[index]
        if (c.scene == "letter") { drawLetter(p, c); drawButtons(p); return }
        p.fillRect(0f, 0f, Game.W, Game.H, C.NIGHT)
        val fx = 60f
        val fy = 10f
        val fw = 280f
        val fh = 126f
        p.fillRoundRect(fx - 4f, fy - 4f, fw + 8f, fh + 8f, 6f, C.PAPER)
        if (c.people.isNotEmpty()) Scenes.people(g, p, c.people, fx, fy, fw, fh)
        else Scenes.draw(g, p, c.scene, fx, fy, fw, fh, t)
        val ln = c.line
        if (ln != null) {
            // the bubble grows with the line: up to three lines of French, then the help under it
            val bw = 172f
            val bx = 334f - bw
            var size = 12.5f
            while (size > 9.5f && Text.wrap(p, ln.fr, size, Font.BOLD, bw - 18f).size > 3) size -= 0.5f
            val lines = Text.wrap(p, ln.fr, size, Font.BOLD, bw - 18f)
            val under = g.unit.english == "under" || showEnglish
            val en = if (ln.en.isNotEmpty()) ln.en else ln.gloss.joinToString(", ")
            val helpLines = if (en.isEmpty()) 0 else if (under) Text.wrap(p, en, 9.5f, Font.TEXT, bw - 18f).size else 1
            val bh = 12f + lines.size * size * 1.25f + helpLines * 11.5f
            Draw.bubble(p, bx, 16f, bw, bh, 16f + Math.min(bh - 10f, 30f))
            val bottom2 = Text.block(p, ln.fr, bx + 9f, 22f, bw - 18f, size, C.INK, Font.BOLD, Align.LEFT, 1.25f)
            if (en.isNotEmpty() && under) Text.block(p, en, bx + 9f, bottom2 + 1f, bw - 18f, 9.5f, C.GREY, Font.TEXT, Align.LEFT, 1.2f)
            else if (en.isNotEmpty()) Text.line(p, "tap for help", bx + 9f, bottom2 + 6f, 8.5f, C.GREY_L, Font.TEXT)
        }
        // long cards get smaller type, so the text and the note both fit above the buttons
        val body = g.fill(Text.french(c.text))
        var ts = 12.5f
        val room = if (c.note.isNotEmpty()) 3 else 4
        while (ts > 10f && Text.wrap(p, body, ts, Font.TEXT, 328f).size > room) ts -= 0.5f
        val bottom = Text.block(p, body, 36f, 146f, 328f, ts, C.WHITE, Font.TEXT, Align.CENTER, 1.3f)
        if (c.note.isNotEmpty()) Text.block(p, c.note, 60f, Math.min(bottom + 2f, 196f), 240f, 9f, C.MUSTARD, Font.TEXT, Align.CENTER, 1.2f)
        // page dots
        if (cards.size > 1) {
            val x0 = 200f - (cards.size - 1) * 5f
            for (i in cards.indices) p.fillOval(x0 + i * 10f - 2.5f, 225f, 5f, 5f, if (i == index) C.MUSTARD else C.INK2)
        }
        drawButtons(p)
    }

    private fun drawLetter(p: Painter, c: StoryCard) {
        p.fillRect(0f, 0f, Game.W, Game.H, 0xFF6B4A2F.toInt())
        p.fillRoundRect(46f, 9f, 312f, 190f, 3f, C.alpha(C.BLACK, 0.25f))
        p.fillRoundRect(44f, 6f, 312f, 190f, 3f, C.PAPER)
        // stamp and postmark
        p.fillRect(322f, 11f, 26f, 30f, C.RED)
        p.fillRect(324.5f, 13.5f, 21f, 25f, 0xFFF3B9B5.toInt())
        Text.line(p, "FRANCE", 335f, 34.5f, 4.5f, C.RED_D, Font.BOLD, Align.CENTER)
        Icons.draw(p, "plane", 326.5f, 13.5f, 17f)
        val parts = g.fill(c.text).split('\n')
        var y = 16f
        for (i in parts.indices) {
            // the last one or two short lines are the signature
            val signature = i > 0 && i >= parts.size - 2 && parts[i].length <= 34
            val size = if (signature) 15f else 17f
            if (signature) {
                if (i == parts.size - 2) y += 8f
                p.text(parts[i], 340f, y + size * 0.82f, size, C.INK, Font.HAND, Align.RIGHT)
                y += size * 1.15f
            } else {
                // the greeting sits beside the stamp; the rest starts under it
                y = Text.block(p, parts[i], 60f, y, if (i == 0) 240f else 282f, size, C.INK, Font.HAND, Align.LEFT, 1.2f) + (if (i == 0) 10f else 4f)
            }
        }
        if (c.note.isNotEmpty()) Text.block(p, c.note, 60f, 202f, 236f, 9.5f, C.PAPER, Font.TEXT, Align.LEFT, 1.25f)
    }

    override fun back(): Boolean { g.go(if (g.save.intros.contains(g.unit.id)) HubScreen(g) else TitleScreen(g)); return true }
    override fun describe() = "Story ${index + 1}/${cards.size} scene=${cards[index].scene}"
}

/**
 * The pictures on story cards. A unit file can describe a picture with tiles
 * and sprites (see "scenes" in docs/UNIT_FORMAT.md); a few special ones are
 * drawn here in code.
 */
object Scenes {
    private const val T = 24f // tile size inside a story picture

    private fun tile(g: Game, p: Painter, name: String, x: Float, y: Float) {
        p.image(g.art.get("tile_$name"), x, y, T + 0.4f, T + 0.4f, false, 1f)
    }

    private fun person(g: Game, p: Painter, img: Img, x: Float, feetY: Float, scale: Float, flip: Boolean = false) {
        p.image(img, x - img.w * scale / 2f, feetY - img.h * scale, img.w * scale, img.h * scale, flip, 1f)
    }

    /** People side by side against a height chart, for "look closely" cards. */
    fun people(g: Game, p: Painter, list: List<Person>, x: Float, y: Float, w: Float, h: Float) {
        p.pushClip(x, y, w, h)
        p.fillRect(x, y, w, h, 0xFFE4ECF6.toInt())
        var ly = y + 14f
        while (ly < y + h) { p.fillRect(x, ly, w, 0.8f, C.GREY_L); ly += 18f }
        val n = list.size
        for (i in 0 until n) {
            val per = list[i]
            val cx = x + w * (i + 0.5f) / n
            val ph = if (per.tall) 112f else 92f
            val pw = ph * Faces.W / Faces.H
            val img = Faces.get("story" + Json.write(per.face.v), FaceSpec.from(per.face), "neutral")
            p.image(img, cx - pw / 2f, y + h - ph, pw, ph, false, 1f)
        }
        p.popClip()
    }

    fun draw(g: Game, p: Painter, name: String, x: Float, y: Float, w: Float, h: Float, t: Float) {
        val data = g.unit.scenes[name]
        if (data != null) { drawData(g, p, data, x, y, w, h, t); return }
        p.pushClip(x, y, w, h)
        val cols = (w / T).toInt() + 1
        when {
            name.startsWith("icon:") -> {
                p.fillRect(x, y, w, h, C.PAPER)
                Icons.draw(p, name.substring(5), x + w / 2f - 52f, y + h / 2f - 52f, 104f)
            }
            name == "office" || name == "office_letter" -> {
                for (c in 0 until cols) {
                    tile(g, p, if (c == 2 || c == 3) "owindow" else "paper_top", x + c * T, y)
                    tile(g, p, if (c == 7 || c == 8 || c == 9) "shelf" else "paper", x + c * T, y + T)
                    for (r in 2..5) tile(g, p, "wood", x + c * T, y + r * T)
                }
                person(g, p, Looks.hero(g, 0, 0), x + 130f, y + 100f, 2.2f)
                tile(g, p, "desk", x + 4 * T, y + 3 * T + 8f)
                tile(g, p, "desk", x + 5 * T, y + 3 * T + 8f)
                tile(g, p, "desk", x + 6 * T, y + 3 * T + 8f)
                tile(g, p, "plant", x + 10 * T, y + 2 * T + 4f)
                if (name == "office_letter") {
                    val bob = Math.sin(t * 3.0).toFloat() * 2f
                    Icons.draw(p, "letter", x + 196f, y + 26f + bob, 46f)
                }
            }
            name == "map" -> {
                p.fillRect(x, y, w, h, C.SKY)
                // home, on the left
                p.fillOval(x - 30f, y + 20f, 110f, 120f, 0xFF8CC97A.toInt())
                p.fillOval(x + 20f, y + 60f, 70f, 80f, 0xFF8CC97A.toInt())
                // France, on the right
                p.fillPoly(floatArrayOf(x + 196f, y + 26f, x + 232f, y + 14f, x + 262f, y + 30f, x + 268f, y + 70f, x + 250f, y + 104f, x + 214f, y + 110f, x + 190f, y + 84f, x + 176f, y + 52f), 0xFF8CC97A.toInt())
                Text.line(p, "FRANCE", x + 226f, y + 84f, 9f, C.GREEN_D, Font.BOLD, Align.CENTER)
                // dotted flight path
                val n = 16
                for (i in 0..n) {
                    val f = i / n.toFloat()
                    val px = x + 66f + (224f - 66f) * f
                    val py = y + 78f + (44f - 78f) * f - Math.sin(f * Math.PI).toFloat() * 34f
                    p.fillOval(px - 1.5f, py - 1.5f, 3f, 3f, C.INK)
                }
                val f = (t * 0.25f) % 1f
                val ax = x + 66f + (224f - 66f) * f
                val ay = y + 78f + (44f - 78f) * f - Math.sin(f * Math.PI).toFloat() * 34f
                val pl = g.art.get("plane")
                p.image(pl, ax - 18f, ay - 10f, 36f, 15f, false, 1f)
                p.fillOval(x + 60f, y + 73f, 9f, 9f, C.INK)
                Text.line(p, "Home", x + 64f, y + 94f, 9f, C.INK, Font.BOLD, Align.CENTER)
                p.fillOval(x + 220f, y + 39f, 9f, 9f, C.RED)
                Text.line(p, "Paris", x + 240f, y + 56f, 11f, C.INK, Font.DISPLAY, Align.CENTER)
            }
            name == "notebook" -> {
                p.fillRect(x, y, w, h, 0xFF8D6A45.toInt())
                p.fillRoundRect(x + 62f, y + 12f, 160f, 108f, 4f, C.INK)
                p.fillRoundRect(x + 70f, y + 16f, 148f, 100f, 2f, C.PAPER)
                var ly = y + 36f
                while (ly < y + 110f) { p.fillRect(x + 70f, ly, 148f, 0.6f, C.RULE); ly += 14f }
                var hy = y + 20f
                while (hy < y + 112f) { p.fillRoundRect(x + 58f, hy, 14f, 4f, 2f, C.GREY_L); hy += 12f }
                Text.line(p, "Bonjour", x + 84f, y + 28f, 15f, C.INK, Font.HAND)
                Text.line(p, "Merci", x + 84f, y + 56f, 15f, C.INK, Font.HAND)
                Text.line(p, "Au revoir", x + 84f, y + 84f, 15f, C.INK, Font.HAND)
                Icons.draw(p, "loupe", x + 214f, y + 54f, 60f)
            }
            name == "airport" -> {
                p.fillRect(x, y, w, h, C.SKY)
                p.image(g.art.get("cloud"), x + 20f + (t * 3f) % 40f, y + 12f, 48f, 20f, false, 1f)
                p.image(g.art.get("plane"), x + 150f + t * 6f % 60f, y + 6f - (t * 2f) % 20f, 96f, 40f, false, 1f)
                for (c in 0 until cols) {
                    tile(g, p, "roof", x + c * T, y + 2 * T)
                    tile(g, p, if (c == 5) "door" else if (c % 3 == 1) "window" else "wall", x + c * T, y + 3 * T)
                    tile(g, p, "floor", x + c * T, y + 4 * T)
                    tile(g, p, "floor", x + c * T, y + 5 * T)
                }
                p.fillRoundRect(x + 96f, y + 54f, 72f, 15f, 2f, C.MUSTARD)
                Text.line(p, "DÉPARTS", x + 132f, y + 61.5f, 10f, C.INK, Font.BOLD, Align.CENTER)
                val step = if ((t * 4f).toInt() % 2 == 0) 1 else 2
                person(g, p, Looks.hero(g, 1, step), x + 132f, y + 122f, 1.8f)
            }
            name == "hall" -> {
                for (c in 0 until cols) {
                    tile(g, p, "roof", x + c * T, y)
                    tile(g, p, if (c % 4 == 2) "window" else "wall", x + c * T, y + T)
                    for (r in 2..5) tile(g, p, "floor", x + c * T, y + r * T)
                }
                tile(g, p, "plant", x + 10 * T, y + 2 * T)
                tile(g, p, "seat", x + 1 * T, y + 2 * T)
                tile(g, p, "seat", x + 2 * T, y + 2 * T)
                p.fillRoundRect(x + 150f, y + 6f, 96f, 16f, 2f, C.MUSTARD)
                Text.line(p, "Portes", x + 160f, y + 14f, 11f, C.INK, Font.BOLD)
                Draw.arrow(p, x + 230f, y + 14f, 11f, "right", C.INK)
                val step = if ((t * 5f).toInt() % 2 == 0) 0 else 1
                val hx = x + 60f + (t * 22f) % 150f
                person(g, p, Looks.heroSide(g, step), hx, y + 110f, 2f)
                val cs = Looks.img(g, "case_red", if (step == 0) 0 else 1)
                p.image(cs, x + 230f, y + 76f, 28f, 28f, false, 1f)
            }
            name == "cabin" -> {
                for (c in 0 until cols) {
                    tile(g, p, "cabin", x + c * T, y)
                    tile(g, p, "pseat", x + c * T, y + T)
                    tile(g, p, "aisle", x + c * T, y + 2 * T)
                    tile(g, p, "aisle", x + c * T, y + 3 * T)
                    tile(g, p, "pseat", x + c * T, y + 4 * T)
                    tile(g, p, "cabin", x + c * T, y + 5 * T)
                }
                person(g, p, Looks.img(g, "crew", 0), x + 108f, y + 92f, 2f)
                person(g, p, Looks.hero(g, 0, 0), x + 52f, y + 94f, 2f)
            }
            name == "sky" || name == "sky_night" -> {
                val night = name == "sky_night"
                p.fillRect(x, y, w, h, if (night) C.NIGHT else C.SKY)
                if (!night) p.fillRect(x, y + h * 0.6f, w, h * 0.4f, 0xFFB9E0F7.toInt())
                val cl = g.art.get("cloud")
                for (i in 0 until 5) {
                    val cx = x + w - ((t * (10f + i * 4f) + i * 77f) % (w + 80f))
                    p.image(cl, cx, y + 10f + i * 22f, 60f - i * 4f, 25f - i * 1.5f, false, if (night) 0.25f else 0.95f)
                }
                val bob = Math.sin(t * 1.6).toFloat() * 3f
                p.image(g.art.get("plane"), x + 80f, y + 34f + bob, 132f, 55f, false, 1f)
            }
            name == "paris" || name == "paris_night" -> {
                val night = name == "paris_night"
                p.fillRect(x, y, w, h, if (night) C.NIGHT else C.SKY)
                if (night) { p.fillOval(x + 226f, y + 12f, 22f, 22f, 0xFFFDF3C4.toInt()); p.fillOval(x + 233f, y + 10f, 20f, 20f, C.NIGHT) }
                else p.image(g.art.get("cloud"), x + 180f, y + 14f, 60f, 25f, false, 1f)
                p.pushClip(x, y, w, h)
                p.fillRect(x, y + h - 14f, w, 14f, if (night) 0xFF0C1124.toInt() else 0xFF8FA0B8.toInt())
                Draw.skylineAt(p, x - 60f, y + h - 12f, if (night) 0xFF0C1124.toInt() else 0xFF6F7B99.toInt())
                p.popClip()
            }
            else -> p.fillRect(x, y, w, h, C.INK2)
        }
        p.popClip()
    }

    private fun drawData(g: Game, p: Painter, d: SceneData, x: Float, y: Float, w: Float, h: Float, t: Float) {
        p.pushClip(x, y, w, h)
        val ts = g.tilesets[d.tileset]
        p.fillRect(x, y, w, h, if (d.night) C.NIGHT else if (d.sky) C.SKY else (ts?.void ?: C.NIGHT))
        if (d.sky && !d.night) p.image(g.art.get("cloud"), x + 30f + (t * 3f) % 50f, y + 8f, 48f, 20f, false, 1f)
        val floor = g.art.get(ts?.floor ?: "tile_floor")
        for (r in d.rows.indices) {
            val row = d.rows[r]
            for (c in 0 until row.length) {
                val ch = row[c]
                if (ch == ' ') continue
                val px = x + c * T
                val py = y + r * T
                if (ch == '.') { p.image(floor, px, py, T + 0.4f, T + 0.4f, false, 1f); continue }
                val def = ts?.defs?.get(ch) ?: continue
                if (def.under) p.image(floor, px, py, T + 0.4f, T + 0.4f, false, 1f)
                p.image(g.art.get(def.img), px, py, T + 0.4f, T + 0.4f, false, 1f)
            }
        }
        for (s in d.signs) {
            val tw = p.textWidth(s.text, 9f, Font.BOLD) + 12f + (if (s.dir.isNotEmpty()) 12f else 0f)
            val sx = x + s.x * T + T / 2f
            val sy = y + s.y * T + T / 2f
            p.fillRoundRect(sx - tw / 2f, sy - 7f, tw, 14f, 2f, C.INK)
            p.fillRoundRect(sx - tw / 2f + 1f, sy - 6f, tw - 2f, 12f, 1.5f, C.MUSTARD)
            if (s.dir.isNotEmpty()) {
                Draw.arrow(p, sx - tw / 2f + 9f, sy, 9f, s.dir, C.INK)
                Text.line(p, s.text, sx - tw / 2f + 17f, sy, 9f, C.INK, Font.BOLD)
            } else Text.line(p, s.text, sx, sy, 9f, C.INK, Font.BOLD, Align.CENTER)
        }
        val list = ArrayList<SceneSprite>(d.sprites)
        list.sortBy { it.y }
        val step = (t * 4f).toInt() % 2
        for (s in list) {
            val img = if (s.look == "hero") Looks.hero(g, 0, s.frame) else Looks.img(g, s.look, if (s.frame < 0) step else s.frame)
            person(g, p, img, x + s.x * T + T / 2f, y + (s.y + 1f) * T, s.scale * 1.5f, s.flip)
        }
        for (ic in d.icons) Icons.draw(p, ic.name, x + ic.x * T, y + ic.y * T, ic.size)
        p.popClip()
    }
}

/**
 * Sprites for people and moving things. A "look" is a base drawing plus a
 * change of colours, listed in assets/art/looks.json.
 */
object Looks {
    private class Look(val base: String, val swaps: Map<Char, Int>)

    private var table: HashMap<String, Look>? = null

    private fun table(g: Game): HashMap<String, Look> {
        table?.let { return it }
        val out = HashMap<String, Look>()
        val src = g.assets.text("art/looks.json")
        if (src != null) {
            val j = Json.parse(src)
            for (name in j.keys()) {
                val l = j[name]
                val swaps = HashMap<Char, Int>()
                val sj = l["swaps"]
                for (k in sj.keys()) if (k.isNotEmpty()) swaps[k[0]] = Art.parseColor(sj.str(k))
                out[name] = Look(l.str("base"), swaps)
            }
        }
        table = out
        return out
    }

    fun known(g: Game, look: String): Boolean =
        look == "hero" || table(g).containsKey(look) || g.art.has(look) || g.art.has(look + "_0")

    /** dir: 0 down, 1 up. step: 0 standing, 1 and 2 walking. */
    fun hero(g: Game, dir: Int, step: Int): Img = dressed(g, (if (dir == 1) "hero_up_" else "hero_down_") + step)

    fun heroSide(g: Game, step: Int): Img = dressed(g, "hero_side_$step")

    /** A unit can change the detective's clothes (a yellow hat, a green scarf) with "hero" colour swaps. */
    private fun dressed(g: Game, name: String): Img {
        val sw = g.unit.heroSwaps
        return if (sw.isEmpty()) g.art.get(name) else g.art.variant(name, "hero@" + g.unit.id, sw)
    }

    /** [frame] 0 is standing still; 1 and 2 are two steps of a walk (or two wheel positions). */
    fun img(g: Game, look: String, frame: Int): Img {
        val l = table(g)[look]
        val base = l?.base ?: look
        val name = when {
            g.art.has(base + "_" + frame) -> base + "_" + frame
            g.art.has(base + "_0") -> base + "_0"
            else -> base
        }
        return if (l == null || l.swaps.isEmpty()) g.art.get(name) else g.art.variant(name, look, l.swaps)
    }
}

/** The case file: the three stages of a unit, and the other units of the case. */
class HubScreen(g: Game) : Screen(g) {
    private var secretTaps = 0

    override fun enter() {
        buttons.clear()
        val u = g.unit
        val done = g.save.stage(u.id)
        val n = u.stages.size
        val gap = if (n > 3) 7f else 10f
        val cw = Math.min(112f, (364f - (n - 1) * gap) / n)
        val x0 = 28f + (364f - (n * cw + (n - 1) * gap)) / 2f
        for (i in 0 until n) {
            val b = add(Button("stage:$i", x0 + i * (cw + gap), 50f, cw, 116f, "", Style.PLAIN) { Flow.start(g, i) })
            b.enabled = i <= done
        }
        add(Button("book", 366f, 4f, 28f, 22f, "", Style.DARK) { g.openOverlay(NotebookScreen(g)) }).icon = "book"
        val key = u.intro
        if (key != null) add(Button("story", 300f, 4f, 60f, 22f, "Story", Style.PLAIN) {
            g.go(StoryScreen(g, u.story[key] ?: emptyList()) { g.go(HubScreen(g)) })
        }).size = 10f
        // with more than one case, a button opens the list of cases
        if (g.cases.size > 1) add(Button("cases", 234f, 4f, 60f, 22f, "Cases", Style.PLAIN) { g.go(CasesScreen(g)) }).size = 10f
        // back to the title screen, without closing the app
        add(Button("home", if (g.cases.size > 1) 200f else 266f, 4f, 28f, 22f, "", Style.PLAIN) { g.go(TitleScreen(g)) }).icon = "home"
        // the other units of this case, as numbered buttons
        val list = g.caseOf(u.id).units
        val bw = 30f
        val bx0 = 30f + 92f
        for (i in list.indices) {
            val info = list[i]
            val b = add(Button("unit:${info.id}", bx0 + i * (bw + 4f), 196f, bw, 30f, "${info.number}",
                if (info.id == u.id) Style.DARK else Style.PLAIN) { Flow.openUnit(g, info.id) })
            b.font = Font.DISPLAY
            b.size = 14f
            b.enabled = g.unlocked(info.id)
        }
    }

    override fun tapUp(x: Float, y: Float) {
        // five taps on the heading open every unit, for a parent or a tester
        if (y < 44f && x < 290f) {
            secretTaps++
            if (secretTaps >= 5 && !g.save.unlockAll) {
                g.save.unlockAll = true
                g.save.commit()
                g.audio.sfx("stamp")
                enter()
            }
        }
    }

    override fun draw(p: Painter) {
        Draw.notebookPage(p)
        val u = g.unit
        val case = g.caseOf(u.id)
        var size = 20f
        val head = "${u.number}. ${u.title}"
        while (size > 13f && p.textWidth(head, size, Font.DISPLAY) > 340f) size -= 1f
        // the small line above must not run under the buttons on the right
        val room = if (g.cases.size > 1) 164f else 230f
        var cs = 10f
        val caseLine = "Case file of ${g.save.name}: ${case.title}"
        while (cs > 7.5f && p.textWidth(caseLine, cs, Font.TEXT) > room) cs -= 0.5f
        Text.line(p, caseLine, 30f, 13f, cs, C.INK2, Font.TEXT)
        Text.line(p, head, 30f, 37f, size, C.INK, Font.DISPLAY)
        val done = g.save.stage(u.id)
        for (i in u.stages.indices) {
            val s = u.stages[i]
            val b = button("stage:$i") ?: continue
            val locked = i > done
            val a = if (locked) 0.45f else 1f
            p.fillRoundRect(b.x, b.y + 3f, b.w, b.h, 8f, C.alpha(C.BLACK, 0.18f))
            val lift = if (b.pressed) 2f else 0f
            p.fillRoundRect(b.x, b.y + lift, b.w, b.h, 8f, if (locked) C.PAPER2 else C.WHITE)
            if (i == done) p.strokeRoundRect(b.x, b.y + lift, b.w, b.h, 8f, 2.5f, C.MUSTARD)
            else p.strokeRoundRect(b.x, b.y + lift, b.w, b.h, 8f, 1f, C.GREY_L)
            // narrow cards (four or five stages) get a smaller icon and smaller type
            val narrow = b.w < 100f
            val isz = if (narrow) 34f else 42f
            Icons.draw(p, if (locked) "lock" else s.icon, b.x + b.w / 2f - isz / 2f, b.y + lift + 9f, isz, C.alpha(C.INK, a))
            var ts = 16f
            while (ts > 10f && p.textWidth(s.title, ts, Font.DISPLAY) > b.w - 8f) ts -= 0.5f
            Text.line(p, s.title, b.x + b.w / 2f, b.y + lift + 62f, ts, C.alpha(C.INK, a), Font.DISPLAY, Align.CENTER)
            Text.block(p, s.sub, b.x + 4f, b.y + lift + 72f, b.w - 8f, if (narrow) 8.5f else 10f, C.alpha(C.INK2, a), Font.TEXT, Align.CENTER, 1.2f)
            if (i < done) Draw.stamp(p, b.x + b.w - (if (narrow) 13f else 20f), b.y + lift + (if (narrow) 13f else 20f), if (narrow) 11f else 15f, "OK", C.RED)
            else if (i == done) {
                p.fillRoundRect(b.x + b.w / 2f - 24f, b.y + lift + 96f, 48f, 15f, 7.5f, C.MUSTARD)
                Text.line(p, "PLAY", b.x + b.w / 2f, b.y + lift + 103.5f, 9.5f, C.INK, Font.BOLD, Align.CENTER)
            }
        }
        Text.line(p, "Units in this case", 30f, 204f, 9f, C.INK2, Font.BOLD)
        Text.line(p, "tap a number", 30f, 216f, 8f, C.GREY, Font.TEXT)
        // the three big cards are drawn above; draw the small buttons normally
        for (b in buttons) if (!b.id.startsWith("stage:")) b.draw(p)
        // a tick on finished units, a padlock on locked ones
        for (info in case.units) {
            val b = button("unit:${info.id}") ?: continue
            if (g.finished(info.id)) Icons.draw(p, "check", b.x + b.w - 11f, b.y - 3f, 11f, C.GREEN)
            else if (!g.unlocked(info.id)) Icons.draw(p, "lock", b.x + b.w - 10f, b.y - 3f, 10f, C.GREY)
        }
    }

    override fun back(): Boolean { g.go(TitleScreen(g)); return true }
    override fun describe() = "Hub unit=${g.unit.id} done=${g.save.stage(g.unit.id)}"
}

/** The list of cases, for when there is more than one. A case opens when the one before it is finished. */
class CasesScreen(g: Game) : Screen(g) {
    private var page = 0
    private val perPage = 5

    override fun enter() {
        // open on the page that holds the current case
        val cur = g.cases.indexOfFirst { c -> c.units.any { it.id == g.unit.id } }
        if (cur >= 0) page = cur / perPage
        rebuild()
    }

    /** The unit to open for a case: the first one not finished yet, or the first of all. */
    private fun target(c: CaseInfo): String {
        for (u in c.units) if (g.unlocked(u.id) && !g.finished(u.id)) return u.id
        return c.units[0].id
    }

    private fun rebuild() {
        buttons.clear()
        add(Button("back", 6f, 6f, 30f, 24f, "", Style.DARK) { g.go(HubScreen(g)) }).icon = "back"
        val from = page * perPage
        for (i in from until Math.min(g.cases.size, from + perPage)) {
            val c = g.cases[i]
            if (c.units.isEmpty()) continue
            val b = add(Button("case:${c.id}", 44f, 44f + (i - from) * 36f, 320f, 31f, "", Style.PLAIN) { Flow.openUnit(g, target(c)) })
            b.enabled = g.unlocked(c.units[0].id)
        }
        val pages = (g.cases.size + perPage - 1) / perPage
        if (pages > 1) {
            add(Button("prev", 150f, 208f, 40f, 24f, "", Style.PLAIN) { if (page > 0) { page--; rebuild() } }).also { it.icon = "back"; it.enabled = page > 0 }
            add(Button("more", 210f, 208f, 40f, 24f, "", Style.PLAIN) { if (page < pages - 1) { page++; rebuild() } }).also { it.icon = "next"; it.enabled = page < pages - 1 }
        }
    }

    override fun draw(p: Painter) {
        Draw.notebookPage(p)
        Text.line(p, "The cases of ${g.save.name}", 204f, 20f, 18f, C.INK, Font.DISPLAY, Align.CENTER)
        drawButtons(p)
        val from = page * perPage
        for (i in from until Math.min(g.cases.size, from + perPage)) {
            val c = g.cases[i]
            val b = button("case:${c.id}") ?: continue
            val open = b.enabled
            var done = 0
            for (u in c.units) if (g.finished(u.id)) done++
            val ink = C.alpha(C.INK, if (open) 1f else 0.45f)
            Text.line(p, "${i + 1}", b.x + 16f, b.y + 15f, 16f, ink, Font.DISPLAY, Align.CENTER)
            var size = 13f
            while (size > 9f && p.textWidth(c.title, size, Font.BOLD) > 190f) size -= 0.5f
            Text.line(p, c.title, b.x + 32f, b.y + 15f, size, ink, Font.BOLD)
            if (!open) Icons.draw(p, "lock", b.x + b.w - 26f, b.y + 7f, 16f, C.GREY)
            else if (done >= c.units.size) Draw.stamp(p, b.x + b.w - 20f, b.y + 15f, 11f, "OK", C.RED)
            else Text.line(p, "$done of ${c.units.size}", b.x + b.w - 10f, b.y + 15f, 10f, C.INK2, Font.TEXT, Align.RIGHT)
        }
    }

    override fun back(): Boolean { g.go(HubScreen(g)); return true }
    override fun describe() = "Cases page=$page"
}

/** Shown when a stage is finished. A natural place to stop for the day. */
class StageDoneScreen(g: Game, private val stage: Int) : Screen(g) {
    override fun enter() {
        buttons.clear()
        g.audio.sfx("win")
        val last = stage >= g.unit.stages.size - 1
        if (!last) {
            val b = add(Button("go", 206f, 190f, 150f, 34f, "Keep going", Style.PRIMARY) { Flow.start(g, stage + 1) })
            b.font = Font.DISPLAY
            b.size = 15f
            add(Button("stop", 58f, 190f, 136f, 34f, "Stop for now", Style.PLAIN) { g.go(HubScreen(g)) })
        } else {
            val nx = g.nextUnit()
            if (nx != null) {
                val b = add(Button("go", 206f, 190f, 150f, 34f, "Next unit", Style.PRIMARY) { Flow.openUnit(g, nx.id) })
                b.font = Font.DISPLAY
                b.size = 15f
                add(Button("stop", 58f, 190f, 136f, 34f, "Stop for now", Style.PLAIN) { g.go(HubScreen(g)) })
            } else {
                add(Button("stop", 118f, 190f, 164f, 34f, "Back to the case file", Style.PRIMARY) { g.go(HubScreen(g)) }).size = 12f
            }
        }
    }

    override fun draw(p: Painter) {
        Draw.notebookPage(p)
        val s = g.unit.stages[stage]
        val last = stage >= g.unit.stages.size - 1
        val grow = Math.min(1f, t * 4f)
        Draw.stamp(p, 212f, 62f, 36f * (1.6f - 0.6f * grow), "OK", C.RED)
        val head = if (last) "Unit ${g.unit.number} complete!" else "${s.title}: done!"
        Text.line(p, head, 212f, 118f, 22f, C.INK, Font.DISPLAY, Align.CENTER)
        var body = g.fill(s.done)
        if (last && g.nextUnit() == null) body += " There are no more units yet."
        Text.block(p, body, 50f, 136f, 324f, 12f, C.INK2, Font.TEXT, Align.CENTER)
        drawButtons(p)
    }

    override fun back(): Boolean { g.go(HubScreen(g)); return true }
    override fun describe() = "StageDone $stage"
}
