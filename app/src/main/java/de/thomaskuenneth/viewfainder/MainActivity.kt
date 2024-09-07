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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.common.util.concurrent.ListenableFuture
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
