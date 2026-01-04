package com.example.myapplication

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.math.sqrt

// --- COLORS ---
val DeepPurpleBackground = Color(0xFF121223)
val CardBackground = Color(0xFF1E1E30)
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFFAAAAAA)
val AccentCyan = Color(0xFF00E5FF)
val AccentPink = Color(0xFFFF4081)
val GradientPurple = Brush.linearGradient(listOf(Color(0xFF7C4DFF), Color(0xFF512DA8)))
val GradientBlue = Brush.linearGradient(listOf(Color(0xFF448AFF), Color(0xFF2962FF)))
val GradientPink = Brush.linearGradient(listOf(Color(0xFFFF4081), Color(0xFFC51162)))

// --- CONFIGURATION ---
// 10.0.2.2 is required for Android Emulator to see localhost
const val SERVER_URL = "http://10.0.2.2:5000"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DeepPurpleBackground
                ) { innerPadding ->
                    AppNavigation(navController = navController, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

class VolleySingleton(context: Context) {
    companion object {
        @Volatile private var INSTANCE: VolleySingleton? = null
        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: VolleySingleton(context).also { INSTANCE = it }
        }
    }
    val requestQueue: RequestQueue by lazy { Volley.newRequestQueue(context.applicationContext) }
}

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val HISTORY = "history"
}

@Composable
fun AppNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = Routes.LOGIN, modifier = modifier) {
        composable(Routes.LOGIN) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Routes.DASHBOARD) { popUpTo(Routes.LOGIN) { inclusive = true } }
            })
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(onNavigateToHistory = { navController.navigate(Routes.HISTORY) })
        }
        composable(Routes.HISTORY) {
            HistoryScreen()
        }
    }
}

// --- ORIGINAL LOGIN SCREEN ---
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(DeepPurpleBackground).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(100.dp).clip(CircleShape).background(GradientPurple),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Star, null, tint = TextWhite, modifier = Modifier.size(50.dp))
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("RT-Spark Monitor", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = TextWhite)
        Text("Remote Emulation Viewer", style = MaterialTheme.typography.bodyLarge, color = TextGray)
        Spacer(modifier = Modifier.height(48.dp))
        OutlinedTextField(
            value = username, onValueChange = { username = it },
            label = { Text("Username", color = TextGray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                focusedBorderColor = AccentCyan, unfocusedBorderColor = TextGray
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password", color = TextGray) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                focusedBorderColor = AccentCyan, unfocusedBorderColor = TextGray
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onLoginSuccess,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(GradientBlue, shape = RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("LOGIN", fontWeight = FontWeight.Bold, color = TextWhite)
            }
        }
    }
}

