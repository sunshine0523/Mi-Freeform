package com.sunshine.freeform.activity.mi_window_setting

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
import android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import android.widget.Toast

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import com.sunshine.freeform.R
import com.sunshine.freeform.activity.choose_free_form_apps.ChooseAppsActivity
import com.sunshine.freeform.activity.floating_setting.FloatingSettingActivity
import com.sunshine.freeform.activity.free_form_setting.FreeFormSettingActivity
import com.sunshine.freeform.callback.FreeFormListener
import com.sunshine.freeform.service.FloatingService
import com.sunshine.freeform.activity.senior.SeniorActivity
import com.sunshine.freeform.hook.service.MiFreeFormService
import com.sunshine.freeform.service.Floating2Service
import com.sunshine.freeform.service.ForegroundService
import com.sunshine.freeform.service.NotificationService
import com.sunshine.freeform.utils.ServiceUtils
import com.sunshine.freeform.utils.ShellUtils
import com.sunshine.freeform.utils.TagUtils

import java.io.File
import java.io.FileOutputStream

/**
 * @author sunshine
 * @date 2021/1/31
 * 小窗设置View
 */
class MiWindowSettingView : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private lateinit var sp: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.mi_window_setting, rootKey)

        sp = requireContext().getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)

        preferenceManager.findPreference<SwitchPreference>("switch_use_system_freeform")?.apply {
            onPreferenceChangeListener = this@MiWindowSettingView
        }

        preferenceManager.findPreference<SwitchPreference>("switch_floating")?.apply {
            onPreferenceChangeListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<SwitchPreference>("switch_notify")?.apply {
            onPreferenceChangeListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<Preference>("setting_freeform_apps")?.apply {
            onPreferenceClickListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<Preference>("setting_floating_compatible")?.apply {
            onPreferenceClickListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<Preference>("battery")?.apply {
            onPreferenceClickListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<SwitchPreference>("switch_foreground_service")?.apply {
            if (MiFreeFormService.getClient() == null) {
                isChecked = true
                isEnabled = false
                summary = getString(R.string.foreground_need_running)
            } else {
                isEnabled = true
                summary = getString(R.string.foreground_service_subtitle)
                onPreferenceChangeListener = this@MiWindowSettingView
            }
        }
        preferenceManager.findPreference<Preference>("senior")?.apply {
            onPreferenceClickListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<Preference>("setting_notification_apps")?.apply {
            onPreferenceClickListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<Preference>("setting_freeform")?.apply {
            isEnabled = !sp.getBoolean("switch_use_system_freeform", false)
            onPreferenceClickListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<Preference>("setting_floating")?.apply {
            onPreferenceClickListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<SwitchPreference>("switch_preference_floating_only_enable_landscape")?.apply {
            onPreferenceChangeListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<SwitchPreference>("switch_preference_only_enable_landscape")?.apply {
            onPreferenceChangeListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<SwitchPreference>("switch_preference_freeform_only_enable_landscape")?.apply {
            onPreferenceChangeListener = this@MiWindowSettingView
        }
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        when(preference?.key) {
            "setting_freeform_apps" -> {
                startActivity(Intent(context, ChooseAppsActivity::class.java).putExtra("type", 1))
            }
            "setting_floating_compatible" -> {
                startActivity(Intent(context, ChooseAppsActivity::class.java).putExtra("type", 3))
            }
            "battery" -> {
                ignoreBatteryOptimization()
            }
            "senior" -> {
                requireContext().startActivity(Intent(requireContext(), SeniorActivity::class.java))
            }
            "setting_notification_apps" -> {
                startActivity(Intent(context, ChooseAppsActivity::class.java).putExtra("type", 2))
            }
            "setting_freeform" -> {
                startActivity(Intent(context, FreeFormSettingActivity::class.java))
            }
            "setting_floating" -> {
                startActivity(Intent(context, FloatingSettingActivity::class.java))
            }
        }
        return true
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        when(preference?.key) {
            "switch_use_system_freeform" -> {
                preferenceManager.findPreference<Preference>("setting_freeform")?.isEnabled =
                    !(newValue as Boolean)
            }
            "switch_service" -> {
                //开启服务状态
                if (newValue as Boolean) {
                    //提示对话框
                    val builder = MaterialAlertDialogBuilder(requireActivity())
                    builder.apply {
                        setTitle(getString(R.string.service_state_dialog_title))
                        setMessage(getString(R.string.service_state_dialog_message))
                    }
                    val dialog = builder.create()
                    dialog.show()

                    Thread {
                        initFreeForm(object : FreeFormListener {
                            override fun onSuccess() {
                                dialog.cancel()
                                Handler(Looper.getMainLooper()).post {
                                    preferenceManager.findPreference<SwitchPreference>("switch_floating")?.isEnabled = true
                                    preferenceManager.findPreference<SwitchPreference>("switch_notify")?.isEnabled = true
                                }
                            }

                            override fun onFail() {
                                dialog.cancel()
                                Looper.prepare()
                                Toast.makeText(
                                        requireContext(),
                                        getString(R.string.init_server_fail),
                                        Toast.LENGTH_LONG
                                ).show()
                                Looper.loop()
                            }
                        })
                    }.start()
                } else {
                    requireActivity().stopService(
                            Intent(
                                    requireContext().applicationContext,
                                    FloatingService::class.java
                            )
                    )
                    requireActivity().stopService(
                        Intent(
                            requireContext().applicationContext,
                            NotificationService::class.java
                        )
                    )
                    //先关闭本地与服务连接，再关闭服务
                    preferenceManager.findPreference<SwitchPreference>("switch_floating")?.apply {
                        isEnabled = false
                        isChecked = false
                    }
                    preferenceManager.findPreference<SwitchPreference>("switch_notify")?.apply {
                        isEnabled = false
                        isChecked = false
                    }

                    //关闭服务进程
                    val pid = ShellUtils.execCommand(
                            "ps -ef | grep com.sunshine.freeform.Server | grep -v grep | awk '{print \$2}'",
                            true
                    ).successMsg
                    ShellUtils.execRootCmdSilent("kill -9 $pid")
                }
            }

            "switch_foreground_service" -> {
                if (newValue as Boolean) {
                    requireContext().startForegroundService(Intent(requireContext(), ForegroundService::class.java))
                } else {
                    requireContext().stopService(Intent(requireContext(), ForegroundService::class.java))
                }
            }

            "switch_floating" -> {
                if (newValue as Boolean) {
                    //有悬浮窗权限
                    if (Settings.canDrawOverlays(requireContext())) {
//                        //如果通知服务已经启动，那么不需要启动了
//                        if (!isShowNotification() && !FreeFormUtils.socketStartSuccess) {
//                            val builder = MaterialAlertDialogBuilder(requireContext())
//                            builder.apply {
//                                setTitle(getString(R.string.service_state_dialog_title))
//                                setMessage(getString(R.string.service_state_dialog_message))
//                                setCancelable(false)
//                            }
//                            val dialog = builder.create()
//                            dialog.show()
//
////                            FreeFormUtils.init(object : ServiceStateListener {
////                                override fun onStart() {
////                                    dialog.cancel()
////                                }
////
////                                override fun onStop() {
////                                    dialog.cancel()
////                                    Looper.prepare()
////                                    Toast.makeText(requireContext(), getString(R.string.init_server_fail), Toast.LENGTH_LONG).show()
////                                    Looper.loop()
////                                }
////
////                            }, getControlModel())
//                        }
                        requireContext().startService(Intent(requireContext(), Floating2Service::class.java))
                    } else {
                        //没有悬浮窗权限
                        try {
                            Toast.makeText(requireContext(), "请选择米窗开启“显示在其他应用界面的上层”权限", Toast.LENGTH_SHORT).show()
                            requireContext().startActivity(
                                    Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${requireContext().packageName}")
                                    )
                            )
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "无法跳转到设置界面，请手动前往设置开启米窗的“显示在其他应用界面的上层”权限", Toast.LENGTH_LONG).show()
                        }

                        return false
                    }

                } else {
                    requireContext().stopService(Intent(requireContext(), Floating2Service::class.java))
                }
            }
            "switch_notify" -> {
                if (newValue as Boolean) {
                    if (hasNotificationPermission()) {
//                        //如果在显示悬浮窗，那就说明服务已经连接了，不需要在连接了
//                        if (!isShowFloating() && !FreeFormUtils.socketStartSuccess) {
//                            val builder = MaterialAlertDialogBuilder(requireContext())
//
//                            builder.apply {
//                                setTitle(getString(R.string.service_state_dialog_title))
//                                setMessage(getString(R.string.service_state_dialog_message))
//                                setCancelable(false)
//                            }
//                            val dialog = builder.create()
//                            dialog.show()
//
////                            FreeFormUtils.init(object : ServiceStateListener {
////                                override fun onStart() {
////                                    dialog.cancel()
////                                }
////
////                                override fun onStop() {
////                                    dialog.cancel()
////                                    Looper.prepare()
////                                    Toast.makeText(requireContext(), getString(R.string.init_server_fail), Toast.LENGTH_LONG).show()
////                                    Looper.loop()
////                                }
////
////                            }, getControlModel())
//                        }
                        requireContext().startService(Intent(requireContext(), NotificationService::class.java))
                    } else {
                        //无通知使用权，去获取
                        try {
                            Toast.makeText(requireContext(), "请开启“通知使用权”权限", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }catch (e: Exception) {
                            Toast.makeText(requireContext(), "无法跳转到设置界面，请手动前往设置开启米窗的“通知使用权”权限", Toast.LENGTH_LONG).show()
                        }

                        return false
                    }

                } else {
                    requireContext().stopService(Intent(requireContext(), NotificationService::class.java))
                }
            }

            "switch_preference_floating_only_enable_landscape" -> {
                if (sp.getBoolean("switch_floating", false)) {
                    requireActivity().stopService(Intent(requireActivity(), FloatingService::class.java))
                    requireActivity().startService(Intent(requireActivity(), FloatingService::class.java))
                }
            }

            "switch_preference_only_enable_landscape" -> {
                NotificationService.onlyEnableLandscape = newValue as Boolean
            }
        }
        return true
    }

    //初始化小窗服务端
    private fun initFreeForm(callBack: FreeFormListener) {
        val serverVersion = sp.getLong("server_version", -1)
        //如果存在，直接开启，不存在就复制过去，如果软件更新了，服务端可能也更新，所以要重新创建
        val longVersionCode = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).longVersionCode

        val localFile = ShellUtils.execCommand("find /data/local/tmp/freeform-server.jar", true)
        //localFile.result == 1说明文件不存在
        if (serverVersion < longVersionCode || localFile.result == 1) {
            try {
                sp.edit().putLong("server_version", longVersionCode).apply()
                val serverFile = File("${requireContext().filesDir.absolutePath}/freeform-server.jar")
                if (serverFile.exists()) serverFile.delete()
                serverFile.createNewFile()
                val fos = FileOutputStream(serverFile, false)
                val inputStream = requireContext().assets.open("freeform-server.jar")
                var hasRead: Int
                while (inputStream.read().also { hasRead = it } != -1) {
                    fos.write(hasRead)
                }
                inputStream.close()
                fos.close()
                ShellUtils.execRootCmdSilent("rm -rf /data/local/tmp/freeform-server.jar")
                ShellUtils.execRootCmdSilent("mv /data/user/0/com.sunshine.freeform/files/freeform-server.jar /data/local/tmp/freeform-server.jar")
                ShellUtils.execRootCmdSilent("chmod 4755 /data/local/tmp/freeform-server.jar")
            }catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.release_file_fail), Toast.LENGTH_LONG).show()
            }
        }
        //使用nohup 并且将输出重定向到null，这样就不会打印日志了，因为这个打印日志会阻塞主线程
        ShellUtils.execRootCmdSilent("CLASSPATH=/data/local/tmp/freeform-server.jar nohup app_process / com.sunshine.freeform.Server >/dev/null 2>&1 &")

        callBack.onSuccess()
    }

    /**
     * 远程服务是否开启
     */
    private fun serviceIsClose(): Boolean {
        val pid = ShellUtils.execCommand("ps -ef | grep com.sunshine.freeform.Server | grep -v grep | awk '{print \$2}'", true).successMsg
        return pid.isNullOrBlank()
    }

    private fun hasNotificationPermission(): Boolean {
        return ServiceUtils.isServiceWork(requireContext(), "${requireContext().packageName}.service.notification.NotificationService")
    }

    private fun getControlModel(): Int {
        //如果是在酷安发布的，只能使用xposed控制
        if (TagUtils.RELEASE_MODE == TagUtils.COOLAPK_MODE) return 2
        return sp.getInt("setting_freeform_control_model", 2)
    }

    private fun isShowFloating(): Boolean {
        return sp.getBoolean("switch_floating", false)
    }

    private fun isShowNotification(): Boolean {
        return sp.getBoolean("switch_notify", false)
    }

    /**
     * 忽略电池优化
     */
    private fun ignoreBatteryOptimization() {
        val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        var hasIgnored = false
        hasIgnored = powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)
        //  判断当前APP是否有加入电池优化的白名单，如果没有，弹出加入电池优化的白名单的设置对话框。
        if (!hasIgnored) {
            //未加入电池优化的白名单 则弹出系统弹窗供用户选择(这个弹窗也是一个页面)
            val intent = Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:" + requireContext().packageName)
            startActivity(intent)
        } else{
            //已加入电池优化的白名单 则进入系统电池优化页面
            val powerUsageIntent = Intent(ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            val resolveInfo = requireContext().packageManager.resolveActivity(powerUsageIntent, 0)
            //判断系统是否有这个页面
            if (resolveInfo != null) {
                startActivity(powerUsageIntent)
            }
        }
    }
}