package com.searchoverlay

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var btnOverlay: Button
    private lateinit var btnCapture: Button
    private lateinit var btnStart: Button
    private lateinit var tvStatus: TextView
    private var projectionResultCode: Int = 0
    private var projectionData: Intent? = null

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            projectionResultCode = result.resultCode
            projectionData = result.data
            tvStatus.text = "Screen capture ready"
            startOverlayService()
        } else {
            tvStatus.text = "Capture permission denied"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnOverlay = findViewById(R.id.btnOverlay)
        btnCapture = findViewById(R.id.btnCapture)
        btnStart = findViewById(R.id.btnStart)
        tvStatus = findViewById(R.id.tvStatus)

        btnOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ))
            } else Toast.makeText(this, "Already granted", Toast.LENGTH_SHORT).show()
        }

        btnCapture.setOnClickListener {
            val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projectionLauncher.launch(mpm.createScreenCaptureIntent())
        }

        btnStart.setOnClickListener {
            if (projectionData == null) {
                Toast.makeText(this, "Grant capture first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startOverlayService()
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java).apply {
            putExtra("resultCode", projectionResultCode)
            putExtra("data", projectionData)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
        tvStatus.text = "Service started"
    }
}
