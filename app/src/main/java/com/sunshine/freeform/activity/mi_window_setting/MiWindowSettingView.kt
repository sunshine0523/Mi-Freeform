package com.sunshine.freeform.activity.mi_window_setting

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
import android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import com.sunshine.freeform.R
import com.sunshine.freeform.activity.choose_free_form_apps.ChooseAppsActivity
import com.sunshine.freeform.activity.floating_setting.FloatingSettingActivity
import com.sunshine.freeform.service.CoreService
import com.sunshine.freeform.service.NotificationService
import com.sunshine.freeform.view.floating.FreeFormView

/**
 * @author sunshine
 * @date 2021/1/31
 * 小窗设置View
 */
class MiWindowSettingView : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private lateinit var sp: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.mi_window_setting, rootKey)

        sp = PreferenceManager.getDefaultSharedPreferences(requireContext())

        preferenceManager.findPreference<SwitchPreference>("show_floating")?.apply {
            onPreferenceChangeListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<Preference>("setting_freeform_apps")?.apply {
            onPreferenceClickListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<Preference>("setting_freeform_dpi")?.apply {
            onPreferenceClickListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<Preference>("setting_floating_compatible")?.apply {
            onPreferenceClickListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<Preference>("battery")?.apply {
            onPreferenceClickListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<SwitchPreference>("show_foreground")?.apply {
            isEnabled = true
            summary = getString(R.string.foreground_service_subtitle)
            onPreferenceChangeListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<Preference>("setting_notification_apps")?.apply {
            onPreferenceClickListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<Preference>("setting_floating")?.apply {
            onPreferenceClickListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<SwitchPreference>("switch_notify")?.apply {
            onPreferenceChangeListener = this@MiWindowSettingView
        }
        preferenceManager.findPreference<SwitchPreference>("switch_preference_only_enable_landscape")?.apply {
            onPreferenceChangeListener = this@MiWindowSettingView
        }
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        when(preference?.key) {
            "setting_freeform_apps" -> {
                startActivity(Intent(context, ChooseAppsActivity::class.java).putExtra("type", 1))
            }
            "setting_freeform_dpi" -> {
                val view = LayoutInflater.from(requireContext()).inflate(R.layout.view_editview, null, false)
                val editText = view.findViewById<AppCompatEditText>(R.id.edit_freeform_dpi)
                val lastDPI = sp.getInt(FreeFormView.DPI, FreeFormView.DEFAULT_DPI).toString()
                editText.setText(lastDPI)
                MaterialAlertDialogBuilder(requireContext()).let {
                    it.setTitle(R.string.input_freeform_dpi)
                    it.setView(view)
                    it.setPositiveButton(R.string.done) { _, _ ->
                        val dpi = editText.text.toString().toInt()
                        sp.edit().apply {
                            putInt(FreeFormView.DPI, dpi)
                            apply()
                        }
                    }
                    it.create()
                }.show()

            }
            "setting_floating_compatible" -> {
                startActivity(Intent(context, ChooseAppsActivity::class.java).putExtra("type", 3))
            }
            "battery" -> {
                ignoreBatteryOptimization()
            }
            "setting_notification_apps" -> {
                startActivity(Intent(context, ChooseAppsActivity::class.java).putExtra("type", 2))
            }
            "setting_floating" -> {
                startActivity(Intent(context, FloatingSettingActivity::class.java))
            }
        }
        return true
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        CoreService.getServiceDataListener()?.onDataChanged(preference?.key!!, newValue!!)
        when(preference?.key) {
            "show_floating" -> {
                if (newValue as Boolean && !Settings.canDrawOverlays(requireContext())) {
                    return false
                }
            }
            "switch_notify" -> {
                if (newValue as Boolean) {
                    if (hasNotificationListenerPermission()) {
                        requireContext().startService(Intent(requireContext(), NotificationService::class.java))
                        return true
                    }else {
                        try {
                            Toast.makeText(requireContext(), getString(R.string.require_notification_permission), Toast.LENGTH_SHORT).show()
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }catch (e: Exception) {
                            Toast.makeText(requireContext(), getString(R.string.require_notification_permission_fail), Toast.LENGTH_LONG).show()
                        }
                        return false
                    }

                } else {
                    requireContext().stopService(Intent(requireContext(), NotificationService::class.java))
                }
            }
            "switch_preference_only_enable_landscape" -> {
                NotificationService.onlyEnableLandscape = newValue as Boolean
            }
        }
        return true
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

    private fun hasNotificationListenerPermission(): Boolean {
        var enable = false
        val flat = Settings.Secure.getString(requireContext().contentResolver, "enabled_notification_listeners")
        if (flat != null) {
            enable = flat.contains(requireContext().packageName)
        }
        return enable
    }
}