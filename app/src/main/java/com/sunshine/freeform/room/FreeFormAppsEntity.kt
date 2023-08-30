package com.sunshine.freeform.room

import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * @author sunshine
 * @date 2021/1/31
 * 开启小窗数据库实体类
 */
@Entity
class FreeFormAppsEntity(
    //用于排序
    @PrimaryKey(autoGenerate = true)
    val sortNum: Int,
    var packageName: String,
    var activityName: String,
    var userId: Int = 0
) {
    override fun toString(): String {
        return "FreeFormAppsEntity(sortNum=$sortNum, packageName='$packageName')"
    }
}

