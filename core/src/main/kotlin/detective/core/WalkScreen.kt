package detective.core

/**
 * A walking scene: the detective walks through a place seen from above, reads
 * the French signs, and does the steps of the scene's quest in order. Nothing
 * is faster than a walk, there is no clock and there are no lives. A bump
 * sends her back a few steps. Stars on the floor are checkpoints.
 *
 * What a scene holds is all in the unit file: the map, signs, people to talk
 * to, things that roll about, people to say "Pardon !" to, someone to chase,
 * someone who follows, things to pick up, places to find from a spoken clue,
 * things to carry to a place or to someone who wants them, guards who watch
 * with a torch, and optionally darkness with a torch of her own.
 */
class WalkScreen(g: Game, private val sceneId: String, private val onExit: () -> Unit, private val onDone: () -> Unit) : Screen(g) {
    companion object {
        const val SPEED = 58f          // the detective, in pixels a second (a tile is 16)
        const val HALF = 4.5f          // half the width of her feet
        const val FEET = 6f            // height of her feet box
    }

    private class Mv(val def: Mover) {
        var x = 0f; var y = 0f; var to = 1; var fwd = true; var flip = false; var spoke = false; var sayT = 0f
    }
    private class Bl(val def: Blocker) { var asideT = 0f }
    private class Rn(val def: Runner) {
        var x = 0f; var y = 0f; var to = 1; var flip = false; var atEnd = false; var caught = false; var moving = false
    }
    private class Wt(val def: Watcher) {
        var x = 0f; var y = 0f; var to = 1; var fwd = true; var pauseT = 0f
        var fx = 0f; var fy = 1f; var sayT = 0f; var moving = false
    }
    private class Fl(val def: Follower) {
        var x = 0f; var y = 0f; var pauseT = 0f; var repathT = 0f; var nextX = 0f; var nextY = 0f
        var flip = false; var moving = false; var sayT = 0f
    }

    private val a = g.unit.walks[sceneId] ?: throw IllegalArgumentException("unit ${g.unit.id} has no walking scene \"$sceneId\"")
    private val ts = g.tilesets[a.tileset] ?: throw IllegalArgumentException("no tileset \"${a.tileset}\" in art/tilesets.json")
    private val mh = a.map.size
    private val mw = a.map.maxOf { it.length }
    private val tiles = Array(mh) { r -> CharArray(mw) { c -> if (c < a.map[r].length) a.map[r][c] else ' ' } }
    private val T = Game.TILE
    val zoom = a.zoom
    private val viewW = Game.W / zoom
    private val viewH = Game.H / zoom
    private val saveKey = g.unit.id + "/" + sceneId
    private var started = false

    // the detective
    var hx = 0f; private set
    var hy = 0f; private set
    private var dir = 0            // 0 down, 1 up, 2 left, 3 right
    private var walkT = 0f
    private var moving = false
    private var pushX = 0f
    private var pushY = 0f
    private var touching = false
    private var needRelease = false
    private var tx = 0f
    private var ty = 0f
    private var kx = 0
    private var ky = 0
    var camX = 0f; private set
    var camY = 0f; private set
    private val trail = ArrayList<Float>()
    private var slideT = -1f
    private var slideFromX = 0f
    private var slideFromY = 0f
    private var slideToX = 0f
    private var slideToY = 0f
    private var safeT = 0f
    private var sayT = 0f
    private var sayText = ""
    var bumps = 0; private set

    private val movers = ArrayList<Mv>()
    private val blockers = ArrayList<Bl>()
    private val runners = ArrayList<Rn>()
    private val followers = ArrayList<Fl>()
    private val watchers = ArrayList<Wt>()
    private val nearNpc = HashSet<String>()
    private val nearSpot = HashSet<Int>()
    private val npcTiles = HashSet<Int>()

    // the quest
    var questIndex = 0; private set
    private var seekIdx = 0
    private val picked = HashSet<Int>()
    private val inventory = HashSet<String>()
    private val wrongNear = HashSet<Int>()
    private var hint = ""
    private var hiderX = 0f
    private var hiderY = 0f
    private var hiderToX = 0f
    private var hiderToY = 0f
    private var netT = 0f
    private var netX = 0f
    private var netY = 0f
    private var needWarned = false
    // carrying
    private var carrying = false
    private var soft = 0f
    private var delivered = 0
    private val fed = HashSet<String>()
    var caughtTimes = 0; private set

    // things on top of the world
    private var tip = true
    private var dialogOpen = false
    private var dLine: Line? = null
    private var dNarr: String? = null
    private var dIcon: String? = null
    private var afterDialog: (() -> Unit)? = null
    private var showEnglish = false
    private var menu = false
    private var ticket = false
    private var toast: String? = null
    private var toastT = 0f
    private var star = ""
    private var finished = false
    private var begun = false

    override fun enter() {
        if (started) { rebuild(); return }
        started = true
        var sx = a.startX
        var sy = a.startY
        star = g.save.checkpoints[saveKey] ?: ""
        if (star.isNotEmpty()) {
            val parts = star.split(',')
            if (parts.size == 2) {
                sx = parts[0].toIntOrNull() ?: sx
                sy = parts[1].toIntOrNull() ?: sy
                tip = false
            }
        }
        if (a.tip.isEmpty()) tip = false
        place(sx, sy)
        for (m in a.movers) {
            if (m.path.size < 2) continue
            val mv = Mv(m)
            mv.x = cx(m.path[0][0])
            mv.y = fy(m.path[0][1])
            movers.add(mv)
            // start part of the way along, so movers are not all in step
            if (m.phase > 0f) {
                mv.x += (cx(m.path[1][0]) - mv.x) * m.phase
                mv.y += (fy(m.path[1][1]) - mv.y) * m.phase
            }
        }
        for (b in a.blockers) blockers.add(Bl(b))
        for (r in a.runners) {
            if (r.path.isEmpty()) continue
            val rn = Rn(r)
            rn.x = cx(r.path[0][0])
            rn.y = fy(r.path[0][1])
            if (r.path.size < 2) rn.atEnd = true
            runners.add(rn)
        }
        for (f in a.followers) {
            val fl = Fl(f)
            fl.x = cx(f.x)
            fl.y = fy(f.y)
            fl.nextX = fl.x
            fl.nextY = fl.y
            followers.add(fl)
        }
        for (w in a.watchers) {
            if (w.path.isEmpty()) continue
            val wt = Wt(w)
            wt.x = cx(w.path[0][0])
            wt.y = fy(w.path[0][1])
            if (w.path.size > 1) {
                val dx = cx(w.path[1][0]) - wt.x
                val dy = fy(w.path[1][1]) - wt.y
                val d = Math.max(0.1f, Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat())
                wt.fx = dx / d; wt.fy = dy / d
            }
            watchers.add(wt)
        }
        for (n in a.npcs) npcTiles.add(n.y * mw + n.x)
        inventory.addAll(a.carry)
        snapCamera()
        if (a.quest.isEmpty()) throw IllegalArgumentException("walking scene \"$sceneId\" has no quest, so it could never end")
        hint = a.quest[0].hint
        if (!tip) { begun = true; beginStep(0) }
        rebuild()
    }

    private fun cx(tileX: Int) = tileX * T + T / 2f
    private fun fy(tileY: Int) = tileY * T + 13f

    private fun place(tileX: Int, tileY: Int) {
        hx = cx(tileX)
        hy = fy(tileY)
        trail.clear()
        touching = false
        slideT = -1f
    }

    private fun snapCamera() {
        camX = clampCamX(hx - viewW / 2f)
        camY = clampCamY(hy - viewH / 2f - 6f)
    }

