package com.sunshine.freeform.ui.app_list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.sunshine.freeform.R
import com.sunshine.freeform.service.ServiceViewModel

/**
 * @author KindBrave
 * @since 2023/8/29
 */
@Composable
fun SearchWidget(textStyle: TextStyle = TextStyle.Default, viewModel: ServiceViewModel) {
    var text by remember { mutableStateOf("") }
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