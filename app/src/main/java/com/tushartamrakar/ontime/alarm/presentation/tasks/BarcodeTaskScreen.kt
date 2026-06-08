package com.tushartamrakar.ontime.alarm.presentation.tasks

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.tushartamrakar.ontime.alarm.domain.BarcodeItem
import com.tushartamrakar.ontime.alarm.domain.TaskType
import com.tushartamrakar.ontime.alarm.domain.WakeUpTask
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.CardBackground
import com.tushartamrakar.ontime.core.ui.theme.Danger
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.Success
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import java.util.UUID
import java.util.concurrent.Executors

// ─── Camera Preview Composable ────────────────────────────────────────────────
@Composable
fun CameraPreviewView(
    onBarcodeScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var lastScanned = remember { "" }
    var lastScannedTime = remember { 0L }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees,
                                )
                                val scanner = BarcodeScanning.getClient()
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        barcodes.firstOrNull()?.rawValue?.let { value ->
                                            val now = System.currentTimeMillis()
                                            if (value != lastScanned || now - lastScannedTime > 2000) {
                                                lastScanned = value
                                                lastScannedTime = now
                                                onBarcodeScanned(value)
                                            }
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize(),
    )
}

// ─── Barcode Task Config UI ───────────────────────────────────────────────────
@Composable
fun BarcodeTaskConfigSheet(
    onSave: (WakeUpTask) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var barcodes by remember { mutableStateOf(listOf<BarcodeItem>()) }
    var itemName by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingBarcode by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) isScanning = true
    }

    if (isScanning) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraPreviewView(
                onBarcodeScanned = { scannedValue ->
                    pendingBarcode = scannedValue
                    isScanning = false
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
            ) {
                Text(
                    text = "Point camera at barcode",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            IconButton(
                onClick = { isScanning = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = "📷 Barcode Scan",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Scan registered items to dismiss your alarm (max 20)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MulishFamily,
            color = TextMuted,
        )

        Spacer(modifier = Modifier.height(24.dp))

        pendingBarcode?.let { scanned ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Success.copy(alpha = 0.1f))
                    .border(1.dp, Success, RoundedCornerShape(12.dp))
                    .padding(12.dp),
            ) {
                Text(
                    text = "✅ Scanned: $scanned\nEnter a name and tap Add",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily,
                    color = Success,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                placeholder = {
                    Text(
                        text = "Item name (e.g. Milk)",
                        color = TextMuted,
                        fontFamily = MulishFamily,
                        fontSize = 14.sp,
                    )
                },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Border,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Primary,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground,
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = MulishFamily,
                    fontSize = 14.sp,
                    color = TextPrimary,
                ),
            )

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Primary)
                    .clickable {
                        if (hasCameraPermission) {
                            isScanning = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = "Scan",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (pendingBarcode != null && itemName.isNotBlank() && barcodes.size < 20) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Success)
                    .clickable {
                        barcodes = barcodes + BarcodeItem(
                            id = UUID.randomUUID().toString(),
                            name = itemName.trim(),
                            barcode = pendingBarcode!!,
                        )
                        itemName = ""
                        pendingBarcode = null
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✅ Add Item",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = Color.White,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (barcodes.isNotEmpty()) {
            Text(
                text = "REGISTERED ITEMS (${barcodes.size}/20)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color = TextMuted,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))

            barcodes.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceHigh)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MulishFamily,
                            color = TextPrimary,
                        )
                        Text(
                            text = item.barcode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily,
                            color = TextMuted,
                        )
                    }
                    IconButton(
                        onClick = {
                            barcodes = barcodes.toMutableList().also { it.removeAt(index) }
                        },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove",
                            tint = Danger,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val canSave = barcodes.isNotEmpty()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (canSave) Primary else SurfaceHigh)
                .clickable {
                    if (canSave) {
                        onSave(
                            WakeUpTask(
                                type = TaskType.BARCODE,
                                barcodes = barcodes,
                            )
                        )
                        onDismiss()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (canSave) "Add Barcode Task" else "Scan at least 1 item",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color = if (canSave) Color.White else TextMuted,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─── Barcode Task Runtime UI ──────────────────────────────────────────────────
@Composable
fun BarcodeTaskRuntimeScreen(
    task: WakeUpTask,
    onUserActiveChange: (Boolean) -> Unit,
    onTaskCompleted: () -> Unit,
) {
    val context = LocalContext.current
    val registeredBarcodes = task.barcodes
    var scannedItems by remember { mutableStateOf(setOf<String>()) }
    var lastMessage by remember { mutableStateOf("") }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    // Camera always active = timer always paused
    LaunchedEffect(Unit) {
        onUserActiveChange(true)
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(scannedItems) {
        if (scannedItems.size >= registeredBarcodes.size && registeredBarcodes.isNotEmpty()) {
            onUserActiveChange(false)
            onTaskCompleted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Background)
                .padding(24.dp),
        ) {
            Column {
                Text(
                    text = "📷 Scan to Dismiss",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = TextPrimary,
                )
                Text(
                    text = "${scannedItems.size}/${registeredBarcodes.size} items scanned",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily,
                    color = Primary,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            registeredBarcodes.forEach { item ->
                val isScanned = scannedItems.contains(item.barcode)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isScanned) Success.copy(alpha = 0.1f) else SurfaceHigh)
                        .border(
                            1.dp,
                            if (isScanned) Success else Border,
                            RoundedCornerShape(12.dp),
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = if (isScanned) "✅" else "⬜", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily,
                        color = if (isScanned) Success else TextPrimary,
                    )
                }
            }
        }

        if (lastMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = lastMessage,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color = if (lastMessage.startsWith("✅")) Success else Danger,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp)),
            ) {
                CameraPreviewView(
                    onBarcodeScanned = { scannedValue ->
                        val matchedItem = registeredBarcodes.find { it.barcode == scannedValue }
                        if (matchedItem != null && !scannedItems.contains(scannedValue)) {
                            scannedItems = scannedItems + scannedValue
                            lastMessage = "✅ ${matchedItem.name} scanned!"
                        } else if (matchedItem == null) {
                            lastMessage = "❌ Unknown item!"
                        }
                    },
                )
            }
        }
    }
}