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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.sunshine.freeform.MiFreeformServiceManager
import com.sunshine.freeform.R
import com.sunshine.freeform.ui.theme.MiFreeformTheme
import kotlin.math.roundToInt

class AppListActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val viewModel = AppListViewModel(application)
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

        viewModel.finishActivity.observe(this) { shouldFinish ->
            if (shouldFinish) finish()
        }
    }
}

@Composable
fun SearchWidget(textStyle: TextStyle = TextStyle.Default, viewModel: AppListViewModel) {
    var text by remember { mutableStateOf("")}
    Box {
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                viewModel.filterApp(text)
            },
            textStyle = textStyle,
            modifier = Modifier
                .padding(20.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .height(60.dp)
                .fillMaxWidth(),
            decorationBox = {
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)) {
                    Box(modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .weight(1f),
                        contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(id = R.string.search_app),
                                style = textStyle
                            )
                        }
                        it()
                    }
                }
            }
        )
    }
}

@Composable
fun ListWidget(viewModel: AppListViewModel) {
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
fun AppListItem(appInfo: AppInfo, viewModel: AppListViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                MiFreeformServiceManager.createDisplay(
                    appInfo.componentName,
                    appInfo.userId,
                    viewModel.getIntSp("freeform_width", (viewModel.screenWidth * 0.8).roundToInt()),
                    viewModel.getIntSp("freeform_height", (viewModel.screenHeight * 0.5).roundToInt()),
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