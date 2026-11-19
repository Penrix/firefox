/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.penrix

import android.content.Context

/**
 * Per-site resume points. The real page, authentication and session remain Firefox-owned.
 * This stores only an allowlisted URL, never cookies, passwords or arbitrary browsing history.
 */
class PenrixSiteHistory(context: Context) {
    private val preferences by lazy {
        context.applicationContext.getSharedPreferences("penrix_site_module_history", Context.MODE_PRIVATE)
    }

    fun lastUrl(module: PenrixSiteModule): String? =
        preferences.getString("last_${module.id}", null)
            ?.takeIf { PenrixSiteModuleRegistry.forUrl(it)?.id == module.id }

    fun record(url: String) {
        val module = PenrixSiteModuleRegistry.forUrl(url) ?: return
        if (preferences.getString("last_${module.id}", null) == url) return
        preferences.edit().putString("last_${module.id}", url).apply()
    }
}
