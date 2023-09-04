package com.sunshine.freeform.ui.main

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.sunshine.freeform.MiFreeformServiceManager

/**
 * @author KindBrave
 * @since 2023/9/4
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogWidget() {
    var log by remember {
        mutableStateOf(MiFreeformServiceManager.getLog() ?: "")
    }
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    MiFreeformServiceManager.clearLog()
                    log = ""
                }
            ) {
                Icon(Icons.Default.Delete, "")
            }
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())) {
            Text(
                text = log,
                fontSize = 15.sp
            )
        }
    }
}