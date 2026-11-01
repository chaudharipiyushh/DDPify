package com.example.ui.webview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

class DPPifyWebViewClient(
    private val context: Context,
    private val onPageLoadStarted: (String?) -> Unit,
    private val onPageLoadFinished: (String?) -> Unit,
    private val onPageLoadError: (String?, Int) -> Unit
) : WebViewClient() {

    companion object {
        const val PRIMARY_HOST = "progress-byte.vercel.app"
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        val host = uri.host?.lowercase() ?: ""

        // Handle custom URL schemes (telephone, email, WhatsApp, app stores, etc.)
        if (scheme != "http" && scheme != "https" && scheme != "file") {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return true
            } catch (_: Exception) {
                return false
            }
        }

        // Keep local asset URLs and primary website domain inside the WebView
        if (scheme == "file" || host == PRIMARY_HOST || host.endsWith(".$PRIMARY_HOST") || host.contains("vercel.app")) {
            return false
        }

        // External links (e.g., YouTube videos, external references, social links) open in browser
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return true
        } catch (_: Exception) {
            return false
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageLoadStarted(url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageLoadFinished(url)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        // Only trigger error screen for the main frame request
        if (request?.isForMainFrame == true) {
            val errorCode = error?.errorCode ?: -1
            val description = error?.description?.toString() ?: "Connection error"
            onPageLoadError(description, errorCode)
        }
    }

    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: SslError?
    ) {
        // In production, reject invalid SSL to ensure secure HTTPS communication
        handler?.cancel()
    }
}
