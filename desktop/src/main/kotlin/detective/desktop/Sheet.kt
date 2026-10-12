package detective.desktop

import detective.core.Art
import detective.core.C
import detective.core.FaceSpec
import detective.core.Faces
import detective.core.Font
import detective.core.Game
import detective.core.Icons
import detective.core.Text
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** Draws every sprite, icon and face on one picture, for checking the art. */
object Sheet {
    @JvmStatic
    fun main(args: Array<String>) {
        val files = Files(File(args[0]))
        val art = Art(FileAssets(files).text("art/sprites.txt") ?: "")
        val p = AwtPainter(files)
        val scale = 3.0
        val img = BufferedImage((Game.W * scale).toInt(), (Game.H * scale * 8).toInt(), BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.color = java.awt.Color(0xF8F0DC)
        g.fillRect(0, 0, img.width, img.height)
        p.begin(g, scale, 0.0, 0.0)
        g.clip = null
        var x = 4f
        var y = 4f
        var rowH = 0f
        for (n in art.names()) {
            val s = art.get(n)
            if (x + s.w + 4f > Game.W) { x = 4f; y += rowH + 12f; rowH = 0f }
            p.image(s, x, y, s.w.toFloat(), s.h.toFloat(), false, 1f)
            Text.line(p, n.removePrefix("tile_"), x, y + s.h + 4f, 3.4f, C.INK, Font.TEXT)
            x += Math.max(s.w.toFloat(), 22f) + 5f
            rowH = Math.max(rowH, s.h.toFloat())
        }
        y += rowH + 14f
        x = 4f
        for (n in Icons.samples()) {
            if (x + 34f > Game.W) { x = 4f; y += 42f }
            Icons.draw(p, n, x, y, 30f)
            Text.line(p, n.substringAfter("prep:"), x, y + 34f, 3.6f, C.INK, Font.TEXT)
            x += 36f
        }
        y += 46f
        x = 4f
        val specs = listOf(
            FaceSpec(hair = 0xFF7A3E1D.toInt(), hairStyle = "bun", scarf = 0xFFD9534F.toInt()),
            FaceSpec(hair = 0xFF33302E.toInt(), hat = "cap", moustache = true),
            FaceSpec(hair = 0xFFE8C55A.toInt(), hairStyle = "long", clothes = 0xFF7C3F8C.toInt(), glasses = true),
            FaceSpec(hair = 0xFF22223B.toInt(), hat = "beret", hatColor = 0xFF22223B.toInt(), clothes = 0xFFFFFFFF.toInt(), moustache = true),
            FaceSpec(hairStyle = "bald", hat = "fedora", hatColor = 0xFF8A5A2B.toInt(), clothes = 0xFFC9A46A.toInt()),
            FaceSpec(skin = 0xFF9AA5C0.toInt(), kind = "mouse", hat = "bonnet", hatColor = 0xFFD9534F.toInt(), clothes = 0xFF3F6DB5.toInt()),
            FaceSpec(skin = 0xFF9AA5C0.toInt(), kind = "mouse", hat = "crown", hairStyle = "long", hair = 0xFFEDEDED.toInt(), clothes = 0xFFF3B9B5.toInt())
        )
        for ((i, sp) in specs.withIndex()) for (m in listOf("neutral", "smile", "cold", "confused", "busy", "talk", "angry", "sad", "scared", "shy", "curious")) {
            if (x + 46f > Game.W) { x = 4f; y += 54f }
            p.image(Faces.get("s$i", sp, m), x, y, 44f, 48f, false, 1f)
            x += 48f
        }
        g.dispose()
        ImageIO.write(img, "png", File(args[1]))
        println("wrote ${args[1]}")
    }
}
