package detective.core

/**
 * A ride through Paris. In "taxi" mode the taxi drives itself; at each corner
 * it stops and the driver asks which way. The map on the detective's knee shows
 * the way, and the player answers in French. A wrong answer sends the taxi
 * slowly round the block and back to the same corner. Between corners a sight
 * passes the window.
 *
 * In "drive" mode she is at the wheel and her passenger calls the way in
 * French; she taps the arrow (or faster, slower). A van follows them, and a
 * wrong turn lets it catch up, but nothing ends the chase.
 *
 * In "follow" mode a car drives ahead, and a tracker on her map shows which way
 * it turned; she tells the driver in French, as in taxi mode.
 */
class RideScreen(g: Game, rideId: String, private val onExit: () -> Unit, private val onDone: () -> Unit) : Screen(g) {
    private companion object {
        const val TIP = 0
        const val DRIVE = 1
        const val STOP = 2
        const val HERO = 3
        const val TURN = 4
        const val LOOP = 5
        const val ARRIVE = 6
    }

    private val ride = g.unit.rides[rideId] ?: throw IllegalArgumentException("unit ${g.unit.id} has no ride \"$rideId\"")
    private val tapMode = g.unit.english == "tap"
    private var leg = 0
    private var phase = TIP
    private var pt = 0f            // time in this phase
    private var scroll = 0f
    private var line: Line? = null
    private var showHelp = false
    private var chosen = ""        // the way the taxi is turning now
    private var wrongTurn = false
    private var misses = 0
    private val keys = ArrayList<String>()
    private var rightKey = ""
    var wrongs = 0; private set
    private val mode = ride.mode
    /** How far back the van is in "drive" mode: 1 is far away, near 0 it is right behind. */
    private var gap = 0.6f

    override fun enter() {
        phase = if (ride.tip.isEmpty()) DRIVE else TIP
        rebuild()
    }

    private fun cur(): Leg = ride.legs[Math.min(leg, ride.legs.size - 1)]

    private fun set(ph: Int) {
        phase = ph
        pt = 0f
        rebuild()
    }

    private fun startLeg() {
        clearTimers()
        line = null
        misses = 0
        if (leg >= ride.legs.size) {
            set(ARRIVE)
            val ar = ride.arrive
            line = ar
            if (ar != null) g.say(ar)
            return
        }
        set(DRIVE)
        after(3.0f) { stop() }
    }

    private fun stop() {
        val l = cur()
        // work out the replies on offer and which one is right
        keys.clear()
        if (l.ask == "confirm") {
            if (l.offer == l.turn) {
                rightKey = "yes"
                keys.add("yes")
                for (d in arrayOf("left", "straight", "right")) if (d != l.offer && keys.size < 3) keys.add("no_$d")
            } else {
                rightKey = "no_" + l.turn
                keys.add(rightKey)
                keys.add("yes")
                for (d in arrayOf("left", "straight", "right")) if (d != l.offer && d != l.turn) keys.add("no_$d")
            }
        } else if (l.turn == "faster" || l.turn == "slower") {
            rightKey = l.turn
            keys.add("slower"); keys.add("faster")
        } else {
            rightKey = l.turn
            keys.add("left"); keys.add("straight"); keys.add("right")
        }
        // arrows keep their places on the dashboard; spoken replies are shuffled
        if (mode != "drive") g.rng.shuffle(keys)
        line = l.line
        showHelp = false
        set(STOP)
        l.line?.let { g.say(it) }
    }

