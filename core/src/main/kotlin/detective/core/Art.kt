package detective.core

/**
 * Pixel art is stored as text in assets/art/sprites.txt so it can be edited
 * without an image editor. Format:
 *
 *   pal k 22223b        one letter = one colour (hex RGB, or 8 digits with alpha first)
 *   img hero_down_0     then one line of letters per pixel row; '.' is see-through
 *   ................
 *
 * A blank line ends a picture. A pal line holds for every picture after it,
 * until another pal line gives that letter a new colour. So a picture can
 * bring its own colours by putting pal lines just above its img line.
 */
class Art(src: String) {
    private var palette = HashMap<Char, Int>()
    private val palettes = HashMap<String, HashMap<Char, Int>>()
    private val grids = HashMap<String, Array<String>>()
    private val cache = HashMap<String, Img>()

    init {
        palette['.'] = 0
        var name: String? = null
        val rows = ArrayList<String>()
        var lineNo = 0
        // true once a picture uses the current palette; the next pal line then starts a copy
        var shared = false
        fun flush() {
            val n = name ?: return
            if (rows.isEmpty()) throw IllegalArgumentException("sprite $n has no rows")
            val w = rows[0].length
            for (r in rows) {
                if (r.length != w) throw IllegalArgumentException("sprite $n: row \"$r\" is ${r.length} wide, expected $w")
            }
            grids[n] = rows.toTypedArray()
            palettes[n] = palette
            shared = true
            rows.clear()
            name = null
        }
        for (raw in src.split('\n')) {
            lineNo++
            val line = raw.trimEnd('\r', ' ', '\t')
            if (name != null) {
                if (line.isEmpty()) { flush(); continue }
                rows.add(line)
                continue
            }
            if (line.isEmpty() || line.startsWith("#")) continue
            val parts = line.split(' ')
            if (parts[0] == "pal" && parts.size >= 3) {
                if (shared) { palette = HashMap(palette); shared = false }
                palette[parts[1][0]] = parseColor(parts[2])
            } else if (parts[0] == "img" && parts.size >= 2) {
                name = parts[1]
            } else {
                throw IllegalArgumentException("sprites.txt line $lineNo: cannot read \"$line\"")
            }
        }
        flush()
    }

    fun has(name: String): Boolean = grids.containsKey(name)

    fun names(): List<String> = ArrayList(grids.keys).sorted()

    /** The picture called [name]. Missing names give a pink square so the gap is obvious. */
    fun get(name: String): Img {
        cache[name]?.let { return it }
        val img = build(name, null)
        cache[name] = img
        return img
    }

    /**
     * The same picture with some palette letters swapped for other colours,
     * e.g. a guard is the hero drawing with a blue coat. [key] names the variant.
     */
    fun variant(name: String, key: String, swaps: Map<Char, Int>): Img {
        val k = "$name@$key"
        cache[k]?.let { return it }
        val img = build(name, swaps)
        cache[k] = img
        return img
    }

    private fun build(name: String, swaps: Map<Char, Int>?): Img {
        val g = grids[name]
        if (g == null) {
            val px = IntArray(16 * 16)
            for (i in px.indices) px[i] = 0xFFFF00FF.toInt()
            return Img(16, 16, px)
        }
        val w = g[0].length
        val h = g.size
        val px = IntArray(w * h)
        val pal = palettes[name] ?: palette
        for (y in 0 until h) {
            val row = g[y]
            for (x in 0 until w) {
                val c = row[x]
                val col = swaps?.get(c) ?: pal[c]
                    ?: throw IllegalArgumentException("sprite $name uses letter '$c' with no pal line")
                px[y * w + x] = col
            }
        }
        return Img(w, h, px)
    }

    companion object {
        fun parseColor(hex: String): Int {
            val h = hex.removePrefix("#")
            val v = h.toLong(16)
            return if (h.length <= 6) (0xFF000000L or v).toInt() else v.toInt()
        }
    }
}

/** A tiny software canvas for pictures the game draws itself (faces). */
class Raster(val w: Int, val h: Int) {
    val px = IntArray(w * h)

    fun set(x: Int, y: Int, c: Int) {
        if (x in 0 until w && y in 0 until h) px[y * w + x] = c
    }

    fun get(x: Int, y: Int): Int = if (x in 0 until w && y in 0 until h) px[y * w + x] else 0

