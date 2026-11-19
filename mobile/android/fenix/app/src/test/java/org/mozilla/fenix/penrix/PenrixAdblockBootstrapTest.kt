/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.penrix

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PenrixAdblockBootstrapTest {
    @Test
    fun `only official Mozilla HTTPS download endpoints are allowed`() {
        assertTrue(
            PenrixAdblockBootstrap.isOfficialAddonDownload(
                "https://addons.mozilla.org/firefox/downloads/file/1000/ublock_origin.xpi"
            )
        )
        assertTrue(
            PenrixAdblockBootstrap.isOfficialAddonDownload(
                "https://addons.cdn.mozilla.net/user-media/addons/607454/ublock_origin.xpi"
            )
        )
    }

    @Test
    fun `a third party or an insecure download URL is refused`() {
        val invalid = listOf(
            "https://addons.mozilla.org.evil.example/ublock.xpi",
            "https://other.example/ublock.xpi",
            "http://addons.mozilla.org/firefox/downloads/file/1000/ublock.xpi",
            "https://addons.mozilla.org@evil.example/ublock.xpi",
            "file:///sdcard/Download/ublock.xpi",
            null,
        )
        invalid.forEach { assertFalse(PenrixAdblockBootstrap.isOfficialAddonDownload(it)) }
    }
}