    private fun pick(key: String) {
        if (phase != STOP) return
        val o = ride.says[key]
        if (o == null && mode != "drive") return
        val l = cur()
        phase = HERO
        pt = 0f
        rebuild()
        button("say:$key")?.let { it.style = Style.PRIMARY; it.enabled = true }
        val d = if (o != null) g.voice.say("hero", o.say) else 0.1f
        after(d + 0.3f) {
            val ok = key == rightKey
            // the car does what she said, right or wrong
            chosen = when {
                key == "yes" -> l.offer
                key.startsWith("no_") -> key.substring(3)
                key == "faster" || key == "slower" -> "straight"
                else -> key
            }
            wrongTurn = !ok
            if (ok) { g.audio.sfx("ok"); gap = Math.min(1f, gap + 0.12f) }
            else { g.audio.sfx("no"); misses++; wrongs++; gap = Math.max(0.12f, gap - 0.3f) }
            line = null
            set(TURN)
            after(1.5f) {
                if (wrongTurn) {
                    val nl = ride.near
                    val ln = if (mode == "drive" && nl != null && gap < 0.35f) nl else ride.wrong
                    line = ln
                    ln?.let { g.say(it) }
                    set(LOOP)
                    after(3.6f) { stop() }
                } else {
                    leg++
                    startLeg()
                }
            }
        }
    }

    private fun rebuild() {
        buttons.clear()
        add(Button("exit", 6f, 3f, 28f, 22f, "", Style.DARK) { g.audio.stopVoice(); onExit() }).icon = "back"
        add(Button("book", 366f, 3f, 28f, 22f, "", Style.DARK) { g.openOverlay(NotebookScreen(g)) }).icon = "book"
        if (phase == TIP) {
            val b = add(Button("next", 156f, 168f, 88f, 30f, "Go!", Style.PRIMARY) { startLeg() })
            b.font = Font.DISPLAY
            b.size = 15f
            return
        }
        val ln = line
        if (ln != null && (phase == STOP || phase == ARRIVE)) {
            add(Button("again", 232f, 46f, 30f, 22f, "", Style.DARK) { g.say(ln) }).icon = "speaker"
        }
        if ((phase == STOP || phase == HERO) && mode == "drive") {
            // the dashboard: big arrows, or a pedal for faster and slower
            val n = keys.size
            val w = 72f
            val x0 = 135f - (n * w + (n - 1) * 10f) / 2f
            for ((i, k) in keys.withIndex()) {
                val b = add(Button("say:$k", x0 + i * (w + 10f), 198f, w, 36f, "", Style.PLAIN) { pick(k) })
                b.icon = when (k) { "faster" -> "faster"; "slower" -> "slow"; else -> "dir:$k" }
                b.iconSize = 28f
                b.enabled = phase == STOP
            }
        } else if (phase == STOP || phase == HERO) {
            val n = keys.size
            val w = (384f - (n - 1) * 6f) / n
            for ((i, k) in keys.withIndex()) {
                val o = ride.says[k] ?: continue
                val b = add(Button("say:$k", 8f + i * (w + 6f), 200f, w, 32f, o.fr, Style.PLAIN) { pick(k) })
                b.size = 13f
                b.enabled = phase == STOP
            }
        }
        if (phase == ARRIVE) {
            val b = add(Button("next", 304f, 202f, 88f, 30f, "Next", Style.PRIMARY) { onDone() })
            b.font = Font.DISPLAY
            b.size = 15f
        }
    }

    override fun update(dt: Float) {
        super.update(dt)
        pt += dt
        if (phase == DRIVE || phase == LOOP) scroll += dt * 70f
    }

    override fun tapUp(x: Float, y: Float) {
        if (line != null && y > 28f && y < 72f && x > 40f && x < 270f) showHelp = !showHelp
    }

    // ---------- drawing ----------

    private val roadX = 105f
    private val roadW = 60f
    private val crossY = 78f
    private val crossH = 54f

    private fun building(p: Painter, x: Float, y: Float, w: Float, h: Float, seed: Int) {
        val wall = if (seed % 2 == 0) 0xFFEADFC4.toInt() else 0xFFE2D4B4.toInt()
        p.fillRect(x, y, w, h, wall)
        p.fillRect(x, y, w, 5f, 0xFF6F7B99.toInt())
        var wy = y + 11f
        while (wy + 8f < y + h) {
            var wx = x + 6f
            while (wx + 7f < x + w) {
                p.fillRect(wx, wy, 7f, 8f, 0xFF9FB6CC.toInt())
                p.fillRect(wx, wy + 8f, 7f, 1.5f, 0xFF8A5A2B.toInt())
                wx += 14f
            }
            wy += 16f
        }
    }

