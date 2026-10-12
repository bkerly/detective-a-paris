package detective.core

/**
 * Word pictures for units 2 to 8, and pictures that take a value after a
 * colon, so one drawing serves many words:
 *
 *   num:7        a number            coin:5        a coin or banknote
 *   swatch:d9534f a colour           hair:5b3a1e   a head with hair that colour
 *   day:LUN      a calendar page     clock:8       a clock at that hour (clock:? for "what time?")
 *   dir:left     a road sign arrow   prep:sous     a ball under a box
 *   prep:sous:table  a ball under a table (objects: box, table, chair, curtain, hat, altar, book, clock, bear)
 *   thing:altar  one of those objects on its own
 */
internal object MoreIcons {
    private val SKIN = 0xFFF2C9A0.toInt()
    private val SKIN_D = 0xFFD9A77A.toInt()
    private val CREAM = 0xFFEADFC4.toInt()
    private val STONE = 0xFFC9C2B2.toInt()
    private val STONE_D = 0xFF9C9484.toInt()
    private val WOOD = 0xFFB07A3F.toInt()
    private val WOOD_D = 0xFF7A5226.toInt()
    private val WATER = 0xFF8EC5E8.toInt()
    private val GLASS = 0xFFD9ECF7.toInt()

    private val NAMES = hashSetOf(
        "taxi", "taxi_green", "taxi_red", "street", "hotel", "here", "there", "where", "goto", "howmuch", "itshere",
        "key", "stairs", "lift", "bed", "floor", "ground", "booking", "breakfast",
        "water", "juice", "hotchoc", "lemonade", "croque", "crepe", "fries", "icecream", "bill", "yum", "thief", "menucard",
        "tall", "short", "moustache", "glasses", "hanger", "hat", "coat", "scarf", "pull",
        "dress", "trousers", "shirt", "shoes", "jacket", "hat_big", "hat_small", "hat_ok", "mirror", "mouse",
        "cathedral", "bridge", "river", "island", "route", "cross_bridge", "first_right", "near", "far",
        "museum", "painting", "bigclock", "lilies", "guardian",
        "suitcase", "voila", "today", "tomorrow",
        "net", "page", "map", "sketch", "eiffel", "arc", "pyramid", "note", "moon", "sun", "he", "she"
    )
    private val PREFIXES = arrayOf("num:", "coin:", "swatch:", "hair:", "day:", "clock:", "dir:", "prep:", "thing:")

    /** One of every picture, for the art sheet. */
    fun samples(): List<String> {
        val out = ArrayList<String>(NAMES.sorted())
        out.addAll(listOf("num:7", "num:15", "coin:1", "coin:2", "coin:5", "coin:10", "coin:20", "coin:50", "swatch:d9534f", "swatch:ffffff", "hair:b5484d",
            "day:lun.", "clock:8", "clock:20", "clock:?", "dir:left", "dir:right", "dir:straight"))
        for (r in listOf("sur", "sous", "derriere", "devant", "dans", "acote", "entre")) out.add("prep:$r")
        for (o in listOf("table", "chair", "curtain", "hat", "altar", "book", "clock", "bear")) out.add("prep:sous:$o")
        out.add("prep:derriere:altar"); out.add("prep:acote:bear"); out.add("prep:derriere:curtain"); out.add("prep:sur:chair"); out.add("prep:dans:box")
        out.addAll(Icons2.samples())
        return out
    }

    fun knows(name: String): Boolean {
        if (NAMES.contains(name) || Icons2.knows(name)) return true
        for (pf in PREFIXES) if (name.startsWith(pf) && name.length > pf.length) return true
        return false
    }

    private fun color(hex: String): Int = try { Art.parseColor(hex) } catch (e: Exception) { C.GREY }

