package detective.core

/**
 * The data model. A case is a list of units; a unit is one JSON file in
 * assets/units. Everything a unit teaches, says or shows is in that file, so
 * a new unit is new data, not new code. docs/UNIT_FORMAT.md lists every field.
 */

/** Something a character says. [say] is the exact text the voice file was made from. */
class Line(
    val who: String, val fr: String, val en: String, val mood: String,
    val lost: Boolean, val test: Boolean, val say: String, val gloss: List<String>
) {
    companion object {
        /** [sink] collects every spoken line of the unit, so sound files can be made and checked. */
        fun from(j: J, defWho: String, sink: MutableList<Spoken>?): Line? {
            if (j.isNull) return null
            val raw = j.str("fr")
            val ln = Line(
                who = j.str("who", defWho),
                fr = Text.french(raw),
                en = j.str("en"),
                mood = j.str("mood", "neutral"),
                lost = j.bool("lost"),
                test = j.bool("test"),
                say = j.str("say", raw),
                gloss = j.strings("gloss")
            )
            if (!j.bool("mute")) sink?.add(Spoken(ln.who, ln.say))
            return ln
        }
    }
}

/** One line that needs a sound file: who says it, and the text the voice reads. */
class Spoken(val who: String, val say: String)

class Word(val id: String, val fr: String, val en: String, val icon: String, val note: String, val say: String)

class Tip(val id: String, val title: String, val text: String)

class Build(val en: String, val tiles: List<String>, val extra: List<String>, val say: String, val who: String) {
    companion object {
        fun from(j: J, defWho: String, sink: MutableList<Spoken>?): Build? {
            if (j.isNull) return null
            val tiles = ArrayList<String>()
            for (t in j.strings("tiles")) tiles.add(Text.french(t))
            val extra = ArrayList<String>()
            for (t in j.strings("extra")) extra.add(Text.french(t))
            val b = Build(j.str("en"), tiles, extra, j.str("say", j.strings("tiles").joinToString(" ")), j.str("who", defWho))
            sink?.add(Spoken(b.who, b.say))
            return b
        }
    }
}

class Quiz(val en: String, val options: List<String>, val says: List<String>, val answer: Int, val speak: Boolean)

class Page(
    val title: String, val words: List<String>, val builds: List<Build>, val tips: List<String>, val quiz: Quiz?,
    val review: Boolean, val tasks: List<String>, val strip: Boolean, val bringBack: List<String>, val refresh: Int
)

/** A person drawn from a description: a face, and whether they are tall. */
class Person(val face: J, val tall: Boolean)

class StoryCard(val scene: String, val text: String, val note: String, val line: Line?, val people: List<Person>)

/** A story picture made of tiles and sprites, described in the unit file. */
class SceneSprite(val look: String, val x: Float, val y: Float, val scale: Float, val flip: Boolean, val frame: Int)
class SceneIcon(val name: String, val x: Float, val y: Float, val size: Float)
class SceneData(
    val tileset: String, val rows: List<String>, val sprites: List<SceneSprite>, val icons: List<SceneIcon>,
    val signs: List<Sign>, val sky: Boolean, val night: Boolean
)

/** [show] is a picture held up while the character reacts, such as the wrong drink arriving. */
class Option(val fr: String, val say: String, val next: String?, val react: Line?, val narr: String?, val show: String?)

class Fact(
    val prompt: String, val options: List<String>, val answer: String, val hint: String,
    val key: String, val label: String, val card: String, val note: List<String>
)

class Pay(val amount: Int, val coins: List<Int>)

class Route(val map: String, val ids: List<String>, val paths: List<FloatArray>, val answer: String, val prompt: String)

/** One thing to sort: a name to show, a picture, the bin it belongs in, and what the speaker says about it. */
class SortItem(val name: String, val icon: String, val bin: Int, val line: Line?, val face: FaceSpec?)

/** Sort each item into one of two or three bins. [bins] are French words; [binSay] is what the voice reads. */
class SortQ(val bins: List<String>, val binSay: List<String>, val items: List<SortItem>)

