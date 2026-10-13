package detective.core

// ======================================================================
// Small games for chapter 3: the metro ride (unit 20) and dressing the
// models backstage (unit 22). They are registered in Games.open.
// ======================================================================

/**
 * Unit 20: the metro. She taps her card on the gate, picks the platform by
 * reading the direction signs, counts the stops and gets off at the right
 * station, changes lines, and goes out by the exit. A wrong platform costs one
 * stop the wrong way; a missed station costs one stop back. Nothing else.
 */
class MetroScreen(g: Game, id: String, onExit: () -> Unit, onDone: () -> Unit) : MiniGame(g, id, onExit, onDone) {
    private class Trip(
        val line: String, val color: Int, val ends: List<String>, val stations: List<String>,
        val says: List<Line?>, val from: Int, val to: Int, val towards: Int, val sign: String
    )

    private companion object {
        const val GATE = 0
        const val PLATFORM = 1
        const val MOVE = 2
        const val STOPPED = 3
        const val BACK = 4
        const val CHANGE = 5
        const val EXIT = 6
    }

    private val trips = ArrayList<Trip>()
    private var trip = 0
    private var phase = GATE
    private var pt = 0f
    /** Where the train is now, as an index into the trip's stations. */
    private var at = 0
    /** Which way the train is going: +1 or -1 along the stations list. */
    private var dir = 1
    private var stopsRidden = 0
    var wrongWays = 0; private set
    var missed = 0; private set
    var early = 0; private set
    private var gateOpen = 0f
    /** Where a trip back across the platform ends: -1 for the platform she started from, else a station. */
    private var backTo = -1

    override fun setup() {
        toastY = 84f
        for (tj in j.list("trips")) {
            val names = ArrayList<String>()
            val says = ArrayList<Line?>()
            for (s in tj.list("stations")) { names.add(Text.french(s.str("fr"))); says.add(Line.from(s, d.who, null)) }
            val from = names.indexOf(Text.french(tj.str("from")))
            val to = names.indexOf(Text.french(tj.str("to")))
            trips.add(Trip(
                tj.str("line"), Art.parseColor(tj.str("color", "f2a0c0")), tj.strings("ends").map { Text.french(it) },
                names, says, Math.max(0, from), Math.max(0, to), if (to > from) 1 else 0, tj.str("sign")
            ))
        }
    }

    private fun cur(): Trip = trips[Math.min(trip, trips.size - 1)]

    private fun set(ph: Int) {
        phase = ph
        pt = 0f
        rebuild()
    }

    override fun playButtons() {
        when (phase) {
            GATE -> {
                val b = add(Button("reader", 236f, 92f, 64f, 56f, "", Style.PLAIN) { tapGate() })
                b.icon = j.str("card", "navigo")
                b.iconSize = 40f
            }
            PLATFORM -> {
                val tr = cur()
                for (k in 0 until 2) {
                    val b = add(Button("dir:$k", if (k == 0) 12f else 204f, 150f, 184f, 44f, "", Style.PLAIN) { pickDir(k) })
                    b.tag = k
                    b.enabled = true
                    if (tr.ends.size < 2) b.enabled = false
                }
            }
            STOPPED -> {
                val b = add(Button("off", 150f, 196f, 100f, 34f, "Descendre", Style.PRIMARY) { getOff() })
                b.font = Font.DISPLAY
                b.size = 15f
            }
            EXIT -> {
                val b = add(Button("sortie", 140f, 150f, 120f, 40f, "Sortie", Style.GOOD) {
                    g.audio.sfx("ok")
                    win(line("done"), j.strOrNull("doneNarr"))
                })
                b.icon = "exit"
                b.font = Font.DISPLAY
                b.size = 16f
            }
        }
    }

    override fun drawsOwn(b: Button) = b.id.startsWith("dir:")

    private fun tapGate() {
        if (phase != GATE || gateOpen > 0f) return
        g.audio.sfx("ok")
        gateOpen = 0.01f
        line("gate")?.let { g.say(it) }
        after(1.6f) { set(PLATFORM); hint = platformHint() }
    }

    private fun platformHint(): String {
        val tr = cur()
        return "Ligne ${tr.line} to ${tr.stations[tr.to]}. Which platform?"
    }

