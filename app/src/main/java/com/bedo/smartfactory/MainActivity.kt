package com.bedo.smartfactory

import android.net.ConnectivityManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.IOException
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val source = ModuleSource(getSystemService(ConnectivityManager::class.java))
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) { Monitor(source) }
        }
    }
}

private val Canvas = Color(0xFFF5F1E8)
private val Ink = Color(0xFF242424)
private val Muted = Color(0xFF595959)

@Composable
private fun Monitor(source: ModuleSource) {
    var demo by rememberSaveable { mutableStateOf(true) }
    var reading by remember(demo) { mutableStateOf<Telemetry?>(null) }
    var problem by remember(demo) { mutableStateOf<String?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(demo, lifecycle, source) {
        if (!demo) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            reading = null
            problem = null
            while (true) {
                try {
                    reading = withContext(Dispatchers.IO) { source.read() }
                    problem = null
                } catch (_: IOException) {
                    reading = null
                    problem = "Disconnected · Join TH-Module Wi-Fi"
                } catch (_: JSONException) {
                    reading = null
                    problem = "Unexpected sensor data"
                }
                delay(2000)
            }
        }
    }

    val sample = reading
    val status = when {
        demo -> "Demo · Simulated readings"
        problem != null -> problem.orEmpty()
        sample?.temperature == null && sample?.humidity == null -> "Waiting for first reading"
        sample?.stale == true -> "Reading is stale"
        sample?.ageSeconds != null -> "Connected · Updated ${sample.ageSeconds} s ago"
        else -> "Connected"
    }
    val statusColor = when {
        demo -> Muted
        problem != null -> Color(0xFFC9362B)
        sample == null || sample.stale -> Color(0xFF914D00)
        else -> Color(0xFF2E8B57)
    }
    val current = sample?.takeUnless { it.stale || problem != null }

    Surface(color = Canvas, contentColor = Ink, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.bedo_logo), "BEDO", Modifier.size(44.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Smart Factory", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Temperature & Humidity", color = Muted, fontSize = 13.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(statusColor, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(status, Modifier.weight(1f), color = Muted, fontSize = 13.sp)
                Switch(
                    checked = demo,
                    onCheckedChange = { demo = it },
                    thumbContent = { Text(if (demo) "D" else "L", fontSize = 10.sp) },
                )
            }
            ReadingCard("TEMPERATURE", if (demo) 25.37 else current?.temperature, "°C",
                Color(0xFF914D00), Color(0xFFFFE9D5))
            ReadingCard("RELATIVE HUMIDITY", if (demo) 45.12 else current?.humidity, "%RH",
                Color(0xFF00658B), Color(0xFFDDF3FF),
                unavailable = !demo && current?.temperature != null && current?.humidity == null)
        }
    }
}

@Composable
private fun ReadingCard(
    label: String, value: Double?, unit: String, accent: Color, tint: Color,
    unavailable: Boolean = false,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label, Modifier.background(tint, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
                color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(value?.let { String.format(Locale.US, "%.2f", it) } ?: "—",
                    Modifier.alignByBaseline(), fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Text(if (unavailable) "Unavailable" else unit, Modifier.alignByBaseline(),
                    color = accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
