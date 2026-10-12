package detective.desktop

import detective.core.Game
import java.io.File

/**
 * Lists every French line in every unit with the voice that says it, one per
 * row as "voice<TAB>text". tools/make_audio.R reads this list, so the game
 * itself decides which sound files are needed.
 *
 *   Lines <assets folder> <output file>
 */
object Lines {
    @JvmStatic
    fun main(args: Array<String>) {
        val files = Files(File(args[0]))
        val game = Game(FileAssets(files), DesktopAudio(null), FileStore(null))
        val seen = LinkedHashSet<String>()
        for (info in game.unitList) {
            val u = game.unitById(info.id)
            for (sp in u.spoken) {
                if (sp.say.contains("{")) throw IllegalStateException("${u.id}: this line has a {placeholder} and no \"say\" text: ${sp.say}")
                if (sp.say.isBlank()) continue
                seen.add(u.voiceFor(sp.who) + "\t" + sp.say)
            }
        }
        val out = File(args[1])
        out.parentFile?.mkdirs()
        out.writeText(seen.joinToString("\n") + "\n", Charsets.UTF_8)
        println("${seen.size} lines in ${game.unitList.size} units -> ${out.path}")
    }
}
