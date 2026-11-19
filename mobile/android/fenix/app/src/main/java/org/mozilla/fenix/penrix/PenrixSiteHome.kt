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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun PenrixSiteHome(
    history: PenrixSiteHistory,
    onOpenPage: (PenrixSiteModule, String, Boolean) -> Unit,
    onOpenTabs: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var library by remember { mutableStateOf<List<JSONObject>?>(null) }
    var shelf by rememberSaveable { mutableStateOf(true) }
    var adblockStatus by remember { mutableStateOf("not_started") }
    val ink = Color(0xFF223D35)
    val muted = Color(0xFF73786F)
    val paper = Color(0xFFF7F6F0)
    LaunchedEffect(context, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            library = withContext(Dispatchers.IO) { PenrixReadingStore(context).use { it.library() } }
        }
    }
    LaunchedEffect(context) {
        val prefs = withContext(Dispatchers.IO) {
            context.getSharedPreferences("penrix_adblock_bootstrap", Context.MODE_PRIVATE).also {
                it.getString("initial_install_status", "not_started")
            }
        }
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "initial_install_status") adblockStatus = PenrixAdblockBootstrap.status(context)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        try {
            adblockStatus = PenrixAdblockBootstrap.status(context)
            awaitCancellation()
        } finally {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    fun read(chapterUrl: String) {
        context.startActivity(Intent(context, PenrixReaderActivity::class.java).putExtra("chapter_url", chapterUrl))
    }
    val twkan = requireNotNull(PenrixSiteModuleRegistry.byId("twkan"))
    val twkanSearchUrl = twkan.homeUrl + "search"
    Column(Modifier.fillMaxSize().background(paper)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                if (shelf) "书架" else "找书",
                color = ink,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (!shelf) {
                Button(
                    onClick = { onOpenPage(twkan, twkanSearchUrl, false) },
                    colors = ButtonDefaults.buttonColors(containerColor = ink, contentColor = Color.White),
                ) { Text("搜台湾小说网") }
                Text("继续浏览", color = ink, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("twkan", "cool18").forEach { id ->
                        val site = requireNotNull(PenrixSiteModuleRegistry.byId(id))
                        Card(onClick = {
                            val url = history.lastUrl(site)?.let { PenrixSiteModuleRegistry.safeResumeUrl(site, it) } ?: site.homeUrl
                            onOpenPage(site, url, true)
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
                val books = library
                when {
                    books == null -> Text("正在载入书架…", color = muted)
                    books.isEmpty() -> {
                        Text("书架还空着", color = ink, style = MaterialTheme.typography.titleLarge)
                        Text("打开章节后，点「收藏」或「阅读」即可加入书架。", color = muted)
                        Button(
                            onClick = { onOpenPage(twkan, twkanSearchUrl, false) },
                            colors = ButtonDefaults.buttonColors(containerColor = ink, contentColor = Color.White),
                        ) { Text("去台湾小说网找书") }
                    }
                    else -> books.forEachIndexed { index, chapter ->
                        val latest = index == 0
                        Card(
                            onClick = { read(chapter.getString("url")) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (latest) ink else Color.White),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (latest) {
                                    Text("上次阅读", color = Color(0xFFC5D2BC), style = MaterialTheme.typography.labelLarge)
                                }
                                Text(
                                    chapter.optString("workTitle").ifBlank { "未命名作品" },
                                    color = if (latest) Color.White else ink,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    chapter.optString("title"),
                                    color = if (latest) Color(0xFFD8E0D5) else muted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFFEFEEE7)).padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = { shelf = true }) { Text("书架", color = if (shelf) ink else muted, fontWeight = if (shelf) FontWeight.Bold else FontWeight.Normal) }
            TextButton(onClick = { shelf = false }) { Text("找书", color = if (!shelf) ink else muted, fontWeight = if (!shelf) FontWeight.Bold else FontWeight.Normal) }
            TextButton(onClick = onOpenTabs) { Text("标签页", color = muted) }
        }
    }
}
