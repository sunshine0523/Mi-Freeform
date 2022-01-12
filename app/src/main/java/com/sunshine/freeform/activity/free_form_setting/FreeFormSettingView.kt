package com.sunshine.freeform.activity.free_form_setting

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.RadioButton
import android.widget.RadioGroup

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

import com.sunshine.freeform.R
import com.sunshine.freeform.utils.HookFun
import com.sunshine.freeform.utils.TagUtils

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

        preferenceManager.findPreference<Preference>("setting_freeform_show_model")?.apply {
            onPreferenceClickListener = this@FreeFormSettingView
        }
    }

    @SuppressLint("InflateParams")
    override fun onPreferenceClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "setting_freeform_size" -> {
                sp.edit().apply {
                    putInt("width", -1)
                    putInt("height", -1)
                    putInt("width_land", -1)
                    putInt("height_land", -1)
                    apply()
                }
                Snackbar.make(requireView(), getString(R.string.to_default_freeform_size), Snackbar.LENGTH_SHORT).show()
            }

            "setting_freeform_show_model" -> {
                val builder = MaterialAlertDialogBuilder(requireContext())
                val view = LayoutInflater.from(requireContext()).inflate(R.layout.view_freeform_show_model, null, false)
                val radioGroup: RadioGroup = view.findViewById(R.id.radio_group_freeform_show_model)
                val radioUser: RadioButton = view.findViewById(R.id.radioButton_user)
                val radioSystem: RadioButton = view.findViewById(R.id.radioButton_system)

                //1 root 2 xposed
                if (sp.getInt("freeform_show_model", 1) == 1) {
                    radioUser.isChecked = true
                } else {
                    radioSystem.isChecked = true
                }

                builder.apply {
                    setTitle(getString(R.string.choose_freeform_show_model_title))
                    setView(view)
                    setPositiveButton(getString(R.string.done)) { _, _ ->
                        val showModel = if (radioGroup.checkedRadioButtonId == R.id.radioButton_system) 2 else 1
                        //User模式直接运行
                        if (showModel == 1) {
                            sp.edit().putInt("freeform_show_model", showModel).apply()
                        } else {
                            //System模式需要判断 首先需要xposed权限
                            if (HookFun.hook()) {
                                //然后判断可不可以成功
                                if (showSystemOverlayTest()) {
                                    sp.edit().putInt("freeform_show_model", showModel).apply()
                                } else {
                                    Snackbar.make(requireView(), "您的设备不支持该模式", Snackbar.LENGTH_SHORT).show()
                                }
                            } else {
                                Snackbar.make(requireView(), "您还没有在Xposed勾选米窗，请勾选后重启设备再试", Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    }
                    setNegativeButton(getString(R.string.cancel)) { _, _ -> }
                }
                builder.create().show()
            }
        }
        return true
    }

    private fun showSystemOverlayTest(): Boolean {
        return try {
            val windowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val view = View(requireContext())
            val layoutParams = WindowManager.LayoutParams().apply {
                type = 2026
            }
            windowManager.addView(view, layoutParams)
            windowManager.removeView(view)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {

        return true
    }
}