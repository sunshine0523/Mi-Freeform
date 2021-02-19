package com.sunshine.freeform.activity.free_form_setting

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.RadioButton
import android.widget.RadioGroup

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import com.sunshine.freeform.R

/**
 * @author sunshine
 * @date 2021/2/19
 */
class FreeFormSettingView : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private lateinit var sp: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.freeform_setting, rootKey)

        sp = requireContext().getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)

        preferenceManager.findPreference<Preference>("setting_freeform_model")?.apply {
            onPreferenceClickListener = this@FreeFormSettingView
        }
    }

    @SuppressLint("InflateParams")
    override fun onPreferenceClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "setting_freeform_model" -> {
                val builder = MaterialAlertDialogBuilder(requireContext())
                val view = LayoutInflater.from(requireContext()).inflate(R.layout.view_freeform_model, null, false)
                val radioGroup: RadioGroup = view.findViewById(R.id.radio_group_freeform_model)
                val radioImageReader: RadioButton = view.findViewById(R.id.radioButton_imageReader)
                val radioMediaCodec: RadioButton = view.findViewById(R.id.radioButton_mediacodec)

                //1 imageReader 2 mediaCodec
                if (sp.getInt("freeform_model", 1) == 1) {
                    radioImageReader.isChecked = true
                } else {
                    radioMediaCodec.isChecked = true
                }

                builder.apply {
                    setTitle(getString(R.string.choose_freeform_model_title))
                    setView(view)
                    setPositiveButton(getString(R.string.done)) { _, _ ->
                        sp.edit().putInt("freeform_model", if (radioGroup.checkedRadioButtonId == R.id.radioButton_mediacodec) 2 else 1).apply()
                    }
                    setNegativeButton(getString(R.string.cancel)) { _, _ -> }
                }
                builder.create().show()
            }
        }
        return true
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {

        return true
    }
}