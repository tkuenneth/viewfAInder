package de.thomaskuenneth.viewfainder

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.provider.Settings
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
import androidx.core.role.RoleManagerCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.Executor

enum class RoleStatus {
    NOT_HELD, HELD, UNAVAILABLE
}

fun getRoleStatus(held: Boolean): RoleStatus = if (held) RoleStatus.HELD else RoleStatus.NOT_HELD

class MainActivity : ComponentActivity() {

    private val cameraPermissionFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val launcher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionFlow.update { granted }
    }

    private lateinit var manager: RoleManager

    private val roleFlow: MutableStateFlow<RoleStatus> = MutableStateFlow(RoleStatus.NOT_HELD)
    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            roleFlow.update { getRoleStatus(manager.isRoleHeld(RoleManagerCompat.ROLE_ASSISTANT)) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val executor = ContextCompat.getMainExecutor(this)
        val previewView = PreviewView(this)
        val future = ProcessCameraProvider.getInstance(this)
        enableEdgeToEdge()

        manager = getSystemService(RoleManager::class.java)
        manager.run {
            if (isRoleAvailable(RoleManagerCompat.ROLE_ASSISTANT)) {
                roleFlow.update {
                    getRoleStatus(isRoleHeld(RoleManagerCompat.ROLE_ASSISTANT))
                }
            } else roleFlow.update { RoleStatus.UNAVAILABLE }
        }
        val requestRole = {
            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
            roleLauncher.launch(intent)
        }

        setContent {
            MaterialTheme {
                val hasCameraPermission by cameraPermissionFlow.collectAsState()
                val roleObtained by roleFlow.collectAsState()
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
                    previewView = previewView,
                    hasCameraPermission = hasCameraPermission,
                    roleStatus = roleObtained,
                    requestRole = requestRole,
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
