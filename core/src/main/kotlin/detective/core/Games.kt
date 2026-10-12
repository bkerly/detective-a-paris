package detective.core

/**
 * Small games of their own, for action scenes that are not a walk: the protest
 * that blocks the cheese truck (unit 9), pétanque (unit 11), the telescope on
 * the Eiffel Tower (unit 12) and the cheese belt (unit 15). A unit file lists
 * them under "games", each with a "kind"; a stage step {"game": "<id>"} plays
 * one. As everywhere in the game there is no clock and nothing can be lost.
 */
object Games {
    fun open(g: Game, id: String, onExit: () -> Unit, onDone: () -> Unit): Screen {
        val d = g.unit.games[id] ?: throw IllegalArgumentException("unit ${g.unit.id} has no game \"$id\"")
        return when (d.kind) {
            "block" -> BlockScreen(g, id, onExit, onDone)
            "boules" -> BoulesScreen(g, id, onExit, onDone)
            "spy" -> SpyScreen(g, id, onExit, onDone)
            "belt" -> BeltScreen(g, id, onExit, onDone)
            else -> throw IllegalArgumentException("unit ${g.unit.id}: game \"$id\" has an unknown kind \"${d.kind}\"")
        }
    }

    val KINDS = setOf("block", "boules", "spy", "belt")
}

/** What every small game shares: the how-to card, a speech panel, a hint at the top, a toast, a way out. */
abstract class MiniGame(g: Game, gameId: String, protected val onExit: () -> Unit, protected val onDone: () -> Unit) : Screen(g) {
    protected val d: GameData = g.unit.games[gameId] ?: throw IllegalArgumentException("unit ${g.unit.id} has no game \"$gameId\"")
    protected val j: J get() = d.j
    protected var tip = false
    protected var hint = ""
    private var dialogOpen = false
    private var dLine: Line? = null
    private var dNarr: String? = null
    private var afterDialog: (() -> Unit)? = null
    private var showEnglish = false
    private var toast: String? = null
    private var toastT = 0f
    protected var finished = false
    private var begun = false

    protected fun line(key: String): Line? = Line.from(j[key], d.who, null)
    protected fun lineOf(o: J): Line? = Line.from(o, d.who, null)

    override fun enter() {
        if (begun) { rebuild(); return }
        begun = true
        tip = j.str("tip").isNotEmpty()
        hint = j.str("hint")
        setup()
        rebuild()
        if (!tip) start()
    }

    /** Called once, before the how-to card. */
    abstract fun setup()
    /** Called when the how-to card goes away. */
    open fun start() {}
    abstract fun play(dt: Float)
    abstract fun drawGame(p: Painter)
    /** Add the game's own buttons. */
    abstract fun playButtons()
    /** Buttons the game draws itself (the base class leaves them alone). */
    open fun drawsOwn(b: Button): Boolean = false
    open fun touchDown(x: Float, y: Float) {}
    open fun touchMove(x: Float, y: Float) {}
    open fun touchUp(x: Float, y: Float) {}

    protected fun paused() = tip || dialogOpen || finished
    fun dialogShowing() = dialogOpen
    fun tipShowing() = tip

    protected fun rebuild() {
        buttons.clear()
        if (tip) return
        if (dialogOpen) {
            val ln = dLine
            if (ln != null) add(Button("again", 16f, 206f, 30f, 24f, "", Style.DARK) { g.say(ln) }).icon = "speaker"
            val ok = add(Button("ok", 312f, 202f, 72f, 30f, "OK", Style.PRIMARY) { closeDialog() })
            ok.font = Font.DISPLAY
            ok.size = 15f
            return
        }
        add(Button("exit", 4f, 4f, 30f, 24f, "", Style.DARK) { g.audio.stopVoice(); onExit() }).icon = "back"
        add(Button("book", 366f, 4f, 30f, 24f, "", Style.DARK) { g.openOverlay(NotebookScreen(g)) }).icon = "book"
        playButtons()
    }

    protected fun open(ln: Line?, narr: String?, then: (() -> Unit)?) {
        if (ln == null && narr == null) { if (then != null) then(); return }
        dLine = ln
        dNarr = narr
        afterDialog = then
        dialogOpen = true
        showEnglish = false
        if (ln != null) g.say(ln)
        rebuild()
    }

    private fun closeDialog() {
        dialogOpen = false
        val then = afterDialog
        afterDialog = null
        rebuild()
        if (then != null) then()
    }

    protected fun showToast(s: String) { toast = s; toastT = 3.5f }

    protected fun win(ln: Line?, narr: String?) {
        if (finished) return
        // words picked up along the way go into the notebook
        for (id in j.strings("found")) g.save.find(id)
        open(ln, narr) {
            finished = true
            g.audio.sfx("win")
            onDone()
        }
    }

    override fun update(dt: Float) {
        super.update(dt)
        if (toastT > 0f) toastT -= dt
        if (!paused()) play(dt)
    }

    override fun tapDown(x: Float, y: Float) {
        if (tip) { tip = false; rebuild(); start(); return }
        if (dialogOpen) { if (y > 150f) showEnglish = !showEnglish; return }
        if (!paused()) touchDown(x, y)
    }

    override fun tapMove(x: Float, y: Float) { if (!paused()) touchMove(x, y) }
    override fun tapUp(x: Float, y: Float) { if (!paused()) touchUp(x, y) }

