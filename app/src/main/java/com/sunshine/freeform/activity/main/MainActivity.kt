package com.sunshine.freeform.activity.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.sunshine.freeform.BaseViewModel
import com.sunshine.freeform.MiFreeForm

import com.sunshine.freeform.R
import com.sunshine.freeform.activity.donation.DonationActivity
import com.sunshine.freeform.activity.mi_window_setting.MiWindowSettingActivity
import com.sunshine.freeform.activity.base.BaseActivity
import com.sunshine.freeform.hook.service.MiFreeFormService
import com.sunshine.freeform.service.CoreService
import com.sunshine.freeform.service.NotificationService
import com.sunshine.freeform.utils.ServiceUtils

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.DelicateCoroutinesApi

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
        const val FEEDBACK_URL = "https://docs.qq.com/form/page/DRE1RTWtCV2dRb1dk"
        const val MARKET_ID = "market://details?id=com.sunshine.freeform"
        const val QQ_GROUP = "https://qun.qq.com/qqweb/qunpro/share?_wv=3&_wwv=128&inviteCode=XKL1t&from=246610&biz=ka"
        const val TELEGRAM_URL = "https://t.me/mi_freeform"
        const val OPEN_SOURCE_URL = "https://github.com/sunshine0523/Mi-FreeForm"
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        doNotShowBackKey()

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        setTitle(getString(R.string.app_name))

        initService()

        button_freeform_setting.setOnClickListener(this)
        button_tell_me.setOnClickListener(this)
        button_donate.setOnClickListener(this)
        button_star.setOnClickListener(this)
        button_coolapk.setOnClickListener(this)
        button_qq_group.setOnClickListener(this)
        button_qq_channel.setOnClickListener(this)
        button_telegram.setOnClickListener(this)
        button_open_source.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        initServiceInfo()
    }

    private fun initServiceInfo() {
        if (MiFreeFormService.getClient() != null) {
            imageView_service.setImageResource(R.drawable.ic_done_white)
            textView_service_info.text = getString(R.string.xposed_start)
            textView_service_description.text = getString(R.string.xposed_service_description)
            info_bg.setBackgroundColor(getColor(R.color.green))
        } else {
            if (!MiFreeForm.baseViewModel.isRunning.value!!) {
                MiFreeForm.baseViewModel.initShizuku()
            }
            MiFreeForm.baseViewModel.isRunning.observe(this, { isRunning ->
                if (isRunning) {
                    imageView_service.setImageResource(R.drawable.ic_done_white)
                    textView_service_info.text = getString(R.string.sui_start)
                    textView_service_description.text = getString(R.string.sui_service_description)
                    info_bg.setBackgroundColor(getColor(R.color.green))
                } else {
                    imageView_service.setImageResource(R.drawable.ic_error_white)
                    textView_service_info.text = getString(R.string.no_start)
                    textView_service_description.text = getString(R.string.no_service_description)
                    info_bg.setBackgroundColor(getColor(R.color.red))
                }
            })

            BaseViewModel.get()
        }
    }

    private fun initService() {
        if (!ServiceUtils.isServiceWork(this, "$packageName.service.CoreService")) startService(Intent(applicationContext, CoreService::class.java))

        if (viewModel.isNotification()) {
            if (!ServiceUtils.isServiceWork(this, "$packageName.service.notification.NotificationService")) startService(Intent(applicationContext, NotificationService::class.java))

            viewModel.getAllNotificationApps().observe(this, Observer {
                NotificationService.notificationApps = it
            })
        }
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.button_freeform_setting -> {
                startActivity(Intent(this, MiWindowSettingActivity::class.java))
            }
            R.id.button_tell_me -> {
                val uri = Uri.parse(FEEDBACK_URL)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
            R.id.button_donate -> {
                startActivity(Intent(this, DonationActivity::class.java))
            }
            R.id.button_star -> {
                try {
                    val localIntent = Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_ID))
                    localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    localIntent.`package` = COOLAPK_PACKAGE
                    startActivity(localIntent)
                } catch (e: Exception) {
                    try {
                        val localIntent = Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_ID))
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
            R.id.button_qq_channel -> {
                val uri = Uri.parse(QQ_GROUP)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
            R.id.button_telegram -> {
                val uri = Uri.parse(TELEGRAM_URL)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
            R.id.button_open_source -> {
                val uri = Uri.parse(OPEN_SOURCE_URL)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
        }
    }
}