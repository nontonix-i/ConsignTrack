package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.RoseError
import com.example.ui.theme.SupabaseGreen
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ImagePickerHelper {

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String {
        val photosDir = File(context.filesDir, "photos")
        if (!photosDir.exists()) photosDir.mkdirs()

        // Scale down if extraordinarily large so saving & loading is fast while keeping sharp detail
        val maxDim = 1600
        val processedBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
            val targetW = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
            val targetH = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        } else {
            bitmap
        }

        val filename = "photo_${System.currentTimeMillis()}.jpg"
        val file = File(photosDir, filename)
        FileOutputStream(file).use { out ->
            processedBitmap.compress(Bitmap.CompressFormat.JPEG, 86, out)
        }
        return Uri.fromFile(file).toString()
    }

    fun copyUriToInternalStorage(context: Context, sourceUri: Uri): String {
        val photosDir = File(context.filesDir, "photos")
        if (!photosDir.exists()) photosDir.mkdirs()

        val filename = "gallery_${System.currentTimeMillis()}.jpg"
        val file = File(photosDir, filename)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return Uri.fromFile(file).toString()
    }

    fun imageProxyToBitmap(imageProxy: ImageProxy, isFrontCamera: Boolean): Bitmap {
        val rawBitmap = imageProxy.toBitmap()
        val rotation = imageProxy.imageInfo.rotationDegrees
        if (rotation == 0 && !isFrontCamera) return rawBitmap

        val matrix = Matrix().apply {
            if (rotation != 0) {
                postRotate(rotation.toFloat())
            }
            if (isFrontCamera) {
                postScale(-1f, 1f, rawBitmap.width / 2f, rawBitmap.height / 2f)
            }
        }
        return Bitmap.createBitmap(
            rawBitmap,
            0,
            0,
            rawBitmap.width,
            rawBitmap.height,
            matrix,
            true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoChooserBottomSheet(
    title: String = "Pilih Sumber Foto",
    hasExistingPhoto: Boolean = false,
    existingPhotoUri: String? = null,
    onPhotoSelected: (uriString: String) -> Unit,
    onDeletePhoto: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showInAppCamera by remember { mutableStateOf(false) }

    // Gallery Launcher (Zero-Permission Android Photo Picker)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localUriString = ImagePickerHelper.copyUriToInternalStorage(context, uri)
            onPhotoSelected(localUriString)
            onDismiss()
        }
    }

    if (showInAppCamera) {
        InAppCameraDialog(
            title = title,
            onPhotoCaptured = { savedUri ->
                showInAppCamera = false
                onPhotoSelected(savedUri)
                onDismiss()
            },
            onOpenGallery = {
                showInAppCamera = false
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismiss = {
                showInAppCamera = false
            }
        )
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = androidx.compose.ui.unit.Dp.Unspecified,
        containerColor = CharcoalSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Kamera bawaan aplikasi • Cepat & tanpa suara",
                        fontSize = 11.sp,
                        color = SupabaseGreen
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondaryDark)
                }
            }

            if (!existingPhotoUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CharcoalBg)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp))
                ) {
                    AsyncImage(
                        model = existingPhotoUri,
                        contentDescription = "Foto Saat Ini",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Foto Tersimpan",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SupabaseGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action 1: Built-in Silent Fast In-App Camera
            PhotoSourceOption(
                icon = Icons.Default.CameraAlt,
                iconTint = SupabaseGreen,
                title = "Kamera Cepat In-App (Tanpa Suara)",
                subtitle = "Jepret instan langsung di dalam aplikasi tanpa suara kamera",
                badgeText = "INSTAN",
                onClick = { showInAppCamera = true },
                modifier = Modifier.testTag("open_in_app_camera_button")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action 2: Gallery
            PhotoSourceOption(
                icon = Icons.Default.PhotoLibrary,
                iconTint = Color(0xFF38BDF8),
                title = "Pilih dari Galeri Foto",
                subtitle = "Ambil dari foto yang sudah ada di HP",
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.testTag("open_gallery_picker_button")
            )

            // Action 3: Delete option
            if (hasExistingPhoto && onDeletePhoto != null) {
                Spacer(modifier = Modifier.height(8.dp))
                PhotoSourceOption(
                    icon = Icons.Default.Delete,
                    iconTint = RoseError,
                    title = "Hapus Foto",
                    subtitle = "Hapus foto yang tersimpan saat ini",
                    onClick = {
                        onDeletePhoto()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("delete_photo_button")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Custom Built-In CameraX Dialog — Fast, Minimalist, 100% Silent (No Shutter Sound),
 * with Instant 1-Tap Capture ("Langsung Simpan") or Preview Mode ("Tinjau Dulu").
 */
@Composable
fun InAppCameraDialog(
    title: String = "Kamera In-App",
    onPhotoCaptured: (String) -> Unit,
    onOpenGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchOn by remember { mutableStateOf(false) }
    var instantSaveMode by remember { mutableStateOf(true) } // Default: 1-tap instant silent capture
    var isCapturing by remember { mutableStateOf(false) }
    var showShutterFlash by remember { mutableStateOf(false) }
    var capturedPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var focusIndicatorOffset by remember { mutableStateOf<Offset?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // COMPATIBLE (TextureView) renders cleanly inside Compose Dialogs and allows 0ms silent bitmap capture
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }

    // Bind CameraX lifecycle whenever permission or lensFacing changes
    LaunchedEffect(hasCameraPermission, lensFacing) {
        if (!hasCameraPermission) return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build()
                        .also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build()

                    val selector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    cameraProvider.unbindAll()
                    val cam = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageCapture
                    )
                    cameraInstance = cam
                    imageCaptureUseCase = imageCapture
                    isTorchOn = false

                    // Configure tap-to-focus on PreviewView
                    previewView.setOnTouchListener { view, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            view.performClick()
                            val factory = previewView.meteringPointFactory
                            val point = factory.createPoint(event.x, event.y)
                            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                .build()
                            cam.cameraControl.startFocusAndMetering(action)
                            focusIndicatorOffset = Offset(event.x, event.y)
                        }
                        true
                    }
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    // Auto-hide focus ring after 900ms
    LaunchedEffect(focusIndicatorOffset) {
        if (focusIndicatorOffset != null) {
            delay(900)
            focusIndicatorOffset = null
        }
    }

    // Turn off torch and unbind when closing dialog
    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                cameraInstance?.cameraControl?.enableTorch(false)
            }
        }
    }

    BackHandler {
        if (capturedPreviewBitmap != null) {
            capturedPreviewBitmap = null
        } else {
            onDismiss()
        }
    }

    fun handleCapturedBitmap(bitmap: Bitmap) {
        if (instantSaveMode) {
            scope.launch {
                val uriStr = withContext(Dispatchers.IO) {
                    ImagePickerHelper.saveBitmapToInternalStorage(context, bitmap)
                }
                isCapturing = false
                onPhotoCaptured(uriStr)
            }
        } else {
            capturedPreviewBitmap = bitmap
            isCapturing = false
        }
    }

    fun triggerSilentCapture() {
        if (isCapturing) return
        isCapturing = true
        scope.launch {
            showShutterFlash = true
            delay(75)
            showShutterFlash = false
        }

        // Fast & 100% Silent Path: grab live frame directly from PreviewView buffer (0ms shutter lag, zero HAL shutter sound)
        val liveFrame = previewView.bitmap
        if (liveFrame != null) {
            handleCapturedBitmap(liveFrame)
            return
        }

        // Fallback Path: ImageCapture in-memory callback (no MediaActionSound)
        val capture = imageCaptureUseCase
        if (capture == null) {
            isCapturing = false
            return
        }
        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bmp = runCatching {
                        ImagePickerHelper.imageProxyToBitmap(
                            image,
                            isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT
                        )
                    }.getOrNull()
                    image.close()
                    if (bmp != null) {
                        handleCapturedBitmap(bmp)
                    } else {
                        isCapturing = false
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .systemBarsPadding()
        ) {
            if (!hasCameraPermission) {
                // Minimalist Permission Request State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(SupabaseGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = SupabaseGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Izin Kamera Diperlukan",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Izinkan akses kamera untuk mengambil foto warung dan rak barang secara cepat & tanpa suara langsung di dalam aplikasi.",
                        fontSize = 12.5.sp,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark)
                        ) {
                            Text("Batal")
                        }
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SupabaseGreen,
                                contentColor = Color(0xFF042114)
                            )
                        ) {
                            Text("Izinkan Kamera", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (capturedPreviewBitmap != null) {
                // Preview Frozen Frame Mode ("Tinjau Dulu")
                val previewBmp = capturedPreviewBitmap!!
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = previewBmp.asImageBitmap(),
                        contentDescription = "Hasil Foto",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top Bar in Preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tinjau Hasil Foto",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(
                            onClick = { capturedPreviewBitmap = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                        }
                    }

                    // Bottom Action Bar in Preview (Ulangi / Gunakan Foto)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(CharcoalSurface.copy(alpha = 0.92f))
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { capturedPreviewBitmap = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Foto Ulang", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                isCapturing = true
                                scope.launch {
                                    val savedUri = withContext(Dispatchers.IO) {
                                        ImagePickerHelper.saveBitmapToInternalStorage(context, previewBmp)
                                    }
                                    isCapturing = false
                                    onPhotoCaptured(savedUri)
                                }
                            },
                            enabled = !isCapturing,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SupabaseGreen,
                                contentColor = Color(0xFF042114)
                            )
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF042114)
                                )
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gunakan Foto", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Live CameraX Viewfinder
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Tap-to-focus reticle indicator
                    focusIndicatorOffset?.let { tapPos ->
                        val density = LocalDensity.current
                        val boxSizePx = with(density) { 56.dp.toPx() }
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (tapPos.x - boxSizePx / 2f).roundToInt(),
                                        (tapPos.y - boxSizePx / 2f).roundToInt()
                                    )
                                }
                                .size(56.dp)
                                .border(1.5.dp, SupabaseGreen, RoundedCornerShape(8.dp))
                        )
                    }

                    // Silent visual shutter flash (no sound!)
                    AnimatedVisibility(
                        visible = showShutterFlash,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.45f))
                        )
                    }

                    // Top Overlay Controls: Title + Silent Badge + Torch + Close
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.VolumeOff,
                                    contentDescription = null,
                                    tint = SupabaseGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Tanpa Suara • Ketuk layar untuk fokus",
                                    fontSize = 10.5.sp,
                                    color = SupabaseGreen
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Torch / Flash Button (only for back camera with flash unit)
                            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                IconButton(
                                    onClick = {
                                        val nextTorch = !isTorchOn
                                        isTorchOn = nextTorch
                                        runCatching {
                                            cameraInstance?.cameraControl?.enableTorch(nextTorch)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (isTorchOn) AmberWarning.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                        contentDescription = "Lampu Senter",
                                        tint = if (isTorchOn) AmberWarning else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Close Camera Button
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Tutup Kamera",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Bottom Camera Control Deck
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(CharcoalBg.copy(alpha = 0.88f))
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Mode Toggle Pill: "Langsung Simpan ⚡" vs "Tinjau Dulu"
                        Row(
                            modifier = Modifier
                                .background(CharcoalSurfaceElevated, RoundedCornerShape(20.dp))
                                .border(1.dp, CharcoalBorder, RoundedCornerShape(20.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (instantSaveMode) SupabaseGreen else Color.Transparent)
                                    .clickable { instantSaveMode = true }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = if (instantSaveMode) Color(0xFF042114) else TextSecondaryDark,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Langsung Simpan",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (instantSaveMode) Color(0xFF042114) else TextSecondaryDark
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (!instantSaveMode) SupabaseGreen else Color.Transparent)
                                    .clickable { instantSaveMode = false }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Tinjau Dulu",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!instantSaveMode) Color(0xFF042114) else TextSecondaryDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Main Row: Gallery Shortcut (Left) | Silent Shutter Button (Center) | Flip Camera (Right)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Quick Gallery Button
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onOpenGallery() }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(CharcoalSurfaceElevated, CircleShape)
                                        .border(1.dp, CharcoalBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PhotoLibrary,
                                        contentDescription = "Pilih dari Galeri",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Galeri", fontSize = 10.sp, color = TextSecondaryDark)
                            }

                            // Center: Fast Silent Shutter Button
                            Box(
                                modifier = Modifier
                                    .size(74.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, SupabaseGreen, CircleShape)
                                    .padding(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isCapturing) SupabaseGreen.copy(alpha = 0.5f) else SupabaseGreen)
                                    .clickable(enabled = !isCapturing) { triggerSilentCapture() }
                                    .testTag("in_app_camera_shutter_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCapturing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp,
                                        color = Color(0xFF042114)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Ambil Foto Tanpa Suara",
                                        tint = Color(0xFF042114),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            // Right: Flip Front/Back Camera
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                        CameraSelector.LENS_FACING_FRONT
                                    } else {
                                        CameraSelector.LENS_FACING_BACK
                                    }
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(CharcoalSurfaceElevated, CircleShape)
                                        .border(1.dp, CharcoalBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Cameraswitch,
                                        contentDescription = "Putar Kamera",
                                        tint = TextPrimaryDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Putar", fontSize = 10.sp, color = TextSecondaryDark)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoSourceOption(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = CharcoalSurfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                    if (badgeText != null) {
                        Box(
                            modifier = Modifier
                                .background(SupabaseGreen.copy(alpha = 0.18f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SupabaseGreen
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = TextSecondaryDark
                )
            }
        }
    }
}