    override fun draw(p: Painter) {
        p.fillRect(0f, 0f, Game.W, Game.H, 0xFFB9B4A8.toInt())
        val atCorner = phase == STOP || phase == HERO || phase == TURN
        // the cross street slides down into view as the taxi comes up to the corner
        var cy = crossY
        if (phase == DRIVE) cy = if (pt > 2.2f) crossY - (3.0f - pt) / 0.8f * 150f else -200f
        if (phase == LOOP) cy = -200f
        if (phase == ARRIVE || phase == TIP) cy = -200f

        p.pushClip(0f, 28f, 270f, 168f)
        // pavements and the road
        p.fillRect(0f, 28f, 270f, 168f, 0xFFCFC9BB.toInt())
        p.fillRect(roadX, 28f, roadW, 168f, 0xFF5E6472.toInt())
        // buildings scroll by on both sides
        val off = scroll % 64f
        var by = -64f + off + 28f
        var k = (scroll / 64f).toInt()
        while (by < 200f) {
            building(p, 6f, by, 88f, 58f, k)
            building(p, 176f, by, 88f, 58f, k + 1)
            by += 64f
            k--
        }
        if (cy > -150f) {
            p.fillRect(0f, cy - 6f, 270f, crossH + 12f, 0xFFCFC9BB.toInt())
            p.fillRect(0f, cy, 270f, crossH, 0xFF5E6472.toInt())
            p.fillRect(roadX, cy - 6f, roadW, crossH + 12f, 0xFF5E6472.toInt())
        }
        // lane dashes
        var dy = 28f - 24f + (scroll % 24f)
        while (dy < 200f) {
            if (cy < -150f || dy < cy - 14f || dy > cy + crossH + 2f) p.fillRect(roadX + roadW / 2f - 1f, dy, 2f, 12f, 0xFFE8E2D0.toInt())
            dy += 24f
        }
        if (phase == ARRIVE && ride.scene == "jump") {
            drawJump(p)
        } else if (phase == ARRIVE && ride.scene == "roadblock") {
            drawRoadblock(p)
        } else if (phase == ARRIVE) {
            // the hotel front, with its doorman's carpet
            p.fillRect(0f, 28f, 270f, 64f, 0xFFEADFC4.toInt())
            p.fillRect(0f, 28f, 270f, 8f, 0xFF6F7B99.toInt())
            var wx = 12f
            while (wx < 260f) { p.fillRect(wx, 44f, 14f, 22f, 0xFF9FB6CC.toInt()); wx += 30f }
            p.fillRoundRect(96f, 52f, 78f, 40f, 4f, C.INK)
            p.fillRect(118f, 64f, 34f, 28f, 0xFFFDF3C4.toInt())
            Text.line(p, "HÔTEL", 135f, 58f, 8f, C.MUSTARD, Font.DISPLAY, Align.CENTER)
            p.fillRect(120f, 92f, 30f, 60f, C.RED_D)
        }

        // the car ahead, in "follow" mode: always a little way up the road, and at a corner it turns first
        if (mode == "follow" && phase != ARRIVE && phase != TIP && phase != LOOP && ride.lead.isNotEmpty()) {
            val lu = Looks.img(g, ride.lead, 0)
            val ls = Looks.img(g, ride.lead.replace("_up", "_side"), 0)
            val lx = roadX + roadW / 2f
            if (atCorner || (phase == DRIVE && pt > 2.2f)) {
                val turn = cur().turn
                val a = if (atCorner) Math.min(1f, (pt + (if (phase == STOP) 0f else 1f)) / 1.2f) else 0f
                val ly = crossY + crossH / 2f + 6f
                when (turn) {
                    "left" -> if (a < 1f) p.image(ls, lx - ls.w - a * 150f, ly - ls.h, ls.w * 2f, ls.h * 2f, true, 1f)
                    "right" -> if (a < 1f) p.image(ls, lx - ls.w + a * 150f, ly - ls.h, ls.w * 2f, ls.h * 2f, false, 1f)
                    else -> p.image(lu, lx - lu.w, ly - lu.h * 2f - a * 120f, lu.w * 2f, lu.h * 2f, false, 1f)
                }
            } else if (phase == DRIVE) p.image(lu, lx - lu.w, 52f - lu.h, lu.w * 2f, lu.h * 2f, false, 1f)
        }

        // her car
        val up = Looks.img(g, ride.car, 0)
        val side = Looks.img(g, ride.car.replace("_up", "_side"), 0)
        var tx = roadX + roadW / 2f
        var ty = 168f
        var drawSide = false
        var flip = false
        if (phase == TURN) {
            val a1 = Math.min(1f, pt / 0.55f)
            ty = 168f - a1 * (168f - (crossY + crossH / 2f + 8f))
            if (pt > 0.55f) {
                val a2 = (pt - 0.55f) / 0.95f
                when (chosen) {
                    "left" -> { drawSide = true; flip = true; tx -= a2 * 190f; ty = crossY + crossH / 2f + 8f }
                    "right" -> { drawSide = true; tx += a2 * 190f; ty = crossY + crossH / 2f + 8f }
                    else -> ty -= a2 * 150f
                }
            }
        } else if (phase == ARRIVE) ty = 150f
        else if (phase == DRIVE && pt < 0.6f && leg > 0) ty = 168f + (0.6f - pt) * 80f
        // the van behind, in "drive" mode
        if (mode == "drive" && ride.chaser.isNotEmpty() && phase != TIP && phase != ARRIVE) {
            val vu = Looks.img(g, ride.chaser, 0)
            val shown = if (phase == LOOP) Math.max(0.12f, gap) else gap
            val vy = 168f + 22f + shown * 22f
            val vb = if (phase == DRIVE || phase == LOOP) Math.sin(t * 15.0).toFloat() * 0.6f else 0f
            p.image(vu, roadX + roadW / 2f - vu.w, vy - vu.h + vb, vu.w * 2f, vu.h * 2f, false, 1f)
        }
        val bob = if (phase == DRIVE || phase == LOOP) Math.sin(t * 18.0).toFloat() * 0.6f else 0f
        if (phase != LOOP && !(phase == ARRIVE && ride.scene != "hotel")) {
            if (drawSide) p.image(side, tx - side.w, ty - side.h, side.w * 2f, side.h * 2f, flip, 1f)
            else p.image(up, tx - up.w, ty - up.h * 2f + bob, up.w * 2f, up.h * 2f, false, 1f)
        }
        p.popClip()

        // the panel on the right: the map on her knee, or what is passing the window
        Draw.card(p, 276f, 30f, 118f, 164f, C.PAPER)
        val l = cur()
        val sight = l.sight
        if (phase == TIP) {
            Icons.draw(p, "map", 303f, 58f, 64f)
        } else if (phase == DRIVE && sight != null && pt < 2.6f) {
            Text.line(p, "Out of the window", 335f, 40f, 8.5f, C.GREY, Font.TEXT, Align.CENTER)
            Icons.draw(p, sight.icon, 305f, 46f, 60f)
            Text.line(p, sight.title, 335f, 116f, 12f, C.INK, Font.DISPLAY, Align.CENTER)
            Text.block(p, sight.text, 282f, 124f, 106f, 8.5f, C.INK2, Font.TEXT, Align.CENTER, 1.22f)
        } else if (phase == LOOP && mode == "drive") {
            Text.line(p, "Round the block...", 335f, 40f, 8.5f, C.GREY, Font.TEXT, Align.CENTER)
            drawMirror(p, 284f, 50f, 102f, 70f)
            Text.block(p, "The van is catching up!", 284f, 134f, 102f, 10f, C.RED_D, Font.BOLD, Align.CENTER, 1.2f)
        } else if (phase == LOOP) {
            Text.line(p, "Round the block...", 335f, 40f, 8.5f, C.GREY, Font.TEXT, Align.CENTER)
            p.fillRoundRect(305f, 70f, 60f, 60f, 4f, 0xFFD9CDB0.toInt())
            val f = (pt / 3.6f) % 1f
            val per = f * 4f
            val s = 76f
            val x0 = 297f
            val y0 = 62f
            val (dxp, dyp) = when (per.toInt()) {
                0 -> Pair(per * s, 0f)
                1 -> Pair(s, (per - 1f) * s)
                2 -> Pair(s - (per - 2f) * s, s)
                else -> Pair(0f, s - (per - 3f) * s)
            }
            p.fillOval(x0 + dxp - 5f, y0 + dyp - 5f, 10f, 10f, C.MUSTARD)
            p.strokeRoundRect(x0 + dxp - 5f, y0 + dyp - 5f, 10f, 10f, 5f, 1f, C.INK)
        } else if (phase == ARRIVE) {
            Icons.draw(p, when (ride.scene) { "jump" -> "river"; "roadblock" -> "stop"; else -> "hotel" }, 303f, 58f, 64f)
        } else if (mode == "drive") {
            Text.line(p, if (ride.panel.isNotEmpty()) ride.panel else "Behind you", 335f, 40f, 8.5f, C.GREY, Font.TEXT, Align.CENTER)
            drawMirror(p, 284f, 50f, 102f, 70f)
            Text.block(p, if (l.turn == "faster" || l.turn == "slower") "Faster or slower?" else "Listen. Which way?", 284f, 134f, 102f, 10f, C.INK2, Font.BOLD, Align.CENTER, 1.2f)
        } else {
            Text.line(p, if (ride.panel.isNotEmpty()) ride.panel else "Your map", 335f, 40f, 8.5f, C.GREY, Font.TEXT, Align.CENTER)
            drawMap(p, 284f, 48f, 102f, 138f, l.turn)
        }

        // the driver's words
        val ln = line
        if (ln != null) {
            Draw.bubble(p, 48f, 30f, 216f, 40f, 50f)
            var size = 13f
            while (size > 9f && p.textWidth(ln.fr, size, Font.BOLD) > 176f) size -= 0.5f
            Text.line(p, ln.fr, 56f, 43f, size, C.INK, Font.BOLD)
            val help = if (ln.en.isNotEmpty()) ln.en else ln.gloss.joinToString("   ")
            if (!tapMode || showHelp) Text.line(p, help, 56f, 59f, 9.5f, C.GREY, Font.TEXT)
            else if (help.isNotEmpty()) Text.line(p, "tap for help", 56f, 59f, 8.5f, C.GREY_L, Font.TEXT)
            Icons.draw(p, "man", 8f, 32f, 34f)
        }

        if (phase == TIP) {
            p.fillRect(0f, 0f, Game.W, Game.H, C.alpha(C.NIGHT, 0.55f))
            Draw.card(p, 60f, 56f, 280f, 150f, C.PAPER)
            Icons.draw(p, "taxi", 76f, 72f, 50f)
            Text.block(p, ride.tip, 136f, 70f, 190f, 12f, C.INK, Font.BOLD, Align.LEFT, 1.3f)
        }
        // progress: one dot for each corner
        if (phase != TIP) {
            val n = ride.legs.size
            val x0 = 135f - (n - 1) * 6f
            for (i in 0 until n) p.fillOval(x0 + i * 12f - 3f, 10f, 6f, 6f, if (i < leg) C.GREEN else if (i == leg) C.MUSTARD else C.GREY_L)
        }
        drawButtons(p)
    }

