package detective.core

/**
 * Pictures for the words of chapter 2: the phone, vehicles, feelings,
 * question words, games, ballet, the sewers, mice, cheese and the farm,
 * the theatre. Drawn in code on a unit square, like MoreIcons.
 */
object Icons2 {
    private val SKIN = 0xFFF2C9A0.toInt()
    private val WOOD = 0xFFB07A3F.toInt()
    private val WOOD_D = 0xFF7A5226.toInt()
    private val STONE = 0xFFC9C2B2.toInt()
    private val STONE_D = 0xFF9C9484.toInt()
    private val CHEESE = 0xFFF3E3A6.toInt()
    private val CHEESE_D = 0xFFD9C37A.toInt()
    private val RIND = 0xFFFFFDF5.toInt()
    private val BLUE = 0xFF3F6DB5.toInt()
    private val BLUE_L = 0xFF5F8FD6.toInt()
    private val PINK = 0xFFF3B9B5.toInt()
    private val GRASS = 0xFF5E9E52.toInt()
    private val MOUSE = 0xFF9AA5C0.toInt()

    private val NAMES = hashSetOf(
        "phone", "phone_hold", "moment", "no_thanks", "good_day", "truck", "truck_red", "car", "car_red", "bike", "moto", "bus", "bus_red",
        "cheese", "stolen", "fast", "downstairs", "because",
        "tennis", "foot", "petanque", "dance", "music", "heart", "heart_no", "heart_plus", "play",
        "boule", "cochonnet", "throw", "point", "trophy", "lost", "clap",
        "plie", "tendu", "jete", "releve", "pointes", "up", "down", "faster",
        "hurry", "now", "wait", "stop", "sewer", "tunnel", "torch", "through", "along",
        "rat", "tail", "ears", "hole", "shh", "good", "bad", "kind", "mean", "crown", "queen", "cheer",
        "hungry", "very_hungry", "full", "eat", "everyone", "more_cheese",
        "brie", "camembert", "comte", "roquefort", "chevre", "cow", "milk", "grass", "rain", "no_rain", "first", "then", "after",
        "help", "enough", "not_enough", "lots", "too_much", "need", "missing",
        "lets_go", "show", "cinema", "restaurant", "watch", "kitchen", "bar", "stage", "backstage", "table", "maitre",
        "label_aop", "label_igp", "label_none", "tree"
    )
    private val PREFIXES = arrayOf("feel:", "q:", "feet:")

    fun samples(): List<String> {
        val out = ArrayList<String>(NAMES.sorted())
        for (m in listOf("angry", "sad", "scared", "shy", "curious", "smile")) out.add("feel:$m")
        for (q in listOf("who", "what", "why", "when", "where", "how")) out.add("q:$q")
        out.add("feet:1"); out.add("feet:2")
        out.addAll(Icons3.samples())
        return out
    }

    fun knows(name: String): Boolean {
        if (NAMES.contains(name)) return true
        for (pf in PREFIXES) if (name.startsWith(pf) && name.length > pf.length) return true
        return Icons3.knows(name)
    }

    // ---------- little helpers ----------

    private fun person(k: Icons.K, cx: Float, footY: Float, h: Float, c: Int) {
        val head = h * 0.26f
        k.oval(cx - head / 2f, footY - h, head, head, SKIN)
        k.rr(cx - h * 0.17f, footY - h * 0.72f, h * 0.34f, h * 0.44f, 0.03f, c)
        k.rect(cx - h * 0.13f, footY - h * 0.30f, h * 0.10f, h * 0.30f, C.INK)
        k.rect(cx + h * 0.03f, footY - h * 0.30f, h * 0.10f, h * 0.30f, C.INK)
    }

    private fun qmark(k: Icons.K, x: Float, y: Float, s: Float, c: Int) {
        k.oval(x, y, s, s, c)
        k.text("?", x + s / 2f, y + s / 2f + s * 0.02f, s * 0.8f, C.WHITE)
    }

    private fun wheel(k: Icons.K, cx: Float, cy: Float, r: Float) {
        k.oval(cx - r, cy - r, r * 2f, r * 2f, C.INK)
        k.oval(cx - r * 0.5f, cy - r * 0.5f, r, r, 0xFF8A8FA0.toInt())
    }

    /** A van or truck from the side. */
    private fun truck(k: Icons.K, body: Int) {
        k.rr(0.04f, 0.26f, 0.62f, 0.46f, 0.03f, C.INK)
        k.rr(0.06f, 0.28f, 0.58f, 0.42f, 0.02f, body)
        k.rr(0.64f, 0.40f, 0.30f, 0.32f, 0.04f, C.INK)
        k.rr(0.66f, 0.42f, 0.26f, 0.28f, 0.03f, BLUE_L)
        k.rect(0.74f, 0.44f, 0.16f, 0.12f, C.SKY)
        k.rect(0.16f, 0.38f, 0.28f, 0.18f, C.MUSTARD)
        k.rect(0.20f, 0.42f, 0.06f, 0.06f, C.MUSTARD_D)
        wheel(k, 0.22f, 0.76f, 0.09f)
        wheel(k, 0.78f, 0.76f, 0.09f)
    }

    private fun car(k: Icons.K, body: Int) {
        k.poly(C.INK, 0.06f, 0.70f, 0.06f, 0.50f, 0.26f, 0.48f, 0.38f, 0.30f, 0.66f, 0.30f, 0.80f, 0.48f, 0.94f, 0.52f, 0.94f, 0.70f)
        k.poly(body, 0.09f, 0.67f, 0.09f, 0.52f, 0.28f, 0.50f, 0.40f, 0.33f, 0.64f, 0.33f, 0.77f, 0.50f, 0.91f, 0.54f, 0.91f, 0.67f)
        k.poly(C.SKY, 0.42f, 0.36f, 0.52f, 0.36f, 0.52f, 0.48f, 0.33f, 0.48f)
        k.poly(C.SKY, 0.56f, 0.36f, 0.63f, 0.36f, 0.72f, 0.48f, 0.56f, 0.48f)
        wheel(k, 0.26f, 0.72f, 0.09f)
        wheel(k, 0.74f, 0.72f, 0.09f)
    }

