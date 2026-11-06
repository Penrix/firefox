/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.penrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class PenrixNovelRoutesTest {
    @Test
    fun `69shuba verified public pages`() {
        assertRoute("https://www.69shuba.com/book/90442.htm", "69shuba", PenrixNovelPageKind.WORK, "90442")
        assertRoute("https://www.69shuba.com/book/90442/", "69shuba", PenrixNovelPageKind.CATALOG, "90442")
        assertRoute(
            "https://www.69shuba.com/txt/90442/41051913",
            "69shuba",
            PenrixNovelPageKind.CHAPTER,
            "90442",
            "41051913",
        )
    }

    @Test
    fun `TWKAN verified public pages`() {
        assertRoute("https://twkan.com/book/82888.html", "twkan", PenrixNovelPageKind.WORK, "82888")
        assertRoute("https://twkan.com/book/82888/index.html", "twkan", PenrixNovelPageKind.CATALOG, "82888")
        assertRoute("https://twkan.com/txt/82888/49191968", "twkan", PenrixNovelPageKind.CHAPTER, "82888", "49191968")
    }

    @Test
    fun `UUkan combined book and catalog and individual chapter pages`() {
        assertRoute("https://uukanshu.cc/book/25312/", "uukan", PenrixNovelPageKind.WORK_AND_CATALOG, "25312")
        assertRoute(
            "https://uukanshu.cc/book/25312/16302351.html",
            "uukan",
            PenrixNovelPageKind.CHAPTER,
            "25312",
            "16302351",
        )
    }

    @Test
    fun `unrecognized pages and other domains never become chapters`() {
        assertNull(PenrixNovelRoutes.classify("https://69shuba.com.evil.test/txt/90442/41051913"))
        assertNull(PenrixNovelRoutes.classify("https://www.wnacg.com/"))
        assertEquals(
            PenrixNovelPageKind.UNKNOWN,
            PenrixNovelRoutes.classify("https://twkan.com/new-ui/chapter/42")?.pageKind,
        )
    }

    @Test
    fun `next chapter must be a genuine chapter in the same book`() {
        // The live 69shuba last chapter's "next" link points back to the work page.
        assertFalse(
            PenrixNovelRoutes.isNextChapter(
                "https://www.69shuba.com/txt/90442/41051913",
                "https://www.69shuba.com/book/90442.htm",
            )
        )
        assertTrue(
            PenrixNovelRoutes.isNextChapter(
                "https://twkan.com/txt/82888/49191968",
                "https://twkan.com/txt/82888/49191969",
            )
        )
        assertTrue(
            PenrixNovelRoutes.isNextChapter(
                "https://uukanshu.cc/book/25312/16302351.html",
                "https://uukanshu.cc/book/25312/16302352.html",
            )
        )
        assertFalse(
            PenrixNovelRoutes.isNextChapter(
                "https://twkan.com/txt/82888/49191968",
                "https://uukanshu.cc/book/25312/16302352.html",
            )
        )
    }

    private fun assertRoute(
        url: String,
        siteId: String,
        expectedKind: PenrixNovelPageKind,
        expectedWork: String? = null,
        expectedChapter: String? = null,
    ) {
        val route = requireNotNull(PenrixNovelRoutes.classify(url))
        assertEquals(siteId, route.siteId)
        assertEquals(expectedKind, route.pageKind)
        assertEquals(expectedWork, route.workKey)
        assertEquals(expectedChapter, route.chapterKey)
    }
}
