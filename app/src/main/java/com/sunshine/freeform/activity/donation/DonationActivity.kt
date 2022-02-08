package com.sunshine.freeform.activity.donation

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import com.sunshine.freeform.R
import com.sunshine.freeform.activity.base.BaseActivity
import kotlinx.android.synthetic.main.activity_donation.*


class DonationActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donation)

        setTitle(getString(R.string.donation_label))

        button_alipay.setOnClickListener {
            try {
                val intent = Intent()
                intent.action = "android.intent.action.VIEW";
                //实现payUrl
                val payUrl = "HTTPS://QR.ALIPAY.COM/fkx18133hemtjbe1id3m558"
                intent.data = Uri.parse("alipayqr://platformapi/startapp?saId=10000007&clientVersion=3.7.0.0718&qrcode=" + payUrl);
                startActivity(intent)
            }
            catch (e: Exception) {
                Snackbar.make(button_alipay, getString(R.string.open_alipay_fail), Snackbar.LENGTH_SHORT).show()
            }
        }

        button_pay_list.setOnClickListener {
            val uri = Uri.parse("https://support.qq.com/products/315901/blog/504410")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        button_so_sad.setOnClickListener {
            finish()
        }
    }
}