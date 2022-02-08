package com.sunshine.freeform.activity.splash

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sunshine.freeform.BuildConfig
import com.sunshine.freeform.R
import com.sunshine.freeform.activity.main.MainActivity
import com.sunshine.freeform.activity.base.BaseActivity
import kotlinx.android.synthetic.main.activity_splash_setup.*

class SplashActivity : BaseActivity() {

    private lateinit var sp: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sp = getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)

        if (sp.getBoolean("first_start", true) || sp.getInt("app_version", -1) < 27) {
            setContentView(R.layout.activity_splash_setup)
            toSetup()
        } else {
            setContentView(R.layout.activity_splash)

            if (sp.getBoolean("switch_hide_recent", false)) {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS))
            } else {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            finish()
        }
    }

    private fun toSetup() {
        setTitle(getString(R.string.welcome_label))

        textview_setup.apply {
            text = setPermissionTextLink()
            movementMethod = LinkMovementMethod.getInstance()
        }

        textview_setup_help.text = Html.fromHtml(getString(R.string.textview_setup_help), Html.FROM_HTML_MODE_LEGACY)

        fab_done.setOnClickListener {
            if (!checkbox_read_statement.isChecked) {
                Snackbar.make(fab_done, "请您阅读并同意上述声明后再使用米窗", Snackbar.LENGTH_SHORT).show()
            } else {
                sp.edit().putBoolean("first_start", false).apply()
                sp.edit().putInt("app_version", BuildConfig.VERSION_CODE).apply()
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                finish()
            }
        }
    }

    /**
     * 设置文字点击出现权限说明
     */
    private fun setPermissionTextLink(): SpannableString {
        val spannableString = SpannableString(getString(R.string.setup_info))
        spannableString.setSpan(UnderlineSpan(), 123, 127, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                val builder = MaterialAlertDialogBuilder(this@SplashActivity)
                builder.apply {
                    setTitle(getString(R.string.permission_instruction))
                    setMessage(R.string.permission_info)
                    setPositiveButton(getString(R.string.done)) {_, _ ->}
                    setCancelable(false)
                }
                builder.create().show()
            }

        }, 123, 127, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        spannableString.setSpan(ForegroundColorSpan(Color.BLUE), 160, 169, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        spannableString.setSpan(UnderlineSpan(), 160, 169, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                val builder = MaterialAlertDialogBuilder(this@SplashActivity)
                builder.apply {
                    setTitle(getString(R.string.service_instruction))
                    setMessage(R.string.service_info)
                    setPositiveButton(getString(R.string.done)) {_, _ ->}
                    setCancelable(false)
                }
                builder.create().show()
            }

        }, 160, 169, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        spannableString.setSpan(ForegroundColorSpan(Color.BLUE), 160, 169, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return spannableString
    }
}