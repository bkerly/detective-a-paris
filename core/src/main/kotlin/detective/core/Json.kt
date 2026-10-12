package detective.core

/**
 * A small JSON reader and writer with no dependencies, so the same code runs
 * on Android and on the desktop. Objects become LinkedHashMap, arrays become
 * ArrayList, numbers become Double.
 */
object Json {
    fun parse(src: String): J = J(Parser(src).parseTop())

    fun write(v: Any?): String {
        val sb = StringBuilder()
        writeTo(sb, v)
        return sb.toString()
    }

    private fun writeTo(sb: StringBuilder, v: Any?) {
        when (v) {
            null -> sb.append("null")
            is J -> writeTo(sb, v.v)
            is String -> writeString(sb, v)
            is Boolean -> sb.append(if (v) "true" else "false")
            is Int -> sb.append(v)
            is Long -> sb.append(v)
            is Number -> {
                val d = v.toDouble()
                if (d == Math.floor(d) && Math.abs(d) < 1e15) sb.append(d.toLong()) else sb.append(d)
            }
            is Map<*, *> -> {
                sb.append('{')
                var first = true
                for (e in v.entries) {
                    if (!first) sb.append(',')
                    first = false
                    writeString(sb, e.key.toString())
                    sb.append(':')
                    writeTo(sb, e.value)
                }
                sb.append('}')
            }
            is Iterable<*> -> {
                sb.append('[')
                var first = true
                for (e in v) {
                    if (!first) sb.append(',')
                    first = false
                    writeTo(sb, e)
                }
                sb.append(']')
            }
            else -> writeString(sb, v.toString())
        }
    }

    private fun writeString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c < ' ') sb.append("\\u").append(String.format("%04x", c.code)) else sb.append(c)
            }
        }
        sb.append('"')
    }

    private class Parser(val s: String) {
        var i = 0

        fun parseTop(): Any? {
            val v = value()
            ws()
            if (i < s.length) fail("extra text after the end")
            return v
        }

        fun fail(msg: String): Nothing {
            var line = 1
            for (k in 0 until minOf(i, s.length)) if (s[k] == '\n') line++
            throw IllegalArgumentException("JSON error on line $line: $msg")
        }

        fun ws() {
            while (i < s.length && (s[i] == ' ' || s[i] == '\n' || s[i] == '\r' || s[i] == '\t')) i++
        }

        fun value(): Any? {
            ws()
            if (i >= s.length) fail("unexpected end")
            val c = s[i]
            return when {
                c == '{' -> obj()
                c == '[' -> arr()
                c == '"' -> str()
                c == 't' -> word("true", true)
                c == 'f' -> word("false", false)
                c == 'n' -> word("null", null)
                c == '-' || (c in '0'..'9') -> num()
                else -> fail("unexpected '$c'")
            }
        }

        fun word(w: String, v: Any?): Any? {
            if (!s.startsWith(w, i)) fail("expected $w")
            i += w.length
            return v
        }

        fun num(): Double {
            val st = i
            while (i < s.length && (s[i] in '0'..'9' || s[i] == '-' || s[i] == '+' || s[i] == '.' || s[i] == 'e' || s[i] == 'E')) i++
            return s.substring(st, i).toDoubleOrNull() ?: fail("bad number")
        }

        fun str(): String {
            i++ // opening quote
            val sb = StringBuilder()
            while (true) {
                if (i >= s.length) fail("string never ends")
                val c = s[i++]
                if (c == '"') break
                if (c == '\\') {
                    if (i >= s.length) fail("string never ends")
                    when (val e = s[i++]) {
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        'r' -> sb.append('\r')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'u' -> {
                            if (i + 4 > s.length) fail("bad \\u escape")
                            sb.append(s.substring(i, i + 4).toInt(16).toChar())
                            i += 4
                        }
                        else -> sb.append(e)
                    }
                } else sb.append(c)
            }
            return sb.toString()
        }

        fun arr(): ArrayList<Any?> {
            i++
            val out = ArrayList<Any?>()
            ws()
            if (i < s.length && s[i] == ']') { i++; return out }
            while (true) {
                out.add(value())
                ws()
                if (i >= s.length) fail("list never ends")
                val c = s[i++]
                if (c == ']') break
                if (c != ',') fail("expected , or ] in list")
            }
            return out
        }

        fun obj(): LinkedHashMap<String, Any?> {
            i++
            val out = LinkedHashMap<String, Any?>()
            ws()
            if (i < s.length && s[i] == '}') { i++; return out }
            while (true) {
                ws()
                if (i >= s.length || s[i] != '"') fail("expected a key in quotes")
                val k = str()
                ws()
                if (i >= s.length || s[i] != ':') fail("expected : after key \"$k\"")
                i++
                out[k] = value()
                ws()
                if (i >= s.length) fail("object never ends")
                val c = s[i++]
                if (c == '}') break
                if (c != ',') fail("expected , or } after \"$k\"")
            }
            return out
        }
    }
}

/** A read-only view of a parsed JSON value with forgiving getters. */
class J(val v: Any?) {
    val isNull: Boolean get() = v == null

    operator fun get(key: String): J = J((v as? Map<*, *>)?.get(key))
    fun has(key: String): Boolean = (v as? Map<*, *>)?.containsKey(key) == true

    fun str(key: String, def: String = ""): String = (this[key].v as? String) ?: def
    fun strOrNull(key: String): String? = this[key].v as? String
    fun int(key: String, def: Int = 0): Int = (this[key].v as? Number)?.toInt() ?: def
    fun num(key: String, def: Float = 0f): Float = (this[key].v as? Number)?.toFloat() ?: def
    fun bool(key: String, def: Boolean = false): Boolean = (this[key].v as? Boolean) ?: def

    fun list(): List<J> {
        val l = v as? List<*> ?: return emptyList()
        val out = ArrayList<J>(l.size)
        for (e in l) out.add(J(e))
        return out
    }

    fun list(key: String): List<J> = this[key].list()

    fun strings(key: String): List<String> {
        val out = ArrayList<String>()
        for (e in list(key)) out.add(e.v?.toString() ?: "")
        return out
    }

    fun keys(): List<String> {
        val m = v as? Map<*, *> ?: return emptyList()
        val out = ArrayList<String>()
        for (k in m.keys) out.add(k.toString())
        return out
    }

    fun asString(def: String = ""): String = (v as? String) ?: def
    fun asInt(def: Int = 0): Int = (v as? Number)?.toInt() ?: def
    fun asFloat(def: Float = 0f): Float = (v as? Number)?.toFloat() ?: def
}