/** One step of a sequence: a short French sentence and a picture. */
class OrderItem(val fr: String, val say: String, val icon: String)

/** Put the steps in order. [items] are listed in the right order; [labels] sit beside the slots ("D'abord", "Puis"…). */
class OrderQ(val labels: List<String>, val items: List<OrderItem>)

/** A room on a plan, in 0 to 100 units across and down. [label] and [icon] show from the start when [fixed]; otherwise once she has marked it. */
class PlanRoom(val id: String, val x: Float, val y: Float, val w: Float, val h: Float, val label: String, val icon: String, val fixed: Boolean)

/** Tap the room the speaker describes. [here] is where she stands, in plan units; [answer] is a room id. */
class PlanQ(val rooms: List<PlanRoom>, val answer: String, val prompt: String, val hereX: Float, val hereY: Float)

class Node(
    val id: String, val narr: String?, val npc: Line?, val mood: String, val ask: String,
    val options: List<Option>, val build: Build?, val items: List<String>, val answer: String,
    val wrong: Line?, val fact: Fact?, val next: String?, val tip: String?,
    val show: String?, val pay: Pay?, val route: Route?, val sort: SortQ?, val order: OrderQ?,
    /** A choice where every option with a "next" is right, such as saying which film you like best. */
    val any: Boolean, val plan: PlanQ?
)

/** [phone]: a phone call. No face, and the words show only after two tries, so she has to listen. */
class Talk(
    val id: String, val board: String, val boardStyle: String, val wall: Int, val sky: Boolean, val counter: Boolean,
    val name: String, val who: String, val face: FaceSpec, val start: String, val nodes: Map<String, Node>,
    val phone: Boolean,
    /** If set, this conversation is played as a battle and this is the name in the opponent's box. */
    val battle: String?
) {
    /** How many right answers the main path of this conversation needs: the length of the opponent's bar in a battle. */
    fun battleSteps(): Int {
        var n = 0
        val seen = HashSet<String>()
        var id: String? = start
        while (id != null && seen.add(id)) {
            val nd = nodes[id] ?: break
            when (nd.ask) {
                "auto", "end" -> {}
                "sort" -> n += nd.sort?.items?.size ?: 1
                else -> n++
            }
            id = if (nd.ask == "choice") {
                var nx: String? = null
                for (o in nd.options) if (o.next != null) { nx = o.next; break }
                nx
            } else nd.next
        }
        return n
    }
}

class Step(val kind: String, val ref: String)

class Stage(val id: String, val title: String, val sub: String, val icon: String, val done: String, val steps: List<Step>)

class Ticket(val airline: String, val to: String, val labels: List<String>, val values: List<String>)

// ---------- walking scenes ----------

class Sign(val x: Float, val y: Float, val text: String, val dir: String)

class Totem(val x: Int, val y: Int, val dirs: List<String>, val texts: List<String>)

class Npc(
    val id: String, val x: Int, val y: Int, val look: String,
    val line: Line?, val narr: String?, val found: String?, val talk: String?
)

class Spot(val x: Int, val y: Int, val w: Int, val line: Line?, val narr: String?, val found: String?)

class Mover(val look: String, val path: List<IntArray>, val speed: Float, val phase: Float, val line: Line?, val found: String?)

/** Someone standing in the way who steps aside when the detective says "Pardon !". */
class Blocker(val x: Int, val y: Int, val look: String)

/** Someone who walks a path: "lead" stays ahead of the detective, "loop" goes round and can be caught. They stand still until quest step [from]. */
class Runner(val id: String, val look: String, val path: List<IntArray>, val speed: Float, val mode: String, val near: Float, val from: Int)

/**
 * Someone who walks a path with a torch or a watchful eye. If the detective is
 * in front of them and close, she is seen and goes back to the last star.
 */
class Watcher(val id: String, val look: String, val path: List<IntArray>, val speed: Float, val range: Float, val pause: Float, val line: Line?)

