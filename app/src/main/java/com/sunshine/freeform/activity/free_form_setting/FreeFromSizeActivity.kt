package com.sunshine.freeform.activity.free_form_setting

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import com.sunshine.freeform.R
import kotlinx.android.synthetic.main.activity_free_from_size.*

class FreeFromSizeActivity : AppCompatActivity(){

    private lateinit var viewModel: FreeFormSettingViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_free_from_size)

        viewModel = ViewModelProvider(this).get(FreeFormSettingViewModel::class.java)

        viewModel.onChanged()
        initView()
    }

    private fun initView() {
        val sizeCardView: CardView = findViewById(R.id.cardView_freeform_size)

        /**
         * 错误，会出现 java.lang.ClassCastException: android.view.ViewGroup$LayoutParams cannot be cast to android.view.ViewGroup$MarginLayoutParams
         * 可以get layoutParams然后用这个设置宽高
         */
        //sizeCardView.layoutParams = ViewGroup.LayoutParams(viewModel.getWidth(), viewModel.getHeight())

        val layoutParams = sizeCardView.layoutParams
        layoutParams.width = viewModel.getWidth()
        layoutParams.height = viewModel.getHeight() + dip2px(50f)
        sizeCardView.layoutParams = layoutParams

        seekBar_width.max = viewModel.getScreenWidth()
        seekBar_height.max = viewModel.getScreenHeight()
        seekBar_width.progress = viewModel.getWidth()
        seekBar_height.progress = viewModel.getHeight()
        seekBar_width.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                layoutParams.width = progress
                sizeCardView.layoutParams = layoutParams
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })
        seekBar_height.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                layoutParams.height = progress + dip2px(50f)
                sizeCardView.layoutParams = layoutParams
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })

        button_size_save.setOnClickListener {
            //宽大于高会影响观感，不允许
            if (layoutParams.width >= layoutParams.height - dip2px(50f)) {
                Toast.makeText(this, getString(R.string.width_greater_height_error), Toast.LENGTH_LONG).show()
            } else {
                viewModel.save(layoutParams.width, layoutParams.height - dip2px(50f))
                Toast.makeText(this, getString(R.string.save_success), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun dip2px(dpValue: Float): Int {
        val scale: Float = resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
}