    override fun draw(p: Painter) {
        drawGame(p)
        if (hint.isNotEmpty() && !dialogOpen && !tip) {
            val w = Math.min(320f, p.textWidth(hint, 9.5f, Font.BOLD) + 16f)
            val lines = Text.wrap(p, hint, 9.5f, Font.BOLD, 304f)
            val h = if (lines.size > 1) 26f else 16f
            p.fillRoundRect(200f - w / 2f, 5f, w, h, 6f, C.alpha(C.INK, 0.88f))
            var ly = 5f + h / 2f - (lines.size - 1) * 5.6f
            for (s in lines) { Text.line(p, s, 200f, ly, 9.5f, C.WHITE, Font.BOLD, Align.CENTER); ly += 11.2f }
        }
        val tst = toast
        if (tst != null && toastT > 0f && !dialogOpen && !tip) {
            val a2 = Math.min(1f, toastT * 2f)
            val w = Math.min(360f, p.textWidth(tst, 10f, Font.BOLD) + 24f)
            p.fillRoundRect(200f - w / 2f, 212f, w, 20f, 10f, C.alpha(C.INK, 0.92f * a2))
            Text.line(p, tst, 200f, 222f, 10f, C.alpha(C.WHITE, a2), Font.BOLD, Align.CENTER)
        }
        if (dialogOpen) {
            Draw.card(p, 8f, 150f, 384f, 86f, C.WHITE)
            p.strokeRoundRect(8f, 150f, 384f, 86f, 8f, 1f, C.INK)
            var y = 158f
            val ln = dLine
            if (ln != null) {
                y = Text.block(p, g.fill(ln.fr), 20f, y, 360f, 13.5f, C.INK, Font.BOLD, Align.LEFT, 1.22f) + 1f
                val under = g.unit.english == "under" || showEnglish
                val en = if (ln.en.isNotEmpty()) ln.en else ln.gloss.joinToString(", ")
                if (en.isNotEmpty()) {
                    y = if (under) Text.block(p, en, 20f, y, 360f, 10.5f, C.GREY, Font.TEXT, Align.LEFT, 1.2f) + 3f
                    else Text.block(p, "tap here for help", 20f, y, 360f, 9f, C.GREY_L, Font.TEXT, Align.LEFT, 1.2f) + 3f
                }
            }
            val nr = dNarr
            if (nr != null) Text.block(p, g.fill(nr), if (ln != null) 54f else 20f, Math.max(y, if (ln != null) 208f else 170f), 250f, 10.5f, C.INK2, Font.TEXT, Align.LEFT, 1.2f)
        }
        for (b in buttons) if (!drawsOwn(b)) b.draw(p)
        if (tip) {
            p.fillRect(0f, 0f, Game.W, Game.H, C.alpha(C.NIGHT, 0.6f))
            Draw.card(p, 70f, 62f, 260f, 116f, C.PAPER)
            val bob = Math.sin(t * 4.0).toFloat() * 3f
            Icons.draw(p, "hand", 84f, 80f + bob, 46f)
            Text.block(p, g.fill(j.str("tip")), 140f, 76f, 178f, 11.5f, C.INK, Font.BOLD, Align.LEFT, 1.3f)
            Text.line(p, "Tap to start", 200f, 166f, 10f, C.GREY, Font.TEXT, Align.CENTER)
        }
    }

    override fun back(): Boolean {
        if (dialogOpen) { closeDialog(); return true }
        if (tip) { tip = false; rebuild(); start(); return true }
        g.audio.stopVoice()
        onExit()
        return true
    }
}

// ======================================================================
// Unit 9: the protest. She fills streets with people holding signs, and
// leaves the cheese truck only one way to go: into the alley.
// ======================================================================

class BlockScreen(g: Game, id: String, onExit: () -> Unit, onDone: () -> Unit) : MiniGame(g, id, onExit, onDone) {
    private class Node(val id: String, val x: Float, val y: Float)
    private class Street(val id: String, val name: String, val a: Int, val b: Int, val alley: Boolean)

    private val nodes = ArrayList<Node>()
    private val streets = ArrayList<Street>()
    private val blocked = HashSet<Int>()
    private var groups = 2
    private val chants = ArrayList<Line>()
    private var chantI = 0

    // the truck: on street [te], heading for its b end if [tFwd], [ts] pixels along
    private var te = 0
    private var tFwd = true
    private var ts = 0f
    private var speed = 44f
    private var stuck = false
    private var grumbleT = 0f
    var turns = 0; private set

    override fun setup() {
        val nj = j["nodes"]
        val index = HashMap<String, Int>()
        for (k in nj.keys()) {
            val l = nj[k].list()
            index[k] = nodes.size
            nodes.add(Node(k, l[0].asFloat(), l[1].asFloat()))
        }
        for (s in j.list("streets")) {
            val a = index[s.str("a")] ?: throw IllegalArgumentException("street ${s.str("id")}: no node ${s.str("a")}")
            val b = index[s.str("b")] ?: throw IllegalArgumentException("street ${s.str("id")}: no node ${s.str("b")}")
            streets.add(Street(s.str("id"), Text.french(s.str("name")), a, b, s.bool("alley")))
        }
        groups = j.int("groups", 2)
        speed = j.num("speed", 44f)
        for (c in j.list("chants")) lineOf(c)?.let { chants.add(it) }
        val start = j.str("start")
        for (i in streets.indices) if (streets[i].id == start) te = i
    }