    private fun clampCamX(v: Float) = if (mw * T <= viewW) (mw * T - viewW) / 2f else Math.max(0f, Math.min(mw * T - viewW, v))
    // the view may run a little past the bottom of the map, so nothing hides under a speech panel
    private fun clampCamY(v: Float, extra: Float = 0f) = Math.max(0f, Math.min(Math.max(0f, mh * T - viewH + 40f + extra), v))

    // ---------- the quest ----------

    private fun step(): QuestStep? = if (questIndex < a.quest.size) a.quest[questIndex] else null

    private fun beginStep(i: Int) {
        questIndex = i
        if (i >= a.quest.size) { finish(); return }
        val q = a.quest[i]
        hint = q.hint
        picked.clear()
        wrongNear.clear()
        seekIdx = 0
        needWarned = false
        carrying = false
        soft = 0f
        delivered = 0
        fed.clear()
        if (q.openX >= 0 && q.openY in 0 until mh && q.openX in 0 until mw) tiles[q.openY][q.openX] = 'O'
        if (q.type == "seek" && q.places.isNotEmpty()) {
            val pl = q.places[0]
            hiderX = cx(pl.x); hiderY = fy(pl.y); hiderToX = hiderX; hiderToY = hiderY
            if (q.line != null) open(q.line, null, null) { announce() } else announce()
        } else if (q.line != null) open(q.line, null, null, null)
    }

    private fun announce() {
        val q = step() ?: return
        if (seekIdx >= q.places.size) return
        val pl = q.places[seekIdx]
        open(pl.line, null, if (pl.hint.isEmpty()) null else pl.hint, null)
    }

    private fun completeStep() {
        val q = step() ?: return
        g.audio.sfx("ok")
        found(q.found)
        val next = questIndex + 1
        if (q.done != null || q.doneNarr != null) open(q.done, q.doneNarr, null) { beginStep(next) }
        else beginStep(next)
    }

    private fun finish() {
        if (finished) return
        finished = true
        g.save.checkpoints.remove(saveKey)
        g.save.commit()
        g.audio.sfx("win")
        onDone()
    }

    private fun runner(id: String): Rn? { for (r in runners) if (r.def.id == id) return r; return null }

    private fun checkQuest(tileX: Int, tileY: Int) {
        val q = step() ?: return
        when (q.type) {
            "reach", "get" -> {
                if (tileX >= q.x && tileX < q.x + q.w && tileY >= q.y && tileY < q.y + q.h) {
                    if (q.type == "get" && q.item.isNotEmpty()) inventory.add(q.item)
                    completeStep()
                }
            }
            "enter" -> {
                // a door in a wall: stand under it and push up
                if (tileX == q.x && tileY == q.y + 1 && moving && pushY < -0.5f && hy - FEET - ((q.y + 1) * T) < 2.5f) {
                    if (q.y in 0 until mh && q.x in 0 until mw) tiles[q.y][q.x] = 'O'
                    completeStep()
                }
            }
            "collect" -> {
                for (i in q.items.indices) {
                    if (picked.contains(i)) continue
                    val it = q.items[i]
                    if (Math.abs(cx(it[0]) - hx) < 10f && Math.abs(fy(it[1]) - hy) < 10f) {
                        picked.add(i)
                        if (picked.size >= q.items.size) completeStep()
                        else { g.audio.sfx("ok"); showToast("${picked.size} / ${q.items.size}") }
                    }
                }
            }
            "catch" -> {
                val r = runner(q.runner) ?: return
                if (r.caught) return
                val close = Math.abs(r.x - hx) < 14f && Math.abs(r.y - hy) < 12f
                if (close && q.needs.isNotEmpty() && !inventory.contains(q.needs)) {
                    if (!needWarned) { needWarned = true; showToast(hint) }
                } else if (close) {
                    r.caught = true
                    netT = 1.2f
                    netX = r.x
                    netY = r.y
                    g.audio.sfx("stamp")
                    touching = false
                    needRelease = true
                    after(1.2f) { completeStep() }
                }
            }
            "carry" -> {
                if (!carrying) {
                    for (i in q.items.indices) {
                        if (picked.contains(i)) continue
                        val it = q.items[i]
                        if (Math.abs(cx(it[0]) - hx) < 11f && Math.abs(fy(it[1]) - hy) < 11f) {
                            picked.add(i)
                            carrying = true
                            soft = 0f
                            g.audio.sfx("tap")
                            break
                        }
                    }
                } else if (q.to.isEmpty() && tileX >= q.x && tileX < q.x + q.w && tileY >= q.y && tileY < q.y + q.h) {
                    deliver(q)
                }
            }
            "lead" -> {
                val r = runner(q.runner) ?: return
                if (r.atEnd && Math.abs(r.x - hx) < 46f && Math.abs(r.y - hy) < 46f) completeStep()
            }
            "seek" -> {
                if (seekIdx >= q.places.size) return
                val pl = q.places[seekIdx]
                if (Math.abs(cx(pl.x) - hx) < 13f && Math.abs(fy(pl.y) - hy) < 13f) {
                    found(pl.found)
                    seekIdx++
                    wrongNear.clear()
                    if (seekIdx >= q.places.size) {
                        netT = 1.2f; netX = hiderX; netY = hiderY
                        g.audio.sfx("stamp")
                        touching = false
                        needRelease = true
                        after(1.2f) { completeStep() }
                    } else {
                        g.audio.sfx("ok")
                        val nx = q.places[seekIdx]
                        hiderToX = cx(nx.x); hiderToY = fy(nx.y)
                        touching = false
                        needRelease = true
                        after(0.9f) { announce() }
                    }
                    return
                }
                for (i in q.places.indices) {
                    if (i == seekIdx) continue
                    val o = q.places[i]
                    val near = Math.abs(cx(o.x) - hx) < 11f && Math.abs(fy(o.y) - hy) < 11f
                    if (near) {
                        if (wrongNear.add(i)) { g.audio.sfx("no"); open(q.wrong ?: pl.line, "Not there. Listen again.", null) { announce() } }
                    } else wrongNear.remove(i)
                }
            }
        }
    }

    /** How many she must bring: the count in the file, or one for each thing to pick up. */
    private fun carryGoal(q: QuestStep): Int = if (q.count > 0) q.count else if (q.to.isNotEmpty()) q.to.size else q.items.size

    private fun deliver(q: QuestStep) {
        carrying = false
        soft = 0f
        delivered++
        if (delivered >= carryGoal(q)) completeStep()
        else { g.audio.sfx("ok"); showToast("$delivered / ${carryGoal(q)}") }
    }

    // ---------- buttons ----------

    private fun rebuild() {
        buttons.clear()
        if (dialogOpen) {
            val ln = dLine
            if (ln != null) add(Button("again", 16f, 206f, 30f, 24f, "", Style.DARK) { g.say(ln) }).icon = "speaker"
            val ok = add(Button("ok", 312f, 202f, 72f, 30f, "OK", Style.PRIMARY) { closeDialog() })
            ok.font = Font.DISPLAY
            ok.size = 15f
            return
        }
        if (menu) {
            add(Button("resume", 120f, 70f, 160f, 32f, "Keep going", Style.PRIMARY) { menu = false; rebuild() })
            add(Button("tostar", 120f, 110f, 160f, 32f, "Back to the last star", Style.PLAIN) {
                menu = false
                val parts = star.split(',')
                if (parts.size == 2) place(parts[0].toIntOrNull() ?: a.startX, parts[1].toIntOrNull() ?: a.startY)
                else place(a.startX, a.startY)
                snapCamera()
                rebuild()
            })
            add(Button("leave", 120f, 150f, 160f, 32f, "Stop for now", Style.PLAIN) { onExit() })
            return
        }
        if (tip || ticket) return
        add(Button("menu", 4f, 4f, 30f, 24f, "", Style.DARK) { menu = true; touching = false; rebuild() }).icon = "menu"
        if (a.ticket && g.unit.ticket != null) {
            add(Button("ticket", 330f, 4f, 32f, 24f, "", Style.PLAIN) { ticket = true; touching = false; rebuild() }).icon = "ticket"
        }
        add(Button("book", 366f, 4f, 30f, 24f, "", Style.DARK) { touching = false; g.openOverlay(NotebookScreen(g)) }).icon = "book"
        val pd = a.pardon
        if (pd != null) {
            val b = add(Button("pardon", 300f, 200f, 94f, 34f, pd.fr, Style.PRIMARY) { pardon() })
            b.font = Font.DISPLAY
            b.size = 15f
        }
    }

