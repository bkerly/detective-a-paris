package detective.core

/** The whole game. The platform calls update, draw and the pointer methods; nothing else. */
class Game(val assets: Assets, val audio: AudioOut, val store: Store) {
    companion object {
        const val W = 400f
        const val H = 240f
        const val TILE = 16f
    }

    val art = Art(assets.text("art/sprites.txt") ?: "")
    val tilesets: Map<String, Tileset> = Tileset.parseAll(Json.parse(assets.text("art/tilesets.json") ?: "{}"))
    val save = Save(store)

    /** The cases and their units, in order, from assets/units/index.json. */
    val cases = ArrayList<CaseInfo>()
    val unitList = ArrayList<UnitInfo>()
    private val loaded = HashMap<String, UnitData>()

    /** The unit being played. */
    lateinit var unit: UnitData
        private set

    val voice = Voice(this)
    val rng = Rng(20261006L)
    /** The microphone. The Android app sets its own; without one, she says a phrase aloud and taps. */
    var ears: Ears = NoEars

    lateinit var screen: Screen
        private set
    var overlay: Screen? = null
    var time = 0f
    private var fade = 0f

    init {
        val idx = Json.parse(assets.text("units/index.json") ?: throw IllegalStateException("units/index.json is missing"))
        var n = 0
        for (c in idx.list("cases")) {
            val us = ArrayList<UnitInfo>()
            for (u in c.list("units")) {
                n++
                val info = UnitInfo(u.str("id"), u.str("title"), n, c.str("id"))
                us.add(info)
                unitList.add(info)
            }
            cases.add(CaseInfo(c.str("id"), c.str("title"), us))
        }
        if (unitList.isEmpty()) throw IllegalStateException("units/index.json lists no units")
        save.load()
        var start = unitList[0].id
        for (u in unitList) if (u.id == save.current) start = u.id
        unit = unitById(start)
        screen = TitleScreen(this)
        screen.enter()
    }

    fun unitById(id: String): UnitData {
        loaded[id]?.let { return it }
        val src = assets.text("units/$id.json") ?: throw IllegalStateException("units/$id.json is missing")
        val u = try { UnitData(Json.parse(src)) } catch (e: Exception) {
            throw IllegalStateException("units/$id.json: " + e.message, e)
        }
        loaded[id] = u
        return u
    }

    fun select(id: String) {
        unit = unitById(id)
        save.current = id
        save.commit()
    }

    fun indexOf(id: String): Int {
        for (i in unitList.indices) if (unitList[i].id == id) return i
        return -1
    }

    fun caseOf(id: String): CaseInfo {
        for (c in cases) for (u in c.units) if (u.id == id) return c
        return cases[0]
    }

    /** A unit opens when the one before it is finished. */
    fun unlocked(id: String): Boolean {
        val i = indexOf(id)
        if (i <= 0 || save.unlockAll) return true
        val prev = unitList[i - 1].id
        return save.stage(prev) >= unitById(prev).stages.size
    }

    fun finished(id: String): Boolean = save.stage(id) >= unitById(id).stages.size

    fun nextUnit(): UnitInfo? {
        val i = indexOf(unit.id)
        return if (i >= 0 && i + 1 < unitList.size) unitList[i + 1] else null
    }

    /** Find a word in this unit or any earlier one. */
    fun findWord(id: String): Word? {
        unit.word(id)?.let { return it }
        for (u in unitList) unitById(u.id).word(id)?.let { return it }
        return null
    }

    fun go(s: Screen) {
        audio.stopVoice()
        overlay = null
        screen = s
        fade = 1f
        s.enter()
    }

    fun openOverlay(s: Screen) {
        overlay = s
        s.enter()
    }

    fun closeOverlay() {
        overlay = null
        screen.resume()
    }

    fun update(dtIn: Float) {
        val dt = if (dtIn > 0.1f) 0.1f else dtIn
        time += dt
        if (fade > 0f) fade -= dt * 4f
        (overlay ?: screen).update(dt)
    }

    fun draw(p: Painter) {
        screen.draw(p)
        overlay?.draw(p)
        if (fade > 0f) p.fillRect(0f, 0f, W, H, C.alpha(C.NIGHT, fade))
    }

    fun down(x: Float, y: Float) { (overlay ?: screen).down(x, y) }
    fun move(x: Float, y: Float) { (overlay ?: screen).move(x, y) }
    fun up(x: Float, y: Float) { (overlay ?: screen).up(x, y) }

