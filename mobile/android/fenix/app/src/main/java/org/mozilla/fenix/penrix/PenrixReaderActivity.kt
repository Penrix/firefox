/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.penrix

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale
import org.mozilla.fenix.IntentReceiverActivity

class PenrixReaderActivity : Activity() {
    private lateinit var reading: PenrixReadingStore
    private lateinit var scroll: ScrollView
    private var chapterUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reading = PenrixReadingStore(this)
        chapterUrl = intent.getStringExtra("chapter_url").orEmpty()
        val chapter = reading.chapter(chapterUrl) ?: run { finish(); return }
        reading.save(chapter, addToLibrary = true)
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val background = Color.rgb(246, 244, 236)
        val foreground = Color.rgb(35, 42, 37)
        window.statusBarColor = background
        window.navigationBarColor = background
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(background)
            setPadding(dp(24), dp(8), dp(24), 0)
        }
        layout.addView(TextView(this).apply {
            text = "‹  返回浏览器"
            textSize = 14f
            setTextColor(Color.rgb(98, 112, 98))
            setPadding(0, dp(12), 0, dp(16))
            setOnClickListener { finish() }
        })
        layout.addView(TextView(this).apply {
            text = chapter.optString("workTitle") + "\n" + chapter.optString("title")
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(foreground)
            setPadding(0, dp(8), 0, dp(10))
        })
        var original = false
        val body = TextView(this).apply {
            text = chapter.getString("text")
            textSize = 21f
            textLocale = Locale.SIMPLIFIED_CHINESE
            setTextColor(foreground)
            setTextIsSelectable(true)
            setLineSpacing(dp(8).toFloat(), 1.15f)
            setPadding(0, dp(8), 0, dp(32))
        }
        scroll = ScrollView(this).apply { addView(body) }
        layout.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER }
        fun button(label: String, action: () -> Unit) {
            controls.addView(Button(this).apply {
                text = label
                textSize = 14f
                minWidth = 0
                setTextColor(Color.rgb(34, 61, 53))
                setBackgroundColor(Color.TRANSPARENT)
                isAllCaps = false
                setPadding(0, 0, 0, 0)
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(0, dp(52), 1f))
        }
        button("上一章") { navigate(chapter.optString("previousUrl"), true) }
        button("目录") { navigate(chapter.optString("catalogUrl"), false) }
        button("原文") {
            original = !original
            body.text = chapter.getString(if (original) "rawText" else "text")
        }
        button("下一章") { navigate(chapter.optString("nextUrl"), true) }
        layout.addView(controls)
        setContentView(layout)
        scroll.post { scroll.scrollTo(0, reading.position(chapterUrl)) }
    }

    private fun navigate(url: String, continueReading: Boolean) {
        if (url.isBlank() || url == "null" || PenrixSiteModuleRegistry.forUrl(url) == null) return
        reading.position(chapterUrl, scroll.scrollY)
        if (continueReading) {
            getSharedPreferences("penrix_reader", Context.MODE_PRIVATE).edit().putString("pending_url", url).apply()
        }
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url), this, IntentReceiverActivity::class.java))
        finish()
    }

    override fun onPause() {
        if (::scroll.isInitialized) reading.position(chapterUrl, scroll.scrollY)
        super.onPause()
    }

    override fun onDestroy() {
        reading.close()
        super.onDestroy()
    }
}