    /** The how-to-play card goes away, and the first step of the quest begins. */
    private fun dismissTip() {
        tip = false
        needRelease = true
        if (!begun) { begun = true; beginStep(0) }
        rebuild()
    }

    private fun closeDialog() {
        dialogOpen = false
        showEnglish = false
        val then = afterDialog
        afterDialog = null
        rebuild()
        if (then != null) then()
    }

    private fun pardon() {
        val pd = a.pardon ?: return
        touching = false
        sayT = 1.2f
        sayText = pd.fr
        g.say(pd)
        for (b in blockers) {
            if (Math.abs(cx(b.def.x) - hx) < 44f && Math.abs(fy(b.def.y) - hy) < 44f) b.asideT = 4.5f
        }
    }

    override fun resume() { touching = false }

    // ---------- touch and keys ----------

    private fun paused() = tip || dialogOpen || menu || ticket || finished

    override fun tapDown(x: Float, y: Float) {
        if (tip) { dismissTip(); return }
        if (ticket) { ticket = false; needRelease = true; rebuild(); return }
        if (dialogOpen) { if (y > 150f) showEnglish = !showEnglish; return }
        if (paused()) return
        needRelease = false
        touching = true
        tx = camX + x / zoom
        ty = camY + y / zoom + 5f
    }

    override fun tapMove(x: Float, y: Float) {
        if (touching) { tx = camX + x / zoom; ty = camY + y / zoom + 5f }
    }

    override fun tapUp(x: Float, y: Float) { touching = false; needRelease = false }

    override fun keys(dx: Int, dy: Int) {
        kx = dx
        ky = dy
        if (tip && (dx != 0 || dy != 0)) dismissTip()
    }

    // ---------- the world ----------

    private fun blockerAt(tileX: Int, tileY: Int): Boolean {
        for (b in blockers) if (b.def.x == tileX && b.def.y == tileY && b.asideT <= 0f) return true
        return false
    }

    /** Walls and furniture only. */
    private fun wall(tileX: Int, tileY: Int): Boolean {
        if (tileX < 0 || tileY < 0 || tileX >= mw || tileY >= mh) return true
        val c = tiles[tileY][tileX]
        if (c == '.' || c == '*') return false
        if (c == ' ') return true
        val d = ts.defs[c] ?: return true
        return d.solid
    }

    fun solid(tileX: Int, tileY: Int): Boolean =
        wall(tileX, tileY) || npcTiles.contains(tileY * mw + tileX) || blockerAt(tileX, tileY)

    private fun blocked(x: Float, y: Float): Boolean {
        val l = Math.floor(((x - HALF) / T).toDouble()).toInt()
        val r = Math.floor(((x + HALF) / T).toDouble()).toInt()
        val top = Math.floor(((y - FEET) / T).toDouble()).toInt()
        val bot = Math.floor((y / T).toDouble()).toInt()
        return solid(l, top) || solid(r, top) || solid(l, bot) || solid(r, bot)
    }

    override fun update(dt: Float) {
        super.update(dt)
        if (toastT > 0f) toastT -= dt
        if (netT > 0f) netT -= dt
        // the view follows her; while someone is speaking it slides up so the panel hides nobody
        val k = Math.min(1f, dt * 8f)
        val lift = if (dialogOpen) 34f else 0f
        camX += (clampCamX(hx - viewW / 2f) - camX) * k
        camY += (clampCamY(hy - viewH / 2f - 6f + lift, lift) - camY) * k
        if (paused()) return
        if (safeT > 0f) safeT -= dt
        if (sayT > 0f) sayT -= dt
        for (b in blockers) {
            if (b.asideT > 0f) {
                b.asideT -= dt
                // do not step back onto her
                if (b.asideT <= 0f && Math.abs(cx(b.def.x) - hx) < 13f && Math.abs(fy(b.def.y) - hy) < 13f) b.asideT = 0.5f
            }
        }
        updateMovers(dt)
        updateRunners(dt)
        updateFollowers(dt)
        updateWatchers(dt)
        updateHider(dt)
        updateHero(dt)
        checkBumps()
        checkPlaces()
    }

    private fun updateMovers(dt: Float) {
        for (m in movers) {
            if (m.sayT > 0f) m.sayT -= dt
            val path = m.def.path
            val gx = cx(path[m.to][0])
            val gy = fy(path[m.to][1])
            val dx = gx - m.x
            val dy = gy - m.y
            val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            val stepLen = m.def.speed * dt
            if (d <= stepLen) {
                m.x = gx
                m.y = gy
                if (m.fwd) { if (m.to >= path.size - 1) { m.fwd = false; m.to-- } else m.to++ }
                else { if (m.to <= 0) { m.fwd = true; m.to++ } else m.to-- }
            } else {
                m.x += dx / d * stepLen
                m.y += dy / d * stepLen
                if (Math.abs(dx) > 0.5f) m.flip = dx < 0f
            }
        }
    }

    private fun updateRunners(dt: Float) {
        for (r in runners) {
            r.moving = false
            if (r.caught || r.def.path.size < 2 || questIndex < r.def.from) continue
            val path = r.def.path
            if (r.def.mode == "lead") {
                // he keeps ahead: he only walks on while she is close behind
                val q = step()
                if (r.atEnd || q == null || q.type != "lead" || q.runner != r.def.id) continue
                val dx0 = r.x - hx
                val dy0 = r.y - hy
                if (dx0 * dx0 + dy0 * dy0 > r.def.near * r.def.near) continue
            }
            val gx = cx(path[r.to][0])
            val gy = fy(path[r.to][1])
            val dx = gx - r.x
            val dy = gy - r.y
            val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            val stepLen = r.def.speed * dt
            r.moving = true
            if (d <= stepLen) {
                r.x = gx
                r.y = gy
                if (r.to >= path.size - 1) {
                    if (r.def.mode == "lead") r.atEnd = true else r.to = 0
                } else r.to++
            } else {
                r.x += dx / d * stepLen
                r.y += dy / d * stepLen
                if (Math.abs(dx) > 0.5f) r.flip = dx < 0f
            }
        }
    }

    /** The next tile on the shortest way from one tile to another, walking round walls. */
    private fun nextTileToward(fromX: Int, fromY: Int, toX: Int, toY: Int): IntArray? {
        if (fromX == toX && fromY == toY) return null
        val prev = IntArray(mw * mh) { -2 }
        val queue = IntArray(mw * mh)
        var head = 0
        var tail = 0
        // search backwards from the goal so the first step is read straight off
        prev[toY * mw + toX] = -1
        queue[tail++] = toY * mw + toX
        val dxs = intArrayOf(1, -1, 0, 0)
        val dys = intArrayOf(0, 0, 1, -1)
        while (head < tail) {
            val cur = queue[head++]
            val cxx = cur % mw
            val cyy = cur / mw
            if (cxx == fromX && cyy == fromY) {
                val nx = prev[cur]
                return if (nx < 0) null else intArrayOf(nx % mw, nx / mw)
            }
            for (i in 0 until 4) {
                val nx = cxx + dxs[i]
                val ny = cyy + dys[i]
                if (nx < 0 || ny < 0 || nx >= mw || ny >= mh) continue
                val idx = ny * mw + nx
                if (prev[idx] != -2 || wall(nx, ny)) continue
                prev[idx] = cur
                queue[tail++] = idx
            }
        }
        return null
    }

