package com.sunshine.freeform.ui.app_list

import android.app.Application
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sunshine.freeform.MiFreeform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * @author KindBrave
 * @since 2023/8/25
 */
class AppListViewModel(private val application: Application): AndroidViewModel(application) {
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

    private fun getAppList() {
        viewModelScope.launch(Dispatchers.IO) {
            userManager.userProfiles.forEach { userHandle ->
                val list = launcherApps.getActivityList(null, userHandle)
                list.forEach {info ->
                    allAppList.add(AppInfo(
                        info.label.toString(),
                        info.applicationInfo.loadIcon(application.packageManager),
                        info.componentName,
                        com.sunshine.freeform.systemapi.UserHandle.getUserId(userHandle)
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

    fun closeActivity() {
        _finishActivity.value = true
    }

    fun getIntSp(name: String, defaultValue: Int): Int {
        return sp.getInt(name, defaultValue)
    }
}