    private fun pickDir(k: Int) {
        if (phase != PLATFORM) return
        val tr = cur()
        at = tr.from
        stopsRidden = 0
        if (k == tr.towards) {
            g.audio.sfx("ok")
            dir = if (tr.to > tr.from) 1 else -1
            hint = "Count the stops. Get off at ${tr.stations[tr.to]}."
            set(MOVE)
        } else {
            // the wrong way: one stop, then back across to the other platform
            g.audio.sfx("no")
            wrongWays++
            dir = if (tr.to > tr.from) -1 else 1
            hint = ""
            set(MOVE)
        }
    }

    private fun getOff() {
        if (phase != STOPPED) return
        val tr = cur()
        if (at == tr.to) arrived()
        else {
            g.audio.sfx("no")
            early++
            val ln = line("early")
            if (ln != null) { g.say(ln); showToast(ln.fr) }
            button("off")?.shake = 1f
        }
    }

    private fun arrived() {
        g.audio.sfx("ok")
        trip++
        if (trip < trips.size) {
            val ln = line("change")
            if (ln != null) g.say(ln)
            hint = ""
            set(CHANGE)
            after(3.0f) { set(PLATFORM); hint = platformHint() }
        } else {
            line("exit")?.let { g.say(it) }
            hint = "Find the way out."
            set(EXIT)
        }
    }

    override fun play(dt: Float) {
        pt += dt
        if (gateOpen > 0f) gateOpen = Math.min(1f, gateOpen + dt * 1.5f)
        when (phase) {
            MOVE -> if (pt > 2.2f) {
                at += dir
                stopsRidden++
                set(STOPPED)
                val tr = cur()
                tr.says.getOrNull(at)?.let { g.say(it) }
                val wrongWay = (tr.to - tr.from) * dir < 0
                if (wrongWay) {
                    // she sees the station is the wrong way, gets off and crosses over
                    after(1.6f) {
                        line("wrongWay")?.let { ln -> g.say(ln); showToast(ln.fr) }
                        backTo = -1
                        set(BACK)
                    }
                } else if ((tr.to - at) * dir < 0) {
                    // too far: off, across, and one stop back
                    missed++
                    after(1.6f) {
                        line("missed")?.let { ln -> g.say(ln); showToast(ln.fr) }
                        backTo = tr.to
                        set(BACK)
                    }
                }
            }
            STOPPED -> {
                val tr = cur()
                if (pt > 4.5f && at != tr.to && (tr.to - at) * dir > 0) set(MOVE)
                else if (pt > 6.5f && at == tr.to) set(MOVE)   // she let her stop go by
            }
            BACK -> if (pt > 2.4f) {
                val tr = cur()
                dir = if (tr.to > tr.from) 1 else -1
                if (backTo < 0) {
                    // came back from the wrong way: on the right platform now
                    at = tr.from
                    set(PLATFORM)
                    hint = platformHint()
                } else {
                    at = backTo
                    set(STOPPED)
                    tr.says.getOrNull(at)?.let { g.say(it) }
                }
            }
        }
    }

    // ---------- drawing ----------

    private val navy = 0xFF1D2B53.toInt()

    private fun roundel(p: Painter, cx: Float, cy: Float, r: Float, tr: Trip) {
        p.fillOval(cx - r, cy - r, r * 2f, r * 2f, tr.color)
        Text.line(p, tr.line, cx, cy + 0.5f, r * 1.1f, C.INK, Font.DISPLAY, Align.CENTER)
    }

