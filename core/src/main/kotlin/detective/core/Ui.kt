package detective.core

/** Colours used across the game. */
object C {
    val INK = 0xFF1D2B53.toInt()
    val INK2 = 0xFF3B4A7A.toInt()
    val NIGHT = 0xFF141B33.toInt()
    val PAPER = 0xFFF8F0DC.toInt()
    val PAPER2 = 0xFFE9DDBF.toInt()
    val RULE = 0xFFB9D3E6.toInt()
    val MUSTARD = 0xFFF2B233.toInt()
    val MUSTARD_D = 0xFFC98A14.toInt()
    val GREEN = 0xFF3A9E6A.toInt()
    val GREEN_D = 0xFF27774C.toInt()
    val RED = 0xFFD9534F.toInt()
    val RED_D = 0xFFA83A37.toInt()
    val SKY = 0xFF9FD3F2.toInt()
    val WHITE = 0xFFFFFFFF.toInt()
    val BLACK = 0xFF000000.toInt()
    val GREY = 0xFF7C8196.toInt()
    val GREY_L = 0xFFD8DBE6.toInt()
    val BROWN = 0xFF8A5A2B.toInt()

    fun alpha(c: Int, a: Float): Int {
        val al = (a.coerceIn(0f, 1f) * ((c ushr 24) and 255)).toInt()
        return (al shl 24) or (c and 0xFFFFFF)
    }
}

object Style {
    const val PRIMARY = 0   // mustard
    const val PLAIN = 1     // white card
    const val DARK = 2      // navy
    const val GOOD = 3      // green
    const val BAD = 4       // red
    const val GHOST = 5     // no fill, used on dark backgrounds
}

/** One tappable thing. Screens keep a list of these and the base class handles the taps. */
class Button(
    val id: String,
    var x: Float, var y: Float, var w: Float, var h: Float,
    var label: String,
    var style: Int = Style.PLAIN,
    var onTap: () -> Unit = {}
) {
    var visible = true
    var enabled = true
    var pressed = false
    var font = Font.BOLD
    var size = 12f
    var align = Align.CENTER
    /** A number a screen can hang on a button, such as which item it stands for. */
    var tag = 0
    /** Draws a small picture at the left of the label, or alone if the label is empty. */
    var icon: String? = null
    /** Push the label right, e.g. to leave room for a speaker button drawn on top. */
    var padLeft = 0f
    /** Icon size in game units; 0 picks a size from the button height. */
    var iconSize = 0f
    var shake = 0f

    fun contains(px: Float, py: Float): Boolean = px >= x && px <= x + w && py >= y && py <= y + h

    fun draw(p: Painter) {
        if (!visible) return
        var dx = 0f
        if (shake > 0f) dx = (Math.sin(shake * 40.0) * 3.0 * shake).toFloat()
        val bx = x + dx
        val fill: Int
        val edge: Int
        val ink: Int
        when (style) {
            Style.PRIMARY -> { fill = C.MUSTARD; edge = C.MUSTARD_D; ink = C.INK }
            Style.DARK -> { fill = C.INK; edge = C.NIGHT; ink = C.WHITE }
            Style.GOOD -> { fill = C.GREEN; edge = C.GREEN_D; ink = C.WHITE }
            Style.BAD -> { fill = C.RED; edge = C.RED_D; ink = C.WHITE }
            Style.GHOST -> { fill = 0x33FFFFFF; edge = 0x66FFFFFF; ink = C.WHITE }
            else -> { fill = C.WHITE; edge = C.GREY_L; ink = C.INK }
        }
        val a = if (enabled) 1f else 0.45f
        val r = Math.min(6f, h / 3f)
        val down = if (pressed && enabled) 1.5f else 0f
        if (style != Style.GHOST) p.fillRoundRect(bx, y + 2f, w, h, r, C.alpha(edge, a))
        p.fillRoundRect(bx, y + down, w, h, r, C.alpha(fill, a))
        if (style == Style.PLAIN || style == Style.GHOST) p.strokeRoundRect(bx, y + down, w, h, r, 1f, C.alpha(edge, a))

        var textLeft = bx + 6f + padLeft
        var textW = w - 12f - padLeft
        val ic = icon
        if (ic != null) {
            val s = if (iconSize > 0f) iconSize else Math.min(h - 6f, 20f)
            if (label.isEmpty()) {
                Icons.draw(p, ic, bx + (w - s) / 2f, y + down + (h - s) / 2f, s, ink)
                return
            }
            Icons.draw(p, ic, bx + 5f, y + down + (h - s) / 2f, s, ink)
            textLeft = bx + 8f + s
            textW = w - 12f - s
        }
        if (label.isEmpty()) return
        // shrink long labels, then wrap to two lines if they still do not fit
        var sz = size
        while (sz > 8f && p.textWidth(label, sz, font) > textW && Text.wrap(p, label, sz, font, textW).size > 2) sz -= 0.5f
        val lines = Text.wrap(p, label, sz, font, textW)
        val lh = sz * 1.2f
        var by = y + down + h / 2f - (lines.size - 1) * lh / 2f + sz * 0.36f
        for (ln in lines) {
            when (align) {
                Align.LEFT -> p.text(ln, textLeft, by, sz, C.alpha(ink, a), font, Align.LEFT)
                else -> p.text(ln, textLeft + textW / 2f, by, sz, C.alpha(ink, a), font, Align.CENTER)
            }
            by += lh
        }
    }
}