    private fun bus(k: Icons.K, body: Int) {
        k.rr(0.04f, 0.18f, 0.92f, 0.56f, 0.06f, C.INK)
        k.rr(0.06f, 0.20f, 0.88f, 0.52f, 0.05f, body)
        for (i in 0 until 4) k.rect(0.10f + i * 0.20f, 0.28f, 0.15f, 0.16f, C.SKY)
        k.rect(0.06f, 0.54f, 0.88f, 0.04f, C.WHITE)
        wheel(k, 0.24f, 0.76f, 0.09f)
        wheel(k, 0.76f, 0.76f, 0.09f)
    }

    /** A round soft cheese with a white rind, a slice cut out. */
    private fun softCheese(k: Icons.K, rind: Int, inside: Int) {
        val side = 0xFFE6DCC0.toInt()
        // a thick round wheel, seen from a little above, with a slice cut out
        k.oval(0.04f, 0.42f, 0.92f, 0.40f, C.INK)
        k.rect(0.04f, 0.38f, 0.92f, 0.24f, C.INK)
        k.oval(0.04f, 0.18f, 0.92f, 0.40f, C.INK)
        k.oval(0.07f, 0.44f, 0.86f, 0.35f, side)
        k.rect(0.07f, 0.38f, 0.86f, 0.24f, side)
        k.oval(0.07f, 0.21f, 0.86f, 0.34f, rind)
        k.poly(C.INK, 0.50f, 0.38f, 0.93f, 0.40f, 0.93f, 0.62f, 0.50f, 0.60f)
        k.poly(inside, 0.52f, 0.40f, 0.91f, 0.42f, 0.91f, 0.60f, 0.52f, 0.58f)
        k.poly(rind, 0.50f, 0.38f, 0.93f, 0.40f, 0.93f, 0.36f, 0.62f, 0.26f)
    }

    /** A wedge of firm cheese. */
    private fun wedge(k: Icons.K, inside: Int, rind: Int, holes: Boolean, veins: Boolean) {
        k.poly(C.INK, 0.06f, 0.74f, 0.94f, 0.74f, 0.94f, 0.36f, 0.10f, 0.58f)
        k.poly(inside, 0.10f, 0.71f, 0.91f, 0.71f, 0.91f, 0.40f, 0.13f, 0.59f)
        k.poly(rind, 0.91f, 0.40f, 0.91f, 0.71f, 0.86f, 0.71f, 0.86f, 0.42f)
        if (holes) { k.oval(0.30f, 0.60f, 0.08f, 0.06f, CHEESE_D); k.oval(0.56f, 0.52f, 0.10f, 0.07f, CHEESE_D); k.oval(0.70f, 0.62f, 0.07f, 0.05f, CHEESE_D) }
        if (veins) for ((x, y) in listOf(0.28f to 0.62f, 0.44f to 0.56f, 0.58f to 0.64f, 0.72f to 0.50f, 0.66f to 0.58f)) k.oval(x, y, 0.07f, 0.04f, 0xFF4A6A8A.toInt())
    }

    private fun jar(k: Icons.K, fill: Float, over: Boolean) {
        k.rr(0.24f, 0.18f, 0.52f, 0.72f, 0.06f, C.INK)
        k.rr(0.27f, 0.21f, 0.46f, 0.66f, 0.04f, C.WHITE)
        val top = 0.87f - 0.66f * fill
        k.rect(0.27f, top, 0.46f, 0.87f - top, CHEESE)
        if (over) { k.oval(0.18f, 0.10f, 0.20f, 0.14f, CHEESE); k.oval(0.56f, 0.08f, 0.24f, 0.16f, CHEESE); k.oval(0.36f, 0.04f, 0.26f, 0.18f, CHEESE) }
    }

    private fun plate(k: Icons.K, food: Boolean) {
        k.oval(0.08f, 0.40f, 0.84f, 0.44f, C.GREY_L)
        k.oval(0.20f, 0.48f, 0.60f, 0.28f, C.WHITE)
        if (food) { k.oval(0.34f, 0.52f, 0.12f, 0.08f, CHEESE_D); k.oval(0.52f, 0.58f, 0.08f, 0.06f, CHEESE_D) }
    }

    /** A dancer's legs and feet, for the ballet words. */
    private fun legs(k: Icons.K, pose: String) {
        val ink = C.INK
        val tight = 0xFFF3B9B5.toInt()
        k.rr(0.32f, 0.06f, 0.36f, 0.20f, 0.06f, tight)
        when (pose) {
            "plie" -> {
                k.line(0.40f, 0.24f, 0.24f, 0.56f, 0.09f, tight); k.line(0.24f, 0.56f, 0.30f, 0.86f, 0.09f, tight)
                k.line(0.60f, 0.24f, 0.76f, 0.56f, 0.09f, tight); k.line(0.76f, 0.56f, 0.70f, 0.86f, 0.09f, tight)
                k.rr(0.18f, 0.84f, 0.18f, 0.07f, 0.03f, ink); k.rr(0.64f, 0.84f, 0.18f, 0.07f, 0.03f, ink)
            }
            "tendu" -> {
                k.line(0.42f, 0.24f, 0.42f, 0.86f, 0.09f, tight); k.rr(0.32f, 0.84f, 0.18f, 0.07f, 0.03f, ink)
                k.line(0.58f, 0.24f, 0.86f, 0.86f, 0.08f, tight); k.poly(ink, 0.82f, 0.82f, 0.94f, 0.90f, 0.86f, 0.92f)
            }
            "jete" -> {
                k.line(0.42f, 0.24f, 0.14f, 0.62f, 0.09f, tight); k.poly(ink, 0.10f, 0.58f, 0.18f, 0.64f, 0.08f, 0.68f)
                k.line(0.58f, 0.24f, 0.88f, 0.52f, 0.09f, tight); k.poly(ink, 0.84f, 0.48f, 0.94f, 0.50f, 0.88f, 0.58f)
                k.rect(0.10f, 0.90f, 0.80f, 0.03f, STONE_D)
            }
            "releve" -> {
                k.line(0.44f, 0.24f, 0.44f, 0.78f, 0.09f, tight); k.line(0.56f, 0.24f, 0.56f, 0.78f, 0.09f, tight)
                k.poly(ink, 0.38f, 0.76f, 0.50f, 0.76f, 0.47f, 0.88f, 0.41f, 0.88f); k.poly(ink, 0.50f, 0.76f, 0.62f, 0.76f, 0.59f, 0.88f, 0.53f, 0.88f)
                k.rect(0.10f, 0.92f, 0.80f, 0.03f, STONE_D)
                Draw.arrow(k.p, k.x + k.s * 0.80f, k.y + k.s * 0.60f, k.s * 0.20f, "up", C.RED)
            }
            "pointes" -> {
                k.line(0.46f, 0.24f, 0.46f, 0.84f, 0.09f, tight); k.line(0.54f, 0.24f, 0.54f, 0.84f, 0.09f, tight)
                k.rect(0.42f, 0.82f, 0.16f, 0.10f, PINK); k.rect(0.42f, 0.90f, 0.16f, 0.03f, ink)
                k.line(0.40f, 0.70f, 0.48f, 0.62f, 0.02f, C.RED_D); k.line(0.60f, 0.70f, 0.52f, 0.62f, 0.02f, C.RED_D)
                k.rect(0.10f, 0.94f, 0.80f, 0.03f, STONE_D)
            }
        }
    }

