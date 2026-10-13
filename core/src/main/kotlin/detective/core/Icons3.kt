package detective.core

/**
 * Pictures for the words of chapter 3: the restaurant, films and driving,
 * jewellery and presents, the metro, the bookshop, the fashion show, strong
 * and weak, exclamations, dressing up. Drawn in code on a unit square, like
 * MoreIcons and Icons2. Names with a value after a colon:
 *   dress:d9534f:long   a dress of that colour, long or short (colour "bw" is black and white)
 *   shoes:22223b        a pair of shoes of that colour
 *   bubble:scared       a speech bubble with a face (angry, scared, curious, smile) or a picture (gift, stars, oldbook)
 */
object Icons3 {
    private val SKIN = 0xFFF2C9A0.toInt()
    private val SKIN_D = 0xFFD9A77A.toInt()
    private val WOOD = 0xFFB07A3F.toInt()
    private val WOOD_D = 0xFF7A5226.toInt()
    private val GOLD = 0xFFF2C14E.toInt()
    private val GOLD_D = 0xFFC98A14.toInt()
    private val SILVER = 0xFFC9CED8.toInt()
    private val SILVER_D = 0xFF8A8FA0.toInt()
    private val PEARL = 0xFFF7F1E3.toInt()
    private val DIAMOND = 0xFFCFE8F7.toInt()
    private val BLUE = 0xFF3F6DB5.toInt()
    private val PURPLE = 0xFF7C3F8C.toInt()
    private val PINK = 0xFFF59BBB.toInt()
    private val OCHRE = 0xFFC9910D.toInt()
    private val NAVY = 0xFF1D2B53.toInt()
    private val PLATE = 0xFFF4F4F8.toInt()
    private val SOUP = 0xFFE59A3A.toInt()
    private val HEDGE = 0xFF2F6B4A.toInt()
    private val BOOKS = intArrayOf(0xFFD9534F.toInt(), 0xFF3F6DB5.toInt(), 0xFF3A9E6A.toInt(), 0xFFF2B233.toInt(), 0xFF7C3F8C.toInt())

    private val NAMES = hashSetOf(
        // the restaurant
        "people", "lunch", "dinner", "name_tag", "noted", "starter", "main", "dessert", "bon_appetit", "sweet", "salty",
        "chicken", "veg", "soup", "with", "goes_with", "sparkling", "juice_apple", "cloche", "guide", "corner",
        "car_green", "car_blue", "van_green", "van_red", "bike_green",
        // films and driving
        "film", "actress", "actor", "action", "comedy", "drama", "cartoon", "scary", "hero_cape", "favourite",
        "slow_down", "roundabout", "quay", "behind_us", "followed",
        // jewellery
        "jewel", "ring", "wristwatch", "necklace_pearl", "necklace_gold", "bracelet", "earrings", "diamond", "pearl", "gold", "silver",
        "expensive", "gift", "give_gift", "birthday", "wedding", "anniversary", "christmas", "mothers_day", "camera", "bell", "panther",
        // the metro
        "card", "navigo", "mobile", "app", "buy", "recharge", "validate", "ride_one", "gate_metro", "phone_reader",
        "metro_line", "direction", "metro", "change", "get_off", "lost_way", "give_back", "stop_count",
        // the bookshop
        "bookshop", "library", "author", "read", "english", "french", "sleep", "rule", "must", "book_a_day", "write",
        "my_life", "autobio", "cat", "typewriter",
        // backstage
        "fashion_show", "runway", "dressing_room", "model", "makeup", "hairdo", "get_dressed", "enter", "safe", "keypad",
        "locked", "unlocked", "before", "ready", "sparkle", "badge", "desk",
        // strong and weak
        "safe_shield", "well_done", "strong", "weak", "weak_spot", "nobody", "only", "tracker", "hedge", "mansion",
        // dressing up
        "chic", "elegant", "too_plain", "evening_dress", "well_dressed", "call_taxi", "classic", "modern", "colourful",
        "black_white", "plain", "couture_house", "style", "golden_keys", "full_skirt", "trouser_suit"
    )
    private val PREFIXES = arrayOf("dress:", "shoes:", "bubble:")

    fun samples(): List<String> {
        val out = ArrayList<String>(NAMES.sorted())
        out.addAll(listOf("dress:d9534f:long", "dress:3f6db5:short", "dress:bw:long", "shoes:22223b", "shoes:d9534f",
            "bubble:scared", "bubble:curious", "bubble:angry", "bubble:smile", "bubble:gift", "bubble:stars", "bubble:oldbook"))
        return out
    }

    fun knows(name: String): Boolean {
        if (NAMES.contains(name)) return true
        for (pf in PREFIXES) if (name.startsWith(pf) && name.length > pf.length) return true
        return false
    }

    private fun color(hex: String): Int = try { Art.parseColor(hex) } catch (e: Exception) { C.GREY }

