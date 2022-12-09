package com.sunshine.freeform.service.notification

import android.content.Context
import com.sunshine.freeform.room.DatabaseRepository
import com.sunshine.freeform.room.NotificationAppsEntity
import kotlinx.coroutines.flow.Flow

/**
 * @author sunshine
 * @date 2022/1/6
 */
class NotificationViewModel(context: Context) {
    private val repository = DatabaseRepository(context)

    fun getAllNotificationApps(): Flow<List<NotificationAppsEntity>?> {
        return repository.getAllNotificationByFlow()
    }

}