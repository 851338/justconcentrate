package com.mobichill.justconcentration.di

import android.content.Context
import com.mobichill.justconcentration.base.database.MyRoomDatabase
import com.mobichill.justconcentration.dao.BadgeDAO
import com.mobichill.justconcentration.dao.ConcentrateSessionDAO
import com.mobichill.justconcentration.dao.TaskDAO
import com.mobichill.justconcentration.dao.UserDAO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMyRoomDatabase(@ApplicationContext context: Context): MyRoomDatabase {
        return MyRoomDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideTaskDao(database: MyRoomDatabase): TaskDAO {
        return database.taskDAO
    }

    @Provides
    @Singleton
    fun provideUserDao(database: MyRoomDatabase): UserDAO {
        return database.userDAO
    }

    @Provides
    @Singleton
    fun provideConcentrateSessionDao(database: MyRoomDatabase): ConcentrateSessionDAO {
        return database.concentrateSessionDAO
    }

    @Provides
    @Singleton
    fun provideBadgeDao(database: MyRoomDatabase): BadgeDAO {
        return database.badgeDAO
    }
}