    private fun updateFollowers(dt: Float) {
        val heroTx = Math.floor((hx / T).toDouble()).toInt()
        val heroTy = Math.floor(((hy - 3f) / T).toDouble()).toInt()
        for (f in followers) {
            f.moving = false
            if (f.sayT > 0f) f.sayT -= dt
            if (questIndex < f.def.from || questIndex >= a.quest.size) continue
            if (f.pauseT > 0f) { f.pauseT -= dt; continue }
            f.repathT -= dt
            val atNext = Math.abs(f.x - f.nextX) < 1f && Math.abs(f.y - f.nextY) < 1f
            if (f.repathT <= 0f || atNext) {
                f.repathT = 0.5f
                val ftx = Math.floor((f.x / T).toDouble()).toInt()
                val fty = Math.floor(((f.y - 3f) / T).toDouble()).toInt()
                val nt = nextTileToward(ftx, fty, heroTx, heroTy)
                if (nt != null) { f.nextX = cx(nt[0]); f.nextY = fy(nt[1]) }
            }
            val dx = f.nextX - f.x
            val dy = f.nextY - f.y
            val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            val stepLen = f.def.speed * dt
            if (d > 0.5f) {
                f.moving = true
                if (d <= stepLen) { f.x = f.nextX; f.y = f.nextY }
                else { f.x += dx / d * stepLen; f.y += dy / d * stepLen }
                if (Math.abs(dx) > 0.5f) f.flip = dx < 0f
            }
        }
    }

    private fun updateWatchers(dt: Float) {
        for (w in watchers) {
            w.moving = false
            if (w.sayT > 0f) w.sayT -= dt
            val path = w.def.path
            if (path.size < 2) continue
            if (w.pauseT > 0f) {
                w.pauseT -= dt
                // halfway through a pause, turn to face the next stretch
                if (w.pauseT < w.def.pause / 2f) {
                    val dx = cx(path[w.to][0]) - w.x
                    val dy = fy(path[w.to][1]) - w.y
                    val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    if (d > 0.5f) { w.fx = dx / d; w.fy = dy / d }
                }
                continue
            }
            val gx = cx(path[w.to][0])
            val gy = fy(path[w.to][1])
            val dx = gx - w.x
            val dy = gy - w.y
            val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            val stepLen = w.def.speed * dt
            if (d <= stepLen) {
                w.x = gx; w.y = gy
                if (w.fwd) { if (w.to >= path.size - 1) { w.fwd = false; w.to-- } else w.to++ }
                else { if (w.to <= 0) { w.fwd = true; w.to++ } else w.to-- }
                w.pauseT = w.def.pause
            } else {
                w.moving = true
                w.x += dx / d * stepLen
                w.y += dy / d * stepLen
                w.fx = dx / d; w.fy = dy / d
            }
        }
    }

    /** True if a wall stands between two points in the world. */
    private fun wallBetween(x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
        val n = 12
        for (i in 1 until n) {
            val x = x0 + (x1 - x0) * i / n
            val y = y0 + (y1 - y0) * i / n
            if (wall(Math.floor((x / T).toDouble()).toInt(), Math.floor(((y - 3f) / T).toDouble()).toInt())) return true
        }
        return false
    }

    /** Is the detective inside this watcher's beam? */
    private fun sees(w: Wt): Boolean {
        val dx = hx - w.x
        val dy = hy - w.y
        val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (d > w.def.range || d < 1f) return d < 1f
        val cos = (dx * w.fx + dy * w.fy) / d
        return cos > 0.80f && !wallBetween(w.x, w.y - 4f, hx, hy - 4f)
    }

    private fun updateHider(dt: Float) {
        val dx = hiderToX - hiderX
        val dy = hiderToY - hiderY
        val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (d < 0.5f) return
        val stepLen = 110f * dt
        if (d <= stepLen) { hiderX = hiderToX; hiderY = hiderToY }
        else { hiderX += dx / d * stepLen; hiderY += dy / d * stepLen }
    }

    private fun updateHero(dt: Float) {
        if (slideT >= 0f) {
            slideT += dt / 0.4f
            val f = Math.min(1f, slideT)
            hx = slideFromX + (slideToX - slideFromX) * f
            hy = slideFromY + (slideToY - slideFromY) * f
            if (slideT >= 1f) slideT = -1f
            moving = false
            return
        }
        // cheese gets soft in the sun and firm again in the shade, and soft cheese is slow to roll
        val q0 = step()
        if (carrying && q0 != null && q0.melt) {
            soft = if (inShade()) Math.max(0f, soft - dt * 0.45f) else Math.min(1f, soft + dt * 0.07f)
        }
        var vx = 0f
        var vy = 0f
        if (touching && !needRelease) {
            val dx = tx - hx
            val dy = ty - hy
            val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (d > 3f) { vx = dx / d; vy = dy / d }
        } else if (kx != 0 || ky != 0) {
            val d = Math.sqrt((kx * kx + ky * ky).toDouble()).toFloat()
            vx = kx / d
            vy = ky / d
        }
        moving = vx != 0f || vy != 0f
        pushX = vx
        pushY = vy
        if (!moving) { walkT = 0f; return }
        val stepLen = SPEED * dt * (1f - 0.55f * soft)
        val ox = hx
        val oy = hy
        if (!blocked(hx + vx * stepLen, hy)) hx += vx * stepLen
        if (!blocked(hx, hy + vy * stepLen)) hy += vy * stepLen
        dir = if (Math.abs(vx) > Math.abs(vy)) (if (vx < 0f) 2 else 3) else (if (vy < 0f) 1 else 0)
        if (hx != ox || hy != oy) {
            walkT += dt
            val n = trail.size
            if (n < 2 || Math.abs(trail[n - 2] - hx) + Math.abs(trail[n - 1] - hy) >= 4f) {
                trail.add(hx)
                trail.add(hy)
                if (trail.size > 160) { trail.removeAt(0); trail.removeAt(0) }
            }
        } else walkT = 0f
    }

    fun inShade(): Boolean {
        val tx0 = Math.floor((hx / T).toDouble()).toInt()
        val ty0 = Math.floor(((hy - 3f) / T).toDouble()).toInt()
        if (tx0 < 0 || ty0 < 0 || tx0 >= mw || ty0 >= mh) return false
        return ts.defs[tiles[ty0][tx0]]?.shade == true
    }

    /** Seen by a guard: he calls out, and she goes back to the last star. */
    private fun caught(w: Wt) {
        caughtTimes++
        g.audio.sfx("no")
        w.sayT = 2.4f
        val ln = w.def.line
        if (ln != null) g.say(ln)
        val parts = star.split(',')
        val sx0 = if (parts.size == 2) parts[0].toIntOrNull() ?: a.startX else a.startX
        val sy0 = if (parts.size == 2) parts[1].toIntOrNull() ?: a.startY else a.startY
        slideFromX = hx; slideFromY = hy
        slideToX = cx(sx0); slideToY = fy(sy0)
        slideT = 0f
        trail.clear()
        safeT = 2.2f
        needRelease = true
        touching = false
        showToast(if (star.isEmpty()) "Seen! Back to the start." else "Seen! Back to the last star.")
    }

