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
@Database(entities = [FreeFormAppsEntity::class, NotificationAppsEntity::class], version = 5, exportSchema = false)
abstract class MyDatabase : RoomDatabase() {
    abstract val freeFormAppsDao: FreeFormAppsDao
    abstract val notificationAppsDao: NotificationAppsDao

    companion object {
        private var database: MyDatabase? = null

        @Synchronized
        fun getDatabase(context: Context): MyDatabase {
            if (database == null) {
                database = Room.databaseBuilder(context.applicationContext, MyDatabase::class.java, "database.db")
                    .allowMainThreadQueries()
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .build()
            }
            return database!!
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS" +
                            "'CompatibleAppsEntity'" +
                            "('packageName' TEXT NOT NULL, PRIMARY KEY('packageName'))"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "DROP TABLE IF EXISTS 'CompatibleAppsEntity'"
                )
                database.execSQL(
                    "ALTER TABLE 'FreeFormAppsEntity'" +
                            "ADD 'sortNum' int NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE 'FreeFormAppsEntity' RENAME TO 'FreeFormAppsEntity_OLD'"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS" +
                            "'FreeFormAppsEntity'" +
                            "('sortNum' INTEGER NOT NULL, 'packageName' TEXT NOT NULL, PRIMARY KEY('sortNum'))"
                )
                database.execSQL(
                    "INSERT INTO 'FreeFormAppsEntity'('packageName')" +
                            "SELECT packageName from 'FreeFormAppsEntity_OLD'"
                )
                database.execSQL(
                    "DROP TABLE 'FreeFormAppsEntity_OLD'"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE 'FreeFormAppsEntity' ADD COLUMN 'userId' INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE 'NotificationAppsEntity' ADD COLUMN 'userId' INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}