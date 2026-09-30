package com.searchoverlay

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageButton
import androidx.core.app.NotificationCompat

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var buttonView: View
    private var searchView: SearchOverlayView? = null
    private lateinit var screenCapture: ScreenCapture
    private lateinit var ocrProcessor: OcrProcessor

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        ocrProcessor = OcrProcessor()
        createFloatingButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>("data")
        if (resultCode != 0 && data != null) {
            screenCapture = ScreenCapture(this, resultCode, data)
        }
        return START_STICKY
    }

    private fun createFloatingButton() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        buttonView = inflater.inflate(R.layout.overlay_button, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 300
        buttonView.setOnTouchListener(DragListener(params))
        buttonView.findViewById<ImageButton>(R.id.btnScan).setOnClickListener { performScan() }
        windowManager.addView(buttonView, params)
    }

    private fun performScan() {
        if (!::screenCapture.isInitialized) return
        if (searchView != null) return
        screenCapture.capture { bitmap ->
            if (bitmap == null) return@capture
            ocrProcessor.extract(bitmap) { text ->
                val query = filterText(text)
                if (!query.isNullOrEmpty()) showSearchOverlay(query)
            }
        }
    }

    private fun filterText(raw: String): String? {
        val cleaned = raw.replace(Regex("[^A-Za-z0-9 .,?!'\\-]"), " ")
            .replace(Regex("\\s+"), " ").trim()
        return if (cleaned.length >= 3) cleaned.take(200) else null
    }

    private fun showSearchOverlay(query: String) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        searchView = SearchOverlayView(this, windowManager, inflater, query) {
            searchView?.let { windowManager.removeView(it) }
            searchView = null
        }
        searchView?.show()
    }

    private fun buildNotification(): Notification {
        val channelId = "search_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Search Overlay", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Search Overlay Active")
            .setContentText("Tap floating button to scan")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setPriority(NotificationCompat.PRIORITY_LOW).build()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { if (::buttonView.isInitialized) windowManager.removeView(buttonView) } catch (_: Exception) {}
        try { searchView?.let { windowManager.removeView(it) } } catch (_: Exception) {}
        try { if (::screenCapture.isInitialized) screenCapture.release() } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class DragListener(private val params: WindowManager.LayoutParams) : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var touchX = 0f
        private var touchY = 0f
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    touchX = event.rawX; touchY = event.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(v, params)
                    return true
                }
            }
            return false
        }
    }
}