/** Someone who walks after the detective once a quest step begins. */
class Follower(val id: String, val look: String, val x: Int, val y: Int, val speed: Float, val from: Int, val line: Line?)

class Place(val x: Int, val y: Int, val line: Line?, val hint: String, val found: String?)

/** One thing to do in a walking scene. The scene ends when every step is done. */
class QuestStep(
    val type: String, val hint: String, val line: Line?, val done: Line?, val doneNarr: String?,
    val npc: String, val x: Int, val y: Int, val w: Int, val h: Int,
    val openX: Int, val openY: Int, val item: String, val img: String,
    val items: List<IntArray>, val runner: String, val needs: String,
    val places: List<Place>, val wrong: Line?, val found: String?,
    /** "carry": the people who want what she carries; empty means bring it to the place at x, y, w, h. */
    val to: List<String>, val count: Int, val melt: Boolean
)

class WalkData(
    val id: String, val tileset: String, val map: List<String>, val startX: Int, val startY: Int,
    val tip: String, val bump: Line?, val pardon: Line?, val ticket: Boolean,
    val signs: List<Sign>, val totems: List<Totem>, val npcs: List<Npc>, val spots: List<Spot>,
    val movers: List<Mover>, val blockers: List<Blocker>, val runners: List<Runner>, val followers: List<Follower>,
    val quest: List<QuestStep>, val darkNear: Float, val darkFar: Float, val zoom: Float,
    val carry: List<String>, val watchers: List<Watcher>, val caught: Line?
)

/**
 * A small game of its own (the protest map, pétanque, the telescope, the
 * cheese belt). Each kind reads its own fields from [j]; see Games.kt.
 */
class GameData(val id: String, val kind: String, val who: String, val j: J)

// ---------- the taxi ride ----------

class Sight(val icon: String, val title: String, val text: String)

/** One corner. [turn] is where the map says to go: left, right or straight. */
class Leg(val turn: String, val ask: String, val offer: String, val line: Line?, val sight: Sight?)

/**
 * [says] holds the player's replies: left, right, straight, yes, no_left, no_right, no_straight.
 * [mode] is "taxi" (she gives the driver the way from her map), "drive" (she drives and taps the way
 * the passenger calls out, with a van behind), or "follow" (she reads a tracker and tells the driver).
 * [car], [chaser] and [lead] are looks seen from above. [scene] is the picture on arrival: "hotel",
 * "jump" (over the river) or "roadblock".
 */
class RideData(
    val id: String, val who: String, val tip: String, val legs: List<Leg>,
    val wrong: Line?, val arrive: Line?, val says: Map<String, Option>,
    val mode: String, val car: String, val chaser: String, val lead: String, val scene: String,
    val panel: String, val near: Line?
)

// ---------- the police sketch ----------

/** [raw] is the word as typed in the unit file; the voice files are keyed on it. */
class FeatOpt(val fr: String, val raw: String, val set: J)

/** [say] is the sentence as the voice should read it, when a word needs respelling for the voice. */
class Feature(val id: String, val fr: String, val say: String, val options: List<FeatOpt>, val answer: String)

class Suspect(val id: String, val title: String, val base: J, val features: List<Feature>, val intro: Line?)

class SketchData(
    val id: String, val who: String, val face: FaceSpec, val board: String, val tip: String,
    val people: List<Suspect>, val wrong: Line?, val right: Line?
)

