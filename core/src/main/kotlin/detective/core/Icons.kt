package detective.core

/**
 * Small flat pictures drawn with shapes, so they stay sharp at any size.
 * Each one is drawn inside a square of side [s] with its corner at (x, y).
 * Words in the unit files name these with their "icon" field.
 */
object Icons {
    private val SKIN = 0xFFF2C9A0.toInt()
    private val SKIN_D = 0xFFD9A77A.toInt()

    internal class K(val p: Painter, val x: Float, val y: Float, val s: Float) {
        fun rect(a: Float, b: Float, w: Float, h: Float, c: Int) = p.fillRect(x + a * s, y + b * s, w * s, h * s, c)
        fun rr(a: Float, b: Float, w: Float, h: Float, r: Float, c: Int) = p.fillRoundRect(x + a * s, y + b * s, w * s, h * s, r * s, c)
        fun ring(a: Float, b: Float, w: Float, h: Float, r: Float, width: Float, c: Int) =
            p.strokeRoundRect(x + a * s, y + b * s, w * s, h * s, r * s, width * s, c)
        fun oval(a: Float, b: Float, w: Float, h: Float, c: Int) = p.fillOval(x + a * s, y + b * s, w * s, h * s, c)
        fun line(a: Float, b: Float, c2: Float, d: Float, width: Float, c: Int) =
            p.line(x + a * s, y + b * s, x + c2 * s, y + d * s, width * s, c)
        fun poly(c: Int, vararg pts: Float) {
            val out = FloatArray(pts.size)
            var i = 0
            while (i < pts.size) {
                out[i] = x + pts[i] * s
                out[i + 1] = y + pts[i + 1] * s
                i += 2
            }
            p.fillPoly(out, c)
        }
        fun arc(cx: Float, cy: Float, r: Float, deg0: Float, deg1: Float, width: Float, c: Int) {
            val n = 8
            var px = 0f
            var py = 0f
            for (i in 0..n) {
                val a = Math.toRadians((deg0 + (deg1 - deg0) * i / n).toDouble())
                val qx = cx + r * Math.cos(a).toFloat()
                val qy = cy + r * Math.sin(a).toFloat()
                if (i > 0) line(px, py, qx, qy, width, c)
                px = qx
                py = qy
            }
        }
        fun text(t: String, cx: Float, cy: Float, size: Float, c: Int, font: Int = Font.DISPLAY) =
            Text.line(p, t, x + cx * s, y + cy * s, size * s, c, font, Align.CENTER)
    }

