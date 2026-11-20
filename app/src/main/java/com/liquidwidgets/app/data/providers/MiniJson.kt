package com.liquidwidgets.app.data.providers

/**
 * Minimal JSON parser for the Open-Meteo response shape.
 *
 * Why not org.json? JSONObject is an Android framework class: in local unit
 * tests it throws RuntimeException("Stub!"), so the weather parser could
 * never be tested on the JVM. This tiny recursive-descent parser is pure
 * Kotlin, has no Android dependency, and only needs to read the handful of
 * fields WeatherDataProvider uses. It is intentionally strict about syntax
 * and returns null for malformed input.
 */
internal sealed interface JsonVal
internal data class JObj(val map: Map<String, JsonVal>) : JsonVal {
    fun obj(key: String): JObj? = map[key] as? JObj
    fun arr(key: String): JArr? = map[key] as? JArr
    fun num(key: String): Double? = (map[key] as? JNum)?.v
    fun str(key: String): String? = (map[key] as? JStr)?.v
}
internal data class JArr(val list: List<JsonVal>) : JsonVal {
    fun numAt(i: Int): Double? = (list.getOrNull(i) as? JNum)?.v
    fun strAt(i: Int): String? = (list.getOrNull(i) as? JStr)?.v
    val size: Int get() = list.size
}
internal data class JStr(val v: String) : JsonVal
internal data class JNum(val v: Double) : JsonVal
internal data class JBool(val v: Boolean) : JsonVal
internal object JNull : JsonVal

internal object MiniJson {
    fun parse(raw: String): JsonVal? = runCatching {
        val p = Parser(raw)
        p.skipWs()
        val v = p.value()
        p.skipWs()
        if (p.pos != raw.length) null else v
    }.getOrNull()

    private class Parser(private val s: String) {
        var pos = 0

        fun skipWs() {
            while (pos < s.length && s[pos].isWhitespace()) pos++
        }

        fun value(): JsonVal {
            skipWs()
            require(pos < s.length) { "unexpected end of input" }
            return when (val c = s[pos]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> JStr(string())
                't' -> { expect("true"); JBool(true) }
                'f' -> { expect("false"); JBool(false) }
                'n' -> { expect("null"); JNull }
                else -> {
                    require(c == '-' || c.isDigit()) { "unexpected char '$c' at $pos" }
                    num()
                }
            }
        }

        private fun expect(word: String) {
            require(s.startsWith(word, pos)) { "expected $word at $pos" }
            pos += word.length
        }

        private fun obj(): JObj {
            pos++ // {
            val map = LinkedHashMap<String, JsonVal>()
            skipWs()
            if (pos < s.length && s[pos] == '}') { pos++; return JObj(map) }
            while (true) {
                skipWs()
                val key = string()
                skipWs()
                require(pos < s.length && s[pos] == ':') { "expected ':' at $pos" }
                pos++
                map[key] = value()
                skipWs()
                when {
                    pos < s.length && s[pos] == ',' -> { pos++; continue }
                    pos < s.length && s[pos] == '}' -> { pos++; return JObj(map) }
                    else -> error("expected ',' or '}' at $pos")
                }
            }
        }

        private fun arr(): JArr {
            pos++ // [
            val list = ArrayList<JsonVal>()
            skipWs()
            if (pos < s.length && s[pos] == ']') { pos++; return JArr(list) }
            while (true) {
                list.add(value())
                skipWs()
                when {
                    pos < s.length && s[pos] == ',' -> { pos++; continue }
                    pos < s.length && s[pos] == ']' -> { pos++; return JArr(list) }
                    else -> error("expected ',' or ']' at $pos")
                }
            }
        }

        private fun string(): String {
            require(pos < s.length && s[pos] == '"') { "expected string at $pos" }
            pos++
            val out = StringBuilder()
            while (pos < s.length) {
                val c = s[pos++]
                when (c) {
                    '"' -> return out.toString()
                    '\\' -> {
                        require(pos < s.length) { "dangling escape" }
                        when (val e = s[pos++]) {
                            '"' -> out.append('"')
                            '\\' -> out.append('\\')
                            '/' -> out.append('/')
                            'b' -> out.append('\b')
                            'f' -> out.append('\u000C')
                            'n' -> out.append('\n')
                            'r' -> out.append('\r')
                            't' -> out.append('\t')
                            'u' -> {
                                val hex = s.substring(pos, (pos + 4).coerceAtMost(s.length))
                                out.append(hex.toInt(16).toChar())
                                pos += 4
                            }
                            else -> error("bad escape \\$e")
                        }
                    }
                    else -> out.append(c)
                }
            }
            error("unterminated string")
        }

        private fun num(): JNum {
            val start = pos
            if (pos < s.length && s[pos] == '-') pos++
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.' || s[pos] == 'e' || s[pos] == 'E' || s[pos] == '+' || s[pos] == '-')) pos++
            return JNum(s.substring(start, pos).toDouble())
        }
    }
}
