package com.sunshine.freeform.ui.app_list

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sunshine.freeform.MiFreeform
import com.sunshine.freeform.room.DatabaseRepository
import com.sunshine.freeform.room.FreeFormAppsEntity
import com.sunshine.freeform.utils.contains
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * @author KindBrave
 * @since 2023/8/25
 */
class AppListViewModel(private val application: Application): AndroidViewModel(application) {
    private val repository = DatabaseRepository(application)
    private val allAppList = ArrayList<AppInfo>()
    val appListLiveData: LiveData<List<AppInfo>>
        get() = _appList
    private val _appList = MutableLiveData<List<AppInfo>>()
    private val _finishActivity = MutableLiveData<Boolean>()
    val finishActivity: LiveData<Boolean> = _finishActivity

    private val launcherApps: LauncherApps = application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager: UserManager = application.getSystemService(Context.USER_SERVICE) as UserManager
    private val userHandleMap = HashMap<Int, UserHandle>()

    private val sp = application.applicationContext.getSharedPreferences(MiFreeform.CONFIG, Context.MODE_PRIVATE)
    val screenWidth = min(application.resources.displayMetrics.widthPixels, application.resources.displayMetrics.heightPixels)
    val screenHeight = max(application.resources.displayMetrics.widthPixels, application.resources.displayMetrics.heightPixels)
    val screenDensityDpi = application.resources.displayMetrics.densityDpi

    init {
        userManager.userProfiles.forEach {
            userHandleMap[com.sunshine.freeform.systemapi.UserHandle.getUserId(it)] = it
        }
        getAppList()
    }

    /**
     * use in IO
     */
    private fun getAllFreeFormApps(): List<FreeFormAppsEntity> {
        return repository.getAllFreeFormWithoutLiveData() ?: ArrayList()
    }

    private fun getAppList() {
        viewModelScope.launch(Dispatchers.IO) {
            val freeformList = getAllFreeFormApps()
            userManager.userProfiles.forEach { userHandle ->
                val list = launcherApps.getActivityList(null, userHandle)
                list.forEach {info ->
                    val packageName = info.componentName.packageName
                    val activityName = info.componentName.className
                    val userId = com.sunshine.freeform.systemapi.UserHandle.getUserId(userHandle)
                    val isFreeformApp = freeformList.contains(FreeFormAppsEntity(-1, packageName, activityName, userId)) { info1, info2 ->
                        info1.packageName == info2.packageName &&
                        info1.activityName == info2.activityName &&
                        info1.userId == info2.userId
                    }
                    allAppList.add(AppInfo(
                        "${info.label}${if (com.sunshine.freeform.systemapi.UserHandle.getUserId(userHandle) != 0) -com.sunshine.freeform.systemapi.UserHandle.getUserId(userHandle) else ""}",
                        info.applicationInfo.loadIcon(application.packageManager),
                        info.componentName,
                        userId,
                        isFreeformApp
                    ))
                }
            }
            _appList.postValue(allAppList)
        }
    }

    fun filterApp(filter: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (filter.isBlank()) _appList.postValue(allAppList)
            else {
                val filterAppList = allAppList.filter { appInfo ->
                    appInfo.label.contains(filter, true)
                }
                _appList.postValue(filterAppList)
            }
        }
    }

    fun addFreeformApp(packageName: String, activityName: String, userId: Int) {
        repository.insertFreeForm(packageName, activityName, userId)
    }

    fun removeFreeformApp(packageName: String, activityName: String, userId: Int) {
        repository.deleteFreeForm(packageName, activityName, userId)
    }

    fun closeActivity() {
        _finishActivity.value = true
    }

    fun getIntSp(name: String, defaultValue: Int): Int {
        return sp.getInt(name, defaultValue)
    }
}