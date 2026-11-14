package com.justantis.pause.presentation.limits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Icecream
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.justantis.pause.R
import com.justantis.pause.presentation.components.formatDuration
import com.justantis.pause.presentation.theme.PauseTheme

@Composable
fun LimitsScreen(){

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.limits_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.limits_subtitle),
            style = MaterialTheme.typography.bodyMedium
        )
        AppLimitsCard(appName = "Instagram", usingCount = 4, usedMinutes = 45)
        AppLimitsCard(appName = "Facebook", usingCount = 4, usedMinutes = 45)
        AppLimitsCard(appName = "Telegram", usingCount = 4, usedMinutes = 45)
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
            Text(text = stringResource(R.string.limits_add_limit), style = MaterialTheme.typography.bodyMedium)
        }
    }

}

@Composable
fun AppLimitsCard(modifier: Modifier = Modifier, appName: String, usingCount: Int, usedMinutes: Int){
    Card(modifier = modifier.fillMaxWidth()) {
        Column() {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Icecream, contentDescription = stringResource(R.string.limits_app_icon_content_description))
                Spacer(modifier = Modifier.width(12.dp))
                Column() {
                    Text(text =  appName, style = MaterialTheme.typography.titleMedium)
                    Text(text = pluralStringResource(R.plurals.limits_used_times, usingCount, usingCount), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(text = formatDuration(usedMinutes))
            }
        }
    }
}



@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun LimitsScreenPreview(){
    PauseTheme() {
        LimitsScreen()
    }
}