package com.example.calculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Everything the two calculator screens share: what has been typed and the answer so far. */
class CalcState {
    var expr by mutableStateOf("")
        private set
    var preview by mutableStateOf("")
        private set
    var history by mutableStateOf("")
        private set
    var degrees by mutableStateOf(true)
        private set
    var inverse by mutableStateOf(false)
        private set

    var lastAnswer = "0"
        private set

    private var fresh = false // true right after pressing "="

    private val ops = "+−×÷^"
    private val funcTokens = listOf("asin(", "acos(", "atan(", "sin(", "cos(", "tan(", "log(", "ln(", "abs(", "³√(", "√(")

    fun toggleDegrees() {
        degrees = !degrees
        refresh()
    }

    fun toggleInverse() {
        inverse = !inverse
    }

    fun clearInverse() {
        inverse = false
    }

    fun clear() {
        expr = ""
        preview = ""
        history = ""
        fresh = false
    }

    fun input(token: String) {
        val operatorLike = token.length == 1 && token[0] in ops
        val postfix = token == "!" || token == "%" || token == "^2" || token == "^(−1)"
        if (fresh) {
            if (!(operatorLike || postfix)) {
                expr = ""
                history = ""
            }
            fresh = false
        }

        var t = token
        when {
            token == "." -> {
                val n = currentNumber()
                if (n.contains('.')) return
                if (n.isEmpty()) t = "0."
            }
            operatorLike -> {
                if (expr.isEmpty()) {
                    if (token != "−") return
                } else if (expr.last() in ops) {
                    if (expr.length == 1) return
                    expr = expr.dropLast(1)
                } else if (expr.last() == '(' && token != "−") {
                    return
                }
            }
            postfix -> {
                if (expr.isEmpty() || expr.last() in ops || expr.last() == '(') return
            }
            token == ")" -> {
                val open = expr.count { it == '(' }
                val close = expr.count { it == ')' }
                if (open <= close || expr.last() in ops || expr.last() == '(') return
            }
            token == "10^(" -> {
                if (expr.isNotEmpty() && (expr.last().isDigit() || expr.last() == '.')) t = "×10^("
            }
        }
        expr += t
        refresh()
    }

    fun backspace() {
        fresh = false
        if (expr.isEmpty()) return
        val f = funcTokens.firstOrNull { expr.endsWith(it) }
        expr = if (f != null) expr.dropLast(f.length) else expr.dropLast(1)
        refresh()
    }

    fun negate() {
        fresh = false
        val n = currentNumber()
        if (n.isNotEmpty()) {
            val start = expr.length - n.length
            expr = expr.substring(0, start) + "(−" + n + ")"
        } else if (expr.endsWith(")")) {
            val m = Regex("\\(−([0-9.]+(?:E[+−]?[0-9]+)?)\\)$").find(expr)
            if (m != null) {
                expr = expr.removeRange(m.range.first, m.range.last + 1) + m.groupValues[1]
            }
        }
        refresh()
    }

    fun useAnswer() {
        input(lastAnswer)
    }

    fun compute() {
        val src = trimTail(expr)
        if (src.isEmpty()) return
        val closed = close(src)
        val r = evaluate(closed, degrees)
        if (r == null) {
            preview = "Error"
            return
        }
        history = "$closed ="
        expr = r
        lastAnswer = r
        fresh = true
        preview = ""
    }

    private fun currentNumber(): String {
        var k = expr.length
        while (k > 0 && (expr[k - 1].isDigit() || expr[k - 1] == '.')) k--
        return expr.substring(k)
    }

    private fun trimTail(s: String): String {
        var r = s
        while (r.isNotEmpty() && r.last() in ops) r = r.dropLast(1)
        return r
    }

    private fun close(s: String): String {
        val open = s.count { it == '(' } - s.count { it == ')' }
        return if (open > 0) s + ")".repeat(open) else s
    }

    private fun refresh() {
        val src = trimTail(expr)
        preview = if (src.isEmpty() || Regex("^[−-]?[0-9.]+$").matches(src)) {
            ""
        } else {
            evaluate(close(src), degrees) ?: ""
        }
    }
}
