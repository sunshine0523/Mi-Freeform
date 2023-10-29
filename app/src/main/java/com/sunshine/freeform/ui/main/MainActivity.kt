package com.sunshine.freeform.ui.main

import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.service.notification.NotificationListenerService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sunshine.freeform.MiFreeformServiceManager
import com.sunshine.freeform.R
import com.sunshine.freeform.ui.theme.MiFreeformTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.OutputStream

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainViewModel = MainViewModel(application)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val saveLogsLauncher: ActivityResultLauncher<String> = registerForActivityResult(
            ActivityResultContracts.CreateDocument("text/plain")
        ) { uri ->
            if (uri == null) return@registerForActivityResult
            GlobalScope.launch(Dispatchers.IO) {
                val context = this@MainActivity
                val cr = context.contentResolver
                try {
                    val outputStream: OutputStream? = cr.openOutputStream(uri)
                    outputStream?.use { os ->
                        os.write(mainViewModel.log.value?.toByteArray())
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }

        setContent {
            MiFreeformTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScaffoldWidget(mainViewModel, saveLogsLauncher)
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldWidget(mainViewModel: MainViewModel, saveLogsLauncher: ActivityResultLauncher<String>) {
    val items = mutableListOf("Home", "Log")
    if (MiFreeformServiceManager.ping()) items.add("Setting")

    var selectedIndex by remember {
        mutableIntStateOf(0)
    }

    Scaffold(
        topBar = {
            TopBarWidget(selectedIndex, mainViewModel, saveLogsLauncher)
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed {index, item ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        icon = { Icon(getNavigationIcon(index), item) }
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) {
        // Need set Modifier.padding()
        Box(modifier = Modifier.padding(it)) {
            when (selectedIndex) {
                0 -> HomeWidget()
                1 -> LogWidget(mainViewModel)
                2 -> SettingWidget(mainViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWidget(
    selectedIndex: Int,
    viewModel: MainViewModel,
    saveLogsLauncher: ActivityResultLauncher<String>
) {
    TopAppBar(
        title = {
            Text(text = getTitle(index = selectedIndex))
        },
        actions  = {
            if (selectedIndex == 1) {
                IconButton(
                    onClick = {
                        viewModel.setLogSoftWrap(viewModel.logSoftWrap.value?.not()?:true)
                    }
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_wrap_text), contentDescription = null)
                }
                IconButton(
                    onClick = {
                        viewModel.clearLog()
                    }
                ) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = null)
                }
                IconButton(
                    onClick = {
                        saveLogsLauncher.launch("Mi-Freeform-Log.log")
                    }
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_save), contentDescription = null)
                }
            }
        }
    )
}

@Composable
private fun getTitle(index: Int): String {
    return when (index) {
        0 -> stringResource(id = R.string.app_name)
        1 -> stringResource(id = R.string.log)
        2 -> stringResource(id =R.string.setting)
        else -> stringResource(id = R.string.app_name)
    }
}

@Composable
private fun getNavigationIcon(index: Int):  Painter{
    return when (index) {
        0 -> painterResource(id = R.drawable.ic_home)
        1 -> painterResource(id = R.drawable.ic_log)
        2 -> painterResource(id = R.drawable.ic_setting)
        else -> painterResource(id = R.drawable.ic_home)
    }
}