    fun draw(p: Painter, name: String, x: Float, y: Float, s: Float, ink: Int = C.INK) {
        val k = K(p, x, y, s)
        when (name) {
            // ---- interface icons, one colour ----
            "speaker" -> {
                k.poly(ink, 0.12f, 0.38f, 0.30f, 0.38f, 0.52f, 0.18f, 0.52f, 0.82f, 0.30f, 0.62f, 0.12f, 0.62f)
                k.arc(0.52f, 0.5f, 0.16f, -45f, 45f, 0.07f, ink)
                k.arc(0.52f, 0.5f, 0.32f, -45f, 45f, 0.07f, ink)
            }
            "mic" -> { // a microphone on a stand
                k.rr(0.36f, 0.08f, 0.28f, 0.48f, 0.14f, ink)
                k.arc(0.5f, 0.42f, 0.24f, 0f, 180f, 0.06f, ink)
                k.rect(0.47f, 0.66f, 0.06f, 0.16f, ink)
                k.rr(0.30f, 0.80f, 0.40f, 0.07f, 0.03f, ink)
            }
            "slow" -> { // a snail
                k.oval(0.30f, 0.22f, 0.50f, 0.50f, ink)
                k.oval(0.42f, 0.34f, 0.26f, 0.26f, C.alpha(C.WHITE, 0.55f))
                k.oval(0.50f, 0.42f, 0.10f, 0.10f, ink)
                k.rr(0.08f, 0.62f, 0.80f, 0.16f, 0.08f, ink)
                k.line(0.14f, 0.66f, 0.10f, 0.40f, 0.06f, ink)
                k.line(0.22f, 0.66f, 0.24f, 0.42f, 0.06f, ink)
                k.oval(0.05f, 0.34f, 0.10f, 0.10f, ink)
                k.oval(0.19f, 0.36f, 0.10f, 0.10f, ink)
            }
            "book" -> {
                k.rr(0.18f, 0.10f, 0.66f, 0.80f, 0.06f, ink)
                k.rr(0.26f, 0.16f, 0.52f, 0.68f, 0.03f, C.PAPER)
                k.rect(0.34f, 0.30f, 0.36f, 0.05f, ink)
                k.rect(0.34f, 0.44f, 0.36f, 0.05f, ink)
                k.rect(0.34f, 0.58f, 0.24f, 0.05f, ink)
                var yy = 0.20f
                while (yy < 0.85f) { k.rr(0.10f, yy, 0.16f, 0.06f, 0.03f, C.GREY); yy += 0.14f }
            }
            "menu" -> {
                k.rr(0.18f, 0.24f, 0.64f, 0.11f, 0.05f, ink)
                k.rr(0.18f, 0.45f, 0.64f, 0.11f, 0.05f, ink)
                k.rr(0.18f, 0.66f, 0.64f, 0.11f, 0.05f, ink)
            }
            "lock" -> {
                k.ring(0.30f, 0.12f, 0.40f, 0.50f, 0.20f, 0.09f, ink)
                k.rr(0.20f, 0.42f, 0.60f, 0.46f, 0.08f, ink)
                k.oval(0.44f, 0.56f, 0.12f, 0.12f, C.alpha(C.WHITE, 0.8f))
            }
            "back" -> Draw.arrow(p, x + s / 2, y + s / 2, s * 0.7f, "left", ink)
            "home" -> { // a little house
                k.poly(ink, 0.08f, 0.50f, 0.50f, 0.12f, 0.92f, 0.50f)
                k.rect(0.20f, 0.46f, 0.60f, 0.42f, ink)
                k.rect(0.42f, 0.62f, 0.16f, 0.26f, C.alpha(C.WHITE, 0.6f))
            }
            "next" -> Draw.arrow(p, x + s / 2, y + s / 2, s * 0.7f, "right", ink)
            "star" -> star(k, 0.5f, 0.52f, 0.44f, ink)
            "check" -> { k.line(0.18f, 0.54f, 0.40f, 0.76f, 0.14f, ink); k.line(0.40f, 0.76f, 0.84f, 0.24f, 0.14f, ink) }
            "cross" -> { k.line(0.22f, 0.22f, 0.78f, 0.78f, 0.14f, ink); k.line(0.78f, 0.22f, 0.22f, 0.78f, 0.14f, ink) }
            "talk" -> {
                k.rr(0.06f, 0.12f, 0.62f, 0.44f, 0.12f, ink)
                k.poly(ink, 0.18f, 0.50f, 0.14f, 0.72f, 0.36f, 0.54f)
                k.rr(0.40f, 0.44f, 0.54f, 0.38f, 0.12f, C.MUSTARD)
                k.poly(C.MUSTARD, 0.72f, 0.78f, 0.84f, 0.94f, 0.62f, 0.80f)
            }
            "walk" -> { // two footprints
                k.oval(0.20f, 0.46f, 0.22f, 0.34f, ink); k.oval(0.22f, 0.84f, 0.18f, 0.12f, ink)
                k.oval(0.58f, 0.10f, 0.22f, 0.34f, ink); k.oval(0.60f, 0.48f, 0.18f, 0.12f, ink)
            }
            "hand" -> { // "touch here": two rings and a fingertip coming in from the corner
                k.ring(0.08f, 0.04f, 0.52f, 0.52f, 0.26f, 0.045f, C.INK2)
                k.ring(0.20f, 0.16f, 0.28f, 0.28f, 0.14f, 0.045f, C.INK2)
                k.line(0.40f, 0.36f, 0.80f, 0.84f, 0.20f, SKIN)
                k.oval(0.62f, 0.66f, 0.34f, 0.34f, SKIN_D)
            }
            "bag" -> {
                k.ring(0.34f, 0.10f, 0.32f, 0.30f, 0.12f, 0.07f, ink)
                k.rr(0.14f, 0.30f, 0.72f, 0.56f, 0.10f, C.BROWN)
                k.rect(0.14f, 0.50f, 0.72f, 0.06f, ink)
                k.rr(0.43f, 0.46f, 0.14f, 0.14f, 0.03f, C.MUSTARD)
            }

            // ---- word pictures ----
            "wave" -> { hand(k); k.arc(0.5f, 0.42f, 0.46f, 200f, 235f, 0.05f, C.INK2); k.arc(0.5f, 0.42f, 0.46f, -55f, -20f, 0.05f, C.INK2) }
            "bye" -> {
                hand(k)
                Draw.arrow(p, x + s * 0.84f, y + s * 0.80f, s * 0.26f, "right", C.INK2)
            }
            "please" -> { // two hands pressed together
                k.poly(SKIN, 0.50f, 0.10f, 0.30f, 0.50f, 0.34f, 0.78f, 0.50f, 0.78f)
                k.poly(SKIN_D, 0.50f, 0.10f, 0.70f, 0.50f, 0.66f, 0.78f, 0.50f, 0.78f)
                k.rr(0.26f, 0.74f, 0.24f, 0.18f, 0.03f, C.INK2)
                k.rr(0.50f, 0.74f, 0.24f, 0.18f, 0.03f, C.INK)
            }
            "thanks" -> { // a heart
                k.oval(0.14f, 0.18f, 0.40f, 0.40f, C.RED)
                k.oval(0.46f, 0.18f, 0.40f, 0.40f, C.RED)
                k.poly(C.RED, 0.17f, 0.48f, 0.83f, 0.48f, 0.50f, 0.88f)
                k.oval(0.24f, 0.26f, 0.12f, 0.10f, C.alpha(C.WHITE, 0.6f))
            }
            "sorry" -> {
                face(k, C.MUSTARD)
                k.line(0.26f, 0.42f, 0.40f, 0.35f, 0.05f, C.INK); k.line(0.74f, 0.42f, 0.60f, 0.35f, 0.05f, C.INK)
                k.oval(0.32f, 0.47f, 0.08f, 0.10f, C.INK); k.oval(0.60f, 0.47f, 0.08f, 0.10f, C.INK)
                k.arc(0.5f, 0.80f, 0.11f, 205f, 335f, 0.05f, C.INK)
                k.poly(C.SKY, 0.84f, 0.26f, 0.78f, 0.42f, 0.90f, 0.42f); k.oval(0.78f, 0.38f, 0.12f, 0.12f, C.SKY)
            }
            "yes" -> { k.oval(0.08f, 0.08f, 0.84f, 0.84f, C.GREEN); k.line(0.28f, 0.52f, 0.44f, 0.68f, 0.11f, C.WHITE); k.line(0.44f, 0.68f, 0.74f, 0.32f, 0.11f, C.WHITE) }
            "no" -> { k.oval(0.08f, 0.08f, 0.84f, 0.84f, C.RED); k.line(0.32f, 0.32f, 0.68f, 0.68f, 0.11f, C.WHITE); k.line(0.68f, 0.32f, 0.32f, 0.68f, 0.11f, C.WHITE) }
            "lady" -> faceImg(p, x, y, s, "icon_lady", FaceSpec(hair = 0xFF7A3E1D.toInt(), hairStyle = "bun", clothes = 0xFF7C3F8C.toInt(), scarf = C.MUSTARD))
            "man" -> faceImg(p, x, y, s, "icon_man", FaceSpec(hair = 0xFF33302E.toInt(), hairStyle = "short", clothes = 0xFF3B4A7A.toInt(), moustache = true))
            "nametag", "nametag_q" -> {
                k.rr(0.06f, 0.22f, 0.88f, 0.60f, 0.08f, C.RED)
                k.rr(0.10f, 0.44f, 0.80f, 0.34f, 0.04f, C.WHITE)
                k.rect(0.20f, 0.29f, 0.60f, 0.045f, C.WHITE)
                k.rect(0.30f, 0.36f, 0.40f, 0.03f, C.alpha(C.WHITE, 0.8f))
                if (name == "nametag_q") k.text("?", 0.5f, 0.61f, 0.30f, C.INK)
                else { k.arc(0.34f, 0.60f, 0.08f, 180f, 360f, 0.05f, C.INK); k.arc(0.50f, 0.60f, 0.08f, 0f, 180f, 0.05f, C.INK); k.arc(0.66f, 0.60f, 0.08f, 180f, 360f, 0.05f, C.INK) }
            }
            "speak_en" -> {
                k.rr(0.06f, 0.14f, 0.88f, 0.56f, 0.14f, C.INK2)
                k.poly(C.INK2, 0.24f, 0.66f, 0.18f, 0.90f, 0.44f, 0.68f)
                k.text("ABC", 0.5f, 0.42f, 0.30f, C.WHITE)
            }
            "confused" -> {
                face(k, C.MUSTARD)
                k.line(0.26f, 0.40f, 0.40f, 0.36f, 0.05f, C.INK); k.line(0.60f, 0.32f, 0.74f, 0.38f, 0.05f, C.INK)
                k.oval(0.31f, 0.46f, 0.08f, 0.10f, C.INK); k.oval(0.61f, 0.46f, 0.08f, 0.10f, C.INK)
                k.oval(0.44f, 0.68f, 0.12f, 0.12f, C.INK)
                k.text("?", 0.86f, 0.16f, 0.34f, C.RED)
            }
            "plane" -> {
                k.oval(0.04f, 0.10f, 0.92f, 0.80f, C.SKY)
                k.poly(C.INK2, 0.70f, 0.44f, 0.86f, 0.20f, 0.94f, 0.20f, 0.86f, 0.48f)
                k.rr(0.10f, 0.42f, 0.78f, 0.17f, 0.085f, C.WHITE)
                k.poly(C.GREY_L, 0.36f, 0.52f, 0.58f, 0.52f, 0.66f, 0.80f, 0.58f, 0.80f)
                k.poly(C.RED, 0.10f, 0.505f, 0.20f, 0.44f, 0.20f, 0.57f)
                var wx = 0.28f
                while (wx < 0.72f) { k.rect(wx, 0.46f, 0.04f, 0.04f, C.INK2); wx += 0.08f }
            }
            "gate" -> {
                k.rr(0.12f, 0.06f, 0.76f, 0.22f, 0.04f, C.INK)
                Draw.arrow(p, x + s * 0.5f, y + s * 0.17f, s * 0.16f, "up", C.MUSTARD)
                k.rect(0.20f, 0.30f, 0.60f, 0.62f, C.GREY)
                k.rect(0.25f, 0.35f, 0.235f, 0.57f, C.SKY)
                k.rect(0.515f, 0.35f, 0.235f, 0.57f, C.SKY)
                k.rect(0.40f, 0.60f, 0.05f, 0.08f, C.INK); k.rect(0.55f, 0.60f, 0.05f, 0.08f, C.INK)
            }
            "ticket" -> {
                k.rr(0.04f, 0.26f, 0.92f, 0.48f, 0.05f, 0xFFFFF4C8.toInt())
                k.rect(0.04f, 0.26f, 0.92f, 0.12f, C.INK2)
                k.line(0.68f, 0.40f, 0.68f, 0.72f, 0.025f, C.GREY)
                k.rect(0.12f, 0.46f, 0.34f, 0.05f, C.INK); k.rect(0.12f, 0.58f, 0.46f, 0.05f, C.GREY)
                k.rect(0.74f, 0.46f, 0.03f, 0.20f, C.INK); k.rect(0.79f, 0.46f, 0.05f, 0.20f, C.INK); k.rect(0.86f, 0.46f, 0.03f, 0.20f, C.INK)
                k.ring(0.04f, 0.26f, 0.92f, 0.48f, 0.05f, 0.025f, C.INK)
            }
            "passport" -> {
                k.rr(0.22f, 0.08f, 0.56f, 0.84f, 0.06f, 0xFF7A1F2B.toInt())
                k.rect(0.22f, 0.08f, 0.07f, 0.84f, 0xFF5C141E.toInt())
                k.oval(0.39f, 0.30f, 0.28f, 0.28f, C.MUSTARD)
                k.oval(0.45f, 0.36f, 0.16f, 0.16f, 0xFF7A1F2B.toInt())
                k.rect(0.38f, 0.66f, 0.30f, 0.04f, C.MUSTARD); k.rect(0.43f, 0.74f, 0.20f, 0.03f, C.MUSTARD)
                k.rect(0.38f, 0.17f, 0.30f, 0.04f, C.MUSTARD)
            }
            "loupe" -> {
                k.line(0.56f, 0.56f, 0.86f, 0.86f, 0.13f, C.BROWN)
                k.oval(0.10f, 0.10f, 0.58f, 0.58f, C.INK)
                k.oval(0.17f, 0.17f, 0.44f, 0.44f, C.SKY)
                k.arc(0.39f, 0.39f, 0.15f, 190f, 260f, 0.04f, C.WHITE)
            }
            "croissant" -> {
                val a = 0xFFD9963A.toInt()
                val b = 0xFFB87424.toInt()
                k.oval(0.06f, 0.44f, 0.26f, 0.30f, b)
                k.oval(0.68f, 0.44f, 0.26f, 0.30f, b)
                k.oval(0.18f, 0.30f, 0.30f, 0.38f, a)
                k.oval(0.52f, 0.30f, 0.30f, 0.38f, a)
                k.oval(0.33f, 0.22f, 0.34f, 0.42f, 0xFFE8AE55.toInt())
                k.line(0.42f, 0.28f, 0.40f, 0.56f, 0.03f, b); k.line(0.58f, 0.28f, 0.60f, 0.56f, 0.03f, b)
            }
            "exit" -> {
                k.rr(0.06f, 0.20f, 0.88f, 0.60f, 0.06f, C.GREEN)
                k.rect(0.60f, 0.30f, 0.22f, 0.40f, C.WHITE)
                Draw.arrow(p, x + s * 0.34f, y + s * 0.5f, s * 0.34f, "right", C.WHITE)
            }
            "wc" -> {
                k.rr(0.06f, 0.10f, 0.88f, 0.80f, 0.08f, C.INK2)
                k.oval(0.22f, 0.20f, 0.16f, 0.16f, C.WHITE); k.poly(C.WHITE, 0.30f, 0.38f, 0.16f, 0.72f, 0.44f, 0.72f)
                k.oval(0.62f, 0.20f, 0.16f, 0.16f, C.WHITE); k.rr(0.60f, 0.38f, 0.20f, 0.36f, 0.04f, C.WHITE)
                k.rect(0.49f, 0.20f, 0.02f, 0.60f, C.alpha(C.WHITE, 0.6f))
            }
            "cafe" -> {
                k.oval(0.10f, 0.70f, 0.80f, 0.18f, C.GREY_L)
                k.ring(0.62f, 0.40f, 0.26f, 0.24f, 0.10f, 0.06f, C.WHITE)
                k.rr(0.22f, 0.36f, 0.50f, 0.42f, 0.12f, C.WHITE)
                k.oval(0.26f, 0.34f, 0.42f, 0.12f, 0xFF6B4226.toInt())
                k.arc(0.36f, 0.20f, 0.06f, 90f, 270f, 0.04f, C.GREY); k.arc(0.52f, 0.20f, 0.06f, -90f, 90f, 0.04f, C.GREY)
            }
            "warning" -> {
                k.poly(C.MUSTARD, 0.50f, 0.08f, 0.94f, 0.88f, 0.06f, 0.88f)
                k.rr(0.455f, 0.34f, 0.09f, 0.30f, 0.04f, C.INK)
                k.oval(0.45f, 0.70f, 0.10f, 0.10f, C.INK)
            }
            "letter" -> {
                k.rr(0.06f, 0.22f, 0.88f, 0.58f, 0.04f, C.WHITE)
                k.poly(C.GREY_L, 0.06f, 0.24f, 0.50f, 0.58f, 0.94f, 0.24f)
                k.ring(0.06f, 0.22f, 0.88f, 0.58f, 0.04f, 0.03f, C.INK)
                k.line(0.06f, 0.24f, 0.50f, 0.58f, 0.03f, C.INK); k.line(0.94f, 0.24f, 0.50f, 0.58f, 0.03f, C.INK)
                k.rect(0.74f, 0.28f, 0.14f, 0.16f, C.RED)
            }
            else -> if (!MoreIcons.draw(k, name)) {
                // unknown name: a plain speech bubble, so a typo in a unit file is harmless
                k.rr(0.10f, 0.16f, 0.80f, 0.52f, 0.14f, C.GREY_L)
                k.poly(C.GREY_L, 0.26f, 0.64f, 0.20f, 0.88f, 0.46f, 0.66f)
            }
        }
    }

