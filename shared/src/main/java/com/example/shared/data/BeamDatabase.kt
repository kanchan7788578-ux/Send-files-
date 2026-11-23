package com.example.shared.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TransferRecordEntity::class], version = 1, exportSchema = false)
abstract class BeamDatabase : RoomDatabase() {
    abstract fun transferDao(): TransferDao

    companion object {
        @Volatile
        private var INSTANCE: BeamDatabase? = null

        fun getInstance(context: Context): BeamDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BeamDatabase::class.java,
                    "beamtv_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
