package detective.desktop

import detective.core.BeltScreen
import detective.core.BlockScreen
import detective.core.BoulesScreen
import detective.core.DressScreen
import detective.core.MetroScreen
import detective.core.CodeScreen
import detective.core.Games
import detective.core.MiniGame
import detective.core.SpyScreen
import detective.core.DrillScreen
import detective.core.Duel
import detective.core.Ears
import detective.core.Heard
import detective.core.HeardWord
import detective.core.Flow
import detective.core.Game
import detective.core.HubScreen
import detective.core.Icons
import detective.core.Looks
import detective.core.NameScreen
import detective.core.NotebookScreen
import detective.core.RideScreen
import detective.core.Screen
import detective.core.SketchScreen
import detective.core.StageDoneScreen
import detective.core.StoryScreen
import detective.core.TalkScreen
import detective.core.TitleScreen
import detective.core.UnitData
import detective.core.WalkScreen
import detective.core.pageRange
import java.awt.image.BufferedImage
import java.io.File
import java.util.ArrayDeque
import javax.imageio.ImageIO

/**
 * Checks the unit files, then plays every unit from the title screen to the
 * end with no person at the controls, saving a picture of each kind of screen
 * on the way. It makes some mistakes on purpose to prove the game recovers.
 * Run it after any change:
 *
 *   Check <assets folder> <output folder> [unit ids to play, e.g. unit3 unit4]
 *
 * It stops with an error if a unit file is inconsistent, a button is missing,
 * a scene never ends, or a French line has no sound file.
 */
class Check(assetDir: File, private val outDir: File, private val only: Set<String>, private val needAudio: Boolean) {
    private val files = Files(assetDir)
    private val audio = DesktopAudio(null)
    private val store = FileStore(null)
    private val painter = AwtPainter(files)
    private val ears = FakeEars()
    private var game = Game(FileAssets(files), audio, store).also { it.ears = ears }
    private var shots = 0
    private var clock = 0f
    private val seen = HashSet<String>()
    private var tag = "u0"

    // a Fire 7 screen
    private val pxW = 1024
    private val pxH = 600

    private fun screen(): Screen = game.overlay ?: game.screen

    private fun step(sec: Float) {
        var left = sec
        while (left > 0f) { game.update(1f / 30f); left -= 1f / 30f; clock += 1f / 30f }
    }

