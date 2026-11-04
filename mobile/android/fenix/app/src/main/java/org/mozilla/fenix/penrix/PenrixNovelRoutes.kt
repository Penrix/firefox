/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.penrix

import java.net.URI

/** Only a URL route classifier. Extracting actual work/chapter content is separate. */
enum class PenrixNovelPageKind {
    HOME,
    WORK,
    CATALOG,
    WORK_AND_CATALOG,
    CHAPTER,
    UNKNOWN,
}

data class PenrixNovelRoute(
    val siteId: String,
    val pageKind: PenrixNovelPageKind,
    val workKey: String? = null,
    val chapterKey: String? = null,
)

/**
 * Known route structures inspected against live pages on 2026-11-04.
 * Don't assume the same URL pattern across sites; a site redesign is UNKNOWN,
 * never silently treated as a readable chapter.
 */
object PenrixNovelRoutes {
    private val numericChapter = Regex("^/txt/(\\d+)/(\\d+)/?$")
    private val bookHtml = Regex("^/book/(\\d+)\\.html?$")
    private val workDirectory = Regex("^/book/(\\d+)/?$")
    private val twkanDirectory = Regex("^/book/(\\d+)/index\\.html$")
    private val uukanChapter = Regex("^/book/(\\d+)/(\\d+)\\.html$")

    fun classify(url: String?): PenrixNovelRoute? {
        val site = PenrixSiteModuleRegistry.forUrl(url) ?: return null
        if (site.kind != PenrixSiteKind.NOVEL) return null
        val path = runCatching { URI(url).path }.getOrNull() ?: return null

        fun route(kind: PenrixNovelPageKind, work: String? = null, chapter: String? = null) =
            PenrixNovelRoute(site.id, kind, work, chapter)

        if (path == "/") return route(PenrixNovelPageKind.HOME)

        return when (site.id) {
            "69shuba" -> {
                numericChapter.matchEntire(path)?.let { route(PenrixNovelPageKind.CHAPTER, it.groupValues[1], it.groupValues[2]) }
                    ?: Regex("^/book/(\\d+)\\.htm$").matchEntire(path)?.let { route(PenrixNovelPageKind.WORK, it.groupValues[1]) }
                    ?: workDirectory.matchEntire(path)?.let { route(PenrixNovelPageKind.CATALOG, it.groupValues[1]) }
                    ?: route(PenrixNovelPageKind.UNKNOWN)
            }
            "twkan" -> {
                numericChapter.matchEntire(path)?.let { route(PenrixNovelPageKind.CHAPTER, it.groupValues[1], it.groupValues[2]) }
                    ?: twkanDirectory.matchEntire(path)?.let { route(PenrixNovelPageKind.CATALOG, it.groupValues[1]) }
                    ?: bookHtml.matchEntire(path)?.let { route(PenrixNovelPageKind.WORK, it.groupValues[1]) }
                    ?: route(PenrixNovelPageKind.UNKNOWN)
            }
            "uukan" -> {
                uukanChapter.matchEntire(path)?.let { route(PenrixNovelPageKind.CHAPTER, it.groupValues[1], it.groupValues[2]) }
                    ?: workDirectory.matchEntire(path)?.let { route(PenrixNovelPageKind.WORK_AND_CATALOG, it.groupValues[1]) }
                    ?: route(PenrixNovelPageKind.UNKNOWN)
            }
            else -> route(PenrixNovelPageKind.UNKNOWN)
        }
    }
}
