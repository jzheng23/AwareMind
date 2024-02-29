package com.jianzheng.studyzero.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
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
//        Log.d("unlock", "OverlayService OnCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent != null) {
            if (!isShowing) {
                unlockTime = intent.getLongExtra("unlock", System.currentTimeMillis())
                Log.d("delayTime", "received: $unlockTime")
                var tag = intent.getStringExtra("tag")
                if (tag == null) tag = "count_random_met"
                callOverlay(tag)
                isShowing = true
            }
//            Log.d("unlock", "OverlayService OnStartCommand with Intent: $intent")
            START_STICKY
        } else {
//            Log.d("unlock", "OverlayService OnStartCommand without Intent")
            START_REDELIVER_INTENT
        }
        // Your service logic here
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
//                    lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
//                    windowManager.removeView(composeView)
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
//        Log.d("unlock", "OverlayService OnDestroy")
    }

}




