package de.thomaskuenneth.viewfainder

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.common.util.concurrent.ListenableFuture
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.Executor

class MainActivity : ComponentActivity() {

    private val cameraPermissionFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val launcher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionFlow.update { granted }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val executor = ContextCompat.getMainExecutor(this)
        val previewView = PreviewView(this)
        val future = ProcessCameraProvider.getInstance(this)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val hasCameraPermission by cameraPermissionFlow.collectAsState()
                    val mainViewModel: MainViewModel = viewModel()
                    val uiState by mainViewModel.uiState.collectAsState()
                    LaunchedEffect(future) {
                        setupCamera(
                            future = future,
                            lifecycleOwner = this@MainActivity,
                            previewView = previewView,
                            executor = executor,
                            rotation = display.rotation
                        ) { mainViewModel.setBitmap(it) }
                    }
                    MainScreen(uiState = uiState,
                        hasCameraPermission = hasCameraPermission,
                        previewView = previewView,
                        askGemini = { mainViewModel.askGemini() },
                        reset = { mainViewModel.reset() })
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionFlow.update { true }
        } else {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupCamera(
        future: ListenableFuture<ProcessCameraProvider>,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        executor: Executor,
        rotation: Int,
        setBitmap: (Bitmap?) -> Unit
    ) {
        future.addListener({
            val cameraProvider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                    it.targetRotation = rotation
                    it.setAnalyzer(executor) { imageProxy ->
                        val matrix = Matrix().also { matrix ->
                            matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                        }
                        val bitmap = imageProxy.toBitmap()
                        val rotatedBitmap = Bitmap.createBitmap(
                            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                        )
                        bitmap.recycle()
                        setBitmap(rotatedBitmap)
                        imageProxy.close()
                    }
                }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer
                )
            } catch (e: Exception) {
                // Handle exceptions, e.g., log the error
            }
        }, executor)
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    uiState: UiState,
    previewView: PreviewView,
    hasCameraPermission: Boolean,
    askGemini: () -> Unit,
    reset: () -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        if (hasCameraPermission) {
            CameraPreview(previewView = previewView,
                onClick = { if (uiState !is UiState.Success) askGemini() })
        }

        when (uiState) {
            is UiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color(0xa0000000))
                        .safeContentPadding()
                ) {
                    MarkdownText(
                        markdown = uiState.outputText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1F)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodyLarge.merge(Color.White)
                    )
                    Button(
                        onClick = { reset() },
                        modifier = Modifier
                            .padding(all = 32.dp)
                            .align(Alignment.End)
                    ) {
                        Text(text = stringResource(id = R.string.done))
                    }
                }
            }

            is UiState.Error -> {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Red,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color(0xa0000000))
                )
            }

            is UiState.Loading -> {
                CircularProgressIndicator()
            }

            is UiState.Initial -> {
                val bitmap by viewModel.bitmap.collectAsState()
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Inside,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .safeContentPadding()
                            .size(200.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(previewView: PreviewView, onClick: () -> Unit) {
    AndroidView(modifier = Modifier
        .fillMaxSize()
        .clickable {
            onClick()
        }, factory = { previewView })
}
