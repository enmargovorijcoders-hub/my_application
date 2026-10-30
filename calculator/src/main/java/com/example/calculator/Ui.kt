package com.example.calculator

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private const val HOME_URL = "https://www.google.com"

private val Bg = Color(0xFF0D1117)
private val Bar = Color(0xFF161B22)
private val NumKey = Color(0xFF2A313C)
private val FnKey = Color(0xFF3B4453)
private val Accent = Color(0xFFF59E0B)
private val Muted = Color(0xFF9AA4B2)
private val Danger = Color(0xFFFCA5A5)

private enum class Mode(val label: String) { Basic("Basic"), Scientific("Scientific"), Web("Web") }

@Composable
fun CalcApp() {
    var mode by remember { mutableStateOf(Mode.Basic) }
    val calc = remember { CalcState() }
    val webHolder = remember { arrayOfNulls<WebView>(1) }
    var progress by remember { mutableIntStateOf(0) }

    BackHandler(enabled = mode == Mode.Web) {
        val w = webHolder[0]
        if (w != null && w.canGoBack()) w.goBack() else mode = Mode.Basic
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (mode) {
                Mode.Basic -> CalcScreen(calc, scientific = false)
                Mode.Scientific -> CalcScreen(calc, scientific = true)
                Mode.Web -> WebPane(
                    holder = webHolder,
                    progress = progress,
                    onProgress = { progress = it },
                )
            }
        }
        TabBar(mode) { mode = it }
    }
}

// ---------------------------------------------------------------- calculator

@Composable
private fun CalcScreen(c: CalcState, scientific: Boolean) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Display(c, Modifier.weight(2f))
        if (scientific) SciRows(c)
        Keypad(c, scientific)
    }
}

@Composable
private fun Display(c: CalcState, modifier: Modifier) {
    val len = c.expr.length
    val size = when {
        len <= 10 -> 56
        len <= 16 -> 44
        len <= 24 -> 34
        len <= 36 -> 26
        else -> 20
    }
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        BasicText(
            text = c.history,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = Muted, fontSize = 16.sp, textAlign = TextAlign.End),
            modifier = Modifier.fillMaxWidth(),
        )
        BasicText(
            text = c.expr.ifEmpty { "0" },
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = Color.White,
                fontSize = size.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        BasicText(
            text = c.preview,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = if (c.preview == "Error") Danger else Muted,
                fontSize = 22.sp,
                textAlign = TextAlign.End,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp),
        )
    }
}

@Composable
private fun ColumnScope.SciRows(c: CalcState) {
    val inv = c.inverse

    fun fn(token: String) {
        c.input(token)
        c.clearInverse()
    }

    Row(Modifier.weight(0.8f).fillMaxWidth()) {
        K("INV", if (inv) Accent else FnKey, if (inv) Bg else Color.White, 15) { c.toggleInverse() }
        K(if (c.degrees) "DEG" else "RAD", FnKey, Color.White, 15) { c.toggleDegrees() }
        K("(", FnKey, Color.White, 18) { c.input("(") }
        K(")", FnKey, Color.White, 18) { c.input(")") }
        K("DEL", FnKey, Color.White, 15) { c.backspace() }
    }
    Row(Modifier.weight(0.8f).fillMaxWidth()) {
        K(if (inv) "asin" else "sin", FnKey, Color.White, 15) { fn(if (inv) "asin(" else "sin(") }
        K(if (inv) "acos" else "cos", FnKey, Color.White, 15) { fn(if (inv) "acos(" else "cos(") }
        K(if (inv) "atan" else "tan", FnKey, Color.White, 15) { fn(if (inv) "atan(" else "tan(") }
        K(if (inv) "e^x" else "ln", FnKey, Color.White, 15) { fn(if (inv) "e^(" else "ln(") }
        K(if (inv) "10^x" else "log", FnKey, Color.White, 15) { fn(if (inv) "10^(" else "log(") }
    }
    Row(Modifier.weight(0.8f).fillMaxWidth()) {
        K("x²", FnKey, Color.White, 15) { c.input("^2") }
        K("√", FnKey, Color.White, 15) { c.input("√(") }
        K("x^y", FnKey, Color.White, 15) { c.input("^") }
        K("n!", FnKey, Color.White, 15) { c.input("!") }
        K("1/x", FnKey, Color.White, 15) { c.input("^(−1)") }
    }
    Row(Modifier.weight(0.8f).fillMaxWidth()) {
        K("π", FnKey, Color.White, 15) { c.input("π") }
        K("e", FnKey, Color.White, 15) { c.input("e") }
        K("|x|", FnKey, Color.White, 15) { c.input("abs(") }
        K("³√", FnKey, Color.White, 15) { c.input("³√(") }
        K("Ans", FnKey, Color.White, 15) { c.useAnswer() }
    }
}