    private fun len(s: Street): Float {
        val a = nodes[s.a]; val b = nodes[s.b]
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y)
    }

    private fun truckXY(): FloatArray {
        val s = streets[te]
        val a = nodes[if (tFwd) s.a else s.b]
        val b = nodes[if (tFwd) s.b else s.a]
        val f = (ts / len(s)).coerceIn(0f, 1f)
        return floatArrayOf(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f, b.x - a.x, b.y - a.y)
    }

    override fun playButtons() {
        for (i in streets.indices) {
            val s = streets[i]
            val a = nodes[s.a]; val b = nodes[s.b]
            val hor = Math.abs(a.y - b.y) < 1f
            val btn = if (hor) Button("street:${s.id}", Math.min(a.x, b.x) + 8f, a.y - 9f, Math.abs(a.x - b.x) - 16f, 18f, "", Style.PLAIN) { tapStreet(i) }
            else Button("street:${s.id}", a.x - 9f, Math.min(a.y, b.y) + 8f, 18f, Math.abs(a.y - b.y) - 16f, "", Style.PLAIN) { tapStreet(i) }
            add(btn)
        }
    }

    override fun drawsOwn(b: Button) = b.id.startsWith("street:")

    private fun tapStreet(i: Int) {
        if (stuck) return
        val s = streets[i]
        if (blocked.contains(i)) { blocked.remove(i); showToast("The people go home from ${s.name}."); return }
        if (s.alley) { showToast("Leave the alley open: that is where the truck must go."); return }
        if (i == te) { showToast("The truck is on that street. Pick another one."); return }
        if (blocked.size >= groups) { showToast("All your signs are out. Tap a street to send its people home."); return }
        blocked.add(i)
        g.audio.sfx("ok")
        if (chants.isNotEmpty()) { val ln = chants[chantI % chants.size]; chantI++; g.say(ln); showToast(ln.fr) }
    }

    override fun play(dt: Float) {
        if (stuck) return
        if (grumbleT > 0f) grumbleT -= dt
        val s = streets[te]
        ts += speed * dt
        if (ts < len(s)) return
        // at a corner: the truck turns into an open street, never the way it came if it can help it
        val at = if (tFwd) s.b else s.a
        if (s.alley && at == deadEnd(s)) {
            stuck = true
            win(line("stuck"), j.strOrNull("doneNarr"))
            return
        }
        val opts = ArrayList<Int>()
        var alley = -1
        var wasBlocked = false
        for (i in streets.indices) {
            if (i == te) continue
            val o = streets[i]
            if (o.a != at && o.b != at) continue
            if (blocked.contains(i)) { wasBlocked = true; continue }
            if (o.alley) { alley = i; continue }
            opts.add(i)
        }
        val next = when {
            opts.isNotEmpty() -> opts[g.rng.next(opts.size)]
            alley >= 0 -> alley
            else -> te
        }
        if (wasBlocked && grumbleT <= 0f) {
            grumbleT = 7f
            line("blocked")?.let { g.say(it) }
        }
        turns++
        tFwd = streets[next].a == at
        te = next
        ts = 0f
    }

    /** The end of the alley that is not a corner. */
    private fun deadEnd(s: Street): Int {
        var na = 0
        for (o in streets) if (o.a == s.a || o.b == s.a) na++
        return if (na <= 1) s.a else s.b
    }

    override fun drawGame(p: Painter) {
        p.fillRect(0f, 0f, Game.W, Game.H, 0xFFD9CDB0.toInt())
        // the roofs between the streets
        for (yy in 0 until 12) for (xx in 0 until 20) {
            p.fillRect(xx * 20f + 2f, yy * 20f + 2f, 16f, 16f, if ((xx + yy) % 3 == 0) 0xFFCDBF9F.toInt() else 0xFFD3C6A8.toInt())
        }
        for (s in streets) {
            val a = nodes[s.a]; val b = nodes[s.b]
            val w = if (s.alley) 10f else 16f
            p.line(a.x, a.y, b.x, b.y, w + 4f, 0xFFB9B4A8.toInt())
            p.line(a.x, a.y, b.x, b.y, w, if (s.alley) 0xFF9AA0AE.toInt() else 0xFF6A7080.toInt())
        }
        for (n in nodes) p.fillOval(n.x - 10f, n.y - 10f, 20f, 20f, 0xFF6A7080.toInt())
        // street names, once per name, on the longest stretch
        val shown = HashSet<String>()
        for (s in streets.sortedByDescending { len(it) }) {
            if (!shown.add(s.name)) continue
            val a = nodes[s.a]; val b = nodes[s.b]
            val mx = (a.x + b.x) / 2f
            val my = (a.y + b.y) / 2f
            val w = p.textWidth(s.name, 7.5f, Font.BOLD) + 8f
            val hor = Math.abs(a.y - b.y) < 1f
            val ny = if (hor) my - 15f else my
            val nx = mx.coerceIn(w / 2f + 2f, Game.W - w / 2f - 2f)
            p.fillRoundRect(nx - w / 2f, ny - 6f, w, 12f, 3f, if (s.alley) C.MUSTARD else C.WHITE)
            Text.line(p, s.name, nx, ny, 7.5f, C.INK, Font.BOLD, Align.CENTER)
        }
        // the hotel
        j.list("marks").forEach { m ->
            val l = m.list("at")
            if (l.size >= 2) {
                val mx = l[0].asFloat(); val my = l[1].asFloat()
                Icons.draw(p, m.str("icon", "hotel"), mx - 14f, my - 14f, 28f)
                Text.line(p, m.str("name"), mx, my + 19f, 7.5f, C.INK, Font.BOLD, Align.CENTER)
            }
        }
        // crowds with signs
        for (i in blocked) {
            val s = streets[i]
            val a = nodes[s.a]; val b = nodes[s.b]
            for (k in 0 until 5) {
                val f = 0.2f + k * 0.15f
                val px = a.x + (b.x - a.x) * f
                val py = a.y + (b.y - a.y) * f
                val bob = Math.sin((t * 6f + k).toDouble()).toFloat() * 1.5f
                p.fillOval(px - 4f, py - 2f + bob, 8f, 8f, intArrayOf(C.RED, 0xFF3F6DB5.toInt(), C.GREEN, C.MUSTARD, 0xFF7C3F8C.toInt())[k])
                p.fillOval(px - 2.5f, py - 6f + bob, 5f, 5f, 0xFFF2C9A0.toInt())
                p.fillRect(px - 0.5f, py - 15f + bob, 1f, 9f, C.BROWN)
                p.fillRect(px - 6f, py - 21f + bob, 12f, 7f, C.WHITE)
                p.fillRect(px - 4f, py - 18.5f + bob, 8f, 1.2f, C.RED)
            }
        }
        // the truck, white with a cheese on the side
        val tp = truckXY()
        val hor = Math.abs(tp[2]) > Math.abs(tp[3])
        val tw = if (hor) 30f else 16f
        val th = if (hor) 16f else 30f
        p.fillRoundRect(tp[0] - tw / 2f + 1f, tp[1] - th / 2f + 2f, tw, th, 3f, C.alpha(C.BLACK, 0.3f))
        p.fillRoundRect(tp[0] - tw / 2f, tp[1] - th / 2f, tw, th, 3f, C.WHITE)
        p.strokeRoundRect(tp[0] - tw / 2f, tp[1] - th / 2f, tw, th, 3f, 1f, C.INK)
        val cab = if (hor) (if (tp[2] > 0) floatArrayOf(tp[0] + tw / 2f - 8f, tp[1] - th / 2f, 8f, th) else floatArrayOf(tp[0] - tw / 2f, tp[1] - th / 2f, 8f, th))
                  else (if (tp[3] > 0) floatArrayOf(tp[0] - tw / 2f, tp[1] + th / 2f - 8f, tw, 8f) else floatArrayOf(tp[0] - tw / 2f, tp[1] - th / 2f, tw, 8f))
        p.fillRoundRect(cab[0], cab[1], cab[2], cab[3], 2f, 0xFF9FD3F2.toInt())
        Icons.draw(p, "cheese", tp[0] - 6f, tp[1] - 6f, 12f)
        // how many signs are left
        p.fillRoundRect(298f, 214f, 98f, 22f, 6f, C.alpha(C.INK, 0.88f))
        Text.line(p, "Signs left: ${groups - blocked.size}", 347f, 225f, 10f, C.WHITE, Font.BOLD, Align.CENTER)
    }

    override fun describe() = "Block street=${streets[te].id} blocked=${blocked.size} turns=$turns stuck=$stuck tip=$tip"

    // ---------- for the scripted playthrough ----------
    /** The streets to fill: the corner of the alley, except the alley and the truck's own street. */
    fun rightTaps(): List<String> {
        val out = ArrayList<String>()
        for (s in streets) if (s.alley) {
            val corner = if (deadEnd(s) == s.a) s.b else s.a
            for (i in streets.indices) {
                val o = streets[i]
                if (o.alley || i == te || blocked.contains(i)) continue
                if (o.a == corner || o.b == corner) out.add("street:${o.id}")
            }
        }
        return out.take(Math.max(0, groups - blocked.size))
    }
    fun isStuck() = stuck
    fun blockedCount() = blocked.size
    fun groupCount() = groups
}

