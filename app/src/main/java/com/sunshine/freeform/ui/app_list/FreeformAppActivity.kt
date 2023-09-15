package com.sunshine.freeform.ui.app_list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.sunshine.freeform.R
import com.sunshine.freeform.service.ServiceViewModel
import com.sunshine.freeform.ui.theme.MiFreeformTheme

class FreeformAppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val viewModel = ServiceViewModel(application)

        setContent {
            MiFreeformTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScaffoldWidget(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldWidget(viewModel: ServiceViewModel) {
    Scaffold(
        topBar = {
            TopBarWidget()
        },
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) {
        Box(modifier = Modifier.padding(it)) {
            Column {
                SearchWidget(
                    textStyle = TextStyle(
                        color = Color(0,0,0,128),
                        fontSize = 20.sp
                    ),
                    viewModel = viewModel
                )
                FreeformAppListWidget(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWidget() {
    TopAppBar(
        title = {
            Text(text = stringResource(id = R.string.title_activity_freeform_app))
        },
    )
}

@Composable
fun FreeformAppListWidget(viewModel: ServiceViewModel) {
    val appList by viewModel.appListLiveData.observeAsState(ArrayList())
    LazyColumn(content = {
        items(appList) { appInfo ->
            FreeformAppListItem(appInfo = appInfo, viewModel = viewModel)
        }
    })
}

@Composable
fun FreeformAppListItem(appInfo: AppInfo, viewModel: ServiceViewModel) {
    var bg by remember { mutableStateOf(Color.Transparent) }
    val selectedBg = MaterialTheme.colorScheme.primaryContainer
    if (appInfo.isFreeformApp) bg = selectedBg
    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(horizontal = 16.dp)
                .clickable {
                    bg = if (appInfo.isFreeformApp) {
                        viewModel.removeFreeformApp(appInfo.componentName.packageName, appInfo.componentName.className, appInfo.userId)
                        Color.Transparent
                    } else {
                        viewModel.addFreeformApp(appInfo.componentName.packageName, appInfo.componentName.className, appInfo.userId)
                        selectedBg
                    }
                    appInfo.isFreeformApp = !appInfo.isFreeformApp
                }
        ) {
            Image(
                painter = rememberDrawablePainter(drawable = appInfo.icon),
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = appInfo.label,
                style = MaterialTheme.typography.bodyLarge,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }
}