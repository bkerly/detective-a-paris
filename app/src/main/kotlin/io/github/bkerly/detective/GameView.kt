package io.github.bkerly.detective

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import detective.core.Game

/**
 * Draws the game and passes touches to it. The game always thinks the screen
 * is 400 x 240; this view scales that up to fit and centres it, leaving black
 * bars if the tablet has a different shape.
 */
class GameView(context: Context) : View(context) {
    private val audio = AndroidAudio(context)
    private val game = Game(AndroidAssets(context), audio, AndroidStore(context))
    private val painter = AndroidPainter(context)
    private var last = 0L
    private var running = false
    private var scale = 1f
    private var offX = 0f
    private var offY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scale = Math.min(w / Game.W, h / Game.H)
        offX = (w - Game.W * scale) / 2f
        offY = (h - Game.H * scale) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.uptimeMillis()
        val dt = if (last == 0L) 0f else (now - last) / 1000f
        last = now
        game.update(dt)

        canvas.drawColor(Color.BLACK)
        canvas.save()
        canvas.translate(offX, offY)
        canvas.scale(scale, scale)
        canvas.clipRect(0f, 0f, Game.W, Game.H)
        painter.canvas = canvas
        game.draw(painter)
        canvas.restore()

        // ask to be drawn again on the next frame
        if (running) postInvalidateOnAnimation()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = (event.x - offX) / scale
        val y = (event.y - offY) / scale
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> game.down(x, y)
            MotionEvent.ACTION_MOVE -> game.move(x, y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> game.up(x, y)
        }
        return true
    }

    fun resume() {
        running = true
        last = 0L
        invalidate()
    }

    fun pause() {
        running = false
        game.pause()
    }

    fun back(): Boolean = game.back()

    fun release() {
        audio.release()
    }
}
