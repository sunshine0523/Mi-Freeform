package com.sunshine.freeform.ui.guide

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.ui.navigateUp
import com.sunshine.freeform.R
import com.sunshine.freeform.databinding.ActivityFreeformGuideBinding

class GuideActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFreeformGuideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFreeformGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}