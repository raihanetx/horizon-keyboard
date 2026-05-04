package com.horizon.keyboard

/**
 * Processes raw voice input text into keyboard actions.
 * Handles voice commands like "enter", "backspace", "space", arrow keys,
 * as well as punctuation words ("dot" → ".", "comma" → ",").
 */
object VoiceCommandProcessor {

    sealed class Action {
        data class Text(val value: String) : Action()
        object Backspace : Action()
        object Enter : Action()
        object Space : Action()
        object Escape : Action()
        object ArrowUp : Action()
        object ArrowDown : Action()
        object ArrowLeft : Action()
        object ArrowRight : Action()
    }

    private val COMMAND_KEYWORDS = setOf(
        "command", "cmd", "slash", "forward slash", "skip", "escape", "esc",
        "enter", "return", "submit", "space", "blank", "backspace", "back", "delete",
        "dot", "period", "full stop", "comma", "at the rate", "at sign", "at",
        "hash", "pound", "hashtag", "underscore", "hyphen", "dash", "minus",
        "plus", "equals", "equal", "question mark", "exclamation", "exclamation mark",
        "open bracket", "open brace", "close bracket", "close brace", "colon", "semicolon",
        "quotes", "double quotes", "single quote", "apostrophe", "backslash",
        "pipe", "vertical bar", "tilde", "star", "asterisk", "ampersand", "and sign",
        "percent", "dollar", "up", "down", "left", "right", "arrow"
    )

    fun process(raw: String): List<Action> {
        val actions = mutableListOf<Action>()
        val normalized = raw.trim().lowercase()
            .replace(Regex("/\\s+"), "/")
            .replace(Regex("\\s+/\\s*"), "/")
            .replace("slush", "slash")
            .replace("slosh", "slash")
            .replace("slash slash", "slash")
        val words = normalized.split("\\s+".toRegex())
        var i = 0
        val buffer = StringBuilder()

        fun flushBuffer() {
            if (buffer.isNotEmpty()) {
                actions.add(Action.Text(buffer.toString()))
                buffer.clear()
            }
        }

        while (i < words.size) {
            val word = words[i]

            when {
                word == "command" || word == "cmd" -> {
                    i++
                    if (i < words.size) {
                        val cmd = words[i]
                        when {
                            cmd == "down" || cmd == "down arrow" -> { flushBuffer(); actions.add(Action.ArrowDown); i++ }
                            cmd == "up" || cmd == "up arrow" -> { flushBuffer(); actions.add(Action.ArrowUp); i++ }
                            cmd == "left" || cmd == "left arrow" -> { flushBuffer(); actions.add(Action.ArrowLeft); i++ }
                            cmd == "right" || cmd == "right arrow" -> { flushBuffer(); actions.add(Action.ArrowRight); i++ }
                            cmd == "exit" || cmd == "escape" || cmd == "esc" || cmd == "skip" -> { flushBuffer(); actions.add(Action.Escape); i++ }
                            cmd == "enter" || cmd == "return" || cmd == "submit" -> { flushBuffer(); actions.add(Action.Enter); i++ }
                            cmd == "backspace" || cmd == "delete" -> { flushBuffer(); actions.add(Action.Backspace); i++ }
                            cmd == "space" -> { flushBuffer(); actions.add(Action.Space); i++ }
                            else -> {
                                flushBuffer()
                                buffer.append("/")
                                buffer.append(cmd)
                                i++
                                while (i < words.size) {
                                    val next = words[i]
                                    if (next in COMMAND_KEYWORDS) break
                                    buffer.append(next)
                                    i++
                                }
                            }
                        }
                    } else {
                        if (buffer.isNotEmpty()) buffer.append(" ")
                        buffer.append("command")
                    }
                }
                word == "down" && i + 1 < words.size && words[i + 1] == "arrow" -> { flushBuffer(); actions.add(Action.ArrowDown); i += 2 }
                word == "up" && i + 1 < words.size && words[i + 1] == "arrow" -> { flushBuffer(); actions.add(Action.ArrowUp); i += 2 }
                word == "left" && i + 1 < words.size && words[i + 1] == "arrow" -> { flushBuffer(); actions.add(Action.ArrowLeft); i += 2 }
                word == "right" && i + 1 < words.size && words[i + 1] == "arrow" -> { flushBuffer(); actions.add(Action.ArrowRight); i += 2 }
                word == "slash" || word == "forward slash" -> {
                    flushBuffer()
                    buffer.append("/")
                    i++
                    while (i < words.size) {
                        val next = words[i]
                        if (next in COMMAND_KEYWORDS) break
                        buffer.append(next)
                        i++
                    }
                }
                word.startsWith("/") -> { flushBuffer(); buffer.append(word); i++ }
                word == "skip" || word == "escape" || word == "esc" -> { flushBuffer(); actions.add(Action.Escape); i++ }
                word == "enter" || word == "return" || word == "submit" -> { flushBuffer(); actions.add(Action.Enter); i++ }
                word == "space" || word == "blank" -> { flushBuffer(); actions.add(Action.Space); i++ }
                word == "backspace" || word == "back space" || word == "delete" -> { flushBuffer(); actions.add(Action.Backspace); i++ }
                word == "dot" || word == "period" || word == "full stop" -> { buffer.append("."); i++ }
                word == "comma" -> { buffer.append(","); i++ }
                word == "at the rate" || word == "at sign" || word == "at" -> { buffer.append("@"); i++ }
                word == "hash" || word == "pound" || word == "hashtag" -> { buffer.append("#"); i++ }
                word == "underscore" -> { buffer.append("_"); i++ }
                word == "hyphen" || word == "dash" || word == "minus" -> { buffer.append("-"); i++ }
                word == "plus" -> { buffer.append("+"); i++ }
                word == "equals" || word == "equal" -> { buffer.append("="); i++ }
                word == "question mark" -> { buffer.append("?"); i++ }
                word == "exclamation" || word == "exclamation mark" -> { buffer.append("!"); i++ }
                word == "open bracket" || word == "open brace" -> { buffer.append("("); i++ }
                word == "close bracket" || word == "close brace" -> { buffer.append(")"); i++ }
                word == "colon" -> { buffer.append(":"); i++ }
                word == "semicolon" -> { buffer.append(";"); i++ }
                word == "quotes" || word == "double quotes" -> { buffer.append("\""); i++ }
                word == "single quote" || word == "apostrophe" -> { buffer.append("'"); i++ }
                word == "backslash" -> { buffer.append("\\"); i++ }
                word == "pipe" || word == "vertical bar" -> { buffer.append("|"); i++ }
                word == "tilde" -> { buffer.append("~"); i++ }
                word == "star" || word == "asterisk" -> { buffer.append("*"); i++ }
                word == "ampersand" || word == "and sign" -> { buffer.append("&"); i++ }
                word == "percent" -> { buffer.append("%"); i++ }
                word == "dollar" -> { buffer.append("$"); i++ }
                else -> {
                    if (buffer.isNotEmpty() && !buffer.endsWith("/")) buffer.append(" ")
                    buffer.append(word)
                    i++
                }
            }
        }

        flushBuffer()
        return actions
    }
}