    /** The map on her knee, drawn the way the taxi faces: the route goes up the page, then turns. */
    private fun drawMap(p: Painter, x: Float, y: Float, w: Float, h: Float, turn: String) {
        val block = 0xFFD9CDB0.toInt()
        p.fillRect(x, y, w, h, 0xFFFFFFFF.toInt())
        // four blocks round a crossing
        val cxm = x + w / 2f
        val cym = y + h * 0.42f
        val st = 16f
        p.fillRect(x + 2f, y + 2f, w / 2f - st / 2f - 2f, cym - y - st / 2f - 2f, block)
        p.fillRect(cxm + st / 2f, y + 2f, w / 2f - st / 2f - 2f, cym - y - st / 2f - 2f, block)
        p.fillRect(x + 2f, cym + st / 2f, w / 2f - st / 2f - 2f, y + h - cym - st / 2f - 2f, block)
        p.fillRect(cxm + st / 2f, cym + st / 2f, w / 2f - st / 2f - 2f, y + h - cym - st / 2f - 2f, block)
        val red = C.RED
        if (mode == "follow") {
            // the tracker: a blinking dot that has gone round the corner, with its trail
            val f = Math.min(1f, (t % 2.4f) / 1.6f)
            val ex = when (turn) { "left" -> x + 14f; "right" -> x + w - 14f; else -> cxm }
            val ey = if (turn == "left" || turn == "right") cym else y + 14f
            val dx: Float
            val dy: Float
            if (f < 0.5f) { dx = cxm; dy = (y + h - 26f) + (cym - (y + h - 26f)) * f * 2f }
            else { dx = cxm + (ex - cxm) * (f - 0.5f) * 2f; dy = cym + (ey - cym) * (f - 0.5f) * 2f }
            p.line(cxm, y + h - 26f, cxm, cym, 2f, C.alpha(red, 0.35f))
            p.line(cxm, cym, ex, ey, 2f, C.alpha(red, 0.35f))
            val blink = (t * 4f).toInt() % 2 == 0
            p.fillOval(dx - 6f, dy - 6f, 12f, 12f, if (blink) red else C.RED_D)
            p.strokeRoundRect(dx - 6f, dy - 6f, 12f, 12f, 6f, 1.5f, C.WHITE)
        } else {
            // the route
            p.line(cxm, y + h - 10f, cxm, cym, 4f, red)
            when (turn) {
                "left" -> { p.line(cxm, cym, x + 14f, cym, 4f, red); Draw.arrow(p, x + 12f, cym, 14f, "left", red) }
                "right" -> { p.line(cxm, cym, x + w - 14f, cym, 4f, red); Draw.arrow(p, x + w - 12f, cym, 14f, "right", red) }
                else -> { p.line(cxm, cym, cxm, y + 14f, 4f, red); Draw.arrow(p, cxm, y + 12f, 14f, "up", red) }
            }
        }
        // the taxi
        p.fillOval(cxm - 5f, y + h - 15f, 10f, 10f, C.MUSTARD)
        p.strokeRoundRect(cxm - 5f, y + h - 15f, 10f, 10f, 5f, 1f, C.INK)
        p.strokeRoundRect(x, y, w, h, 2f, 1f, C.INK)
    }