class UnitData(j: J) {
    val id = j.str("id")
    val number = j.int("number", 1)
    val title = j.str("title")
    /** "under": English under each line. "tap": English, or a word list, only when she taps. */
    val english = j.str("english", "under")
    /** Story shown the first time the unit is opened. */
    val intro = j.strOrNull("intro")
    /** A secret word that jumps a lost or damaged save to this unit (capital letters only). */
    val code = j.str("code").uppercase()
    val voices = HashMap<String, String>()
    /** Colour swaps for the detective's sprite in this unit, e.g. a yellow hat. Empty for her usual clothes. */
    val heroSwaps = HashMap<Char, Int>()
    val words = ArrayList<Word>()
    val bonus = ArrayList<Word>()
    val tips = ArrayList<Tip>()
    val pages = ArrayList<Page>()
    val story = HashMap<String, List<StoryCard>>()
    val scenes = HashMap<String, SceneData>()
    val talks = HashMap<String, Talk>()
    val walks = HashMap<String, WalkData>()
    val rides = HashMap<String, RideData>()
    val sketches = HashMap<String, SketchData>()
    val games = HashMap<String, GameData>()
    val stages = ArrayList<Stage>()
    val caseNotes = j.strings("case")
    val ticket: Ticket?
    /** Every line of the unit that needs a sound file. */
    val spoken = ArrayList<Spoken>()

    private val wordById = HashMap<String, Word>()

    init {
        val vj = j["voices"]
        for (k in vj.keys()) voices[k] = vj.str(k)
        val hj = j["hero"]
        for (k in hj.keys()) if (k.isNotEmpty()) heroSwaps[k[0]] = Art.parseColor(hj.str(k))

        for (w in j.list("words")) words.add(word(w))
        for (w in j.list("bonus")) bonus.add(word(w))
        for (w in words) wordById[w.id] = w
        for (w in bonus) wordById[w.id] = w

        for (t in j.list("tips")) tips.add(Tip(t.str("id"), t.str("title"), t.str("text")))

        for (pg in j["notebook"].list("pages")) {
            val builds = ArrayList<Build>()
            for (b in pg.list("builds")) Build.from(b, "teacher", spoken)?.let { builds.add(it) }
            val qj = pg["quiz"]
            var quiz: Quiz? = null
            if (!qj.isNull) {
                val opts = ArrayList<String>()
                for (o in qj.strings("options")) opts.add(Text.french(o))
                quiz = Quiz(qj.str("en"), opts, qj.strings("options"), qj.int("answer"), qj.bool("speak"))
                if (quiz.speak) for (o in quiz.says) spoken.add(Spoken("teacher", o))
            }
            var tasks = pg.strings("tasks")
            if (tasks.isEmpty()) tasks = listOf("learn", "listen", "meaning", "reverse")
            // "tip" names one tip card; "tips" names several
            val pageTips = ArrayList<String>()
            pg.strOrNull("tip")?.let { pageTips.add(it) }
            pageTips.addAll(pg.strings("tips"))
            pages.add(Page(
                pg.str("title"), pg.strings("words"), builds, pageTips, quiz, pg.bool("review"),
                tasks, pg.bool("strip"), pg.strings("bringBack"), pg.int("refresh")
            ))
        }

        val sj = j["story"]
        for (k in sj.keys()) {
            val cards = ArrayList<StoryCard>()
            for (c in sj.list(k)) {
                val people = ArrayList<Person>()
                for (pj in c.list("people")) people.add(Person(pj, pj.bool("tall")))
                cards.add(StoryCard(c.str("scene"), c.str("text"), c.str("note"), Line.from(c["line"], "teacher", spoken), people))
            }
            story[k] = cards
        }

        val scj = j["scenes"]
        for (k in scj.keys()) scenes[k] = scene(scj[k])

        val tj = j["talks"]
        for (k in tj.keys()) talks[k] = talk(k, tj[k])

        val wj = j["walks"]
        for (k in wj.keys()) walks[k] = walk(k, wj[k])

        val rj = j["rides"]
        for (k in rj.keys()) rides[k] = ride(k, rj[k])

        val kj = j["sketches"]
        for (k in kj.keys()) sketches[k] = sketch(k, kj[k])

        val gj = j["games"]
        for (k in gj.keys()) {
            val gd = GameData(k, gj[k].str("kind"), gj[k].str("who", "teacher"), gj[k])
            games[k] = gd
            collectLines(gd.j, gd.who)
        }

        for (s in j.list("stages")) {
            val steps = ArrayList<Step>()
            for (st in s.list("steps")) {
                val keys = st.keys()
                if (keys.isNotEmpty()) steps.add(Step(keys[0], st[keys[0]].v?.toString() ?: ""))
            }
            stages.add(Stage(s.str("id"), s.str("title"), s.str("sub"), s.str("icon"), s.str("done"), steps))
        }

        val tk = j["ticket"]
        ticket = if (tk.isNull) null else {
            val labels = ArrayList<String>()
            val values = ArrayList<String>()
            for (f in tk.list("fields")) {
                val pair = f.list()
                if (pair.size >= 2) { labels.add(pair[0].asString()); values.add(pair[1].asString()) }
            }
            Ticket(tk.str("airline"), tk.str("to"), labels, values)
        }
    }

