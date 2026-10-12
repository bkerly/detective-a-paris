package detective.desktop

import detective.core.Align
import detective.core.Assets
import detective.core.AudioOut
import detective.core.Font as F
import detective.core.Game
import detective.core.Img
import detective.core.Painter
import detective.core.Store
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

/**
 * Where game files come from: a folder on disk (the Android app's assets
 * folder) or, in the packed jar, the "assets/" folder inside the jar.
 */
class Files(private val root: File?) {
    fun open(path: String): InputStream? {
        if (root != null) {
            val f = File(root, path)
            return if (f.isFile) f.inputStream() else null
        }
        return Files::class.java.classLoader.getResourceAsStream("assets/$path")
    }
}

class FileAssets(private val files: Files) : Assets {
    override fun text(path: String): String? {
        val s = files.open(path) ?: return null
        return s.use { String(it.readBytes(), Charsets.UTF_8) }
    }
}

/** Keeps the save in memory, and in a file if one is given. */
class FileStore(private val file: File?) : Store {
    private val mem = HashMap<String, String>()
    override fun load(key: String): String? {
        mem[key]?.let { return it }
        val f = file ?: return null
        return if (f.isFile) f.readText(Charsets.UTF_8) else null
    }
    override fun save(key: String, value: String) {
        mem[key] = value
        file?.writeText(value, Charsets.UTF_8)
    }
}

/** Plays .wav files if they exist; records every request either way. */
class DesktopAudio(private val files: Files?) : AudioOut {
    val log = ArrayList<String>()
    private var clip: Clip? = null

    override fun voice(path: String) {
        log.add(path)
        stopVoice()
        play(path)?.let { clip = it }
    }

    override fun stopVoice() {
        try { clip?.stop(); clip?.close() } catch (e: Exception) { }
        clip = null
    }

    override fun sfx(name: String) { play("sfx/$name") }

    private fun play(path: String): Clip? {
        val r = files ?: return null
        val src = r.open("$path.wav") ?: return null
        return try {
            val c = AudioSystem.getClip()
            c.open(pcm(AudioSystem.getAudioInputStream(BufferedInputStream(src))))
            c.start()
            c
        } catch (e: Exception) { null }
    }
}

/** Sound files packed small (u-law) are unpacked to plain samples before they are played. */
fun pcm(input: javax.sound.sampled.AudioInputStream): javax.sound.sampled.AudioInputStream =
    if (input.format.encoding == javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED) input
    else AudioSystem.getAudioInputStream(javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED, input)

/** Draws the game with Java2D. The Android painter does the same job with android.graphics. */
class AwtPainter(private val files: Files) : Painter {
    lateinit var g: Graphics2D
    private val fonts = arrayOfNulls<Font>(F.FILES.size)
    private val clips = ArrayList<Shape?>()

    fun begin(g2: Graphics2D, scale: Double, offX: Double, offY: Double) {
        g = g2
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        g.translate(offX, offY)
        g.scale(scale, scale)
        g.clip(Rectangle2D.Float(0f, 0f, Game.W, Game.H))
        clips.clear()
    }

    private fun col(c: Int) = Color(c, true)

    private fun font(i: Int, size: Float): Font {
        var f = fonts[i]
        if (f == null) {
            val src = files.open(F.FILES[i]) ?: throw IllegalStateException("missing font " + F.FILES[i])
            f = src.use { Font.createFont(Font.TRUETYPE_FONT, it) }
            fonts[i] = f
        }
        return f!!.deriveFont(size)
    }

    override fun fillRect(x: Float, y: Float, w: Float, h: Float, color: Int) {
        g.color = col(color); g.fill(Rectangle2D.Float(x, y, w, h))
    }

    override fun fillRoundRect(x: Float, y: Float, w: Float, h: Float, r: Float, color: Int) {
        g.color = col(color); g.fill(RoundRectangle2D.Float(x, y, w, h, r * 2, r * 2))
    }

    override fun strokeRoundRect(x: Float, y: Float, w: Float, h: Float, r: Float, width: Float, color: Int) {
        g.color = col(color); g.stroke = BasicStroke(width)
        g.draw(RoundRectangle2D.Float(x, y, w, h, r * 2, r * 2))
    }

    override fun fillOval(x: Float, y: Float, w: Float, h: Float, color: Int) {
        g.color = col(color); g.fill(Ellipse2D.Float(x, y, w, h))
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Int) {
        g.color = col(color); g.stroke = BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(Line2D.Float(x1, y1, x2, y2))
    }

    override fun fillPoly(pts: FloatArray, color: Int) {
        if (pts.size < 6) return
        val p = Path2D.Float()
        p.moveTo(pts[0], pts[1])
        var i = 2
        while (i < pts.size) { p.lineTo(pts[i], pts[i + 1]); i += 2 }
        p.closePath()
        g.color = col(color); g.fill(p)
    }

    /** Unbreakable spaces only matter for line wrapping; draw them as plain spaces. */
    private fun plain(s: String): String = if (s.indexOf('\u00A0') >= 0) s.replace('\u00A0', ' ') else s

    override fun text(s0: String, x: Float, baseline: Float, size: Float, color: Int, font: Int, align: Int) {
        val s = plain(s0)
        g.font = font(font, size)
        g.color = col(color)
        var tx = x
        if (align != Align.LEFT) {
            val w = g.fontMetrics.getStringBounds(s, g).width.toFloat()
            tx = if (align == Align.CENTER) x - w / 2f else x - w
        }
        g.drawString(s, tx, baseline)
    }

    override fun textWidth(s: String, size: Float, font: Int): Float {
        g.font = font(font, size)
        return g.fontMetrics.getStringBounds(plain(s), g).width.toFloat()
    }

    override fun image(img: Img, x: Float, y: Float, w: Float, h: Float, flipX: Boolean, alpha: Float) {
        var bi = img.handle as? BufferedImage
        if (bi == null) {
            bi = BufferedImage(img.w, img.h, BufferedImage.TYPE_INT_ARGB)
            bi.setRGB(0, 0, img.w, img.h, img.px, 0, img.w)
            img.handle = bi
        }
        val old = g.composite
        if (alpha < 1f) g.composite = java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha)
        val at = java.awt.geom.AffineTransform()
        if (flipX) { at.translate((x + w).toDouble(), y.toDouble()); at.scale(-w / img.w.toDouble(), h / img.h.toDouble()) }
        else { at.translate(x.toDouble(), y.toDouble()); at.scale(w / img.w.toDouble(), h / img.h.toDouble()) }
        g.drawImage(bi, at, null)
        g.composite = old
    }

    override fun pushClip(x: Float, y: Float, w: Float, h: Float) {
        clips.add(g.clip)
        g.clip(Rectangle2D.Float(x, y, w, h))
    }

    override fun popClip() {
        if (clips.isNotEmpty()) g.clip = clips.removeAt(clips.size - 1)
    }
}
