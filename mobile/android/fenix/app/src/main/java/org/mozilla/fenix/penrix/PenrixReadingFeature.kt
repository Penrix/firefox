/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.penrix

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.webextension.MessageHandler
import mozilla.components.concept.engine.webextension.Port
import mozilla.components.lib.state.ext.flow
import mozilla.components.support.webextensions.BuiltInWebExtensionController
import org.json.JSONObject

object PenrixReadingFeature {
    fun install(context: Context, engine: Engine, store: BrowserStore, scope: CoroutineScope) {
        val app = context.applicationContext
        val controller = BuiltInWebExtensionController("reading@penrix.local", "resource://android/assets/extensions/penrix/", "penrixReading")
        val reading = PenrixReadingStore(app)
        val bindings = mutableMapOf<String, EngineSession>()
        controller.install(engine, onSuccess = {
            scope.launch {
                store.flow().collect { state ->
                    bindings.keys.retainAll(state.tabs.map { it.id }.toSet())
                    state.tabs.filterNot { it.content.private }.forEach { tab ->
                        val session = tab.engineState.engineSession ?: return@forEach
                        if (bindings[tab.id] === session) return@forEach
                        bindings[tab.id] = session
                        controller.registerContentMessageHandler(session, object : MessageHandler {
                            override fun onPortMessage(message: Any, port: Port) {
                                val payload = message as? JSONObject ?: return
                                val chapter = payload.optJSONObject("chapter") ?: return
                                val current = store.state.tabs.firstOrNull { it.id == tab.id } ?: return
                                val url = chapter.optString("url")
                                val site = PenrixSiteModuleRegistry.forUrl(current.content.url) ?: return
                                if (current.content.private || current.content.url != url || site.id != chapter.optString("siteId")) return
                                val type = payload.optString("type")
                                reading.save(chapter, addToLibrary = type == "save" || type == "read")
                                val prefs = app.getSharedPreferences("penrix_reader", Context.MODE_PRIVATE)
                                val pending = prefs.getString("pending_url", null)
                                if (store.state.selectedTabId == tab.id && (type == "read" || type == "chapter_ready" && pending == url)) {
                                    val flags = if (pending == url) prefs.getInt("pending_reader_flags", 0) else 0
                                    prefs.edit().remove("pending_url").remove("pending_reader_flags").apply()
                                    app.startActivity(Intent(app, PenrixReaderActivity::class.java).putExtra("chapter_url", url)
                                        .putExtra("reader_flags", flags).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                } else if (type == "save") {
                                    Toast.makeText(app, "已加入书架", Toast.LENGTH_SHORT).show()
                                }
                            }
                        })
                    }
                }
            }
        }, onError = { Log.e("PenrixReading", "Extension installation failed", it) })
    }
}