    /** Every object with a "fr" inside a game is a spoken line, unless it says "mute". */
    private fun collectLines(j: J, who: String) {
        val v = j.v
        if (v is Map<*, *>) {
            val fr = j.strOrNull("fr")
            if (fr != null && !j.bool("mute")) spoken.add(Spoken(j.str("who", who), j.str("say", fr)))
            for (k in j.keys()) collectLines(j[k], who)
        } else if (v is List<*>) {
            for (e in j.list()) collectLines(e, who)
        }
    }

    fun word(id: String): Word? = wordById[id]
    fun tip(id: String): Tip? { for (t in tips) if (t.id == id) return t; return null }
    fun voiceFor(who: String): String = voices[who] ?: voices["teacher"] ?: "siwis"

    private fun word(w: J): Word {
        val raw = w.str("fr")
        val wd = Word(w.str("id"), Text.french(raw), w.str("en"), w.str("icon", "talk"), w.str("note"), w.str("say", raw))
        spoken.add(Spoken("teacher", wd.say))
        return wd
    }

    private fun xy(j: J, key: String): IntArray? {
        val l = j.list(key)
        return if (l.size >= 2) intArrayOf(l[0].asInt(), l[1].asInt()) else null
    }

    private fun points(j: J, key: String): List<IntArray> {
        val out = ArrayList<IntArray>()
        for (pt in j.list(key)) {
            val l = pt.list()
            if (l.size >= 2) out.add(intArrayOf(l[0].asInt(), l[1].asInt()))
        }
        return out
    }

    private fun signs(j: J): List<Sign> {
        val out = ArrayList<Sign>()
        for (s in j.list("signs")) out.add(Sign(s.num("x"), s.num("y"), s.str("text"), s.str("dir")))
        return out
    }

    private fun scene(s: J): SceneData {
        val sprites = ArrayList<SceneSprite>()
        for (sp in s.list("sprites")) sprites.add(SceneSprite(sp.str("look"), sp.num("x"), sp.num("y"), sp.num("scale", 1f), sp.bool("flip"), sp.int("frame")))
        val icons = ArrayList<SceneIcon>()
        for (ic in s.list("icons")) icons.add(SceneIcon(ic.str("name"), ic.num("x"), ic.num("y"), ic.num("size", 40f)))
        return SceneData(s.str("tileset", "airport"), s.strings("rows"), sprites, icons, signs(s), s.bool("sky"), s.bool("night"))
    }

    private fun option(o: J, who: String): Option {
        val raw = o.str("fr")
        val op = Option(Text.french(raw), o.str("say", raw), o.strOrNull("next"), Line.from(o["react"], who, spoken), o.strOrNull("narr"), o.strOrNull("show"))
        spoken.add(Spoken("hero", op.say))
        return op
    }

