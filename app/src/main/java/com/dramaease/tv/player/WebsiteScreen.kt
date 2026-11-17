package com.dramaease.tv.player

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.Text
import com.dramaease.tv.provider.KissKhLibrary
import com.dramaease.tv.ui.components.BigButton
import com.dramaease.tv.ui.theme.DramaType
import com.dramaease.tv.ui.theme.LocalDramaColors
import com.dramaease.tv.ui.theme.LocalStrings

/**
 * Shows a kisskh.do page inside the app, using the site's OWN player. DramaEase does not read, rewrite or
 * extract anything from the page, and does not touch the site's bot check — it is an ordinary WebView.
 * Navigation is limited to kisskh.do; pop-ups and other sites are blocked.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebsiteScreen(url: String, onBack: () -> Unit, onNext: (() -> Unit)?) {
    val s = LocalStrings.current
    val c = LocalDramaColors.current
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    fun exitFullscreen() { customCallback?.onCustomViewHidden(); customCallback = null; customView = null }

    // Back leaves fullscreen first, otherwise returns to the episode list.
    BackHandler { if (customView != null) exitFullscreen() else onBack() }

    DisposableEffect(Unit) { onDispose { webView?.apply { stopLoading(); destroy() } } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize()) {
            if (customView == null) {
                Row(
                    Modifier.fillMaxWidth().background(c.background).padding(horizontal = 48.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    BigButton(s.backToEpisodes, onClick = onBack)
                    if (onNext != null) BigButton(s.nextEpisode, onClick = onNext)
                    Text(s.websiteHint, style = DramaType.label, color = c.textDim, modifier = Modifier.weight(1f))
                }
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.setSupportMultipleWindows(false)
                        isFocusable = true
                        isFocusableInTouchMode = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean =
                                !KissKhLibrary.isAllowedPageUrl(r.url.toString()) // block other sites
                            override fun onPageStarted(v: WebView, u: String?, f: Bitmap?) { loading = true }
                            override fun onPageFinished(v: WebView, u: String?) { loading = false }
                            override fun onReceivedError(v: WebView, r: WebResourceRequest, e: WebResourceError) {
                                if (r.isForMainFrame) { failed = true; loading = false }
                            }
                            override fun onReceivedSslError(v: WebView, h: SslErrorHandler, e: SslError) { h.cancel(); failed = true }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(view: View, cb: CustomViewCallback) { customView = view; customCallback = cb }
                            override fun onHideCustomView() { customView = null; customCallback = null }
                        }
                        loadUrl(url)
                        webView = this
                        requestFocus()
                    }
                },
            )
        }
        customView?.let { v ->
            AndroidView(factory = { v }, modifier = Modifier.fillMaxSize().background(Color.Black))
        }
        if (loading && !failed) {
            Text(s.loading, style = DramaType.title, color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        if (failed) {
            Column(
                Modifier.fillMaxSize().background(Color.Black).padding(64.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(s.websiteFailed, style = DramaType.title, color = Color.White)
                BigButton(s.tryAgain, primary = true, onClick = { failed = false; loading = true; webView?.reload() })
                BigButton(s.goBack, onClick = onBack)
            }
        }
    }
}
