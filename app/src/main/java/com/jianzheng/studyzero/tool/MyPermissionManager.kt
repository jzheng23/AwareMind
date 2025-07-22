package com.jianzheng.studyzero.tool

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.APP_OPS_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jianzheng.studyzero.service.MyAccessibilityService


object MyPermissionManager {

    fun grantNotificationPermission(context: Context) {
        val intent = Intent()
        intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        intent.putExtra(Settings.EXTRA_CHANNEL_ID, context.applicationInfo.uid)
        context.startActivity(intent)
    }


    fun grantOverlayPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }


    fun checkUserStatPermission(context: Context): Boolean {
        val appOpsManager = context.getSystemService(APP_OPS_SERVICE) as AppOpsManager

        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // For API 34+ use the new method
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.packageName
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For API 29-33 use the deprecated method (still needed for compatibility)
            @Suppress("DEPRECATION")
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.packageName
            )
        } else {
            // For API < 29 use the original method
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun checkNotificationPermission(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun checkOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun grantUserStatPermission(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        context.startActivity(intent)
    }

    fun grantAccessibilityPermission(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        context.startActivity(intent)
    }

    fun checkAccessibilityPermission(context: Context): Boolean {
        val serviceName = ComponentName(context, MyAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(serviceName.flattenToString()) ?: false
    }

    fun checkActivityPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        } else {
            true
        }
    }

    fun grandActivityPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                context as Activity, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                48
            )

            MyPreferenceManager.setBoolean(context, "activity_enabled", true)
        }
    }

    fun checkLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun grandLocationPermission(context: Context) {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            51
        )
        MyPreferenceManager.setBoolean(context, "location_enabled", true)
    }


    fun checkAllPermissions(context: Context): Boolean =
        (checkNotificationPermission(context)
                and checkOverlayPermission(context)
                and checkUserStatPermission(context)
                and checkActivityPermission(context)
//                and checkBatteryPermission(context)
                and checkAccessibilityPermission(context))


}



