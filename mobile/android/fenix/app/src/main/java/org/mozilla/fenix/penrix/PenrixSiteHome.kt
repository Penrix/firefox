/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.penrix

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun PenrixSiteHome(
    history: PenrixSiteHistory,
    onOpenPage: (PenrixSiteModule, String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var library by remember { mutableStateOf(PenrixReadingStore(context).use { it.library() }) }
    var shelf by rememberSaveable { mutableStateOf(false) }
    var adblockStatus by remember { mutableStateOf(PenrixAdblockBootstrap.status(context)) }
    val ink = Color(0xFF223D35)
    val muted = Color(0xFF73786F)
    val paper = Color(0xFFF7F6F0)
    DisposableEffect(lifecycleOwner) {
        val listener = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                library = PenrixReadingStore(context).use { it.library() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(listener)
        onDispose { lifecycleOwner.lifecycle.removeObserver(listener) }
    }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences("penrix_adblock_bootstrap", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "initial_install_status") adblockStatus = PenrixAdblockBootstrap.status(context)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    fun read(chapterUrl: String) {
        context.startActivity(Intent(context, PenrixReaderActivity::class.java).putExtra("chapter_url", chapterUrl))
    }
    Column(Modifier.fillMaxSize().background(paper)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("PENRIX  /  私人浏览器", color = muted, style = MaterialTheme.typography.labelMedium)
            Text(if (shelf) "我的书架" else "留一点时间，\n给喜欢的文字。", color = ink,
                style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            if (!shelf) {
                val latest = library.firstOrNull()
                Card(colors = CardDefaults.cardColors(containerColor = ink), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("继续阅读", color = Color(0xFFC5D2BC), style = MaterialTheme.typography.labelLarge)
                        Text(latest?.optString("workTitle")?.ifBlank { "上次阅读" } ?: "从一本好书开始",
                            color = Color.White, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(latest?.optString("title") ?: "打开站点，收藏喜欢的章节。下次从这里继续。",
                            color = Color(0xFFD8E0D5), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (latest != null) Button(onClick = { read(latest.getString("url")) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8EEDB), contentColor = ink)) { Text("接着读  →") }
                    }
                }
                Text("我的站点", color = ink, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("twkan", "cool18").forEach { id ->
                        val site = requireNotNull(PenrixSiteModuleRegistry.byId(id))
                        Card(onClick = {
                            val url = history.lastUrl(site)?.let { PenrixSiteModuleRegistry.safeResumeUrl(site, it) } ?: site.homeUrl
                            onOpenPage(site, url)
                        }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = if (id == "twkan") Color(0xFFE7ECDf) else Color(0xFFEFE7DC))) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(if (id == "twkan") "文" else "帖", color = ink, style = MaterialTheme.typography.headlineMedium)
                                Text(if (id == "twkan") "台湾小说网" else site.title, color = ink,
                                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(if (id == "twkan") "搜书 · 目录 · 阅读" else "主题 · 正文 · 收藏", color = muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Text(when (adblockStatus) {
                    "installed" -> "去广告已开启  ·  正文清洗与简体阅读"
                    "installing" -> "正在准备去广告功能…"
                    "install_failed", "metadata_unavailable", "not_enabled" -> "去广告尚未就绪，可在扩展菜单检查"
                    else -> "正在准备阅读环境…"
                }, color = muted, style = MaterialTheme.typography.bodySmall)
            } else {
                Text("读到哪里，就从哪里继续。", color = muted, style = MaterialTheme.typography.bodyMedium)
                if (library.isEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Text("书架还空着", color = ink, style = MaterialTheme.typography.titleLarge)
                    Text("进入章节后点“收藏”或“阅读”，就会留在这里。", color = muted, style = MaterialTheme.typography.bodyMedium)
                }
                library.forEach { chapter ->
                    Card(onClick = { read(chapter.getString("url")) }, modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(chapter.optString("workTitle").ifBlank { "未命名作品" }, color = ink,
                                style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(chapter.optString("title"), color = muted, style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("继续阅读  →", color = ink, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFFEFEEE7)).padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = { shelf = false }) { Text("阅读", color = if (!shelf) ink else muted, fontWeight = if (!shelf) FontWeight.Bold else FontWeight.Normal) }
            TextButton(onClick = { shelf = true }) { Text("书架", color = if (shelf) ink else muted, fontWeight = if (shelf) FontWeight.Bold else FontWeight.Normal) }
        }
    }
}
