/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.penrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PenrixSiteModuleRegistryTest {
    @Test
    fun `registered site recognizes its own host and subdomain`() {
        assertEquals("69shuba", PenrixSiteModuleRegistry.forUrl("https://www.69shuba.com/book/123")?.id)
        assertEquals("twkan", PenrixSiteModuleRegistry.forUrl("https://twkan.com/novel/1")?.id)
        assertEquals("wnacg", PenrixSiteModuleRegistry.forUrl("https://a.wnacg.org/photos")?.id)
    }

    @Test
    fun `lookalike domains and non-http schemes never become a site module`() {
        val invalid = listOf(
            "https://69shuba.com.attacker.test/",
            "https://fake69shuba.com/",
            "https://www.69shuba.com@evil.test/",
            "javascript:alert(1)",
            "file:///sdcard/file.txt",
            "https://example.com/",
            "",
        )
        invalid.forEach { assertNull(it, PenrixSiteModuleRegistry.forUrl(it)) }
    }

    @Test
    fun `resume URLs cannot cross module boundaries`() {
        val site = requireNotNull(PenrixSiteModuleRegistry.byId("69shuba"))
        assertEquals(
            "https://www.69shuba.com/book/123",
            PenrixSiteModuleRegistry.safeResumeUrl(site, "https://www.69shuba.com/book/123"),
        )
        assertEquals(
            site.homeUrl,
            PenrixSiteModuleRegistry.safeResumeUrl(site, "https://twkan.com/novel/1"),
        )
        assertEquals(site.homeUrl, PenrixSiteModuleRegistry.safeResumeUrl(site, null))
    }

    @Test
    fun `module IDs are unique and initial site URLs belong to their modules`() {
        val modules = PenrixSiteModuleRegistry.modules
        assertEquals(modules.size, modules.map { it.id }.distinct().size)
        assertTrue(modules.isNotEmpty())
        modules.forEach { module ->
            assertEquals(module.id, PenrixSiteModuleRegistry.forUrl(module.homeUrl)?.id)
        }
    }
}