    /** Arrow keys on a keyboard; dx and dy are -1, 0 or 1. Only walking scenes use them. */
    fun keys(dx: Int, dy: Int) { (overlay ?: screen).keys(dx, dy) }

    /** The Android back button. Returns false when the app should close. */
    fun back(): Boolean {
        val o = overlay
        if (o != null) { if (!o.back()) closeOverlay(); return true }
        return screen.back()
    }

    /** Called when the app goes to the background. */
    fun pause() {
        audio.stopVoice()
        save.commit()
    }

    /** Put the detective's name into a sentence. */
    fun fill(s: String): String = s.replace("{name}", save.name)

    fun say(line: Line?, slow: Boolean = false): Float {
        if (line == null) return 0f
        return voice.say(line.who, line.say, slow)
    }
}

/** A small repeatable random number generator, so a test run always plays the same way. */
class Rng(seed: Long) {
    private var s = if (seed == 0L) 88172645463325252L else seed

    fun next(n: Int): Int {
        s = s xor (s shl 13)
        s = s xor (s ushr 7)
        s = s xor (s shl 17)
        val v = (s ushr 11).toInt() and 0x7FFFFFFF
        return if (n <= 0) 0 else v % n
    }

    fun <T> shuffle(list: MutableList<T>) {
        for (i in list.size - 1 downTo 1) {
            val j = next(i + 1)
            val t = list[i]
            list[i] = list[j]
            list[j] = t
        }
    }
}

/**
 * Finds the sound file for a French line. assets/audio/manifest.json maps
 * "voice|text" to a file name and its length, and is written by tools/make_audio.R.
 */
class Voice(private val g: Game) {
    private class Clip(val file: String, val ms: Int, val slowMs: Int)

    private val clips = HashMap<String, Clip>()
    /** Lines the game asked for that have no sound file. The desktop check prints these. */
    val missing = LinkedHashSet<String>()

    init {
        val src = g.assets.text("audio/manifest.json")
        if (src != null) {
            val j = Json.parse(src)["clips"]
            for (k in j.keys()) {
                val c = j[k]
                clips[k] = Clip(c.str("f"), c.int("ms", 1000), c.int("slow", 1400))
            }
        }
    }

    fun has(who: String, text: String): Boolean = clips.containsKey(g.unit.voiceFor(who) + "|" + text)

    /** True if the named voice (not the speaker) has a clip for this text. */
    fun hasVoice(voice: String, text: String): Boolean = clips.containsKey("$voice|$text")

    /** Start playing and return how long the clip lasts, in seconds. */
    fun say(who: String, text: String, slow: Boolean = false): Float {
        val key = g.unit.voiceFor(who) + "|" + text
        val c = clips[key]
        if (c == null) {
            missing.add(key)
            return 0.5f + text.length * 0.055f
        }
        g.audio.voice("audio/" + c.file + if (slow) "_slow" else "")
        return (if (slow) c.slowMs else c.ms) / 1000f
    }
}

/** What the player has done so far. Kept as one JSON string in the platform's store. */
class Save(private val store: Store) {
    var name = ""
    /** The unit last opened. */
    var current = ""
    /** Set by tapping the case-file heading five times: opens every unit, for a parent or a tester. */
    var unlockAll = false
    /** How many stages of each unit are finished, by unit id. */
    val stages = HashMap<String, Int>()
    /** Notebook pages finished, by unit id. */
    val pages = HashMap<String, Int>()
    /** Units whose opening story has been shown. */
    val intros = ArrayList<String>()
    /** Right and wrong answers by word id. A word is "met" once its card has been shown. */
    val right = HashMap<String, Int>()
    val wrong = HashMap<String, Int>()
    val met = ArrayList<String>()
    /** Bonus words found in walking scenes. */
    val found = ArrayList<String>()
    /** Tips already shown. */
    val tips = ArrayList<String>()
    /** Facts written down in conversations, e.g. "unit1/gate" = "Gate 7". */
    val facts = LinkedHashMap<String, String>()
    /** Last star touched in a walking scene, by "unit/scene", as "x,y" in tiles. */
    val checkpoints = HashMap<String, String>()

    fun stage(unitId: String): Int = stages[unitId] ?: 0

    fun setStage(unitId: String, n: Int) {
        if (n > stage(unitId)) stages[unitId] = n
        commit()
    }

    fun page(unitId: String): Int = pages[unitId] ?: 0

    fun meet(id: String) {
        if (!met.contains(id)) met.add(id)
    }

    fun mark(id: String, ok: Boolean) {
        if (ok) right[id] = (right[id] ?: 0) + 1 else wrong[id] = (wrong[id] ?: 0) + 1
    }

