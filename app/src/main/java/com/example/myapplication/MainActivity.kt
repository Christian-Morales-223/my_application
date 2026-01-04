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
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.myapplication.ui.theme.AccentCyan
import com.example.myapplication.ui.theme.AccentPink
import com.example.myapplication.ui.theme.CardBackground
import com.example.myapplication.ui.theme.DeepPurpleBackground
import com.example.myapplication.ui.theme.GradientBlue
import com.example.myapplication.ui.theme.GradientPink
import com.example.myapplication.ui.theme.GradientPurple
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.TextGray
import com.example.myapplication.ui.theme.TextWhite
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
                navController.navigate(Routes.DASHBOARD) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            })
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) }
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen()
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepPurpleBackground)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(GradientPurple),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = TextWhite,
                modifier = Modifier.size(50.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
        Text(
            text = "Sign in to continue",
            style = MaterialTheme.typography.bodyLarge,
            color = TextGray
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username", color = TextGray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = TextGray,
                cursorColor = AccentCyan
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = TextGray) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = TextGray,
                cursorColor = AccentCyan
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
                modifier = Modifier
                    .fillMaxSize()
                    .background(GradientBlue, shape = RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LOGIN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(onNavigateToHistory: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val requestQueue = remember { VolleySingleton.getInstance(context).requestQueue }

    var accelerometerData by remember { mutableStateOf(Triple(0f, 0f, 0f)) }
    var gyroscopeData by remember { mutableStateOf(Triple(0f, 0f, 0f)) }
    var currentPrediction by remember { mutableStateOf("Initializing...") }

    // Chart Data State
    val accelHistory = remember { mutableStateListOf<Float>() }
    val gyroHistory = remember { mutableStateListOf<Float>() }

    // Sensor Registration
    DisposableEffect(Unit) {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val x = it.values[0]
                        val y = it.values[1]
                        val z = it.values[2]
                        accelerometerData = Triple(x, y, z)
                        
                        // Calculate Magnitude for Chart
                        val mag = sqrt(x*x + y*y + z*z)
                        accelHistory.add(mag)
                        if (accelHistory.size > 20) accelHistory.removeAt(0)
                        
                    } else if (it.sensor.type == Sensor.TYPE_GYROSCOPE) {
                        val x = it.values[0]
                        val y = it.values[1]
                        val z = it.values[2]
                        gyroscopeData = Triple(x, y, z)
                        
                         // Calculate Magnitude for Chart
                        val mag = sqrt(x*x + y*y + z*z)
                        gyroHistory.add(mag)
                        if (gyroHistory.size > 20) gyroHistory.removeAt(0)
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

    // Real-time Prediction Loop
    LaunchedEffect(Unit) {
        while (true) {
            currentPrediction = fetchPredictionFromCloud(requestQueue, accelerometerData)
            delay(2000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepPurpleBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sensor Dashboard",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "Real-time monitoring",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
            // User Icon Placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GradientPink)
            )
        }

        // Circular Indicators Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Visualize Accel Magnitude
            val accelMag = sqrt(accelerometerData.first * accelerometerData.first + 
                              accelerometerData.second * accelerometerData.second + 
                              accelerometerData.third * accelerometerData.third)
            val accelProgress = (accelMag / 20f).coerceIn(0f, 1f)
            
            CircularSensorIndicator(
                value = "%.1f".format(accelMag),
                label = "Accel",
                progress = accelProgress,
                gradient = GradientPurple
            )

            // Visualize Gyro Magnitude
            val gyroMag = sqrt(gyroscopeData.first * gyroscopeData.first + 
                             gyroscopeData.second * gyroscopeData.second + 
                             gyroscopeData.third * gyroscopeData.third)
            val gyroProgress = (gyroMag / 5f).coerceIn(0f, 1f)

            CircularSensorIndicator(
                value = "%.1f".format(gyroMag),
                label = "Gyro",
                progress = gyroProgress,
                gradient = GradientBlue
            )
        }
        
        Text(
            text = "Check your status",
            style = MaterialTheme.typography.titleMedium.merge(
                TextStyle(brush = GradientPink)
            ),
            fontWeight = FontWeight.SemiBold
        )

        // Main Prediction Card (Chart removed)
        PredictionCard(prediction = currentPrediction)

        // Sensor Activity Line Chart (Real-time Accel & Gyro)
        LineChartCard(accelData = accelHistory, gyroData = gyroHistory)

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNavigateToHistory,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GradientBlue, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("View History", color = TextWhite, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LineChartCard(accelData: List<Float>, gyroData: List<Float>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier.fillMaxWidth().height(250.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Real-time Sensor Magnitude", color = TextGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF4285F4), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Accel", color = TextGray, fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF69F0AE), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gyro", color = TextGray, fontSize = 10.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val bottomMargin = 20.dp.toPx()
                    val chartHeight = height - bottomMargin
                    
                    val maxVal = 20f // Approximate max magnitude for display scaling
                    val pointsCount = 20 // Fixed window size
                    
                    val xStep = width / (pointsCount - 1)
                    
                    // Draw Grid
                    val xGridCount = 5
                    for (i in 0..xGridCount) {
                        val x = i * (width / xGridCount)
                         drawLine(
                            color = Color.Gray.copy(alpha = 0.1f),
                            start = Offset(x, 0f),
                            end = Offset(x, chartHeight),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw Horizontal Grid lines
                    val ySteps = 5
                    val yStepPx = chartHeight / ySteps
                    for (i in 0..ySteps) {
                         val y = i * yStepPx
                         drawLine(
                            color = Color.Gray.copy(alpha = 0.1f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    fun drawSeries(values: List<Float>, color: Color) {
                        if (values.isEmpty()) return
                        val path = Path()
                        
                        // We want to draw the most recent data points filling the chart from right to left or fixed window
                        // Let's assume the list is a fixed sliding window of 'pointsCount'
                        
                        values.forEachIndexed { i, value ->
                            val x = i * xStep
                            // Clamp value to maxVal for drawing purposes
                            val clampedVal = value.coerceIn(0f, maxVal)
                            val y = chartHeight - (clampedVal / maxVal * chartHeight)
                            
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            
                            // Draw Point
                            drawCircle(color, radius = 3.dp.toPx(), center = Offset(x, y))
                        }
                        drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
                    }

                    drawSeries(accelData, Color(0xFF4285F4)) // Blue - Accel
                    drawSeries(gyroData, Color(0xFF69F0AE)) // Green/Teal - Gyro
                }
            }
        }
    }
}

@Composable
fun CircularSensorIndicator(
    value: String,
    label: String,
    progress: Float,
    gradient: Brush
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = CardBackground,
                    radius = size.minDimension / 2,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    brush = gradient,
                    startAngle = -90f,
                    sweepAngle = 360 * progress,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Text(text = value, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, color = TextGray, fontSize = 14.sp)
    }
}

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val requestQueue = remember { VolleySingleton.getInstance(context).requestQueue }
    var activityHistory by remember { mutableStateOf<List<ActivityRecord>>(emptyList()) }
    var isHistoryLoading by remember { mutableStateOf(true) }

    // Fetch History
    LaunchedEffect(Unit) {
        activityHistory = fetchHistoryFromCloud(requestQueue)
        isHistoryLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepPurpleBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Activity History",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isHistoryLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentCyan)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(activityHistory) { record ->
                    HistoryItemCard(record)
                }
            }
        }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Current Status", color = TextGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = prediction,
                style = MaterialTheme.typography.headlineMedium.merge(
                    TextStyle(brush = GradientPink)
                ),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HistoryItemCard(record: ActivityRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DeepPurpleBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AccentPink,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = record.activity,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(record.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }
            Text(
                text = record.duration,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = AccentCyan
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
            // Fallback to local logic for demonstration purposes
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