    // ---------- drawing ----------

    internal fun draw(k: Icons.K, name: String): Boolean {
        val colon = name.indexOf(':')
        if (colon > 0) {
            val kind = name.substring(0, colon)
            val arg = name.substring(colon + 1)
            when (kind) {
                "feel" -> {
                    val img = Faces.get("feelicon", FaceSpec(hair = 0xFF8A5A2B.toInt(), hairStyle = "short", clothes = 0xFF8FA0B8.toInt()), arg)
                    val w = k.s * Faces.W / Faces.H
                    k.p.image(img, k.x + (k.s - w) / 2f, k.y, w, k.s, false, 1f)
                }
                "q" -> {
                    // a small picture of the question, with a big question mark
                    when (arg) {
                        "who" -> person(k, 0.36f, 0.92f, 0.70f, BLUE)
                        "what" -> { k.rr(0.10f, 0.40f, 0.50f, 0.48f, 0.04f, WOOD); k.rect(0.10f, 0.40f, 0.50f, 0.10f, WOOD_D) }
                        "why" -> { }
                        "when" -> { k.oval(0.08f, 0.36f, 0.54f, 0.54f, C.INK); k.oval(0.12f, 0.40f, 0.46f, 0.46f, C.WHITE); k.line(0.35f, 0.63f, 0.35f, 0.47f, 0.05f, C.INK); k.line(0.35f, 0.63f, 0.46f, 0.66f, 0.05f, C.INK) }
                        "where" -> { k.poly(C.RED, 0.20f, 0.56f, 0.48f, 0.56f, 0.34f, 0.92f); k.oval(0.16f, 0.32f, 0.36f, 0.36f, C.RED); k.oval(0.28f, 0.44f, 0.12f, 0.12f, C.WHITE) }
                        "how" -> { k.oval(0.10f, 0.40f, 0.48f, 0.48f, STONE_D); k.oval(0.24f, 0.54f, 0.20f, 0.20f, C.WHITE); for (i in 0 until 6) { val a = Math.toRadians((i * 60).toDouble()); k.oval(0.30f + 0.25f * Math.cos(a).toFloat(), 0.60f + 0.25f * Math.sin(a).toFloat(), 0.10f, 0.10f, STONE_D) } }
                    }
                    if (arg == "why") qmark(k, 0.14f, 0.14f, 0.72f, C.RED) else qmark(k, 0.50f, 0.06f, 0.44f, C.RED)
                }
                "feet" -> {
                    // ballet positions seen from above: heels together (1) or apart (2), toes turned out
                    val gap = if (arg == "2") 0.16f else 0.0f
                    k.poly(PINK, 0.46f - gap, 0.56f, 0.08f, 0.38f, 0.06f, 0.50f, 0.44f - gap, 0.70f)
                    k.oval(0.38f - gap, 0.54f, 0.14f, 0.16f, PINK)
                    k.oval(0.02f, 0.36f, 0.12f, 0.16f, PINK)
                    k.poly(PINK, 0.54f + gap, 0.56f, 0.92f, 0.38f, 0.94f, 0.50f, 0.56f + gap, 0.70f)
                    k.oval(0.48f + gap, 0.54f, 0.14f, 0.16f, PINK)
                    k.oval(0.86f, 0.36f, 0.12f, 0.16f, PINK)
                    k.text(arg, 0.5f, 0.24f, 0.34f, C.INK)
                }
                else -> return Icons3.draw(k, name)
            }
            return true
        }
        when (name) {
            "phone" -> {
                // an old telephone with its handset on top
                k.poly(C.RED, 0.14f, 0.88f, 0.24f, 0.46f, 0.76f, 0.46f, 0.86f, 0.88f)
                k.oval(0.36f, 0.56f, 0.28f, 0.24f, C.WHITE)
                for (i in 0 until 8) { val a = Math.toRadians((i * 45).toDouble()); k.oval(0.48f + 0.09f * Math.cos(a).toFloat(), 0.66f + 0.08f * Math.sin(a).toFloat(), 0.04f, 0.04f, C.INK) }
                k.rr(0.10f, 0.22f, 0.80f, 0.16f, 0.07f, C.RED_D)
                k.rr(0.06f, 0.18f, 0.20f, 0.26f, 0.06f, C.RED_D); k.rr(0.74f, 0.18f, 0.20f, 0.26f, 0.06f, C.RED_D)
                k.rect(0.30f, 0.38f, 0.06f, 0.10f, C.INK); k.rect(0.64f, 0.38f, 0.06f, 0.10f, C.INK)
            }
            "phone_hold" -> { draw(k, "phone"); k.oval(0.58f, 0.02f, 0.40f, 0.40f, C.WHITE); k.ring(0.58f, 0.02f, 0.40f, 0.40f, 0.20f, 0.04f, C.INK); k.line(0.78f, 0.22f, 0.78f, 0.10f, 0.04f, C.INK); k.line(0.78f, 0.22f, 0.88f, 0.24f, 0.04f, C.INK) }
            "moment" -> {
                k.rect(0.24f, 0.08f, 0.52f, 0.06f, WOOD_D); k.rect(0.24f, 0.86f, 0.52f, 0.06f, WOOD_D)
                k.poly(0xFFD9ECF7.toInt(), 0.30f, 0.14f, 0.70f, 0.14f, 0.52f, 0.50f, 0.70f, 0.86f, 0.30f, 0.86f, 0.48f, 0.50f)
                k.poly(CHEESE_D, 0.36f, 0.86f, 0.64f, 0.86f, 0.50f, 0.66f)
                k.poly(CHEESE_D, 0.40f, 0.24f, 0.60f, 0.24f, 0.50f, 0.42f)
            }
            "no_thanks" -> { palm(k); k.oval(0.56f, 0.56f, 0.40f, 0.40f, C.RED); k.line(0.64f, 0.76f, 0.88f, 0.76f, 0.07f, C.WHITE) }
            "good_day" -> { Icons.draw(k.p, "wave", k.x, k.y + k.s * 0.2f, k.s * 0.8f); k.oval(0.62f, 0.04f, 0.32f, 0.32f, C.MUSTARD) }
            "truck" -> truck(k, C.WHITE)
            "truck_red" -> truck(k, C.RED)
            "car" -> car(k, BLUE_L)
            "car_red" -> car(k, C.RED)
            "bus" -> bus(k, C.GREEN)
            "bus_red" -> bus(k, C.RED)
            "bike" -> {
                k.ring(0.06f, 0.48f, 0.34f, 0.34f, 0.17f, 0.05f, C.INK); k.ring(0.60f, 0.48f, 0.34f, 0.34f, 0.17f, 0.05f, C.INK)
                k.line(0.23f, 0.65f, 0.44f, 0.40f, 0.05f, C.RED); k.line(0.44f, 0.40f, 0.77f, 0.65f, 0.05f, C.RED); k.line(0.23f, 0.65f, 0.52f, 0.65f, 0.05f, C.RED)
                k.line(0.52f, 0.65f, 0.44f, 0.40f, 0.05f, C.RED); k.line(0.68f, 0.30f, 0.77f, 0.65f, 0.05f, C.INK); k.rect(0.38f, 0.34f, 0.14f, 0.05f, C.INK); k.rect(0.62f, 0.28f, 0.14f, 0.05f, C.INK)
            }
            "moto" -> {
                wheel(k, 0.20f, 0.70f, 0.15f); wheel(k, 0.80f, 0.70f, 0.15f)
                k.poly(C.INK, 0.24f, 0.62f, 0.36f, 0.42f, 0.70f, 0.42f, 0.80f, 0.62f)
                k.poly(BLUE_L, 0.30f, 0.58f, 0.40f, 0.46f, 0.66f, 0.46f, 0.74f, 0.58f)
                k.rr(0.40f, 0.34f, 0.24f, 0.10f, 0.04f, C.INK); k.line(0.70f, 0.42f, 0.78f, 0.24f, 0.05f, C.INK)
            }
            "cheese" -> softCheese(k, RIND, CHEESE)
            "brie" -> { softCheese(k, RIND, CHEESE); k.text("brie", 0.32f, 0.54f, 0.16f, C.INK2) }
            "camembert" -> {
                k.oval(0.14f, 0.20f, 0.72f, 0.72f, WOOD)
                k.oval(0.18f, 0.24f, 0.64f, 0.64f, 0xFFD9B47A.toInt())
                k.oval(0.24f, 0.30f, 0.52f, 0.52f, RIND)
                k.text("Cam", 0.5f, 0.56f, 0.18f, C.RED)
            }
            "comte" -> wedge(k, 0xFFF2D27A.toInt(), 0xFFB98A55.toInt(), false, false)
            "roquefort" -> wedge(k, 0xFFF4F1E6.toInt(), 0xFFD9D2BE.toInt(), false, true)
            "chevre" -> {
                k.rr(0.14f, 0.40f, 0.72f, 0.28f, 0.14f, C.INK); k.rr(0.16f, 0.42f, 0.68f, 0.24f, 0.12f, RIND)
                k.oval(0.70f, 0.12f, 0.20f, 0.20f, C.WHITE); k.oval(0.60f, 0.06f, 0.10f, 0.14f, C.WHITE); k.oval(0.75f, 0.17f, 0.04f, 0.04f, C.INK)
            }
            "stolen" -> { softCheese(k, RIND, CHEESE); Icons.draw(k.p, "thief", k.x + k.s * 0.50f, k.y, k.s * 0.50f) }
            "more_cheese" -> { softCheese(k, RIND, CHEESE); k.rect(0.62f, 0.10f, 0.08f, 0.28f, C.GREEN); k.rect(0.52f, 0.20f, 0.28f, 0.08f, C.GREEN) }
            "fast", "faster" -> {
                person(k, 0.62f, 0.92f, 0.80f, BLUE)
                for (i in 0 until 3) k.rect(0.06f, 0.36f + i * 0.14f, 0.26f - i * 0.05f, 0.04f, C.INK2)
            }
            "hurry" -> { draw(k, "fast"); k.text("!", 0.86f, 0.22f, 0.34f, C.RED) }
            "downstairs" -> { Icons.draw(k.p, "stairs", k.x, k.y, k.s); Draw.arrow(k.p, k.x + k.s * 0.72f, k.y + k.s * 0.30f, k.s * 0.26f, "down", C.RED) }
            "because" -> { k.rect(0.10f, 0.44f, 0.56f, 0.12f, C.INK); k.poly(C.INK, 0.62f, 0.30f, 0.92f, 0.50f, 0.62f, 0.70f); k.oval(0.08f, 0.08f, 0.24f, 0.24f, C.MUSTARD) }
            // ---- games ----
            "tennis" -> { k.oval(0.10f, 0.06f, 0.50f, 0.56f, C.INK); k.oval(0.14f, 0.10f, 0.42f, 0.48f, 0xFFE9EEF7.toInt()); for (i in 1 until 4) { k.line(0.14f + i * 0.105f, 0.12f, 0.14f + i * 0.105f, 0.56f, 0.015f, C.INK2); k.line(0.16f, 0.12f + i * 0.11f, 0.54f, 0.12f + i * 0.11f, 0.015f, C.INK2) }
                k.line(0.40f, 0.58f, 0.60f, 0.94f, 0.08f, WOOD_D); k.oval(0.64f, 0.56f, 0.26f, 0.26f, 0xFFD6E64A.toInt()) }
            "foot" -> { k.oval(0.12f, 0.12f, 0.76f, 0.76f, C.INK); k.oval(0.15f, 0.15f, 0.70f, 0.70f, C.WHITE); k.poly(C.INK, 0.50f, 0.32f, 0.64f, 0.42f, 0.59f, 0.58f, 0.41f, 0.58f, 0.36f, 0.42f)
                k.poly(C.INK, 0.20f, 0.34f, 0.30f, 0.30f, 0.30f, 0.48f, 0.18f, 0.52f); k.poly(C.INK, 0.80f, 0.34f, 0.70f, 0.30f, 0.70f, 0.48f, 0.82f, 0.52f); k.poly(C.INK, 0.40f, 0.82f, 0.46f, 0.70f, 0.56f, 0.70f, 0.60f, 0.82f) }
            "petanque" -> { draw(k, "boule"); k.oval(0.70f, 0.18f, 0.16f, 0.16f, WOOD) }
            "boule" -> { k.oval(0.14f, 0.20f, 0.64f, 0.64f, C.INK); k.oval(0.17f, 0.23f, 0.58f, 0.58f, MOUSE); k.oval(0.26f, 0.30f, 0.20f, 0.14f, C.WHITE); for (i in 0 until 3) k.line(0.20f, 0.46f + i * 0.08f, 0.72f, 0.46f + i * 0.08f, 0.015f, C.INK2) }
            "cochonnet" -> { k.oval(0.30f, 0.30f, 0.40f, 0.40f, C.INK); k.oval(0.33f, 0.33f, 0.34f, 0.34f, WOOD); k.oval(0.40f, 0.38f, 0.10f, 0.08f, 0xFFD9A56A.toInt()) }
            "throw" -> { person(k, 0.30f, 0.94f, 0.80f, C.GREEN); k.line(0.38f, 0.38f, 0.56f, 0.24f, 0.06f, SKIN); k.oval(0.62f, 0.10f, 0.22f, 0.22f, MOUSE); for (i in 0 until 3) k.rect(0.86f, 0.10f + i * 0.08f, 0.10f, 0.03f, C.INK2) }
            "point" -> { k.oval(0.10f, 0.10f, 0.80f, 0.80f, C.GREEN); k.text("+1", 0.5f, 0.52f, 0.40f, C.WHITE) }
            "trophy" -> { k.poly(C.MUSTARD, 0.26f, 0.12f, 0.74f, 0.12f, 0.68f, 0.50f, 0.32f, 0.50f); k.ring(0.08f, 0.16f, 0.24f, 0.24f, 0.12f, 0.05f, C.MUSTARD); k.ring(0.68f, 0.16f, 0.24f, 0.24f, 0.12f, 0.05f, C.MUSTARD)
                k.rect(0.45f, 0.50f, 0.10f, 0.20f, C.MUSTARD_D); k.rect(0.30f, 0.70f, 0.40f, 0.14f, WOOD_D) }
            "lost" -> { k.oval(0.14f, 0.20f, 0.44f, 0.30f, 0xFF8A8FA0.toInt()); k.oval(0.38f, 0.12f, 0.46f, 0.36f, 0xFF8A8FA0.toInt()); for (i in 0 until 4) k.line(0.24f + i * 0.16f, 0.56f, 0.20f + i * 0.16f, 0.72f, 0.04f, BLUE_L); Icons.draw(k.p, "feel:sad", k.x + k.s * 0.30f, k.y + k.s * 0.60f, k.s * 0.38f) }
            "clap" -> { k.rr(0.20f, 0.30f, 0.26f, 0.44f, 0.10f, SKIN); k.rr(0.50f, 0.26f, 0.26f, 0.44f, 0.10f, SKIN); for (i in 0 until 3) k.line(0.46f + i * 0.06f, 0.06f, 0.42f + i * 0.08f, 0.18f, 0.03f, C.MUSTARD) }
            "dance" -> { k.oval(0.42f, 0.04f, 0.18f, 0.18f, SKIN); k.poly(PINK, 0.30f, 0.56f, 0.50f, 0.24f, 0.70f, 0.56f); k.line(0.50f, 0.30f, 0.16f, 0.16f, 0.04f, SKIN); k.line(0.50f, 0.30f, 0.84f, 0.16f, 0.04f, SKIN)
                k.line(0.46f, 0.56f, 0.40f, 0.92f, 0.05f, SKIN); k.line(0.54f, 0.56f, 0.82f, 0.74f, 0.05f, SKIN) }
            "music" -> { k.oval(0.14f, 0.62f, 0.24f, 0.20f, C.INK); k.oval(0.56f, 0.54f, 0.24f, 0.20f, C.INK); k.rect(0.34f, 0.18f, 0.05f, 0.56f, C.INK); k.rect(0.76f, 0.10f, 0.05f, 0.56f, C.INK); k.poly(C.INK, 0.34f, 0.18f, 0.81f, 0.10f, 0.81f, 0.22f, 0.34f, 0.30f) }
            "heart" -> heart(k, C.RED)
            "heart_no" -> { heart(k, C.GREY); k.line(0.14f, 0.14f, 0.86f, 0.86f, 0.08f, C.INK) }
            "heart_plus" -> { heart(k, C.RED); k.oval(0.60f, 0.02f, 0.38f, 0.38f, C.MUSTARD); k.text("+", 0.79f, 0.21f, 0.30f, C.INK) }
            "play" -> { draw(k, "foot"); }
            // ---- ballet, up and down ----
            "plie", "tendu", "jete", "releve", "pointes" -> legs(k, name)
            "up" -> Draw.arrow(k.p, k.x + k.s / 2f, k.y + k.s / 2f, k.s * 0.8f, "up", C.GREEN)
            "down" -> Draw.arrow(k.p, k.x + k.s / 2f, k.y + k.s / 2f, k.s * 0.8f, "down", C.RED)
            "now" -> { k.oval(0.08f, 0.08f, 0.84f, 0.84f, C.INK); k.oval(0.13f, 0.13f, 0.74f, 0.74f, C.WHITE); k.line(0.5f, 0.5f, 0.5f, 0.22f, 0.06f, C.RED); k.line(0.5f, 0.5f, 0.70f, 0.5f, 0.06f, C.INK); k.text("!", 0.80f, 0.20f, 0.30f, C.RED) }
            "wait" -> palm(k)
            "stop" -> { k.poly(C.RED, 0.30f, 0.06f, 0.70f, 0.06f, 0.94f, 0.30f, 0.94f, 0.70f, 0.70f, 0.94f, 0.30f, 0.94f, 0.06f, 0.70f, 0.06f, 0.30f); k.text("STOP", 0.5f, 0.52f, 0.24f, C.WHITE) }
            // ---- the sewers ----
            "sewer" -> { k.oval(0.08f, 0.24f, 0.84f, 0.52f, C.INK); k.oval(0.11f, 0.27f, 0.78f, 0.46f, 0xFF6A7080.toInt()); for (i in 0 until 4) k.rect(0.18f, 0.36f + i * 0.08f, 0.64f, 0.03f, C.INK) }
            "tunnel" -> { k.rect(0.04f, 0.10f, 0.92f, 0.82f, 0xFF6B5A4A.toInt()); k.oval(0.20f, 0.24f, 0.60f, 0.80f, C.INK); k.rect(0.20f, 0.64f, 0.60f, 0.28f, C.INK); k.rect(0.30f, 0.80f, 0.40f, 0.12f, 0xFF3F6B5A.toInt()) }
            "torch" -> { k.rr(0.20f, 0.46f, 0.36f, 0.20f, 0.04f, C.INK); k.poly(C.MUSTARD, 0.56f, 0.46f, 0.94f, 0.22f, 0.94f, 0.90f, 0.56f, 0.66f); k.rect(0.06f, 0.50f, 0.16f, 0.12f, C.GREY) }
            "through" -> { draw(k, "tunnel"); Draw.arrow(k.p, k.x + k.s * 0.5f, k.y + k.s * 0.64f, k.s * 0.32f, "up", C.MUSTARD) }
            "along" -> { k.rect(0.04f, 0.60f, 0.92f, 0.20f, BLUE_L); k.rect(0.04f, 0.50f, 0.92f, 0.10f, STONE); Draw.arrow(k.p, k.x + k.s * 0.75f, k.y + k.s * 0.30f, k.s * 0.30f, "right", C.RED); k.rect(0.10f, 0.27f, 0.50f, 0.06f, C.RED) }
            // ---- mice ----
            "rat" -> { k.oval(0.16f, 0.36f, 0.66f, 0.40f, 0xFF7A6A5A.toInt()); k.oval(0.64f, 0.30f, 0.26f, 0.26f, 0xFF7A6A5A.toInt()); k.oval(0.66f, 0.22f, 0.12f, 0.12f, PINK); k.oval(0.82f, 0.40f, 0.04f, 0.04f, C.INK); k.line(0.16f, 0.60f, 0.02f, 0.84f, 0.04f, PINK) }
            "tail" -> { k.line(0.10f, 0.20f, 0.40f, 0.50f, 0.05f, PINK); k.line(0.40f, 0.50f, 0.30f, 0.80f, 0.05f, PINK); k.line(0.30f, 0.80f, 0.70f, 0.86f, 0.05f, PINK); k.oval(0.56f, 0.10f, 0.40f, 0.30f, MOUSE) }
            "ears" -> { k.oval(0.06f, 0.10f, 0.38f, 0.38f, MOUSE); k.oval(0.56f, 0.10f, 0.38f, 0.38f, MOUSE); k.oval(0.13f, 0.17f, 0.24f, 0.24f, PINK); k.oval(0.63f, 0.17f, 0.24f, 0.24f, PINK); k.oval(0.22f, 0.34f, 0.56f, 0.52f, MOUSE) }
            "hole" -> { k.rect(0.04f, 0.10f, 0.92f, 0.84f, 0xFF8F7A5A.toInt()); k.oval(0.28f, 0.40f, 0.44f, 0.40f, C.INK); k.rect(0.28f, 0.60f, 0.44f, 0.34f, C.INK); k.oval(0.40f, 0.62f, 0.06f, 0.06f, C.WHITE); k.oval(0.52f, 0.62f, 0.06f, 0.06f, C.WHITE) }
            "shh" -> { Icons.draw(k.p, "feel:shy", k.x, k.y, k.s); k.rr(0.47f, 0.48f, 0.08f, 0.30f, 0.03f, SKIN); k.ring(0.47f, 0.48f, 0.08f, 0.30f, 0.03f, 0.015f, C.INK) }
            // ---- good and bad ----
            "good" -> thumb(k, true)
            "bad" -> thumb(k, false)
            "kind" -> { Icons.draw(k.p, "feel:smile", k.x, k.y, k.s); heart(k, C.RED, 0.62f, 0.02f, 0.34f) }
            "mean" -> Icons.draw(k.p, "feel:angry", k.x, k.y, k.s)
            "crown" -> { k.poly(C.MUSTARD, 0.08f, 0.80f, 0.08f, 0.30f, 0.30f, 0.54f, 0.50f, 0.20f, 0.70f, 0.54f, 0.92f, 0.30f, 0.92f, 0.80f); k.rect(0.08f, 0.70f, 0.84f, 0.10f, C.MUSTARD_D); k.oval(0.44f, 0.56f, 0.12f, 0.12f, C.RED) }
            "queen" -> { draw(k, "crown"); k.oval(0.12f, 0.20f, 0.10f, 0.10f, 0xFF7C3F8C.toInt()); k.oval(0.78f, 0.20f, 0.10f, 0.10f, 0xFF7C3F8C.toInt()); k.oval(0.45f, 0.12f, 0.10f, 0.10f, PINK) }
            "cheer" -> { k.rect(0.16f, 0.10f, 0.05f, 0.82f, WOOD_D); k.rect(0.21f, 0.12f, 0.22f, 0.40f, BLUE); k.rect(0.43f, 0.12f, 0.22f, 0.40f, C.WHITE); k.rect(0.65f, 0.12f, 0.22f, 0.40f, C.RED) }
            // ---- hungry and full ----
            "hungry" -> { plate(k, false); k.line(0.06f, 0.20f, 0.06f, 0.70f, 0.04f, C.GREY); k.line(0.94f, 0.20f, 0.94f, 0.70f, 0.04f, C.GREY) }
            "very_hungry" -> { plate(k, false); k.text("!!", 0.5f, 0.24f, 0.32f, C.RED) }
            "full" -> { plate(k, true); k.line(0.62f, 0.20f, 0.72f, 0.30f, 0.05f, C.GREEN); k.line(0.72f, 0.30f, 0.90f, 0.10f, 0.05f, C.GREEN) }
            "eat" -> { k.rect(0.30f, 0.30f, 0.05f, 0.62f, C.GREY); for (i in 0 until 3) k.rect(0.24f + i * 0.06f, 0.08f, 0.03f, 0.24f, C.GREY); k.rr(0.58f, 0.08f, 0.14f, 0.40f, 0.06f, C.GREY); k.rect(0.63f, 0.46f, 0.05f, 0.46f, C.GREY) }
            "everyone" -> { person(k, 0.24f, 0.92f, 0.62f, C.RED); person(k, 0.76f, 0.92f, 0.62f, C.GREEN); person(k, 0.50f, 0.96f, 0.72f, BLUE) }
            // ---- the farm ----
            "cow" -> { k.rr(0.14f, 0.34f, 0.62f, 0.34f, 0.08f, C.WHITE); k.oval(0.22f, 0.40f, 0.18f, 0.14f, C.INK); k.oval(0.48f, 0.48f, 0.16f, 0.12f, C.INK)
                k.rr(0.68f, 0.24f, 0.26f, 0.26f, 0.08f, C.WHITE); k.rr(0.70f, 0.40f, 0.24f, 0.12f, 0.05f, PINK); k.oval(0.74f, 0.30f, 0.04f, 0.04f, C.INK)
                for (x in floatArrayOf(0.18f, 0.30f, 0.56f, 0.66f)) k.rect(x, 0.66f, 0.06f, 0.22f, C.INK); k.line(0.14f, 0.40f, 0.06f, 0.60f, 0.03f, C.INK) }
            "milk" -> { k.poly(C.WHITE, 0.28f, 0.30f, 0.40f, 0.12f, 0.60f, 0.12f, 0.72f, 0.30f, 0.72f, 0.92f, 0.28f, 0.92f); k.rect(0.40f, 0.06f, 0.20f, 0.08f, BLUE); k.rect(0.28f, 0.50f, 0.44f, 0.20f, BLUE_L); k.text("lait", 0.5f, 0.60f, 0.13f, C.WHITE) }
            "grass" -> { k.rect(0.04f, 0.80f, 0.92f, 0.12f, WOOD); for (i in 0 until 7) k.poly(GRASS, 0.08f + i * 0.12f, 0.82f, 0.14f + i * 0.12f, 0.30f + (i % 3) * 0.08f, 0.20f + i * 0.12f, 0.82f) }
            "rain" -> { k.oval(0.10f, 0.16f, 0.46f, 0.34f, 0xFF8A8FA0.toInt()); k.oval(0.36f, 0.08f, 0.50f, 0.40f, 0xFF8A8FA0.toInt()); for (i in 0 until 4) k.line(0.22f + i * 0.16f, 0.58f, 0.16f + i * 0.16f, 0.82f, 0.04f, BLUE_L) }
            "no_rain" -> { Icons.draw(k.p, "sun", k.x, k.y, k.s); k.rect(0.04f, 0.84f, 0.92f, 0.10f, 0xFFC9A46A.toInt()); for (i in 0 until 4) k.line(0.12f + i * 0.22f, 0.86f, 0.20f + i * 0.22f, 0.92f, 0.02f, WOOD_D) }
            "first" -> { k.oval(0.10f, 0.10f, 0.80f, 0.80f, C.MUSTARD); k.text("1", 0.5f, 0.52f, 0.52f, C.INK) }
            "then" -> { k.oval(0.10f, 0.10f, 0.80f, 0.80f, C.GREY_L); k.text("2", 0.5f, 0.52f, 0.52f, C.INK) }
            "after" -> { k.rect(0.08f, 0.42f, 0.56f, 0.16f, C.INK2); k.poly(C.INK2, 0.60f, 0.24f, 0.94f, 0.50f, 0.60f, 0.76f) }
            // ---- enough ----
            "help" -> { k.rr(0.06f, 0.40f, 0.42f, 0.22f, 0.10f, SKIN); k.rr(0.52f, 0.40f, 0.42f, 0.22f, 0.10f, 0xFFD9A77A.toInt()); heart(k, C.RED, 0.36f, 0.06f, 0.28f) }
            "enough" -> { jar(k, 1f, false); k.line(0.62f, 0.10f, 0.72f, 0.20f, 0.05f, C.GREEN); k.line(0.72f, 0.20f, 0.92f, 0.0f, 0.05f, C.GREEN) }
            "not_enough" -> jar(k, 0.25f, false)
            "lots" -> { for (i in 0 until 3) for (j in 0 until 3) k.oval(0.10f + i * 0.28f, 0.14f + j * 0.26f, 0.22f, 0.22f, CHEESE_D) }
            "too_much" -> jar(k, 1f, true)
            "need" -> { k.rr(0.06f, 0.46f, 0.50f, 0.20f, 0.10f, SKIN); k.oval(0.62f, 0.30f, 0.32f, 0.32f, CHEESE); k.ring(0.62f, 0.30f, 0.32f, 0.32f, 0.16f, 0.02f, C.INK2) }
            "missing" -> { for (i in 0 until 3) { if (i == 1) k.ring(0.08f + i * 0.30f, 0.34f, 0.24f, 0.30f, 0.04f, 0.02f, C.RED) else k.rr(0.08f + i * 0.30f, 0.34f, 0.24f, 0.30f, 0.04f, CHEESE_D) } }
            // ---- the evening, the theatre ----
            "lets_go" -> { person(k, 0.30f, 0.92f, 0.70f, BLUE); person(k, 0.52f, 0.92f, 0.70f, C.RED); Draw.arrow(k.p, k.x + k.s * 0.82f, k.y + k.s * 0.50f, k.s * 0.30f, "right", C.GREEN) }
            "show", "stage" -> { k.rect(0.04f, 0.06f, 0.92f, 0.10f, C.MUSTARD); k.poly(C.RED, 0.04f, 0.16f, 0.30f, 0.16f, 0.20f, 0.80f, 0.04f, 0.80f); k.poly(C.RED, 0.96f, 0.16f, 0.70f, 0.16f, 0.80f, 0.80f, 0.96f, 0.80f)
                k.rect(0.04f, 0.80f, 0.92f, 0.12f, WOOD); if (name == "show") { k.oval(0.44f, 0.30f, 0.12f, 0.12f, SKIN); k.poly(PINK, 0.38f, 0.66f, 0.50f, 0.42f, 0.62f, 0.66f); k.line(0.52f, 0.62f, 0.68f, 0.40f, 0.04f, SKIN) } }
            "backstage" -> { k.rect(0.04f, 0.06f, 0.92f, 0.86f, 0xFF4A3324.toInt()); k.poly(C.RED, 0.04f, 0.06f, 0.40f, 0.06f, 0.24f, 0.92f, 0.04f, 0.92f); k.rect(0.54f, 0.30f, 0.30f, 0.62f, WOOD); k.oval(0.58f, 0.60f, 0.05f, 0.05f, C.MUSTARD); k.text("*", 0.69f, 0.20f, 0.26f, C.MUSTARD) }
            "cinema" -> { k.rr(0.06f, 0.20f, 0.88f, 0.60f, 0.04f, C.INK); k.rr(0.18f, 0.28f, 0.64f, 0.44f, 0.02f, C.SKY); for (i in 0 until 5) { k.rect(0.08f, 0.24f + i * 0.11f, 0.06f, 0.06f, C.WHITE); k.rect(0.86f, 0.24f + i * 0.11f, 0.06f, 0.06f, C.WHITE) } }
            "restaurant" -> { draw(k, "eat"); k.oval(0.10f, 0.70f, 0.80f, 0.20f, C.GREY_L) }
            "watch" -> { k.oval(0.08f, 0.26f, 0.84f, 0.48f, C.WHITE); k.ring(0.08f, 0.26f, 0.84f, 0.48f, 0.24f, 0.04f, C.INK); k.oval(0.34f, 0.32f, 0.32f, 0.36f, BLUE); k.oval(0.43f, 0.42f, 0.14f, 0.16f, C.INK) }
            "kitchen" -> { k.rr(0.12f, 0.40f, 0.76f, 0.46f, 0.06f, C.GREY); k.rect(0.06f, 0.44f, 0.10f, 0.06f, C.GREY); k.rect(0.84f, 0.44f, 0.10f, 0.06f, C.GREY); k.rr(0.10f, 0.34f, 0.80f, 0.08f, 0.04f, 0xFF6A7080.toInt())
                for (i in 0 until 3) k.line(0.30f + i * 0.18f, 0.28f, 0.34f + i * 0.18f, 0.08f, 0.03f, C.GREY_L) }
            "bar" -> { k.rect(0.04f, 0.62f, 0.92f, 0.30f, WOOD); k.rect(0.04f, 0.58f, 0.92f, 0.06f, WOOD_D)
                k.poly(0xFFD9ECF7.toInt(), 0.16f, 0.20f, 0.36f, 0.20f, 0.30f, 0.56f, 0.22f, 0.56f); k.poly(0xFFF2E14A.toInt(), 0.18f, 0.32f, 0.34f, 0.32f, 0.30f, 0.54f, 0.22f, 0.54f)
                k.rect(0.56f, 0.18f, 0.12f, 0.40f, C.GREEN_D); k.rect(0.59f, 0.08f, 0.06f, 0.12f, C.GREEN_D) }
            "table" -> { k.oval(0.08f, 0.30f, 0.84f, 0.26f, C.RED); k.oval(0.12f, 0.32f, 0.76f, 0.20f, 0xFFE58B86.toInt()); k.rect(0.46f, 0.54f, 0.08f, 0.32f, C.INK); k.rect(0.30f, 0.86f, 0.40f, 0.06f, C.INK); k.oval(0.44f, 0.16f, 0.12f, 0.18f, 0xFFFFF4C8.toInt()) }
            "maitre" -> { person(k, 0.5f, 0.96f, 0.90f, C.INK); k.poly(C.RED, 0.38f, 0.36f, 0.50f, 0.40f, 0.62f, 0.36f, 0.62f, 0.46f, 0.50f, 0.42f, 0.38f, 0.46f) }
            // ---- labels on cheese ----
            "tree" -> { k.oval(0.10f, 0.78f, 0.84f, 0.16f, 0xFF8F8B80.toInt()); k.rect(0.45f, 0.48f, 0.10f, 0.38f, WOOD_D); k.oval(0.18f, 0.06f, 0.64f, 0.52f, GRASS); k.oval(0.30f, 0.12f, 0.24f, 0.18f, 0xFF7CC26E.toInt()) }
            "label_aop" -> label(k, C.RED, "AOP")
            "label_igp" -> label(k, BLUE, "IGP")
            "label_none" -> { k.oval(0.12f, 0.12f, 0.76f, 0.76f, C.GREY_L); k.ring(0.12f, 0.12f, 0.76f, 0.76f, 0.38f, 0.04f, C.GREY) }
            else -> return Icons3.draw(k, name)
        }
        return true
    }