    private fun shade(c: Int, f: Float = 0.75f): Int {
        val r = (((c shr 16) and 255) * f).toInt()
        val g = (((c shr 8) and 255) * f).toInt()
        val b = ((c and 255) * f).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    // ---------- little helpers ----------

    private fun sub(k: Icons.K, name: String, a: Float, b: Float, s: Float) = Icons.draw(k.p, name, k.x + a * k.s, k.y + b * k.s, s * k.s)

    private fun person(k: Icons.K, cx: Float, footY: Float, h: Float, c: Int, hair: Int = WOOD_D) {
        val head = h * 0.26f
        k.oval(cx - head / 2f, footY - h, head, head, SKIN)
        k.rr(cx - head / 2f, footY - h - head * 0.05f, head, head * 0.38f, head * 0.2f, hair)
        k.rr(cx - h * 0.17f, footY - h * 0.72f, h * 0.34f, h * 0.44f, 0.03f, c)
        k.rect(cx - h * 0.13f, footY - h * 0.30f, h * 0.10f, h * 0.30f, C.INK)
        k.rect(cx + h * 0.03f, footY - h * 0.30f, h * 0.10f, h * 0.30f, C.INK)
    }

    private fun woman(k: Icons.K, cx: Float, footY: Float, h: Float, dress: Int, hair: Int = WOOD_D) {
        val head = h * 0.24f
        k.oval(cx - head * 0.65f, footY - h - head * 0.08f, head * 1.3f, head * 1.25f, hair)
        k.oval(cx - head / 2f, footY - h, head, head, SKIN)
        k.poly(dress, cx - h * 0.12f, footY - h * 0.74f, cx + h * 0.12f, footY - h * 0.74f, cx + h * 0.26f, footY - h * 0.12f, cx - h * 0.26f, footY - h * 0.12f)
        k.rect(cx - h * 0.10f, footY - h * 0.12f, h * 0.06f, h * 0.12f, SKIN_D)
        k.rect(cx + h * 0.04f, footY - h * 0.12f, h * 0.06f, h * 0.12f, SKIN_D)
    }

    private fun plate(k: Icons.K) {
        k.oval(0.06f, 0.50f, 0.88f, 0.36f, C.GREY_L)
        k.oval(0.16f, 0.54f, 0.68f, 0.26f, PLATE)
    }

    private fun bottle(k: Icons.K, x: Float, body: Int, label: Int) {
        k.rr(x + 0.08f, 0.06f, 0.10f, 0.20f, 0.02f, shade(body, 0.8f))
        k.rr(x, 0.24f, 0.26f, 0.66f, 0.06f, body)
        k.rect(x, 0.48f, 0.26f, 0.18f, label)
    }

    private fun glass(k: Icons.K, x: Float, y: Float, w: Float, h: Float, drink: Int) {
        k.rr(x, y, w, h, 0.03f, C.GREY_L)
        k.rect(x + w * 0.1f, y + h * 0.35f, w * 0.8f, h * 0.6f, drink)
        k.rect(x + w * 0.1f, y + h * 0.35f, w * 0.8f, h * 0.06f, shade(drink, 1.0f))
    }

    private fun card(k: Icons.K, x: Float, y: Float, w: Float, h: Float, c: Int, text: String) {
        k.rr(x, y, w, h, 0.05f, c)
        k.rect(x + w * 0.10f, y + h * 0.20f, w * 0.20f, h * 0.22f, GOLD)
        if (text.isNotEmpty()) k.text(text, x + w * 0.55f, y + h * 0.72f, h * 0.28f, C.WHITE)
    }

    private fun phone(k: Icons.K, x: Float, y: Float, w: Float, h: Float) {
        k.rr(x, y, w, h, 0.05f, C.INK)
        k.rr(x + w * 0.10f, y + h * 0.08f, w * 0.80f, h * 0.80f, 0.02f, C.SKY)
        k.oval(x + w * 0.42f, y + h * 0.90f, w * 0.16f, h * 0.06f, C.GREY_L)
    }

    private fun bookC(k: Icons.K, x: Float, y: Float, w: Float, h: Float, c: Int) {
        k.rr(x, y, w, h, 0.02f, shade(c, 0.7f))
        k.rr(x + w * 0.08f, y, w * 0.92f, h, 0.02f, c)
        k.rect(x + w * 0.25f, y + h * 0.18f, w * 0.55f, h * 0.10f, C.WHITE)
    }

    private fun shelfRow(k: Icons.K, x: Float, y: Float, w: Float, h: Float) {
        var bx = x
        var i = 0
        while (bx < x + w - 0.04f) {
            val bw = 0.05f + (i % 3) * 0.015f
            val bh = h * (0.75f + (i % 2) * 0.2f)
            k.rect(bx, y + h - bh, bw, bh, BOOKS[i % BOOKS.size])
            bx += bw + 0.012f
            i++
        }
        k.rect(x - 0.02f, y + h, w + 0.04f, 0.04f, WOOD_D)
    }

    private fun gem(k: Icons.K, cx: Float, cy: Float, s: Float, c: Int) {
        k.poly(c, cx - s, cy - s * 0.3f, cx - s * 0.5f, cy - s * 0.8f, cx + s * 0.5f, cy - s * 0.8f, cx + s, cy - s * 0.3f, cx, cy + s)
        k.poly(C.WHITE, cx - s * 0.5f, cy - s * 0.3f, cx - s * 0.2f, cy - s * 0.7f, cx + s * 0.1f, cy - s * 0.3f)
    }

    private fun ring(k: Icons.K, cx: Float, cy: Float, r: Float, band: Int) {
        k.ring(cx - r, cy - r, r * 2f, r * 2f, r, r * 0.30f, band)
    }

    private fun necklace(k: Icons.K, bead: Int, pendant: Int?) {
        for (i in 0..10) {
            val a = Math.toRadians((20 + i * 14).toDouble())
            val x = 0.5f + 0.38f * Math.cos(a).toFloat()
            val y = 0.24f + 0.40f * Math.sin(a).toFloat()
            k.oval(x - 0.055f, y - 0.055f, 0.11f, 0.11f, if (bead == PEARL) 0xFFB9AE96.toInt() else GOLD_D)
            k.oval(x - 0.045f, y - 0.045f, 0.09f, 0.09f, bead)
            k.oval(x - 0.02f, y - 0.03f, 0.03f, 0.03f, C.WHITE)
        }
        if (pendant != null) gem(k, 0.5f, 0.72f, 0.09f, pendant)
    }

    private fun calendar(k: Icons.K, x: Float, y: Float, w: Float, top: Int) {
        k.rr(x, y, w, w, w * 0.10f, C.INK2)
        k.rr(x + w * 0.05f, y + w * 0.05f, w * 0.90f, w * 0.90f, w * 0.06f, C.WHITE)
        k.rr(x + w * 0.05f, y + w * 0.05f, w * 0.90f, w * 0.24f, w * 0.06f, top)
    }

    private fun lock(k: Icons.K, open: Boolean) {
        if (open) k.arc(0.62f, 0.34f, 0.18f, 180f, 360f, 0.07f, SILVER_D) else k.arc(0.5f, 0.38f, 0.18f, 180f, 360f, 0.07f, SILVER_D)
        if (!open) { k.rect(0.29f, 0.38f, 0.07f, 0.10f, SILVER_D); k.rect(0.64f, 0.38f, 0.07f, 0.10f, SILVER_D) }
        else k.rect(0.73f, 0.34f, 0.07f, 0.14f, SILVER_D)
        k.rr(0.22f, 0.46f, 0.56f, 0.44f, 0.06f, GOLD)
        k.oval(0.45f, 0.58f, 0.10f, 0.10f, C.INK)
        k.rect(0.48f, 0.64f, 0.04f, 0.12f, C.INK)
    }

    private fun wall(k: Icons.K, x: Float, y: Float, w: Float, h: Float, cracked: Boolean) {
        val c = if (cracked) 0xFFC9BFA8.toInt() else 0xFFB5523F.toInt()
        k.rect(x, y, w, h, c)
        var row = 0
        var yy = y
        while (yy < y + h - 0.01f) {
            k.rect(x, yy, w, 0.02f, 0xFFE9E1CF.toInt())
            var xx = x + if (row % 2 == 0) 0f else 0.08f
            while (xx < x + w) { k.rect(xx, yy, 0.02f, 0.12f, 0xFFE9E1CF.toInt()); xx += 0.16f }
            yy += 0.12f
            row++
        }
        if (cracked) {
            k.line(x + w * 0.5f, y, x + w * 0.38f, y + h * 0.35f, 0.035f, C.INK)
            k.line(x + w * 0.38f, y + h * 0.35f, x + w * 0.58f, y + h * 0.6f, 0.035f, C.INK)
            k.line(x + w * 0.58f, y + h * 0.6f, x + w * 0.45f, y + h, 0.035f, C.INK)
        }
    }

    /** A dress on a hanger: [c] is the colour, or -1 for black and white. */
    private fun dress(k: Icons.K, c: Int, long: Boolean, full: Boolean = false) {
        val bw = c == -1
        val body = if (bw) C.WHITE else c
        val d = if (bw) C.INK else shade(c)
        k.line(0.5f, 0.04f, 0.5f, 0.12f, 0.03f, SILVER_D)
        k.line(0.32f, 0.16f, 0.68f, 0.16f, 0.03f, SILVER_D)
        val bottom = if (long) 0.94f else 0.68f
        val spread = if (full) 0.44f else if (long) 0.30f else 0.26f
        k.poly(d, 0.36f, 0.14f, 0.64f, 0.14f, 0.62f, 0.42f, 0.5f + spread + 0.02f, bottom + 0.02f, 0.5f - spread - 0.02f, bottom + 0.02f, 0.38f, 0.42f)
        k.poly(body, 0.38f, 0.16f, 0.62f, 0.16f, 0.60f, 0.42f, 0.5f + spread, bottom, 0.5f - spread, bottom, 0.40f, 0.42f)
        k.rect(0.38f, 0.40f, 0.24f, 0.05f, if (bw) C.INK else shade(c, 0.6f))
        if (bw) { k.rect(0.40f, 0.16f, 0.06f, 0.24f, C.INK); k.rect(0.54f, 0.16f, 0.06f, 0.24f, C.INK)
            k.poly(C.INK, 0.42f, 0.45f, 0.5f, 0.45f, 0.5f - spread * 0.4f, bottom, 0.5f - spread * 0.85f, bottom) }
    }

    private fun shoes(k: Icons.K, c: Int) {
        val d = if (c == C.WHITE) C.GREY else shade(c)
        for (i in 0 until 2) {
            val x = 0.08f + i * 0.44f
            val y = 0.40f + i * 0.08f
            k.poly(d, x, y + 0.34f, x + 0.04f, y + 0.12f, x + 0.14f, y + 0.08f, x + 0.40f, y + 0.26f, x + 0.40f, y + 0.36f)
            k.poly(c, x + 0.02f, y + 0.32f, x + 0.06f, y + 0.14f, x + 0.14f, y + 0.11f, x + 0.37f, y + 0.27f, x + 0.37f, y + 0.33f)
            k.rect(x + 0.02f, y + 0.30f, 0.05f, 0.10f, d)
        }
    }

    private fun bubble(k: Icons.K, arg: String) {
        k.rr(0.04f, 0.06f, 0.92f, 0.70f, 0.18f, C.INK)
        k.rr(0.07f, 0.09f, 0.86f, 0.64f, 0.16f, C.WHITE)
        k.poly(C.INK, 0.22f, 0.72f, 0.40f, 0.72f, 0.16f, 0.96f)
        k.poly(C.WHITE, 0.25f, 0.70f, 0.37f, 0.70f, 0.20f, 0.88f)
        when (arg) {
            "gift" -> sub(k, "gift", 0.26f, 0.14f, 0.50f)
            "stars" -> { for ((x, y) in listOf(0.30f to 0.22f, 0.58f to 0.18f, 0.44f to 0.44f)) Icons.draw(k.p, "star", k.x + x * k.s, k.y + y * k.s, 0.22f * k.s, GOLD_D) }
            "oldbook" -> { bookC(k, 0.28f, 0.20f, 0.30f, 0.40f, 0xFF7A1F2B.toInt()); k.text("!", 0.74f, 0.42f, 0.36f, C.RED) }
            else -> {
                val img = Faces.get("bubble", FaceSpec(hair = 0xFF8A5A2B.toInt(), hairStyle = "short", clothes = 0xFF8FA0B8.toInt()), arg)
                val h = 0.56f * k.s
                val w = h * Faces.W / Faces.H
                k.p.image(img, k.x + 0.5f * k.s - w / 2f, k.y + 0.13f * k.s, w, h, false, 1f)
                k.text("!", 0.82f, 0.36f, 0.30f, C.RED)
            }
        }
    }

    private fun car(k: Icons.K, body: Int) {
        k.poly(C.INK, 0.06f, 0.70f, 0.06f, 0.50f, 0.26f, 0.48f, 0.38f, 0.30f, 0.66f, 0.30f, 0.80f, 0.48f, 0.94f, 0.52f, 0.94f, 0.70f)
        k.poly(body, 0.09f, 0.67f, 0.09f, 0.52f, 0.28f, 0.50f, 0.40f, 0.33f, 0.64f, 0.33f, 0.77f, 0.50f, 0.91f, 0.54f, 0.91f, 0.67f)
        k.poly(C.SKY, 0.42f, 0.36f, 0.52f, 0.36f, 0.52f, 0.48f, 0.33f, 0.48f)
        k.poly(C.SKY, 0.56f, 0.36f, 0.63f, 0.36f, 0.72f, 0.48f, 0.56f, 0.48f)
        wheel(k, 0.26f, 0.72f, 0.09f)
        wheel(k, 0.74f, 0.72f, 0.09f)
    }

    private fun van(k: Icons.K, body: Int) {
        k.rr(0.04f, 0.24f, 0.70f, 0.48f, 0.04f, C.INK)
        k.rr(0.06f, 0.26f, 0.66f, 0.44f, 0.03f, body)
        k.poly(C.INK, 0.72f, 0.36f, 0.86f, 0.36f, 0.96f, 0.52f, 0.96f, 0.72f, 0.72f, 0.72f)
        k.poly(body, 0.74f, 0.38f, 0.85f, 0.38f, 0.94f, 0.53f, 0.94f, 0.70f, 0.74f, 0.70f)
        k.poly(C.SKY, 0.76f, 0.41f, 0.84f, 0.41f, 0.90f, 0.52f, 0.76f, 0.52f)
        wheel(k, 0.24f, 0.74f, 0.09f)
        wheel(k, 0.78f, 0.74f, 0.09f)
    }

    private fun wheel(k: Icons.K, cx: Float, cy: Float, r: Float) {
        k.oval(cx - r, cy - r, r * 2f, r * 2f, C.INK)
        k.oval(cx - r * 0.5f, cy - r * 0.5f, r, r, 0xFF8A8FA0.toInt())
    }

    private fun bike(k: Icons.K, c: Int) {
        k.ring(0.06f, 0.48f, 0.34f, 0.34f, 0.17f, 0.05f, C.INK)
        k.ring(0.60f, 0.48f, 0.34f, 0.34f, 0.17f, 0.05f, C.INK)
        k.line(0.23f, 0.65f, 0.45f, 0.40f, 0.05f, c); k.line(0.45f, 0.40f, 0.77f, 0.65f, 0.05f, c)
        k.line(0.23f, 0.65f, 0.52f, 0.65f, 0.05f, c); k.line(0.52f, 0.65f, 0.66f, 0.38f, 0.05f, c)
        k.line(0.45f, 0.40f, 0.42f, 0.30f, 0.04f, C.INK); k.rect(0.36f, 0.27f, 0.14f, 0.04f, C.INK)
        k.line(0.66f, 0.38f, 0.70f, 0.28f, 0.04f, C.INK); k.rect(0.66f, 0.26f, 0.12f, 0.04f, C.INK)
    }

    private fun clockFace(k: Icons.K, x: Float, y: Float, s: Float) {
        k.oval(x, y, s, s, C.INK)
        k.oval(x + s * 0.08f, y + s * 0.08f, s * 0.84f, s * 0.84f, C.WHITE)
        k.line(x + s * 0.5f, y + s * 0.5f, x + s * 0.5f, y + s * 0.22f, s * 0.06f, C.INK)
        k.line(x + s * 0.5f, y + s * 0.5f, x + s * 0.70f, y + s * 0.58f, s * 0.06f, C.INK)
    }

    private fun metroSign(k: Icons.K, x: Float, y: Float, s: Float) {
        k.oval(x, y, s, s, GOLD)
        k.oval(x + s * 0.08f, y + s * 0.08f, s * 0.84f, s * 0.84f, NAVY)
        k.text("M", x + s * 0.5f, y + s * 0.53f, s * 0.6f, GOLD)
    }

    private fun suit(k: Icons.K, c: Int) {
        k.line(0.5f, 0.04f, 0.5f, 0.12f, 0.03f, SILVER_D)
        k.line(0.30f, 0.16f, 0.70f, 0.16f, 0.03f, SILVER_D)
        k.poly(c, 0.30f, 0.16f, 0.70f, 0.16f, 0.74f, 0.54f, 0.26f, 0.54f)
        k.poly(C.WHITE, 0.44f, 0.16f, 0.56f, 0.16f, 0.5f, 0.36f)
        k.rect(0.30f, 0.54f, 0.18f, 0.40f, shade(c, 0.85f))
        k.rect(0.52f, 0.54f, 0.18f, 0.40f, shade(c, 0.85f))
    }

    private fun sparkles(k: Icons.K, pts: List<Pair<Float, Float>>, s: Float) {
        for ((x, y) in pts) {
            k.poly(GOLD, x, y - s, x + s * 0.25f, y - s * 0.25f, x + s, y, x + s * 0.25f, y + s * 0.25f, x, y + s, x - s * 0.25f, y + s * 0.25f, x - s, y, x - s * 0.25f, y - s * 0.25f)
        }
    }

    // ---------- drawing ----------

    internal fun draw(k: Icons.K, name: String): Boolean {
        val colon = name.indexOf(':')
        if (colon > 0) {
            val kind = name.substring(0, colon)
            val arg = name.substring(colon + 1)
            when (kind) {
                "dress" -> {
                    val parts = arg.split(':')
                    val c = if (parts[0] == "bw") -1 else color(parts[0])
                    dress(k, c, parts.size < 2 || parts[1] != "short")
                }
                "shoes" -> shoes(k, color(arg))
                "bubble" -> bubble(k, arg)
                else -> return false
            }
            return true
        }
        when (name) {
            // ---- the restaurant ----
            "people" -> { person(k, 0.30f, 0.92f, 0.62f, BLUE); person(k, 0.70f, 0.92f, 0.62f, C.RED); k.text("?", 0.5f, 0.18f, 0.30f, C.RED) }
            "lunch" -> { plate(k); k.oval(0.36f, 0.56f, 0.28f, 0.14f, 0xFFB5523F.toInt()); k.oval(0.62f, 0.04f, 0.32f, 0.32f, C.MUSTARD) }
            "dinner" -> { plate(k); k.oval(0.36f, 0.56f, 0.28f, 0.14f, 0xFFB5523F.toInt()); sub(k, "moon", 0.60f, 0.0f, 0.36f) }
            "name_tag" -> { k.rr(0.10f, 0.24f, 0.80f, 0.52f, 0.06f, C.RED); k.rr(0.14f, 0.40f, 0.72f, 0.30f, 0.03f, C.WHITE); k.line(0.22f, 0.56f, 0.70f, 0.56f, 0.03f, C.INK2); k.rect(0.10f, 0.24f, 0.80f, 0.12f, C.RED_D) }
            "noted" -> { k.rr(0.16f, 0.08f, 0.56f, 0.80f, 0.04f, C.WHITE); k.ring(0.16f, 0.08f, 0.56f, 0.80f, 0.04f, 0.02f, C.GREY)
                for (i in 0 until 4) k.line(0.24f, 0.24f + i * 0.14f, 0.62f, 0.24f + i * 0.14f, 0.025f, C.GREY_L)
                Icons.draw(k.p, "check", k.x + 0.48f * k.s, k.y + 0.50f * k.s, 0.44f * k.s, C.GREEN) }
            "starter" -> { plate(k); k.oval(0.30f, 0.52f, 0.20f, 0.12f, 0xFF5E9E52.toInt()); k.oval(0.46f, 0.56f, 0.18f, 0.10f, 0xFF7FBF6A.toInt()); k.oval(0.56f, 0.52f, 0.10f, 0.08f, C.RED) }
            "main" -> { plate(k); k.poly(0xFFF2D7C0.toInt(), 0.24f, 0.62f, 0.40f, 0.52f, 0.62f, 0.56f, 0.64f, 0.66f, 0.40f, 0.70f); k.oval(0.58f, 0.56f, 0.10f, 0.08f, 0xFF5E9E52.toInt()); k.oval(0.66f, 0.62f, 0.08f, 0.07f, 0xFF5E9E52.toInt()) }
            "dessert" -> { plate(k); k.poly(0xFF8A5A2B.toInt(), 0.28f, 0.66f, 0.70f, 0.66f, 0.70f, 0.44f, 0.28f, 0.58f); k.poly(0xFFFFE6F0.toInt(), 0.28f, 0.58f, 0.70f, 0.44f, 0.70f, 0.40f, 0.28f, 0.54f); k.oval(0.58f, 0.32f, 0.10f, 0.10f, C.RED) }
            "bon_appetit" -> { plate(k); k.line(0.12f, 0.20f, 0.12f, 0.80f, 0.05f, SILVER_D); for (i in 0 until 3) k.line(0.08f + i * 0.04f, 0.10f, 0.08f + i * 0.04f, 0.28f, 0.02f, SILVER_D)
                k.line(0.88f, 0.12f, 0.88f, 0.80f, 0.05f, SILVER_D); k.rr(0.84f, 0.10f, 0.08f, 0.30f, 0.03f, SILVER) }
            "sweet" -> { k.rr(0.20f, 0.30f, 0.30f, 0.30f, 0.03f, C.WHITE); k.ring(0.20f, 0.30f, 0.30f, 0.30f, 0.03f, 0.02f, C.GREY); k.rr(0.50f, 0.44f, 0.30f, 0.30f, 0.03f, C.WHITE); k.ring(0.50f, 0.44f, 0.30f, 0.30f, 0.03f, 0.02f, C.GREY)
                k.oval(0.62f, 0.08f, 0.28f, 0.28f, PINK) }
            "salty" -> { k.rr(0.30f, 0.30f, 0.40f, 0.60f, 0.10f, C.WHITE); k.ring(0.30f, 0.30f, 0.40f, 0.60f, 0.10f, 0.03f, C.GREY); k.rr(0.30f, 0.16f, 0.40f, 0.18f, 0.06f, SILVER)
                for ((x, y) in listOf(0.40f to 0.22f, 0.5f to 0.20f, 0.60f to 0.22f)) k.oval(x - 0.02f, y - 0.02f, 0.04f, 0.04f, C.INK); for ((x, y) in listOf(0.2f to 0.08f, 0.34f to 0.04f, 0.6f to 0.06f)) k.rect(x, y, 0.03f, 0.03f, C.GREY) }
            "chicken" -> { plate(k); k.oval(0.26f, 0.42f, 0.42f, 0.26f, 0xFFD9944A.toInt()); k.rect(0.62f, 0.50f, 0.16f, 0.06f, 0xFFF4E9D8.toInt()); k.oval(0.74f, 0.46f, 0.10f, 0.14f, 0xFFF4E9D8.toInt()) }
            "veg" -> { k.poly(0xFFF08A3C.toInt(), 0.20f, 0.30f, 0.36f, 0.30f, 0.28f, 0.86f); k.poly(0xFF5E9E52.toInt(), 0.24f, 0.30f, 0.20f, 0.12f, 0.28f, 0.28f, 0.34f, 0.10f, 0.32f, 0.30f)
                k.oval(0.44f, 0.44f, 0.24f, 0.24f, 0xFF4F8F46.toInt()); k.oval(0.56f, 0.38f, 0.26f, 0.26f, 0xFF5E9E52.toInt()); k.rect(0.58f, 0.60f, 0.08f, 0.24f, 0xFF7FBF6A.toInt())
                k.oval(0.70f, 0.66f, 0.22f, 0.22f, C.RED) }
            "soup" -> { k.oval(0.08f, 0.44f, 0.84f, 0.20f, C.GREY_L); k.poly(C.WHITE, 0.10f, 0.52f, 0.90f, 0.52f, 0.74f, 0.86f, 0.26f, 0.86f); k.oval(0.14f, 0.46f, 0.72f, 0.14f, SOUP)
                for (i in 0 until 3) k.arc(0.34f + i * 0.16f, 0.30f, 0.05f, 180f, 360f, 0.025f, C.GREY) }
            "with" -> { k.oval(0.06f, 0.30f, 0.34f, 0.34f, C.RED); k.oval(0.60f, 0.30f, 0.34f, 0.34f, BLUE); k.rect(0.44f, 0.44f, 0.12f, 0.06f, C.INK); k.rect(0.47f, 0.41f, 0.06f, 0.12f, C.INK) }
            "goes_with" -> { sub(k, "cheese", 0.02f, 0.20f, 0.44f); glass(k, 0.64f, 0.24f, 0.24f, 0.50f, 0xFFE8C55A.toInt()); k.line(0.44f, 0.46f, 0.60f, 0.46f, 0.04f, C.GREEN); sub(k, "heart", 0.42f, 0.10f, 0.20f) }
            "sparkling" -> { bottle(k, 0.14f, 0xFF7FB8D9.toInt(), C.WHITE); glass(k, 0.54f, 0.36f, 0.30f, 0.54f, 0xFFD3ECFB.toInt()); for ((x, y) in listOf(0.62f to 0.66f, 0.70f to 0.58f, 0.76f to 0.72f, 0.66f to 0.78f)) k.oval(x, y, 0.04f, 0.04f, C.WHITE) }
            "juice_apple" -> { glass(k, 0.20f, 0.26f, 0.34f, 0.64f, 0xFFE8C55A.toInt()); k.oval(0.56f, 0.40f, 0.34f, 0.34f, 0xFF7FBF4A.toInt()); k.rect(0.72f, 0.32f, 0.03f, 0.10f, WOOD_D); k.oval(0.74f, 0.30f, 0.10f, 0.06f, 0xFF5E9E52.toInt()) }
            "cloche" -> { k.oval(0.06f, 0.70f, 0.88f, 0.16f, SILVER_D); k.poly(SILVER, 0.12f, 0.74f, 0.20f, 0.44f, 0.36f, 0.30f, 0.64f, 0.30f, 0.80f, 0.44f, 0.88f, 0.74f); k.oval(0.44f, 0.22f, 0.12f, 0.10f, SILVER_D); k.oval(0.28f, 0.40f, 0.12f, 0.08f, C.WHITE) }
            "guide" -> { k.rr(0.18f, 0.08f, 0.64f, 0.84f, 0.04f, 0xFFB5302F.toInt()); k.rect(0.18f, 0.08f, 0.08f, 0.84f, 0xFF8A2020.toInt()); for (i in 0 until 3) Icons.draw(k.p, "star", k.x + (0.30f + i * 0.15f) * k.s, k.y + 0.30f * k.s, 0.14f * k.s, C.WHITE); k.rect(0.32f, 0.56f, 0.40f, 0.06f, C.WHITE) }
            "corner" -> { k.rect(0.0f, 0.0f, 0.46f, 0.46f, 0xFFEADFC4.toInt()); k.rect(0.0f, 0.46f, 1f, 0.54f, 0xFF5E6472.toInt()); k.rect(0.46f, 0.0f, 0.54f, 1f, 0xFF5E6472.toInt()); k.rect(0.0f, 0.46f, 1f, 0.0f, C.INK)
                k.rr(0.06f, 0.10f, 0.36f, 0.16f, 0.02f, 0xFF2F548F.toInt()); k.rect(0.10f, 0.16f, 0.28f, 0.03f, C.WHITE); k.oval(0.38f, 0.38f, 0.16f, 0.16f, C.RED) }
            "car_green" -> car(k, C.GREEN)
            "car_blue" -> car(k, BLUE)
            "van_green" -> van(k, C.GREEN)
            "van_red" -> van(k, C.RED)
            "bike_green" -> bike(k, C.GREEN)

            // ---- films and driving ----
            "film" -> { k.rr(0.10f, 0.20f, 0.80f, 0.60f, 0.04f, C.INK); for (i in 0 until 5) { k.rect(0.16f + i * 0.15f, 0.24f, 0.08f, 0.07f, C.WHITE); k.rect(0.16f + i * 0.15f, 0.69f, 0.08f, 0.07f, C.WHITE) }
                k.rect(0.16f, 0.36f, 0.30f, 0.28f, C.SKY); k.rect(0.54f, 0.36f, 0.30f, 0.28f, C.SKY) }
            "actress" -> { woman(k, 0.5f, 0.92f, 0.80f, 0xFF7A1F2B.toInt(), C.INK); sparkles(k, listOf(0.18f to 0.22f, 0.82f to 0.30f), 0.08f) }
            "actor" -> { person(k, 0.5f, 0.92f, 0.80f, C.INK, C.INK); k.poly(C.WHITE, 0.46f, 0.36f, 0.54f, 0.36f, 0.5f, 0.46f); sparkles(k, listOf(0.18f to 0.22f, 0.82f to 0.30f), 0.08f) }
            "action" -> { car(k, C.RED); k.poly(GOLD, 0.62f, 0.02f, 0.50f, 0.22f, 0.60f, 0.22f, 0.48f, 0.40f, 0.72f, 0.16f, 0.62f, 0.16f, 0.72f, 0.02f); for (i in 0 until 3) k.line(0.0f, 0.40f + i * 0.08f, 0.10f, 0.40f + i * 0.08f, 0.03f, C.GREY) }
            "comedy" -> { k.oval(0.14f, 0.14f, 0.72f, 0.72f, GOLD); k.oval(0.30f, 0.32f, 0.12f, 0.08f, C.INK); k.oval(0.58f, 0.32f, 0.12f, 0.08f, C.INK); k.arc(0.5f, 0.50f, 0.20f, 10f, 170f, 0.06f, C.INK) }
            "drama" -> { k.oval(0.14f, 0.14f, 0.72f, 0.72f, 0xFF8FA0B8.toInt()); k.oval(0.30f, 0.34f, 0.12f, 0.08f, C.INK); k.oval(0.58f, 0.34f, 0.12f, 0.08f, C.INK); k.arc(0.5f, 0.74f, 0.18f, 200f, 340f, 0.06f, C.INK); k.oval(0.32f, 0.46f, 0.05f, 0.08f, C.SKY) }
            "cartoon" -> { k.rr(0.08f, 0.14f, 0.74f, 0.58f, 0.04f, C.INK); k.rr(0.12f, 0.18f, 0.66f, 0.50f, 0.02f, C.WHITE); k.oval(0.24f, 0.30f, 0.22f, 0.22f, GOLD); k.oval(0.50f, 0.38f, 0.20f, 0.20f, C.RED)
                k.line(0.62f, 0.92f, 0.92f, 0.52f, 0.08f, 0xFFF2B233.toInt()); k.poly(SKIN, 0.58f, 0.98f, 0.62f, 0.88f, 0.66f, 0.94f) }
            "scary" -> { k.poly(C.WHITE, 0.24f, 0.90f, 0.24f, 0.36f, 0.36f, 0.14f, 0.64f, 0.14f, 0.76f, 0.36f, 0.76f, 0.90f, 0.66f, 0.80f, 0.56f, 0.90f, 0.46f, 0.80f, 0.36f, 0.90f)
                k.oval(0.36f, 0.34f, 0.10f, 0.14f, C.INK); k.oval(0.54f, 0.34f, 0.10f, 0.14f, C.INK); k.oval(0.44f, 0.54f, 0.12f, 0.12f, C.INK) }
            "hero_cape" -> { k.poly(C.RED, 0.34f, 0.30f, 0.66f, 0.30f, 0.86f, 0.90f, 0.14f, 0.90f); person(k, 0.5f, 0.92f, 0.76f, BLUE); Icons.draw(k.p, "star", k.x + 0.42f * k.s, k.y + 0.36f * k.s, 0.16f * k.s, GOLD) }
            "favourite" -> { sub(k, "film", 0.0f, 0.12f, 0.70f); sub(k, "heart", 0.52f, 0.46f, 0.44f) }
            "slow_down" -> { k.oval(0.06f, 0.06f, 0.88f, 0.88f, C.RED); k.oval(0.14f, 0.14f, 0.72f, 0.72f, C.WHITE); Icons.draw(k.p, "slow", k.x + 0.22f * k.s, k.y + 0.24f * k.s, 0.56f * k.s, C.INK) }
            "roundabout" -> { k.oval(0.04f, 0.04f, 0.92f, 0.92f, 0xFF5E6472.toInt()); k.oval(0.32f, 0.32f, 0.36f, 0.36f, 0xFF5E9E52.toInt()); k.arc(0.5f, 0.5f, 0.30f, 200f, 320f, 0.04f, C.WHITE)
                k.poly(C.WHITE, 0.74f, 0.20f, 0.80f, 0.34f, 0.66f, 0.32f); k.arc(0.5f, 0.5f, 0.30f, 20f, 140f, 0.04f, C.WHITE); k.poly(C.WHITE, 0.26f, 0.80f, 0.20f, 0.66f, 0.34f, 0.68f) }
            "quay" -> { k.rect(0f, 0.10f, 1f, 0.34f, 0xFFB9B4A8.toInt()); k.rect(0f, 0.44f, 1f, 0.12f, 0xFF9C9484.toInt()); k.rect(0f, 0.56f, 1f, 0.40f, 0xFF4F7FB0.toInt())
                for (i in 0 until 3) k.rect(0.10f + i * 0.30f, 0.70f + (i % 2) * 0.10f, 0.16f, 0.03f, 0xFF7FA7D0.toInt()); sub(k, "tree", 0.08f, -0.02f, 0.34f); sub(k, "tree", 0.58f, -0.02f, 0.34f) }
            "behind_us" -> { car(k, C.GREEN); van(Icons.K(k.p, k.x - 0.02f * k.s, k.y + 0.28f * k.s, k.s * 0.42f), C.RED); Draw.arrow(k.p, k.x + 0.18f * k.s, k.y + 0.14f * k.s, 0.18f * k.s, "left", C.RED) }
            "followed" -> { k.oval(0.10f, 0.08f, 0.36f, 0.22f, C.WHITE); k.oval(0.54f, 0.08f, 0.36f, 0.22f, C.WHITE); k.oval(0.22f, 0.12f, 0.14f, 0.14f, C.INK); k.oval(0.66f, 0.12f, 0.14f, 0.14f, C.INK)
                van(Icons.K(k.p, k.x + 0.10f * k.s, k.y + 0.30f * k.s, k.s * 0.80f), C.RED) }

            // ---- jewellery and presents ----
            "jewel" -> { gem(k, 0.5f, 0.44f, 0.34f, DIAMOND); sparkles(k, listOf(0.16f to 0.16f, 0.84f to 0.74f), 0.07f) }
            "ring" -> { ring(k, 0.5f, 0.62f, 0.26f, GOLD); gem(k, 0.5f, 0.28f, 0.14f, DIAMOND) }
            "wristwatch" -> { k.rr(0.38f, 0.02f, 0.24f, 0.96f, 0.06f, 0xFF8A5A2B.toInt()); clockFace(k, 0.22f, 0.26f, 0.56f); k.ring(0.22f, 0.26f, 0.56f, 0.56f, 0.28f, 0.04f, SILVER_D) }
            "necklace_pearl" -> necklace(k, PEARL, null)
            "necklace_gold" -> necklace(k, GOLD, DIAMOND)
            "bracelet" -> { k.ring(0.14f, 0.26f, 0.72f, 0.50f, 0.25f, 0.10f, GOLD); k.ring(0.17f, 0.29f, 0.66f, 0.44f, 0.22f, 0.02f, GOLD_D); for (x in listOf(0.30f, 0.5f, 0.70f)) k.oval(x - 0.04f, 0.70f, 0.08f, 0.08f, DIAMOND) }
            "earrings" -> { for (x in listOf(0.30f, 0.70f)) { k.ring(x - 0.08f, 0.10f, 0.16f, 0.16f, 0.08f, 0.03f, GOLD); k.line(x, 0.26f, x, 0.44f, 0.03f, GOLD); k.oval(x - 0.115f, 0.425f, 0.23f, 0.33f, 0xFFB9AE96.toInt()); k.oval(x - 0.10f, 0.44f, 0.20f, 0.30f, PEARL); k.oval(x - 0.04f, 0.48f, 0.06f, 0.08f, C.WHITE) } }
            "diamond" -> { gem(k, 0.5f, 0.46f, 0.40f, DIAMOND); k.line(0.18f, 0.32f, 0.82f, 0.32f, 0.02f, C.WHITE) }
            "pearl" -> { k.oval(0.18f, 0.18f, 0.64f, 0.64f, PEARL); k.ring(0.18f, 0.18f, 0.64f, 0.64f, 0.32f, 0.03f, 0xFFB9AE96.toInt()); k.oval(0.32f, 0.28f, 0.16f, 0.12f, C.WHITE) }
            "gold" -> { k.poly(GOLD_D, 0.10f, 0.74f, 0.90f, 0.74f, 0.76f, 0.40f, 0.24f, 0.40f); k.poly(GOLD, 0.28f, 0.44f, 0.72f, 0.44f, 0.66f, 0.30f, 0.34f, 0.30f); k.rect(0.30f, 0.50f, 0.40f, 0.04f, 0xFFFFE08A.toInt()) }
            "silver" -> { k.poly(SILVER_D, 0.10f, 0.74f, 0.90f, 0.74f, 0.76f, 0.40f, 0.24f, 0.40f); k.poly(SILVER, 0.28f, 0.44f, 0.72f, 0.44f, 0.66f, 0.30f, 0.34f, 0.30f); k.rect(0.30f, 0.50f, 0.40f, 0.04f, C.WHITE) }
            "expensive" -> { k.poly(GOLD, 0.10f, 0.40f, 0.36f, 0.14f, 0.86f, 0.14f, 0.86f, 0.66f, 0.36f, 0.66f); k.oval(0.24f, 0.34f, 0.10f, 0.10f, C.WHITE); k.text("€€€", 0.60f, 0.40f, 0.20f, C.INK); k.text("!", 0.5f, 0.84f, 0.26f, C.RED) }
            "gift" -> { k.rr(0.14f, 0.38f, 0.72f, 0.54f, 0.03f, C.RED); k.rr(0.10f, 0.28f, 0.80f, 0.14f, 0.03f, C.RED_D); k.rect(0.45f, 0.28f, 0.10f, 0.64f, GOLD)
                k.oval(0.28f, 0.12f, 0.22f, 0.18f, GOLD); k.oval(0.50f, 0.12f, 0.22f, 0.18f, GOLD) }
            "give_gift" -> { sub(k, "gift", 0.26f, 0.0f, 0.50f); k.rr(0.06f, 0.56f, 0.40f, 0.16f, 0.06f, SKIN); k.rr(0.54f, 0.56f, 0.40f, 0.16f, 0.06f, SKIN_D); k.rect(0.0f, 0.60f, 0.08f, 0.24f, BLUE); k.rect(0.92f, 0.60f, 0.08f, 0.24f, C.RED) }
            "birthday" -> { k.rr(0.16f, 0.46f, 0.68f, 0.40f, 0.04f, 0xFFF3B9B5.toInt()); k.rect(0.16f, 0.58f, 0.68f, 0.06f, C.WHITE); for (i in 0 until 3) { val x = 0.30f + i * 0.20f; k.rect(x - 0.025f, 0.26f, 0.05f, 0.20f, BLUE); k.oval(x - 0.04f, 0.14f, 0.08f, 0.12f, GOLD) } }
            "wedding" -> { ring(k, 0.38f, 0.56f, 0.22f, GOLD); ring(k, 0.62f, 0.56f, 0.22f, GOLD_D); sub(k, "heart", 0.38f, 0.04f, 0.24f) }
            "anniversary" -> { calendar(k, 0.08f, 0.10f, 0.50f, C.RED); ring(k, 0.62f, 0.66f, 0.16f, GOLD); ring(k, 0.80f, 0.66f, 0.16f, GOLD_D) }
            "christmas" -> { k.poly(0xFF2F7A4A.toInt(), 0.5f, 0.06f, 0.20f, 0.46f, 0.32f, 0.46f, 0.12f, 0.76f, 0.88f, 0.76f, 0.68f, 0.46f, 0.80f, 0.46f); k.rect(0.44f, 0.76f, 0.12f, 0.14f, WOOD_D)
                Icons.draw(k.p, "star", k.x + 0.42f * k.s, k.y + 0.0f, 0.16f * k.s, GOLD); for ((x, y) in listOf(0.40f to 0.40f, 0.60f to 0.56f, 0.34f to 0.66f)) k.oval(x, y, 0.07f, 0.07f, C.RED) }
            "mothers_day" -> { woman(k, 0.36f, 0.94f, 0.80f, PURPLE); for ((x, y) in listOf(0.72f to 0.30f, 0.82f to 0.40f, 0.70f to 0.44f)) k.oval(x - 0.06f, y - 0.06f, 0.12f, 0.12f, PINK); k.line(0.74f, 0.44f, 0.72f, 0.80f, 0.03f, C.GREEN) }
            "camera" -> { k.rr(0.12f, 0.30f, 0.60f, 0.34f, 0.05f, 0xFFD8DBE6.toInt()); k.poly(0xFFB9C0CC.toInt(), 0.72f, 0.36f, 0.92f, 0.28f, 0.92f, 0.66f, 0.72f, 0.58f); k.oval(0.20f, 0.38f, 0.16f, 0.16f, C.INK); k.oval(0.24f, 0.42f, 0.06f, 0.06f, C.SKY)
                k.oval(0.58f, 0.36f, 0.06f, 0.06f, C.RED); k.rect(0.38f, 0.64f, 0.08f, 0.20f, C.GREY); k.rect(0.26f, 0.84f, 0.32f, 0.06f, C.GREY) }
            "bell" -> { k.poly(GOLD, 0.24f, 0.70f, 0.30f, 0.30f, 0.5f, 0.16f, 0.70f, 0.30f, 0.76f, 0.70f); k.rect(0.16f, 0.70f, 0.68f, 0.06f, GOLD_D); k.oval(0.44f, 0.76f, 0.12f, 0.12f, GOLD_D); k.oval(0.45f, 0.08f, 0.10f, 0.10f, GOLD_D) }
            "panther" -> {
                // a sitting big cat, in black and white diamonds
                val c = 0xFF2B2B3A.toInt()
                k.poly(c, 0.30f, 0.90f, 0.30f, 0.50f, 0.40f, 0.34f, 0.60f, 0.40f, 0.66f, 0.62f, 0.72f, 0.90f)
                k.oval(0.34f, 0.12f, 0.32f, 0.28f, c); k.poly(c, 0.36f, 0.16f, 0.38f, 0.06f, 0.44f, 0.14f); k.poly(c, 0.56f, 0.14f, 0.62f, 0.06f, 0.64f, 0.16f)
                k.oval(0.40f, 0.20f, 0.06f, 0.05f, 0xFF3FB56A.toInt()); k.oval(0.54f, 0.20f, 0.06f, 0.05f, 0xFF3FB56A.toInt())
                k.line(0.72f, 0.88f, 0.90f, 0.60f, 0.05f, c)
                for ((x, y) in listOf(0.40f to 0.52f, 0.52f to 0.60f, 0.42f to 0.72f, 0.58f to 0.78f, 0.48f to 0.44f)) k.oval(x, y, 0.06f, 0.06f, DIAMOND)
            }

            // ---- the metro ----
            "card" -> card(k, 0.08f, 0.24f, 0.84f, 0.52f, BLUE, "")
            "navigo" -> { card(k, 0.08f, 0.24f, 0.84f, 0.52f, PURPLE, "EASY"); k.oval(0.62f, 0.30f, 0.20f, 0.20f, C.WHITE); k.ring(0.66f, 0.34f, 0.12f, 0.12f, 0.06f, 0.02f, PURPLE) }
            "mobile" -> phone(k, 0.30f, 0.06f, 0.40f, 0.86f)
            "app" -> { phone(k, 0.30f, 0.06f, 0.40f, 0.86f); for (r in 0 until 3) for (c2 in 0 until 2) k.rr(0.38f + c2 * 0.14f, 0.18f + r * 0.18f, 0.10f, 0.10f, 0.02f, if ((r + c2) % 2 == 0) C.RED else C.GREEN) }
            "buy" -> { k.poly(C.GREY, 0.10f, 0.20f, 0.24f, 0.20f, 0.34f, 0.66f, 0.80f, 0.66f, 0.88f, 0.34f, 0.28f, 0.34f); k.rect(0.30f, 0.34f, 0.56f, 0.28f, C.GREY_L)
                k.oval(0.34f, 0.72f, 0.12f, 0.12f, C.INK); k.oval(0.70f, 0.72f, 0.12f, 0.12f, C.INK); sub(k, "coin:2", 0.44f, 0.04f, 0.30f) }
            "recharge" -> { card(k, 0.06f, 0.30f, 0.62f, 0.40f, PURPLE, ""); k.oval(0.56f, 0.40f, 0.40f, 0.40f, C.GREEN); k.rect(0.73f, 0.46f, 0.06f, 0.28f, C.WHITE); k.rect(0.62f, 0.57f, 0.28f, 0.06f, C.WHITE) }
            "validate" -> { k.rr(0.14f, 0.50f, 0.72f, 0.40f, 0.04f, 0xFF4A5068.toInt()); k.rr(0.28f, 0.42f, 0.44f, 0.16f, 0.03f, PURPLE); card(k, 0.30f, 0.06f, 0.40f, 0.28f, PURPLE, "")
                for (i in 0 until 2) k.arc(0.5f, 0.40f, 0.10f + i * 0.06f, 200f, 340f, 0.025f, C.GREEN) }
            "ride_one" -> { metroSign(k, 0.06f, 0.20f, 0.46f); sub(k, "num:1", 0.54f, 0.24f, 0.40f) }
            "gate_metro" -> { k.rr(0.06f, 0.26f, 0.18f, 0.66f, 0.03f, 0xFF4A5068.toInt()); k.rr(0.76f, 0.26f, 0.18f, 0.66f, 0.03f, 0xFF4A5068.toInt())
                k.rr(0.26f, 0.36f, 0.22f, 0.44f, 0.05f, C.SKY); k.rr(0.52f, 0.36f, 0.22f, 0.44f, 0.05f, C.SKY); k.rect(0.06f, 0.26f, 0.18f, 0.08f, PURPLE) }
            "phone_reader" -> { k.rr(0.14f, 0.58f, 0.72f, 0.34f, 0.04f, 0xFF4A5068.toInt()); k.rr(0.30f, 0.52f, 0.40f, 0.12f, 0.03f, PURPLE); phone(k, 0.38f, 0.04f, 0.24f, 0.42f) }
            "metro_line" -> { k.rect(0.06f, 0.46f, 0.88f, 0.08f, PINK); for (i in 0 until 4) { val x = 0.10f + i * 0.26f; k.oval(x, 0.42f, 0.14f, 0.14f, C.WHITE); k.ring(x, 0.42f, 0.14f, 0.14f, 0.07f, 0.03f, C.INK) }
                k.oval(0.04f, 0.04f, 0.30f, 0.30f, PINK); k.text("7", 0.19f, 0.20f, 0.22f, C.INK) }
            "direction" -> { k.rr(0.04f, 0.28f, 0.92f, 0.36f, 0.04f, NAVY); k.text("Direction", 0.46f, 0.40f, 0.10f, C.WHITE, Font.BOLD); k.rect(0.16f, 0.50f, 0.50f, 0.05f, C.WHITE); Draw.arrow(k.p, k.x + 0.82f * k.s, k.y + 0.46f * k.s, 0.18f * k.s, "right", GOLD) }
            "metro" -> { k.rect(0.47f, 0.40f, 0.06f, 0.56f, 0xFF27774C.toInt()); metroSign(k, 0.18f, 0.04f, 0.64f) }
            "change" -> { k.line(0.06f, 0.70f, 0.94f, 0.30f, 0.08f, PINK); k.line(0.06f, 0.30f, 0.94f, 0.70f, 0.08f, OCHRE); k.oval(0.40f, 0.40f, 0.20f, 0.20f, C.WHITE); k.ring(0.40f, 0.40f, 0.20f, 0.20f, 0.10f, 0.04f, C.INK) }
            "get_off" -> { k.rr(0.04f, 0.10f, 0.50f, 0.80f, 0.04f, 0xFF3F6DB5.toInt()); k.rect(0.12f, 0.20f, 0.34f, 0.60f, C.SKY); person(k, 0.72f, 0.92f, 0.64f, C.RED); Draw.arrow(k.p, k.x + 0.86f * k.s, k.y + 0.16f * k.s, 0.18f * k.s, "right", C.GREEN) }
            "lost_way" -> { person(k, 0.40f, 0.92f, 0.72f, BLUE); k.text("?", 0.76f, 0.24f, 0.34f, C.RED); k.text("?", 0.16f, 0.30f, 0.22f, C.RED) }
            "give_back" -> { bookC(k, 0.32f, 0.06f, 0.36f, 0.48f, 0xFF2F548F.toInt()); Draw.arrow(k.p, k.x + 0.30f * k.s, k.y + 0.76f * k.s, 0.22f * k.s, "left", C.GREEN); Draw.arrow(k.p, k.x + 0.70f * k.s, k.y + 0.76f * k.s, 0.22f * k.s, "right", C.GREEN) }
            "stop_count" -> { k.rect(0.06f, 0.46f, 0.88f, 0.08f, PINK); for (i in 0 until 4) { val x = 0.10f + i * 0.26f; k.oval(x, 0.42f, 0.14f, 0.14f, C.WHITE); k.ring(x, 0.42f, 0.14f, 0.14f, 0.07f, 0.03f, C.INK); k.text("${i + 1}", x + 0.07f, 0.28f, 0.14f, C.INK) } }

            // ---- the bookshop ----
            "bookshop" -> { k.rect(0.06f, 0.24f, 0.88f, 0.70f, 0xFF2F6B4A.toInt()); k.rect(0.06f, 0.10f, 0.88f, 0.16f, 0xFF27574C.toInt()); k.rect(0.14f, 0.36f, 0.32f, 0.40f, 0xFFF2D58A.toInt()); shelfRow(k, 0.16f, 0.48f, 0.28f, 0.22f)
                k.rect(0.56f, 0.36f, 0.28f, 0.58f, 0xFF1D3F2F.toInt()) }
            "library" -> { k.poly(0xFFE9E1CF.toInt(), 0.04f, 0.30f, 0.5f, 0.06f, 0.96f, 0.30f); for (i in 0 until 4) k.rect(0.12f + i * 0.22f, 0.34f, 0.10f, 0.48f, 0xFFE9E1CF.toInt()); k.rect(0.04f, 0.82f, 0.92f, 0.10f, 0xFFC9C2B2.toInt())
                bookC(k, 0.40f, 0.40f, 0.20f, 0.28f, C.RED) }
            "author" -> { person(k, 0.34f, 0.92f, 0.76f, PURPLE); k.rr(0.56f, 0.40f, 0.36f, 0.46f, 0.02f, C.WHITE); for (i in 0 until 3) k.line(0.62f, 0.50f + i * 0.10f, 0.86f, 0.50f + i * 0.10f, 0.02f, C.GREY); k.line(0.66f, 0.34f, 0.84f, 0.12f, 0.04f, BLUE) }
            "read" -> { person(k, 0.5f, 0.92f, 0.80f, C.GREEN); k.poly(C.WHITE, 0.24f, 0.50f, 0.5f, 0.56f, 0.76f, 0.50f, 0.76f, 0.72f, 0.5f, 0.78f, 0.24f, 0.72f); k.line(0.5f, 0.56f, 0.5f, 0.78f, 0.02f, C.GREY) }
            "english" -> { bookC(k, 0.20f, 0.10f, 0.60f, 0.80f, 0xFF2F548F.toInt()); k.text("EN", 0.54f, 0.58f, 0.26f, C.WHITE) }
            "french" -> { bookC(k, 0.20f, 0.10f, 0.60f, 0.80f, C.RED); k.text("FR", 0.54f, 0.58f, 0.26f, C.WHITE) }
            "sleep" -> { sub(k, "bed", 0.02f, 0.26f, 0.70f); k.text("z", 0.70f, 0.40f, 0.16f, BLUE); k.text("z", 0.80f, 0.26f, 0.20f, BLUE); k.text("Z", 0.90f, 0.10f, 0.24f, BLUE) }
            "rule" -> { k.rr(0.16f, 0.06f, 0.68f, 0.88f, 0.04f, C.WHITE); k.ring(0.16f, 0.06f, 0.68f, 0.88f, 0.04f, 0.02f, C.GREY)
                for (i in 0 until 3) { k.text("${i + 1}", 0.28f, 0.24f + i * 0.24f, 0.14f, C.RED); k.line(0.38f, 0.24f + i * 0.24f, 0.74f, 0.24f + i * 0.24f, 0.03f, C.INK2) } }
            "must" -> { k.poly(C.MUSTARD, 0.5f, 0.06f, 0.94f, 0.88f, 0.06f, 0.88f); k.poly(C.INK, 0.5f, 0.12f, 0.88f, 0.84f, 0.12f, 0.84f); k.poly(C.MUSTARD, 0.5f, 0.20f, 0.80f, 0.80f, 0.20f, 0.80f); k.rect(0.46f, 0.38f, 0.08f, 0.24f, C.INK); k.oval(0.46f, 0.66f, 0.08f, 0.08f, C.INK) }
            "book_a_day" -> { bookC(k, 0.06f, 0.30f, 0.38f, 0.52f, C.RED); calendar(k, 0.52f, 0.20f, 0.42f, C.RED); k.text("1", 0.73f, 0.52f, 0.18f, C.INK) }
            "write" -> { k.rr(0.10f, 0.16f, 0.58f, 0.76f, 0.03f, C.WHITE); k.ring(0.10f, 0.16f, 0.58f, 0.76f, 0.03f, 0.02f, C.GREY); for (i in 0 until 4) k.line(0.18f, 0.32f + i * 0.14f, 0.58f, 0.32f + i * 0.14f, 0.02f, C.GREY_L)
                k.line(0.46f, 0.70f, 0.88f, 0.20f, 0.06f, BLUE); k.poly(C.INK, 0.42f, 0.78f, 0.44f, 0.68f, 0.50f, 0.72f) }
            "my_life" -> { person(k, 0.20f, 0.92f, 0.50f, C.SKY); person(k, 0.50f, 0.92f, 0.66f, C.GREEN); person(k, 0.80f, 0.92f, 0.82f, PURPLE); Draw.arrow(k.p, k.x + 0.5f * k.s, k.y + 0.10f * k.s, 0.18f * k.s, "right", C.RED) }
            "autobio" -> { k.rr(0.14f, 0.04f, 0.72f, 0.92f, 0.03f, C.WHITE); k.ring(0.14f, 0.04f, 0.72f, 0.92f, 0.03f, 0.02f, C.GREY); k.oval(0.36f, 0.12f, 0.28f, 0.28f, SKIN); k.rr(0.36f, 0.10f, 0.28f, 0.10f, 0.05f, 0xFFB5484D.toInt())
                for (i in 0 until 4) k.line(0.24f, 0.52f + i * 0.10f, 0.76f, 0.52f + i * 0.10f, 0.025f, C.GREY) }
            "cat" -> { val c = 0xFF8A8A8A.toInt(); k.oval(0.16f, 0.46f, 0.60f, 0.40f, c); k.oval(0.56f, 0.26f, 0.32f, 0.30f, c); k.poly(c, 0.58f, 0.32f, 0.60f, 0.16f, 0.68f, 0.28f); k.poly(c, 0.76f, 0.28f, 0.84f, 0.16f, 0.86f, 0.32f)
                k.oval(0.64f, 0.36f, 0.06f, 0.05f, C.INK); k.oval(0.76f, 0.36f, 0.06f, 0.05f, C.INK); k.line(0.18f, 0.70f, 0.06f, 0.46f, 0.06f, c) }
            "typewriter" -> { k.rr(0.10f, 0.40f, 0.80f, 0.46f, 0.06f, 0xFF3A3D52.toInt()); k.rr(0.30f, 0.12f, 0.40f, 0.34f, 0.02f, C.WHITE); k.rect(0.16f, 0.36f, 0.68f, 0.08f, C.INK)
                for (r in 0 until 2) for (c2 in 0 until 5) k.oval(0.20f + c2 * 0.13f, 0.54f + r * 0.13f, 0.08f, 0.08f, C.GREY_L) }

            // ---- backstage ----
            "fashion_show" -> { k.rect(0.38f, 0.40f, 0.24f, 0.56f, 0xFFE9E4D8.toInt()); woman(k, 0.5f, 0.70f, 0.60f, C.RED, C.INK); for (x in listOf(0.12f, 0.24f, 0.76f, 0.88f)) k.oval(x - 0.05f, 0.80f, 0.10f, 0.10f, C.INK2); sparkles(k, listOf(0.20f to 0.16f, 0.82f to 0.20f), 0.06f) }
            "runway" -> { k.poly(0xFFE9E4D8.toInt(), 0.36f, 0.10f, 0.64f, 0.10f, 0.84f, 0.96f, 0.16f, 0.96f); k.rect(0f, 0f, 1f, 0.10f, C.RED_D); for (i in 0 until 4) { k.oval(0.04f, 0.30f + i * 0.18f, 0.10f, 0.10f, C.INK2); k.oval(0.86f, 0.30f + i * 0.18f, 0.10f, 0.10f, C.INK2) } }
            "dressing_room" -> { k.rr(0.12f, 0.10f, 0.76f, 0.62f, 0.04f, 0xFF8A5A2B.toInt()); k.rr(0.20f, 0.18f, 0.60f, 0.46f, 0.02f, 0xFFD3ECFB.toInt()); for (i in 0 until 4) { k.oval(0.16f + i * 0.20f, 0.06f, 0.08f, 0.08f, 0xFFFFF4C8.toInt()) }
                k.rect(0.06f, 0.72f, 0.88f, 0.08f, WOOD_D); sub(k, "dress:d9534f:short", 0.04f, 0.30f, 0.34f) }
            "model" -> { woman(k, 0.5f, 0.94f, 0.90f, 0xFF22223B.toInt(), 0xFFE8C55A.toInt()); sparkles(k, listOf(0.18f to 0.30f), 0.07f) }
            "makeup" -> { k.rr(0.16f, 0.40f, 0.20f, 0.50f, 0.03f, C.INK); k.rr(0.18f, 0.20f, 0.16f, 0.24f, 0.06f, C.RED); k.rr(0.46f, 0.50f, 0.42f, 0.30f, 0.15f, 0xFFF3B9B5.toInt()); k.oval(0.52f, 0.54f, 0.30f, 0.22f, 0xFFE58B96.toInt())
                k.line(0.64f, 0.44f, 0.86f, 0.06f, 0.05f, WOOD); k.oval(0.56f, 0.38f, 0.16f, 0.12f, 0xFF4A3018.toInt()) }
            "hairdo" -> { k.oval(0.24f, 0.24f, 0.52f, 0.60f, SKIN); k.oval(0.18f, 0.08f, 0.64f, 0.40f, 0xFF8A5A2B.toInt()); k.oval(0.38f, 0.00f, 0.24f, 0.22f, 0xFF8A5A2B.toInt())
                k.rect(0.70f, 0.40f, 0.26f, 0.08f, C.INK); for (i in 0 until 5) k.rect(0.72f + i * 0.05f, 0.48f, 0.02f, 0.10f, C.INK) }
            "get_dressed" -> { sub(k, "dress:3f6db5:long", 0.0f, 0.0f, 0.60f); person(k, 0.72f, 0.94f, 0.80f, C.GREY_L) }
            "enter" -> { k.rr(0.40f, 0.08f, 0.50f, 0.86f, 0.03f, WOOD_D); k.rect(0.46f, 0.14f, 0.38f, 0.80f, 0xFF2B2B3A.toInt()); Draw.arrow(k.p, k.x + 0.38f * k.s, k.y + 0.52f * k.s, 0.34f * k.s, "right", C.GREEN) }
            "safe" -> { k.rr(0.10f, 0.10f, 0.80f, 0.80f, 0.06f, C.INK); k.rr(0.16f, 0.16f, 0.68f, 0.68f, 0.04f, SILVER_D); k.oval(0.34f, 0.34f, 0.32f, 0.32f, C.INK); k.oval(0.40f, 0.40f, 0.20f, 0.20f, SILVER)
                for (i in 0 until 8) { val a = Math.toRadians((i * 45).toDouble()); k.line(0.5f, 0.5f, 0.5f + 0.13f * Math.cos(a).toFloat(), 0.5f + 0.13f * Math.sin(a).toFloat(), 0.02f, C.INK) }
                k.rect(0.74f, 0.40f, 0.06f, 0.20f, GOLD) }
            "keypad" -> { k.rr(0.20f, 0.06f, 0.60f, 0.88f, 0.06f, C.INK); for (r in 0 until 4) for (c2 in 0 until 3) k.rr(0.28f + c2 * 0.16f, 0.16f + r * 0.18f, 0.12f, 0.12f, 0.02f, if (r == 3 && c2 != 1) C.GREEN else SILVER) }
            "locked" -> lock(k, false)
            "unlocked" -> lock(k, true)
            "before" -> { clockFace(k, 0.40f, 0.20f, 0.56f); Draw.arrow(k.p, k.x + 0.20f * k.s, k.y + 0.48f * k.s, 0.30f * k.s, "left", C.RED) }
            "ready" -> { woman(k, 0.40f, 0.94f, 0.86f, C.RED, C.INK); Icons.draw(k.p, "check", k.x + 0.56f * k.s, k.y + 0.04f * k.s, 0.40f * k.s, C.GREEN) }
            "sparkle" -> sparkles(k, listOf(0.30f to 0.30f, 0.68f to 0.50f, 0.36f to 0.74f), 0.16f)
            "badge" -> { k.rect(0.40f, 0.0f, 0.20f, 0.30f, C.RED); k.rr(0.16f, 0.24f, 0.68f, 0.70f, 0.05f, C.WHITE); k.ring(0.16f, 0.24f, 0.68f, 0.70f, 0.05f, 0.03f, C.INK); k.rect(0.16f, 0.24f, 0.68f, 0.16f, C.INK)
                k.oval(0.36f, 0.46f, 0.28f, 0.28f, SKIN); k.text("VIP", 0.5f, 0.84f, 0.14f, C.RED) }
            "desk" -> { k.rect(0.06f, 0.44f, 0.88f, 0.12f, WOOD); k.rect(0.10f, 0.56f, 0.08f, 0.38f, WOOD_D); k.rect(0.82f, 0.56f, 0.08f, 0.38f, WOOD_D); k.rr(0.22f, 0.14f, 0.36f, 0.30f, 0.02f, C.INK); k.rr(0.25f, 0.17f, 0.30f, 0.22f, 0.02f, C.SKY) }

            // ---- strong and weak, the plan ----
            "safe_shield" -> { k.poly(C.GREEN_D, 0.5f, 0.04f, 0.88f, 0.18f, 0.84f, 0.60f, 0.5f, 0.96f, 0.16f, 0.60f, 0.12f, 0.18f); k.poly(C.GREEN, 0.5f, 0.10f, 0.82f, 0.22f, 0.78f, 0.58f, 0.5f, 0.88f, 0.22f, 0.58f, 0.18f, 0.22f)
                Icons.draw(k.p, "check", k.x + 0.30f * k.s, k.y + 0.28f * k.s, 0.40f * k.s, C.WHITE) }
            "well_done" -> { // a rosette with a tick
                for (i in 0 until 10) { val a = Math.toRadians((i * 36).toDouble()); k.oval(0.5f + 0.30f * Math.cos(a).toFloat() - 0.12f, 0.42f + 0.30f * Math.sin(a).toFloat() - 0.12f, 0.24f, 0.24f, GOLD_D) }
                k.poly(C.RED, 0.34f, 0.62f, 0.44f, 0.62f, 0.40f, 0.96f, 0.30f, 0.90f); k.poly(C.RED, 0.56f, 0.62f, 0.66f, 0.62f, 0.70f, 0.90f, 0.60f, 0.96f)
                k.oval(0.22f, 0.14f, 0.56f, 0.56f, GOLD); Icons.draw(k.p, "check", k.x + 0.30f * k.s, k.y + 0.22f * k.s, 0.40f * k.s, C.WHITE) }
            "strong" -> { wall(k, 0.06f, 0.10f, 0.88f, 0.80f, false) }
            "weak" -> { wall(k, 0.06f, 0.10f, 0.88f, 0.80f, true) }
            "weak_spot" -> { wall(k, 0.06f, 0.10f, 0.88f, 0.80f, true); k.ring(0.30f, 0.30f, 0.40f, 0.40f, 0.20f, 0.04f, C.RED); k.ring(0.40f, 0.40f, 0.20f, 0.20f, 0.10f, 0.04f, C.RED) }
            "nobody" -> { person(k, 0.5f, 0.92f, 0.80f, C.GREY_L); k.ring(0.10f, 0.08f, 0.80f, 0.84f, 0.40f, 0.07f, C.RED); k.line(0.20f, 0.20f, 0.80f, 0.80f, 0.07f, C.RED) }
            "only" -> { // three people in the dark, and a spotlight on one of them
                k.poly(0xFFFFF4C8.toInt(), 0.44f, 0.0f, 0.56f, 0.0f, 0.74f, 0.94f, 0.26f, 0.94f)
                person(k, 0.14f, 0.94f, 0.56f, C.GREY_L, C.GREY); person(k, 0.86f, 0.94f, 0.56f, C.GREY_L, C.GREY); person(k, 0.5f, 0.94f, 0.66f, C.RED, C.INK) }
            "tracker" -> { k.rr(0.20f, 0.30f, 0.60f, 0.40f, 0.08f, C.INK); k.oval(0.40f, 0.40f, 0.20f, 0.20f, C.RED); k.oval(0.46f, 0.44f, 0.06f, 0.06f, C.WHITE)
                for (i in 0 until 2) { k.arc(0.5f, 0.30f, 0.16f + i * 0.10f, 220f, 320f, 0.03f, C.RED) } }
            "hedge" -> { k.rr(0.06f, 0.26f, 0.88f, 0.64f, 0.16f, HEDGE); for ((x, y) in listOf(0.18f to 0.36f, 0.42f to 0.30f, 0.66f to 0.40f, 0.30f to 0.60f, 0.58f to 0.66f)) k.oval(x, y, 0.16f, 0.14f, 0xFF3A7A5A.toInt()) }
            "mansion" -> { k.rect(0.06f, 0.32f, 0.88f, 0.60f, 0xFFE9E1CF.toInt()); k.poly(0xFF4A5068.toInt(), 0.02f, 0.34f, 0.5f, 0.08f, 0.98f, 0.34f)
                for (r in 0 until 2) for (c2 in 0 until 4) if (!(r == 1 && (c2 == 1 || c2 == 2))) k.rect(0.12f + c2 * 0.21f, 0.40f + r * 0.26f, 0.12f, 0.16f, 0xFFF2D58A.toInt())
                k.rect(0.42f, 0.66f, 0.16f, 0.26f, 0xFF22305A.toInt()) }

            // ---- dressing up ----
            "chic" -> { k.poly(C.INK, 0.10f, 0.30f, 0.46f, 0.50f, 0.10f, 0.70f); k.poly(C.INK, 0.90f, 0.30f, 0.54f, 0.50f, 0.90f, 0.70f); k.oval(0.42f, 0.42f, 0.16f, 0.16f, C.INK2); sparkles(k, listOf(0.80f to 0.16f), 0.08f) }
            "elegant" -> { dress(k, 0xFF7A1F2B.toInt(), true); sparkles(k, listOf(0.16f to 0.20f, 0.84f to 0.60f), 0.07f) }
            "too_plain" -> { k.poly(C.GREY_L, 0.30f, 0.20f, 0.70f, 0.20f, 0.70f, 0.86f, 0.30f, 0.86f); k.poly(C.GREY, 0.30f, 0.20f, 0.12f, 0.40f, 0.20f, 0.48f, 0.32f, 0.36f); k.poly(C.GREY, 0.70f, 0.20f, 0.88f, 0.40f, 0.80f, 0.48f, 0.68f, 0.36f)
                k.text("?", 0.84f, 0.12f, 0.22f, C.RED) }
            "evening_dress" -> { dress(k, 0xFF22223B.toInt(), true); sub(k, "moon", 0.66f, 0.0f, 0.30f); sparkles(k, listOf(0.40f to 0.60f, 0.58f to 0.76f), 0.05f) }
            "well_dressed" -> { sub(k, "mirror", 0.0f, 0.06f, 0.70f); Icons.draw(k.p, "check", k.x + 0.56f * k.s, k.y + 0.44f * k.s, 0.40f * k.s, C.GREEN) }
            "call_taxi" -> { sub(k, "taxi", 0.30f, 0.30f, 0.66f); person(k, 0.20f, 0.94f, 0.78f, PURPLE); k.rr(0.24f, 0.08f, 0.08f, 0.24f, 0.04f, SKIN) }
            "classic" -> { dress(k, 0xFF22223B.toInt(), false); k.rect(0.40f, 0.20f, 0.20f, 0.03f, PEARL) }
            "modern" -> { k.poly(C.RED, 0.30f, 0.14f, 0.70f, 0.14f, 0.82f, 0.88f, 0.18f, 0.88f); k.poly(GOLD, 0.30f, 0.14f, 0.70f, 0.14f, 0.18f, 0.88f); k.oval(0.44f, 0.04f, 0.12f, 0.10f, C.INK) }
            "colourful" -> { val cs = intArrayOf(C.RED, GOLD, C.GREEN, BLUE, PURPLE); for (i in 0 until 5) k.poly(cs[i], 0.30f + i * 0.08f, 0.14f, 0.38f + i * 0.08f, 0.14f, 0.24f + (i + 1) * 0.104f, 0.90f, 0.24f + i * 0.104f, 0.90f) }
            "black_white" -> dress(k, -1, true)
            "plain" -> { k.poly(C.GREY_L, 0.30f, 0.20f, 0.70f, 0.20f, 0.70f, 0.86f, 0.30f, 0.86f); k.poly(C.GREY, 0.30f, 0.20f, 0.12f, 0.40f, 0.20f, 0.48f, 0.32f, 0.36f); k.poly(C.GREY, 0.70f, 0.20f, 0.88f, 0.40f, 0.80f, 0.48f, 0.68f, 0.36f) }
            "couture_house" -> { k.rect(0.10f, 0.30f, 0.80f, 0.62f, 0xFFE9E1CF.toInt()); k.rect(0.06f, 0.20f, 0.88f, 0.12f, C.INK); k.rect(0.16f, 0.42f, 0.28f, 0.38f, 0xFFF2D58A.toInt()); sub(k, "dress:d9534f:long", 0.14f, 0.40f, 0.32f)
                k.rect(0.56f, 0.42f, 0.24f, 0.50f, 0xFF22305A.toInt()) }
            "style" -> { sub(k, "dress:7c3f8c:long", 0.0f, 0.06f, 0.56f); sub(k, "hat_big", 0.48f, 0.0f, 0.44f); sub(k, "shoes:22223b", 0.46f, 0.50f, 0.50f) }
            "golden_keys" -> { for (s2 in listOf(-1f, 1f)) { val x0 = 0.5f - s2 * 0.30f; val x1 = 0.5f + s2 * 0.30f; k.line(x0, 0.20f, x1, 0.80f, 0.07f, GOLD); k.ring(x0 - 0.10f, 0.10f, 0.20f, 0.20f, 0.10f, 0.05f, GOLD); k.rect(x1 - 0.08f, 0.76f, 0.16f, 0.06f, GOLD_D) } }
            "full_skirt" -> dress(k, 0xFF22223B.toInt(), true, true)
            "trouser_suit" -> suit(k, 0xFF22223B.toInt())
            else -> return false
        }
        return true
    }
}
