package com.easypeasy.textedit.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EntryDao {
    @Query("SELECT * FROM entry ORDER BY timestamp DESC")
    fun getLastEntry(): List<Entry?>

    @Insert
    fun insert(entry: Entry)

    @Delete
    fun delete(entry: Entry)
}