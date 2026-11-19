/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.penrix

import android.content.Context
import android.util.Log
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.concept.engine.webextension.InstallationMethod
import org.mozilla.fenix.components.Components

/**
 * First-run installer for the official, signed Firefox/Android version of full uBlock Origin.
 *
 * The XPI is resolved at runtime from Mozilla Add-ons, rather than from an untrusted
 * mirror or a pinned download URL. Installation needs network access on first run,
 * and Fenix may request extension permissions. Users retain normal extension controls.
 *
 * A successful bootstrap is a one-time preference: uninstalling or disabling uBO
 * afterwards will not cause the app to silently reinstall/re-enable it.
 */
object PenrixAdblockBootstrap {
    private const val TAG = "PenrixAdblock"
    private const val PREFS = "penrix_adblock_bootstrap"
    private const val PREF_COMPLETED = "initial_install_completed"
    private const val PREF_STATUS = "initial_install_status"
    private const val UBO_ID = "uBlock0@raymondhill.net"
    private val started = AtomicBoolean(false)

    fun start(context: Context, components: Components, scope: CoroutineScope) {
        if (!started.compareAndSet(false, true)) return

        scope.launch {
            val prefs = withContext(Dispatchers.IO) {
                context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .takeUnless { it.getBoolean(PREF_COMPLETED, false) }
            } ?: return@launch
            val manager = components.addonManager
            try {
                // getAddonByID waits for GeckoView's installed extension state.
                val installed = manager.getAddonByID(UBO_ID)
                if (installed?.isInstalled() == true) {
                    prefs.edit().putBoolean(PREF_COMPLETED, true)
                        .putString(PREF_STATUS, "installed").apply()
                    return@launch
                }

                prefs.edit().putString(PREF_STATUS, "installing").apply()
                val addon = components.addonsProvider.getAddonByID(UBO_ID)
                val downloadUrl = addon?.takeIf { it.id == UBO_ID }?.downloadUrl

                if (!isOfficialAddonDownload(downloadUrl)) {
                    Log.w(TAG, "uBO metadata missing or download URL is not from Mozilla Add-ons")
                    prefs.edit().putString(PREF_STATUS, "metadata_unavailable").apply()
                    return@launch
                }

                manager.installAddon(
                    url = checkNotNull(downloadUrl),
                    installationMethod = InstallationMethod.MANAGER,
                    onSuccess = { installedAddon ->
                        if (installedAddon.id == UBO_ID && installedAddon.isEnabled()) {
                            prefs.edit().putBoolean(PREF_COMPLETED, true)
                                .putString(PREF_STATUS, "installed").apply()
                            Log.i(TAG, "Official uBlock Origin successfully installed")
                        } else {
                            // A failed/disabled extension is not an active ad blocker.
                            prefs.edit().putString(PREF_STATUS, "not_enabled").apply()
                            Log.w(TAG, "Installed extension is not active or has unexpected ID")
                        }
                    },
                    onError = { error ->
                        prefs.edit().putString(PREF_STATUS, "install_failed").apply()
                        Log.w(TAG, "uBlock Origin installation failed; retry on next launch", error)
                    },
                )
            } catch (error: Exception) {
                prefs.edit().putString(PREF_STATUS, "install_failed").apply()
                Log.w(TAG, "uBlock Origin setup failed; retry on next launch", error)
            }
        }
    }

    /** Download source is controlled by AMO; never accept a third-party extension mirror. */
    internal fun isOfficialAddonDownload(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val parsed = runCatching { URI(url) }.getOrNull() ?: return false
        return parsed.scheme.equals("https", ignoreCase = true) &&
            parsed.host?.lowercase() in setOf("addons.mozilla.org", "addons.cdn.mozilla.net") &&
            parsed.rawUserInfo == null
    }

    fun status(context: Context): String =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(PREF_STATUS, "not_started") ?: "not_started"
}
