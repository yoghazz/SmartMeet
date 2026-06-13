package com.smartmeet.app.di

import android.content.Context
import androidx.room.Room
import com.smartmeet.app.data.db.SmartMeetDatabase
import com.smartmeet.app.data.db.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartMeetDatabase {
        return Room.databaseBuilder(
            context,
            SmartMeetDatabase::class.java,
            "smartmeet.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideSessionDao(db: SmartMeetDatabase): SessionDao = db.sessionDao()
}
