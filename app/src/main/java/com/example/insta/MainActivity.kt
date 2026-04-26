package com.example.insta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insta.ui.theme.InstaDownloaderTheme
import com.example.insta.ui.theme.GradientEnd
import com.example.insta.ui.theme.GradientStart

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            System.loadLibrary("insta_core")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        setContent {
            InstaDownloaderTheme {
                MainScreen()
            }
        }
    }

    external fun resolveUrl(url: String): String
}

@Composable
fun MainScreen() {
    var urlText by remember { mutableStateOf("") }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Reels Downloader",
                fontSize = 24.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
                style = MaterialTheme.typography.headlineMedium
            )
            
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                label = { Text("Paste reel link here") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { /* TODO: Paste from clipboard */ }) {
                        Text("Paste", color = GradientStart)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { /* TODO: Trigger resolve and download */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd)),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Download", color = Color.White, fontSize = 18.sp)
                }
            }
            
            // TODO: Add Progress card and Recent Downloads list
        }
    }
}