    fun rect(x: Int, y: Int, rw: Int, rh: Int, c: Int) {
        for (yy in y until y + rh) for (xx in x until x + rw) set(xx, yy, c)
    }

    fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float, c: Int) {
        val y0 = Math.floor((cy - ry).toDouble()).toInt()
        val y1 = Math.ceil((cy + ry).toDouble()).toInt()
        val x0 = Math.floor((cx - rx).toDouble()).toInt()
        val x1 = Math.ceil((cx + rx).toDouble()).toInt()
        for (y in y0..y1) for (x in x0..x1) {
            val dx = (x + 0.5f - cx) / rx
            val dy = (y + 0.5f - cy) / ry
            if (dx * dx + dy * dy <= 1f) set(x, y, c)
        }
    }

    /** Put a dark edge round everything drawn so far. */
    fun outline(c: Int) {
        val src = px.copyOf()
        fun solid(x: Int, y: Int) = x in 0 until w && y in 0 until h && (src[y * w + x] ushr 24) != 0
        for (y in 0 until h) for (x in 0 until w) {
            if (!solid(x, y) && (solid(x - 1, y) || solid(x + 1, y) || solid(x, y - 1) || solid(x, y + 1))) {
                px[y * w + x] = c
            }
        }
    }

    fun toImg(): Img = Img(w, h, px)
}

/** How a face looks. Unit 5 (the police sketch) can reuse this by changing one field at a time. */
class FaceSpec(
    val skin: Int = 0xFFF2C9A0.toInt(),
    val hair: Int = 0xFF5B3A1E.toInt(),
    val hairStyle: String = "short",   // short, bun, long, bald
    val hat: String = "none",          // none, cap, beret, fedora
    val hatColor: Int = 0xFF1D2B53.toInt(),
    val clothes: Int = 0xFF1D2B53.toInt(),
    val scarf: Int = 0,                // 0 = no scarf
    val moustache: Boolean = false,
    val glasses: Boolean = false,
    val stripes: Int = 0,              // 0 = plain top; otherwise the colour of the stripes
    val kind: String = "person"        // person, mouse
) {
    companion object {
        fun from(j: J): FaceSpec {
            fun col(key: String, def: Int): Int {
                val s = j.strOrNull(key) ?: return def
                return if (s.isEmpty()) 0 else Art.parseColor(s)
            }
            val d = FaceSpec()
            return FaceSpec(
                skin = col("skin", d.skin),
                hair = col("hair", d.hair),
                hairStyle = j.str("hairStyle", d.hairStyle),
                hat = j.str("hat", d.hat),
                hatColor = col("hatColor", d.hatColor),
                clothes = col("clothes", d.clothes),
                scarf = col("scarf", 0),
                moustache = j.bool("moustache"),
                glasses = j.bool("glasses"),
                stripes = col("stripes", 0),
                kind = j.str("kind", "person")
            )
        }
    }
}

object Faces {
    const val W = 44
    const val H = 48
    private val cache = HashMap<String, Img>()
    private const val INK = 0xFF22223B.toInt()

