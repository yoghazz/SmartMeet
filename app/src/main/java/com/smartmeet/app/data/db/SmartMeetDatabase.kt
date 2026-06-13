package com.smartmeet.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartmeet.app.data.db.dao.SessionDao
import com.smartmeet.app.data.db.entities.SessionEntity

@Database(entities = [SessionEntity::class], version = 1, exportSchema = false)
abstract class SmartMeetDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