// ======================================================================
// Unit 11: pétanque. Each side throws two balls; the balls nearest the
// little wooden one (le cochonnet) score. Nobody loses: if the dancers
// get there first, everyone plays again.
// ======================================================================

class BoulesScreen(g: Game, id: String, onExit: () -> Unit, onDone: () -> Unit) : MiniGame(g, id, onExit, onDone) {
    private class Ball(var x: Float, var y: Float, val ours: Boolean, val jack: Boolean) {
        var vx = 0f; var vy = 0f
        val r: Float get() = if (jack) 3.5f else 7f
    }

    private val balls = ArrayList<Ball>()
    private var jack = Ball(200f, 80f, false, true)
    private var turn = 0                 // 0..3: them, us, them, us
    private var rolling = false
    private var aiming = false
    private var aimX = 0f
    private var aimY = 0f
    private var us = 0
    private var them = 0
    private var target = 3
    private var waitT = 0f
    var throwsMade = 0; private set
    var rounds = 0; private set
    private val circleX = 200f
    private val circleY = 206f
    private val pitchTop = 40f
    private val pitchBottom = 188f

    override fun setup() {
        target = j.int("to", 3)
        newRound()
    }

    private fun newRound() {
        balls.clear()
        jack = Ball(130f + g.rng.next(140), 64f + g.rng.next(50), false, true)
        balls.add(jack)
        turn = 0
        rounds++
        waitT = 1.2f
    }

    override fun playButtons() {}

    private fun theirTurn() = turn % 2 == 0
    fun herTurn() = !rolling && turn < 4 && !theirTurn() && waitT <= 0f

    private fun throwTo(tx: Float, ty: Float, ours: Boolean, err: Float) {
        val ang = g.rng.next(360) * Math.PI / 180.0
        val e = err * g.rng.next(100) / 100f
        val gx = (tx + Math.cos(ang).toFloat() * e).coerceIn(30f, 370f)
        val gy = (ty + Math.sin(ang).toFloat() * e).coerceIn(pitchTop + 6f, pitchBottom - 6f)
        val b = Ball(circleX, circleY - 10f, ours, false)
        val dx = gx - b.x
        val dy = gy - b.y
        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        // friction slows a ball by FR pixels a second, every second, so this speed stops it on the spot
        val v = Math.sqrt((2.0 * FR * dist)).toFloat()
        b.vx = dx / dist * v
        b.vy = dy / dist * v
        balls.add(b)
        rolling = true
        throwsMade++
        g.audio.sfx("tap")
    }

