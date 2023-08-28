package com.sunshine.freeform.ui.main

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunshine.freeform.MiFreeformServiceManager
import com.sunshine.freeform.R

@Composable
fun HomeWidget() {
    StatusWidget()
    InfoWidget()
}

@Composable
fun StatusWidget() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(125.dp)
            .padding(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.primary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (MiFreeformServiceManager.ping()) Icons.Filled.Done else Icons.Filled.Clear, "")
            Text(
                modifier = Modifier.padding(horizontal = 5.dp),
                text = stringResource(if (MiFreeformServiceManager.ping()) R.string.service_running else R.string.service_not_running),
                color = Color.White,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                ),
            )
        }
    }
}

@Composable
fun InfoWidget() {

}