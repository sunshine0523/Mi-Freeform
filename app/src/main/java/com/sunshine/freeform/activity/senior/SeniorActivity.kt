package com.sunshine.freeform.activity.senior

import android.os.Bundle
import com.sunshine.freeform.R
import com.sunshine.freeform.base.BaseActivity

class SeniorActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_senior)
        setTitle(getString(R.string.senior_label))

//        supportFragmentManager
//                .beginTransaction()
//                .replace(R.id.root, )
    }
}