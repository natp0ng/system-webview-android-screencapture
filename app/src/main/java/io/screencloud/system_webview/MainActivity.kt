package io.screencloud.system_webview

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.squareup.moshi.Moshi
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    private lateinit var mWebView: WebView
    private val defaultURL = "http://example.com"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mWebView = findViewById(R.id.webView)
        setupWebView()
        mWebView.loadUrl(defaultURL)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val ws: WebSettings = mWebView.settings
        ws.userAgentString = ws.userAgentString + " Custom/1.0.0-dev"
        ws.domStorageEnabled = true
        ws.loadsImagesAutomatically = true
        // Zoom out if the content width is greater than the width of the viewport
        ws.loadWithOverviewMode = true
        // Enable responsive layout
        ws.useWideViewPort = true

        // Zoom out if the content width is greater than the width of the viewport
        // ws.setLoadWithOverviewMode(true)
        ws.javaScriptEnabled = true

        // disable the default zoom controls on the page
        ws.builtInZoomControls = false

        ws.domStorageEnabled = true;
        ws.mediaPlaybackRequiresUserGesture = false;

        // disable cache ?
        // ws.setAppCacheEnabled(false)
        // ws.setCacheMode(WebSettings.LOAD_NO_CACHE)
        mWebView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY

        // disable require user gesture before start to play video (auto player video issue)
        ws.mediaPlaybackRequiresUserGesture = false // <- this require min sdk level 17
        // this will open url in app not redirect to browser outside app
        mWebView.webViewClient = SystemWebViewClient()
        mWebView.webChromeClient = SystemWebViewWebChromeClient()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
        }

        mWebView.addJavascriptInterface(JSInterface(this), "JSInterface")
    }

    private class SystemWebViewClient internal constructor() : WebViewClient() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            view.loadUrl(request.url.toString())
            return true
        }
    }

    private class SystemWebViewWebChromeClient : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            //Log.d(TAG, "JS: "+consoleMessage.message());
            return false
        }
    }

    fun screenShot(): Bitmap {
        val view = window.decorView.rootView
        val bitmap = Bitmap.createBitmap(
            view.width,
            view.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) bgDrawable.draw(canvas) else canvas.drawColor(Color.WHITE)
        view.draw(canvas)
        return bitmap
    }

    class JSInterface(private var act: MainActivity) {
        @get:JavascriptInterface
        val deviceId: String
            get() = "fake-device-id"

        @JavascriptInterface
        fun screenShot(jsonParamsForUpload: String): String {
            println("screenShot('$jsonParamsForUpload')")
            val res = JSONObject()
            try {
                val moshi = Moshi.Builder().build()
                val jsonAdapter =
                    moshi.adapter(UploadUtil.UploadParams::class.java)
                //example JSON
                // {"url":"http://foo.bar", "headers":[["hello","world"],["foo","bar"]]}
                // JSInterface.screenShot('{"url":"http://192.168.100.116:3000/upload", "headers":[["hello","world"],["foo","bar"]]}')
                val params = jsonAdapter.fromJson(jsonParamsForUpload)!!
                val bm = act.screenShot()
                val file = File.createTempFile("screenshot", null, act.cacheDir)
                FileOutputStream(file).use { fos ->
                    bm.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        fos
                    )
                }
                UploadUtil.uploadFile(params, file, "image/png")

                res.put("status", "ok: post queued")
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    res.put("status", "error: " + e.message)
                } catch (e1: JSONException) {
                    e1.printStackTrace()
                }
            }
            return res.toString()
        }

        @JavascriptInterface
        fun hello() {
            println("hello :)")
        }
    }

}