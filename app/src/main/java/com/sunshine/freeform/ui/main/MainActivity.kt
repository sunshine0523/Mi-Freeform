package com.sunshine.freeform.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sunshine.freeform.MiFreeformServiceManager
import com.sunshine.freeform.R
import com.sunshine.freeform.ui.theme.MiFreeformTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingViewModel = SettingViewModel(application)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MiFreeformTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScaffoldWidget(settingViewModel)
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldWidget(settingViewModel: SettingViewModel) {
    val items = mutableListOf("Home")
    if (MiFreeformServiceManager.ping()) items.add("Setting")
    items.add("Log")

    var selectedItem by remember {
        mutableStateOf("Home")
    }

    Scaffold(
        topBar = {
            TopBarWidget()
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed {index, item ->
                    NavigationBarItem(
                        selected = selectedItem == item,
                        onClick = { selectedItem = item },
                        icon = { Icon(getNavigationIcon(index), item) }
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) {
        // 需要设置Modifier.padding()
        Box(modifier = Modifier.padding(it)) {
            when (selectedItem) {
                "Home" -> HomeWidget()
                "Setting" -> SettingWidget(settingViewModel)
                "Log" -> LogWidget()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWidget() {
    TopAppBar(
        title = {
            Text(text = stringResource(id = R.string.app_name))
        },
    )
}

private fun getNavigationIcon(index: Int): ImageVector {
    return when (index) {
        0 -> Icons.Filled.Home
        1 -> Icons.Filled.Settings
        2 -> Icons.Filled.List
        else -> Icons.Filled.Home
    }
}