    /** The line as a strip: its stations in a row, the ends named, where she is and where she is going. */
    private fun strip(p: Painter, y: Float, tr: Trip, train: Float) {
        val x0 = 46f
        val x1 = 354f
        val n = tr.stations.size
        p.fillRoundRect(8f, y - 30f, 384f, 62f, 6f, C.WHITE)
        roundel(p, 26f, y, 11f, tr)
        p.fillRect(x0, y - 2.5f, x1 - x0, 5f, tr.color)
        // the ends of the line, past the dots
        if (tr.ends.size >= 2) {
            Draw.arrow(p, x0 + 4f, y - 21f, 8f, "left", C.INK2)
            Text.line(p, tr.ends[0], x0 + 11f, y - 21f, 8f, C.INK2, Font.BOLD)
            Draw.arrow(p, x1 - 4f, y - 21f, 8f, "right", C.INK2)
            Text.line(p, tr.ends[1], x1 - 11f, y - 21f, 8f, C.INK2, Font.BOLD, Align.RIGHT)
        }
        for (i in 0 until n) {
            val x = x0 + (x1 - x0) * i / (n - 1).toFloat()
            val big = i == tr.from || i == tr.to
            p.fillOval(x - (if (big) 5f else 3.5f), y - (if (big) 5f else 3.5f), if (big) 10f else 7f, if (big) 10f else 7f, C.WHITE)
            p.strokeRoundRect(x - (if (big) 5f else 3.5f), y - (if (big) 5f else 3.5f), if (big) 10f else 7f, if (big) 10f else 7f, 5f, 1.5f, C.INK)
            if (big || n <= 8) {
                val name = tr.stations[i]
                var fs = 7f
                while (fs > 5.5f && p.textWidth(name, fs, Font.TEXT) > 70f) fs -= 0.5f
                val below = i % 2 == 0
                Text.line(p, name, x, if (below) y + 14f else y + 23f, fs, if (i == tr.to) C.RED_D else C.INK2, if (big) Font.BOLD else Font.TEXT, Align.CENTER)
            }
            if (i == tr.to) Icons.draw(p, "star", x - 6f, y - 18f, 12f, C.RED)
        }
        // the train, as a dot that slides along
        val tx = x0 + (x1 - x0) * train / (n - 1).toFloat()
        p.fillRoundRect(tx - 7f, y - 6f, 14f, 12f, 3f, C.INK)
        p.fillRect(tx - 4f, y - 3f, 8f, 3f, C.SKY)
    }

    override fun drawGame(p: Painter) {
        val tr = cur()
        when (phase) {
            GATE -> drawGate(p)
            PLATFORM -> drawPlatform(p, tr)
            MOVE, STOPPED, BACK -> drawCarriage(p, tr)
            CHANGE -> drawChange(p, tr)
            EXIT -> drawExit(p)
        }
    }

    private fun tiles(p: Painter, top: Float, bottom: Float) {
        p.fillRect(0f, top, Game.W, bottom - top, 0xFFF2EFE6.toInt())
        var y = top
        while (y < bottom) {
            var x = if (((y - top) / 10f).toInt() % 2 == 0) 0f else -10f
            while (x < Game.W) { p.strokeRoundRect(x, y, 20f, 10f, 2f, 0.6f, 0xFFD6D0C2.toInt()); x += 20f }
            y += 10f
        }
    }

    private fun drawGate(p: Painter) {
        tiles(p, 0f, 160f)
        p.fillRect(0f, 160f, Game.W, 80f, 0xFF8A8FA0.toInt())
        // the gates: two glass doors that slide apart
        p.fillRoundRect(120f, 70f, 40f, 110f, 4f, 0xFF4A5068.toInt())
        p.fillRoundRect(240f, 70f, 40f, 110f, 4f, 0xFF4A5068.toInt())
        val open = gateOpen * 36f
        p.fillRoundRect(160f - open, 92f, 40f, 66f, 6f, C.alpha(0xFF9FD3F2.toInt(), 0.8f))
        p.fillRoundRect(200f + open, 92f, 40f, 66f, 6f, C.alpha(0xFF9FD3F2.toInt(), 0.8f))
        // the reader, purple, where the card goes
        p.fillRoundRect(244f, 76f, 32f, 14f, 3f, 0xFF7C3F8C.toInt())
        Text.line(p, "Navigo", 260f, 83f, 7f, C.WHITE, Font.BOLD, Align.CENTER)
        // the sign over the gates
        p.fillRoundRect(110f, 30f, 180f, 26f, 4f, navy)
        Text.line(p, "Accès aux quais", 200f, 43f, 12f, C.WHITE, Font.DISPLAY, Align.CENTER)
        Text.block(p, "Tap the reader to put your card or phone on it.", 20f, 92f, 92f, 10f, C.INK2, Font.BOLD, Align.LEFT, 1.25f)
        if (gateOpen > 0.5f) Draw.stamp(p, 330f, 120f, 22f, "OK", C.GREEN)
    }