    override fun touchDown(x: Float, y: Float) {
        if (!herTurn()) return
        aiming = true
        aimX = x; aimY = y
    }

    override fun touchMove(x: Float, y: Float) { if (aiming) { aimX = x; aimY = y } }

    override fun touchUp(x: Float, y: Float) {
        if (!aiming) return
        aiming = false
        if (!herTurn() || y < pitchTop || y > pitchBottom) return
        // the further she throws, the more it can wander, but only a little
        val dist = Math.abs(circleY - y) + Math.abs(circleX - x)
        throwTo(x, y, true, 3f + dist * 0.035f)
    }

    override fun play(dt: Float) {
        if (waitT > 0f) {
            waitT -= dt
            if (waitT <= 0f && theirTurn() && turn < 4 && !rolling) {
                // a dancer throws: near, but not as near as a careful detective
                throwTo(jack.x, jack.y, false, 14f + g.rng.next(26))
            }
            return
        }
        if (!rolling) return
        var any = false
        for (b in balls) {
            val v = Math.sqrt((b.vx * b.vx + b.vy * b.vy).toDouble()).toFloat()
            if (v < 0.5f) { b.vx = 0f; b.vy = 0f; continue }
            any = true
            val nv = Math.max(0f, v - FR * dt)
            b.vx = b.vx / v * nv
            b.vy = b.vy / v * nv
            b.x += b.vx * dt
            b.y += b.vy * dt
            if (b.x < 24f || b.x > 376f) { b.vx = -b.vx; b.x = b.x.coerceIn(24f, 376f) }
            if (b.y < pitchTop + 4f) { b.vy = -b.vy; b.y = pitchTop + 4f }
        }
        // a rolling ball knocks a still one along
        for (i in balls.indices) for (k in balls.indices) {
            if (i == k) continue
            val a = balls[i]; val c = balls[k]
            val dx = c.x - a.x; val dy = c.y - a.y
            val dd = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            val minD = a.r + c.r
            if (dd < minD && dd > 0.01f) {
                val nx = dx / dd; val ny = dy / dd
                val rel = (a.vx - c.vx) * nx + (a.vy - c.vy) * ny
                if (rel > 0f) {
                    val k2 = if (c.jack) 1.4f else 1f
                    a.vx -= rel * nx; a.vy -= rel * ny
                    c.vx += rel * nx * k2; c.vy += rel * ny * k2
                }
                val push = (minD - dd) / 2f
                a.x -= nx * push; a.y -= ny * push
                c.x += nx * push; c.y += ny * push
            }
        }
        if (!any) {
            rolling = false
            val wasHers = !theirTurn()
            turn++
            if (turn >= 4) { waitT = 0f; afterRound() }
            else {
                if (wasHers && oursNearest() == true) showToast("Plus près ! Your ball is the nearest.")
                waitT = if (theirTurn()) 1.0f else 0.3f
            }
        }
    }

    private fun dist(b: Ball): Float { val dx = b.x - jack.x; val dy = b.y - jack.y; return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat() }

    /** Who is nearest now: true for her side. */
    private fun oursNearest(): Boolean? {
        var best: Ball? = null
        for (b in balls) if (!b.jack && (best == null || dist(b) < dist(best))) best = b
        return best?.ours
    }

    private fun afterRound() {
        // the round is over: count the balls nearer than the other side's best
        var bestUs = 1e9f; var bestThem = 1e9f
        for (b in balls) if (!b.jack) { if (b.ours) bestUs = Math.min(bestUs, dist(b)) else bestThem = Math.min(bestThem, dist(b)) }
        var pts = 0
        val ours = bestUs < bestThem
        for (b in balls) if (!b.jack && b.ours == ours && dist(b) < (if (ours) bestThem else bestUs)) pts++
        if (ours) us += pts else them += pts
        val say = if (ours) line("pointUs") else line("pointThem")
        when {
            us >= target -> win(line("win"), j.strOrNull("doneNarr"))
            them >= target -> {
                us = 0; them = 0
                open(line("again"), "The dancers got there first. Nobody loses here: you start again.") { newRound() }
            }
            else -> open(say, scoreText()) { newRound() }
        }
    }

    private fun scoreText() = "Nous : ${num(us)}. Eux : ${num(them)}."
    private fun num(n: Int) = arrayOf("zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf", "dix", "onze", "douze", "treize")[Math.min(n, 13)]