    private fun talk(id: String, t: J): Talk {
        val who = t.str("who", "teacher")
        val nodes = LinkedHashMap<String, Node>()
        val nj = t["nodes"]
        for (nid in nj.keys()) {
            val n = nj[nid]
            val options = ArrayList<Option>()
            for (o in n.list("options")) options.add(option(o, who))
            val fj = n["fact"]
            val fact = if (fj.isNull) null else Fact(
                fj.str("prompt"), fj.strings("options"), fj.str("answer"), fj.str("hint"),
                fj.str("key"), fj.str("label"), fj.str("card", "ticket"), fj.strings("note")
            )
            val pj = n["pay"]
            val pay = if (pj.isNull) null else {
                val coins = ArrayList<Int>()
                for (c in pj.list("coins")) coins.add(c.asInt())
                Pay(pj.int("amount"), coins)
            }
            val rj = n["route"]
            val route = if (rj.isNull) null else {
                val ids = ArrayList<String>()
                val paths = ArrayList<FloatArray>()
                for (r in rj.list("routes")) {
                    ids.add(r.str("id"))
                    val pts = r.list("pts")
                    val arr = FloatArray(pts.size * 2)
                    for (i in pts.indices) {
                        val l = pts[i].list()
                        arr[i * 2] = (l[0].v as Number).toFloat()
                        arr[i * 2 + 1] = (l[1].v as Number).toFloat()
                    }
                    paths.add(arr)
                }
                Route(rj.str("map", "island"), ids, paths, rj.str("answer"), rj.str("prompt"))
            }
            val soj = n["sort"]
            val sort = if (soj.isNull) null else {
                val bins = ArrayList<String>()
                val says = ArrayList<String>()
                for (b in soj.list("bins")) {
                    bins.add(Text.french(b.str("fr")))
                    says.add(b.str("say", b.str("fr")))
                    spoken.add(Spoken("hero", b.str("say", b.str("fr"))))
                }
                val its = ArrayList<SortItem>()
                for (it in soj.list("items")) its.add(SortItem(Text.french(it.str("name")), it.str("icon"), it.int("bin"), Line.from(it["line"], who, spoken),
                    if (it["face"].isNull) null else FaceSpec.from(it["face"])))
                SortQ(bins, says, its)
            }
            val plj = n["plan"]
            val plan = if (plj.isNull) null else {
                val rooms = ArrayList<PlanRoom>()
                for (r in plj.list("rooms")) rooms.add(PlanRoom(r.str("id"), r.num("x"), r.num("y"), r.num("w", 20f), r.num("h", 20f),
                    Text.french(r.str("label")), r.str("icon"), r.bool("fixed")))
                val here = plj.list("here")
                PlanQ(rooms, plj.str("answer"), plj.str("prompt"), if (here.size >= 2) here[0].asFloat() else -1f, if (here.size >= 2) here[1].asFloat() else -1f)
            }
            val oj = n["order"]
            val order = if (oj.isNull) null else {
                val its = ArrayList<OrderItem>()
                for (it in oj.list("items")) {
                    its.add(OrderItem(Text.french(it.str("fr")), it.str("say", it.str("fr")), it.str("icon")))
                    spoken.add(Spoken("hero", it.str("say", it.str("fr"))))
                }
                OrderQ(oj.strings("labels").map { Text.french(it) }, its)
            }
            nodes[nid] = Node(
                id = nid, narr = n.strOrNull("narr"), npc = Line.from(n["npc"], who, spoken), mood = n.str("mood", "neutral"),
                ask = n.str("ask", "choice"), options = options, build = Build.from(n["build"], "hero", spoken),
                items = n.strings("items"), answer = n.str("answer"), wrong = Line.from(n["wrong"], who, spoken),
                fact = fact, next = n.strOrNull("next"), tip = n.strOrNull("tip"),
                show = n.strOrNull("show"), pay = pay, route = route, sort = sort, order = order,
                any = n.bool("any"), plan = plan
            )
        }
        val wall = t.strOrNull("wall")
        return Talk(
            id, t.str("board"), t.str("boardStyle", "dark"), if (wall == null) 0xFFC9D6EA.toInt() else Art.parseColor(wall),
            t.bool("sky"), t.bool("counter", true), t.str("name"), who, FaceSpec.from(t["face"]), t.str("start"), nodes,
            t.bool("phone"), if (t.has("battle")) t["battle"].str("title", t.str("name")) else null
        )
    }