    private fun checkBumps() {
        if (safeT > 0f || slideT >= 0f) return
        for (w in watchers) if (sees(w)) { caught(w); return }
        for (m in movers) {
            val wide = if (m.def.look == "cleaner") 14f else 10f
            if (Math.abs(m.x - hx) < wide && Math.abs(m.y - hy) < 9f) {
                bump()
                val ln = m.def.line
                if (ln != null && !m.spoke) {
                    m.spoke = true
                    m.sayT = 2.2f
                    after(0.9f) { g.say(ln) }
                    found(m.def.found)
                } else if (ln != null) m.sayT = 1.6f
                return
            }
        }
        for (f in followers) {
            if (questIndex < f.def.from || f.pauseT > 0f) continue
            if (Math.abs(f.x - hx) < 10f && Math.abs(f.y - hy) < 9f) {
                bump()
                f.pauseT = 3.2f
                f.sayT = 2f
                val ln = f.def.line
                if (ln != null) after(0.9f) { g.say(ln) }
                return
            }
        }
    }

    /** Something has walked into her: she says sorry and goes back about three tiles the way she came. */
    private fun bump() {
        bumps++
        g.audio.sfx("bump")
        val bl = a.bump
        if (bl != null) { sayT = 1.3f; sayText = bl.fr; g.say(bl) }
        val back = 12
        val n = trail.size / 2
        slideFromX = hx
        slideFromY = hy
        if (n > 0) {
            val i = Math.max(0, n - 1 - back)
            slideToX = trail[i * 2]
            slideToY = trail[i * 2 + 1]
            while (trail.size > (i + 1) * 2) trail.removeAt(trail.size - 1)
        } else { slideToX = hx; slideToY = hy }
        slideT = 0f
        safeT = 1.8f
        needRelease = true
    }

    private fun found(id: String?) {
        if (id == null) return
        val w = g.findWord(id) ?: return
        if (g.save.find(id)) showToast("New word in your notebook: ${w.fr} = ${w.en}")
    }

    private fun showToast(s: String) { toast = s; toastT = 4f }

    private fun checkPlaces() {
        val tileX = Math.floor((hx / T).toDouble()).toInt()
        val tileY = Math.floor(((hy - 3f) / T).toDouble()).toInt()

        // stars
        if (tileY in 0 until mh && tileX in 0 until mw && tiles[tileY][tileX] == '*') {
            val key = "$tileX,$tileY"
            if (key != star) {
                star = key
                g.save.checkpoints[saveKey] = key
                g.save.commit()
                g.audio.sfx("ok")
                showToast("Star! If you stop now, you start again from here.")
            }
        }

        checkQuest(tileX, tileY)
        if (paused()) return

        // people: walk into them to talk
        for (n in a.npcs) {
            // someone behind a counter is reached across it
            val reach = if (n.y + 1 < mh && isCounter(n.x, n.y + 1)) n.y + 1 else n.y
            val dx = cx(n.x) - hx
            val dy = fy(reach) - hy
            val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (d < 21f && moving && d > 0.1f && (pushX * dx + pushY * dy) / d > 0.6f) {
                if (nearNpc.add(n.id)) { meet(n); return }
            } else if (d > 30f) nearNpc.remove(n.id)
        }

        // places on the wall: walk up into them
        for ((i, s) in a.spots.withIndex()) {
            val inside = tileY == s.y && tileX >= s.x && tileX < s.x + s.w
            if (inside && moving && pushY < -0.5f && hy - FEET - (s.y * T) < 2.5f) {
                if (nearSpot.add(i)) { open(s.line, s.narr, null, null); found(s.found); return }
            } else if (!inside) nearSpot.remove(i)
        }
    }

    private fun isCounter(tileX: Int, tileY: Int): Boolean {
        val c = tiles[tileY][tileX]
        return c == 'C'
    }

    private fun meet(n: Npc) {
        val q = step()
        if (q != null && q.type == "carry" && q.to.isNotEmpty()) {
            val wants = q.to.contains(n.id) && !fed.contains(n.id)
            if (carrying && wants) {
                fed.add(n.id)
                touching = false
                needRelease = true
                deliver(q)
                return
            }
            // fed already: the step's "wrong" line is what a full mouse says
            if (fed.contains(n.id) && q.wrong != null) { open(q.wrong, null, null, null); return }
        }
        val talkId = n.talk
        if (talkId != null && q != null && q.type == "talk" && q.npc == n.id) {
            touching = false
            g.go(TalkScreen(g, talkId, onExit) {
                g.go(this)
                completeStep()
            })
            return
        }
        open(n.line, n.narr, null, null)
        found(n.found)
    }

    private fun open(ln: Line?, narr: String?, icon: String?, then: (() -> Unit)?) {
        if (ln == null && narr == null) { if (then != null) then(); return }
        dLine = ln
        dNarr = narr
        dIcon = icon
        afterDialog = then
        dialogOpen = true
        showEnglish = false
        touching = false
        needRelease = true
        if (ln != null) g.say(ln)
        rebuild()
    }

    // ---------- drawing ----------

    private fun sx(wx: Float) = (wx - camX) * zoom
    private fun sy(wy: Float) = (wy - camY) * zoom

    private fun sprite(p: Painter, img: Img, wx: Float, feetY: Float, flip: Boolean, alpha: Float) {
        p.image(img, sx(wx - img.w / 2f), sy(feetY + 2f - img.h), img.w * zoom, img.h * zoom, flip, alpha)
    }

    private fun shadow(p: Painter, wx: Float, feetY: Float, w: Float) {
        p.fillOval(sx(wx - w / 2f), sy(feetY - 1f), w * zoom, 4f * zoom, C.alpha(C.BLACK, 0.2f))
    }