object Text {
    private const val NBSP = ' '

    /**
     * Tidy French text for the screen: the space French puts before ? ! : ;
     * becomes unbreakable, and ' becomes a curly apostrophe.
     */
    fun french(s: String): String {
        if (s.indexOf(' ') < 0 && s.indexOf('\'') < 0) return s
        val sb = StringBuilder(s.length)
        for (i in s.indices) {
            val c = s[i]
            if (c == '\'') { sb.append('\u2019'); continue }
            if (c == ' ') {
                val next = if (i + 1 < s.length) s[i + 1] else ' '
                val prev = if (i > 0) s[i - 1] else ' '
                if (next == '?' || next == '!' || next == ':' || next == ';' || next == '»' || prev == '«') {
                    sb.append(NBSP)
                    continue
                }
            }
            sb.append(c)
        }
        return sb.toString()
    }

    fun wrap(p: Painter, s: String, size: Float, font: Int, maxW: Float): List<String> {
        val out = ArrayList<String>()
        for (para in s.split('\n')) {
            val words = para.split(' ')
            var line = ""
            for (w in words) {
                if (w.isEmpty()) continue
                val trial = if (line.isEmpty()) w else "$line $w"
                if (line.isNotEmpty() && p.textWidth(trial, size, font) > maxW) {
                    out.add(line)
                    line = w
                } else line = trial
            }
            out.add(line)
        }
        return out
    }

    /** Draw wrapped text with its top edge at [top]. Returns the y just under the last line. */
    fun block(
        p: Painter, s: String, x: Float, top: Float, maxW: Float, size: Float, color: Int,
        font: Int = Font.TEXT, align: Int = Align.LEFT, lineH: Float = 1.3f
    ): Float {
        val lines = wrap(p, s, size, font, maxW)
        var y = top + size * 0.82f
        for (ln in lines) {
            val tx = when (align) {
                Align.CENTER -> x + maxW / 2f
                Align.RIGHT -> x + maxW
                else -> x
            }
            p.text(ln, tx, y, size, color, font, align)
            y += size * lineH
        }
        return top + lines.size * size * lineH
    }

    fun height(p: Painter, s: String, maxW: Float, size: Float, font: Int = Font.TEXT, lineH: Float = 1.3f): Float =
        wrap(p, s, size, font, maxW).size * size * lineH

    /** One line, vertically centred on [cy]. */
    fun line(p: Painter, s: String, x: Float, cy: Float, size: Float, color: Int, font: Int = Font.TEXT, align: Int = Align.LEFT) {
        p.text(s, x, cy + size * 0.36f, size, color, font, align)
    }
}

/** Shared drawing for backgrounds and panels. */
object Draw {
    /** The detective's notebook page: cream paper, blue rules, a red margin and a spiral. */
    fun notebookPage(p: Painter) {
        p.fillRect(0f, 0f, Game.W, Game.H, C.PAPER)
        var y = 34f
        while (y < Game.H) {
            p.fillRect(0f, y, Game.W, 0.6f, C.RULE)
            y += 14f
        }
        p.fillRect(22f, 0f, 0.8f, Game.H, C.alpha(C.RED, 0.5f))
        var hy = 10f
        while (hy < Game.H) {
            p.fillOval(6f, hy, 8f, 8f, C.INK)
            p.fillRoundRect(-4f, hy + 2.5f, 12f, 3f, 1.5f, C.GREY)
            hy += 22f
        }
    }

    fun nightSky(p: Painter, t: Float) {
        p.fillRect(0f, 0f, Game.W, Game.H, C.NIGHT)
        p.fillRect(0f, 120f, Game.W, 120f, 0xFF1B2547.toInt())
        // a few fixed stars that twinkle
        var seed = 7
        for (i in 0 until 40) {
            seed = seed * 1103515245 + 12345
            val sx = ((seed ushr 8) and 0xFFFF) % 400
            seed = seed * 1103515245 + 12345
            val sy = ((seed ushr 8) and 0xFFFF) % 130
            val tw = 0.55f + 0.45f * Math.sin((t * 1.5f + i).toDouble()).toFloat()
            p.fillRect(sx.toFloat(), sy.toFloat(), 1.4f, 1.4f, C.alpha(C.WHITE, tw))
        }
        p.fillOval(350f, 10f, 28f, 28f, 0xFFFDF3C4.toInt())
        p.fillOval(358f, 8f, 25f, 25f, C.NIGHT)
    }

