package com.sunshine.freeform.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * @author sunshine
 * @date 2021/1/31
 * 开启小窗数据库实体类
 */
@Entity
class NotificationAppsEntity(
//    @PrimaryKey(autoGenerate = true)
//    val id: Int,
    @PrimaryKey
    val packageName: String
)