    override fun drawGame(p: Painter) {
        // trees and grass round a gravel pitch
        p.fillRect(0f, 0f, Game.W, Game.H, 0xFF8CC97A.toInt())
        for (k in 0 until 9) p.fillOval(k * 48f - 10f, -18f, 60f, 44f, 0xFF5E9E52.toInt())
        p.fillRoundRect(18f, pitchTop, 364f, pitchBottom - pitchTop + 30f, 6f, 0xFFE3D3AE.toInt())
        var seed = 7
        for (k in 0 until 220) {
            seed = seed * 1103515245 + 12345
            val gx = 22f + ((seed ushr 8) and 0xFFFF) % 356
            seed = seed * 1103515245 + 12345
            val gy = pitchTop + 4f + ((seed ushr 8) and 0xFFFF) % 170
            p.fillRect(gx, gy, 1.5f, 1.5f, 0xFFC9B68C.toInt())
        }
        // the throwing circle
        p.strokeRoundRect(circleX - 14f, circleY - 10f, 28f, 20f, 10f, 1.5f, C.WHITE)
        // the aim line
        if (aiming && herTurn()) {
            val n = 12
            for (k in 0 until n) {
                val f = k / n.toFloat()
                p.fillOval(circleX + (aimX - circleX) * f - 1.5f, circleY - 10f + (aimY - circleY + 10f) * f - 1.5f, 3f, 3f, C.alpha(C.INK, 0.6f))
            }
            p.strokeRoundRect(aimX - 6f, aimY - 6f, 12f, 12f, 6f, 1.2f, C.INK)
        }
        for (b in balls) {
            p.fillOval(b.x - b.r + 1f, b.y - b.r + 2f, b.r * 2f, b.r * 2f, C.alpha(C.BLACK, 0.25f))
            val col = if (b.jack) 0xFFC98A14.toInt() else if (b.ours) 0xFF9AA5C0.toInt() else 0xFFB98A55.toInt()
            p.fillOval(b.x - b.r, b.y - b.r, b.r * 2f, b.r * 2f, col)
            if (!b.jack) {
                p.fillOval(b.x - b.r * 0.5f, b.y - b.r * 0.6f, b.r * 0.6f, b.r * 0.5f, C.alpha(C.WHITE, 0.6f))
                p.strokeRoundRect(b.x - b.r, b.y - b.r, b.r * 2f, b.r * 2f, b.r, 0.8f, C.INK)
            }
        }
        // who throws, and the score
        p.fillRoundRect(8f, 212f, 128f, 24f, 6f, C.alpha(C.INK, 0.88f))
        Text.line(p, "Nous ${us}  –  ${them} Eux", 72f, 224f, 11f, C.WHITE, Font.BOLD, Align.CENTER)
        val who = when {
            rolling -> ""
            turn >= 4 -> ""
            theirTurn() -> "The dancers throw…"
            else -> "À vous ! Touch where the ball should stop."
        }
        if (who.isNotEmpty()) {
            val w = p.textWidth(who, 10f, Font.BOLD) + 16f
            p.fillRoundRect(392f - w, 212f, w, 24f, 6f, if (theirTurn()) C.alpha(C.INK, 0.7f) else C.MUSTARD)
            Text.line(p, who, 392f - w / 2f, 224f, 10f, if (theirTurn()) C.WHITE else C.INK, Font.BOLD, Align.CENTER)
        }
        // her two balls and their two, still to throw
        var left = 0
        for (k in turn until 4) if (k % 2 == 1) left++
        for (k in 0 until left) p.fillOval(150f + k * 16f, 216f, 12f, 12f, 0xFF9AA5C0.toInt())
    }

    override fun describe() = "Boules turn=$turn us=$us them=$them rounds=$rounds rolling=$rolling tip=$tip"

    // ---------- for the scripted playthrough ----------
    fun jackAt(): FloatArray = floatArrayOf(jack.x, jack.y + 4f)

    private companion object { const val FR = 120f }
}

// ======================================================================
// Unit 12: the telescope at the top of the Eiffel Tower. She pans over
// Paris and taps the vehicle she was told about.
// ======================================================================

class SpyScreen(g: Game, id: String, onExit: () -> Unit, onDone: () -> Unit) : MiniGame(g, id, onExit, onDone) {
    private class Target(val icon: String, val x: Float, val y: Float, val right: Boolean, val say: Line?, val mice: Boolean)

    private val targets = ArrayList<Target>()
    private val worldW = 1000f
    private val worldH = 340f
    var camX = 300f; private set
    var camY = 50f; private set
    private var dragX = -1f
    private var dragY = -1f
    private var dragged = 0f
    private val wcx = 200f
    private val wcy = 122f
    private val wr = 92f
    var wrongs = 0; private set
    private var foundIt = false

    override fun setup() {
        for (o in j.list("targets")) {
            targets.add(Target(o.str("icon"), o.num("x"), o.num("y"), o.bool("right"), lineOf(o["say"]), o.bool("mice")))
        }
        camX = j.num("camX", 300f)
        camY = j.num("camY", 60f)
    }

    override fun playButtons() {}

    override fun touchDown(x: Float, y: Float) { dragX = x; dragY = y; dragged = 0f }

    override fun touchMove(x: Float, y: Float) {
        if (dragX < 0f) return
        camX = (camX - (x - dragX)).coerceIn(0f, worldW - Game.W)
        camY = (camY - (y - dragY)).coerceIn(0f, worldH - Game.H)
        dragged += Math.abs(x - dragX) + Math.abs(y - dragY)
        dragX = x; dragY = y
    }

    override fun touchUp(x: Float, y: Float) {
        val wasDrag = dragged > 8f
        dragX = -1f
        if (wasDrag || foundIt) return
        val dx0 = x - wcx; val dy0 = y - wcy
        if (dx0 * dx0 + dy0 * dy0 > wr * wr) return
        var best: Target? = null
        var bestD = 18f
        for (tg in targets) {
            val d = Math.abs(tg.x - camX - x) + Math.abs(tg.y - camY - y)
            if (d < bestD) { bestD = d; best = tg }
        }
        val tg = best ?: return
        if (tg.right) {
            foundIt = true
            g.audio.sfx("stamp")
            open(tg.say, null) { win(line("done"), j.strOrNull("doneNarr")) }
        } else {
            wrongs++
            g.audio.sfx("no")
            open(tg.say, "Not that one. Keep looking.", null)
        }
    }

    override fun keys(dx: Int, dy: Int) {
        camX = (camX + dx * 12f).coerceIn(0f, worldW - Game.W)
        camY = (camY + dy * 12f).coerceIn(0f, worldH - Game.H)
    }

    override fun play(dt: Float) {}

