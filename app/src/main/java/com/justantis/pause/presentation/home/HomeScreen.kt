package com.justantis.pause.presentation.home

import android.widget.Space
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview(device = "spec:width=411dp,height=891dp", showSystemUi = true, showBackground = true)
@Composable
fun HomeScreen(){

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .background(MaterialTheme.colorScheme.background),
    ) {
        Text("Friday, 29 December", fontSize = 12.sp)
        Text(
            text = "Hi Roman!",
            fontSize = 24.sp
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ){
                Icon(
                    modifier = Modifier.size(48.dp),
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = "Seria"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "12 days!", fontSize = 34.sp)
                    Text(text = "You're so good!")
                }

                Card(
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "1/2")
                        Text(text = "freeze")
                    }
                }
            }

        }
        Spacer(modifier = Modifier.size(12.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Screen time", fontSize = 18.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = "Goal: 3h", fontSize = 16.sp)
                }
                Text(text = "2h 14m", fontSize = 32.sp)
                LinearProgressIndicator(progress = {20f})
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "46 unblocks")
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = "4 after limits")
                }
            }
        }
    }
}