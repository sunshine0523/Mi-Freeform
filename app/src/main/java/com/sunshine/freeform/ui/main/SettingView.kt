package com.sunshine.freeform.ui.main

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sunshine.freeform.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * @author KindBrave
 * @since 2023/8/26
 */

@Composable
fun SettingWidget(mainViewModel: MainViewModel) {
    val enableSideBar by mainViewModel.enableSideBar.observeAsState(false)
    val showImeInFreeform by mainViewModel.showImeInFreeform.observeAsState(false)
    val freeformWidth by mainViewModel.freeformWidth.observeAsState((mainViewModel.screenWidth * 0.8).roundToInt())
    val freeformHeight by mainViewModel.freeformHeight.observeAsState((mainViewModel.screenHeight * 0.5).roundToInt())
    val freeformDpi by mainViewModel.freeformDensityDpi.observeAsState(mainViewModel.screenDensityDpi)
    val coroutineScope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }
    var isShowingSnackBar by remember { mutableStateOf(false) }
    val warn = stringResource(id = R.string.freeform_width_height_warn)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            SettingSwitchOption(
                stringResource(id = R.string.sidebar),
                stringResource(id = R.string.sidebar_message),
                enableSideBar
            ) {
                mainViewModel.saveRemoteSidebar(it)
            }
            SettingSlideBarOption(
                stringResource(id = R.string.freeform_width),
                freeformWidth,
                50,
                100f..mainViewModel.screenWidth.toFloat()
            ) {
                if (it.roundToInt() >= freeformHeight) {
                    if (isShowingSnackBar.not()) {
                        coroutineScope.launch {
                            isShowingSnackBar = true
                            val result = snackBarHostState.showSnackbar(warn, withDismissAction = true)
                            if (result == SnackbarResult.Dismissed) {
                                isShowingSnackBar = false
                            }
                        }
                    }
                } else {
                    mainViewModel.setFreeformWidth(it.roundToInt())
                }
            }
            SettingSlideBarOption(
                stringResource(id = R.string.freeform_height),
                freeformHeight,
                50,
                100f..mainViewModel.screenHeight.toFloat()
            ) {
                if (it.roundToInt() <= freeformWidth) {
                    if (isShowingSnackBar.not()) {
                        coroutineScope.launch {
                            isShowingSnackBar = true
                            val result = snackBarHostState.showSnackbar(warn, withDismissAction = true)
                            if (result == SnackbarResult.Dismissed) {
                                isShowingSnackBar = false
                            }
                        }
                    }
                } else {
                    mainViewModel.setFreeformHeight(it.roundToInt())
                }
            }
            SettingSlideBarOption(
                stringResource(id = R.string.freeform_dpi),
                freeformDpi,
                100,
                100f..700f
            ) {
                mainViewModel.setFreeformDpi(it.roundToInt())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingSwitchOption(
                    stringResource(id = R.string.show_ime_in_freeform),
                    stringResource(id = R.string.show_ime_in_freeform_message),
                    showImeInFreeform
                ) {
                    mainViewModel.saveShowImeInFreeform(it)
                }
            }
        }
    }

    SnackbarHost(hostState = snackBarHostState)
}

@Composable
fun SettingSwitchOption(
    title: String,
    message: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(8.dp)
            .clickable { onCheckedChange(!isChecked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingSlideBarOption(
    title: String,
    value: Int,
    steps: Int,
    range: ClosedFloatingPointRange<Float>,
    onValueChanged: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(text = "$title $value", style = MaterialTheme.typography.titleLarge)
        Slider(
            value = value.toFloat(),
            valueRange = range,
            steps = steps,
            onValueChange = onValueChanged,
        )
    }
}