    override fun drawGame(p: Painter) {
        val ox = -camX
        val oy = -camY
        // the sky and the river, then the city
        p.fillRect(0f, 0f, Game.W, Game.H, 0xFF9FD3F2.toInt())
        p.fillRect(ox, oy + 60f, worldW, 40f, 0xFFBFE2F6.toInt())
        Draw.skylineAt(p, ox, oy + 120f, 0xFF8FA0B8.toInt())
        Draw.skylineAt(p, ox + 400f, oy + 120f, 0xFF8FA0B8.toInt())
        Draw.skylineAt(p, ox + 800f, oy + 120f, 0xFF8FA0B8.toInt())
        p.fillRect(ox, oy + 120f, worldW, worldH - 120f, 0xFFD9CDB0.toInt())
        // the Seine, with its bridges
        p.fillRect(ox, oy + 122f, worldW, 20f, 0xFF5F8FD6.toInt())
        for (k in 0 until 6) p.fillRect(ox + 60f + k * 170f, oy + 120f, 20f, 24f, 0xFFC9C2B2.toInt())
        // blocks of roofs; streets run between them at y 200 and 270 and every 80 across
        var bx = 0f
        while (bx < worldW) {
            var by = 150f
            while (by < worldH) {
                p.fillRect(ox + bx + 6f, oy + by + 4f, 70f, 42f, 0xFFB9B4C4.toInt())
                p.fillRect(ox + bx + 6f, oy + by + 4f, 70f, 8f, 0xFF7C89A8.toInt())
                for (k in 0 until 4) p.fillRect(ox + bx + 12f + k * 16f, oy + by + 18f, 6f, 8f, 0xFF4A5577.toInt())
                by += 70f
            }
            bx += 80f
        }
        for (k in 0 until 2) p.fillRect(ox, oy + 199f + k * 70f, worldW, 16f, 0xFF6A7080.toInt())
        var sx0 = 0f
        while (sx0 < worldW) { p.fillRect(ox + sx0, oy + 146f, 6f, worldH - 146f, 0xFF6A7080.toInt()); sx0 += 80f }
        // the vehicles, and mice by the red truck
        for (tg in targets) {
            Icons.draw(p, tg.icon, ox + tg.x - 12f, oy + tg.y - 12f, 24f)
            if (tg.mice) for (k in 0 until 5) {
                val mx = ox + tg.x + 14f + k * 7f
                val my = oy + tg.y + 6f + Math.sin((t * 8f + k).toDouble()).toFloat() * 1.5f
                p.fillOval(mx - 2.5f, my - 2f, 5f, 4f, 0xFF9AA5C0.toInt())
                p.fillOval(mx + 1.5f, my - 3.5f, 2.5f, 2.5f, 0xFF9AA5C0.toInt())
            }
        }
        // the telescope: dark all round a round window
        val dark = 0xFF0F1326.toInt()
        p.fillRect(0f, 0f, Game.W, wcy - wr, dark)
        p.fillRect(0f, wcy + wr, Game.W, Game.H - wcy - wr, dark)
        p.fillRect(0f, 0f, wcx - wr, Game.H, dark)
        p.fillRect(wcx + wr, 0f, Game.W - wcx - wr, Game.H, dark)
        p.strokeRoundRect(wcx - wr - 30f, wcy - wr - 30f, (wr + 30f) * 2f, (wr + 30f) * 2f, wr + 30f, 62f, dark)
        p.strokeRoundRect(wcx - wr, wcy - wr, wr * 2f, wr * 2f, wr, 3f, 0xFF4A5577.toInt())
        // cross hairs
        p.fillRect(wcx - 8f, wcy - 0.5f, 16f, 1f, C.alpha(C.INK, 0.5f))
        p.fillRect(wcx - 0.5f, wcy - 8f, 1f, 16f, C.alpha(C.INK, 0.5f))
        Text.line(p, "Drag to look around. Tap what you see.", 200f, 228f, 9.5f, C.alpha(C.WHITE, 0.8f), Font.TEXT, Align.CENTER)
        // a small map of where the window is
        p.fillRoundRect(318f, 182f, 74f, 30f, 3f, C.alpha(C.WHITE, 0.2f))
        p.strokeRoundRect(318f + camX / worldW * 74f, 182f + camY / worldH * 30f, Game.W / worldW * 74f, Game.H / worldH * 30f, 2f, 1f, C.MUSTARD)
    }

    override fun describe() = "Spy cam=(${camX.toInt()},${camY.toInt()}) wrongs=$wrongs found=$foundIt tip=$tip"

    // ---------- for the scripted playthrough ----------
    /** World positions of the targets, the right one first, then a wrong one. */
    fun rightSpot(): FloatArray? { for (tg in targets) if (tg.right) return floatArrayOf(tg.x, tg.y); return null }
    fun wrongSpot(): FloatArray? { for (tg in targets) if (!tg.right) return floatArrayOf(tg.x, tg.y); return null }
    fun windowCentre(): FloatArray = floatArrayOf(wcx, wcy)
}

// ======================================================================
// Unit 15: the cheese belt. Cheeses ride past; she taps the shelf each one
// belongs on, by its label. The belt speeds up while she is right and slows
// after a miss, and a cheese she misses comes round again.
// ======================================================================

class BeltScreen(g: Game, id: String, onExit: () -> Unit, onDone: () -> Unit) : MiniGame(g, id, onExit, onDone) {
    private class Cheese(val name: String, val icon: String, val shelf: Int, val tag: String)

    private val shelves = ArrayList<String>()
    private val shelfSay = ArrayList<String>()
    private val cheeses = ArrayList<Cheese>()
    private var idx = 0
    private var x = -30f
    private var speed = 34f
    private var flyT = -1f
    private var flyShelf = 0
    private val count = IntArray(4)
    var misses = 0; private set
    private var wrongLine: Line? = null

