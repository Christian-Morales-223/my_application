package com.example.myapplication

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SensorDashboard(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// Volley Singleton pattern
class VolleySingleton(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: VolleySingleton? = null
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: VolleySingleton(context).also { INSTANCE = it }
            }
    }
    val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(context.applicationContext)
    }
}

@Composable
fun SensorDashboard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val requestQueue = remember { VolleySingleton.getInstance(context).requestQueue }

    var accelerometerData by remember { mutableStateOf(Triple(0f, 0f, 0f)) }
    var gyroscopeData by remember { mutableStateOf(Triple(0f, 0f, 0f)) }

    // Cloud Data States
    var currentPrediction by remember { mutableStateOf("Initializing...") }
    var activityHistory by remember { mutableStateOf<List<ActivityRecord>>(emptyList()) }
    var isHistoryLoading by remember { mutableStateOf(true) }

    // Sensor Registration
    DisposableEffect(Unit) {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        accelerometerData = Triple(it.values[0], it.values[1], it.values[2])
                    } else if (it.sensor.type == Sensor.TYPE_GYROSCOPE) {
                        gyroscopeData = Triple(it.values[0], it.values[1], it.values[2])
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, gyroscope, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Real-time Prediction Loop using Volley
    LaunchedEffect(Unit) {
        while (true) {
            // Fetch prediction from cloud (with fallback)
            currentPrediction = fetchPredictionFromCloud(requestQueue, accelerometerData)
            delay(2000) // Update every 2 seconds
        }
    }

    // Fetch History using Volley
    LaunchedEffect(Unit) {
        activityHistory = fetchHistoryFromCloud(requestQueue)
        isHistoryLoading = false
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Sensor Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            SensorCard(
                title = "Accelerometer",
                data = accelerometerData,
                unit = "m/s²",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        item {
            SensorCard(
                title = "Gyroscope",
                data = gyroscopeData,
                unit = "rad/s",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        item {
            Text(
                text = "Real-time Prediction (Cloud)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            PredictionCard(prediction = currentPrediction)
        }

        item {
            Text(
                text = "Activity History (Cloud)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (isHistoryLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(activityHistory) { record ->
                HistoryItemCard(record)
            }
        }
    }
}

@Composable
fun SensorCard(
    title: String,
    data: Triple<Float, Float, Float>,
    unit: String,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            AxisRow("X", data.first, unit)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = contentColor.copy(alpha = 0.2f))
            AxisRow("Y", data.second, unit)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = contentColor.copy(alpha = 0.2f))
            AxisRow("Z", data.third, unit)
        }
    }
}

@Composable
fun AxisRow(axis: String, value: Float, unit: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$axis Axis",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "%.2f %s".format(value, unit),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PredictionCard(prediction: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Current Activity",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = prediction,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun HistoryItemCard(record: ActivityRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = record.activity,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(record.timestamp)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = record.duration,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// --- Data Models & Volley Cloud Services ---

data class ActivityRecord(
    val id: String,
    val timestamp: Long,
    val activity: String,
    val duration: String
)

suspend fun fetchPredictionFromCloud(queue: RequestQueue, data: Triple<Float, Float, Float>): String = suspendCancellableCoroutine { cont ->
    // Placeholder URL - replace with actual endpoint
    val url = "https://example.com/api/predict" 
    
    val jsonBody = JSONObject().apply {
        put("acc_x", data.first)
        put("acc_y", data.second)
        put("acc_z", data.third)
    }

    val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
        { response ->
            // Assume server returns {"activity": "Walking"}
            val activity = response.optString("activity", "Unknown")
            if (cont.isActive) cont.resume(activity)
        },
        { _ ->
            // Network error or server not reachable
            // Fallback to local logic for demonstration purposes (so the app works without a real server)
            val magnitude = sqrt(data.first * data.first + data.second * data.second + data.third * data.third)
            val fallback = when {
                magnitude < 1.0 -> "Idle"
                magnitude < 11.0 -> "Sitting"
                magnitude < 15.0 -> "Walking"
                else -> "Running"
            }
            if (cont.isActive) cont.resume(fallback)
        }
    )

    queue.add(request)
}

suspend fun fetchHistoryFromCloud(queue: RequestQueue): List<ActivityRecord> = suspendCancellableCoroutine { cont ->
    // Placeholder URL - replace with actual endpoint
    val url = "https://example.com/api/history"

    val request = JsonArrayRequest(Request.Method.GET, url, null,
        { response ->
            val list = mutableListOf<ActivityRecord>()
            for (i in 0 until response.length()) {
                val obj = response.optJSONObject(i)
                list.add(ActivityRecord(
                    id = obj.optString("id"),
                    timestamp = obj.optLong("timestamp"),
                    activity = obj.optString("activity"),
                    duration = obj.optString("duration")
                ))
            }
            if (cont.isActive) cont.resume(list)
        },
        { _ ->
            // Fallback to mock data if network fails
            val now = System.currentTimeMillis()
            val mockData = listOf(
                ActivityRecord(UUID.randomUUID().toString(), now - 3600000, "Running", "35 min"),
                ActivityRecord(UUID.randomUUID().toString(), now - 86400000, "Walking", "1 hr 10 min"),
                ActivityRecord(UUID.randomUUID().toString(), now - 172800000, "Cycling", "45 min"),
                ActivityRecord(UUID.randomUUID().toString(), now - 259200000, "Hiking", "2 hr"),
                ActivityRecord(UUID.randomUUID().toString(), now - 345600000, "Running", "20 min")
            )
            if (cont.isActive) cont.resume(mockData)
        }
    )

    queue.add(request)
}
