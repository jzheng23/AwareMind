package com.jianzheng.studyzero.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.cycle.MyLifecycleOwner
import com.jianzheng.studyzero.ui.ShowOverlay

class OverlayService : Service() {

    private val windowManager get() = getSystemService(WINDOW_SERVICE) as WindowManager
    private var unlockTime: Long = 0
    private var isShowing: Boolean = false
    private lateinit var composeView: ComposeView
    private lateinit var lifecycleOwner: MyLifecycleOwner


    override fun onCreate() {
        super.onCreate()
        setTheme(R.style.Theme_StudyZero)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent != null) {
            if (!isShowing) {

                // Add safety cleanup
                try {
                    if (::composeView.isInitialized && composeView.isAttachedToWindow) {
                        windowManager.removeView(composeView)
                    }
                } catch (_: Exception) {
                    // View wasn't attached, that's fine
                }

                unlockTime = intent.getLongExtra("unlock", System.currentTimeMillis())
                val tag = intent.getStringExtra("tag") ?: "count_random_met"
                callOverlay(tag)
                isShowing = true
            }
            START_STICKY
        } else {
            START_REDELIVER_INTENT
        }
    }


    private fun callOverlay(tag: String = "count_random_met") {

        val layoutFlag: Int =
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        composeView = ComposeView(this)
        lifecycleOwner = MyLifecycleOwner()

        composeView.setContent {
            ShowOverlay(
                tag = tag,
                unlockTime = unlockTime,
                onClick = {
                    stopSelf()
                }
            )
        }

        val viewModelStoreOwner = object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore
                get() = ViewModelStore()
        }
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        composeView.setViewTreeLifecycleOwner(lifecycleOwner)
        composeView.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

        // This is required or otherwise the UI will not recompose
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        if (composeView.isAttachedToWindow) windowManager.removeView(composeView)
        windowManager.addView(composeView, params)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (composeView.isAttachedToWindow) windowManager.removeView(composeView)
        isShowing = false
    }

}