    /** The rear-view mirror: the van grows as it gets closer. */
    private fun drawMirror(p: Painter, x: Float, y: Float, w: Float, h: Float) {
        p.fillRoundRect(x - 3f, y - 3f, w + 6f, h + 6f, 10f, C.INK)
        p.fillRoundRect(x, y, w, h, 8f, 0xFFB9C9DA.toInt())
        p.pushClip(x, y, w, h)
        p.fillRect(x + w * 0.30f, y, w * 0.40f, h, 0xFF7A808C.toInt())
        if (ride.chaser.isNotEmpty()) {
            val s = 20f + (1f - gap) * 46f
            Icons.draw(p, "truck_red", x + (w - s) / 2f, y + h - s * 0.86f, s)
        }
        p.popClip()
        val warn = gap < 0.35f && ((t * 3f).toInt() % 2 == 0)
        if (warn) p.strokeRoundRect(x - 3f, y - 3f, w + 6f, h + 6f, 10f, 3f, C.RED)
    }

    /** Over the river: the car flies off the ramp and lands on the far bank; the van stops at the edge. */
    private fun drawJump(p: Painter) {
        p.fillRect(0f, 28f, 270f, 168f, 0xFFCFC9BB.toInt())
        p.fillRect(0f, 70f, 270f, 64f, 0xFF4F7FB0.toInt())
        for (i in 0 until 9) p.fillRect(10f + i * 30f + (t * 12f) % 30f, 96f + (i % 3) * 10f, 14f, 2f, 0xFF7FA7D0.toInt())
        p.fillRect(0f, 64f, 270f, 6f, 0xFF9C9484.toInt())
        p.fillRect(0f, 134f, 270f, 6f, 0xFF9C9484.toInt())
        p.fillRect(roadX, 28f, roadW, 36f, 0xFF5E6472.toInt())
        p.fillRect(roadX, 140f, roadW, 56f, 0xFF5E6472.toInt())
        // the ramp
        p.fillPoly(floatArrayOf(roadX + 6f, 152f, roadX + roadW - 6f, 152f, roadX + roadW - 12f, 138f, roadX + 12f, 138f), 0xFFC98A14.toInt())
        val up = Looks.img(g, ride.car, 0)
        val f = Math.min(1f, pt / 1.6f)
        val cy = 176f - f * 140f
        val sc = 2f + Math.sin(f * Math.PI).toFloat() * 1.0f
        p.fillOval(roadX + roadW / 2f - 10f, cy + 6f + Math.sin(f * Math.PI).toFloat() * 14f, 20f, 7f, C.alpha(C.INK, 0.25f))
        p.image(up, roadX + roadW / 2f - up.w * sc / 2f, cy - up.h * sc, up.w * sc, up.h * sc, false, 1f)
        if (ride.chaser.isNotEmpty()) {
            val vu = Looks.img(g, ride.chaser, 0)
            val vy = Math.min(186f, 220f - pt * 30f)
            p.image(vu, roadX + roadW / 2f - vu.w, vy - vu.h, vu.w * 2f, vu.h * 2f, false, 1f)
        }
    }