    override fun draw(p: Painter) {
        p.fillRect(0f, 0f, Game.W, Game.H, ts.void)
        val x0 = Math.max(0, Math.floor((camX / T).toDouble()).toInt())
        val y0 = Math.max(0, Math.floor((camY / T).toDouble()).toInt())
        val x1 = Math.min(mw - 1, x0 + (viewW / T).toInt() + 1)
        val y1 = Math.min(mh - 1, y0 + (viewH / T).toInt() + 1)
        val tsz = T * zoom + 0.4f
        val floor = g.art.get(ts.floor)
        for (r in y0..y1) for (c in x0..x1) {
            val ch = tiles[r][c]
            if (ch == ' ') continue
            val px = sx(c * T)
            val py = sy(r * T)
            if (ch == '.') { p.image(floor, px, py, tsz, tsz, false, 1f); continue }
            if (ch == '*') {
                p.image(floor, px, py, tsz, tsz, false, 1f)
                p.image(g.art.get(if (star == "$c,$r") "tile_star_on" else "tile_star"), px, py, tsz, tsz, false, 1f)
                continue
            }
            val def = ts.defs[ch] ?: continue
            if (def.under) p.image(floor, px, py, tsz, tsz, false, 1f)
            p.image(g.art.get(def.img), px, py, tsz, tsz, false, 1f)
        }

        // signs on the walls
        for (s in a.signs) drawSign(p, s.x * T + T / 2f, s.y * T + T / 2f, s.text, s.dir)
        for (tm in a.totems) {
            val bx = tm.x * T + T / 2f
            var by = tm.y * T - tm.texts.size * 9f + 2f
            for (i in tm.texts.indices) { drawSign(p, bx, by, tm.texts[i], tm.dirs[i], 62f); by += 9f }
        }

        // things to pick up, and the place she is looking for
        val q = step()
        if (q != null && q.type == "collect") {
            val img = g.art.get(if (q.img.isEmpty()) "item_page" else q.img)
            val bob = Math.sin(t * 5.0).toFloat()
            for (i in q.items.indices) {
                if (picked.contains(i)) continue
                val it = q.items[i]
                p.image(img, sx(cx(it[0]) - img.w / 2f), sy(fy(it[1]) - img.h - 1f + bob), img.w * zoom, img.h * zoom, false, 1f)
            }
        }
        if (q != null && q.type == "carry") {
            val img = g.art.get(if (q.img.isEmpty()) "item_page" else q.img)
            for (i in q.items.indices) {
                if (picked.contains(i)) continue
                val it = q.items[i]
                p.image(img, sx(cx(it[0]) - img.w / 2f), sy(fy(it[1]) - img.h), img.w * zoom, img.h * zoom, false, 1f)
            }
            if (q.to.isEmpty() && q.x >= 0) {
                val bob = Math.sin(t * 6.0).toFloat() * 2f
                Draw.arrow(p, sx(cx(q.x) + (q.w - 1) * T / 2f), sy(q.y * T + 26f + bob), 12f * zoom, "up", C.MUSTARD)
            }
        }
        if (q != null && (q.type == "reach" || q.type == "get") && q.x >= 0) {
            val bob = Math.sin(t * 6.0).toFloat() * 2f
            // "img": "arrow" marks the place with a bouncing arrow; any other name is an icon lying there
            if (q.img == "arrow") Draw.arrow(p, sx(cx(q.x) + (q.w - 1) * T / 2f), sy(q.y * T + 26f + bob), 12f * zoom, "up", C.MUSTARD)
            else if (q.img.isNotEmpty()) Icons.draw(p, q.img, sx(cx(q.x) - 7f), sy(q.y * T + 1f + bob * 0.5f), 14f * zoom)
        }

        // people and things, furthest first
        class Ent(val y: Float, val draw: () -> Unit)
        val ents = ArrayList<Ent>()
        val frame = if ((t * 6f).toInt() % 2 == 0) 0 else 1
        for (n in a.npcs) {
            val img = Looks.img(g, n.look, 0)
            ents.add(Ent(fy(n.y)) { sprite(p, img, cx(n.x), fy(n.y), false, 1f) })
        }
        for (b in blockers) {
            val img = Looks.img(g, b.def.look, 0)
            val aside = b.asideT > 0f
            ents.add(Ent(fy(b.def.y)) {
                sprite(p, img, cx(b.def.x) + (if (aside) 11f else 0f), fy(b.def.y) - (if (aside) 5f else 0f), false, if (aside) 0.55f else 1f)
            })
        }
        for (m in movers) {
            val img = Looks.img(g, m.def.look, frame + 1)
            ents.add(Ent(m.y) {
                shadow(p, m.x, m.y, img.w * 0.8f)
                sprite(p, img, m.x, m.y, m.flip, 1f)
            })
        }
        for (r in runners) {
            val img = Looks.img(g, r.def.look, if (r.moving) frame + 1 else 0)
            ents.add(Ent(r.y) {
                shadow(p, r.x, r.y, 12f)
                sprite(p, img, r.x, r.y, r.flip, 1f)
            })
        }
        for (w in watchers) {
            val img = Looks.img(g, w.def.look, if (w.moving) frame + 1 else 0)
            ents.add(Ent(w.y) {
                shadow(p, w.x, w.y, 12f)
                sprite(p, img, w.x, w.y, w.fx < -0.3f, 1f)
            })
        }
        for (f in followers) {
            val img = Looks.img(g, f.def.look, if (f.moving) frame + 1 else 0)
            val dim = f.pauseT > 0f && (t * 8f).toInt() % 2 == 0
            ents.add(Ent(f.y) {
                shadow(p, f.x, f.y, 12f)
                sprite(p, img, f.x, f.y, f.flip, if (dim) 0.6f else 1f)
            })
        }
        if (q != null && q.type == "seek" && seekIdx < q.places.size) {
            // the hidden thing peeps out only while it runs from one place to the next
            val running = Math.abs(hiderToX - hiderX) + Math.abs(hiderToY - hiderY) > 1f
            if (running || netT > 0f) {
                val img = Looks.img(g, if (q.img.isEmpty()) "mouse" else q.img, frame)
                ents.add(Ent(hiderY) { sprite(p, img, hiderX, hiderY, hiderToX < hiderX, 1f) })
            }
        }
        run {
            val st = if (!moving || walkT == 0f) 0 else (walkT * 8f).toInt() % 4
            val img = when (dir) {
                0 -> Looks.hero(g, 0, intArrayOf(1, 0, 2, 0)[st])
                1 -> Looks.hero(g, 1, intArrayOf(1, 0, 2, 0)[st])
                else -> Looks.heroSide(g, if (st % 2 == 0 && moving && walkT > 0f) 1 else 0)
            }
            val blink = safeT > 0f && (t * 12f).toInt() % 2 == 0
            ents.add(Ent(hy) {
                shadow(p, hx, hy, 12f)
                sprite(p, img, hx, hy, dir == 2, if (blink) 0.45f else 1f)
                if (inventory.contains("net")) Icons.draw(p, "net", sx(hx + 3f), sy(hy - 24f), 12f * zoom)
                val cq = step()
                if (carrying && cq != null) {
                    // what she carries, held in front; soft cheese sags and drips
                    val ci = g.art.get(if (cq.img.isEmpty()) "item_page" else cq.img)
                    val sq = 1f + 0.35f * soft
                    val w = ci.w * sq
                    val h = ci.h / sq
                    p.image(ci, sx(hx - w / 2f), sy(hy - 7f - h), w * zoom, h * zoom, false, 1f)
                    if (soft > 0.3f) {
                        val dropY = (t * 18f) % 6f
                        p.fillOval(sx(hx + 2f) , sy(hy - 7f + dropY), 2f * zoom, 2.5f * zoom, 0xFFF3E3A6.toInt())
                    }
                }
            })
        }
        ents.sortBy { it.y }
        for (e in ents) e.draw()

        if (netT > 0f) {
            val s = (20f + (1.2f - netT) * 6f) * zoom
            Icons.draw(p, "net", sx(netX) - s / 2f, sy(netY - 12f) - s / 2f, s)
        }

        // darkness, with a pool of torchlight round her
        if (a.darkFar > 0f) {
            for (r in y0..y1) for (c in x0..x1) {
                val dx = c * T + T / 2f - hx
                val dy = r * T + T / 2f - (hy - 6f)
                val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val k = ((d - a.darkNear) / (a.darkFar - a.darkNear)).coerceIn(0f, 1f)
                if (k > 0.02f) p.fillRect(sx(c * T), sy(r * T), tsz, tsz, C.alpha(0xFF05070F.toInt(), k * 0.94f))
            }
        }

        // guards' beams, drawn over any darkness so they show
        for (w in watchers) drawBeam(p, w)
        if (a.darkFar > 0f) for (w in watchers) {
            val img = Looks.img(g, w.def.look, if (w.moving) frame + 1 else 0)
            sprite(p, img, w.x, w.y, w.fx < -0.3f, 1f)
        }

        // how soft the cheese is
        val mq = step()
        if (carrying && mq != null && mq.melt) {
            p.fillRoundRect(150f, 212f, 100f, 18f, 9f, C.alpha(C.INK, 0.85f))
            p.fillRoundRect(154f, 216f, 92f * soft + 0.1f, 10f, 5f, if (soft > 0.6f) C.RED else C.MUSTARD)
            Text.line(p, if (inShade()) "Shade: firming up" else "Sun: getting soft", 200f, 202f, 9f, C.WHITE, Font.BOLD, Align.CENTER)
        }

        // little speech bubbles
        if (sayT > 0f) smallBubble(p, sx(hx), sy(hy - 22f), sayText)
        for (w in watchers) if (w.sayT > 0f) w.def.line?.let { smallBubble(p, sx(w.x), sy(w.y - 22f), it.fr) }
        for (m in movers) if (m.sayT > 0f) m.def.line?.let { smallBubble(p, sx(m.x), sy(m.y - 22f), it.fr) }
        for (f in followers) if (f.sayT > 0f) f.def.line?.let { smallBubble(p, sx(f.x), sy(f.y - 22f), it.fr) }

        if (!paused()) drawSignReader(p)

        // what to do now
        if (hint.isNotEmpty() && !dialogOpen && !menu && !tip && !ticket) {
            val right = if (a.ticket) 326f else 362f
            val w = Math.min(right - 40f, p.textWidth(hint, 9.5f, Font.BOLD) + 16f)
            val lines = Text.wrap(p, hint, 9.5f, Font.BOLD, right - 56f)
            val h = if (lines.size > 1) 26f else 16f
            val bx = 38f + (right - 40f - w) / 2f
            p.fillRoundRect(bx, 5f, w, h, 6f, C.alpha(C.INK, 0.88f))
            var ly = 5f + h / 2f - (lines.size - 1) * 5.6f
            for (s in lines) { Text.line(p, s, bx + w / 2f, ly, 9.5f, C.WHITE, Font.BOLD, Align.CENTER); ly += 11.2f }
        }

        val tst = toast
        if (tst != null && toastT > 0f && !dialogOpen && !tip) {
            val a2 = Math.min(1f, toastT * 2f)
            val maxW = if (a.pardon != null) 280f else 360f
            val w = Math.min(maxW, p.textWidth(tst, 10f, Font.BOLD) + 24f)
            val cxp = if (a.pardon != null) 150f else 200f
            p.fillRoundRect(cxp - w / 2f, 212f, w, 20f, 10f, C.alpha(C.INK, 0.92f * a2))
            var size = 10f
            while (size > 7f && p.textWidth(tst, size, Font.BOLD) > w - 14f) size -= 0.5f
            Text.line(p, tst, cxp, 222f, size, C.alpha(C.WHITE, a2), Font.BOLD, Align.CENTER)
        }

        if (dialogOpen) drawDialog(p)
        if (menu) {
            p.fillRect(0f, 0f, Game.W, Game.H, C.alpha(C.NIGHT, 0.7f))
            Draw.card(p, 100f, 30f, 200f, 166f, C.PAPER)
            Text.line(p, "Paused", 200f, 50f, 18f, C.INK, Font.DISPLAY, Align.CENTER)
        }
        val tk = g.unit.ticket
        if (ticket && tk != null) {
            p.fillRect(0f, 0f, Game.W, Game.H, C.alpha(C.NIGHT, 0.7f))
            TicketView.draw(p, tk, 60f, 60f, 280f, 100f)
            Text.line(p, "Tap to put it away", 200f, 184f, 11f, C.WHITE, Font.TEXT, Align.CENTER)
        }
        if (tip) {
            p.fillRect(0f, 0f, Game.W, Game.H, C.alpha(C.NIGHT, 0.6f))
            Draw.card(p, 70f, 62f, 260f, 116f, C.PAPER)
            val bob = Math.sin(t * 4.0).toFloat() * 3f
            Icons.draw(p, "hand", 84f, 80f + bob, 46f)
            Text.block(p, a.tip, 140f, 76f, 178f, 12f, C.INK, Font.BOLD, Align.LEFT, 1.3f)
            Text.line(p, "Tap to start", 200f, 166f, 10f, C.GREY, Font.TEXT, Align.CENTER)
        }
        drawButtons(p)
    }

