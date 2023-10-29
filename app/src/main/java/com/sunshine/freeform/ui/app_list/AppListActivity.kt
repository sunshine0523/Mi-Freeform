package com.sunshine.freeform.ui.app_list

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.sunshine.freeform.MiFreeformServiceManager
import com.sunshine.freeform.service.ServiceViewModel
import com.sunshine.freeform.service.FloatingService
import com.sunshine.freeform.ui.theme.MiFreeformTheme
import com.sunshine.freeform.utils.ServiceUtils

class AppListActivity : ComponentActivity() {

    private var serviceConnection: ServiceConnection ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, FloatingService::class.java)
        if (ServiceUtils.isServiceWork(this, "com.sunshine.freeform.service.FloatingService").not()) {
            startService(intent)
        }
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(p0: ComponentName, binder: IBinder) {
                val viewModel = (binder as FloatingService.MyBinder).getService().getViewModel()
                viewModel.filterApp("")
                viewModel.initAppList()
                setContent {
                    MiFreeformTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Column {
                                SearchWidget(
                                    textStyle = TextStyle(
                                        color = Color(0,0,0,128),
                                        fontSize = 20.sp
                                    ),
                                    viewModel = viewModel
                                )
                                ListWidget(viewModel = viewModel)
                            }
                        }
                    }
                }

                viewModel.finishActivity.observe(this@AppListActivity) { shouldFinish ->
                    if (shouldFinish) {
                        viewModel.finishActivity.postValue(false)
                        finish()
                    }
                }
            }

            override fun onServiceDisconnected(p0: ComponentName) {

            }
        }
        bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceConnection != null) {
            unbindService(serviceConnection!!)
        }
    }
}

@Composable
fun ListWidget(viewModel: ServiceViewModel) {
    val appList by viewModel.appListLiveData.observeAsState(ArrayList())
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        content = {
            items(appList) { appInfo ->
                AppListItem(appInfo = appInfo, viewModel = viewModel)
            }
        }
    )
}

@Composable
fun AppListItem(appInfo: AppInfo, viewModel: ServiceViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                MiFreeformServiceManager.createWindow(
                    appInfo.componentName.packageName,
                    appInfo.componentName.className,
                    appInfo.userId,
                    // I think AppListActivity must open in Freeform.
                    // So this `screen` is Freeform. I do not need scale. 0.8x 0.5x
                    viewModel.getIntSp("freeform_width", viewModel.screenWidth),
                    viewModel.getIntSp("freeform_height", viewModel.screenHeight),
                    viewModel.getIntSp("freeform_dpi", viewModel.screenDensityDpi),
                )
                viewModel.closeActivity()
                MiFreeformServiceManager.removeFreeform("com.sunshine.freeform,com.sunshine.freeform.ui.app_list.AppListActivity,0")
            }
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = appInfo.icon),
            contentDescription = null,
            modifier = Modifier
                .size(60.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = appInfo.label,
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis, maxLines = 1
        )
    }
}