    private fun drawPlatform(p: Painter, tr: Trip) {
        tiles(p, 0f, 140f)
        strip(p, 72f, tr, tr.from.toFloat())
        Text.line(p, "Vous êtes ici : ${tr.stations[tr.from]}", 200f, 112f, 9.5f, C.INK, Font.BOLD, Align.CENTER)
        Text.line(p, "Where does the line end, on the side of the star?", 200f, 128f, 9f, C.INK2, Font.TEXT, Align.CENTER)
        p.fillRect(0f, 140f, Game.W, 100f, 0xFF6F7B99.toInt())
        p.fillRect(198f, 140f, 4f, 100f, 0xFFF2B233.toInt())
        for (k in 0 until 2) {
            val b = button("dir:$k") ?: continue
            val sx = b.x + b.shake * 4f * Math.sin(t * 40.0).toFloat()
            p.fillRoundRect(sx, b.y, b.w, b.h, 5f, navy)
            p.strokeRoundRect(sx, b.y, b.w, b.h, 5f, 1.5f, C.WHITE)
            roundel(p, sx + 18f, b.y + b.h / 2f, 10f, tr)
            Text.line(p, "Direction", sx + 34f, b.y + 12f, 8f, C.alpha(C.WHITE, 0.75f), Font.TEXT)
            val name = if (k < tr.ends.size) tr.ends[k] else ""
            var fs = 11f
            while (fs > 7f && p.textWidth(name, fs, Font.BOLD) > b.w - 40f) fs -= 0.5f
            Text.line(p, name, sx + 34f, b.y + 28f, fs, C.WHITE, Font.BOLD)
            Draw.arrow(p, sx + b.w / 2f, b.y + b.h + 12f, 14f, if (k == 0) "left" else "right", C.WHITE)
        }
    }

    private fun drawCarriage(p: Painter, tr: Trip) {
        val moving = phase == MOVE || phase == BACK
        // the strip map over the doors, with the train sliding along it
        val f = if (moving) Math.min(1f, pt / 2.2f) else 0f
        val shown = if (moving && phase == MOVE) at + dir * f else at.toFloat()
        p.fillRect(0f, 0f, Game.W, Game.H, 0xFFDAD6CC.toInt())
        strip(p, 66f, tr, shown)
        // the window: dark tunnel while moving, the station when stopped
        val wy = 106f
        p.fillRoundRect(40f, wy, 320f, 76f, 10f, C.INK)
        if (moving) {
            p.fillRoundRect(44f, wy + 4f, 312f, 68f, 8f, 0xFF15182B.toInt())
            val off = (t * 400f) % 80f
            var x = 44f - off
            while (x < 356f) { if (x > 40f) p.fillRect(x, wy + 30f, 20f, 3f, 0xFFF2B233.toInt()); x += 80f }
            if (phase == BACK) Text.line(p, "…", 200f, wy + 38f, 20f, C.WHITE, Font.DISPLAY, Align.CENTER)
        } else {
            tiles(p, wy + 4f, wy + 72f)
            // the station's name on its blue sign
            val name = tr.stations.getOrNull(at) ?: ""
            var fs = 15f
            while (fs > 9f && p.textWidth(name, fs, Font.DISPLAY) > 260f) fs -= 0.5f
            val w = Math.min(290f, p.textWidth(name, fs, Font.DISPLAY) + 30f)
            p.fillRoundRect(200f - w / 2f, wy + 22f, w, 30f, 4f, navy)
            Text.line(p, name, 200f, wy + 37f, fs, C.WHITE, Font.DISPLAY, Align.CENTER)
        }
        // the floor and seats
        p.fillRect(0f, 186f, Game.W, 54f, 0xFF8A8FA0.toInt())
        p.fillRoundRect(8f, 120f, 26f, 60f, 4f, 0xFF3F6DB5.toInt())
        p.fillRoundRect(366f, 120f, 26f, 60f, 4f, 0xFF3F6DB5.toInt())
        Text.line(p, "Arrêts : $stopsRidden", 12f, 228f, 10f, C.WHITE, Font.BOLD)
    }

