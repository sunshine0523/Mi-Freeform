package com.sunshine.freeform.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * @author sunshine
 * @date 2021/1/31
 */
@Database(entities = [FreeFormAppsEntity::class], version = 5, exportSchema = false)
abstract class MyDatabase : RoomDatabase() {
    abstract val freeFormAppsDao: FreeFormAppsDao

    companion object {
        private var database: MyDatabase? = null

        @Synchronized
        fun getDatabase(context: Context): MyDatabase {
            if (database == null) {
                database = Room.databaseBuilder(context.applicationContext, MyDatabase::class.java, "database.db")
                    .allowMainThreadQueries()
                    .build()
            }
            return database!!
        }
    }
}