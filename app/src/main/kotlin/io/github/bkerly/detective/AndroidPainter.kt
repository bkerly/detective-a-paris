package io.github.bkerly.detective

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import detective.core.Align
import detective.core.Font
import detective.core.Img
import detective.core.Painter

/** Draws the game's shapes, text and pixel art on an Android canvas. */
class AndroidPainter(context: Context) : Painter {
    lateinit var canvas: Canvas

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG)
    /** No smoothing, so pixel art stays sharp when it is scaled up. */
    private val pixels = Paint()
    private val fonts = Array(Font.FILES.size) { Typeface.createFromAsset(context.assets, Font.FILES[it]) }
    private val rect = RectF()
    private val path = Path()

    override fun fillRect(x: Float, y: Float, w: Float, h: Float, color: Int) {
        fill.color = color
        canvas.drawRect(x, y, x + w, y + h, fill)
    }

    override fun fillRoundRect(x: Float, y: Float, w: Float, h: Float, r: Float, color: Int) {
        fill.color = color
        rect.set(x, y, x + w, y + h)
        canvas.drawRoundRect(rect, r, r, fill)
    }

    override fun strokeRoundRect(x: Float, y: Float, w: Float, h: Float, r: Float, width: Float, color: Int) {
        stroke.color = color
        stroke.strokeWidth = width
        stroke.strokeCap = Paint.Cap.BUTT
        rect.set(x, y, x + w, y + h)
        canvas.drawRoundRect(rect, r, r, stroke)
    }

    override fun fillOval(x: Float, y: Float, w: Float, h: Float, color: Int) {
        fill.color = color
        rect.set(x, y, x + w, y + h)
        canvas.drawOval(rect, fill)
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Int) {
        stroke.color = color
        stroke.strokeWidth = width
        stroke.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(x1, y1, x2, y2, stroke)
    }

    override fun fillPoly(pts: FloatArray, color: Int) {
        if (pts.size < 6) return
        path.rewind()
        path.moveTo(pts[0], pts[1])
        var i = 2
        while (i < pts.size) {
            path.lineTo(pts[i], pts[i + 1])
            i += 2
        }
        path.close()
        fill.color = color
        canvas.drawPath(path, fill)
    }

    override fun text(s: String, x: Float, baseline: Float, size: Float, color: Int, font: Int, align: Int) {
        text.typeface = fonts[font]
        text.textSize = size
        text.color = color
        text.textAlign = when (align) {
            Align.CENTER -> Paint.Align.CENTER
            Align.RIGHT -> Paint.Align.RIGHT
            else -> Paint.Align.LEFT
        }
        canvas.drawText(plain(s), x, baseline, text)
    }

    override fun textWidth(s: String, size: Float, font: Int): Float {
        text.typeface = fonts[font]
        text.textSize = size
        return text.measureText(plain(s))
    }

    /**
     * The game marks some spaces as unbreakable so French punctuation stays on
     * its line. Not every font has that character, so draw it as a plain space.
     */
    private fun plain(s: String): String = if (s.indexOf('\u00A0') >= 0) s.replace('\u00A0', ' ') else s

    override fun image(img: Img, x: Float, y: Float, w: Float, h: Float, flipX: Boolean, alpha: Float) {
        var bmp = img.handle as? Bitmap
        if (bmp == null) {
            bmp = Bitmap.createBitmap(img.px, img.w, img.h, Bitmap.Config.ARGB_8888)
            img.handle = bmp
        }
        pixels.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
        rect.set(x, y, x + w, y + h)
        if (flipX) {
            canvas.save()
            canvas.scale(-1f, 1f, x + w / 2f, y)
            canvas.drawBitmap(bmp!!, null, rect, pixels)
            canvas.restore()
        } else {
            canvas.drawBitmap(bmp!!, null, rect, pixels)
        }
    }

    override fun pushClip(x: Float, y: Float, w: Float, h: Float) {
        canvas.save()
        canvas.clipRect(x, y, x + w, y + h)
    }

    override fun popClip() {
        canvas.restore()
    }
}