    private fun shade(c: Int, f: Float): Int {
        val r = (((c shr 16) and 255) * f).toInt().coerceIn(0, 255)
        val g = (((c shr 8) and 255) * f).toInt().coerceIn(0, 255)
        val b = ((c and 255) * f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(sh: Int): Int {
            val x = (a shr sh) and 255
            val y = (b shr sh) and 255
            return (x + (y - x) * t).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    /** mood: neutral, smile, cold, confused, busy, talk, angry, sad, scared, shy, curious */
    fun get(id: String, spec: FaceSpec, mood: String): Img {
        val key = "$id/$mood"
        cache[key]?.let { return it }
        val img = draw(spec, mood)
        cache[key] = img
        return img
    }

    /** A slanted eyebrow from (x0, y0) to (x1, y1), two pixels thick. */
    private fun brow(r: Raster, x0: Int, y0: Int, x1: Int, y1: Int, c: Int) {
        val n = Math.max(1, x1 - x0)
        for (i in 0..n) {
            val y = Math.round(y0 + (y1 - y0) * i / n.toFloat())
            r.set(x0 + i, y, c); r.set(x0 + i, y + 1, c)
        }
    }

    /** Eyebrows, eyes and mouth for the feelings in unit 10. Returns false for the older moods. */
    private fun feeling(r: Raster, mood: String, browC: Int, mouth: Int, skin: Int, eyeY: Int) {
        val white = 0xFFFFFFFF.toInt()
        when (mood) {
            "angry" -> { brow(r, 13, eyeY - 5, 18, eyeY - 2, browC); brow(r, 25, eyeY - 2, 30, eyeY - 5, browC) }
            "sad" -> { brow(r, 13, eyeY - 2, 18, eyeY - 5, browC); brow(r, 25, eyeY - 5, 30, eyeY - 2, browC) }
            "scared" -> { brow(r, 13, eyeY - 7, 18, eyeY - 8, browC); brow(r, 25, eyeY - 8, 30, eyeY - 7, browC) }
            "shy" -> { brow(r, 13, eyeY - 4, 18, eyeY - 5, browC); brow(r, 25, eyeY - 5, 30, eyeY - 4, browC) }
            "curious" -> { brow(r, 13, eyeY - 4, 18, eyeY - 4, browC); brow(r, 25, eyeY - 7, 30, eyeY - 6, browC) }
        }
        // eyes
        when (mood) {
            "scared" -> {
                r.rect(13, eyeY - 2, 6, 5, white); r.rect(25, eyeY - 2, 6, 5, white)
                r.rect(15, eyeY - 1, 2, 2, INK); r.rect(27, eyeY - 1, 2, 2, INK)
            }
            "shy" -> {
                // looking down and away
                r.rect(14, eyeY, 4, 2, white); r.rect(26, eyeY, 4, 2, white)
                r.rect(14, eyeY + 1, 2, 1, INK); r.rect(26, eyeY + 1, 2, 1, INK)
                r.rect(14, eyeY - 1, 4, 1, shade(skin, 0.8f)); r.rect(26, eyeY - 1, 4, 1, shade(skin, 0.8f))
            }
            "curious" -> {
                r.rect(14, eyeY - 1, 4, 3, white); r.rect(26, eyeY - 2, 4, 4, white)
                r.rect(16, eyeY - 1, 2, 2, INK); r.rect(28, eyeY - 2, 2, 2, INK)
            }
            else -> {
                r.rect(14, eyeY - 1, 4, 3, white); r.rect(26, eyeY - 1, 4, 3, white)
                r.rect(15, eyeY - 1, 2, 3, INK); r.rect(27, eyeY - 1, 2, 3, INK)
                if (mood == "angry") { r.rect(14, eyeY - 1, 4, 1, shade(skin, 0.8f)); r.rect(26, eyeY - 1, 4, 1, shade(skin, 0.8f)) }
            }
        }
        // mouth
        when (mood) {
            "angry" -> { r.rect(18, 29, 8, 1, mouth); r.set(17, 30, mouth); r.set(26, 30, mouth) }
            "sad" -> { r.rect(18, 29, 8, 1, mouth); r.set(17, 30, mouth); r.set(26, 30, mouth); r.rect(31, eyeY + 3, 1, 3, 0xFF8FC7F0.toInt()) }
            "scared" -> { r.rect(20, 27, 4, 4, mouth); r.rect(21, 28, 2, 2, shade(mouth, 0.5f)) }
            "shy" -> {
                r.set(18, 29, mouth); r.set(19, 28, mouth); r.set(20, 29, mouth); r.set(21, 28, mouth); r.set(22, 29, mouth); r.set(23, 28, mouth); r.set(24, 29, mouth)
                val blush = mix(skin, 0xFFE8837D.toInt(), 0.6f)
                r.rect(10, 23, 4, 3, blush); r.rect(30, 23, 4, 3, blush)
            }
            "curious" -> { r.rect(21, 28, 3, 2, mouth); r.set(25, 27, mouth) }
        }
    }

    private val FEELINGS = setOf("angry", "sad", "scared", "shy", "curious")

    /** A mouse in clothes, for the mice of chapter 2. */
    private fun drawMouse(s: FaceSpec, mood: String): Img {
        val r = Raster(W, H)
        val cx = 22f
        val fur = s.skin
        val furD = shade(fur, 0.8f)
        val furL = mix(fur, 0xFFFFFFFF.toInt(), 0.35f)
        val pink = 0xFFF2A0B0.toInt()
        // shoulders
        r.ellipse(cx, 50f, 19f, 13f, s.clothes)
        if (s.stripes != 0) {
            var sy = 39
            while (sy < H) { for (sx in 0 until W) if ((r.get(sx, sy) ushr 24) != 0) r.set(sx, sy, s.stripes); sy += 3 }
        }
        if (s.scarf != 0) { r.ellipse(cx, 38.5f, 10f, 3.5f, s.scarf); r.rect(24, 39, 5, 7, shade(s.scarf, 0.85f)) }
        // big round ears
        r.ellipse(9f, 11f, 8f, 8f, furD); r.ellipse(35f, 11f, 8f, 8f, furD)
        r.ellipse(9f, 11f, 5f, 5f, pink); r.ellipse(35f, 11f, 5f, 5f, pink)
        // head and snout
        r.ellipse(cx, 23f, 13f, 12f, fur)
        r.ellipse(cx, 30f, 7.5f, 5f, furL)
        if (s.hairStyle == "long") {
            // a powdered wig, for the queen
            r.ellipse(cx, 10f, 12f, 6f, s.hair)
            r.ellipse(11f, 18f, 4f, 6f, s.hair); r.ellipse(33f, 18f, 4f, 6f, s.hair)
        }
        // crown or cap
        when (s.hat) {
            "crown" -> {
                r.rect(13, 7, 18, 5, 0xFFF2B233.toInt())
                for (k in 0 until 4) r.rect(13 + k * 5, 3, 3, 4, 0xFFF2B233.toInt())
                r.rect(20, 8, 4, 3, 0xFFD9534F.toInt())
            }
            "bonnet" -> {
                // the red cap of the revolution, flopping forward
                r.ellipse(cx, 11f, 11f, 6f, s.hatColor)
                r.ellipse(cx + 8f, 7f, 5f, 4f, s.hatColor)
                r.rect(11, 13, 22, 2, shade(s.hatColor, 0.75f))
                r.rect(20, 10, 3, 3, 0xFFFFFFFF.toInt()); r.set(21, 11, 0xFF3F6DB5.toInt())
            }
            "cap", "beret", "hat", "fedora" -> {
                r.ellipse(cx, 12f, 11f, 5f, s.hatColor)
                r.rect(11, 12, 22, 2, shade(s.hatColor, 0.75f))
            }
        }
        val eyeY = 21
        if (FEELINGS.contains(mood)) feeling(r, mood, furD, INK, fur, eyeY)
        else {
            when (mood) {
                "cold" -> { r.rect(14, eyeY - 3, 5, 1, furD); r.rect(25, eyeY - 3, 5, 1, furD); r.rect(15, eyeY, 3, 1, INK); r.rect(26, eyeY, 3, 1, INK) }
                "busy" -> { r.rect(15, eyeY + 1, 3, 1, INK); r.rect(26, eyeY + 1, 3, 1, INK) }
                else -> { r.rect(15, eyeY - 1, 3, 3, INK); r.rect(26, eyeY - 1, 3, 3, INK); r.set(15, eyeY - 1, 0xFFFFFFFF.toInt()); r.set(26, eyeY - 1, 0xFFFFFFFF.toInt()) }
            }
            when (mood) {
                "smile" -> { r.rect(19, 32, 6, 1, INK); r.set(18, 31, INK); r.set(25, 31, INK) }
                "talk" -> { r.rect(20, 31, 4, 3, 0xFFB5484D.toInt()) }
                "confused" -> { r.rect(20, 32, 4, 1, INK); r.set(24, 31, INK) }
                else -> r.rect(20, 32, 4, 1, INK)
            }
        }
        // nose, teeth and whiskers
        r.ellipse(cx, 28f, 2f, 1.5f, pink)
        r.rect(21, 28, 2, 1, INK)
        if (mood != "talk" && mood != "scared") { r.rect(20, 33, 2, 2, 0xFFFFFFFF.toInt()); r.rect(22, 33, 2, 2, 0xFFFFFFFF.toInt()) }
        for (k in 0 until 3) {
            val wy = 29 + k * 2
            for (i in 0 until 7) { r.set(13 - i, wy - (k - 1) * i / 3, furD); r.set(30 + i, wy - (k - 1) * i / 3, furD) }
        }
        if (s.glasses) {
            for (gx in intArrayOf(12, 24)) {
                r.rect(gx, eyeY - 3, 8, 1, INK); r.rect(gx, eyeY + 3, 8, 1, INK)
                r.rect(gx, eyeY - 3, 1, 7, INK); r.rect(gx + 7, eyeY - 3, 1, 7, INK)
            }
            r.rect(20, eyeY - 1, 4, 1, INK)
        }
        r.outline(INK)
        return r.toImg()
    }

    private fun draw(s: FaceSpec, mood: String): Img {
        if (s.kind == "mouse") return drawMouse(s, mood)
        val r = Raster(W, H)
        val cx = 22f
        val skinD = shade(s.skin, 0.86f)
        val hairD = shade(s.hair, 0.8f)

        // long hair and buns sit behind the head
        if (s.hairStyle == "long") {
            r.ellipse(cx, 22f, 14.5f, 16f, hairD)
            r.rect(8, 22, 28, 14, hairD)
        }
        val feminine = s.hairStyle == "bun" || s.hairStyle == "long"
        if (s.hairStyle == "bun") {
            r.ellipse(cx, 4f, 6f, 4.2f, s.hair)
            r.ellipse(cx - 1.5f, 3f, 2.5f, 1.6f, hairD)
            // hair falls beside the face
            r.rect(7, 12, 5, 17, hairD)
            r.rect(32, 12, 5, 17, hairD)
        }

        // shoulders and neck
        r.ellipse(cx, 50f, 19f, 13f, s.clothes)
        if (s.stripes != 0) {
            // a striped sailor top
            var sy = 39
            while (sy < H) {
                for (sx in 0 until W) if ((r.get(sx, sy) ushr 24) != 0) r.set(sx, sy, s.stripes)
                sy += 3
            }
        }
        r.rect(18, 33, 8, 6, skinD)
        if (s.scarf != 0) {
            r.ellipse(cx, 38.5f, 9f, 3.5f, s.scarf)
            r.rect(24, 39, 5, 7, shade(s.scarf, 0.85f))
        } else {
            // a white collar
            r.rect(17, 38, 4, 3, 0xFFFFFFFF.toInt())
            r.rect(23, 38, 4, 3, 0xFFFFFFFF.toInt())
        }

        // ears and head
        r.ellipse(9.5f, 21f, 2.5f, 3.5f, skinD)
        r.ellipse(34.5f, 21f, 2.5f, 3.5f, skinD)
        r.ellipse(cx, 20f, 12f, 14f, s.skin)
        if (feminine) { r.rect(8, 26, 2, 2, 0xFFF2B233.toInt()); r.rect(34, 26, 2, 2, 0xFFF2B233.toInt()) }

        // hair on top
        if (s.hairStyle != "bald") {
            r.ellipse(cx, 11f, 12.5f, 7.5f, s.hair)
            r.rect(9, 11, 3, 8, s.hair)
            r.rect(32, 11, 3, 8, s.hair)
            // fringe
            r.ellipse(16f, 13f, 6f, 3.5f, s.hair)
            r.ellipse(27f, 12.5f, 7f, 3f, hairD)
        }

        // hats
        when (s.hat) {
            "cap" -> {
                r.ellipse(cx, 9f, 13f, 6.5f, s.hatColor)
                r.rect(9, 9, 26, 4, s.hatColor)
                r.rect(8, 13, 28, 2, shade(s.hatColor, 0.7f))
                r.rect(20, 7, 4, 3, 0xFFF2B233.toInt())
            }
            "beret" -> {
                r.ellipse(cx - 2, 8f, 15f, 6f, s.hatColor)
                r.rect(22, 1, 2, 3, s.hatColor)
            }
            "kepi" -> {
                r.rect(11, 2, 22, 9, s.hatColor)
                r.rect(11, 9, 22, 2, 0xFFF2B233.toInt())
                r.ellipse(cx, 13f, 13f, 2.4f, shade(s.hatColor, 0.6f))
            }
            "fedora", "hat" -> {
                r.ellipse(cx, 7f, 10f, 6f, s.hatColor)
                r.rect(12, 7, 20, 5, s.hatColor)
                r.rect(12, 10, 20, 2, 0xFFF2B233.toInt())
                r.ellipse(cx, 13f, 17f, 2.6f, shade(s.hatColor, 0.8f))
            }
        }

        // the feelings of unit 10 have their own eyebrows, eyes and mouths
        if (FEELINGS.contains(mood)) {
            feeling(r, mood, shade(s.hair, 0.5f), if (feminine) 0xFFD23B4B.toInt() else 0xFFB5484D.toInt(), s.skin, 20)
            if (feminine) { r.set(13, 18, INK); r.set(12, 19, INK); r.set(30, 18, INK); r.set(31, 19, INK) }
            if (s.glasses) {
                for (gx in intArrayOf(12, 24)) {
                    r.rect(gx, 17, 8, 1, INK); r.rect(gx, 23, 8, 1, INK)
                    r.rect(gx, 17, 1, 7, INK); r.rect(gx + 7, 17, 1, 7, INK)
                }
                r.rect(20, 19, 4, 1, INK)
            }
            r.rect(21, 23, 2, 3, skinD)
            if (s.moustache) { r.rect(16, 26, 12, 2, hairD); r.rect(15, 27, 3, 2, hairD); r.rect(26, 27, 3, 2, hairD) }
            r.outline(INK)
            return r.toImg()
        }

        // eyebrows, eyes, mouth change with the mood
        val browL: Int
        val browR: Int
        when (mood) {
            "cold" -> { browL = 17; browR = 17 }
            "confused" -> { browL = 14; browR = 17 }
            "smile", "talk" -> { browL = 15; browR = 15 }
            else -> { browL = 16; browR = 16 }
        }
        r.rect(14, browL, 5, 1, hairD)
        r.rect(25, browR, 5, 1, hairD)
        if (mood == "cold") { r.set(18, 18, hairD); r.set(25, 18, hairD) }

        val eyeY = 20
        if (mood == "busy") {
            // looking down
            r.rect(14, eyeY + 1, 4, 1, INK)
            r.rect(26, eyeY + 1, 4, 1, INK)
        } else if (mood == "cold") {
            r.rect(14, eyeY, 4, 2, 0xFFFFFFFF.toInt())
            r.rect(26, eyeY, 4, 2, 0xFFFFFFFF.toInt())
            r.rect(15, eyeY, 2, 2, INK)
            r.rect(27, eyeY, 2, 2, INK)
            r.rect(14, eyeY - 1, 4, 1, skinD)
            r.rect(26, eyeY - 1, 4, 1, skinD)
        } else {
            r.rect(14, eyeY - 1, 4, 3, 0xFFFFFFFF.toInt())
            r.rect(26, eyeY - 1, 4, 3, 0xFFFFFFFF.toInt())
            r.rect(15, eyeY - 1, 2, 3, INK)
            r.rect(27, eyeY - 1, 2, 3, INK)
            r.set(15, eyeY - 1, 0xFFFFFFFF.toInt())
            r.set(27, eyeY - 1, 0xFFFFFFFF.toInt())
        }
        if (feminine && mood != "busy") {
            r.set(13, eyeY - 2, INK); r.set(12, eyeY - 1, INK)
            r.set(30, eyeY - 2, INK); r.set(31, eyeY - 1, INK)
        }
        if (s.glasses) {
            for (gx in intArrayOf(12, 24)) {
                r.rect(gx, eyeY - 3, 8, 1, INK); r.rect(gx, eyeY + 3, 8, 1, INK)
                r.rect(gx, eyeY - 3, 1, 7, INK); r.rect(gx + 7, eyeY - 3, 1, 7, INK)
            }
            r.rect(20, eyeY - 1, 4, 1, INK)
        }

        // nose
        r.rect(21, 23, 2, 3, skinD)

        val mouth = if (feminine) 0xFFD23B4B.toInt() else 0xFFB5484D.toInt()
        when (mood) {
            "smile" -> {
                r.rect(17, 28, 10, 1, mouth)
                r.rect(18, 29, 8, 1, mouth)
                r.rect(19, 30, 6, 1, 0xFFFFFFFF.toInt())
                r.set(16, 27, mouth); r.set(27, 27, mouth)
                val blush = mix(s.skin, 0xFFE8837D.toInt(), 0.45f)
                r.rect(11, 24, 3, 2, blush); r.rect(30, 24, 3, 2, blush)
            }
            "talk" -> {
                r.rect(19, 28, 6, 3, mouth)
                r.rect(20, 28, 4, 1, 0xFFFFFFFF.toInt())
            }
            "cold" -> r.rect(18, 29, 8, 1, mouth)
            "confused" -> { r.rect(20, 28, 4, 3, mouth); r.rect(21, 29, 2, 1, shade(mouth, 0.6f)) }
            "busy" -> r.rect(19, 29, 6, 1, mouth)
            else -> { r.rect(18, 29, 8, 1, mouth); r.set(17, 28, mouth); r.set(26, 28, mouth) }
        }
        if (s.moustache) {
            r.rect(16, 26, 12, 2, hairD)
            r.rect(15, 27, 3, 2, hairD)
            r.rect(26, 27, 3, 2, hairD)
        }

        r.outline(INK)
        return r.toImg()
    }
}
