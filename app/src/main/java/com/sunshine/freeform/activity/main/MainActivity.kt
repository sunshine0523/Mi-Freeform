package com.sunshine.freeform.activity.main

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

import com.sunshine.freeform.R
import com.sunshine.freeform.activity.donation.DonationActivity
import com.sunshine.freeform.activity.floating_view.FloatingViewActivity
import com.sunshine.freeform.activity.mi_window_setting.MiWindowSettingActivity
import com.sunshine.freeform.base.BaseActivity
import com.sunshine.freeform.callback.SuiServerListener
import com.sunshine.freeform.hook.service.MiFreeFormService
import com.sunshine.freeform.service.Floating2Service
import com.sunshine.freeform.service.FloatingService
import com.sunshine.freeform.service.ForegroundService
import com.sunshine.freeform.service.NotificationService
import com.sunshine.freeform.utils.FreeFormUtils
import com.sunshine.freeform.utils.PermissionUtils
import com.sunshine.freeform.utils.ServiceUtils
import com.sunshine.freeform.utils.TagUtils
import com.sunshine.freeform.view.floating.FreeFormHelper
import com.sunshine.freeform.view.floating.FreeFormView

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.DelicateCoroutinesApi

import rikka.shizuku.Shizuku

/**
 * @author sunshine
 * @date 2021/1/31
 */
@DelicateCoroutinesApi
class MainActivity : BaseActivity(), View.OnClickListener {

    companion object {
        const val TAG = "MainActivity"
        const val MY_COOLAPK_PAGE = "http://www.coolapk.com/u/810697"
        const val COOLAPK_PACKAGE = "com.coolapk.market"
        var listener: SuiServerListener? = null
    }

    private lateinit var viewModel: MainViewModel

