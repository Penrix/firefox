/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.penrix

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import android.text.style.LineHeightSpan
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale
import kotlin.math.roundToInt
import org.json.JSONObject
import org.mozilla.fenix.IntentReceiverActivity

class PenrixReaderActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("penrix_reader", Context.MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var reading: PenrixReadingStore
    private lateinit var chapter: JSONObject
    private lateinit var root: LinearLayout
    private var systemInsets = Insets.NONE
    private lateinit var header: TextView
    private lateinit var body: TextView
    private lateinit var scroll: ScrollView
    private lateinit var gestures: GestureDetector
    private var original = false
    private var automatic = false
    private var foreground = false
    private var ready = false
    private var dialog: AlertDialog? = null
    private val autoTurn = Runnable { turn(1); scheduleAuto() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        reading = PenrixReadingStore(this)
        val url = savedInstanceState?.getString("chapter_url") ?: intent.getStringExtra("chapter_url").orEmpty()
        val initial = reading.chapter(url) ?: run { finish(); return }
        val flags = intent.getIntExtra("reader_flags", 0)
        original = savedInstanceState?.getBoolean("original") ?: (flags and ORIGINAL != 0)
        automatic = savedInstanceState?.getBoolean("automatic") ?: (flags and AUTO != 0)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        header = TextView(this).apply {
            textSize = 14f
            maxLines = 1
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(10))
            setOnClickListener { showMenu() }
        }
        body = TextView(this).apply {
            textLocale = Locale.SIMPLIFIED_CHINESE
            setTextIsSelectable(true)
            setPadding(0, 0, 0, dp(8))
        }
        scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(body)
        }
        root.addView(header)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            systemInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            applyReaderPadding()
            insets
        }
        setContentView(root)
        ViewCompat.requestApplyInsets(root)
        gestures = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onSingleTapUp(e: MotionEvent): Boolean = readerTap(e)
        })
        openChapter(initial, if (savedInstanceState == null) flags else 0)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
    private fun setting(key: String, default: Int): Int = prefs.getInt(key, default)
    private fun chapterUrl(): String = chapter.getString("url")

    private fun openChapter(value: JSONObject, flags: Int) {
        chapter = value
        reading.save(value, addToLibrary = true)
        header.text = value.optString("title")
        val position = reading.position(chapterUrl())
        ready = false
        render()
        scroll.post {
            when {
                flags and FROM_END != 0 -> scroll.scrollTo(0, body.height)
                flags and FROM_START != 0 -> scroll.scrollTo(0, 0)
                position.offset >= 0 -> scrollToOffset(position.offset)
                else -> scroll.scrollTo(0, position.pixels)
            }
            ready = true
        }
    }

    private fun applyReaderPadding() {
        val margin = dp(setting("margin", 24))
        root.setPadding(
            margin + systemInsets.left, systemInsets.top,
            margin + systemInsets.right, systemInsets.bottom,
        )
    }

    private fun render() {
        applyReaderPadding()
        val night = prefs.getBoolean("night", false)
        val background = if (night) Color.rgb(24, 29, 27) else Color.rgb(246, 244, 236)
        val ink = if (night) Color.rgb(217, 225, 215) else Color.rgb(35, 42, 37)
        root.setBackgroundColor(background)
        body.setTextColor(ink)
        header.setTextColor(ink)
        styleSystemBars(background, night)
        body.textSize = setting("font_size", 21).toFloat()
        body.setLineSpacing(dp(setting("line_spacing", 6)).toFloat(), 1f)
        val paragraphGap = dp(setting("paragraph_spacing", 12))
        val normal = body.paint.fontMetricsInt
        val source = chapter.getString(if (original) "rawText" else "text")
        val formatted = SpannableStringBuilder()
        for (line in source.lineSequence().map(String::trim).filter(String::isNotEmpty)) {
            if (formatted.isNotEmpty()) formatted.append('\n')
            val start = formatted.length
            formatted.append(line)
            formatted.setSpan(LeadingMarginSpan.Standard((body.textSize * 2).roundToInt(), 0),
                start, formatted.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            val last = formatted.length - 1
            formatted.setSpan(LineHeightSpan { _, start, end, _, _, metrics ->
                if (last >= start && last < end) {
                    metrics.descent = normal.descent + paragraphGap
                    metrics.bottom = normal.bottom + paragraphGap
                }
            }, last, last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        body.text = formatted
    }

    private fun offset(): Int = body.layout?.let {
        it.getLineStart(it.getLineForVertical(scroll.scrollY - body.totalPaddingTop))
    } ?: 0

    private fun scrollToOffset(offset: Int) {
        val layout = body.layout ?: return
        val line = layout.getLineForOffset(offset.coerceIn(0, body.length()))
        scroll.scrollTo(0, if (line == 0) 0 else body.totalPaddingTop + layout.getLineTop(line))
    }

    private fun reflow() {
        val anchor = offset()
        render()
        scroll.post { scrollToOffset(anchor) }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (::gestures.isInitialized && gestures.onTouchEvent(event)) {
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                val cancel = MotionEvent.obtain(event)
                cancel.action = MotionEvent.ACTION_CANCEL
                super.dispatchTouchEvent(cancel)
                cancel.recycle()
                return true
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun readerTap(event: MotionEvent): Boolean {
        val location = IntArray(2)
        scroll.getLocationOnScreen(location)
        val x = event.rawX - location[0]
        val y = event.rawY - location[1]
        if (x < 0 || y < 0 || x >= scroll.width || y >= scroll.height || body.hasSelection()) return false
        if (automatic) {
            automatic = false
            scheduleAuto()
            return true
        }
        val column = (x * 3 / scroll.width).toInt()
        val row = (y * 3 / scroll.height).toInt()
        when {
            column == 1 && row == 1 -> showMenu()
            column == 2 || column == 1 && row == 2 -> turn(1)
            else -> turn(-1)
        }
        return true
    }

    private fun turn(direction: Int) {
        if (!ready) return
        if (!scroll.canScrollVertically(direction)) {
            val forward = direction > 0
            go(chapter.optString(if (forward) "nextUrl" else "previousUrl"), true,
                if (forward) FROM_START else FROM_END)
            return
        }
        val layout = body.layout ?: return
        val target = scroll.scrollY + direction * scroll.height - body.totalPaddingTop
        var line = layout.getLineForVertical(target)
        if (direction < 0 && target > layout.getLineTop(line) && line + 1 < layout.lineCount) line++
        val pageY = body.totalPaddingTop + layout.getLineTop(line)
        scroll.scrollTo(0, if (direction > 0 && pageY <= scroll.scrollY) scroll.scrollY + scroll.height else pageY)
    }

    private fun showMenu() {
        val labels = arrayOf("上一章", "下一章", "目录", if (original) "大陆简体" else "原文",
            if (automatic) "暂停自动翻页" else "开始自动翻页", "排版与速度", "返回浏览器")
        showDialog(AlertDialog.Builder(this).setTitle("阅读菜单").setItems(labels) { _, selected ->
            when (selected) {
                0 -> go(chapter.optString("previousUrl"), true, FROM_START)
                1 -> go(chapter.optString("nextUrl"), true, FROM_START)
                2 -> go(chapter.optString("catalogUrl"), false)
                3 -> { original = !original; reflow() }
                4 -> automatic = !automatic
                5 -> showStyle()
                6 -> finish()
            }
        }.create())
    }

    private fun showStyle() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(12))
        }
        fun slider(label: String, key: String, initial: Int, range: IntRange, unit: String) {
            val title = TextView(this)
            val bar = SeekBar(this).apply {
                max = range.last - range.first
                progress = setting(key, initial) - range.first
            }
            title.text = "$label  ${bar.progress + range.first}$unit"
            bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(view: SeekBar, value: Int, fromUser: Boolean) {
                    title.text = "$label  ${value + range.first}$unit"
                }
                override fun onStartTrackingTouch(view: SeekBar) = Unit
                override fun onStopTrackingTouch(view: SeekBar) {
                    prefs.edit().putInt(key, view.progress + range.first).apply()
                    if (key != "auto_seconds") reflow()
                }
            })
            panel.addView(title)
            panel.addView(bar)
        }
        slider("字号", "font_size", 21, 14..32, "")
        slider("行距", "line_spacing", 6, 0..18, "")
        slider("段距", "paragraph_spacing", 12, 0..24, "")
        slider("页边距", "margin", 24, 8..40, "")
        slider("自动翻页", "auto_seconds", 10, 3..45, "秒/页")
        val container = ScrollView(this).apply { addView(panel) }
        showDialog(AlertDialog.Builder(this).setTitle("排版与速度").setView(container)
            .setNeutralButton(if (prefs.getBoolean("night", false)) "日间" else "夜间") { _, _ ->
                prefs.edit().putBoolean("night", !prefs.getBoolean("night", false)).apply()
                reflow()
            }.setPositiveButton("完成", null).create())
    }

    private fun showDialog(value: AlertDialog) {
        dialog = value
        scheduleAuto()
        value.setOnDismissListener {
            if (dialog === value) dialog = null
            scheduleAuto()
        }
        value.show()
    }

    private fun go(url: String, continueReading: Boolean, edge: Int = 0) {
        if (url.isBlank() || url == "null" || PenrixSiteModuleRegistry.forUrl(url) == null) {
            automatic = false
            scheduleAuto()
            Toast.makeText(this, "没有可打开的页面", Toast.LENGTH_SHORT).show()
            return
        }
        savePosition()
        if (continueReading) {
            val cached = reading.chapter(url)
            if (cached != null) {
                openChapter(cached, edge)
                return
            }
            val flags = edge or (if (automatic) AUTO else 0) or (if (original) ORIGINAL else 0)
            prefs.edit().putString("pending_url", url).putInt("pending_reader_flags", flags).apply()
        }
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url), this, IntentReceiverActivity::class.java))
        finish()
    }

    private fun savePosition() {
        if (ready) reading.position(chapterUrl(), scroll.scrollY, offset())
    }

    private fun scheduleAuto() {
        handler.removeCallbacks(autoTurn)
        if (automatic && foreground && dialog == null && !isFinishing) {
            handler.postDelayed(autoTurn, setting("auto_seconds", 10) * 1000L)
        }
    }

    @Suppress("DEPRECATION")
    private fun styleSystemBars(background: Int, night: Boolean) {
        window.statusBarColor = background
        window.navigationBarColor = background
        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }
    }

    override fun onResume() {
        super.onResume()
        foreground = true
        scheduleAuto()
    }

    override fun onPause() {
        foreground = false
        scheduleAuto()
        savePosition()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("automatic", automatic)
        outState.putBoolean("original", original)
        outState.putString("chapter_url", if (::chapter.isInitialized) chapterUrl() else "")
    }

    override fun onDestroy() {
        reading.close()
        super.onDestroy()
    }

    private companion object {
        const val AUTO = 1
        const val FROM_START = 2
        const val FROM_END = 4
        const val ORIGINAL = 8
    }
}
