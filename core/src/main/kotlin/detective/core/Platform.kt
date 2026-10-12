package detective.core

/**
 * Everything the game needs from the device. The Android app and the desktop
 * harness each supply these four things; all game logic lives in this module
 * and never touches Android classes.
 */

/** A small picture as raw ARGB pixels. The platform keeps its own bitmap in [handle]. */
class Img(val w: Int, val h: Int, val px: IntArray) {
    @JvmField var handle: Any? = null
}

object Font {
    const val TEXT = 0     // Atkinson Hyperlegible Regular
    const val BOLD = 1     // Atkinson Hyperlegible Bold
    const val DISPLAY = 2  // Lilita One
    const val HAND = 3     // Patrick Hand
    val FILES = arrayOf("fonts/text.ttf", "fonts/bold.ttf", "fonts/display.ttf", "fonts/hand.ttf")
}

object Align {
    const val LEFT = 0
    const val CENTER = 1
    const val RIGHT = 2
}

/** Draws in game units. The screen is always 400 x 240 units; the platform scales it. */
interface Painter {
    fun fillRect(x: Float, y: Float, w: Float, h: Float, color: Int)
    fun fillRoundRect(x: Float, y: Float, w: Float, h: Float, r: Float, color: Int)
    fun strokeRoundRect(x: Float, y: Float, w: Float, h: Float, r: Float, width: Float, color: Int)
    fun fillOval(x: Float, y: Float, w: Float, h: Float, color: Int)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Int)
    /** [pts] is x0, y0, x1, y1, ... */
    fun fillPoly(pts: FloatArray, color: Int)
    fun text(s: String, x: Float, baseline: Float, size: Float, color: Int, font: Int, align: Int)
    fun textWidth(s: String, size: Float, font: Int): Float
    fun image(img: Img, x: Float, y: Float, w: Float, h: Float, flipX: Boolean, alpha: Float)
    fun pushClip(x: Float, y: Float, w: Float, h: Float)
    fun popClip()
}

interface AudioOut {
    /** Play one voice clip, e.g. "audio/siwis_bonjour". The platform adds the file extension. */
    fun voice(path: String)
    fun stopVoice()
    /** Play a short sound effect, e.g. "ok". */
    fun sfx(name: String)
}

interface Store {
    fun load(key: String): String?
    fun save(key: String, value: String)
}

interface Assets {
    /** Read a text file from the assets folder, or null if it is missing. */
    fun text(path: String): String?
}
