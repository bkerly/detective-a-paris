package detective.desktop

import detective.core.Game
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * Plays the game in a window on a computer. The mouse is the finger; the arrow
 * keys also walk in action scenes. Run with the assets folder as the argument,
 * or with no argument from the packed jar.
 */
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val dir = if (args.isNotEmpty()) File(args[0]) else null
        val files = Files(dir)
        val wavs = if (args.size > 1) Files(File(args[1])) else files
        val saveFile = File(System.getProperty("user.home"), ".detective-paris-save.json")
        val audio = DesktopAudio(wavs)
        val game = Game(FileAssets(files), audio, FileStore(saveFile))
        val painter = AwtPainter(files)

        SwingUtilities.invokeLater {
            val keys = HashSet<Int>()
            var last = System.nanoTime()
            val panel = object : JPanel() {
                fun scale() = Math.min(width / Game.W.toDouble(), height / Game.H.toDouble())
                fun offX() = (width - Game.W * scale()) / 2.0
                fun offY() = (height - Game.H * scale()) / 2.0
                override fun paintComponent(g: Graphics) {
                    g.color = Color.BLACK
                    g.fillRect(0, 0, width, height)
                    val g2 = g.create() as Graphics2D
                    painter.begin(g2, scale(), offX(), offY())
                    game.draw(painter)
                    g2.dispose()
                }
            }
            panel.preferredSize = Dimension(1024, 600)
            val mouse = object : MouseAdapter() {
                fun gx(e: MouseEvent) = ((e.x - panel.offX()) / panel.scale()).toFloat()
                fun gy(e: MouseEvent) = ((e.y - panel.offY()) / panel.scale()).toFloat()
                override fun mousePressed(e: MouseEvent) = game.down(gx(e), gy(e))
                override fun mouseDragged(e: MouseEvent) = game.move(gx(e), gy(e))
                override fun mouseReleased(e: MouseEvent) = game.up(gx(e), gy(e))
            }
            panel.addMouseListener(mouse)
            panel.addMouseMotionListener(mouse)
            val frame = JFrame("Détective à Paris")
            fun sendKeys() {
                val dx = (if (keys.contains(KeyEvent.VK_RIGHT)) 1 else 0) - (if (keys.contains(KeyEvent.VK_LEFT)) 1 else 0)
                val dy = (if (keys.contains(KeyEvent.VK_DOWN)) 1 else 0) - (if (keys.contains(KeyEvent.VK_UP)) 1 else 0)
                game.keys(dx, dy)
            }
            frame.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_ESCAPE) { if (!game.back()) frame.dispatchEvent(WindowEvent(frame, WindowEvent.WINDOW_CLOSING)) }
                    else { keys.add(e.keyCode); sendKeys() }
                }
                override fun keyReleased(e: KeyEvent) { keys.remove(e.keyCode); sendKeys() }
            })
            frame.addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent) { game.pause() }
            })
            frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            frame.contentPane.add(panel)
            frame.pack()
            frame.setLocationRelativeTo(null)
            frame.isVisible = true
            Timer(16) {
                val now = System.nanoTime()
                game.update((now - last) / 1e9f)
                last = now
                panel.repaint()
            }.start()
        }
    }
}