    /** A row of Paris rooftops with the Eiffel Tower, as flat shapes. */
    fun skyline(p: Painter, baseY: Float, color: Int) { skylineAt(p, 0f, baseY, color) }

    fun skylineAt(p: Painter, x0: Float, baseY: Float, color: Int) {
        val widths = intArrayOf(26, 18, 30, 22, 16, 34, 20, 28, 24, 18, 30, 22, 26, 20, 28, 24)
        val heights = intArrayOf(34, 46, 30, 52, 38, 28, 44, 36, 50, 32, 40, 30, 48, 36, 42, 34)
        var x = x0
        for (i in widths.indices) {
            val w = widths[i].toFloat()
            val h = heights[i].toFloat()
            p.fillRect(x, baseY - h, w + 1f, h, color)
            // mansard roof
            p.fillPoly(floatArrayOf(x, baseY - h, x + 3f, baseY - h - 6f, x + w - 3f, baseY - h - 6f, x + w, baseY - h), color)
            // chimney
            p.fillRect(x + w * 0.6f, baseY - h - 10f, 3f, 6f, color)
            x += w
        }
        // the tower
        val tx = x0 + 96f
        p.fillPoly(floatArrayOf(tx - 22f, baseY, tx - 5f, baseY - 62f, tx + 5f, baseY - 62f, tx + 22f, baseY), color)
        p.fillPoly(floatArrayOf(tx - 5f, baseY - 62f, tx - 1.5f, baseY - 118f, tx + 1.5f, baseY - 118f, tx + 5f, baseY - 62f), color)
        p.fillRect(tx - 0.6f, baseY - 128f, 1.2f, 12f, color)
        p.fillRect(tx - 13f, baseY - 36f, 26f, 3f, color)
        p.fillRect(tx - 7f, baseY - 64f, 14f, 3f, color)
    }

    /** A speech bubble with a tail pointing left at [tailY]. */
    fun bubble(p: Painter, x: Float, y: Float, w: Float, h: Float, tailY: Float, fill: Int = C.WHITE) {
        p.fillRoundRect(x + 1f, y + 2f, w, h, 8f, C.alpha(C.BLACK, 0.18f))
        p.fillRoundRect(x, y, w, h, 8f, fill)
        p.fillPoly(floatArrayOf(x + 1f, tailY - 6f, x - 10f, tailY + 2f, x + 1f, tailY + 6f), fill)
        p.strokeRoundRect(x, y, w, h, 8f, 1f, C.INK)
        p.fillPoly(floatArrayOf(x + 0.6f, tailY - 5f, x - 8.5f, tailY + 1.6f, x + 0.6f, tailY + 5f), fill)
        p.line(x, tailY - 6f, x - 10f, tailY + 2f, 1f, C.INK)
        p.line(x - 10f, tailY + 2f, x, tailY + 6f, 1f, C.INK)
    }

    fun card(p: Painter, x: Float, y: Float, w: Float, h: Float, fill: Int = C.WHITE, r: Float = 8f) {
        p.fillRoundRect(x + 1f, y + 2.5f, w, h, r, C.alpha(C.BLACK, 0.2f))
        p.fillRoundRect(x, y, w, h, r, fill)
    }

    /** A rubber stamp, tilted look made from two rings and bold text. */
    fun stamp(p: Painter, cx: Float, cy: Float, r: Float, text: String, color: Int) {
        p.fillOval(cx - r, cy - r, r * 2, r * 2, C.alpha(color, 0.12f))
        p.strokeRoundRect(cx - r, cy - r, r * 2, r * 2, r, 2f, color)
        p.strokeRoundRect(cx - r + 3.5f, cy - r + 3.5f, r * 2 - 7f, r * 2 - 7f, r - 3.5f, 0.8f, color)
        Text.line(p, text, cx, cy, r * 0.62f, color, Font.DISPLAY, Align.CENTER)
    }

    fun arrow(p: Painter, cx: Float, cy: Float, size: Float, dir: String, color: Int) {
        val s = size / 2f
        val pts = when (dir) {
            "up" -> floatArrayOf(cx, cy - s, cx + s, cy, cx + s * 0.4f, cy, cx + s * 0.4f, cy + s, cx - s * 0.4f, cy + s, cx - s * 0.4f, cy, cx - s, cy)
            "down" -> floatArrayOf(cx, cy + s, cx + s, cy, cx + s * 0.4f, cy, cx + s * 0.4f, cy - s, cx - s * 0.4f, cy - s, cx - s * 0.4f, cy, cx - s, cy)
            "left" -> floatArrayOf(cx - s, cy, cx, cy - s, cx, cy - s * 0.4f, cx + s, cy - s * 0.4f, cx + s, cy + s * 0.4f, cx, cy + s * 0.4f, cx, cy + s)
            else -> floatArrayOf(cx + s, cy, cx, cy - s, cx, cy - s * 0.4f, cx - s, cy - s * 0.4f, cx - s, cy + s * 0.4f, cx, cy + s * 0.4f, cx, cy + s)
        }
        p.fillPoly(pts, color)
    }
}