    override fun setup() {
        for (s in j.list("shelves")) { shelves.add(Text.french(s.str("fr"))); shelfSay.add(s.str("say", s.str("fr"))) }
        for (c in j.list("cheeses")) cheeses.add(Cheese(Text.french(c.str("name")), c.str("icon", "cheese"), c.int("shelf"), c.str("tag")))
        speed = j.num("speed", 34f)
        wrongLine = line("wrong")
    }

    override fun playButtons() {
        val n = shelves.size
        val gap = 8f
        val w = (384f - (n - 1) * gap) / n
        for (i in 0 until n) {
            val b = add(Button("shelf:$i", 8f + i * (w + gap), 168f, w, 40f, shelves[i], Style.PLAIN) { pick(i) })
            b.font = Font.DISPLAY
            b.size = if (shelves[i].length > 8) 12f else 16f
        }
    }

    private fun onBelt(): Boolean = flyT < 0f && idx < cheeses.size && x > 40f && x < 390f

    private fun pick(i: Int) {
        if (!onBelt()) return
        g.voice.say("hero", shelfSay[i])
        val c = cheeses[idx]
        if (c.shelf == i) {
            g.audio.sfx("ok")
            flyT = 0f
            flyShelf = i
            speed = Math.min(80f, speed * 1.15f)
        } else {
            g.audio.sfx("no")
            misses++
            speed = Math.max(24f, speed * 0.8f)
            button("shelf:$i")?.shake = 1f
            val wl = wrongLine
            if (wl != null && misses % 3 == 1) showToast(wl.fr)
        }
    }

    override fun play(dt: Float) {
        if (idx >= cheeses.size) return
        if (flyT >= 0f) {
            flyT += dt / 0.6f
            if (flyT >= 1f) {
                count[flyShelf]++
                flyT = -1f
                idx++
                x = -30f
                if (idx >= cheeses.size) win(line("done"), j.strOrNull("doneNarr"))
            }
            return
        }
        x += speed * dt
        if (x > 430f) x = -30f   // round it comes again: no cheese is lost
    }

    private fun drawCheese(p: Painter, c: Cheese, cx: Float, cy: Float, s: Float) {
        Icons.draw(p, c.icon, cx - s / 2f, cy - s / 2f, s)
        if (c.tag.isNotEmpty()) {
            val col = if (c.tag == "AOP") C.RED else 0xFF3F6DB5.toInt()
            p.fillOval(cx + s * 0.18f, cy - s * 0.52f, s * 0.42f, s * 0.42f, C.MUSTARD)
            p.fillOval(cx + s * 0.21f, cy - s * 0.49f, s * 0.36f, s * 0.36f, col)
            Text.line(p, c.tag, cx + s * 0.39f, cy - s * 0.31f, s * 0.13f, C.WHITE, Font.BOLD, Align.CENTER)
        }
    }

    override fun drawGame(p: Painter) {
        p.fillRect(0f, 0f, Game.W, Game.H, 0xFFE9E4D8.toInt())
        // the shelves on the back wall
        p.fillRect(0f, 30f, Game.W, 52f, 0xFFD6D0C2.toInt())
        for (i in shelves.indices) {
            val b = button("shelf:$i") ?: continue
            val cx = b.x + b.w / 2f
            p.fillRect(b.x + 6f, 74f, b.w - 12f, 4f, C.BROWN)
            for (k in 0 until Math.min(count[i], 8)) p.fillOval(cx - 30f + k * 8f, 64f, 10f, 10f, 0xFFF3E3A6.toInt())
            Text.line(p, "${shelves[i]} : ${count[i]}", cx, 44f, 11f, C.INK2, Font.BOLD, Align.CENTER)
        }
        // the belt
        p.fillRect(0f, 118f, Game.W, 26f, 0xFF4A5577.toInt())
        val off = (t * speed) % 16f
        var bx = -16f + off
        while (bx < Game.W) { p.fillRect(bx, 120f, 2f, 22f, 0xFF6A7080.toInt()); bx += 16f }
        p.fillRect(0f, 144f, Game.W, 6f, 0xFF2B3358.toInt())
        if (idx < cheeses.size) {
            val c = cheeses[idx]
            if (flyT >= 0f) {
                val b = button("shelf:$flyShelf")
                val tx = if (b != null) b.x + b.w / 2f else 200f
                val f = flyT
                val cx = x + (tx - x) * f
                val cy = 112f + (62f - 112f) * f - Math.sin(f * Math.PI).toFloat() * 30f
                drawCheese(p, c, cx, cy, 34f - f * 14f)
            } else {
                drawCheese(p, c, x, 110f, 40f)
                val w = p.textWidth(c.name, 10f, Font.BOLD) + 12f
                p.fillRoundRect(x - w / 2f, 84f, w, 16f, 4f, C.WHITE)
                Text.line(p, c.name, x, 92f, 10f, C.INK, Font.BOLD, Align.CENTER)
            }
        }
        Text.line(p, "${Math.min(idx, cheeses.size)} / ${cheeses.size}", 380f, 228f, 10f, C.INK2, Font.BOLD, Align.RIGHT)
        Text.line(p, "Read the label. Tap the shelf it goes on.", 12f, 228f, 9.5f, C.INK2, Font.TEXT)
    }

    override fun describe() = "Belt cheese=$idx/${cheeses.size} x=${x.toInt()} speed=${speed.toInt()} misses=$misses tip=$tip"

    // ---------- for the scripted playthrough ----------
    fun ready(): Boolean = onBelt()
    fun rightTap(): String? = if (idx < cheeses.size) "shelf:${cheeses[idx].shelf}" else null
    fun wrongTap(): String? = if (idx < cheeses.size) "shelf:${(cheeses[idx].shelf + 1) % shelves.size}" else null
}