    private val onRequestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == TagUtils.SUI_CODE && grantResult == PERMISSION_GRANTED) {
                checkSuiPermission()
            }
        }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        FreeFormHelper.init(this, object : SuiServerListener() {
            override fun onStart() {
                try {
                    imageView_service.setImageResource(R.drawable.ic_done_white)
                    textView_service_info.text = getString(R.string.sui_start)
                    textView_service_description.text = getString(R.string.sui_service_description)
                    info_bg.setBackgroundColor(getColor(R.color.green))
                }catch (e: Exception) {
                    imageView_service.setImageResource(R.drawable.ic_error_white)
                    textView_service_info.text = getString(R.string.no_start)
                    textView_service_description.text = getString(R.string.no_service_description)
                    info_bg.setBackgroundColor(getColor(R.color.red))
                }
            }
            override fun onStop() {
                imageView_service.setImageResource(R.drawable.ic_error_white)
                textView_service_info.text = getString(R.string.no_start)
                textView_service_description.text = getString(R.string.no_service_description)
                info_bg.setBackgroundColor(getColor(R.color.red))
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        doNotShowBackKey()

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        setTitle(getString(R.string.app_name))

        initService()

        button_freeform_setting.setOnClickListener(this)
        button_donate.setOnClickListener(this)
        button_star.setOnClickListener(this)
        button_coolapk.setOnClickListener(this)
        button_qq_group.setOnClickListener(this)
        button_telegram.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        initServiceInfo()
    }

    private fun initServiceInfo() {
        if (MiFreeFormService.getClient() != null) {
            //if (viewModel.isStartForegroundService()) startForegroundService(Intent(this, ForegroundService::class.java))
            imageView_service.setImageResource(R.drawable.ic_done_white)
            textView_service_info.text = getString(R.string.xposed_start)
            textView_service_description.text = getString(R.string.xposed_service_description)
            info_bg.setBackgroundColor(getColor(R.color.green))
        } else {
            //if (viewModel.isStartForegroundService()) startForegroundService(Intent(this, ForegroundService::class.java))

            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            try {
                if (Shizuku.checkSelfPermission() != PERMISSION_GRANTED) {
                    Shizuku.addRequestPermissionResultListener(onRequestPermissionResultListener)
                    Shizuku.requestPermission(TagUtils.SUI_CODE)
                } else {
                    imageView_service.setImageResource(R.drawable.ic_done_white)
                    textView_service_info.text = getString(R.string.sui_start)
                    textView_service_description.text = getString(R.string.sui_service_description)
                    info_bg.setBackgroundColor(getColor(R.color.green))
                }
            }catch (e: Exception) {
                Toast.makeText(this, getString(R.string.shizuku_not_running), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initService() {
        if (viewModel.isShowFloating() && !ServiceUtils.isServiceWork(this, "$packageName.service.Floating2Service")) startService(Intent(applicationContext, Floating2Service::class.java))

        if (viewModel.isNotification()) {
            if (!ServiceUtils.isServiceWork(this, "$packageName.service.notification.NotificationService")) startService(Intent(applicationContext, NotificationService::class.java))

            viewModel.getAllNotificationApps().observe(this, Observer {
                NotificationService.notificationApps = it
            })
        }
    }

    private fun checkSuiPermission() {
        if (Shizuku.checkSelfPermission() == PERMISSION_GRANTED) {
            FreeFormHelper.init(this, object : SuiServerListener() {
                override fun onStart() {
                    try {
                        imageView_service.setImageResource(R.drawable.ic_done_white)
                        textView_service_info.text = getString(R.string.sui_start)
                        textView_service_description.text = getString(R.string.sui_service_description)
                        info_bg.setBackgroundColor(getColor(R.color.green))
                    }catch (e: Exception) {
                        imageView_service.setImageResource(R.drawable.ic_error_white)
                        textView_service_info.text = getString(R.string.no_start)
                        textView_service_description.text = getString(R.string.no_service_description)
                        info_bg.setBackgroundColor(getColor(R.color.red))
                    }
                }
                override fun onStop() {}
            })
        } else {
            imageView_service.setImageResource(R.drawable.ic_error_white)
            textView_service_info.text = getString(R.string.no_start)
            textView_service_description.text = getString(R.string.no_service_description)
            info_bg.setBackgroundColor(getColor(R.color.red))
        }
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.button_freeform_setting -> {
                startActivity(Intent(this, MiWindowSettingActivity::class.java))
            }
            R.id.button_donate -> {
                startActivity(Intent(this, DonationActivity::class.java))
            }
            R.id.button_star -> {
                try {
                    val str = "market://details?id=com.sunshine.freeform"
                    val localIntent = Intent(Intent.ACTION_VIEW, Uri.parse(str))
                    localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    localIntent.`package` = "com.coolapk.market"
                    startActivity(localIntent)
                } catch (e: Exception) {
                    try {
                        val str = "market://details?id=com.sunshine.freeform"
                        val localIntent = Intent(Intent.ACTION_VIEW, Uri.parse(str))
                        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(localIntent)
                    }catch (e : Exception){
                        Toast.makeText(this, getString(R.string.start_market_fail), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            R.id.button_coolapk -> {
                try {
                    val str = MY_COOLAPK_PAGE
                    val localIntent = Intent(Intent.ACTION_VIEW, Uri.parse(str))
                    localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    localIntent.`package` = COOLAPK_PACKAGE
                    startActivity(localIntent)
                } catch (e: Exception) {
                    Toast.makeText(this, getString(R.string.start_coolapk_fail), Toast.LENGTH_SHORT).show()
                }
            }
            R.id.button_qq_group -> {
                try {
                    val intent = Intent()
                    val key = "qNbvThGAg7lPnCfLNWL-NKw0Teaso05e"
                    intent.data =
                        Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D$key")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, getString(R.string.start_qq_fail), Toast.LENGTH_SHORT).show()
                }
            }
            R.id.button_telegram -> {
                val uri = Uri.parse("https://t.me/mi_freeform")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(onRequestPermissionResultListener)
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
    }
}