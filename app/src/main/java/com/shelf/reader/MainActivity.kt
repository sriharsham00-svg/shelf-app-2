package com.shelf.reader

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var root: FrameLayout
    private var pending: ValueCallback<Array<Uri>>? = null

    private val picker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        pending?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(r.resultCode, r.data))
        pending = null
    }

    private fun applyColors(c: Int, light: Boolean) {
        root.setBackgroundColor(c)
        web.setBackgroundColor(c)
        window.setBackgroundDrawable(ColorDrawable(c))
        WindowInsetsControllerCompat(window, root).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
    }

    inner class Bridge {
        @JavascriptInterface
        fun setBars(color: String, light: Boolean) {
            runOnUiThread { try { applyColors(Color.parseColor(color), light) } catch (e: Exception) {} }
        }

        @JavascriptInterface
        fun keepAwake(on: Boolean) {
            runOnUiThread {
                if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        @JavascriptInterface
        fun setFullscreen(on: Boolean) {
            runOnUiThread {
                val c = WindowInsetsControllerCompat(window, root)
                if (on) {
                    c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    c.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    c.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        web = WebView(this)
        root = FrameLayout(this)
        root.addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)

        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        applyColors(if (night) 0xFF111111.toInt() else 0xFFFAFAF8.toInt(), !night)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(b.left, b.top, b.right, maxOf(b.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = false
        web.addJavascriptInterface(Bridge(), "Android")

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(v: WebView, cb: ValueCallback<Array<Uri>>, p: FileChooserParams): Boolean {
                pending?.onReceiveValue(null)
                pending = cb
                return try {
                    val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    picker.launch(i); true
                } catch (e: Exception) { pending = null; false }
            }
        }
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("(function(){return window.__back?window.__back():false})()") { r ->
                    if (r != "true") finish()
                }
            }
        })
    }
}