    private fun walk(id: String, a: J): WalkData {
        val totems = ArrayList<Totem>()
        for (t in a.list("totems")) {
            val dirs = ArrayList<String>()
            val texts = ArrayList<String>()
            for (ln in t.list("lines")) {
                val pair = ln.list()
                if (pair.size >= 2) { dirs.add(pair[0].asString()); texts.add(pair[1].asString()) }
            }
            totems.add(Totem(t.int("x"), t.int("y"), dirs, texts))
        }
        val npcs = ArrayList<Npc>()
        for (n in a.list("npcs")) {
            npcs.add(Npc(
                n.str("id"), n.int("x"), n.int("y"), n.str("look", "agent_m"),
                Line.from(n["line"], n.str("id"), spoken), n.strOrNull("narr"), n.strOrNull("found"), n.strOrNull("talk")
            ))
        }
        val spots = ArrayList<Spot>()
        for (s in a.list("spots")) spots.add(Spot(s.int("x"), s.int("y"), s.int("w", 1), Line.from(s["line"], "teacher", spoken), s.strOrNull("narr"), s.strOrNull("found")))
        val movers = ArrayList<Mover>()
        for (m in a.list("movers")) {
            movers.add(Mover(m.str("look", "case_red"), points(m, "path"), m.num("speed", 24f), m.num("phase", 0f), Line.from(m["line"], "teacher", spoken), m.strOrNull("found")))
        }
        val blockers = ArrayList<Blocker>()
        for (b in a.list("blockers")) blockers.add(Blocker(b.int("x"), b.int("y"), b.str("look", "agent_m")))
        val runners = ArrayList<Runner>()
        for (r in a.list("runners")) runners.add(Runner(r.str("id"), r.str("look"), points(r, "path"), r.num("speed", 50f), r.str("mode", "loop"), r.num("near", 72f), r.int("from")))
        val followers = ArrayList<Follower>()
        for (f in a.list("followers")) followers.add(Follower(f.str("id"), f.str("look"), f.int("x"), f.int("y"), f.num("speed", 32f), f.int("from"), Line.from(f["line"], f.str("id"), spoken)))
        val quest = ArrayList<QuestStep>()
        for (q in a.list("quest")) {
            val at = xy(q, "at")
            val open = xy(q, "open")
            val places = ArrayList<Place>()
            for (pl in q.list("places")) {
                val pat = xy(pl, "at")
                places.add(Place(pat?.get(0) ?: 0, pat?.get(1) ?: 0, Line.from(pl["line"], q.str("who", "teacher"), spoken), pl.str("hint"), pl.strOrNull("found")))
            }
            quest.add(QuestStep(
                type = q.str("type"), hint = q.str("hint"), line = Line.from(q["line"], q.str("who", "teacher"), spoken),
                done = Line.from(q["done"], q.str("who", "teacher"), spoken), doneNarr = q.strOrNull("doneNarr"),
                npc = q.str("npc"), x = at?.get(0) ?: -1, y = at?.get(1) ?: -1, w = q.int("w", 1), h = q.int("h", 1),
                openX = open?.get(0) ?: -1, openY = open?.get(1) ?: -1, item = q.str("item"), img = q.str("img"),
                items = points(q, "items"), runner = q.str("runner"), needs = q.str("needs"),
                places = places, wrong = Line.from(q["wrong"], q.str("who", "teacher"), spoken), found = q.strOrNull("found"),
                to = q.strings("to"), count = q.int("count", 0), melt = q.bool("melt")
            ))
        }
        val watchers = ArrayList<Watcher>()
        for (w in a.list("watchers")) watchers.add(Watcher(
            w.str("id"), w.str("look"), points(w, "path"), w.num("speed", 22f), w.num("range", 44f), w.num("pause", 1.2f),
            Line.from(w["line"], w.str("id"), spoken)
        ))
        val start = xy(a, "start")
        val dark = a["dark"]
        return WalkData(
            id = id, tileset = a.str("tileset", "airport"), map = a.strings("map"),
            startX = start?.get(0) ?: 1, startY = start?.get(1) ?: 1,
            tip = a.str("tip"), bump = Line.from(a["bump"], "hero", spoken), pardon = Line.from(a["pardon"], "hero", spoken),
            ticket = a.bool("ticket"), signs = signs(a), totems = totems, npcs = npcs, spots = spots, movers = movers,
            blockers = blockers, runners = runners, followers = followers, quest = quest,
            darkNear = if (dark.isNull) 0f else dark.num("near", 40f), darkFar = if (dark.isNull) 0f else dark.num("far", 80f),
            zoom = a.num("zoom", 1.5f), carry = a.strings("carry"), watchers = watchers,
            caught = Line.from(a["caught"], "hero", spoken)
        )
    }

