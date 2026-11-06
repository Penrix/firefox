/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.penrix

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.Closeable
import org.json.JSONObject

class PenrixReadingStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "penrix-reading.db", null, 2), Closeable {
    data class Position(val pixels: Int, val offset: Int)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE chapters(url TEXT PRIMARY KEY, payload TEXT NOT NULL, scroll INTEGER NOT NULL DEFAULT 0, text_offset INTEGER NOT NULL DEFAULT -1)")
        db.execSQL("CREATE TABLE library(work_url TEXT PRIMARY KEY, title TEXT NOT NULL, chapter_url TEXT NOT NULL, updated INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE chapters ADD COLUMN text_offset INTEGER NOT NULL DEFAULT -1")
    }

    fun save(chapter: JSONObject, addToLibrary: Boolean = false) {
        val db = writableDatabase
        db.insertWithOnConflict("chapters", null, ContentValues().apply {
            put("url", chapter.getString("url"))
            put("payload", chapter.toString())
        }, SQLiteDatabase.CONFLICT_IGNORE)
        db.update("chapters", ContentValues().apply { put("payload", chapter.toString()) }, "url=?", arrayOf(chapter.getString("url")))
        val work = chapter.getString("workUrl")
        if (addToLibrary) {
            db.insertWithOnConflict("library", null, ContentValues().apply {
                put("work_url", work)
                put("title", chapter.getString("workTitle"))
                put("chapter_url", chapter.getString("url"))
                put("updated", System.currentTimeMillis())
            }, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun chapter(url: String): JSONObject? = readableDatabase.rawQuery("SELECT payload FROM chapters WHERE url=?", arrayOf(url)).use {
        if (it.moveToFirst()) JSONObject(it.getString(0)) else null
    }

    fun position(url: String): Position = readableDatabase.rawQuery("SELECT scroll, text_offset FROM chapters WHERE url=?", arrayOf(url)).use {
        if (it.moveToFirst()) Position(it.getInt(0), it.getInt(1)) else Position(0, -1)
    }

    fun position(url: String, scroll: Int, offset: Int) {
        writableDatabase.update("chapters", ContentValues().apply {
            put("scroll", scroll)
            put("text_offset", offset)
        }, "url=?", arrayOf(url))
    }

    fun library(): List<JSONObject> = readableDatabase.rawQuery("SELECT chapter_url FROM library ORDER BY updated DESC", null).use {
        buildList { while (it.moveToNext()) chapter(it.getString(0))?.let(::add) }
    }
}
