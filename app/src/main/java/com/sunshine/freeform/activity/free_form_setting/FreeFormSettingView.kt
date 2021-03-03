package com.sunshine.freeform.activity.free_form_setting

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast

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

        preferenceManager.findPreference<Preference>("setting_freeform_size")?.apply {
            onPreferenceClickListener = this@FreeFormSettingView
        }

        preferenceManager.findPreference<Preference>("setting_freeform_display_model")?.apply {
            onPreferenceClickListener = this@FreeFormSettingView
        }

        preferenceManager.findPreference<Preference>("setting_freeform_control_model")?.apply {
            onPreferenceClickListener = this@FreeFormSettingView
        }
    }

    @SuppressLint("InflateParams")
    override fun onPreferenceClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "setting_freeform_size" -> {
                startActivity(Intent(requireActivity(), FreeFromSizeActivity::class.java))
            }

            "setting_freeform_display_model" -> {
                val builder = MaterialAlertDialogBuilder(requireContext())
                val view = LayoutInflater.from(requireContext()).inflate(R.layout.view_freeform_display_model, null, false)
                val radioGroup: RadioGroup = view.findViewById(R.id.radio_group_freeform_display_model)
                val radioImageReader: RadioButton = view.findViewById(R.id.radioButton_imageReader)
                val radioMediaCodec: RadioButton = view.findViewById(R.id.radioButton_mediaCodec)
                val radioTexture: RadioButton = view.findViewById(R.id.radioButton_texture)

                //1 imageReader 2 mediaCodec 3 texture
                when (sp.getInt("freeform_display_model", 1)) {
                    1 -> radioImageReader.isChecked = true
                    2 -> radioMediaCodec.isChecked = true
                    3 -> radioTexture.isChecked = true
                }

                builder.apply {
                    setTitle(getString(R.string.choose_freeform_model_title))
                    setView(view)
                    setPositiveButton(getString(R.string.done)) { _, _ ->
                        sp.edit().putInt("freeform_display_model",when (radioGroup.checkedRadioButtonId) {
                            R.id.radioButton_imageReader -> 1
                            R.id.radioButton_mediaCodec -> 2
                            R.id.radioButton_texture -> 3
                            else -> 3
                        }).apply()
                    }
                    setNegativeButton(getString(R.string.cancel)) { _, _ -> }
                }
                builder.create().show()
            }

            "setting_freeform_control_model" -> {
                val builder = MaterialAlertDialogBuilder(requireContext())
                val view = LayoutInflater.from(requireContext()).inflate(R.layout.view_freeform_control_model, null, false)
                val radioGroup: RadioGroup = view.findViewById(R.id.radio_group_freeform_control_model)
                val radioRoot: RadioButton = view.findViewById(R.id.radioButton_root)
                val radioXposed: RadioButton = view.findViewById(R.id.radioButton_xposed)

                //1 root 2 xposed
                if (sp.getInt("freeform_control_model", 1) == 1) {
                    radioRoot.isChecked = true
                } else {
                    radioXposed.isChecked = true
                }

                builder.apply {
                    setTitle(getString(R.string.choose_freeform_model_title))
                    setView(view)
                    setPositiveButton(getString(R.string.done)) { _, _ ->
                        val controlModel = if (radioGroup.checkedRadioButtonId == R.id.radioButton_xposed) 2 else 1
                        sp.edit().putInt("freeform_control_model",controlModel).apply()
                        //Xposed模式
                        if (controlModel == 2) {
                            xposedFun()
                        }
                        Toast.makeText(requireContext(), getString(R.string.require_reboot), Toast.LENGTH_LONG).show()
                    }
                    setNegativeButton(getString(R.string.cancel)) { _, _ -> }
                }
                builder.create().show()
            }
        }
        return true
    }

    /**
     * xposed模式需要获取uid，
     */
    private fun xposedFun() {
        val xposedBuilder = MaterialAlertDialogBuilder(requireContext())
        xposedBuilder.apply {
            setTitle(getString(R.string.dialog_title))
            setMessage(getString(R.string.xposed_message))
            setCancelable(false)
            setPositiveButton(getString(R.string.done)) { _, _ -> }
        }
        xposedBuilder.create().show()

        //获取uid供hook时注入
        val uid = requireContext().packageManager.getApplicationInfo(requireContext().packageName, 0).uid
        sp.edit().putInt("uid", uid).apply()
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {

        return true
    }
}