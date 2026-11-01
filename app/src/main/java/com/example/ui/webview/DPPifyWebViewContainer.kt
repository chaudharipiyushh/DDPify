package com.example.ui.webview

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class DPPifyJavaScriptInterface(
    private val onRetry: () -> Unit
) {
    @JavascriptInterface
    fun retryConnection() {
        onRetry()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DPPifyWebViewContainer(
    webView: WebView,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize(),
        update = { /* Updates handled directly via WebView controller */ }
    )
}

@SuppressLint("SetJavaScriptEnabled")
fun setupDPPifyWebView(
    webView: WebView,
    context: Context,
    webViewClient: DPPifyWebViewClient,
    webChromeClient: DPPifyWebChromeClient,
    onRetryRequested: () -> Unit
) {
    with(webView.settings) {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false

        // Cache mode for seamless online/offline transition
        cacheMode = WebSettings.LOAD_DEFAULT

        allowFileAccess = true
        allowContentAccess = true
        mediaPlaybackRequiresUserGesture = false

        // Enhanced User-Agent to let web know it's DPPify Android
        val defaultUserAgent = userAgentString
        userAgentString = "$defaultUserAgent DPPify-Android/1.0"
    }

    // Enable cookies
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(webView, true)

    // JS interface for offline page communication
    webView.addJavascriptInterface(
        DPPifyJavaScriptInterface(onRetry = onRetryRequested),
        "AndroidBridge"
    )

    webView.webViewClient = webViewClient
    webView.webChromeClient = webChromeClient

    // Handle worksheet/DPP file downloads
    webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
                setTitle(filename)
                setDescription("Downloading DPPify educational material...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            }
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            downloadManager?.enqueue(request)
            Toast.makeText(context, "Downloading study material...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not start download: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