    private fun dark(c: Int, f: Float = 0.75f): Int {
        val r = (((c shr 16) and 255) * f).toInt()
        val g = (((c shr 8) and 255) * f).toInt()
        val b = ((c and 255) * f).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun pin(k: Icons.K, cx: Float, tipY: Float, r: Float, c: Int) {
        k.poly(c, cx - r * 0.8f, tipY - r * 1.5f, cx + r * 0.8f, tipY - r * 1.5f, cx, tipY)
        k.oval(cx - r, tipY - r * 2.9f, r * 2f, r * 2f, c)
        k.oval(cx - r * 0.4f, tipY - r * 2.3f, r * 0.8f, r * 0.8f, C.WHITE)
    }

    private fun person(k: Icons.K, cx: Float, footY: Float, h: Float, c: Int) {
        val head = h * 0.26f
        k.oval(cx - head / 2f, footY - h, head, head, SKIN)
        k.rr(cx - h * 0.17f, footY - h * 0.72f, h * 0.34f, h * 0.44f, 0.03f, c)
        k.rect(cx - h * 0.13f, footY - h * 0.30f, h * 0.10f, h * 0.30f, C.INK)
        k.rect(cx + h * 0.03f, footY - h * 0.30f, h * 0.10f, h * 0.30f, C.INK)
    }

    private fun glass(k: Icons.K, liquid: Int, fill: Float) {
        k.poly(GLASS, 0.30f, 0.18f, 0.70f, 0.18f, 0.64f, 0.86f, 0.36f, 0.86f)
        val top = 0.86f - 0.62f * fill
        val inset = 0.06f * (0.86f - top) / 0.68f
        k.poly(liquid, 0.36f - inset + 0.01f, top, 0.64f + inset - 0.01f, top, 0.635f, 0.85f, 0.365f, 0.85f)
        k.line(0.30f, 0.18f, 0.36f, 0.86f, 0.03f, C.INK2)
        k.line(0.70f, 0.18f, 0.64f, 0.86f, 0.03f, C.INK2)
        k.line(0.36f, 0.86f, 0.64f, 0.86f, 0.03f, C.INK2)
    }

    private fun clock(k: Icons.K, hour: Int, gold: Boolean) {
        k.oval(0.08f, 0.08f, 0.84f, 0.84f, if (gold) C.MUSTARD_D else C.INK)
        k.oval(0.14f, 0.14f, 0.72f, 0.72f, C.WHITE)
        for (i in 0 until 12) {
            val a = Math.toRadians((i * 30).toDouble())
            val x1 = 0.5f + 0.30f * Math.sin(a).toFloat()
            val y1 = 0.5f - 0.30f * Math.cos(a).toFloat()
            k.oval(x1 - 0.02f, y1 - 0.02f, 0.04f, 0.04f, C.INK2)
        }
        if (hour >= 0) {
            val a = Math.toRadians(((hour % 12) * 30).toDouble())
            k.line(0.5f, 0.5f, 0.5f + 0.18f * Math.sin(a).toFloat(), 0.5f - 0.18f * Math.cos(a).toFloat(), 0.07f, C.INK)
            k.line(0.5f, 0.5f, 0.5f, 0.24f, 0.045f, C.RED)
            k.oval(0.46f, 0.46f, 0.08f, 0.08f, C.INK)
            // 13 to 24 are the afternoon and evening hours: show a moon; morning hours get a sun
            if (hour >= 18 || hour < 6) { k.oval(0.70f, 0.02f, 0.26f, 0.26f, C.INK); k.oval(0.74f, 0.06f, 0.16f, 0.16f, 0xFFFDF3C4.toInt()); k.oval(0.79f, 0.04f, 0.14f, 0.14f, C.INK) }
            else { k.oval(0.72f, 0.03f, 0.24f, 0.24f, C.MUSTARD) }
        } else k.text("?", 0.5f, 0.5f, 0.42f, C.RED)
    }

    /** The thing a place word is measured from, drawn in the box at (ox, oy) with side os. */
    private fun thing(k: Icons.K, obj: String, ox: Float, oy: Float, os: Float) {
        fun r(a: Float, b: Float, w: Float, h: Float, c: Int) = k.rect(ox + a * os, oy + b * os, w * os, h * os, c)
        fun rr(a: Float, b: Float, w: Float, h: Float, c: Int) = k.rr(ox + a * os, oy + b * os, w * os, h * os, 0.04f * os, c)
        when (obj) {
            "table" -> { rr(0f, 0.20f, 1f, 0.16f, WOOD); r(0.08f, 0.36f, 0.10f, 0.64f, WOOD_D); r(0.82f, 0.36f, 0.10f, 0.64f, WOOD_D) }
            "chair" -> { r(0.16f, 0f, 0.12f, 1f, WOOD_D); rr(0.16f, 0.46f, 0.68f, 0.14f, WOOD); r(0.72f, 0.58f, 0.12f, 0.42f, WOOD_D); rr(0.16f, 0.06f, 0.14f, 0.34f, WOOD) }
            "curtain" -> {
                r(0f, 0f, 1f, 0.08f, C.MUSTARD_D)
                for (i in 0 until 5) r(0.02f + i * 0.196f, 0.08f, 0.176f, 0.92f, if (i % 2 == 0) C.RED else C.RED_D)
            }
            "hat" -> { rr(0.22f, 0.10f, 0.56f, 0.62f, WOOD_D); r(0.22f, 0.52f, 0.56f, 0.10f, C.MUSTARD); k.oval(ox, oy + 0.62f * os, os, 0.30f * os, WOOD_D) }
            "altar" -> { rr(0f, 0.30f, 1f, 0.14f, C.WHITE); r(0.06f, 0.44f, 0.88f, 0.56f, STONE); r(0.44f, 0.54f, 0.12f, 0.36f, C.MUSTARD); r(0.34f, 0.64f, 0.32f, 0.10f, C.MUSTARD) }
            "book" -> { k.poly(WOOD_D, ox, oy + 0.40f * os, ox + 0.5f * os, oy + 0.52f * os, ox + os, oy + 0.40f * os, ox + os, oy + 0.80f * os, ox + 0.5f * os, oy + 0.92f * os, ox, oy + 0.80f * os)
                k.poly(C.WHITE, ox + 0.05f * os, oy + 0.36f * os, ox + 0.5f * os, oy + 0.48f * os, ox + 0.5f * os, oy + 0.86f * os, ox + 0.05f * os, oy + 0.74f * os)
                k.poly(C.PAPER, ox + 0.95f * os, oy + 0.36f * os, ox + 0.5f * os, oy + 0.48f * os, ox + 0.5f * os, oy + 0.86f * os, ox + 0.95f * os, oy + 0.74f * os) }
            "clock" -> { k.oval(ox + 0.05f * os, oy + 0.05f * os, 0.9f * os, 0.9f * os, C.MUSTARD_D); k.oval(ox + 0.14f * os, oy + 0.14f * os, 0.72f * os, 0.72f * os, C.WHITE)
                k.line(ox + 0.5f * os, oy + 0.5f * os, ox + 0.5f * os, oy + 0.24f * os, 0.06f * os, C.INK); k.line(ox + 0.5f * os, oy + 0.5f * os, ox + 0.70f * os, oy + 0.56f * os, 0.06f * os, C.INK) }
            "bear" -> { k.oval(ox + 0.05f * os, oy + 0.30f * os, 0.78f * os, 0.50f * os, C.WHITE); k.oval(ox + 0.60f * os, oy + 0.14f * os, 0.36f * os, 0.34f * os, C.WHITE)
                k.oval(ox + 0.64f * os, oy + 0.10f * os, 0.10f * os, 0.10f * os, C.WHITE); k.oval(ox + 0.84f * os, oy + 0.10f * os, 0.10f * os, 0.10f * os, C.WHITE)
                r(0.14f, 0.70f, 0.14f, 0.30f, C.WHITE); r(0.58f, 0.70f, 0.14f, 0.30f, C.WHITE); k.oval(ox + 0.86f * os, oy + 0.28f * os, 0.08f * os, 0.07f * os, C.INK) }
            else -> { rr(0f, 0f, 1f, 1f, WOOD); r(0f, 0.44f, 1f, 0.10f, WOOD_D); r(0.45f, 0f, 0.10f, 1f, WOOD_D) }
        }
    }

    /** A page of a wall calendar: a coloured top, two rings, a few days. */
    private fun calendar(k: Icons.K, a: Float, b: Float, w: Float, top: Int) {
        k.rr(a, b, w, w, w * 0.10f, C.INK2)
        k.rr(a + w * 0.05f, b + w * 0.05f, w * 0.90f, w * 0.90f, w * 0.06f, C.WHITE)
        k.rr(a + w * 0.05f, b + w * 0.05f, w * 0.90f, w * 0.24f, w * 0.06f, top)
        k.rect(a + w * 0.24f, b - w * 0.08f, w * 0.09f, w * 0.20f, C.INK2); k.rect(a + w * 0.67f, b - w * 0.08f, w * 0.09f, w * 0.20f, C.INK2)
        for (r in 0 until 2) for (c in 0 until 3) k.rect(a + w * (0.18f + c * 0.24f), b + w * (0.42f + r * 0.24f), w * 0.14f, w * 0.12f, C.GREY)
    }

    private fun ball(k: Icons.K, cx: Float, cy: Float, r: Float) {
        k.oval(cx - r, cy - r, r * 2f, r * 2f, C.RED)
        k.oval(cx - r * 0.5f, cy - r * 0.6f, r * 0.55f, r * 0.45f, 0xFFF3B9B5.toInt())
    }

    private fun prep(k: Icons.K, rel: String, obj: String) {
        val white = obj == "bear" || obj == "altar"
        if (white) k.rr(0.02f, 0.02f, 0.96f, 0.96f, 0.10f, 0xFFDCE4F0.toInt())
        when (rel) {
            "sur" -> {
                // the ball rests on the top of the thing: a table top, a chair seat, a lid
                val top = when (obj) { "table" -> 0.20f; "chair" -> 0.46f; "hat" -> 0.10f; "altar" -> 0.30f; "book" -> 0.40f; else -> 0f }
                thing(k, obj, 0.20f, 0.38f, 0.60f)
                ball(k, if (obj == "chair") 0.54f else 0.5f, 0.38f + top * 0.60f - 0.12f, 0.13f)
            }
            "sous" -> { thing(k, obj, 0.14f, 0.08f, 0.72f); ball(k, 0.5f, if (obj == "table" || obj == "chair") 0.66f else 0.90f, if (obj == "table" || obj == "chair") 0.13f else 0.10f) }
            "derriere" -> {
                // the ball peeps out from behind, half hidden
                ball(k, 0.76f, 0.40f, 0.17f)
                thing(k, obj, 0.08f, 0.26f, 0.66f)
            }
            "devant" -> { thing(k, obj, 0.16f, 0.06f, 0.68f); ball(k, 0.50f, 0.76f, 0.18f) }
            "dans" -> {
                // an open box seen from the front, with the ball inside
                k.rr(0.14f, 0.30f, 0.72f, 0.62f, 0.04f, WOOD_D)
                k.rr(0.20f, 0.36f, 0.60f, 0.50f, 0.03f, 0xFF4A3018.toInt())
                ball(k, 0.5f, 0.62f, 0.17f)
                k.rect(0.14f, 0.74f, 0.72f, 0.18f, WOOD)
            }
            "acote" -> { thing(k, obj, 0.06f, 0.26f, 0.52f); ball(k, 0.80f, 0.64f, 0.14f) }
            "entre" -> { thing(k, obj, 0.02f, 0.34f, 0.32f); thing(k, obj, 0.66f, 0.34f, 0.32f); ball(k, 0.5f, 0.56f, 0.13f) }
            else -> thing(k, obj, 0.15f, 0.15f, 0.7f)
        }
    }

    private fun garment(k: Icons.K, c: Int, long: Boolean) {
        val d = dark(c)
        // body, then sleeves
        k.poly(c, 0.32f, 0.16f, 0.68f, 0.16f, 0.72f, if (long) 0.92f else 0.80f, 0.28f, if (long) 0.92f else 0.80f)
        k.poly(d, 0.32f, 0.16f, 0.10f, 0.42f, 0.22f, 0.52f, 0.34f, 0.36f)
        k.poly(d, 0.68f, 0.16f, 0.90f, 0.42f, 0.78f, 0.52f, 0.66f, 0.36f)
    }

    fun draw(k: Icons.K, name: String): Boolean {
        val colon = name.indexOf(':')
        if (colon > 0) {
            val kind = name.substring(0, colon)
            val arg = name.substring(colon + 1)
            when (kind) {
                "num" -> {
                    k.oval(0.06f, 0.06f, 0.88f, 0.88f, C.MUSTARD)
                    k.oval(0.12f, 0.12f, 0.76f, 0.76f, 0xFFFBD76B.toInt())
                    k.text(arg, 0.5f, 0.51f, if (arg.length > 1) 0.44f else 0.56f, C.INK)
                }
                "coin" -> {
                    val v = arg.toIntOrNull() ?: 1
                    if (v < 5) {
                        k.oval(0.10f, 0.10f, 0.80f, 0.80f, C.MUSTARD_D)
                        k.oval(0.16f, 0.16f, 0.68f, 0.68f, if (v == 2) 0xFFD8DBE6.toInt() else C.MUSTARD)
                        k.oval(0.26f, 0.26f, 0.48f, 0.48f, if (v == 2) C.MUSTARD else 0xFFFBD76B.toInt())
                        k.text(arg, 0.5f, 0.51f, 0.38f, C.INK)
                    } else {
                        val c = when (v) { 5 -> 0xFF8FBF8F.toInt(); 10 -> 0xFFE58B86.toInt(); 20 -> 0xFF8FB4E8.toInt(); else -> 0xFFF0B070.toInt() }
                        k.rr(0.02f, 0.22f, 0.96f, 0.56f, 0.05f, dark(c, 0.7f))
                        k.rr(0.06f, 0.26f, 0.88f, 0.48f, 0.03f, c)
                        k.oval(0.62f, 0.34f, 0.26f, 0.32f, dark(c, 0.85f))
                        k.text(arg, 0.36f, 0.51f, 0.34f, C.INK)
                    }
                }
                "swatch" -> {
                    val c = color(arg)
                    k.rr(0.10f, 0.10f, 0.80f, 0.80f, 0.16f, if (c == C.WHITE) C.GREY else dark(c, 0.7f))
                    k.rr(0.15f, 0.15f, 0.70f, 0.70f, 0.12f, c)
                    k.oval(0.22f, 0.22f, 0.22f, 0.14f, C.alpha(C.WHITE, if (c == C.WHITE) 0f else 0.45f))
                }
                "hair" -> {
                    val img = Faces.get("hair$arg", FaceSpec(hair = color(arg), hairStyle = "long", clothes = 0xFF8FA0B8.toInt()), "smile")
                    val w = k.s * Faces.W / Faces.H
                    k.p.image(img, k.x + (k.s - w) / 2f, k.y, w, k.s, false, 1f)
                }
                "day" -> {
                    k.rr(0.10f, 0.12f, 0.80f, 0.78f, 0.08f, C.INK2)
                    k.rr(0.14f, 0.16f, 0.72f, 0.70f, 0.05f, C.WHITE)
                    k.rr(0.14f, 0.16f, 0.72f, 0.20f, 0.05f, C.RED)
                    k.rect(0.28f, 0.06f, 0.07f, 0.16f, C.INK2); k.rect(0.65f, 0.06f, 0.07f, 0.16f, C.INK2)
                    k.text(arg, 0.5f, 0.62f, if (arg.length > 2) 0.26f else 0.34f, C.INK)
                }
                "clock" -> clock(k, arg.toIntOrNull() ?: -1, false)
                "dir" -> {
                    k.oval(0.06f, 0.06f, 0.88f, 0.88f, 0xFF2F6FD0.toInt())
                    Draw.arrow(k.p, k.x + k.s * 0.5f, k.y + k.s * 0.5f, k.s * 0.56f, if (arg == "straight") "up" else arg, C.WHITE)
                }
                "thing" -> {
                    if (arg == "bear" || arg == "altar") k.rr(0.02f, 0.02f, 0.96f, 0.96f, 0.10f, 0xFFDCE4F0.toInt())
                    thing(k, arg, 0.14f, 0.14f, 0.72f)
                }
                "prep" -> {
                    val c2 = arg.indexOf(':')
                    if (c2 > 0) prep(k, arg.substring(0, c2), arg.substring(c2 + 1)) else prep(k, arg, "box")
                }
                else -> return Icons2.draw(k, name)
            }
            return true
        }
        when (name) {
            "taxi", "taxi_green", "taxi_red" -> {
                k.rr(0.06f, 0.44f, 0.88f, 0.30f, 0.08f, C.INK)
                k.poly(C.INK, 0.24f, 0.46f, 0.34f, 0.24f, 0.68f, 0.24f, 0.80f, 0.46f)
                k.poly(WATER, 0.30f, 0.44f, 0.37f, 0.29f, 0.49f, 0.29f, 0.49f, 0.44f)
                k.poly(WATER, 0.53f, 0.44f, 0.53f, 0.29f, 0.65f, 0.29f, 0.73f, 0.44f)
                k.oval(0.16f, 0.62f, 0.22f, 0.22f, C.GREY); k.oval(0.62f, 0.62f, 0.22f, 0.22f, C.GREY)
                k.oval(0.22f, 0.68f, 0.10f, 0.10f, C.INK); k.oval(0.68f, 0.68f, 0.10f, 0.10f, C.INK)
                // the roof light: green for free, red for taken, pale when it is off
                val light = if (name == "taxi_red") C.RED else if (name == "taxi_green") 0xFF2FCB6A.toInt() else 0xFFFDF3C4.toInt()
                if (name != "taxi") k.oval(0.33f, -0.02f, 0.36f, 0.32f, C.alpha(light, 0.35f))
                k.rr(0.36f, 0.06f, 0.30f, 0.19f, 0.05f, C.INK)
                k.rr(0.385f, 0.085f, 0.25f, 0.14f, 0.03f, light)
                k.text("TAXI", 0.51f, 0.16f, 0.085f, if (name == "taxi_red") C.WHITE else C.INK, Font.BOLD)
            }
            "street" -> {
                k.rr(0.06f, 0.26f, 0.88f, 0.48f, 0.06f, C.GREEN_D)
                k.rr(0.10f, 0.30f, 0.80f, 0.40f, 0.04f, 0xFF1F3E8A.toInt())
                k.text("RUE", 0.5f, 0.51f, 0.26f, C.WHITE)
            }
            "hotel" -> {
                k.rect(0.16f, 0.18f, 0.68f, 0.74f, CREAM)
                k.rect(0.16f, 0.10f, 0.68f, 0.10f, 0xFF6F7B99.toInt())
                k.rect(0.16f, 0.28f, 0.68f, 0.14f, C.RED_D)
                k.text("HÔTEL", 0.5f, 0.35f, 0.115f, C.WHITE, Font.BOLD)
                for (i in 0 until 3) { k.rect(0.23f + i * 0.20f, 0.48f, 0.12f, 0.14f, WATER) }
                k.rect(0.23f, 0.70f, 0.12f, 0.14f, WATER); k.rect(0.63f, 0.70f, 0.12f, 0.14f, WATER)
                k.rr(0.42f, 0.68f, 0.16f, 0.24f, 0.02f, C.INK)
            }
            "here" -> { k.oval(0.26f, 0.76f, 0.48f, 0.16f, C.GREEN); pin(k, 0.5f, 0.84f, 0.22f, C.RED) }
            "itshere" -> { k.oval(0.16f, 0.76f, 0.48f, 0.16f, C.GREEN); pin(k, 0.40f, 0.84f, 0.20f, C.RED); k.oval(0.58f, 0.10f, 0.36f, 0.36f, C.GREEN); k.line(0.66f, 0.29f, 0.74f, 0.37f, 0.06f, C.WHITE); k.line(0.74f, 0.37f, 0.86f, 0.20f, 0.06f, C.WHITE) }
            "there" -> {
                k.oval(0.08f, 0.76f, 0.16f, 0.16f, C.INK)
                var f = 0.2f
                while (f < 0.9f) { k.oval(0.14f + f * 0.52f, 0.80f - f * 0.42f, 0.05f, 0.05f, C.INK2); f += 0.2f }
                pin(k, 0.76f, 0.42f, 0.14f, C.RED)
            }
            "where" -> { pin(k, 0.5f, 0.90f, 0.26f, C.INK2); k.oval(0.34f, 0.20f, 0.32f, 0.32f, C.INK2); k.text("?", 0.5f, 0.38f, 0.34f, C.WHITE) }
            "goto" -> { person(k, 0.24f, 0.88f, 0.56f, C.INK2); Draw.arrow(k.p, k.x + k.s * 0.52f, k.y + k.s * 0.60f, k.s * 0.24f, "right", C.INK); pin(k, 0.82f, 0.84f, 0.13f, C.RED) }
            "howmuch" -> {
                k.poly(C.MUSTARD, 0.10f, 0.50f, 0.34f, 0.24f, 0.90f, 0.24f, 0.90f, 0.76f, 0.34f, 0.76f)
                k.oval(0.22f, 0.44f, 0.12f, 0.12f, C.WHITE)
                k.text("€ ?", 0.62f, 0.51f, 0.26f, C.INK)
            }
            "key" -> {
                k.ring(0.10f, 0.30f, 0.36f, 0.36f, 0.18f, 0.09f, C.MUSTARD_D)
                k.rect(0.44f, 0.44f, 0.46f, 0.09f, C.MUSTARD_D)
                k.rect(0.72f, 0.52f, 0.07f, 0.14f, C.MUSTARD_D); k.rect(0.84f, 0.52f, 0.06f, 0.10f, C.MUSTARD_D)
            }
            "suitcase" -> {
                k.ring(0.36f, 0.12f, 0.28f, 0.22f, 0.06f, 0.06f, C.INK2)
                // red, like the suitcase the guard asks about in unit 1
                k.rr(0.12f, 0.26f, 0.76f, 0.56f, 0.07f, C.RED)
                k.rect(0.30f, 0.26f, 0.06f, 0.56f, C.RED_D); k.rect(0.64f, 0.26f, 0.06f, 0.56f, C.RED_D)
                k.oval(0.18f, 0.80f, 0.12f, 0.12f, C.INK); k.oval(0.70f, 0.80f, 0.12f, 0.12f, C.INK)
            }
            "voila" -> {
                // an open hand holding something up: "here it is!"
                k.rr(0.30f, 0.56f, 0.40f, 0.26f, 0.08f, SKIN)
                for (i in 0 until 4) k.rr(0.30f + i * 0.10f, 0.46f, 0.08f, 0.16f, 0.04f, SKIN)
                k.rr(0.66f, 0.58f, 0.16f, 0.08f, 0.04f, SKIN)
                k.rect(0.36f, 0.82f, 0.28f, 0.14f, 0xFF3F6DB5.toInt())
                k.rr(0.34f, 0.22f, 0.32f, 0.24f, 0.04f, C.MUSTARD)
                k.line(0.20f, 0.16f, 0.28f, 0.24f, 0.04f, C.RED); k.line(0.80f, 0.16f, 0.72f, 0.24f, 0.04f, C.RED); k.line(0.50f, 0.04f, 0.50f, 0.14f, 0.04f, C.RED)
            }
            "today" -> {
                calendar(k, 0.16f, 0.14f, 0.68f, C.RED)
                k.ring(0.30f, 0.46f, 0.40f, 0.32f, 0.16f, 0.05f, C.RED)
            }
            "tomorrow" -> {
                calendar(k, 0.04f, 0.30f, 0.44f, C.GREY)
                calendar(k, 0.52f, 0.10f, 0.44f, C.RED)
                Draw.arrow(k.p, k.x + k.s * 0.50f, k.y + k.s * 0.86f, k.s * 0.26f, "right", C.RED)
            }
            "stairs" -> {
                for (i in 0 until 4) k.rect(0.12f + i * 0.19f, 0.70f - i * 0.17f, 0.80f - i * 0.19f, 0.17f, if (i % 2 == 0) STONE else STONE_D)
                Draw.arrow(k.p, k.x + k.s * 0.30f, k.y + k.s * 0.30f, k.s * 0.26f, "up", C.RED)
            }
            "lift" -> {
                k.rr(0.16f, 0.10f, 0.68f, 0.82f, 0.04f, C.INK2)
                k.rect(0.22f, 0.24f, 0.27f, 0.62f, STONE); k.rect(0.51f, 0.24f, 0.27f, 0.62f, STONE_D)
                k.poly(C.MUSTARD, 0.44f, 0.20f, 0.50f, 0.12f, 0.56f, 0.20f)
            }
            "bed" -> {
                k.rect(0.06f, 0.30f, 0.08f, 0.56f, WOOD_D); k.rect(0.86f, 0.46f, 0.08f, 0.40f, WOOD_D)
                k.rr(0.12f, 0.50f, 0.78f, 0.24f, 0.04f, 0xFF7C9FD0.toInt())
                k.rr(0.14f, 0.42f, 0.24f, 0.14f, 0.05f, C.WHITE)
                k.rect(0.06f, 0.72f, 0.88f, 0.06f, WOOD)
            }
            "floor", "ground" -> {
                k.rect(0.20f, 0.10f, 0.60f, 0.82f, CREAM)
                val hi = if (name == "floor") 1 else 0
                for (i in 0 until 3) {
                    val yy = 0.66f - i * 0.26f
                    if (i == hi) k.rect(0.20f, yy - 0.02f, 0.60f, 0.26f, C.MUSTARD)
                    k.rect(0.28f, yy + 0.04f, 0.14f, 0.14f, WATER); k.rect(0.58f, yy + 0.04f, 0.14f, 0.14f, WATER)
                }
                k.rect(0.20f, 0.06f, 0.60f, 0.06f, 0xFF6F7B99.toInt())
                Draw.arrow(k.p, k.x + k.s * 0.08f, k.y + k.s * (if (hi == 1) 0.51f else 0.77f), k.s * 0.16f, "right", C.RED)
            }
            "booking" -> {
                k.rr(0.20f, 0.12f, 0.60f, 0.78f, 0.05f, WOOD_D)
                k.rr(0.25f, 0.18f, 0.50f, 0.66f, 0.03f, C.WHITE)
                k.rr(0.38f, 0.07f, 0.24f, 0.12f, 0.03f, C.GREY)
                for (i in 0 until 3) { k.rect(0.42f, 0.30f + i * 0.16f, 0.26f, 0.05f, C.GREY_L); k.line(0.30f, 0.32f + i * 0.16f, 0.33f, 0.35f + i * 0.16f, 0.035f, C.GREEN); k.line(0.33f, 0.35f + i * 0.16f, 0.39f, 0.28f + i * 0.16f, 0.035f, C.GREEN) }
            }
            "breakfast" -> {
                k.oval(0.04f, 0.70f, 0.52f, 0.16f, C.GREY_L)
                k.rr(0.12f, 0.40f, 0.34f, 0.36f, 0.08f, C.WHITE)
                k.oval(0.15f, 0.39f, 0.28f, 0.09f, 0xFF6B4226.toInt())
                k.ring(0.42f, 0.46f, 0.16f, 0.18f, 0.07f, 0.04f, C.WHITE)
                Icons.draw(k.p, "croissant", k.x + k.s * 0.50f, k.y + k.s * 0.42f, k.s * 0.48f)
            }
            "water" -> glass(k, WATER, 0.75f)
            "juice" -> { glass(k, 0xFFF39A2D.toInt(), 0.8f); k.oval(0.56f, 0.06f, 0.30f, 0.30f, 0xFFF08A3C.toInt()); k.oval(0.62f, 0.12f, 0.18f, 0.18f, 0xFFFBD76B.toInt()) }
            "lemonade" -> { glass(k, 0xFFF7EE9A.toInt(), 0.85f); k.line(0.58f, 0.06f, 0.48f, 0.80f, 0.04f, C.RED); k.oval(0.42f, 0.50f, 0.05f, 0.05f, C.WHITE); k.oval(0.52f, 0.62f, 0.05f, 0.05f, C.WHITE); k.oval(0.44f, 0.70f, 0.04f, 0.04f, C.WHITE) }
            "hotchoc" -> {
                k.oval(0.10f, 0.76f, 0.70f, 0.16f, C.GREY_L)
                k.rr(0.20f, 0.36f, 0.48f, 0.48f, 0.10f, C.RED)
                k.ring(0.62f, 0.44f, 0.24f, 0.26f, 0.10f, 0.06f, C.RED)
                k.oval(0.23f, 0.33f, 0.42f, 0.13f, 0xFF6B4226.toInt())
                k.oval(0.34f, 0.34f, 0.10f, 0.07f, C.WHITE); k.oval(0.46f, 0.36f, 0.10f, 0.07f, C.WHITE)
                k.arc(0.36f, 0.18f, 0.06f, 90f, 270f, 0.04f, C.GREY); k.arc(0.52f, 0.18f, 0.06f, -90f, 90f, 0.04f, C.GREY)
            }
            "croque" -> {
                k.oval(0.06f, 0.66f, 0.88f, 0.22f, C.GREY_L)
                k.rr(0.18f, 0.48f, 0.64f, 0.26f, 0.04f, 0xFFC98A3C.toInt())
                k.rect(0.16f, 0.44f, 0.68f, 0.06f, 0xFFF7D24A.toInt())
                k.rect(0.18f, 0.38f, 0.64f, 0.07f, 0xFFE88F8F.toInt())
                k.rr(0.18f, 0.20f, 0.64f, 0.20f, 0.04f, 0xFFD9A04A.toInt())
                k.rr(0.24f, 0.17f, 0.52f, 0.08f, 0.04f, 0xFFF7D24A.toInt())
            }
            "crepe" -> {
                k.oval(0.06f, 0.56f, 0.88f, 0.30f, C.GREY_L)
                k.poly(0xFFE8C07A.toInt(), 0.16f, 0.72f, 0.50f, 0.20f, 0.86f, 0.70f)
                k.poly(0xFFD9A85A.toInt(), 0.50f, 0.20f, 0.86f, 0.70f, 0.60f, 0.72f)
                k.oval(0.30f, 0.56f, 0.07f, 0.06f, WOOD_D); k.oval(0.42f, 0.44f, 0.06f, 0.05f, WOOD_D); k.oval(0.46f, 0.62f, 0.06f, 0.05f, WOOD_D)
            }
            "fries" -> {
                for (i in 0 until 6) k.rect(0.26f + i * 0.085f, 0.14f + (i % 3) * 0.05f, 0.06f, 0.42f, 0xFFF7D24A.toInt())
                k.poly(C.RED, 0.22f, 0.44f, 0.78f, 0.44f, 0.70f, 0.90f, 0.30f, 0.90f)
                k.oval(0.40f, 0.58f, 0.20f, 0.18f, 0xFFF7D24A.toInt())
            }
            "icecream" -> {
                k.poly(0xFFD9A04A.toInt(), 0.32f, 0.50f, 0.68f, 0.50f, 0.50f, 0.94f)
                k.line(0.40f, 0.60f, 0.56f, 0.76f, 0.02f, WOOD_D); k.line(0.60f, 0.60f, 0.44f, 0.76f, 0.02f, WOOD_D)
                k.oval(0.26f, 0.26f, 0.48f, 0.34f, 0xFFF3B9B5.toInt())
                k.oval(0.32f, 0.06f, 0.36f, 0.32f, 0xFFFFF4C8.toInt())
                k.oval(0.46f, 0.02f, 0.10f, 0.10f, C.RED)
            }
            "bill" -> {
                k.poly(C.WHITE, 0.24f, 0.08f, 0.76f, 0.08f, 0.76f, 0.88f, 0.67f, 0.82f, 0.585f, 0.88f, 0.50f, 0.82f, 0.415f, 0.88f, 0.33f, 0.82f, 0.24f, 0.88f)
                for (i in 0 until 3) { k.rect(0.32f, 0.20f + i * 0.13f, 0.24f, 0.045f, C.GREY_L); k.rect(0.60f, 0.20f + i * 0.13f, 0.08f, 0.045f, C.GREY) }
                k.rect(0.32f, 0.60f, 0.36f, 0.02f, C.INK)
                k.text("€", 0.60f, 0.72f, 0.16f, C.INK)
            }
            "yum" -> {
                k.oval(0.10f, 0.12f, 0.80f, 0.80f, C.MUSTARD)
                k.arc(0.34f, 0.44f, 0.07f, 180f, 360f, 0.05f, C.INK); k.arc(0.66f, 0.44f, 0.07f, 180f, 360f, 0.05f, C.INK)
                k.arc(0.5f, 0.56f, 0.20f, 20f, 160f, 0.05f, C.INK)
                k.oval(0.50f, 0.68f, 0.14f, 0.16f, C.RED)
            }
            "thief" -> {
                k.oval(0.18f, 0.22f, 0.64f, 0.66f, SKIN)
                k.rr(0.14f, 0.40f, 0.72f, 0.18f, 0.08f, C.INK)
                k.oval(0.28f, 0.44f, 0.14f, 0.10f, C.WHITE); k.oval(0.58f, 0.44f, 0.14f, 0.10f, C.WHITE)
                k.oval(0.33f, 0.46f, 0.05f, 0.06f, C.INK); k.oval(0.63f, 0.46f, 0.05f, 0.06f, C.INK)
                k.oval(0.10f, 0.10f, 0.74f, 0.26f, C.INK)
                k.rect(0.46f, 0.04f, 0.04f, 0.08f, C.INK)
                k.arc(0.5f, 0.66f, 0.12f, 20f, 160f, 0.04f, C.INK)
            }
            "menucard" -> {
                k.rr(0.20f, 0.08f, 0.60f, 0.84f, 0.05f, C.RED_D)
                k.rr(0.25f, 0.13f, 0.50f, 0.74f, 0.03f, C.PAPER)
                k.text("MENU", 0.5f, 0.24f, 0.10f, C.INK, Font.BOLD)
                for (i in 0 until 4) { k.rect(0.31f, 0.36f + i * 0.11f, 0.24f, 0.04f, C.GREY); k.rect(0.61f, 0.36f + i * 0.11f, 0.08f, 0.04f, C.GREY_L) }
            }
            "tall", "short" -> {
                val tall = name == "tall"
                for (i in 0 until 5) k.rect(0.10f, 0.16f + i * 0.17f, 0.14f, 0.02f, C.GREY)
                k.rect(0.10f, 0.10f, 0.025f, 0.80f, C.GREY)
                person(k, 0.56f, 0.90f, if (tall) 0.80f else 0.50f, C.INK2)
                Draw.arrow(k.p, k.x + k.s * 0.86f, k.y + k.s * (if (tall) 0.24f else 0.62f), k.s * 0.20f, if (tall) "up" else "down", C.RED)
            }
            "moustache" -> {
                k.oval(0.14f, 0.14f, 0.72f, 0.74f, SKIN)
                k.oval(0.30f, 0.38f, 0.08f, 0.08f, C.INK); k.oval(0.62f, 0.38f, 0.08f, 0.08f, C.INK)
                k.oval(0.24f, 0.56f, 0.28f, 0.14f, C.INK); k.oval(0.48f, 0.56f, 0.28f, 0.14f, C.INK)
                k.oval(0.16f, 0.52f, 0.14f, 0.10f, C.INK); k.oval(0.70f, 0.52f, 0.14f, 0.10f, C.INK)
            }
            "glasses" -> {
                k.oval(0.14f, 0.14f, 0.72f, 0.74f, SKIN)
                k.ring(0.20f, 0.34f, 0.26f, 0.22f, 0.10f, 0.05f, C.INK); k.ring(0.54f, 0.34f, 0.26f, 0.22f, 0.10f, 0.05f, C.INK)
                k.rect(0.46f, 0.42f, 0.08f, 0.04f, C.INK)
                k.oval(0.30f, 0.41f, 0.07f, 0.07f, C.INK); k.oval(0.63f, 0.41f, 0.07f, 0.07f, C.INK)
                k.arc(0.5f, 0.62f, 0.12f, 20f, 160f, 0.04f, C.INK)
            }
            "hanger" -> {
                k.arc(0.5f, 0.22f, 0.08f, 150f, 420f, 0.045f, C.GREY)
                k.line(0.5f, 0.30f, 0.5f, 0.40f, 0.045f, C.GREY)
                k.line(0.5f, 0.40f, 0.12f, 0.66f, 0.06f, WOOD); k.line(0.5f, 0.40f, 0.88f, 0.66f, 0.06f, WOOD)
                k.line(0.12f, 0.66f, 0.88f, 0.66f, 0.06f, WOOD)
            }
            "hat" -> {
                k.oval(0.06f, 0.52f, 0.88f, 0.26f, WOOD_D)
                k.rr(0.26f, 0.22f, 0.48f, 0.44f, 0.12f, WOOD)
                k.rect(0.26f, 0.50f, 0.48f, 0.09f, C.INK)
            }
            "coat" -> { garment(k, 0xFFC9A46A.toInt(), true); k.rect(0.49f, 0.20f, 0.025f, 0.70f, WOOD_D); k.poly(WOOD_D, 0.40f, 0.16f, 0.50f, 0.34f, 0.60f, 0.16f)
                for (i in 0 until 3) k.oval(0.54f, 0.42f + i * 0.14f, 0.05f, 0.05f, C.INK) }
            "pull" -> { garment(k, C.GREEN, false); k.rect(0.28f, 0.72f, 0.44f, 0.08f, C.GREEN_D); k.arc(0.5f, 0.16f, 0.10f, 0f, 180f, 0.05f, C.GREEN_D) }
            "shirt" -> { garment(k, C.WHITE, false); k.poly(C.GREY_L, 0.40f, 0.16f, 0.50f, 0.30f, 0.60f, 0.16f); k.rect(0.49f, 0.28f, 0.02f, 0.50f, C.GREY_L)
                for (i in 0 until 3) k.oval(0.48f, 0.38f + i * 0.13f, 0.04f, 0.04f, C.GREY) }
            "jacket" -> { garment(k, C.INK2, false); k.poly(C.WHITE, 0.42f, 0.16f, 0.50f, 0.46f, 0.58f, 0.16f); k.poly(C.INK, 0.36f, 0.16f, 0.50f, 0.50f, 0.42f, 0.16f); k.poly(C.INK, 0.64f, 0.16f, 0.50f, 0.50f, 0.58f, 0.16f) }
            "scarf" -> {
                k.rr(0.18f, 0.22f, 0.64f, 0.22f, 0.11f, C.GREEN)
                k.rr(0.52f, 0.34f, 0.18f, 0.54f, 0.04f, C.GREEN_D)
                for (i in 0 until 3) k.rect(0.53f + i * 0.06f, 0.86f, 0.03f, 0.08f, C.GREEN_D)
                k.rect(0.52f, 0.62f, 0.18f, 0.05f, C.WHITE)
            }
            "dress" -> {
                k.poly(C.RED, 0.38f, 0.10f, 0.62f, 0.10f, 0.60f, 0.40f, 0.82f, 0.90f, 0.18f, 0.90f, 0.40f, 0.40f)
                k.rect(0.38f, 0.38f, 0.24f, 0.05f, C.RED_D)
                k.line(0.40f, 0.10f, 0.36f, 0.04f, 0.03f, C.RED_D); k.line(0.60f, 0.10f, 0.64f, 0.04f, 0.03f, C.RED_D)
            }
            "trousers" -> {
                k.poly(C.INK2, 0.26f, 0.10f, 0.74f, 0.10f, 0.80f, 0.92f, 0.56f, 0.92f, 0.50f, 0.36f, 0.44f, 0.92f, 0.20f, 0.92f)
                k.rect(0.26f, 0.10f, 0.48f, 0.08f, C.INK)
            }
            "shoes" -> {
                k.poly(WOOD_D, 0.06f, 0.52f, 0.30f, 0.44f, 0.44f, 0.58f, 0.52f, 0.64f, 0.52f, 0.74f, 0.06f, 0.74f)
                k.rect(0.06f, 0.72f, 0.46f, 0.05f, C.INK)
                k.poly(WOOD, 0.48f, 0.38f, 0.72f, 0.30f, 0.86f, 0.44f, 0.94f, 0.50f, 0.94f, 0.60f, 0.48f, 0.60f)
                k.rect(0.48f, 0.58f, 0.46f, 0.05f, C.INK)
            }
            "hat_big", "hat_small", "hat_ok" -> {
                k.oval(0.22f, 0.34f, 0.56f, 0.58f, SKIN)
                k.arc(0.5f, 0.70f, 0.12f, 20f, 160f, 0.04f, C.INK)
                when (name) {
                    "hat_big" -> { k.rr(0.14f, 0.10f, 0.72f, 0.48f, 0.12f, C.MUSTARD); k.oval(0.02f, 0.46f, 0.96f, 0.20f, C.MUSTARD_D) }
                    "hat_small" -> { k.oval(0.34f, 0.50f, 0.08f, 0.08f, C.INK); k.oval(0.58f, 0.50f, 0.08f, 0.08f, C.INK)
                        k.rr(0.40f, 0.16f, 0.20f, 0.16f, 0.04f, C.MUSTARD); k.oval(0.34f, 0.28f, 0.32f, 0.08f, C.MUSTARD_D) }
                    else -> { k.oval(0.34f, 0.56f, 0.08f, 0.08f, C.INK); k.oval(0.58f, 0.56f, 0.08f, 0.08f, C.INK)
                        k.rr(0.26f, 0.12f, 0.48f, 0.30f, 0.10f, C.MUSTARD); k.oval(0.12f, 0.34f, 0.76f, 0.16f, C.MUSTARD_D)
                        k.oval(0.80f, 0.08f, 0.14f, 0.14f, C.GREEN); k.line(0.84f, 0.15f, 0.86f, 0.18f, 0.03f, C.WHITE); k.line(0.86f, 0.18f, 0.91f, 0.12f, 0.03f, C.WHITE) }
                }
            }
            "mirror" -> {
                k.rr(0.22f, 0.06f, 0.56f, 0.80f, 0.20f, C.MUSTARD_D)
                k.rr(0.27f, 0.11f, 0.46f, 0.70f, 0.17f, GLASS)
                k.line(0.36f, 0.26f, 0.48f, 0.18f, 0.03f, C.WHITE); k.line(0.36f, 0.36f, 0.56f, 0.22f, 0.03f, C.WHITE)
                k.rect(0.46f, 0.86f, 0.08f, 0.06f, C.MUSTARD_D); k.rect(0.34f, 0.90f, 0.32f, 0.04f, C.MUSTARD_D)
            }
            "mouse" -> {
                k.arc(0.16f, 0.60f, 0.14f, 90f, 250f, 0.03f, 0xFFE8A0A0.toInt())
                k.oval(0.20f, 0.42f, 0.56f, 0.38f, C.GREY)
                k.oval(0.58f, 0.44f, 0.30f, 0.28f, C.GREY)
                k.oval(0.56f, 0.28f, 0.18f, 0.20f, C.GREY); k.oval(0.60f, 0.32f, 0.10f, 0.12f, 0xFFE8A0A0.toInt())
                k.oval(0.76f, 0.52f, 0.05f, 0.05f, C.INK); k.oval(0.86f, 0.58f, 0.05f, 0.05f, 0xFFE8A0A0.toInt())
            }
            "cathedral" -> {
                k.rect(0.16f, 0.22f, 0.22f, 0.70f, STONE); k.rect(0.62f, 0.22f, 0.22f, 0.70f, STONE)
                k.rect(0.38f, 0.44f, 0.24f, 0.48f, STONE_D)
                k.rect(0.14f, 0.18f, 0.26f, 0.05f, STONE_D); k.rect(0.60f, 0.18f, 0.26f, 0.05f, STONE_D)
                k.oval(0.41f, 0.48f, 0.18f, 0.18f, 0xFF7C3F8C.toInt()); k.oval(0.46f, 0.53f, 0.08f, 0.08f, C.MUSTARD)
                k.rr(0.44f, 0.72f, 0.12f, 0.20f, 0.05f, WOOD_D)
                k.rr(0.22f, 0.74f, 0.10f, 0.18f, 0.04f, WOOD_D); k.rr(0.68f, 0.74f, 0.10f, 0.18f, 0.04f, WOOD_D)
                for (tx in floatArrayOf(0.21f, 0.67f)) { k.rect(tx, 0.30f, 0.04f, 0.14f, C.INK2); k.rect(tx + 0.08f, 0.30f, 0.04f, 0.14f, C.INK2) }
            }
            "bridge" -> {
                k.rect(0.02f, 0.62f, 0.96f, 0.30f, WATER)
                k.rect(0.02f, 0.40f, 0.96f, 0.16f, STONE)
                k.rect(0.02f, 0.36f, 0.96f, 0.05f, STONE_D)
                for (i in 0 until 3) { val bx = 0.06f + i * 0.31f; k.rect(bx, 0.56f, 0.07f, 0.28f, STONE_D); if (i < 2) k.oval(bx + 0.07f, 0.44f, 0.24f, 0.30f, WATER) }
                k.rect(0.86f, 0.56f, 0.07f, 0.28f, STONE_D)
            }
            "river" -> {
                k.rect(0.04f, 0.10f, 0.92f, 0.80f, 0xFF8CC97A.toInt())
                k.poly(WATER, 0.04f, 0.56f, 0.30f, 0.30f, 0.56f, 0.48f, 0.96f, 0.24f, 0.96f, 0.50f, 0.60f, 0.74f, 0.32f, 0.56f, 0.04f, 0.84f)
                k.arc(0.40f, 0.50f, 0.06f, 200f, 340f, 0.025f, C.WHITE); k.arc(0.70f, 0.50f, 0.06f, 200f, 340f, 0.025f, C.WHITE)
            }
            "island" -> {
                k.rr(0.04f, 0.14f, 0.92f, 0.72f, 0.10f, WATER)
                k.oval(0.20f, 0.38f, 0.60f, 0.30f, 0xFFEADFA0.toInt())
                k.oval(0.28f, 0.40f, 0.44f, 0.20f, 0xFF8CC97A.toInt())
                k.rect(0.48f, 0.26f, 0.04f, 0.22f, WOOD_D); k.oval(0.38f, 0.14f, 0.24f, 0.20f, C.GREEN)
            }
            "route" -> {
                k.rect(0.47f, 0.14f, 0.06f, 0.78f, WOOD_D)
                k.poly(C.MUSTARD, 0.50f, 0.18f, 0.84f, 0.18f, 0.94f, 0.27f, 0.84f, 0.36f, 0.50f, 0.36f)
                k.poly(0xFF2F6FD0.toInt(), 0.50f, 0.44f, 0.16f, 0.44f, 0.06f, 0.53f, 0.16f, 0.62f, 0.50f, 0.62f)
                k.text("?", 0.70f, 0.27f, 0.16f, C.INK)
            }
            "cross_bridge" -> { draw(k, "bridge"); Draw.arrow(k.p, k.x + k.s * 0.5f, k.y + k.s * 0.22f, k.s * 0.30f, "right", C.RED) }
            "first_right" -> {
                k.rr(0.06f, 0.06f, 0.88f, 0.88f, 0.05f, 0xFFD9CDB0.toInt())
                k.rect(0.40f, 0.06f, 0.16f, 0.88f, C.WHITE)
                k.rect(0.40f, 0.52f, 0.54f, 0.14f, C.WHITE); k.rect(0.40f, 0.20f, 0.54f, 0.12f, C.WHITE)
                k.line(0.48f, 0.90f, 0.48f, 0.59f, 0.05f, C.RED); k.line(0.48f, 0.59f, 0.80f, 0.59f, 0.05f, C.RED)
                Draw.arrow(k.p, k.x + k.s * 0.82f, k.y + k.s * 0.59f, k.s * 0.18f, "right", C.RED)
                k.text("1", 0.72f, 0.42f, 0.14f, C.INK2)
            }
            "near" -> { k.oval(0.18f, 0.74f, 0.64f, 0.16f, C.GREEN); pin(k, 0.40f, 0.84f, 0.15f, C.INK2); pin(k, 0.60f, 0.84f, 0.15f, C.RED) }
            "far" -> {
                pin(k, 0.14f, 0.86f, 0.11f, C.INK2); pin(k, 0.86f, 0.44f, 0.11f, C.RED)
                var f = 0.14f
                while (f < 0.9f) { k.oval(0.16f + f * 0.66f, 0.84f - f * 0.42f, 0.04f, 0.04f, C.GREY); f += 0.14f }
            }
            "museum" -> {
                k.poly(STONE_D, 0.08f, 0.34f, 0.50f, 0.10f, 0.92f, 0.34f)
                k.rect(0.10f, 0.34f, 0.80f, 0.07f, STONE)
                for (i in 0 until 5) k.rect(0.15f + i * 0.155f, 0.41f, 0.08f, 0.40f, STONE)
                k.rect(0.08f, 0.81f, 0.84f, 0.09f, STONE_D)
                k.oval(0.44f, 0.20f, 0.12f, 0.10f, C.MUSTARD)
            }
            "painting" -> {
                k.rr(0.08f, 0.16f, 0.84f, 0.68f, 0.04f, C.MUSTARD_D)
                k.rect(0.14f, 0.22f, 0.72f, 0.56f, WATER)
                k.rect(0.14f, 0.56f, 0.72f, 0.22f, 0xFF8CC97A.toInt())
                k.oval(0.62f, 0.28f, 0.14f, 0.14f, C.MUSTARD)
                k.poly(C.GREEN_D, 0.20f, 0.58f, 0.36f, 0.36f, 0.52f, 0.58f)
            }
            "bigclock" -> clock(k, 10, true)
            "lilies" -> {
                k.rr(0.04f, 0.14f, 0.92f, 0.72f, 0.10f, 0xFF5F8FD6.toInt())
                k.oval(0.12f, 0.24f, 0.30f, 0.16f, C.GREEN); k.oval(0.50f, 0.20f, 0.34f, 0.18f, C.GREEN_D)
                k.oval(0.20f, 0.52f, 0.36f, 0.20f, C.GREEN_D); k.oval(0.58f, 0.56f, 0.30f, 0.16f, C.GREEN)
                k.oval(0.22f, 0.24f, 0.10f, 0.09f, 0xFFF3B9B5.toInt()); k.oval(0.62f, 0.22f, 0.12f, 0.10f, C.WHITE)
                k.oval(0.34f, 0.54f, 0.12f, 0.10f, 0xFFF3B9B5.toInt()); k.oval(0.68f, 0.58f, 0.10f, 0.08f, C.WHITE)
            }
            "guardian" -> {
                val img = Faces.get("icon_guardian", FaceSpec(hair = 0xFF8A8A8A.toInt(), hat = "cap", hatColor = C.INK, clothes = C.INK2, moustache = true), "neutral")
                val w = k.s * Faces.W / Faces.H
                k.p.image(img, k.x + (k.s - w) / 2f, k.y, w, k.s, false, 1f)
            }
            "he" -> Icons.draw(k.p, "man", k.x, k.y, k.s)
            "she" -> Icons.draw(k.p, "lady", k.x, k.y, k.s)
            "net" -> {
                k.line(0.14f, 0.90f, 0.50f, 0.50f, 0.07f, WOOD)
                k.oval(0.38f, 0.08f, 0.54f, 0.50f, C.alpha(C.WHITE, 0.55f))
                k.ring(0.38f, 0.08f, 0.54f, 0.50f, 0.25f, 0.05f, C.INK2)
                for (i in 1 until 4) { k.line(0.38f + i * 0.135f, 0.14f, 0.38f + i * 0.135f, 0.52f, 0.015f, C.INK2); k.line(0.42f, 0.10f + i * 0.115f, 0.88f, 0.10f + i * 0.115f, 0.015f, C.INK2) }
            }
            "page", "note" -> {
                k.poly(C.WHITE, 0.22f, 0.08f, 0.64f, 0.08f, 0.80f, 0.24f, 0.80f, 0.92f, 0.22f, 0.92f)
                k.poly(C.GREY_L, 0.64f, 0.08f, 0.80f, 0.24f, 0.64f, 0.24f)
                for (i in 0 until 4) k.rect(0.30f, 0.36f + i * 0.13f, if (i == 3) 0.26f else 0.42f, 0.04f, C.INK2)
            }
            "map" -> {
                k.poly(C.PAPER2, 0.06f, 0.20f, 0.34f, 0.12f, 0.34f, 0.84f, 0.06f, 0.92f)
                k.poly(C.WHITE, 0.34f, 0.12f, 0.66f, 0.22f, 0.66f, 0.92f, 0.34f, 0.84f)
                k.poly(C.PAPER2, 0.66f, 0.22f, 0.94f, 0.12f, 0.94f, 0.84f, 0.66f, 0.92f)
                k.line(0.16f, 0.74f, 0.40f, 0.52f, 0.04f, C.RED); k.line(0.40f, 0.52f, 0.62f, 0.62f, 0.04f, C.RED); k.line(0.62f, 0.62f, 0.80f, 0.36f, 0.04f, C.RED)
                pin(k, 0.80f, 0.36f, 0.07f, C.RED)
            }
            "sketch" -> {
                k.rr(0.12f, 0.08f, 0.62f, 0.84f, 0.04f, C.WHITE)
                k.oval(0.26f, 0.22f, 0.34f, 0.40f, C.GREY_L)
                k.oval(0.34f, 0.36f, 0.05f, 0.05f, C.INK); k.oval(0.47f, 0.36f, 0.05f, 0.05f, C.INK)
                k.arc(0.43f, 0.48f, 0.07f, 20f, 160f, 0.03f, C.INK)
                k.line(0.62f, 0.86f, 0.90f, 0.42f, 0.07f, C.MUSTARD); k.poly(C.INK, 0.58f, 0.92f, 0.60f, 0.83f, 0.65f, 0.87f)
            }
            "eiffel" -> {
                k.poly(STONE_D, 0.18f, 0.94f, 0.42f, 0.46f, 0.58f, 0.46f, 0.82f, 0.94f, 0.68f, 0.94f, 0.50f, 0.62f, 0.32f, 0.94f)
                k.poly(STONE_D, 0.42f, 0.46f, 0.47f, 0.10f, 0.53f, 0.10f, 0.58f, 0.46f)
                k.rect(0.495f, 0.02f, 0.012f, 0.10f, STONE_D)
                k.rect(0.36f, 0.46f, 0.28f, 0.04f, C.INK2); k.rect(0.28f, 0.68f, 0.44f, 0.04f, C.INK2)
            }
            "arc" -> {
                k.rect(0.14f, 0.20f, 0.72f, 0.72f, STONE)
                k.rect(0.10f, 0.14f, 0.80f, 0.08f, STONE_D)
                k.rr(0.34f, 0.44f, 0.32f, 0.60f, 0.16f, WATER)
                k.rect(0.34f, 0.80f, 0.32f, 0.12f, WATER)
                k.rect(0.14f, 0.32f, 0.72f, 0.04f, STONE_D)
            }
            "pyramid" -> {
                k.poly(GLASS, 0.50f, 0.14f, 0.94f, 0.86f, 0.06f, 0.86f)
                k.poly(WATER, 0.50f, 0.14f, 0.94f, 0.86f, 0.56f, 0.86f)
                k.line(0.50f, 0.14f, 0.36f, 0.86f, 0.015f, C.INK2); k.line(0.50f, 0.14f, 0.70f, 0.86f, 0.015f, C.INK2)
                k.line(0.30f, 0.50f, 0.72f, 0.50f, 0.015f, C.INK2); k.line(0.18f, 0.68f, 0.83f, 0.68f, 0.015f, C.INK2)
            }
            "moon" -> { k.oval(0.06f, 0.06f, 0.88f, 0.88f, C.INK); k.oval(0.22f, 0.20f, 0.56f, 0.56f, 0xFFFDF3C4.toInt()); k.oval(0.38f, 0.14f, 0.50f, 0.50f, C.INK)
                k.oval(0.70f, 0.62f, 0.05f, 0.05f, C.WHITE); k.oval(0.26f, 0.74f, 0.04f, 0.04f, C.WHITE) }
            "sun" -> {
                k.oval(0.24f, 0.24f, 0.52f, 0.52f, C.MUSTARD)
                for (i in 0 until 8) { val a = Math.toRadians((i * 45).toDouble()); k.line(0.5f + 0.32f * Math.cos(a).toFloat(), 0.5f + 0.32f * Math.sin(a).toFloat(), 0.5f + 0.44f * Math.cos(a).toFloat(), 0.5f + 0.44f * Math.sin(a).toFloat(), 0.05f, C.MUSTARD) }
            }
            else -> return Icons2.draw(k, name)
        }
        return true
    }
}
