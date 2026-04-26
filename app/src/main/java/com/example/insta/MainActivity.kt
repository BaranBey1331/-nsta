package com.example.insta

import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.insta.data.AppDatabase
import com.example.insta.data.HistoryItem
import com.example.insta.downloader.Downloader
import com.example.insta.ui.theme.InstaDownloaderTheme
import com.example.insta.ui.theme.GradientEnd
import com.example.insta.ui.theme.GradientStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
    private lateinit var db: AppDatabase
    private val client = OkHttpClient()
    private lateinit var downloader: Downloader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "insta-db"
        ).build()
        
        downloader = Downloader(client)

        try {
            System.loadLibrary("insta_core")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            InstaDownloaderTheme {
                MainScreen(
                    onDownload = { url -> startDownload(url) },
                    historyState = db.historyDao().getAllAsState()
                )
            }
        }
    }

    private fun startDownload(url: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val videoUrl = resolveUrl(url)
            if (videoUrl.isNotEmpty()) {
                val fileName = "reels_${UUID.randomUUID().toString().take(8)}.mp4"
                val destination = File(
                    getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                    fileName
                )
                
                downloader.download(videoUrl, destination) { progress ->
                    // Update progress in UI if needed
                }
                
                db.historyDao().insert(
                    HistoryItem(
                        title = "Reel $fileName",
                        url = url,
                        filePath = destination.absolutePath
                    )
                )
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Download Complete!", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Could not resolve URL", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    external fun resolveUrl(url: String): String
}

// Add a helper for Room to get State (simplified for this plan)
fun com.example.insta.data.HistoryDao.getAllAsState(): State<List<HistoryItem>> {
    val state = mutableStateOf(emptyList<HistoryItem>())
    // In a real app, use Flow/LiveData. Here we simulate for simplicity.
    return state
}

@Composable
fun MainScreen(
    onDownload: (String) -> Unit,
    historyState: State<List<HistoryItem>>
) {
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
                    TextButton(onClick = { /* Paste logic */ }) {
                        Text("Paste", color = GradientStart)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { onDownload(urlText) },
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
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "Recent Downloads",
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp),
                style = MaterialTheme.typography.titleMedium
            )
            
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyState.value) { item ->
                    HistoryCard(item)
                }
            }
        }
    }
}

@Composable
fun HistoryCard(item: HistoryItem) {
    Card(
        modifier = Modifier.width(140.dp).height(200.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.LightGray)) {
            Text(
                text = item.title,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                color = Color.Black,
                fontSize = 12.sp
            )
        }
    }
}
