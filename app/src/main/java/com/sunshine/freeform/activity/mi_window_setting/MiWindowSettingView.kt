package com.sunshine.freeform.activity.mi_window_setting

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.sunshine.freeform.callback.ServiceStateListener
import com.sunshine.freeform.service.floating.FloatingService
import com.sunshine.freeform.service.floating.FreeFormConfig
import com.sunshine.freeform.service.notification.NotificationService
import com.sunshine.freeform.utils.InputEventUtils
import com.sunshine.freeform.utils.ShellUtils

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

        preferenceManager.findPreference<SwitchPreference>("switch_service")?.apply {
            //xposed模式无需开启服务
            val isXposed = sp.getInt("freeform_control_model", 1) == 2
            if (isXposed) {
                isChecked = true
                isEnabled = false
                summary = getString(R.string.xposed_donnot_start_service)
            }
            onPreferenceChangeListener = this@MiWindowSettingView
        }

        preferenceManager.findPreference<SwitchPreference>("switch_floating")?.apply {
            isEnabled = sp.getBoolean("switch_service", false)
            //注入失败，可能是xposed没有激活
            if (sp.getInt("freeform_control_model", 1) == 2 && !InputEventUtils().testXposedInjectMotionEvent()) {
                isChecked = false
                isEnabled = InputEventUtils().testXposedInjectMotionEvent()
                summary = getString(R.string.control_false)
            }

            onPreferenceChangeListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<SwitchPreference>("switch_notify")?.apply {
            isEnabled = sp.getBoolean("switch_service", false)
            //注入失败，可能是xposed没有激活
            if (sp.getInt("freeform_control_model", 1) == 2 && !InputEventUtils().testXposedInjectMotionEvent()) {
                isChecked = false
                isEnabled = InputEventUtils().testXposedInjectMotionEvent()
                summary = getString(R.string.control_false)
            }
            onPreferenceChangeListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<Preference>("setting_freeform_apps")?.apply {
            onPreferenceClickListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<Preference>("setting_notification_apps")?.apply {
            onPreferenceClickListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<Preference>("setting_freeform")?.apply {
            onPreferenceClickListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<Preference>("setting_floating")?.apply {
            onPreferenceClickListener = this@MiWindowSettingView
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
            "switch_floating" -> {
                if (newValue as Boolean) {
                    //如果通知服务已经启动，那么不需要启动了
                    if (!isShowNotification()) {
                        val builder = MaterialAlertDialogBuilder(requireContext())
                        builder.apply {
                            setTitle(getString(R.string.service_state_dialog_title))
                            setMessage(getString(R.string.service_state_dialog_message))
                            setCancelable(false)
                        }
                        val dialog = builder.create()
                        dialog.show()
                        FloatingService.listener = object : ServiceStateListener {
                            override fun onStart() {
                                dialog.cancel()
                            }

                            override fun onStop() {
                                dialog.cancel()
                                Looper.prepare()
                                Toast.makeText(requireContext(), getString(R.string.init_server_fail), Toast.LENGTH_LONG).show()
                                Looper.loop()
                            }

                        }
                    }

                    requireActivity().startService(Intent(requireActivity(), FloatingService::class.java))
                } else {
                    requireActivity().stopService(Intent(requireActivity(), FloatingService::class.java))
                }
            }
            "switch_notify" -> {
                if (newValue as Boolean) {
                    requireActivity().startService(Intent(requireActivity(), NotificationService::class.java))
                    //如果在显示悬浮窗，那就说明服务已经连接了，不需要在连接了
                    if (!isShowFloating()) {
                        val builder = MaterialAlertDialogBuilder(requireContext())
                        builder.apply {
                            setTitle(getString(R.string.service_state_dialog_title))
                            setMessage(getString(R.string.service_state_dialog_message))
                            setCancelable(false)
                        }
                        val dialog = builder.create()
                        dialog.show()

                        FreeFormConfig.init(object : ServiceStateListener {
                            override fun onStart() {
                                dialog.cancel()
                            }

                            override fun onStop() {
                                dialog.cancel()
                                Looper.prepare()
                                Toast.makeText(requireContext(), getString(R.string.init_server_fail), Toast.LENGTH_LONG).show()
                                Looper.loop()
                            }

                        }, getControlModel())
                    }
                } else {
                    if (!isShowFloating()) {
                        val pid = ShellUtils.execCommand(
                            "ps -ef | grep com.sunshine.freeform.Server | grep -v grep | awk '{print \$2}'",
                            true
                        ).successMsg
                        ShellUtils.execRootCmdSilent("kill -9 $pid")
                    }
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
                println("initFreeForm $e")
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

    private fun getControlModel(): Int {
        return sp.getInt("setting_freeform_control_model", 1)
    }

    private fun isShowFloating(): Boolean {
        return sp.getBoolean("switch_floating", false)
    }

    private fun isShowNotification(): Boolean {
        return sp.getBoolean("switch_notify", false)
    }
}