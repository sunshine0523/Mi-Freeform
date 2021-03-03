package com.sunshine.freeform.activity.floating_setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference
import com.sunshine.freeform.R
import com.sunshine.freeform.service.floating.FloatingService

/**
 * @author sunshine
 * @date 2021/2/1
 */
class FloatingSettingView(context: Context) : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private val sp = context.getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.floating_setting, rootKey)

        preferenceManager.findPreference<SwitchPreference>("switch_show_location")?.apply {
            onPreferenceChangeListener = this@FloatingSettingView
        }
        preferenceManager.findPreference<SeekBarPreference>("floating_button_size")?.apply {
            onPreferenceChangeListener = this@FloatingSettingView
        }
        preferenceManager.findPreference<SeekBarPreference>("floating_button_alpha")?.apply {
            onPreferenceChangeListener = this@FloatingSettingView
        }
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        when(preference?.key) {

        }
        return true
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        when(preference?.key) {
            "switch_show_location" -> {
                if (sp.getBoolean("switch_floating", false)) {
                    requireActivity().stopService(Intent(requireActivity(), FloatingService::class.java))
                    requireActivity().startService(Intent(requireActivity(), FloatingService::class.java))
                }
            }
            "floating_button_size" -> {
                FloatingService.floatButtonSizeChangeListener?.onChanged(newValue as Int)
            }
            "floating_button_alpha" -> {
                FloatingService.floatButtonAlphaChangeListener?.onChanged(newValue as Int)
            }
        }
        return true
    }
}