    private fun shot(name: String) {
        val img = BufferedImage(pxW, pxH, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        val scale = Math.min(pxW / Game.W.toDouble(), pxH / Game.H.toDouble())
        painter.begin(g, scale, (pxW - Game.W * scale) / 2.0, (pxH - Game.H * scale) / 2.0)
        game.draw(painter)
        g.dispose()
        shots++
        val clean = name.replace(Regex("[^A-Za-z0-9_]+"), "-")
        ImageIO.write(img, "png", File(outDir, String.format("%s_%03d_%s.png", tag, shots, clean)))
    }

    /** Take a picture the first time each kind of screen appears in a unit. */
    private fun shotOnce(name: String) { if (seen.add("$tag/$name")) { step(0.3f); shot(name) } }

    private fun tap(id: String) {
        val b = screen().button(id) ?: fail("no button \"$id\" on: ${screen().describe()}")
        if (!b.visible || !b.enabled) fail("button \"$id\" cannot be tapped on: ${screen().describe()}")
        val x = b.x + b.w / 2f
        val y = b.y + b.h / 2f
        game.down(x, y)
        game.up(x, y)
    }

    private fun has(id: String): Boolean { val b = screen().button(id); return b != null && b.visible && b.enabled }

    private fun fail(msg: String): Nothing {
        try { shot("FAILED") } catch (e: Exception) { }
        throw IllegalStateException("[$tag] $msg")
    }

    private inline fun waitFor(what: String, maxSec: Float = 40f, cond: () -> Boolean) {
        var waited = 0f
        while (!cond()) {
            step(0.1f)
            waited += 0.1f
            if (waited > maxSec) fail("waited too long for $what on: ${screen().describe()}")
        }
    }

    // ---------- checks on the unit files themselves ----------

    private fun validate() {
        val problems = ArrayList<String>()
        val wordOwner = HashMap<String, String>()
        val codes = HashMap<String, String>()
        val earlier = HashSet<String>()
        for (info in game.unitList) {
            val u = game.unitById(info.id)
            fun bad(msg: String) { problems.add("${u.id}: $msg") }
            for (w in u.words + u.bonus) {
                val prev = wordOwner.put(w.id, u.id)
                if (prev != null) bad("word id \"${w.id}\" is also used in $prev")
                if (!Icons.known(w.icon)) bad("word \"${w.id}\" uses unknown icon \"${w.icon}\"")
            }
            for ((i, pg) in u.pages.withIndex()) {
                for (id in pg.words) if (u.word(id) == null) bad("page ${i + 1} lists unknown word \"$id\"")
                for (id in pg.bringBack) if (!earlier.contains(id) && u.word(id) == null) bad("page ${i + 1} brings back \"$id\", which no earlier unit teaches")
                for (tp in pg.tips) if (u.tip(tp) == null) bad("page ${i + 1} names unknown tip \"$tp\"")
                val q = pg.quiz
                if (q != null && (q.answer < 0 || q.answer >= q.options.size)) bad("page ${i + 1} quiz answer is out of range")
                for (t in pg.tasks) if (t !in listOf("learn", "picture", "listen", "meaning", "reverse")) bad("page ${i + 1} has unknown task \"$t\"")
            }
            for (tp in u.tips) if (u.pages.none { it.tips.contains(tp.id) }) bad("tip \"${tp.id}\" is on no notebook page, so nobody will ever see it")
            for (w in u.words) if (u.pages.none { it.words.contains(w.id) }) bad("word \"${w.id}\" is on no notebook page, so it is never taught")
            // every notebook page belongs to some notebook step
            val covered = HashSet<Int>()
            for (st in u.stages) for (s in st.steps) if (s.kind == "drill") {
                if (s.ref != "all" && !Regex("\\d+(-\\d+)?").matches(s.ref.trim())) bad("notebook step \"${s.ref}\" should be \"all\", a page number, or two numbers like \"1-3\"")
                val r = pageRange(s.ref, u.pages.size)
                if (s.ref != "all" && (r[0] > r[1] || s.ref.split('-').any { (it.trim().toIntOrNull() ?: 0) > u.pages.size })) bad("notebook step \"${s.ref}\" names pages the unit does not have (it has ${u.pages.size})")
                for (k in r[0]..r[1]) covered.add(k)
            }
            for (k in u.pages.indices) if (!covered.contains(k)) bad("notebook page ${k + 1} is in no notebook step, so it is never shown")
            if (!Regex("[A-Z]{4,12}").matches(u.code)) bad("needs a secret \"code\" of 4 to 12 capital letters (it has \"${u.code}\")")
            val codeOwner = codes.put(u.code, u.id)
            if (codeOwner != null) bad("code \"${u.code}\" is also the code of $codeOwner")
            val intro = u.intro
            if (intro != null && u.story[intro] == null) bad("intro story \"$intro\" is missing")
            if (u.stages.isEmpty()) bad("no stages")
            for (st in u.stages) {
                if (st.steps.isEmpty()) bad("stage \"${st.id}\" has no steps")
                if (!Icons.known(st.icon)) bad("stage \"${st.id}\" uses unknown icon \"${st.icon}\"")
                for (s in st.steps) {
                    val ok = when (s.kind) {
                        "drill" -> u.pages.isNotEmpty()
                        "story" -> u.story[s.ref] != null
                        "talk" -> u.talks[s.ref] != null
                        "walk" -> u.walks[s.ref] != null
                        "ride" -> u.rides[s.ref] != null
                        "sketch" -> u.sketches[s.ref] != null
                        "game" -> u.games[s.ref] != null
                        else -> false
                    }
                    if (!ok) bad("stage \"${st.id}\" step ${s.kind} \"${s.ref}\" has nothing to run")
                }
            }
            for (cards in u.story.values) for (c in cards) {
                val known = c.scene == "letter" || c.people.isNotEmpty() || u.scenes[c.scene] != null || c.scene.startsWith("icon:") ||
                    c.scene in listOf("office", "office_letter", "map", "notebook", "airport", "hall", "cabin", "sky", "sky_night", "paris", "paris_night")
                if (!known) bad("story card uses unknown scene \"${c.scene}\"")
                if (c.scene.startsWith("icon:") && !Icons.known(c.scene.substring(5))) bad("story card uses unknown icon \"${c.scene}\"")
            }
            for ((name, sc) in u.scenes) {
                val ts = game.tilesets[sc.tileset]
                if (ts == null) { bad("scene \"$name\" uses unknown tileset \"${sc.tileset}\""); continue }
                for (row in sc.rows) for (ch in row) if (ch != ' ' && ch != '.' && ts.defs[ch] == null) bad("scene \"$name\" uses letter '$ch' that tileset ${sc.tileset} does not have")
                for (sp in sc.sprites) if (sp.look != "hero" && !Looks.known(game, sp.look)) bad("scene \"$name\" uses unknown look \"${sp.look}\"")
                for (ic in sc.icons) if (!Icons.known(ic.name)) bad("scene \"$name\" uses unknown icon \"${ic.name}\"")
            }
            var duels = 0
            for ((name, tk) in u.talks) {
                if (tk.nodes[tk.start] == null) bad("talk \"$name\" has no start node \"${tk.start}\"")
                for (n in tk.nodes.values) {
                    fun nodeBad(msg: String) = bad("talk \"$name\" node \"${n.id}\": $msg")
                    val nx = n.next
                    if (nx != null && tk.nodes[nx] == null) nodeBad("next \"$nx\" does not exist")
                    when (n.ask) {
                        "choice" -> {
                            val nexts = n.options.count { it.next != null }
                            if (n.any && nexts < 1) nodeBad("needs at least one option with \"next\"")
                            if (!n.any && nexts != 1) nodeBad("needs exactly one option with \"next\" (or \"any\": true if every such option is right)")
                            for (o in n.options) { val on = o.next; if (on != null && tk.nodes[on] == null) nodeBad("option goes to missing node \"$on\"") }
                        }
                        "build" -> if (n.build == null || nx == null) nodeBad("build needs \"build\" and \"next\"")
                        "give" -> {
                            if (!n.items.contains(n.answer) || nx == null) nodeBad("give needs \"answer\" among \"items\", and \"next\"")
                            for (it in n.items) if (!Icons.known(it)) nodeBad("unknown icon \"$it\"")
                        }
                        "fact" -> { val f = n.fact; if (f == null || !f.options.contains(f.answer) || nx == null) nodeBad("fact needs its answer among its options, and \"next\"") }
                        "pay" -> { val pay = n.pay; if (pay == null || pay.coins.isEmpty() || nx == null) nodeBad("pay needs \"pay\" and \"next\"") }
                        "route" -> { val r = n.route; if (r == null || !r.ids.contains(r.answer) || nx == null) nodeBad("route needs its answer among its routes, and \"next\"") }
                        "auto" -> if (nx == null) nodeBad("auto needs \"next\"")
                        "sort" -> {
                            val q = n.sort
                            if (q == null || q.bins.size < 2 || q.items.isEmpty() || nx == null) nodeBad("sort needs two or more bins, some items, and \"next\"")
                            else for (it in q.items) {
                                if (it.bin < 0 || it.bin >= q.bins.size) nodeBad("item \"${it.name}\" goes in bin ${it.bin}, which does not exist")
                                if (it.face == null && !Icons.known(it.icon)) nodeBad("item \"${it.name}\" has unknown icon \"${it.icon}\"")
                            }
                        }
                        "order" -> {
                            val q = n.order
                            if (q == null || q.items.size < 2 || nx == null) nodeBad("order needs two or more items, and \"next\"")
                            else for (it in q.items) if (!Icons.known(it.icon)) nodeBad("step \"${it.fr}\" has unknown icon \"${it.icon}\"")
                        }
                        "plan" -> {
                            val q = n.plan
                            if (q == null || q.rooms.none { it.id == q.answer } || nx == null) nodeBad("plan needs its answer among its rooms, and \"next\"")
                            else {
                                if (q.rooms.first { it.id == q.answer }.fixed) nodeBad("plan answer \"${q.answer}\" is already marked (fixed)")
                                for (r in q.rooms) if (r.icon.isNotEmpty() && !Icons.known(r.icon)) nodeBad("room \"${r.id}\" has unknown icon \"${r.icon}\"")
                            }
                        }
                        "duel" -> {
                            duels++
                            val d = n.duel
                            if (d == null || nx == null) nodeBad("duel needs \"duel\" and \"next\"")
                            else {
                                if (d.title.isEmpty() || d.title.length > 24) nodeBad("a duel title must be 1 to 24 letters (\"${d.title}\")")
                                if (n.npc == null) nodeBad("a duel needs \"npc\": the speaker's first, doubtful line")
                                val kinds = d.moves.map { it.kind }
                                for (k in kinds) if (k !in Duel.KINDS) nodeBad("unknown move kind \"$k\" (use ${Duel.KINDS})")
                                for (k in listOf("plead", "insist", "who")) if (kinds.count { it == k } != 1) nodeBad("a duel needs exactly one \"$k\" move")
                                if (kinds.count { it == "greet" } > 1) nodeBad("a duel has at most one \"greet\" move")
                                for (m in d.moves) if (m.fr.length > 42) nodeBad("move \"${m.fr}\" is too long for its button")
                                val gr = d.moves.firstOrNull { it.kind == "greet" }
                                if (gr != null && !gr.fr.startsWith("Bonjour") && !gr.fr.startsWith("Bonsoir") && !gr.fr.startsWith("Allô")) nodeBad("the greet move should start with Bonjour or Bonsoir")
                            }
                        }
                        "end" -> { }
                        else -> nodeBad("unknown ask \"${n.ask}\"")
                    }
                    val sh = n.show
                    if (sh != null && !Icons.known(sh)) nodeBad("unknown icon \"$sh\"")
                    for (o in n.options) { val os = o.show; if (os != null && !Icons.known(os)) nodeBad("unknown icon \"$os\"") }
                }
            }
            if (duels > 1) bad("only one duel per unit; found $duels")
            for ((name, w) in u.walks) {
                fun walkBad(msg: String) = bad("walk \"$name\": $msg")
                val ts = game.tilesets[w.tileset]
                if (ts == null) { walkBad("unknown tileset \"${w.tileset}\""); continue }
                for ((r, row) in w.map.withIndex()) for (ch in row) {
                    if (ch != ' ' && ch != '.' && ch != '*' && ts.defs[ch] == null) walkBad("row $r uses letter '$ch' that tileset ${w.tileset} does not have")
                }
                for (n in w.npcs) {
                    if (!Looks.known(game, n.look)) walkBad("unknown look \"${n.look}\"")
                    val t = n.talk
                    if (t != null && u.talks[t] == null) walkBad("person \"${n.id}\" has unknown talk \"$t\"")
                }
                for (m in w.movers) if (!Looks.known(game, m.look)) walkBad("unknown look \"${m.look}\"")
                for (b in w.blockers) if (!Looks.known(game, b.look)) walkBad("unknown look \"${b.look}\"")
                for (r in w.runners) if (!Looks.known(game, r.look)) walkBad("unknown look \"${r.look}\"")
                for (f in w.followers) if (!Looks.known(game, f.look)) walkBad("unknown look \"${f.look}\"")
                for (wt in w.watchers) {
                    if (!Looks.known(game, wt.look)) walkBad("unknown look \"${wt.look}\"")
                    if (wt.path.size < 2) walkBad("watcher \"${wt.id}\" needs a path of two or more points")
                }
                if (w.quest.isEmpty()) walkBad("no quest")
                for (q in w.quest) {
                    if (q.type !in listOf("talk", "reach", "get", "enter", "collect", "catch", "lead", "seek", "carry")) walkBad("unknown quest step \"${q.type}\"")
                    if (q.type == "carry") {
                        if (q.items.isEmpty()) walkBad("carry step needs \"items\" to pick up")
                        for (id in q.to) if (w.npcs.none { it.id == id }) walkBad("carry step brings things to \"$id\", who is not in the scene")
                        if (q.to.isEmpty() && q.x < 0) walkBad("carry step needs \"to\" (people) or \"at\" (a place)")
                        val need = if (q.count > 0) q.count else if (q.to.isNotEmpty()) q.to.size else q.items.size
                        if (need > q.items.size) walkBad("carry step needs $need things but there are only ${q.items.size}")
                        if (q.img.isNotEmpty() && !game.art.has(q.img)) walkBad("carry step uses unknown picture \"${q.img}\"")
                    }
                    if (q.type == "talk" && w.npcs.none { it.id == q.npc && it.talk != null }) walkBad("quest talks to \"${q.npc}\", who has no talk")
                    if ((q.type == "catch" || q.type == "lead") && w.runners.none { it.id == q.runner }) walkBad("quest names unknown runner \"${q.runner}\"")
                    for (pl in q.places) if (pl.hint.isNotEmpty() && !Icons.known(pl.hint)) walkBad("unknown icon \"${pl.hint}\"")
                }
            }
            for ((name, r) in u.rides) {
                if (r.mode !in listOf("taxi", "drive", "follow")) bad("ride \"$name\" has an unknown mode \"${r.mode}\"")
                if (r.scene !in listOf("hotel", "jump", "roadblock")) bad("ride \"$name\" has an unknown scene \"${r.scene}\"")
                for (lk in listOf(r.car, r.chaser, r.lead, r.car.replace("_up", "_side"), r.lead.replace("_up", "_side"))) if (lk.isNotEmpty() && !Looks.known(game, lk)) bad("ride \"$name\" uses unknown look \"$lk\"")
                if (r.mode == "follow" && r.lead.isEmpty()) bad("ride \"$name\" follows a car, so it needs a \"lead\" look")
                if (r.mode != "drive") for (k in listOf("left", "right", "straight")) if (r.says[k] == null) bad("ride \"$name\" has no reply for \"$k\"")
                for (l in r.legs) {
                    val turns = if (r.mode == "drive") listOf("left", "right", "straight", "faster", "slower") else listOf("left", "right", "straight")
                    if (l.turn !in turns) bad("ride \"$name\" has a turn \"${l.turn}\"")
                    if (l.ask == "confirm") for (k in listOf("yes", "no_left", "no_right", "no_straight")) if (r.says[k] == null) bad("ride \"$name\" has no reply for \"$k\"")
                    val s = l.sight
                    if (s != null && !Icons.known(s.icon)) bad("ride \"$name\" uses unknown icon \"${s.icon}\"")
                }
            }
            for ((name, gd) in u.games) {
                fun gameBad(msg: String) = bad("game \"$name\": $msg")
                if (gd.kind !in Games.KINDS) { gameBad("unknown kind \"${gd.kind}\""); continue }
                val gj = gd.j
                when (gd.kind) {
                    "block" -> {
                        val nodes = gj["nodes"].keys().toSet()
                        val sts = gj.list("streets")
                        if (sts.count { it.bool("alley") } != 1) gameBad("needs exactly one street with \"alley\": true")
                        if (sts.none { it.str("id") == gj.str("start") }) gameBad("start street \"${gj.str("start")}\" does not exist")
                        for (s in sts) {
                            if (s.str("a") !in nodes || s.str("b") !in nodes) gameBad("street \"${s.str("id")}\" joins a corner that does not exist")
                            else {
                                val a = gj["nodes"].list(s.str("a")); val b = gj["nodes"].list(s.str("b"))
                                if (a[0].asFloat() != b[0].asFloat() && a[1].asFloat() != b[1].asFloat()) gameBad("street \"${s.str("id")}\" is not straight across or straight down")
                            }
                        }
                    }
                    "spy" -> {
                        val ts = gj.list("targets")
                        if (ts.count { it.bool("right") } != 1) gameBad("needs exactly one target with \"right\": true")
                        for (t in ts) if (!Icons.known(t.str("icon"))) gameBad("unknown icon \"${t.str("icon")}\"")
                    }
                    "metro" -> {
                        val trips = gj.list("trips")
                        if (trips.isEmpty()) gameBad("needs one or more trips")
                        for (t in trips) {
                            val names = t.list("stations").map { detective.core.Text.french(it.str("fr")) }
                            if (detective.core.Text.french(t.str("from")) !in names || detective.core.Text.french(t.str("to")) !in names) gameBad("trip on line ${t.str("line")}: \"from\" and \"to\" must be stations of the trip")
                            if (t.strings("ends").size != 2) gameBad("trip on line ${t.str("line")} needs two \"ends\"")
                        }
                    }
                    "dress" -> {
                        val all = HashSet<String>()
                        for (r in gj.list("racks")) for (it in r.strings("items")) { all.add(it); if (!Icons.known(it)) gameBad("unknown icon \"$it\"") }
                        for (m in gj.list("models")) for (w in m.list("wants")) if (w.str("item") !in all) gameBad("model \"${m.str("name")}\" wants \"${w.str("item")}\", which is on no rack")
                    }
                    "belt" -> {
                        val n = gj.list("shelves").size
                        if (n < 2) gameBad("needs two or more shelves")
                        for (c in gj.list("cheeses")) {
                            if (c.int("shelf") !in 0 until n) gameBad("cheese \"${c.str("name")}\" goes on a shelf that does not exist")
                            if (!Icons.known(c.str("icon", "cheese"))) gameBad("unknown icon \"${c.str("icon")}\"")
                        }
                    }
                }
            }
            for ((name, sk) in u.sketches) for (per in sk.people) for (f in per.features) {
                if (f.options.none { it.fr == f.answer }) bad("sketch \"$name\": the answer \"${f.answer}\" for \"${f.id}\" is not one of its options")
            }
            for (sp in u.spoken) if (sp.who != "teacher" && u.voices[sp.who] == null) bad("no voice set for \"${sp.who}\" (line: ${sp.say})")
            for (w in u.words + u.bonus) earlier.add(w.id)
        }
        if (problems.isNotEmpty()) {
            for (pr in problems.distinct()) println("  PROBLEM $pr")
            throw IllegalStateException("${problems.distinct().size} problems in the unit files")
        }
        println("unit files are consistent: ${game.unitList.size} units, ${wordOwner.size} words")
    }

    // ---------- the playthrough ----------

    fun run() {
        outDir.mkdirs()
        validate()

        if (game.screen !is TitleScreen) fail("should start on the title")
        step(0.5f); shot("title")
        tap("start")
        if (game.screen !is NameScreen) fail("Start should ask for a name")
        if (has("ok")) fail("OK should be off until a name is typed")
        for (ch in "VIVIENNE") tap("key:$ch")
        tap("del"); tap("del"); tap("del"); tap("del"); tap("del")
        step(0.3f); shot("name")
        tap("ok")
        if (game.save.name != "Viv") fail("name was saved as \"${game.save.name}\"")

        val first = game.unitList[0].id
        var played = 0
        for (info in game.unitList) {
            if (only.isNotEmpty() && !only.contains(info.id)) continue
            tag = "u${info.number}"
            if (info.id != first && !game.unlocked(info.id)) {
                // jumping ahead for a partial run: open every unit the way a parent would
                game.save.unlockAll = true
            }
            if (game.unit.id != info.id || game.screen !is HubScreen) {
                if (game.screen is StoryScreen && game.unit.id == info.id) { /* its opening story is already up */ }
                else Flow.openUnit(game, info.id)
            }
            val t0 = clock
            playUnit(game.unitById(info.id))
            played++
            println("[$tag] ${info.title}: done in ${"%.0f".format(clock - t0)} s of game time with instant answers")
        }

        // ---- the notebook overlay ----
        tag = "end"
        if (game.screen !is HubScreen) game.go(HubScreen(game))
        step(0.4f); shot("hub")
        tap("book")
        if (game.overlay !is NotebookScreen) fail("the notebook should open")
        step(0.2f); shot("notebook_words")
        while (has("prev")) tap("prev")
        step(0.2f); shot("notebook_words_first")
        tap("tab:1"); step(0.2f); shot("notebook_tips")
        tap("tab:2"); step(0.2f); shot("notebook_case")
        tap("close")
        if (game.overlay != null) fail("the notebook should close")

        // ---- with more than one case, the list of cases opens each of them ----
        if (game.cases.size > 1) {
            if (!has("cases")) fail("with ${game.cases.size} cases there should be a Cases button")
            tap("cases")
            step(0.3f); shot("cases")
            val firstCase = game.cases[0]
            tap("case:${firstCase.id}")
            if (game.screen is StoryScreen) story("intro")
            if (game.screen !is HubScreen || firstCase.units.none { it.id == game.unit.id }) fail("the first case should open: ${screen().describe()}")
            step(0.3f); shot("hub_first_case")
        } else if (has("cases")) fail("one case needs no Cases button")

        // ---- a saved game comes back ----
        val again = Game(FileAssets(files), DesktopAudio(null), store)
        if (again.save.name != "Viv") fail("the save did not load back")
        for (info in game.unitList) if (again.save.stage(info.id) != game.save.stage(info.id)) fail("stage of ${info.id} did not load back")
        if (again.save.met.size != game.save.met.size) fail("met words did not load back")

        // ---- every line in every unit needs a sound file, played or not ----
        var lines = 0
        val silent = ArrayList<String>()
        for (info in game.unitList) {
            val u = game.unitById(info.id)
            for (sp in u.spoken) {
                if (sp.say.isBlank()) continue
                lines++
                if (!game.voice.hasVoice(u.voiceFor(sp.who), sp.say)) silent.add("${u.id} ${u.voiceFor(sp.who)}: ${sp.say}")
            }
        }
        println("spoken lines in the unit files: $lines; voice clips played in this run: ${audio.log.size}")
        if (silent.isNotEmpty()) {
            val d = silent.distinct()
            println("LINES WITH NO SOUND FILE (${d.size}):")
            for (m in d.take(40)) println("  $m")
            if (needAudio) fail("run tools/make_audio.R to make the missing sound files")
        } else {
            for (path in audio.log.toSet()) if (files.open("$path.ogg") == null) fail("the list names $path.ogg but the file is missing")
            println("every line has a sound file")
        }
        println("words met: ${game.save.met.size}, bonus words found: ${game.save.found}")
        println("units played: $played, pictures saved: $shots")
        codeTest()
    }

    /** A lost save: the secret code for the last unit opens it, with every earlier unit done. */
    private fun codeTest() {
        tag = "code"
        // a saved name stays the same after a code
        game = Game(FileAssets(files), DesktopAudio(null), FileStore(null))
        game.save.name = "Brian"
        game.save.commit()
        repeat(5) { game.down(200f, 62f); game.up(200f, 62f) }
        if (game.screen !is CodeScreen) fail("five taps on the title should open the secret code screen")
        val mid = game.unitById(game.unitList[game.unitList.size / 2].id)
        for (ch in mid.code) tap("key:$ch")
        tap("ok")
        if (game.screen is StoryScreen) story("intro_named")
        if (game.screen !is HubScreen || game.unit.id != mid.id) fail("the code should open ${mid.id}: ${screen().describe()}")
        if (game.save.name != "Brian") fail("a code changed the saved name to \"${game.save.name}\"")
        println("[code] the code ${mid.code} opened ${mid.id} and kept the name ${game.save.name}")
        game = Game(FileAssets(files), DesktopAudio(null), FileStore(null))
        if (game.screen !is TitleScreen) fail("a new game should start on the title")
        repeat(5) { game.down(200f, 62f); game.up(200f, 62f) }
        if (game.screen !is CodeScreen) fail("five taps on the title should open the secret code screen")
        for (ch in "WRONG") tap("key:$ch")
        tap("ok")
        if (game.screen !is CodeScreen) fail("a wrong code should leave the code screen up")
        step(0.3f); shot("code_wrong")
        val lastInfo = game.unitList[game.unitList.size - 1]
        val target = game.unitById(lastInfo.id)
        for (ch in target.code) tap("key:$ch")
        step(0.2f); shot("code_typed")
        tap("ok")
        if (game.screen !is NameScreen) fail("with no name saved, a good code should ask for the name next")
        for (ch in "VIV") tap("key:$ch")
        tap("ok")
        if (game.screen is StoryScreen) story("intro")
        if (game.screen !is HubScreen || game.unit.id != target.id) fail("the code should open ${target.id}: ${screen().describe()}")
        step(0.3f); shot("code_hub")
        for (info in game.unitList) {
            if (info.id == target.id) break
            val u = game.unitById(info.id)
            if (!game.finished(u.id)) fail("the code should mark ${u.id} finished")
            for (w in u.words) if (!game.save.met.contains(w.id)) fail("the code should put ${w.id} back in the notebook")
        }
        if (!game.unlocked(target.id)) fail("${target.id} should be open after its code")
        println("[code] the code ${target.code} opened ${target.id} with every earlier unit done")
    }

    private fun playUnit(u: UnitData) {
        if (game.screen is StoryScreen) story("intro")
        if (game.screen !is HubScreen || game.unit.id != u.id) fail("expected the case file of ${u.id}, got: ${screen().describe()}")
        step(0.4f); shot("hub")
        if (u.stages.size > 1 && game.save.stage(u.id) == 0 && has("stage:1")) fail("stage 2 should be locked before stage 1 is done")
        tap("stage:" + Math.min(game.save.stage(u.id), u.stages.size - 1))
        var guard = 0
        while (true) {
            if (++guard > 400) fail("unit ${u.id} never ends: ${screen().describe()}")
            when (val s = game.screen) {
                is StoryScreen -> story("story")
                is DrillScreen -> notebook()
                is TalkScreen -> talk()
                is WalkScreen -> walk()
                is RideScreen -> ride()
                is SketchScreen -> sketch()
                is BlockScreen -> block()
                is BoulesScreen -> boules()
                is SpyScreen -> spy()
                is BeltScreen -> belt()
                is MetroScreen -> metro()
                is DressScreen -> dress()
                is StageDoneScreen -> {
                    val done = game.save.stage(u.id)
                    step(0.6f); shot("stage${done}_done")
                    if (done >= u.stages.size) {
                        if (game.nextUnit() != null) tap("go") else tap("stop")
                        return
                    }
                    tap("go")
                }
                else -> fail("did not expect this screen in the middle of a unit: ${s.describe()}")
            }
        }
    }

    private fun story(name: String) {
        var n = 0
        val scr = game.screen
        while (game.screen === scr) {
            step(0.8f)
            shotOnce("${name}_" + scr.describe().substringAfter("scene=") + "_" + scr.describe().substringAfter("Story ").substringBefore("/"))
            // tapping the bubble must never break anything
            game.down(260f, 45f); game.up(260f, 45f)
            tap("next")
            if (++n > 30) fail("story never ends")
        }
    }

    private fun notebook() {
        var questions = 0
        var guard = 0
        val scr = game.screen as DrillScreen
        while (game.screen === scr) {
            if (++guard > 3000) fail("notebook never ends")
            val serial = scr.serial
            if (scr.pageDoneShowing()) {
                shotOnce("drill_pagedone")
                tap("go")
                continue
            }
            if (scr.isBuild()) {
                shotOnce("drill_build")
                if (seen.add("$tag/drill_build_wrong")) {
                    // tiles in the wrong order, twice, to bring out the hint
                    for (k in scr.rightTiles().reversed()) tap("tile:$k")
                    tap("check"); step(0.4f); shot("drill_build_wrong"); step(1.2f)
                    for (k in scr.rightTiles().reversed()) tap("tile:$k")
                    tap("check"); step(1.6f); shot("drill_build_hint")
                }
                for (k in scr.rightTiles()) tap("tile:$k")
                step(0.2f)
                shotOnce("drill_build_filled")
                tap("check")
                waitFor("the tile task to finish") { game.screen !== scr || scr.serial != serial }
                continue
            }
            val right = scr.rightButton()
            val kind = scr.kindName()
            if (right == null) {
                // a word card, a card of several words, or a tip
                step(0.6f)
                shotOnce("drill_$kind")
                if (kind == "strip" && has("hear:0")) tap("hear:0")
                tap("next")
                continue
            }
            step(0.6f)
            shotOnce("drill_$kind")
            questions++
            // get every ninth question wrong on purpose
            if (questions % 9 == 4) {
                val wrong = (0..3).map { "opt:$it" }.firstOrNull { it != right && screen().button(it) != null }
                if (wrong != null) {
                    tap(wrong)
                    step(0.5f)
                    shotOnce("drill_wrong")
                    waitFor("the next task") { game.screen !== scr || scr.serial != serial }
                    continue
                }
            }
            tap(right)
            step(0.4f)
            shotOnce("drill_right_$kind")
            waitFor("the next task") { game.screen !== scr || scr.serial != serial }
        }
        println("[$tag] notebook questions answered: $questions")
    }

    private fun talk() {
        val tk = game.screen as TalkScreen
        val name = tk.describe().substringAfter("Talk ").substringBefore(' ')
        val missed = HashSet<String>()
        var first = true
        var guard = 0
        while (game.screen === tk) {
            if (++guard > 2000) fail("conversation never ends: ${tk.describe()}")
            if (tk.duelStep() != "") { duel(tk, name); continue }
            if (!tk.waiting() || tk.askKind() == "end") { step(0.1f); continue }
            val node = tk.nodeId()
            step(0.2f)
            shotOnce("talk_${name}_$node")
            // tapping the bubble shows or hides the help; it must never break anything
            // (screens that cover the bubble, such as putting steps in order, have buttons there instead)
            if (tk.askKind() !in listOf("order", "fact", "route", "plan")) { game.down(260f, 80f); game.up(260f, 80f) }
            if (missed.add(node)) {
                // one wrong answer per question; two on the first question, to see the hint
                var times = if (first) 2 else 1
                first = false
                while (times-- > 0 && game.screen === tk && tk.nodeId() == node) {
                    val wrong = tk.wrongTaps()
                    if (wrong.isEmpty()) break
                    for (id in wrong) { if (!has(id)) break; tap(id) }
                    step(0.4f)
                    if (tk.askKind() == "fact" || tk.askKind() == "route" || tk.askKind() == "plan") { shotOnce("talk_${name}_${node}_hint"); break }
                    waitFor("the reaction to a wrong answer") { game.screen !== tk || tk.reacting() || tk.waiting() }
                    step(0.3f)
                    shotOnce("talk_${name}_${node}_wrong")
                    waitFor("the question to come back") { game.screen !== tk || tk.waiting() }
                    step(0.2f)
                }
                if (game.screen !== tk || tk.nodeId() != node) continue
            }
            val taps = tk.rightTaps()
            if (taps.isEmpty()) fail("nothing to tap on: ${tk.describe()}")
            val before = tk.progress()
            for ((i, id) in taps.withIndex()) {
                tap(id)
                if (i == taps.size - 2) { step(0.2f); shotOnce("talk_${name}_${node}_filled") }
            }
            step(0.4f)
            if (tk.noted()) shotOnce("talk_${name}_${node}_noted")
            waitFor("the conversation to move on") { game.screen !== tk || tk.nodeId() != node || (tk.waiting() && tk.askKind() == "end") || (tk.waiting() && tk.progress() != before) }
        }
    }

    /**
     * Plays a duel. Each unit tries a different path: skipping hello first,
     * no microphone ("J'ai dit !"), only weak tries until the speaker gives in,
     * the microphone not allowed yet, and nothing heard the first time.
     */
    private fun duel(tk: TalkScreen, name: String) {
        val unitNo = game.unit.number
        ears.on = unitNo != 2
        ears.plan.clear()
        when (unitNo) {
            1 -> ears.plan.addAll(listOf("nothing", "strong"))
            3 -> for (i in 0 until 40) ears.plan.add("nothing")
            4 -> ears.plan.addAll(listOf("permission", "good"))
        }
        val path = when (unitNo) { 1 -> "rude first, then nothing heard"; 2 -> "no microphone"; 3 -> "weak tries until he gives in"; 4 -> "microphone not allowed yet"; else -> "best moves" }
        var guard = 0
        var moves = 0
        var winSteps = 0
        var hitShot = false
        var retryShot = false
        var won = false
        while (game.screen === tk && tk.duelStep() != "") {
            if (++guard > 4000) fail("duel never ends: ${tk.describe()}")
            when (tk.duelStep()) {
                "intro" -> { step(0.1f); if (tk.t > 1.5f) shotOnce("talk_${name}_duel_intro") }
                "win" -> {
                    step(0.1f)
                    won = true
                    if (++winSteps == 10) shotOnce("talk_${name}_duel_win")
                }
                "listen" -> step(0.1f)
                "busy" -> {
                    step(0.1f)
                    if (!hitShot && tk.duelDoubt() < 100) { step(0.2f); shotOnce("talk_${name}_duel_hit"); hitShot = true }
                }
                "moves" -> {
                    step(0.2f)
                    shotOnce("talk_${name}_duel_moves")
                    val kinds = tk.duelKinds()
                    val pick = when {
                        unitNo == 1 && moves == 0 && kinds.contains("greet") -> kinds.first { it != "greet" }
                        !tk.duelPolite() -> "greet"
                        unitNo == 3 -> kinds.first { it != "greet" && it != tk.duelBest() }
                        else -> tk.duelBest() ?: kinds.first { it != "greet" }
                    }
                    tap("move:$pick")
                    moves++
                    if (moves > 12) fail("duel $name: more than 12 moves")
                }
                "speak" -> {
                    step(0.2f)
                    if (tk.duelStatus().startsWith("Encore") || tk.duelStatus().startsWith("Le micro")) {
                        if (!retryShot) { shotOnce("talk_${name}_duel_again"); retryShot = true }
                    } else shotOnce("talk_${name}_duel_speak")
                    if (has("talk")) tap("talk") else tap("said")
                    step(0.1f)
                }
            }
        }
        if (!won) fail("duel $name ended without a win: ${tk.describe()}")
        println("[duel] unit $unitNo $name: won in $moves moves ($path)")
        ears.on = true
    }

    private fun ride() {
        val r = game.screen as RideScreen
        if (r.tipShowing()) { step(0.4f); shot("ride_tip"); tap("next") }
        var guard = 0
        var wrongDone = false
        while (game.screen === r) {
            if (++guard > 4000) fail("ride never ends: ${r.describe()}")
            if (r.arrived()) { step(0.5f); shot("ride_arrive"); tap("next"); continue }
            if (!r.waiting()) { step(0.1f); if (guard % 12 == 0) shotOnce("ride_drive_${r.legIndex()}"); continue }
            step(0.3f)
            shotOnce("ride_corner_${r.legIndex()}")
            if (!wrongDone) {
                wrongDone = true
                val w = r.wrongTap() ?: fail("no wrong answer on offer")
                tap(w)
                waitFor("the taxi to turn the wrong way") { r.describe().contains("phase=5") }
                step(1f); shot("ride_round_the_block")
                waitFor("the taxi to come back to the corner") { r.waiting() }
                continue
            }
            tap(r.rightTap())
            waitFor("the taxi to move on") { game.screen !== r || !r.waiting() }
        }
        if (r.wrongs != 1) fail("expected exactly one wrong turn, got ${r.wrongs}")
    }

    private fun sketch() {
        val s = game.screen as SketchScreen
        if (s.tipShowing()) { step(0.4f); shot("sketch_tip"); tap("next") }
        var guard = 0
        var wrongDone = false
        while (game.screen === s) {
            if (++guard > 4000) fail("sketch never ends: ${s.describe()}")
            if (!s.waiting()) { step(0.1f); continue }
            step(0.2f)
            shotOnce("sketch_start_" + s.describe().substringAfter("person=").substringBefore('/'))
            val taps = s.rightTaps()
            if (taps.size == 1 && taps[0] == "check") {
                if (!wrongDone) {
                    // put one wrong word in, and see that the officer notices
                    wrongDone = true
                    for (id in s.wrongTaps()) { waitFor("the sketch to be ready") { s.waiting() }; tap(id) }
                    waitFor("the sketch to be ready") { s.waiting() }
                    step(0.3f); shot("sketch_wrong_word")
                    tap("check")
                    step(0.6f); shot("sketch_officer_says_no")
                    continue
                }
                step(0.3f); shotOnce("sketch_right_" + s.describe().substringAfter("person=").substringBefore('/'))
                tap("check")
                step(0.5f)
                continue
            }
            for (id in taps) { waitFor("the sketch to be ready") { s.waiting() }; tap(id) }
        }
    }

    // ---------- small games ----------

    /** Close the how-to card, and any speech panel that is up. */
    private fun gameStart(s: MiniGame, name: String) {
        if (s.tipShowing()) { step(0.4f); shot("${name}_tip"); game.down(200f, 120f); game.up(200f, 120f) }
        step(0.4f); shotOnce("${name}_start")
    }

    private fun closePanels(s: MiniGame, name: String, n: Int) {
        if (s.dialogShowing()) { step(0.3f); shotOnce("${name}_says_$n"); tap("ok"); step(0.2f) }
    }

    private fun block() {
        val s = game.screen as BlockScreen
        gameStart(s, "block")
        // a wrong tap first: the alley cannot be filled
        var guard = 0
        while (game.screen === s) {
            if (++guard > 20000) fail("the protest never ends: ${s.describe()}")
            if (s.dialogShowing()) { closePanels(s, "block", guard / 200); continue }
            if (!s.isStuck() && s.blockedCount() < s.groupCount()) {
                for (id in s.rightTaps()) { if (has(id)) { tap(id); step(0.3f) } }
                shotOnce("block_filled")
            }
            step(0.1f)
            if (guard % 300 == 0) shotOnce("block_drive_${guard / 300}")
        }
        if (s.turns < 1) fail("the truck never turned a corner")
    }

    private fun boules() {
        val s = game.screen as BoulesScreen
        gameStart(s, "boules")
        var guard = 0
        while (game.screen === s) {
            if (++guard > 40000) fail("pétanque never ends: ${s.describe()}")
            if (s.dialogShowing()) { closePanels(s, "boules", s.rounds); continue }
            if (s.herTurn()) {
                val at = s.jackAt()
                game.down(at[0], at[1] + 3f)
                step(0.1f)
                shotOnce("boules_aim")
                game.up(at[0], at[1] + 3f)
                step(0.5f)
                shotOnce("boules_rolling")
                continue
            }
            step(0.1f)
        }
        println("[$tag] pétanque: ${s.rounds} rounds, ${s.throwsMade} throws")
    }

    private fun spy() {
        val s = game.screen as SpyScreen
        gameStart(s, "spy")
        val c = s.windowCentre()
        fun lookAt(wx: Float, wy: Float) {
            // drag the view until that spot is in the middle of the window
            var guard = 0
            while (Math.abs(wx - s.camX - c[0]) + Math.abs(wy - s.camY - c[1]) > 6f) {
                if (++guard > 40) break
                val dx = (wx - s.camX - c[0]).coerceIn(-150f, 150f)
                val dy = (wy - s.camY - c[1]).coerceIn(-100f, 100f)
                game.down(200f, 120f)
                game.move(200f - dx / 2f, 120f - dy / 2f)
                game.move(200f - dx, 120f - dy)
                game.up(200f - dx, 120f - dy)
                step(0.05f)
            }
        }
        val wrong = s.wrongSpot()
        if (wrong != null) {
            lookAt(wrong[0], wrong[1])
            step(0.3f); shot("spy_wrong_look")
            game.down(wrong[0] - s.camX, wrong[1] - s.camY); game.up(wrong[0] - s.camX, wrong[1] - s.camY)
            step(0.3f)
            if (!s.dialogShowing()) fail("a wrong vehicle should say something")
            shot("spy_wrong")
            tap("ok")
        }
        val right = s.rightSpot() ?: fail("the telescope has nothing to find")
        lookAt(right[0], right[1])
        step(0.3f); shot("spy_found_look")
        game.down(right[0] - s.camX, right[1] - s.camY); game.up(right[0] - s.camX, right[1] - s.camY)
        var guard = 0
        while (game.screen === s) {
            if (++guard > 200) fail("the telescope never ends: ${s.describe()}")
            if (s.dialogShowing()) { closePanels(s, "spy", guard); continue }
            step(0.1f)
        }
        if (wrong != null && s.wrongs != 1) fail("expected one wrong look, got ${s.wrongs}")
    }

    private fun belt() {
        val s = game.screen as BeltScreen
        gameStart(s, "belt")
        var guard = 0
        var wrongDone = false
        while (game.screen === s) {
            if (++guard > 20000) fail("the cheese belt never ends: ${s.describe()}")
            if (s.dialogShowing()) { closePanels(s, "belt", guard); continue }
            if (s.ready()) {
                if (!wrongDone) { wrongDone = true; tap(s.wrongTap() ?: fail("no wrong shelf")); step(0.3f); shot("belt_wrong"); continue }
                tap(s.rightTap() ?: fail("no right shelf"))
                step(0.2f); shotOnce("belt_fly")
                step(0.6f)
                continue
            }
            step(0.1f)
        }
        if (s.misses != 1) fail("expected one wrong shelf, got ${s.misses}")
    }

    private fun metro() {
        val s = game.screen as MetroScreen
        gameStart(s, "metro")
        var guard = 0
        var wrongDone = false
        var earlyDone = false
        var missedDone = false
        while (game.screen === s) {
            if (++guard > 40000) fail("the metro never ends: ${s.describe()}")
            if (s.dialogShowing()) { closePanels(s, "metro", guard); continue }
            when {
                s.atGate() -> { step(0.3f); shotOnce("metro_gate"); tap("reader"); step(0.4f); shotOnce("metro_gate_open") }
                s.onPlatform() -> {
                    step(0.3f); shotOnce("metro_platform_${s.tripIndex()}")
                    if (!wrongDone) {
                        // the wrong platform first: one stop the wrong way, then back
                        wrongDone = true
                        tap(s.wrongDir())
                        waitFor("the wrong-way stop") { s.stopped() }
                        step(0.4f); shot("metro_wrong_way")
                        waitFor("the way back to the platform") { s.onPlatform() }
                        if (s.wrongWays != 1) fail("expected one wrong way, got ${s.wrongWays}")
                        continue
                    }
                    tap(s.rightDir())
                    step(0.3f)
                }
                s.atTarget() -> {
                    if (s.tripIndex() == 1 && !missedDone) {
                        // let her stop go by, and see that the train comes back for her
                        missedDone = true
                        shotOnce("metro_target_passed")
                        waitFor("the train to leave her stop") { !s.atTarget() }
                        waitFor("the way back to her stop", 60f) { s.atTarget() }
                        if (s.missed != 1) fail("expected one missed stop, got ${s.missed}")
                        step(0.3f); shot("metro_back_at_stop")
                        continue
                    }
                    shotOnce("metro_target_${s.tripIndex()}")
                    tap("off")
                    step(0.6f); shotOnce("metro_change_${s.tripIndex()}")
                }
                s.stopped() -> {
                    if (!earlyDone) { earlyDone = true; shotOnce("metro_stop"); tap("off"); step(0.4f); shot("metro_too_early"); if (s.early != 1) fail("getting off too early should be noticed") }
                    step(0.1f)
                }
                s.atExit() -> { step(0.3f); shotOnce("metro_exit"); tap("sortie"); step(0.3f) }
                else -> step(0.1f)
            }
        }
    }

    private fun dress() {
        val s = game.screen as DressScreen
        gameStart(s, "dress")
        var guard = 0
        var wrongDone = false
        while (game.screen === s) {
            if (++guard > 20000) fail("dressing the models never ends: ${s.describe()}")
            if (s.dialogShowing()) { closePanels(s, "dress", guard); continue }
            if (!s.ready()) { step(0.1f); continue }
            step(0.2f)
            shotOnce("dress_model_" + s.describe().substringAfter("model=").substringBefore('/'))
            if (!wrongDone) { wrongDone = true; tap(s.wrongTap() ?: fail("no wrong item")); step(0.3f); shot("dress_wrong"); continue }
            tap(s.rightTap() ?: fail("no right item"))
            step(0.3f)
        }
        if (s.misses != 1) fail("expected one wrong item, got ${s.misses}")
    }

    // ---------- walking scenes ----------

    private fun tileOf(a: WalkScreen): IntArray = intArrayOf((a.hx / 16f).toInt(), ((a.hy - 3f) / 16f).toInt())

    /** Shortest way over open floor. People who would step aside for "Pardon !" do not count as walls. */
    private fun path(a: WalkScreen, toX: Int, toY: Int, avoid: Set<Int> = emptySet()): List<IntArray>? {
        val w = a.mapWidth()
        val h = a.mapHeight()
        val from = tileOf(a)
        if (toX < 0 || toY < 0 || toX >= w || toY >= h) return null
        val prev = IntArray(w * h) { -2 }
        val q = ArrayDeque<Int>()
        prev[from[1] * w + from[0]] = -1
        q.add(from[1] * w + from[0])
        while (q.isNotEmpty()) {
            val cur = q.poll()
            val cx = cur % w
            val cy = cur / w
            if (cx == toX && cy == toY) break
            for (d in arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))) {
                val nx = cx + d[0]
                val ny = cy + d[1]
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                if (a.wallAt(nx, ny) || prev[ny * w + nx] != -2) continue
                if (avoid.contains(ny * w + nx) && !(nx == toX && ny == toY)) continue
                prev[ny * w + nx] = cur
                q.add(ny * w + nx)
            }
        }
        if (prev[toY * w + toX] == -2) return null
        val out = ArrayList<IntArray>()
        var cur = toY * w + toX
        while (cur != -1) { out.add(0, intArrayOf(cur % w, cur / w)); cur = prev[cur] }
        return out
    }

    private fun hold(a: WalkScreen, wx: Float, wy: Float) {
        val sx = (wx - a.camX) * a.zoom
        val sy = (wy - a.camY - 5f) * a.zoom
        game.down(Math.max(1f, Math.min(Game.W - 1f, sx)), Math.max(32f, Math.min(Game.H - 1f, sy)))
    }

    /**
     * Take one small step towards a tile. Returns true once she stands on it.
     * With [careful], she waits whenever something is rolling close by.
     */
    private fun stepTowards(a: WalkScreen, toX: Int, toY: Int, careful: Boolean): Boolean {
        val here = tileOf(a)
        if (here[0] == toX && here[1] == toY && Math.abs(a.hx - (toX * 16f + 8f)) < 3f) { game.up(0f, 0f); return true }
        // like a player, go round things that roll about when there is room to
        val danger = HashSet<Int>()
        if (careful) for (m in a.moverSpots()) {
            val mtx = (m[0] / 16f).toInt()
            val mty = ((m[1] - 3f) / 16f).toInt()
            for (dx in -1..1) danger.add(mty * a.mapWidth() + mtx + dx)
        }
        if (careful) for (m in a.beamSpots()) {
            val mtx = (m[0] / 16f).toInt()
            val mty = ((m[1] - 3f) / 16f).toInt()
            for (dx in -1..1) for (dy in -1..1) danger.add((mty + dy) * a.mapWidth() + mtx + dx)
        }
        val route = (if (danger.isEmpty()) null else path(a, toX, toY, danger))
            ?: path(a, toX, toY) ?: fail("no way to walk to ($toX,$toY): ${a.describe()}")
        val nxt = if (route.size > 1) route[1] else route[0]
        if (a.blockerOn(nxt[0], nxt[1])) {
            // someone is standing in the way
            game.up(0f, 0f)
            if (!a.hasPardon()) fail("someone blocks the way at (${nxt[0]},${nxt[1]}) and there is no Pardon button")
            shotOnce("walk_blocked")
            tap("pardon")
            step(0.3f)
            shotOnce("walk_pardon")
            return false
        }
        val wx = nxt[0] * 16f + 8f
        val wy = nxt[1] * 16f + 13f
        var wait = false
        if (careful) for (m in a.moverSpots()) {
            val near = Math.abs(m[0] - wx) < 30f && Math.abs(m[1] - wy) < 26f
            val onMe = Math.abs(m[0] - a.hx) < 16f && Math.abs(m[1] - a.hy) < 12f
            if (near && !onMe) wait = true
        }
        // a beam close to the next tile: wait in the dark until it has gone by
        if (careful) for (m in a.beamSpots()) {
            if (Math.abs(m[0] - wx) < 22f && Math.abs(m[1] - wy) < 22f && !(Math.abs(m[0] - a.hx) < 10f && Math.abs(m[1] - a.hy) < 10f)) wait = true
        }
        if (wait) game.up(0f, 0f) else hold(a, wx, wy)
        step(1f / 30f)
        return false
    }

    /** Walk to a tile, closing any speech panels that open on the way. Stops early if the scene changes. */
    private fun walkTo(a: WalkScreen, toX: Int, toY: Int, careful: Boolean, stopAtDialog: Boolean = false): Boolean {
        var guard = 0
        while (game.screen === a) {
            if (++guard > 9000) fail("could not reach ($toX,$toY): ${a.describe()}")
            if (a.dialogShowing()) {
                game.up(0f, 0f)
                if (stopAtDialog) return true
                step(0.3f); tap("ok"); step(0.2f); continue
            }
            if (stepTowards(a, toX, toY, careful)) return true
        }
        return false
    }

    private fun push(a: WalkScreen, dx: Int, dy: Int, sec: Float = 0.6f) {
        game.keys(dx, dy)
        var left = sec
        while (left > 0f && game.screen === a && !a.dialogShowing()) { step(1f / 30f); left -= 1f / 30f }
        game.keys(0, 0)
    }

    /** Walk up to someone or something and push against it until it speaks. */
    private fun bumpInto(a: WalkScreen, toX: Int, toY: Int, dx: Int, dy: Int, what: String, shotName: String) {
        walkTo(a, toX, toY, true, true)
        if (!a.dialogShowing()) push(a, dx, dy)
        if (!a.dialogShowing()) fail("$what should say something: ${a.describe()}")
        step(0.3f); shot(shotName); tap("ok")
    }

    private fun walk() {
        val a = game.screen as WalkScreen
        val name = a.describe().substringAfter("Walk ").substringBefore(' ')
        if (a.tipShowing()) {
            step(0.5f); shot("walk_${name}_tip")
            game.down(200f, 120f); game.up(200f, 120f)
            if (a.tipShowing()) fail("a tap should close the tip")
        }
        step(0.3f); shotOnce("walk_${name}_start")
        if (game.unit.id == "unit1" && name == "airport" && seen.add("airport-extras")) airportExtras(a)
        if (a.beamSpots().isNotEmpty() && seen.add("caught-$name")) {
            // walk straight into a guard's beam once, to see her sent back
            var tries = 0
            while (a.caughtTimes == 0 && tries++ < 900 && game.screen === a) {
                if (a.dialogShowing()) { tap("ok"); continue }
                val b = a.beamSpots()[2]
                hold(a, b[0], b[1])
                step(1f / 30f)
            }
            game.up(0f, 0f)
            if (a.caughtTimes == 0) fail("walked into a beam but was never seen")
            step(0.3f); shot("walk_${name}_caught")
            step(2.5f)
        }
        var guard = 0
        var lastStep = -1
        var wrongSeekDone = false
        while (game.screen === a) {
            if (++guard > 40000) fail("walking scene never ends: ${a.describe()}")
            if (a.dialogShowing()) {
                game.up(0f, 0f)
                step(0.3f)
                shotOnce("walk_${name}_says_${a.questIndex}_" + (guard / 50))
                // tapping the panel shows or hides the help; it must never break anything
                game.down(120f, 180f); game.up(120f, 180f)
                tap("ok"); step(0.2f)
                continue
            }
            if (a.questIndex != lastStep) {
                lastStep = a.questIndex
                step(0.2f); shot("walk_${name}_step${a.questIndex}")
            }
            if (!wrongSeekDone) {
                val wp = a.wrongPlace()
                if (wp != null) {
                    // go to a wrong hiding place once, to see the clerk put her right
                    wrongSeekDone = true
                    walkTo(a, wp[0], wp[1], true, true)
                    if (a.dialogShowing()) { step(0.3f); shot("walk_${name}_wrong_place") }
                    continue
                }
            }
            if (a.isCarrying() && guard % 40 == 0) shotOnce("walk_${name}_carry")
            val goal = a.goal()
            if (goal == null) { game.up(0f, 0f); step(0.1f); continue }
            if (stepTowards(a, goal[0], goal[1], true)) {
                if (goal[2] != 0 || goal[3] != 0) push(a, goal[2], goal[3], 0.5f)
                else step(1f / 30f)
            }
        }
        game.up(0f, 0f)
        game.keys(0, 0)
        if (game.screen is TalkScreen) {
            // a person in the scene started a conversation; play it, then carry on walking
            talk()
            if (game.screen === a) walk()
        }
    }

    /** Extra tests in the Unit 1 airport: side trips, a bump, a wrong gate, and stopping at a star. */
    private fun airportExtras(a0: WalkScreen) {
        var a = a0
        tap("ticket"); step(0.2f); shot("walk_ticket")
        game.down(200f, 120f); game.up(200f, 120f)

        bumpInto(a, 6, 15, 0, -1, "the desk agent", "walk_claire")
        bumpInto(a, 17, 15, 0, -1, "the toilet door", "walk_toilets")
        if (!game.save.found.contains("toilettes")) fail("toilettes should be in the notebook")

        // walk straight into the first suitcase to test a bump
        val before = a.bumps
        walkTo(a, 19, 16, false)
        var tries = 0
        while (a.bumps == before && tries++ < 600) {
            hold(a, 23 * 16f, 16 * 16f + 13f); step(1f / 30f)
            if (tileOf(a)[0] >= 22) { game.up(0f, 0f); walkTo(a, 19, 16, false) }
        }
        if (a.bumps == before) fail("never bumped the suitcase")
        shot("walk_bump")
        game.up(0f, 0f); step(2.2f)
        val after = tileOf(a)
        println("[$tag] bump: back to tile (${after[0]},${after[1]})")

        walkTo(a, 26, 17, true)
        step(0.5f); shot("walk_signpost")

        bumpInto(a, 26, 24, 0, 1, "the guard", "walk_guard")
        if (!game.save.found.contains("sortie")) fail("sortie should be in the notebook")
        bumpInto(a, 35, 15, 0, -1, "the gate 5 agent", "walk_wrong_gate")

        // stop here, and come back: the star should be remembered
        val star = game.save.checkpoints["unit1/airport"] ?: fail("no star was touched on the way")
        tap("menu"); step(0.2f); shot("walk_menu"); tap("leave")
        if (game.screen !is HubScreen) fail("Stop for now should go to the case file")
        // tap the card of the stage that holds this walk
        val walkStage = game.unit.stages.indexOfFirst { st -> st.steps.any { it.kind == "walk" && it.ref == "airport" } }
        tap("stage:$walkStage")
        story("toGate2")
        a = game.screen as? WalkScreen ?: fail("expected the airport again")
        val t = tileOf(a)
        if ("${t[0]},${t[1]}" != star) fail("should restart at star $star, not (${t[0]},${t[1]})")
        println("[$tag] stopped and came back at star $star")
        // walk() sees that the screen has changed and returns; the unit loop then plays the new one
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if (args.size < 2) { println("usage: Check <assets folder> <output folder> [--no-audio] [unit ids]"); return }
            val rest = args.drop(2)
            val only = rest.filter { !it.startsWith("--") }.toSet()
            Check(File(args[0]), File(args[1]), only, !rest.contains("--no-audio")).run()
            println("CHECK PASSED")
        }
    }
}

/** Pretend ears for the check: each try gives the next result from [plan] ("strong" when it runs out). */
class FakeEars : Ears {
    val plan = ArrayList<String>()
    var on = true
    val log = ArrayList<String>()
    override fun ready() = on
    override fun listen(phrase: String, words: List<String>, done: (Heard) -> Unit) {
        val what = if (plan.isEmpty()) "strong" else plan.removeAt(0)
        done(
            when (what) {
                "strong" -> Heard(words.map { HeardWord(it, 0.95f) })
                "good" -> Heard(words.take(Math.max(1, Math.ceil(words.size * 0.7).toInt())).map { HeardWord(it, 0.6f) })
                "nothing" -> Heard(emptyList())
                "permission" -> Heard(emptyList(), "permission")
                else -> Heard(emptyList(), "none")
            }
        )
    }
    override fun stop() {}
    override fun log(line: String) { log.add(line) }
}