    val ALL = arrayOf(
        "speaker", "mic", "slow", "book", "menu", "lock", "back", "home", "next", "star", "check", "cross", "talk", "walk", "hand", "bag",
        "wave", "bye", "please", "thanks", "sorry", "yes", "no", "lady", "man", "nametag", "nametag_q", "speak_en",
        "confused", "plane", "gate", "ticket", "passport", "loupe", "croissant", "exit", "wc", "cafe", "warning", "letter"
    )

    /** One of every picture, for the art sheet. */
    fun samples(): List<String> = ALL.toList() + MoreIcons.samples()

    /** True if [name] is an icon this game can draw. The check uses it to catch typos in unit files. */
    fun known(name: String): Boolean = ALL.contains(name) || MoreIcons.knows(name)

    private fun hand(k: K) {
        k.rr(0.30f, 0.44f, 0.40f, 0.44f, 0.12f, SKIN)
        k.rr(0.30f, 0.20f, 0.085f, 0.36f, 0.04f, SKIN)
        k.rr(0.405f, 0.12f, 0.085f, 0.44f, 0.04f, SKIN)
        k.rr(0.51f, 0.14f, 0.085f, 0.42f, 0.04f, SKIN)
        k.rr(0.615f, 0.22f, 0.085f, 0.34f, 0.04f, SKIN)
        k.poly(SKIN_D, 0.30f, 0.60f, 0.14f, 0.46f, 0.20f, 0.40f, 0.36f, 0.50f)
        k.rr(0.32f, 0.84f, 0.36f, 0.12f, 0.03f, C.INK2)
    }

    private fun face(k: K, c: Int) {
        k.oval(0.10f, 0.14f, 0.80f, 0.80f, c)
    }

    private fun star(k: K, cx: Float, cy: Float, r: Float, c: Int) {
        val pts = FloatArray(20)
        for (i in 0 until 10) {
            val rr = if (i % 2 == 0) r else r * 0.42f
            val a = Math.toRadians((-90 + i * 36).toDouble())
            pts[i * 2] = cx + rr * Math.cos(a).toFloat()
            pts[i * 2 + 1] = cy + rr * Math.sin(a).toFloat()
        }
        k.poly(c, *pts)
    }

    private fun faceImg(p: Painter, x: Float, y: Float, s: Float, id: String, spec: FaceSpec) {
        val img = Faces.get(id, spec, "smile")
        val w = s * Faces.W / Faces.H
        p.image(img, x + (s - w) / 2f, y, w, s, false, 1f)
    }
}
