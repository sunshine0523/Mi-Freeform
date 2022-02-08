package com.sunshine.freeform.activity.floating_setting

import android.os.Bundle
import androidx.preference.*
import com.sunshine.freeform.R
import com.sunshine.freeform.service.CoreService
import kotlinx.coroutines.DelicateCoroutinesApi

/**
 * @author sunshine
 * @date 2021/2/1
 */
@DelicateCoroutinesApi
class FloatingSettingView : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.floating_setting, rootKey)

        preferenceManager.findPreference<SwitchPreference>("show_location")?.apply {
            onPreferenceChangeListener = this@FloatingSettingView
        }
        preferenceManager.findPreference<SwitchPreference>("switch_hide_floating")?.apply {
            onPreferenceChangeListener = this@FloatingSettingView
        }
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        return true
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        CoreService.getServiceDataListener()?.onDataChanged("show_location", newValue!!)
        return true
    }
}