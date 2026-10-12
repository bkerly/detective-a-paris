package detective.desktop

import detective.core.Align
import detective.core.C
import detective.core.Font
import detective.core.Icons
import detective.core.Text
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Draws the launcher icon at each size Android wants and saves the PNG files
 * into app/src/main/res. Run it again if you change the design:
 *
 *   AppIcon <assets folder> <res folder>
 */
object AppIcon {
    @JvmStatic
    fun main(args: Array<String>) {
        val p = AwtPainter(Files(File(args[0])))
        val sizes = linkedMapOf("mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192)
        for ((name, px) in sizes) {
            val img = BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB)
            val g = img.createGraphics()
            p.begin(g, px / 100.0, 0.0, 0.0)
            g.clip = null
            // the drawing is made on a 100 x 100 square
            p.fillRoundRect(0f, 0f, 100f, 100f, 22f, C.INK)
            p.fillOval(60f, 8f, 26f, 26f, 0xFFFDF3C4.toInt())
            p.fillOval(68f, 5f, 24f, 24f, C.INK)
            Icons.draw(p, "loupe", 12f, 16f, 76f)
            p.fillOval(27f, 31f, 30f, 30f, C.PAPER)
            Text.line(p, "é", 42f, 45f, 30f, C.INK, Font.DISPLAY, Align.CENTER)
            g.dispose()
            val dir = File(args[1], "mipmap-$name")
            dir.mkdirs()
            ImageIO.write(img, "png", File(dir, "ic_launcher.png"))
        }
        println("wrote ${sizes.size} icons")
    }
}
