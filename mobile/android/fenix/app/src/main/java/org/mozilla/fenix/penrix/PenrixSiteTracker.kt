/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.penrix

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.ext.flow

/**
 * Observe the real Firefox selected tab during this activity's lifetime.
 *
 * Unlike saving the module's home URL on tap, this records where the user actually
 * navigated within an allowlisted site. Incognito tabs must not be persisted.
 */
object PenrixSiteTracker {
    fun start(scope: CoroutineScope, store: BrowserStore, context: Context): Job {
        val history = PenrixSiteHistory(context.applicationContext)
        return scope.launch {
            store.flow()
                .map { state ->
                    state.selectedTab?.content?.let { content ->
                        if (content.private) null else content.url
                    }
                }
                .distinctUntilChanged()
                .collect { url ->
                    if (url != null) history.record(url)
                }
        }
    }
}