    /** A pale fan of light in front of a guard. */
    private fun drawBeam(p: Painter, w: Wt) {
        val r = w.def.range
        val px0 = w.x
        val py0 = w.y - 4f
        val ang = Math.atan2(w.fy.toDouble(), w.fx.toDouble())
        val spread = Math.acos(0.80)
        val pts = FloatArray(2 + 2 * 9)
        pts[0] = sx(px0); pts[1] = sy(py0)
        for (i in 0 until 9) {
            val a2 = ang - spread + 2 * spread * i / 8.0
            // stop the beam at walls
            var len = r
            var k = 1
            while (k <= 8) {
                val lx = px0 + Math.cos(a2).toFloat() * r * k / 8f
                val ly = py0 + Math.sin(a2).toFloat() * r * k / 8f
                if (wall(Math.floor((lx / T).toDouble()).toInt(), Math.floor(((ly + 1f) / T).toDouble()).toInt())) { len = r * (k - 0.5f) / 8f; break }
                k++
            }
            pts[2 + i * 2] = sx(px0 + Math.cos(a2).toFloat() * len)
            pts[3 + i * 2] = sy(py0 + Math.sin(a2).toFloat() * len)
        }
        p.fillPoly(pts, C.alpha(0xFFFFE680.toInt(), 0.33f))
    }

    /** A yellow sign. [wx], [wy] is its centre in the world. */
    private fun drawSign(p: Painter, wx: Float, wy: Float, text: String, dir: String, fixedW: Float = 0f) {
        val z = zoom
        val size = 6.5f * z
        val arrow = dir.isNotEmpty()
        val tw = p.textWidth(text, size, Font.BOLD)
        val w = if (fixedW > 0f) fixedW * z else tw + 8f * z + (if (arrow) 9f * z else 0f)
        val h = 10f * z
        val cxp = sx(wx)
        val cyp = sy(wy)
        p.fillRoundRect(cxp - w / 2f, cyp - h / 2f, w, h, 2f, C.INK)
        p.fillRoundRect(cxp - w / 2f + 1f, cyp - h / 2f + 1f, w - 2f, h - 2f, 1.5f, C.MUSTARD)
        if (arrow) {
            Draw.arrow(p, cxp - w / 2f + 7f * z, cyp, 6.5f * z, dir, C.INK)
            Text.line(p, text, cxp - w / 2f + 13f * z, cyp, size, C.INK, Font.BOLD)
        } else Text.line(p, text, cxp, cyp, size, C.INK, Font.BOLD, Align.CENTER)
    }

    /** When the detective stands near a signpost, show it large at the top so it is easy to read. */
    private fun drawSignReader(p: Painter) {
        var dirs: List<String>? = null
        var texts: List<String>? = null
        for (tm in a.totems) {
            val dx = cx(tm.x) - hx
            val dy = tm.y * T + 8f - hy
            if (dx * dx + dy * dy < 46f * 46f) { dirs = tm.dirs; texts = tm.texts }
        }
        val tl = texts ?: return
        val dl = dirs ?: return
        var w = 0f
        for (s in tl) w = Math.max(w, p.textWidth(s, 14f, Font.BOLD))
        w += 44f
        val h = tl.size * 20f + 8f
        val x = 200f - w / 2f
        val y = 34f
        p.fillRoundRect(x + 1f, y + 2f, w, h, 5f, C.alpha(C.BLACK, 0.3f))
        p.fillRoundRect(x, y, w, h, 5f, C.INK)
        p.fillRoundRect(x + 1.5f, y + 1.5f, w - 3f, h - 3f, 4f, C.MUSTARD)
        for (i in tl.indices) {
            val cy = y + 14f + i * 20f
            if (dl[i].isNotEmpty()) Draw.arrow(p, x + 16f, cy, 13f, dl[i], C.INK)
            Text.line(p, tl[i], x + 30f, cy, 14f, C.INK, Font.BOLD)
        }
    }

    private fun smallBubble(p: Painter, cxp: Float, bottom: Float, text: String) {
        val w = p.textWidth(text, 9f, Font.BOLD) + 10f
        p.fillRoundRect(cxp - w / 2f, bottom - 14f, w, 14f, 5f, C.WHITE)
        p.strokeRoundRect(cxp - w / 2f, bottom - 14f, w, 14f, 5f, 0.8f, C.INK)
        p.fillPoly(floatArrayOf(cxp - 3f, bottom - 0.6f, cxp + 3f, bottom - 0.6f, cxp, bottom + 4f), C.WHITE)
        Text.line(p, text, cxp, bottom - 7f, 9f, C.INK, Font.BOLD, Align.CENTER)
    }