    private fun drawChange(p: Painter, tr: Trip) {
        tiles(p, 0f, Game.H)
        p.fillRoundRect(70f, 70f, 260f, 70f, 6f, navy)
        Text.line(p, "Correspondance", 200f, 92f, 16f, C.WHITE, Font.DISPLAY, Align.CENTER)
        roundel(p, 160f, 120f, 13f, tr)
        Text.line(p, "Ligne ${tr.line}", 214f, 120f, 13f, C.WHITE, Font.BOLD, Align.CENTER)
        Draw.arrow(p, 200f, 170f, 26f, "right", navy)
    }

    private fun drawExit(p: Painter) {
        tiles(p, 0f, Game.H)
        p.fillRoundRect(100f, 40f, 200f, 70f, 6f, 0xFFF2B233.toInt())
        Text.line(p, "SORTIE", 200f, 66f, 22f, C.INK, Font.DISPLAY, Align.CENTER)
        Text.line(p, j.str("exitSign", "Sortie"), 200f, 92f, 10f, C.INK, Font.BOLD, Align.CENTER)
    }

    override fun describe() = "Metro trip=${trip + 1}/${trips.size} phase=$phase at=$at dir=$dir stops=$stopsRidden tip=$tip"

    // ---------- for the scripted playthrough ----------
    fun atGate() = phase == GATE && gateOpen == 0f
    fun onPlatform() = phase == PLATFORM
    fun stopped() = phase == STOPPED
    fun atTarget() = phase == STOPPED && at == cur().to
    fun atExit() = phase == EXIT
    fun rightDir(): String = "dir:${cur().towards}"
    fun wrongDir(): String = "dir:${1 - cur().towards}"
    fun tripIndex() = trip
}

/**
 * Unit 22: backstage, the models need dressing. Each model asks for a dress,
 * shoes or jewellery in French; she taps the right one on the rack. A wrong
 * one comes back with a shake of the head.
 */
class DressScreen(g: Game, id: String, onExit: () -> Unit, onDone: () -> Unit) : MiniGame(g, id, onExit, onDone) {
    private class Want(val item: String, val line: Line?)
    private class Model(val name: String, val face: FaceSpec, val wants: List<Want>, val thanks: Line?)

    private val models = ArrayList<Model>()
    private val racks = ArrayList<List<String>>()
    private val rackNames = ArrayList<String>()
    private var mi = 0
    private var wi = 0
    private val worn = ArrayList<String>()
    private var busy = false
    var misses = 0; private set
    private var slide = 0f

    override fun setup() {
        for (m in j.list("models")) {
            val ws = ArrayList<Want>()
            for (w in m.list("wants")) ws.add(Want(w.str("item"), Line.from(w["line"], m.str("who", d.who), null)))
            models.add(Model(m.str("name"), FaceSpec.from(m["face"]), ws, Line.from(m["thanks"], m.str("who", d.who), null)))
        }
        for (r in j.list("racks")) { racks.add(r.strings("items")); rackNames.add(Text.french(r.str("name"))) }
    }

    override fun start() { ask() }

    private fun want(): Want? = models.getOrNull(mi)?.wants?.getOrNull(wi)

    private fun ask() {
        busy = false
        rebuild()
        want()?.line?.let { g.say(it) }
    }

    private fun rackFor(item: String): Int {
        for ((i, r) in racks.withIndex()) if (r.contains(item)) return i
        return 0
    }

    override fun playButtons() {
        val w = want() ?: return
        val ri = rackFor(w.item)
        val items = racks[ri]
        val cols = 3
        val bw = 70f
        val bh = 62f
        for ((i, it) in items.withIndex()) {
            val b = add(Button("item:$it", 170f + (i % cols) * (bw + 6f), 64f + (i / cols) * (bh + 6f), bw, bh, "", Style.PLAIN) { pick(it) })
            b.icon = it
            b.iconSize = 48f
            b.enabled = !busy
        }
        val ln = w.line
        if (ln != null) add(Button("again", 170f, 206f, 64f, 24f, "Again", Style.DARK) { g.say(ln) }).also { it.icon = "speaker"; it.size = 10f }
        if (ln != null) add(Button("slow", 240f, 206f, 62f, 24f, "Slow", Style.DARK) { g.say(ln, true) }).also { it.icon = "slow"; it.size = 10f }
    }

