/*** AutoClickerHelper.kt ***/
package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.example.myapplication.BackgroundAutoClickerService

class AutoClickerHelper {
    companion object {
        // Function to set up auto clicker with all required permissions
        fun setupAutoClicker(context: Context) {
            // Check and request accessibility permission
            if (!isAccessibilityServiceEnabled(context)) {
                requestAccessibilityPermission(context)
            }

            // Check and request overlay permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                requestOverlayPermission(context)
            }

            // Check and request battery optimization exemption
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestBatteryOptimizationExemption(context)
            }
        }

        // Check if all permissions are granted
        fun areAllPermissionsGranted(context: Context): Boolean {
            val accessibilityEnabled = isAccessibilityServiceEnabled(context)
            val overlayEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }

            val batteryOptEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true
            }

            return accessibilityEnabled && overlayEnabled && batteryOptEnabled
        }

        // Start the auto-clicker service
        fun startService(context: Context, x: Float, y: Float) {
            val serviceIntent = Intent(context, BackgroundAutoClickerService::class.java).apply {
                putExtra(BackgroundAutoClickerService.EXTRA_X_COORDINATE, x)
                putExtra(BackgroundAutoClickerService.EXTRA_Y_COORDINATE, y)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }

        // Check if accessibility service is enabled
        private fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val service = "${context.packageName}/${BackgroundAutoClickerService::class.java.canonicalName}"
            val accessibilityEnabled = try {
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED
                )
            } catch (e: Settings.SettingNotFoundException) {
                0
            }

            if (accessibilityEnabled != 1) return false

            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            return settingValue.split(":\\s*".toRegex()).any {
                it.equals(service, ignoreCase = true)
            }
        }

        // Request accessibility permission
        private fun requestAccessibilityPermission(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        // Request overlay permission
        @RequiresApi(Build.VERSION_CODES.M)
        private fun requestOverlayPermission(context: Context) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        // Request battery optimization exemption
        @RequiresApi(Build.VERSION_CODES.M)
        private fun requestBatteryOptimizationExemption(context: Context) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = context.packageName

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        }

        // Function to directly click at coordinates
        fun clickAt(x: Float, y: Float) {
            val service = BackgroundAutoClickerService.getInstance()
            if (service != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                service.performClick(x, y)
            }
        }
    }
}