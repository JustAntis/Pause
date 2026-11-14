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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.justantis.pause.presentation.stats.StatsScreen
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
            text = "Limits",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Day limits for applications",
            style = MaterialTheme.typography.bodyMedium
        )
        AppLimitsCard(appName = "Instagram", usingCount = "4", usedTime = "45 min")
        AppLimitsCard(appName = "Facebook", usingCount = "4", usedTime = "45 min")
        AppLimitsCard(appName = "Telegram", usingCount = "4", usedTime = "45 min")
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add limit")
            Text(text = "Add limit", style = MaterialTheme.typography.bodyMedium)
        }
    }

}

@Composable
fun AppLimitsCard(modifier: Modifier = Modifier, appName: String, usingCount: String, usedTime: String){
    Card(modifier = modifier.fillMaxWidth()) {
        Column() {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Icecream, contentDescription = "Application Icon")
                Spacer(modifier = Modifier.width(12.dp))
                Column() {
                    Text(text =  appName, style = MaterialTheme.typography.titleMedium)
                    Text(text = "Used - $usingCount times", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(text = usedTime)
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