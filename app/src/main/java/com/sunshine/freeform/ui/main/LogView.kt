package com.sunshine.freeform.ui.main

import android.annotation.SuppressLint
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.sunshine.freeform.MiFreeformServiceManager

/**
 * @author KindBrave
 * @since 2023/9/4
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun LogWidget(viewModel: MainViewModel) {
    val log by viewModel.log.observeAsState("")
    val softWrap by viewModel.logSoftWrap.observeAsState(false)
    val modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
    val horizontalScrollModifier = modifier.horizontalScroll(rememberScrollState())

    Column(if (softWrap) modifier else horizontalScrollModifier) {
        Text(
            text = log,
            fontSize = 12.sp,
            softWrap = softWrap
        )
    }
}