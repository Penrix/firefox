/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.penrix

import java.net.URI
import java.util.Locale

/** A dedicated website experience, not a generic browser bookmark. */
enum class PenrixSiteKind(val label: String, val userJourney: String) {
    NOVEL("小说", "作品 → 目录 → 阅读 → 进度"),
    FORUM("论坛", "版块 → 帖子 → 楼层 → 返回"),
    COMIC("漫画", "作品 → 图集 → 连续阅读"),
    VIDEO("视频", "分类 → 播放 → 全屏 → 返回"),
}

/**
 * Configuration for an explicitly supported site.
 *
 * Adding a site here only registers its module entrance. Deep page adapters are developed
 * separately and must never be implied by the presence of a module.
 */
data class PenrixSiteModule(
    val id: String,
    val title: String,
    val homeUrl: String,
    val hosts: Set<String>,
    val kind: PenrixSiteKind,
    val description: String,
) {
    init {
        require(id.matches(Regex("[a-z0-9_-]+")))
        require(homeUrl.startsWith("https://"))
        require(hosts.isNotEmpty())
    }
}

/**
 * Small initial set copied from the existing Penrix-Browser SiteRegistry.
 *
 * This is intentionally not a migration of every old shortcut: only sites that are
 * candidates for full per-site journeys appear. The owner's final priority order
 * remains configurable by changing this list.
 */
object PenrixSiteModuleRegistry {
    val modules: List<PenrixSiteModule> = listOf(
        PenrixSiteModule(
            id = "69shuba",
            title = "69书吧",
            homeUrl = "https://www.69shuba.com/",
            hosts = setOf("69shuba.com"),
            kind = PenrixSiteKind.NOVEL,
            description = "作品、目录与连续阅读",
        ),
        PenrixSiteModule(
            id = "twkan",
            title = "TWKAN",
            homeUrl = "https://twkan.com/",
            hosts = setOf("twkan.com"),
            kind = PenrixSiteKind.NOVEL,
            description = "搜书、正文与阅读进度",
        ),
        PenrixSiteModule(
            id = "uukan",
            title = "UU看书",
            homeUrl = "https://uukanshu.cc/",
            hosts = setOf("uukanshu.cc", "uukan.org"),
            kind = PenrixSiteKind.NOVEL,
            description = "小说搜索、完整目录与章节阅读",
        ),
        PenrixSiteModule(
            id = "sis001",
            title = "SIS001",
            homeUrl = "https://www.sis001.com/forum/index.php",
            hosts = setOf("sis001.com"),
            kind = PenrixSiteKind.FORUM,
            description = "版块、帖子与返回位置",
        ),
        PenrixSiteModule(
            id = "wnacg",
            title = "WNACG",
            homeUrl = "https://www.wnacg.com/",
            hosts = setOf("wnacg.com", "wnacg.org"),
            kind = PenrixSiteKind.COMIC,
            description = "作品、图集与连续浏览",
        ),
        PenrixSiteModule(
            id = "iwara",
            title = "Iwara",
            homeUrl = "https://www.iwara.tv/profile/user937858",
            hosts = setOf("iwara.tv"),
            kind = PenrixSiteKind.VIDEO,
            description = "视频、播放与观看位置",
        ),
    )

    fun byId(id: String?): PenrixSiteModule? = modules.firstOrNull { it.id == id }

    /** Match complete host boundaries. Reject lookalike domains, invalid and non-HTTP URLs. */
    fun forUrl(url: String?): PenrixSiteModule? {
        if (url.isNullOrBlank()) return null
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("https", "http")) return null
        val host = uri.host?.lowercase(Locale.ROOT)?.trimEnd('.') ?: return null
        return modules.firstOrNull { module ->
            module.hosts.any { domain ->
                host == domain || host.endsWith(".$domain")
            }
        }
    }

    /** Never reopen a URL belonging to a different site (or a lookalike hostname). */
    fun safeResumeUrl(module: PenrixSiteModule, savedUrl: String?): String =
        if (forUrl(savedUrl)?.id == module.id) savedUrl!! else module.homeUrl
}