    /** How shaky a word is: more wrong answers and fewer right ones give a bigger number. */
    fun trouble(id: String): Int = (wrong[id] ?: 0) * 3 - (right[id] ?: 0)

    fun find(id: String): Boolean {
        if (found.contains(id)) return false
        found.add(id)
        commit()
        return true
    }

    fun reset() {
        name = ""; current = ""; unlockAll = false
        stages.clear(); pages.clear(); intros.clear(); right.clear(); wrong.clear(); met.clear()
        found.clear(); tips.clear(); facts.clear(); checkpoints.clear()
        commit()
    }

    fun load() {
        val src = store.load("save") ?: return
        val j = try { Json.parse(src) } catch (e: Exception) { return }
        name = j.str("name")
        current = j.str("current")
        unlockAll = j.bool("unlockAll")
        for (k in j["stages"].keys()) stages[k] = j["stages"].int(k)
        for (k in j["pages"].keys()) pages[k] = j["pages"].int(k)
        intros.addAll(j.strings("intros"))
        for (k in j["right"].keys()) right[k] = j["right"].int(k)
        for (k in j["wrong"].keys()) wrong[k] = j["wrong"].int(k)
        met.addAll(j.strings("met"))
        found.addAll(j.strings("found"))
        tips.addAll(j.strings("tips"))
        for (k in j["facts"].keys()) facts[k] = j["facts"].str(k)
        for (k in j["checkpoints"].keys()) checkpoints[k] = j["checkpoints"].str(k)
    }

    fun commit() {
        val m = LinkedHashMap<String, Any?>()
        m["name"] = name
        m["current"] = current
        m["unlockAll"] = unlockAll
        m["stages"] = stages
        m["pages"] = pages
        m["intros"] = intros
        m["right"] = right
        m["wrong"] = wrong
        m["met"] = met
        m["found"] = found
        m["tips"] = tips
        m["facts"] = facts
        m["checkpoints"] = checkpoints
        store.save("save", Json.write(m))
    }
}

/** One screen of the game. Subclasses add buttons and draw; taps on buttons are handled here. */
abstract class Screen(val g: Game) {
    val buttons = ArrayList<Button>()
    private val timers = ArrayList<Timer>()
    private var pressed: Button? = null
    var t = 0f

    private class Timer(var left: Float, val fn: () -> Unit)

    open fun enter() {}
    /** Called when an overlay on top of this screen closes. */
    open fun resume() {}

    open fun update(dt: Float) {
        t += dt
        if (timers.isNotEmpty()) {
            val due = ArrayList<Timer>()
            for (tm in timers) { tm.left -= dt; if (tm.left <= 0f) due.add(tm) }
            timers.removeAll(due)
            for (tm in due) tm.fn()
        }
        for (b in buttons) if (b.shake > 0f) b.shake -= dt * 2.5f
    }

    abstract fun draw(p: Painter)

    open fun down(x: Float, y: Float) {
        pressed = null
        for (i in buttons.size - 1 downTo 0) {
            val b = buttons[i]
            if (b.visible && b.enabled && b.contains(x, y)) { pressed = b; b.pressed = true; break }
        }
        if (pressed == null) tapDown(x, y)
    }

    open fun move(x: Float, y: Float) {
        val b = pressed
        if (b != null) b.pressed = b.contains(x, y) else tapMove(x, y)
    }

    open fun up(x: Float, y: Float) {
        val b = pressed
        pressed = null
        if (b != null) {
            b.pressed = false
            if (b.contains(x, y) && b.visible && b.enabled) { g.audio.sfx("tap"); b.onTap() }
        } else tapUp(x, y)
    }

    /** Touches that did not land on a button. */
    open fun tapDown(x: Float, y: Float) {}
    open fun tapMove(x: Float, y: Float) {}
    open fun tapUp(x: Float, y: Float) {}
    open fun keys(dx: Int, dy: Int) {}

    /** Return true if the back button was used up by this screen. */
    open fun back(): Boolean = false

    fun after(sec: Float, fn: () -> Unit) { timers.add(Timer(sec, fn)) }
    fun clearTimers() { timers.clear() }

    fun add(b: Button): Button { buttons.add(b); return b }

    fun button(id: String): Button? { for (b in buttons) if (b.id == id) return b; return null }

    fun drawButtons(p: Painter) { for (b in buttons) b.draw(p) }

    /** A one-line description of what is on screen, for the scripted playthrough. */
    open fun describe(): String = javaClass.simpleName
}
