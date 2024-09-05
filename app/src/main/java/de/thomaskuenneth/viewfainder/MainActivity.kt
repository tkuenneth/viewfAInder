package de.thomaskuenneth.viewfainder

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jeziellago.compose.markdowntext.MarkdownText

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen(mainViewModel: MainViewModel = viewModel()) {
    val uiState by mainViewModel.uiState.collectAsState()
    var hasCameraPermission by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(true) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            hasCameraPermission = true
        } else {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Box {
        if (hasCameraPermission) {
            CameraPreview(
                updateBitmap = { mainViewModel.setBitmap(it) },
                onClick = { mainViewModel.askGemini() })
        }
        (uiState as? UiState.Success)?.let {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color(0xa0000000))
                    .safeContentPadding()
            ) {
                MarkdownText(
                    markdown = it.outputText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1F)
                        .verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodyLarge.merge(Color.White)
                )
                Button(
                    onClick = { mainViewModel.reset() },
                    modifier = Modifier
                        .padding(all = 32.dp)
                        .align(Alignment.End)
                ) {
                    Text(text = stringResource(id = R.string.done))
                }
            }
        }
        (uiState as? UiState.Error)?.let {
            Text(
                text = it.errorMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color(0xa0000000))
            )
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(updateBitmap: (Bitmap?) -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(modifier = Modifier
        .fillMaxSize()
        .clickable {
            onClick()
        }, factory = { ctx ->
        val previewView = PreviewView(ctx)
        val executor = ContextCompat.getMainExecutor(ctx)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Use setBackpressureStrategy instead ofsetAnalysisMode
                .build().also {
                    it.setAnalyzer(executor) { imageProxy ->
                        updateBitmap(imageProxy.toBitmap())
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
        previewView
    })
}