// --- ORIGINAL DASHBOARD (MODIFIED FOR REMOTE DATA) ---
@Composable
fun DashboardScreen(onNavigateToHistory: () -> Unit) {
    val context = LocalContext.current
    val requestQueue = remember { VolleySingleton.getInstance(context).requestQueue }

    // Data Holders for Charts
    var accelData by remember { mutableStateOf(Triple(0f, 0f, 0f)) }
    var gyroData by remember { mutableStateOf(Triple(0f, 0f, 0f)) }
    var currentPrediction by remember { mutableStateOf("Connecting...") }
    var currentConfidence by remember { mutableStateOf("0.0") }

    val accelHistory = remember { mutableStateListOf<Triple<Float, Float, Float>>() }
    val gyroHistory = remember { mutableStateListOf<Triple<Float, Float, Float>>() }

    // POLLING LOOP: Fetch data from Server every 300ms
    LaunchedEffect(Unit) {
        while (true) {
            val status = fetchFullStatusFromCloud(requestQueue)

            // Update Activity Text
            currentPrediction = status.activity
            currentConfidence = status.confidence

            // Update Instant Values for Gauges
            accelData = Triple(status.ax, status.ay, status.az)
            gyroData = Triple(status.gx, status.gy, status.gz)

            // Update History for Charts
            accelHistory.add(accelData)
            if (accelHistory.size > 50) accelHistory.removeAt(0)

            gyroHistory.add(gyroData)
            if (gyroHistory.size > 50) gyroHistory.removeAt(0)

            delay(300) // Fast polling for smooth charts
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepPurpleBackground).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Live Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text("Source: RT-Spark Emulation", style = MaterialTheme.typography.bodySmall, color = TextGray)
                }
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(GradientPink))
            }
        }

        // GAUGES ROW
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                // Calculate Magnitude for Visuals
                val accelMag = sqrt(accelData.first * accelData.first + accelData.second * accelData.second + accelData.third * accelData.third)
                val accelProgress = (accelMag / 2.0f).coerceIn(0f, 1f) // Scale for display

                CircularSensorIndicator(value = "%.2f".format(accelMag), label = "Accel G", progress = accelProgress, gradient = GradientPurple)

                val gyroMag = sqrt(gyroData.first * gyroData.first + gyroData.second * gyroData.second + gyroData.third * gyroData.third)
                val gyroProgress = (gyroMag / 5f).coerceIn(0f, 1f)

                CircularSensorIndicator(value = "%.1f".format(gyroMag), label = "Gyro", progress = gyroProgress, gradient = GradientBlue)
            }
        }

        item {
            Text("AI Classification", style = MaterialTheme.typography.titleMedium.merge(TextStyle(brush = GradientPink)), fontWeight = FontWeight.SemiBold)
        }

        item {
            PredictionCard(prediction = "$currentPrediction ($currentConfidence)")
        }

        item {
            SensorChart(title = "Remote Accelerometer", yAxisLabel = "G-Force", data = accelHistory, range = 2.0f)
        }

        item {
            SensorChart(title = "Remote Gyroscope", yAxisLabel = "deg/s", data = gyroHistory, range = 5.0f)
        }

        item {
            Button(
                onClick = onNavigateToHistory,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(GradientBlue, shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("View Database History", color = TextWhite, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- REUSABLE UI COMPONENTS (CHARTS & CARDS) ---
@Composable
fun SensorChart(title: String, yAxisLabel: String, data: List<Triple<Float, Float, Float>>, range: Float, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = modifier.fillMaxWidth().height(200.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextWhite)
                Row {
                    LegendItem(Color(0xFFFF5252), "X")
                    Spacer(modifier = Modifier.width(8.dp))
                    LegendItem(Color(0xFF4CAF50), "Y")
                    Spacer(modifier = Modifier.width(8.dp))
                    LegendItem(Color(0xFF448AFF), "Z")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val pointsCount = 50 // Match history size
                    val xStep = width / (pointsCount - 1)
                    val midY = height / 2

                    drawLine(Color.Gray.copy(alpha = 0.5f), Offset(0f, midY), Offset(width, midY), 1.dp.toPx())

                    fun drawSeries(values: List<Float>, color: Color) {
                        if (values.isEmpty()) return
                        val path = Path()
                        values.forEachIndexed { i, value ->
                            val x = i * xStep
                            val normalized = (value / range).coerceIn(-1f, 1f)
                            val y = midY - (normalized * midY)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
                    }
                    drawSeries(data.map { it.first }, Color(0xFFFF5252))
                    drawSeries(data.map { it.second }, Color(0xFF4CAF50))
                    drawSeries(data.map { it.third }, Color(0xFF448AFF))
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = TextGray, fontSize = 10.sp)
    }
}

@Composable
fun CircularSensorIndicator(value: String, label: String, progress: Float, gradient: Brush) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = CardBackground, radius = size.minDimension / 2, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                drawArc(brush = gradient, startAngle = -90f, sweepAngle = 360 * progress, useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
            }
            Text(text = value, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = TextGray, fontSize = 12.sp)
    }
}

@Composable
fun PredictionCard(prediction: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Live AI Status", color = TextGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            val color = if (prediction.contains("Run")) AccentPink else if (prediction.contains("Stand")) TextGray else AccentCyan
            Text(
                text = prediction,
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// --- ORIGINAL HISTORY SCREEN ---
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val requestQueue = remember { VolleySingleton.getInstance(context).requestQueue }
    var activityHistory by remember { mutableStateOf<List<ActivityRecord>>(emptyList()) }
    var isHistoryLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        activityHistory = fetchHistoryFromCloud(requestQueue)
        isHistoryLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(DeepPurpleBackground).padding(16.dp)) {
        Text("Firebase History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextWhite, modifier = Modifier.padding(bottom = 16.dp))
        if (isHistoryLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AccentCyan) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                items(activityHistory) { record ->
                    HistoryItemCard(record)
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(record: ActivityRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(DeepPurpleBackground), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = AccentPink, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = record.activity, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text(text = record.timestampStr, style = MaterialTheme.typography.bodySmall, color = TextGray)
                }
            }
        }
    }
}

// --- DATA MODELS ---
data class RemoteStatus(
    val activity: String, val confidence: String,
    val ax: Float, val ay: Float, val az: Float,
    val gx: Float, val gy: Float, val gz: Float
)

data class ActivityRecord(val id: String, val timestampStr: String, val activity: String)

// --- NETWORK LOGIC (Pulls Full Data from Server) ---
suspend fun fetchFullStatusFromCloud(queue: RequestQueue): RemoteStatus = suspendCancellableCoroutine { cont ->
    val url = "$SERVER_URL/status"
    val request = JsonObjectRequest(Request.Method.GET, url, null,
        { response ->
            if (cont.isActive) {
                cont.resume(RemoteStatus(
                    activity = response.optString("activity", "Unknown"),
                    confidence = response.optString("confidence", "0.00"),
                    ax = response.optDouble("ax", 0.0).toFloat(),
                    ay = response.optDouble("ay", 0.0).toFloat(),
                    az = response.optDouble("az", 0.0).toFloat(),
                    gx = response.optDouble("gx", 0.0).toFloat(),
                    gy = response.optDouble("gy", 0.0).toFloat(),
                    gz = response.optDouble("gz", 0.0).toFloat()
                ))
            }
        },
        {
            if (cont.isActive) cont.resume(RemoteStatus("Connecting...", "0.0", 0f, 0f, 0f, 0f, 0f, 0f))
        }
    )
    request.retryPolicy = DefaultRetryPolicy(2000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
    queue.add(request)
}

suspend fun fetchHistoryFromCloud(queue: RequestQueue): List<ActivityRecord> = suspendCancellableCoroutine { cont ->
    val url = "$SERVER_URL/history"
    val request = JsonArrayRequest(Request.Method.GET, url, null,
        { response ->
            val list = mutableListOf<ActivityRecord>()
            for (i in 0 until response.length()) {
                val obj = response.optJSONObject(i)
                list.add(ActivityRecord(obj.optString("id"), obj.optString("timestamp"), obj.optString("activity")))
            }
            if (cont.isActive) cont.resume(list)
        },
        {
            if (cont.isActive) cont.resume(listOf(ActivityRecord("0", "Now", "No History Found")))
        }
    )
    queue.add(request)
}