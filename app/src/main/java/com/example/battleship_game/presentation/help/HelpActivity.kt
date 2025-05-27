package com.example.battleship_game.presentation.help

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.core.view.isVisible
import com.example.battleship_game.common.BaseActivity
import com.example.battleship_game.R
import com.example.battleship_game.databinding.ActivityHelpBinding
import com.google.android.material.snackbar.Snackbar

class HelpActivity : BaseActivity() {

    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-edge отступы
        applyEdgeInsets(binding.main)

        // Скрываем SystemBars
        enterImmersiveMode()

        initWebView()
        setupListeners()

        onBackPressedDispatcher.addCallback(this) {
            handleBack()
        }
    }

    private fun handleBack() {
        if (binding.webViewContainer.isVisible) {
            setHelpMode(false)
        } else {
            finish()
        }
    }

    /** Инициализация WebView: выполняется **один раз** в onCreate */
    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        binding.webViewHelp.apply {
            settings.apply {
                javaScriptEnabled     = false   // без JS для безопасности
                domStorageEnabled     = true    // включаем DOM-хранилище
                loadWithOverviewMode  = true    // масштабируем страницу под экран
                useWideViewPort       = true
            }
            webViewClient = object : WebViewClient() {
                // При старте загрузки — показываем спиннер
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    binding.progressBar.visibility = View.VISIBLE
                }
                // При финише — скрываем спиннер
                override fun onPageFinished(view: WebView?, url: String?) {
                    binding.progressBar.visibility = View.GONE
                }
                // Обработка ошибок (например, если файл не найден)
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.main, R.string.txt_error_loading, Snackbar.LENGTH_LONG)
                        .setAction(R.string.txt_retry) { view?.reload() }
                        .show()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.apply {
            btnSystemInfo.setOnClickListener {
                setHelpMode(true)
                Snackbar.make(main, R.string.txt_open_help, Snackbar.LENGTH_SHORT).show()
            }

            btnBack.setOnClickListener {
                handleBack()
            }

            btnRefresh.setOnClickListener {
                webViewHelp.reload()
            }
        }
    }

    /** Переключает режим экрана:
     *  showWeb = true  → WebView виден, нативный текст скрыт
     *  showWeb = false → обратно
     */
    private fun setHelpMode(showWeb: Boolean) {
        binding.apply {
            scrollHelp.isVisible = !showWeb
            tvHelpTitle.isVisible = !showWeb
            btnSystemInfo.isVisible = !showWeb

            webViewContainer.isVisible = showWeb
            if (showWeb) {
                webViewHelp.loadUrl("file:///android_asset/help/help.html")
            }
        }
    }

    /** Уничтожаем WebView, чтобы не было утечек */
    override fun onDestroy() {
        binding.webViewHelp.apply {
            clearHistory()
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }

    /** При сворачивании/разворачивании можно приостановить WebView */
    override fun onPause() {
        binding.webViewHelp.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.webViewHelp.onResume()
    }
}