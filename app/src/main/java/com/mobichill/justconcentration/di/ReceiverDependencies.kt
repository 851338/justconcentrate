package com.mobichill.justconcentration.di

import com.mobichill.justconcentration.helper.AlarmHelper
import com.mobichill.justconcentration.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Suppress("unused")
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReceiverDependencies {
    fun taskRepository(): TaskRepository
    fun alarmHelper(): AlarmHelper
}