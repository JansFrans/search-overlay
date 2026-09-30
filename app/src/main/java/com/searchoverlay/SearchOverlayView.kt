package com.searchoverlay

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.TextView

class SearchOverlayView(
    private val context: Context,
    private val windowManager: WindowManager,
    inflater: LayoutInflater,
    private val query: String,
    private val onClose: () -> Unit
) {
    private val rootView: View = inflater.inflate(R.layout.overlay_search, null)
    private val params: WindowManager.LayoutParams

    init {
        val metrics = context.resources.displayMetrics
        val w = (metrics.widthPixels * 0.9).toInt()
        val h = (metrics.heightPixels * 0.7).toInt()
        params = WindowManager.LayoutParams(
            w, h,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER
    }

    fun show() {
        val tvQuery = rootView.findViewById<TextView>(R.id.tvQuery)
        val btnClose = rootView.findViewById<ImageButton>(R.id.btnClose)
        val btnGoogle = rootView.findViewById<ImageButton>(R.id.btnGoogle)
        val webView = rootView.findViewById<WebView>(R.id.webView)

        tvQuery.text = query
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.setBackgroundColor(Color.WHITE)
        webView.loadUrl("https://www.google.com/search?q=${Uri.encode(query)}")

        btnClose.setOnClickListener { onClose() }
        btnGoogle.setOnClickListener {
            context.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        windowManager.addView(rootView, params)
    }
}