    private fun ride(id: String, r: J): RideData {
        val who = r.str("who", "driver")
        val legs = ArrayList<Leg>()
        for (l in r.list("legs")) {
            val sj = l["sight"]
            val sight = if (sj.isNull) null else Sight(sj.str("icon"), sj.str("title"), sj.str("text"))
            legs.add(Leg(l.str("turn"), l.str("ask", "pick"), l.str("offer"), Line.from(l["line"], who, spoken), sight))
        }
        val says = LinkedHashMap<String, Option>()
        val sj = r["says"]
        for (k in sj.keys()) says[k] = option(sj[k], who)
        return RideData(
            id, who, r.str("tip"), legs, Line.from(r["wrong"], who, spoken), Line.from(r["arrive"], who, spoken), says,
            r.str("mode", "taxi"), r.str("car", "taxi_up"), r.str("chaser"), r.str("lead"), r.str("scene", "hotel"),
            r.str("panel"), Line.from(r["near"], who, spoken)
        )
    }

    private fun sketch(id: String, s: J): SketchData {
        val who = s.str("who", "officer")
        val people = ArrayList<Suspect>()
        for (pj in s.list("people")) {
            val feats = ArrayList<Feature>()
            for (f in pj.list("features")) {
                val opts = ArrayList<FeatOpt>()
                for (o in f.list("options")) {
                    opts.add(FeatOpt(Text.french(o.str("fr")), o.str("fr"), o["set"]))
                    spoken.add(Spoken("hero", f.str("say", f.str("fr")).replace("{x}", o.str("fr"))))
                }
                feats.add(Feature(f.str("id"), f.str("fr"), f.str("say", f.str("fr")), opts, Text.french(f.str("answer"))))
            }
            people.add(Suspect(pj.str("id"), pj.str("title"), pj["base"], feats, Line.from(pj["intro"], who, spoken)))
        }
        return SketchData(
            id, who, FaceSpec.from(s["face"]), s.str("board"), s.str("tip"), people,
            Line.from(s["wrong"], who, spoken), Line.from(s["right"], who, spoken)
        )
    }
}

/** How a map letter is drawn and whether she can walk on it. */
class TileDef(val img: String, val solid: Boolean, val under: Boolean, val shade: Boolean)

class Tileset(val floor: String, val void: Int, val defs: Map<Char, TileDef>) {
    companion object {
        fun parseAll(j: J): Map<String, Tileset> {
            val out = HashMap<String, Tileset>()
            for (name in j.keys()) {
                val t = j[name]
                val defs = HashMap<Char, TileDef>()
                val tj = t["tiles"]
                for (k in tj.keys()) {
                    if (k.isEmpty()) continue
                    val d = tj[k]
                    defs[k[0]] = TileDef(d.str("img"), d.bool("solid", true), d.bool("under"), d.bool("shade"))
                }
                out[name] = Tileset(t.str("floor", "tile_floor"), Art.parseColor(t.str("void", "0f1326")), defs)
            }
            return out
        }
    }
}

class UnitInfo(val id: String, val title: String, val number: Int, val caseId: String)

class CaseInfo(val id: String, val title: String, val units: List<UnitInfo>)