    private fun drawDialog(p: Painter) {
        Draw.card(p, 8f, 150f, 384f, 86f, C.WHITE)
        p.strokeRoundRect(8f, 150f, 384f, 86f, 8f, 1f, C.INK)
        var y = 158f
        val ln = dLine
        val ic = dIcon
        val textW = if (ic != null) 296f else 360f
        if (ic != null) Icons.draw(p, ic, 326f, 154f, 46f)
        if (ln != null) {
            y = Text.block(p, ln.fr, 20f, y, textW, 13.5f, C.INK, Font.BOLD, Align.LEFT, 1.22f) + 1f
            val under = g.unit.english == "under" || showEnglish
            val en = if (ln.en.isNotEmpty()) ln.en else ln.gloss.joinToString(", ")
            if (en.isNotEmpty()) {
                y = if (under) Text.block(p, en, 20f, y, textW, 10.5f, C.GREY, Font.TEXT, Align.LEFT, 1.2f) + 3f
                else Text.block(p, "tap here for help", 20f, y, textW, 9f, C.GREY_L, Font.TEXT, Align.LEFT, 1.2f) + 3f
            }
        }
        val nr = dNarr
        if (nr != null) Text.block(p, nr, if (ln != null) 54f else 20f, Math.max(y, if (ln != null) 208f else 170f), 250f, 10.5f, C.INK2, Font.TEXT, Align.LEFT, 1.2f)
    }

    override fun back(): Boolean {
        if (dialogOpen) { closeDialog(); return true }
        if (ticket) { ticket = false; rebuild(); return true }
        if (tip) { dismissTip(); return true }
        menu = !menu
        rebuild()
        return true
    }

    override fun describe(): String =
        "Walk $sceneId hero=(${(hx / T).toInt()},${((hy - 3f) / T).toInt()}) step=$questIndex/${a.quest.size} " +
            "${step()?.type ?: "-"} tip=$tip dialog=$dialogOpen bumps=$bumps star=$star"

    // ---------- for the scripted playthrough ----------

    fun isPaused(): Boolean = paused()
    fun dialogShowing(): Boolean = dialogOpen
    fun tipShowing(): Boolean = tip
    fun hasPardon(): Boolean = a.pardon != null
    fun moverSpots(): List<FloatArray> {
        val out = ArrayList<FloatArray>()
        for (m in movers) out.add(floatArrayOf(m.x, m.y))
        for (f in followers) if (questIndex >= f.def.from && f.pauseT <= 0f) out.add(floatArrayOf(f.x, f.y))
        return out
    }
    /** Points inside the guards' beams, so a careful player can keep out of them. */
    fun beamSpots(): List<FloatArray> {
        val out = ArrayList<FloatArray>()
        for (w in watchers) {
            for (k in 0..4) {
                val d = w.def.range * k / 4f
                out.add(floatArrayOf(w.x + w.fx * d, w.y + w.fy * d))
            }
            // and where he will look next, if he is about to turn
            val path = w.def.path
            if (path.size > 1) {
                val dx = cx(path[w.to][0]) - w.x
                val dy = fy(path[w.to][1]) - w.y
                val d = Math.max(0.1f, Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat())
                for (k in 1..3) out.add(floatArrayOf(w.x + dx / d * w.def.range * k / 3f, w.y + dy / d * w.def.range * k / 3f))
            }
        }
        return out
    }
    fun isCarrying(): Boolean = carrying
    fun mapWidth(): Int = mw
    fun mapHeight(): Int = mh
    /** True if only a person stands on this tile, so "Pardon !" would clear it. */
    fun blockerOn(tileX: Int, tileY: Int): Boolean = blockerAt(tileX, tileY)
    fun wallAt(tileX: Int, tileY: Int): Boolean = wall(tileX, tileY) || npcTiles.contains(tileY * mw + tileX)

    /**
     * Where a player should head now: a tile to stand on, and a direction to
     * push in once there (0, 0 for none). Null when there is nothing to do.
     */
    fun goal(): IntArray? {
        val q = step() ?: return null
        when (q.type) {
            "talk" -> {
                for (n in a.npcs) if (n.id == q.npc) {
                    val down = if (n.y + 1 < mh && isCounter(n.x, n.y + 1)) 2 else 1
                    val cands = arrayOf(intArrayOf(0, down), intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1))
                    for (c in cands) {
                        val gx = n.x + c[0]
                        val gy = n.y + c[1]
                        if (!wall(gx, gy)) return intArrayOf(gx, gy, -Integer.signum(c[0]), -Integer.signum(c[1]))
                    }
                }
                return null
            }
            "reach", "get" -> return intArrayOf(q.x, q.y, 0, 0)
            "enter" -> return intArrayOf(q.x, q.y + 1, 0, -1)
            "collect" -> {
                var best = -1
                var bestD = 1e9f
                for (i in q.items.indices) {
                    if (picked.contains(i)) continue
                    val d = Math.abs(cx(q.items[i][0]) - hx) + Math.abs(fy(q.items[i][1]) - hy)
                    if (d < bestD) { bestD = d; best = i }
                }
                return if (best < 0) null else intArrayOf(q.items[best][0], q.items[best][1], 0, 0)
            }
            "catch", "lead" -> {
                val r = runner(q.runner) ?: return null
                var gx = Math.floor((r.x / T).toDouble()).toInt()
                var gy = Math.floor(((r.y - 3f) / T).toDouble()).toInt()
                if (wall(gx, gy)) {
                    // he stands on something she cannot: aim for the open tile beside him
                    val cands = arrayOf(intArrayOf(0, 1), intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1))
                    for (c in cands) if (!wall(gx + c[0], gy + c[1])) { gx += c[0]; gy += c[1]; break }
                }
                return intArrayOf(gx, gy, 0, 0)
            }
            "seek" -> {
                if (seekIdx >= q.places.size) return null
                return intArrayOf(q.places[seekIdx].x, q.places[seekIdx].y, 0, 0)
            }
            "carry" -> {
                if (!carrying) {
                    var best = -1
                    var bestD = 1e9f
                    for (i in q.items.indices) {
                        if (picked.contains(i)) continue
                        val d = Math.abs(cx(q.items[i][0]) - hx) + Math.abs(fy(q.items[i][1]) - hy)
                        if (d < bestD) { bestD = d; best = i }
                    }
                    return if (best < 0) null else intArrayOf(q.items[best][0], q.items[best][1], 0, 0)
                }
                if (q.to.isEmpty()) return intArrayOf(q.x, q.y, 0, 0)
                for (n in a.npcs) if (q.to.contains(n.id) && !fed.contains(n.id)) {
                    val cands = arrayOf(intArrayOf(0, 1), intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1))
                    for (c in cands) {
                        val gx = n.x + c[0]
                        val gy = n.y + c[1]
                        if (!wall(gx, gy) && !npcTiles.contains(gy * mw + gx)) return intArrayOf(gx, gy, -Integer.signum(c[0]), -Integer.signum(c[1]))
                    }
                }
                return null
            }
        }
        return null
    }
    /** A wrong place to walk to in a "seek" step, to test that mistakes are handled. */
    fun wrongPlace(): IntArray? {
        val q = step() ?: return null
        if (q.type != "seek") return null
        for (i in q.places.indices) if (i != seekIdx) return intArrayOf(q.places[i].x, q.places[i].y)
        return null
    }
    fun needsItem(): Boolean {
        val q = step() ?: return false
        return q.type == "catch" && q.needs.isNotEmpty() && !inventory.contains(q.needs)
    }
}
