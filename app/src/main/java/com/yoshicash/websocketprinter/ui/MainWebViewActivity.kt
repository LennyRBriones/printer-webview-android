package com.yoshicash.websocketprinter.ui

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.yoshicash.websocketprinter.databinding.ActivityMainWebViewBinding
import com.yoshicash.websocketprinter.interfaces.AndroidBridge
import com.yoshicash.websocketprinter.manager.PrintManager
import com.yoshicash.websocketprinter.utils.EnvironmentConfig.getDashboardURl
import com.yoshicash.websocketprinter.utils.requestBluetoothPermissions
import com.yoshicash.websocketprinter.utils.requestLocalizationPermissions

class MainWebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainWebViewBinding
    private lateinit var printManager: PrintManager
    private var wasOffline = false
    private lateinit var cm: ConnectivityManager

    private val networkCallback  = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (wasOffline) {
                wasOffline = false
                runOnUiThread {
                    binding.wbMain.reload()
                }
            }
        }

        override fun onLost(network: Network) {
            wasOffline = true
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            val online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            wasOffline = !online
        }
    }

    // Guarda la URL inicial para usarla si no hay estado que restaurar
    private val initialUrl by lazy { getDashboardURl() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()

        // Restaurar estado del WebView si existe (evita recargar/“perder sesión” al rotar)
        if (savedInstanceState != null) {
            binding.wbMain.restoreState(savedInstanceState)
        } else {
            binding.wbMain.loadUrl(initialUrl)
        }
        binding.mainSwipeLayout.isEnabled = false
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cm.registerDefaultNetworkCallback(networkCallback)
        } else {
            val req = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(req, networkCallback)
        }
    }

    override fun onStop() {
        super.onStop()
        cm.unregisterNetworkCallback(networkCallback)
    }

    private fun init() {
        requestLocalizationPermissions(this)
        requestBluetoothPermissions(this)
        setKioskoMode()
        printManager = PrintManager()
        cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        configWebView(binding.wbMain)
        setupSwipeToRefresh()
        handleBackPressed()
    }

    @SuppressLint("BatteryLife")
    private fun setKioskoMode() {
        val devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            startLockTask()
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val wifi = applicationContext.getSystemService(WifiManager::class.java)
        val wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "app:wifi")
        wifiLock.acquire()

        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:$packageName"))
            )
        }
    }

    private fun setupSwipeToRefresh() {
        // Recargar al “pull-to-refresh”
        binding.mainSwipeLayout.setOnRefreshListener {
            binding.wbMain.reload()
        }

        // Habilitar el gesto solo cuando estás en el tope (para no pelear con el scroll)
        binding.wbMain.setOnScrollChangeListener { v, _, scrollY, _, _ ->
            binding.mainSwipeLayout.isEnabled = (v as WebView).scrollY == 0
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configWebView(webView: WebView) {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            // Mejora compatibilidad con sitios modernos:
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            // Evita re-crear el proceso por falta de memoria de caché al rotar:
            databaseEnabled = true
            // Mantén el zoom/despliegue igual tras rotación:
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            displayZoomControls = false
        }

        // Cookies persistentes (evita “perder sesión” si el sitio usa cookies)
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                // Abre todo en el mismo WebView (incluye target=_blank)
                view.loadUrl(request.url.toString())
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                // Termina animación del refresh cuando acabe la carga
                binding.mainSwipeLayout.isRefreshing = false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            // Si quieres mostrar un ProgressBar en la toolbar, puedes hacerlo aquí:
            // override fun onProgressChanged(view: WebView?, newProgress: Int) { ... }
        }

        webView.resumeTimers()
        webView.onResume()

        webView.addJavascriptInterface(
            AndroidBridge(this, Gson()),
            "AndroidBridge"
        )
    }

    private fun handleBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.wbMain.canGoBack()) {
                    binding.wbMain.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Guarda el estado completo del WebView (historial, scroll, formularios, etc.)
        binding.wbMain.saveState(outState)
    }

    override fun onPause() {
        super.onPause()
        binding.wbMain.onPause()
        binding.wbMain.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        binding.wbMain.onResume()
        binding.wbMain.resumeTimers()
    }
}
