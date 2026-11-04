/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.penrix

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Penrix-specific start screen. Each site opens a dedicated module screen first.
 *
 * Site-specific page adapters (reader, forum thread, comics, media) attach to this
 * module contract in later milestones. This scaffold deliberately does not pretend
 * a generic URL launch is already a deep native site experience.
 */
@Composable
fun PenrixSiteHome(
    history: PenrixSiteHistory,
    onOpenPage: (PenrixSiteModule, String) -> Unit,
) {
    var currentId by rememberSaveable { mutableStateOf<String?>(null) }
    val currentModule = PenrixSiteModuleRegistry.byId(currentId)
    BackHandler(enabled = currentModule != null) { currentId = null }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (currentModule == null) {
            Text("Penrix Private", style = MaterialTheme.typography.headlineMedium)
            Text(
                "我的站点 · 每个网站独立使用，原网页作为备用入口",
                style = MaterialTheme.typography.bodyMedium,
            )
            PenrixSiteModuleRegistry.modules.groupBy { it.kind }.forEach { (kind, sites) ->
                Text(kind.label, style = MaterialTheme.typography.titleMedium)
                sites.forEach { module ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(module.title, style = MaterialTheme.typography.titleLarge)
                            Text(module.description, style = MaterialTheme.typography.bodyMedium)
                            Button(onClick = { currentId = module.id }) {
                                Text("进入模块")
                            }
                        }
                    }
                }
            }
        } else {
            OutlinedButton(onClick = { currentId = null }) {
                Text("返回全部模块")
            }
            Text(currentModule.title, style = MaterialTheme.typography.headlineMedium)
            Text(currentModule.kind.userJourney, style = MaterialTheme.typography.titleMedium)
            Text(currentModule.description, style = MaterialTheme.typography.bodyMedium)

            val lastUrl = history.lastUrl(currentModule)
            if (lastUrl != null) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onOpenPage(
                            currentModule,
                            PenrixSiteModuleRegistry.safeResumeUrl(currentModule, lastUrl),
                        )
                    },
                ) {
                    Text("继续上次浏览")
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenPage(currentModule, currentModule.homeUrl) },
            ) {
                Text("打开站点首页")
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("模块功能", style = MaterialTheme.typography.titleMedium)
                    Text(
                        when (currentModule.kind) {
                            PenrixSiteKind.NOVEL -> "搜书、作品目录、原生阅读器和书架将接入这里。"
                            PenrixSiteKind.FORUM -> "版块导航、帖子阅读和列表位置恢复将接入这里。"
                            PenrixSiteKind.COMIC -> "作品图集、连续看图和阅读进度将接入这里。"
                            PenrixSiteKind.VIDEO -> "分类、播放器和观看位置将接入这里。"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "当前为模块框架：已提供独立入口、站内状态记录和真实网页通道；" +
                            "内容级优化仍待逐站实现。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