    /** An open hand held up: wait, or no thank you. */
    private fun palm(k: Icons.K) {
        k.rr(0.28f, 0.40f, 0.44f, 0.46f, 0.12f, SKIN)
        for (i in 0 until 4) k.rr(0.28f + i * 0.115f, 0.10f + (if (i == 0 || i == 3) 0.08f else 0f), 0.095f, 0.36f, 0.05f, SKIN)
        k.rr(0.12f, 0.46f, 0.20f, 0.10f, 0.05f, SKIN)
        k.rect(0.30f, 0.84f, 0.40f, 0.10f, 0xFF3F6DB5.toInt())
    }

    private fun label(k: Icons.K, c: Int, text: String) {
        k.oval(0.08f, 0.08f, 0.84f, 0.84f, C.MUSTARD)
        k.oval(0.14f, 0.14f, 0.72f, 0.72f, c)
        k.text(text, 0.5f, 0.52f, 0.22f, C.WHITE)
    }

    private fun heart(k: Icons.K, c: Int, x: Float = 0.08f, y: Float = 0.12f, s: Float = 0.84f) {
        k.oval(x, y, s * 0.54f, s * 0.54f, c)
        k.oval(x + s * 0.46f, y, s * 0.54f, s * 0.54f, c)
        k.poly(c, x + s * 0.03f, y + s * 0.38f, x + s * 0.97f, y + s * 0.38f, x + s * 0.5f, y + s * 0.95f)
    }

    private fun thumb(k: Icons.K, up: Boolean) {
        // a fist with the thumb along its left side, pointing up (or down): the curled fingers show as creases
        val crease = 0xFFD9A77A.toInt()
        if (up) {
            k.rr(0.30f, 0.40f, 0.48f, 0.46f, 0.10f, SKIN)
            for (i in 0 until 3) k.rect(0.56f, 0.52f + i * 0.10f, 0.20f, 0.025f, crease)
            k.rr(0.26f, 0.10f, 0.16f, 0.44f, 0.08f, SKIN)
            k.rect(0.40f, 0.42f, 0.10f, 0.025f, crease)
            k.rect(0.14f, 0.44f, 0.14f, 0.44f, C.GREEN)
        } else {
            k.rr(0.30f, 0.14f, 0.48f, 0.46f, 0.10f, SKIN)
            for (i in 0 until 3) k.rect(0.56f, 0.24f + i * 0.10f, 0.20f, 0.025f, crease)
            k.rr(0.26f, 0.46f, 0.16f, 0.44f, 0.08f, SKIN)
            k.rect(0.40f, 0.56f, 0.10f, 0.025f, crease)
            k.rect(0.14f, 0.12f, 0.14f, 0.44f, C.RED)
        }
    }


}
