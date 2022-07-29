package com.example.lab09_camerax

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.TextureView
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

private const val REQUEST_CODE_PERMISSIONS = 10
private val REQUIRED_PERMISSIONS =
    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
private val tag = MainActivity::class.java.simpleName

@SuppressLint("RestrictedApi, ClickableViewAccessibility")
class MainActivity : AppCompatActivity(), LifecycleOwner {

    private lateinit var viewFinder: TextureView
    private lateinit var captureButton: ImageButton
    private lateinit var videoCapture: VideoCapture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.view_finder)
        captureButton = findViewById(R.id.capture_button)

        // Verificar los permisos de la camara
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        val file = File(externalMediaDirs.first(), "${System.currentTimeMillis()}.mp4")

        captureButton.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                captureButton.setBackgroundColor(Color.GREEN)
                videoCapture.startRecording(file, object : VideoCapture.OnVideoSavedListener {
                    override fun onVideoSaved(file: File?) {
                        Toast.makeText(baseContext, "Video File: $file", Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(
                        useCaseError: VideoCapture.UseCaseError?,
                        message: String?,
                        cause: Throwable?
                    ) {
                        Toast.makeText(baseContext, "Video Error: $message", Toast.LENGTH_SHORT)
                            .show()
                    }
                })

            } else if (event.action == MotionEvent.ACTION_UP) {
                captureButton.setBackgroundColor(Color.RED)
                videoCapture.stopRecording()
                Toast.makeText(baseContext, "Video File stopped", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this, permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    private fun startCamera() {
        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().build()
        // Build the viewfinder use case
        val preview = Preview(previewConfig)

        // Create a configuration object for the video use case
        val videoCaptureConfig = VideoCaptureConfig.Builder().apply {
            setTargetRotation(viewFinder.display.rotation)
        }.build()
        videoCapture = VideoCapture(videoCaptureConfig)

        preview.setOnPreviewOutputUpdateListener {
            viewFinder.setSurfaceTexture(it.surfaceTexture)
        }

        // Bind use cases to lifecycle
        CameraX.bindToLifecycle(this, preview, videoCapture)
    }
}