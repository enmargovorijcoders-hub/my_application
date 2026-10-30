package com.example.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class MathError : Exception()

private val FUNCS = listOf("asin", "acos", "atan", "sqrt", "cbrt", "sin", "cos", "tan", "log", "abs", "ln")

/**
 * Reads an expression such as 2+3*sin(30)^2 and works out its value.
 * Supports + - * / ^ ! % ( ), constants pi and e, and implicit multiplication like 2(3) or 2pi.
 */
class Parser(private val s: String, private val degrees: Boolean) {
    private var i = 0
    private var pctTail = false

    fun parse(): Double {
        val v = expr()
        if (i < s.length) throw MathError()
        return v
    }

    private fun peek(): Char = if (i < s.length) s[i] else '\u0000'

    private fun expr(): Double {
        var left = term()
        while (true) {
            val c = peek()
            if (c == '+' || c == '-') {
                i++
                val right = term()
                // "200+10%" means 200 plus 10 percent of 200
                val r = if (pctTail) left * right else right
                left = if (c == '+') left + r else left - r
                pctTail = false
            } else {
                return left
            }
        }
    }

    private fun term(): Double {
        var left = unary()
        while (true) {
            val c = peek()
            if (c == '*' || c == '/') {
                i++
                val right = unary()
                if (c == '/' && right == 0.0) throw MathError()
                left = if (c == '*') left * right else left / right
                pctTail = false
            } else if (startsOperand(c)) {
                val right = unary()
                left *= right
                pctTail = false
            } else {
                return left
            }
        }
    }

    private fun startsOperand(c: Char): Boolean =
        c.isDigit() || c == '.' || c == '(' || c == 'π' || c.isLetter()

    private fun unary(): Double {
        val c = peek()
        if (c == '-') {
            i++
            return -unary()
        }
        if (c == '+') {
            i++
            return unary()
        }
        return power()
    }

    private fun power(): Double {
        val base = postfix()
        if (peek() == '^') {
            i++
            val e = unary()
            pctTail = false
            return Math.pow(base, e)
        }
        return base
    }

    private fun postfix(): Double {
        var v = primary()
        pctTail = false
        while (true) {
            val c = peek()
            if (c == '!') {
                i++
                v = factorial(v)
                pctTail = false
            } else if (c == '%') {
                i++
                v /= 100.0
                pctTail = true
            } else {
                return v
            }
        }
    }

    private fun primary(): Double {
        val c = peek()
        if (c == '(') {
            i++
            val v = expr()
            if (peek() != ')') throw MathError()
            i++
            return v
        }
        if (c.isDigit() || c == '.') return number()
        if (c == 'π') {
            i++
            return Math.PI
        }
        for (name in FUNCS) {
            if (s.startsWith(name, i) && i + name.length < s.length && s[i + name.length] == '(') {
                i += name.length + 1
                val a = expr()
                if (peek() != ')') throw MathError()
                i++
                return callFunc(name, a)
            }
        }
        if (c == 'e') {
            i++
            return Math.E
        }
        throw MathError()
    }

    private fun number(): Double {
        val start = i
        while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
        if (i < s.length && s[i] == 'E') {
            var j = i + 1
            if (j < s.length && (s[j] == '+' || s[j] == '-')) j++
            if (j < s.length && s[j].isDigit()) {
                while (j < s.length && s[j].isDigit()) j++
                i = j
            }
        }
        return s.substring(start, i).toDoubleOrNull() ?: throw MathError()
    }

    private fun toRad(x: Double) = if (degrees) Math.toRadians(x) else x
    private fun fromRad(x: Double) = if (degrees) Math.toDegrees(x) else x
    private fun clean(x: Double) = if (abs(x) < 1e-12) 0.0 else x

    private fun callFunc(name: String, a: Double): Double = when (name) {
        "sin" -> clean(sin(toRad(a)))
        "cos" -> clean(cos(toRad(a)))
        "tan" -> {
            val c = cos(toRad(a))
            if (abs(c) < 1e-12) throw MathError()
            clean(sin(toRad(a)) / c)
        }
        "asin" -> {
            if (a < -1.0 || a > 1.0) throw MathError()
            fromRad(asin(a))
        }
        "acos" -> {
            if (a < -1.0 || a > 1.0) throw MathError()
            fromRad(acos(a))
        }
        "atan" -> fromRad(atan(a))
        "ln" -> if (a <= 0.0) throw MathError() else ln(a)
        "log" -> if (a <= 0.0) throw MathError() else log10(a)
        "sqrt" -> if (a < 0.0) throw MathError() else sqrt(a)
        "cbrt" -> Math.cbrt(a)
        "abs" -> abs(a)
        else -> throw MathError()
    }

    private fun factorial(v: Double): Double {
        if (v < 0.0 || v != floor(v) || v > 170.0) throw MathError()
        var r = 1.0
        var k = 2
        while (k <= v.toInt()) {
            r *= k
            k++
        }
        return r
    }
}

/** Returns the formatted answer, or null if the expression can't be worked out. */
fun evaluate(src: String, degrees: Boolean): String? {
    return try {
        val s = src
            .replace('×', '*')
            .replace('÷', '/')
            .replace('−', '-')
            .replace("³√", "cbrt")
            .replace("√", "sqrt")
            .replace(" ", "")
        val v = Parser(s, degrees).parse()
        if (v.isNaN() || v.isInfinite()) null else format(v)
    } catch (_: Exception) {
        null
    }
}

fun format(d: Double): String {
    if (d == 0.0) return "0"
    val bd = BigDecimal(d).round(MathContext(12, RoundingMode.HALF_UP)).stripTrailingZeros()
    val a = abs(d)
    val s = if (a >= 1e12 || a < 1e-7) bd.toString() else bd.toPlainString()
    return s.replace('-', '−')
}