@Composable
private fun ColumnScope.Keypad(c: CalcState, scientific: Boolean) {
    Row(Modifier.weight(1f).fillMaxWidth()) {
        K("AC", FnKey) { c.clear() }
        if (scientific) {
            K("±", FnKey) { c.negate() }
        } else {
            K("DEL", FnKey, size = 18) { c.backspace() }
        }
        K("%", FnKey) { c.input("%") }
        K("÷", Accent, Bg, 28) { c.input("÷") }
    }
    Row(Modifier.weight(1f).fillMaxWidth()) {
        K("7", NumKey) { c.input("7") }
        K("8", NumKey) { c.input("8") }
        K("9", NumKey) { c.input("9") }
        K("×", Accent, Bg, 28) { c.input("×") }
    }
    Row(Modifier.weight(1f).fillMaxWidth()) {
        K("4", NumKey) { c.input("4") }
        K("5", NumKey) { c.input("5") }
        K("6", NumKey) { c.input("6") }
        K("−", Accent, Bg, 28) { c.input("−") }
    }
    Row(Modifier.weight(1f).fillMaxWidth()) {
        K("1", NumKey) { c.input("1") }
        K("2", NumKey) { c.input("2") }
        K("3", NumKey) { c.input("3") }
        K("+", Accent, Bg, 28) { c.input("+") }
    }
    Row(Modifier.weight(1f).fillMaxWidth()) {
        if (scientific) {
            K("0", NumKey) { c.input("0") }
        } else {
            K("±", NumKey) { c.negate() }
        }
        if (scientific) {
            K(".", NumKey) { c.input(".") }
            K("DEL", NumKey, size = 18) { c.backspace() }
        } else {
            K("0", NumKey) { c.input("0") }
            K(".", NumKey) { c.input(".") }
        }
        K("=", Accent, Bg, 28) { c.compute() }
    }
}

/** One calculator key. Takes an equal share of its row. */
@Composable
private fun RowScope.K(
    label: String,
    bg: Color,
    fg: Color = Color.White,
    size: Int = 24,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (pressed) lerp(bg, Color.White, 0.22f) else bg)
            .clickable(interactionSource = source, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            maxLines = 1,
            style = TextStyle(color = fg, fontSize = size.sp, fontWeight = FontWeight.Medium),
        )
    }
}

// ---------------------------------------------------------------- web

@Composable
private fun WebPane(
    holder: Array<WebView?>,
    progress: Int,
    onProgress: (Int) -> Unit,
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Bar)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WebButton(Icons.Filled.ArrowBack) { holder[0]?.let { if (it.canGoBack()) it.goBack() } }
            WebButton(Icons.Filled.ArrowForward) { holder[0]?.let { if (it.canGoForward()) it.goForward() } }
            WebButton(Icons.Filled.Refresh) { holder[0]?.reload() }
            WebButton(Icons.Filled.Home) { holder[0]?.loadUrl(HOME_URL) }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
        ) {
            if (progress in 1..99) {
                Box(
                    Modifier
                        .fillMaxWidth(progress / 100f)
                        .fillMaxHeight()
                        .background(Accent)
                )
            }
        }
        AndroidView(
            factory = { ctx ->
                // Reuse the same page when coming back to this tab, so it isn't reloaded.
                val existing = holder[0]
                if (existing != null) {
                    (existing.parent as? ViewGroup)?.removeView(existing)
                    existing
                } else {
                    WebView(ctx).also {
                        setupWeb(it, ctx, onProgress)
                        holder[0] = it
                        it.loadUrl(HOME_URL)
                    }
                }
            },
            update = { it.onResume() },
            onRelease = { it.onPause() },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun WebButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(NumKey)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(Color.White),
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun setupWeb(web: WebView, context: Context, onProgress: (Int) -> Unit) {
    val s = web.settings
    s.javaScriptEnabled = true
    s.domStorageEnabled = true
    s.useWideViewPort = true
    s.loadWithOverviewMode = true
    CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)

    web.webViewClient = object : WebViewClient() {
        // This older version is still called on every Android version, including 5.0.
        @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            val scheme = Uri.parse(url).scheme
            if (scheme == "http" || scheme == "https") return false
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {
            }
            return true
        }
    }
    web.webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            onProgress(newProgress)
        }
    }
}

// ---------------------------------------------------------------- tabs

@Composable
private fun TabBar(mode: Mode, onMode: (Mode) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Bar)
    ) {
        Mode.values().forEach { m ->
            val selected = m == mode
            Box(
                Modifier
                    .weight(1f)
                    .clickable { onMode(m) }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = m.label,
                    style = TextStyle(
                        color = if (selected) Accent else Muted,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}
