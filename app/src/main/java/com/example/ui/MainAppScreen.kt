package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.network.NetworkMonitor
import com.example.ui.offline.OfflineScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.VibrantYellow
import com.example.ui.webview.DPPifyWebChromeClient
import com.example.ui.webview.DPPifyWebViewClient
import com.example.ui.webview.DPPifyWebViewContainer
import com.example.ui.webview.setupDPPifyWebView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PRIMARY_URL = "https://progress-byte.vercel.app/"
private const val OFFLINE_URL = "file:///android_asset/offline.html"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    networkMonitor: NetworkMonitor,
    onShowFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSplash by remember { mutableStateOf(true) }
    val isOnline by networkMonitor.isOnlineFlow.collectAsState(initial = networkMonitor.isCurrentlyConnected())

    // Web states
    var webProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var hasLoadError by remember { mutableStateOf(false) }
    var currentTitle by remember { mutableStateOf("DPPify") }
    var currentUrl by remember { mutableStateOf(PRIMARY_URL) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isRetrying by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    // Persistent WebView instance
    val webView = remember {
        WebView(context).apply {
            id = android.view.View.generateViewId()
        }
    }

    // Configure WebView once
    DisposableEffect(Unit) {
        val client = DPPifyWebViewClient(
            context = context,
            onPageLoadStarted = { url ->
                isLoading = true
                hasLoadError = false
                currentUrl = url ?: PRIMARY_URL
            },
            onPageLoadFinished = { url ->
                isLoading = false
                canGoBack = webView.canGoBack()
                canGoForward = webView.canGoForward()
                currentUrl = url ?: PRIMARY_URL
            },
            onPageLoadError = { _, _ ->
                isLoading = false
                // Only show offline error if it's the main website failing
                if (webView.url == null || webView.url?.startsWith(PRIMARY_URL) == true) {
                    hasLoadError = true
                }
            }
        )

        val chromeClient = DPPifyWebChromeClient(
            onProgressUpdate = { progress ->
                webProgress = progress
                isLoading = progress in 1..99
                canGoBack = webView.canGoBack()
                canGoForward = webView.canGoForward()
            },
            onTitleReceived = { title ->
                if (!title.isNullOrBlank() && !title.startsWith("http")) {
                    currentTitle = title
                }
            },
            onFileChooser = onShowFileChooser
        )

        setupDPPifyWebView(
            webView = webView,
            context = context,
            webViewClient = client,
            webChromeClient = chromeClient,
            onRetryRequested = {
                scope.launch {
                    hasLoadError = false
                    webView.loadUrl(PRIMARY_URL)
                }
            }
        )

        webView.loadUrl(PRIMARY_URL)

        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    // Auto-reload when network reconnects after an error
    LaunchedEffect(isOnline) {
        if (isOnline && hasLoadError) {
            hasLoadError = false
            webView.loadUrl(PRIMARY_URL)
        }
    }

    // Hardware / Gesture Back Button handling
    BackHandler(enabled = !showSplash) {
        if (hasLoadError) {
            // If on offline screen, retry or exit
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressTime = now
                scope.launch {
                    snackbarHostState.showSnackbar("Press back again to exit DPPify")
                }
            }
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressTime = now
                scope.launch {
                    snackbarHostState.showSnackbar("Press back again to exit DPPify")
                }
            }
        }
    }

    if (showSplash) {
        SplashScreen(
            onSplashFinished = {
                showSplash = false
            }
        )
    } else {
        Scaffold(
            topBar = {
                Surface(
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Logo & App Name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable {
                                        webView.loadUrl(PRIMARY_URL)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_dppify_logo),
                                    contentDescription = "DPPify Logo",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = stringResource(id = R.string.app_name),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = RoyalBlue,
                                        letterSpacing = (-0.3).sp
                                    )
                                    Text(
                                        text = stringResource(id = R.string.app_tagline),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF855A00)
                                    )
                                }
                            }

                            // Action Buttons
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Back Button
                                IconButton(
                                    onClick = { if (webView.canGoBack()) webView.goBack() },
                                    enabled = canGoBack,
                                    modifier = Modifier.testTag("nav_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Go Back",
                                        tint = if (canGoBack) RoyalBlue else TextMuted.copy(alpha = 0.4f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Forward Button
                                IconButton(
                                    onClick = { if (webView.canGoForward()) webView.goForward() },
                                    enabled = canGoForward,
                                    modifier = Modifier.testTag("nav_forward_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Go Forward",
                                        tint = if (canGoForward) RoyalBlue else TextMuted.copy(alpha = 0.4f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Reload Button
                                IconButton(
                                    onClick = {
                                        hasLoadError = false
                                        webView.reload()
                                    },
                                    modifier = Modifier.testTag("reload_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reload Page",
                                        tint = RoyalBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Share Button
                                IconButton(
                                    onClick = {
                                        shareUrl(context, webView.url ?: PRIMARY_URL, currentTitle)
                                    },
                                    modifier = Modifier.testTag("share_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = TextDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Loading Progress Indicator (Smooth Royal Blue with Yellow Accent)
                        if (isLoading && webProgress in 1..99) {
                            LinearProgressIndicator(
                                progress = { webProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = VibrantYellow,
                                trackColor = RoyalBlue.copy(alpha = 0.15f)
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // WebView Container
                DPPifyWebViewContainer(
                    webView = webView,
                    modifier = Modifier.fillMaxSize()
                )

                // Offline Screen Overlay if error occurred
                if (hasLoadError || (!isOnline && webView.url == null)) {
                    OfflineScreen(
                        isRetrying = isRetrying,
                        onRetry = {
                            isRetrying = true
                            scope.launch {
                                delay(600)
                                if (networkMonitor.isCurrentlyConnected()) {
                                    hasLoadError = false
                                    webView.loadUrl(PRIMARY_URL)
                                } else {
                                    snackbarHostState.showSnackbar("Still offline. Please check your internet connection.")
                                }
                                isRetrying = false
                            }
                        },
                        onOpenOfflineHub = {
                            hasLoadError = false
                            webView.loadUrl(OFFLINE_URL)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private fun shareUrl(context: Context, url: String, title: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Check out $title on DPPify: $url")
            putExtra(Intent.EXTRA_TITLE, title)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share DPPify Link")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(shareIntent)
    } catch (_: Exception) {
    }
}
