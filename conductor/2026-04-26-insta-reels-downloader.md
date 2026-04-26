# Instagram Reels Downloader Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a lightweight, serverless Instagram Reels downloader for Android (Min API 28, Target API 35) using Kotlin for the UI and Rust for link resolution.

**Architecture:** A Jetpack Compose UI communicates with a Rust-based native library via JNI. The Rust core handles the parsing of Instagram HTML/JSON to find video URLs, while Kotlin manages the UI, file downloads, and history.

**Tech Stack:** Kotlin, Jetpack Compose, Rust (with `reqwest` and `scraper`), JNI, Room, OkHttp.

---

### Task 1: Android Project Scaffolding

**Files:**
- Create: `app/build.gradle.kts`
- Create: `build.gradle.kts`
- Create: `settings.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create the root build.gradle.kts**
```kotlin
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
```

- [ ] **Step 2: Create settings.gradle.kts**
```kotlin
rootProject.name = "InstaDownloader"
include(":app")
```

- [ ] **Step 3: Create app/build.gradle.kts**
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.insta"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.insta"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
}
```

- [ ] **Step 4: Create AndroidManifest.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="Reels Downloader"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@android:style/Theme.Material.Light.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 5: Commit**
```bash
git add .
git commit -m "chore: scaffold android project"
```

---

### Task 2: Rust Core Implementation

**Files:**
- Create: `rust-core/Cargo.toml`
- Create: `rust-core/src/lib.rs`

- [ ] **Step 1: Create Cargo.toml**
```toml
[package]
name = "insta-core"
version = "0.1.0"
edition = "2021"

[lib]
crate-type = ["cdylib"]

[dependencies]
jni = "0.21.1"
reqwest = { version = "0.11", features = ["blocking", "json"] }
scraper = "0.18.1"
regex = "1.10.2"
serde_json = "1.0.108"
```

- [ ] **Step 2: Implement Rust link resolver in src/lib.rs**
```rust
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;
use reqwest::blocking::Client;
use scraper::{Html, Selector};
use regex::Regex;

#[no_mangle]
pub extern "system" fn Java_com_example_insta_MainActivity_resolveUrl(
    mut env: JNIEnv,
    _class: JClass,
    input: JString,
) -> jstring {
    let url: String = env.get_string(&input).expect("Couldn't get java string!").into();
    
    let client = Client::builder()
        .user_agent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
        .build()
        .unwrap();

    let response = client.get(&url).send();
    
    let video_url = match response {
        Ok(res) => {
            let body = res.text().unwrap_or_default();
            // Try meta tag first
            let document = Html::parse_document(&body);
            let selector = Selector::parse("meta[property='og:video']").unwrap();
            
            if let Some(element) = document.select(&selector).next() {
                element.value().attr("content").unwrap_or("").to_string()
            } else {
                // Fallback to regex for __additionalData
                let re = Regex::new(r#""video_url":"([^"]+)""#).unwrap();
                if let Some(caps) = re.captures(&body) {
                    caps.get(1).map_or("", |m| m.as_str()).replace("\\u0026", "&")
                } else {
                    "".to_string()
                }
            }
        }
        Err(_) => "".to_string(),
    };

    let output = env.new_string(video_url).expect("Couldn't create java string!");
    output.into_raw()
}
```

- [ ] **Step 3: Commit**
```bash
git add rust-core/
git commit -m "feat: implement rust link resolver with JNI"
```

---

### Task 3: UI Design (Jetpack Compose)

**Files:**
- Create: `app/src/main/java/com/example/insta/MainActivity.kt`
- Create: `app/src/main/java/com/example/insta/ui/theme/Color.kt`
- Create: `app/src/main/java/com/example/insta/ui/theme/Theme.kt`

- [ ] **Step 1: Define Colors**
```kotlin
package com.example.insta.ui.theme
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val GradientStart = Color(0xFF6200EE)
val GradientEnd = Color(0xFFE91E63)
```

- [ ] **Step 2: Implement UI in MainActivity.kt**
```kotlin
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
import com.example.insta.ui.theme.GradientEnd
import com.example.insta.ui.theme.GradientStart

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.loadLibrary("insta_core")
        setContent {
            MainScreen()
        }
    }

    external fun resolveUrl(url: String): String
}

@Composable
fun MainScreen() {
    var urlText by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Reels Downloader", fontSize = 24.sp, modifier = Modifier.padding(bottom = 32.dp))
        
        OutlinedTextField(
            value = urlText,
            onValueChange = { urlText = it },
            label = { Text("Paste reel link here") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                TextButton(onClick = { /* TODO: Paste from clipboard */ }) {
                    Text("Paste", color = GradientStart)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { /* TODO: Trigger resolve and download */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd)),
                    shape = RoundedCornerShape(28.dp)
                ),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Text("Download", color = Color.White, fontSize = 18.sp)
        }
        
        // TODO: Add Progress card and Recent Downloads list
    }
}
```

- [ ] **Step 3: Commit**
```bash
git add .
git commit -m "feat: implement basic UI with Jetpack Compose"
```

---

### Task 4: Download and Storage Logic

**Files:**
- Create: `app/src/main/java/com/example/insta/downloader/Downloader.kt`

- [ ] **Step 1: Implement Downloader using OkHttp**
```kotlin
package com.example.insta.downloader

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class Downloader(private val client: OkHttpClient) {
    fun download(url: String, destination: File, onProgress: (Float) -> Unit) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            val body = response.body ?: return
            val totalBytes = body.contentLength()
            body.byteStream().use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        onProgress(totalRead.toFloat() / totalBytes)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**
```bash
git add .
git commit -m "feat: add download logic with progress tracking"
```

---

### Task 5: History Persistence (Room)

**Files:**
- Create: `app/src/main/java/com/example/insta/data/History.kt`

- [ ] **Step 1: Define Room Entity and DAO**
```kotlin
package com.example.insta.data

import androidx.room.*

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAll(): List<HistoryItem>

    @Insert
    fun insert(item: HistoryItem)
}

@Database(entities = [HistoryItem::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
```

- [ ] **Step 2: Commit**
```bash
git add .
git commit -m "feat: add Room database for download history"
```

---

### Task 6: Final Integration and Test Execution

- [ ] **Step 1: Connect UI to Logic in MainActivity.kt**
(Implementation of coroutines to call Rust JNI and then Download logic)

- [ ] **Step 2: Run verification tests with provided URLs**
- Verify: `https://www.instagram.com/reel/DXXlxgaCmqf/`
- Verify: `https://www.instagram.com/p/DXaarjVDWB1/`
- Verify: `https://www.instagram.com/reel/DXemXckiESX/`

- [ ] **Step 3: Final Commit**
```bash
git add .
git commit -m "feat: final integration and verification"
```