    private fun pick(item: String) {
        val w = want() ?: return
        if (busy) return
        if (item == w.item) {
            g.audio.sfx("ok")
            busy = true
            worn.add(item)
            rebuild()
            after(0.9f) {
                wi++
                val m = models[mi]
                if (wi >= m.wants.size) {
                    val d0 = g.say(m.thanks)
                    after(d0 + 0.4f) {
                        slide = 0.01f
                        after(1.0f) {
                            slide = 0f
                            mi++
                            wi = 0
                            worn.clear()
                            if (mi >= models.size) win(line("done"), j.strOrNull("doneNarr"))
                            else ask()
                        }
                    }
                } else ask()
            }
        } else {
            g.audio.sfx("no")
            misses++
            button("item:$item")?.shake = 1f
            val wl = line("wrong")
            if (wl != null && misses % 2 == 1) { g.say(wl); showToast(wl.fr) }
        }
    }

    override fun play(dt: Float) { if (slide > 0f) slide = Math.min(1f, slide + dt) }

    override fun drawGame(p: Painter) {
        // the dressing room: a mirror with bulbs, a clothes rail
        p.fillRect(0f, 0f, Game.W, Game.H, 0xFF3A2A3E.toInt())
        p.fillRect(0f, 200f, Game.W, 40f, 0xFF5C4A42.toInt())
        p.fillRoundRect(12f, 34f, 146f, 196f, 8f, 0xFFF4EEDC.toInt())
        for (k in 0 until 6) p.fillOval(16f + k * 26f, 30f, 9f, 9f, 0xFFFFF4C8.toInt())
        // the model, made of a face and the clothes she has on
        val m = models.getOrNull(mi)
        if (m != null) {
            val dx = -slide * 160f
            val face = Faces.get("dress/" + m.name, m.face, if (wi >= m.wants.size) "smile" else "neutral")
            p.image(face, 49f + dx, 40f, 70f, 76f, false, 1f)
            val dress = worn.firstOrNull { it.startsWith("dress:") } ?: "dress:d8dbe6:short"
            Icons.draw(p, dress, 44f + dx, 104f, 80f)
            val shoes = worn.firstOrNull { it.startsWith("shoes:") }
            if (shoes != null) Icons.draw(p, shoes, 60f + dx, 180f, 48f)
            for (it in worn) if (!it.startsWith("dress:") && !it.startsWith("shoes:")) Icons.draw(p, it, 66f + dx, 100f, 36f)
            Text.line(p, m.name, 84f + dx, 224f, 10f, C.INK, Font.BOLD, Align.CENTER)
        }
        // what she asks for
        val w = want()
        if (w != null && !busy) {
            val ln = w.line
            if (ln != null) {
                Draw.bubble(p, 170f, 30f, 222f, 28f, 175f)
                var fs = 11.5f
                while (fs > 8f && p.textWidth(ln.fr, fs, Font.BOLD) > 210f) fs -= 0.5f
                Text.line(p, ln.fr, 178f, 44f, fs, C.INK, Font.BOLD)
            }
        }
        val rk = if (w != null) rackFor(w.item) else 0
        if (w != null && rk < rackNames.size) Text.line(p, rackNames[rk], 390f, 228f, 9f, C.alpha(C.WHITE, 0.8f), Font.BOLD, Align.RIGHT)
        Text.line(p, "${Math.min(mi + 1, models.size)} / ${models.size}", 384f, 210f, 9f, C.alpha(C.WHITE, 0.6f), Font.TEXT, Align.RIGHT)
    }

    override fun describe() = "Dress model=${mi + 1}/${models.size} want=$wi busy=$busy misses=$misses tip=$tip"

    // ---------- for the scripted playthrough ----------
    fun ready(): Boolean = !busy && want() != null && slide == 0f && !finished
    fun rightTap(): String? = want()?.let { "item:${it.item}" }
    fun wrongTap(): String? {
        val w = want() ?: return null
        for (it in racks[rackFor(w.item)]) if (it != w.item) return "item:$it"
        return null
    }
}
