package detective.core

/**
 * The notebook, opened on top of any scene. It lists every word the player
 * has met in any unit (tap one to hear it), the tips, and what is known about
 * the case, one unit to a page.
 */
class NotebookScreen(g: Game) : Screen(g) {
    private var tab = 0
    private var page = -1
    private val perPage = 12

    override fun enter() { rebuild() }

    private fun words(): List<Word> {
        val out = ArrayList<Word>()
        for (info in g.unitList) {
            if (!g.unlocked(info.id)) continue
            val u = g.unitById(info.id)
            for (w in u.words) if (g.save.met.contains(w.id)) out.add(w)
            for (w in u.bonus) if (g.save.found.contains(w.id)) out.add(w)
        }
        return out
    }

    private fun tips(): List<Tip> {
        val out = ArrayList<Tip>()
        for (info in g.unitList) {
            if (!g.unlocked(info.id)) continue
            for (tp in g.unitById(info.id).tips) if (g.save.tips.contains(tp.id)) out.add(tp)
        }
        return out
    }

    /** Units the player has opened, for the Case tab. */
    private fun opened(): List<UnitData> {
        val out = ArrayList<UnitData>()
        for (info in g.unitList) {
            if (info.id == g.unit.id || g.save.intros.contains(info.id) || g.save.stage(info.id) > 0) out.add(g.unitById(info.id))
        }
        if (out.isEmpty()) out.add(g.unit)
        return out
    }

    private fun rebuild() {
        buttons.clear()
        val names = arrayOf("Words", "Tips", "Case")
        for (i in names.indices) {
            val b = add(Button("tab:$i", 34f + i * 70f, 12f, 66f, 24f, names[i], if (i == tab) Style.DARK else Style.PLAIN) { tab = i; page = -1; rebuild() })
            b.size = 11f
        }
        add(Button("close", 352f, 12f, 34f, 24f, "", Style.BAD) { g.audio.stopVoice(); g.closeOverlay() }).icon = "cross"
        var pages = 1
        if (tab == 0) {
            val ws = words()
            pages = Math.max(1, (ws.size + perPage - 1) / perPage)
            // open on the newest words
            if (page < 0 || page >= pages) page = pages - 1
            val from = page * perPage
            for (k in from until Math.min(ws.size, from + perPage)) {
                val w = ws[k]
                val i = k - from
                val col = i % 2
                val row = i / 2
                add(Button("word:${w.id}", 32f + col * 180f, 44f + row * 27f, 174f, 24f, "", Style.PLAIN) { g.voice.say("teacher", w.say) })
            }
        } else if (tab == 1) {
            pages = Math.max(1, tips().size)
            if (page < 0 || page >= pages) page = pages - 1
        } else {
            val us = opened()
            pages = us.size
            if (page < 0 || page >= pages) {
                page = pages - 1
                for (i in us.indices) if (us[i].id == g.unit.id) page = i
            }
        }
        if (pages > 1) {
            add(Button("prev", 150f, 208f, 40f, 22f, "", Style.PLAIN) { if (page > 0) { page--; rebuild() } }).also { it.icon = "back"; it.enabled = page > 0 }
            add(Button("more", 230f, 208f, 40f, 22f, "", Style.PLAIN) { if (page < pages - 1) { page++; rebuild() } }).also { it.icon = "next"; it.enabled = page < pages - 1 }
        }
    }

    override fun draw(p: Painter) {
        p.fillRect(0f, 0f, Game.W, Game.H, C.alpha(C.NIGHT, 0.75f))
        p.fillRoundRect(22f, 6f, 372f, 230f, 6f, C.INK)
        p.pushClip(26f, 9f, 364f, 224f)
        p.fillRect(26f, 9f, 364f, 224f, C.PAPER)
        var y = 42f
        while (y < 233f) { p.fillRect(26f, y, 364f, 0.6f, C.RULE); y += 13.5f }
        p.popClip()

        if (tab == 0) {
            val ws = words()
            if (ws.isEmpty()) Text.block(p, "No words yet. They appear here as you learn them.", 60f, 100f, 300f, 12f, C.INK2, Font.TEXT, Align.CENTER)
            drawButtons(p)
            val from = page * perPage
            for (k in from until Math.min(ws.size, from + perPage)) {
                val w = ws[k]
                val b = button("word:${w.id}") ?: continue
                Icons.draw(p, "speaker", b.x + 5f, b.y + 5f, 14f, C.INK2)
                var size = 10.5f
                while (size > 8f && p.textWidth(w.fr, size, Font.BOLD) > 144f) size -= 0.5f
                Text.line(p, w.fr, b.x + 26f, b.y + 8f, size, C.INK, Font.BOLD)
                var es = 8.5f
                while (es > 7f && p.textWidth(w.en, es, Font.TEXT) > 144f) es -= 0.5f
                Text.line(p, w.en, b.x + 26f, b.y + 18f, es, C.GREY, Font.TEXT)
            }
            if (ws.size > perPage) Text.line(p, "${page + 1} / ${(ws.size + perPage - 1) / perPage}", 210f, 219f, 10f, C.INK2, Font.BOLD, Align.CENTER)
            return
        }
        if (tab == 1) {
            val ts = tips()
            if (ts.isEmpty()) Text.block(p, "No tips yet.", 60f, 100f, 300f, 12f, C.INK2, Font.TEXT, Align.CENTER)
            else {
                val tp = ts[Math.min(Math.max(0, page), ts.size - 1)]
                Icons.draw(p, "loupe", 40f, 52f, 40f)
                Text.line(p, tp.title, 90f, 66f, 18f, C.INK, Font.DISPLAY)
                Text.block(p, Text.french(tp.text), 90f, 84f, 284f, 12f, C.INK, Font.TEXT, Align.LEFT, 1.35f)
                if (ts.size > 1) Text.line(p, "${page + 1} / ${ts.size}", 210f, 219f, 10f, C.INK2, Font.BOLD, Align.CENTER)
            }
            drawButtons(p)
            return
        }
        // the case so far, one unit to a page
        val us = opened()
        val u = us[Math.min(Math.max(0, page), us.size - 1)]
        Text.line(p, "Detective: ${g.save.name}", 40f, 54f, 14f, C.INK, Font.HAND)
        Text.line(p, "${u.number}. ${u.title}", 384f, 54f, 10f, C.INK2, Font.BOLD, Align.RIGHT)
        var cy = 66f
        for (s in u.caseNotes) cy = Text.block(p, "• " + g.fill(s), 40f, cy, 336f, 12.5f, C.INK, Font.HAND, Align.LEFT, 1.15f) + 2f
        for (k in g.save.facts.keys) {
            if (!k.startsWith(u.id + "/")) continue
            cy = Text.block(p, "• ${g.save.facts[k]}", 40f, cy, 336f, 12.5f, C.RED_D, Font.HAND, Align.LEFT, 1.15f) + 2f
        }
        if (us.size > 1) Text.line(p, "${page + 1} / ${us.size}", 210f, 219f, 10f, C.INK2, Font.BOLD, Align.CENTER)
        drawButtons(p)
    }

    override fun back(): Boolean = false

    override fun describe() = "Notebook tab=$tab page=$page words=${words().size}"
}
