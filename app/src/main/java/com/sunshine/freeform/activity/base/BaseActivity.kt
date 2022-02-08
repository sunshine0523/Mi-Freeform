package com.sunshine.freeform.activity.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.sunshine.freeform.R
import kotlinx.android.synthetic.main.activity_base.view.*

/**
 * @author sunshine
 * @date 2021/3/4
 */
open class BaseActivity : AppCompatActivity() {

    private lateinit var baseView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        baseView = LayoutInflater.from(this).inflate(R.layout.activity_base, null, false)
    }

    override fun setContentView(layoutResID: Int) {
        val view = LayoutInflater.from(this).inflate(layoutResID, baseView.container, false)
        baseView.container.addView(view)
        window.setContentView(baseView)

        setSupportActionBar(getToolbar())
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    /**
     * 设置标题
     */
    fun setTitle(title: String) {
        baseView.toolbar.title = title
    }

    /**
     * 设置无默认toolbar，可自定义toolbar
     */
    fun setNoTitle() {
        baseView.appBarLayout.visibility = View.GONE
    }

    fun getToolbar(): MaterialToolbar? {
        return baseView.toolbar
    }

    /**
     * toolbar不显示返回键
     */
    fun doNotShowBackKey(){
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return true
    }
}