    /** The end of the chase: police cars across the road, and the car ahead stopped in front of them. */
    private fun drawRoadblock(p: Painter) {
        p.fillRect(0f, 28f, 270f, 168f, 0xFF2B3358.toInt())
        p.fillRect(roadX, 28f, roadW, 168f, 0xFF4A5068.toInt())
        // the barrier, red and white
        for (i in 0 until 9) p.fillRect(roadX - 40f + i * 15f, 104f, 15f, 8f, if (i % 2 == 0) C.RED else C.WHITE)
        for (k in 0 until 2) {
            val bx = if (k == 0) roadX - 52f else roadX + roadW + 8f
            p.fillRoundRect(bx, 88f, 44f, 26f, 5f, 0xFF2F548F.toInt())
            p.fillRect(bx + 4f, 92f, 36f, 6f, C.WHITE)
            val on = ((t * 4f).toInt() + k) % 2 == 0
            p.fillOval(bx + 16f, 82f, 12f, 8f, if (on) 0xFF5F8FD6.toInt() else 0xFF22305A.toInt())
        }
        if (ride.lead.isNotEmpty()) {
            val lu = Looks.img(g, ride.lead, 0)
            p.image(lu, roadX + roadW / 2f - lu.w, 154f - lu.h * 2f, lu.w * 2f, lu.h * 2f, false, 1f)
        }
        val up = Looks.img(g, ride.car, 0)
        p.image(up, roadX + roadW / 2f - up.w, 196f - up.h * 2f + 8f, up.w * 2f, up.h * 2f, false, 1f)
    }

    override fun back(): Boolean { g.audio.stopVoice(); onExit(); return true }

    override fun describe() = "Ride leg=${leg + 1}/${ride.legs.size} phase=$phase right=$rightKey"

    // ---------- for the scripted playthrough ----------

    fun tipShowing(): Boolean = phase == TIP
    fun waiting(): Boolean = phase == STOP
    fun arrived(): Boolean = phase == ARRIVE
    fun legIndex(): Int = leg
    fun rightTap(): String = "say:$rightKey"
    fun wrongTap(): String? { for (k in keys) if (k != rightKey) return "say:$k"; return null }
}
