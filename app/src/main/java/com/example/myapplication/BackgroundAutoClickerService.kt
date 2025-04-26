/*** BackgroundAutoClickerService.kt ***/
package com.example.myapplication

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.myapplication.MainActivity

class BackgroundAutoClickerService : AccessibilityService() {

    companion object {
        private var instance: BackgroundAutoClickerService? = null
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "AutoClickerChannel"

        // Intent extra keys
        const val EXTRA_X_COORDINATE = "x_coordinate"
        const val EXTRA_Y_COORDINATE = "y_coordinate"

        fun getInstance(): BackgroundAutoClickerService? {
            return instance
        }
    }

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var isServiceRunning = false
    private var xCoordinate: Float = 500f
    private var yCoordinate: Float = 800f

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isServiceRunning = true

        // Start as foreground service with notification
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // Create overlay button to trigger clicks
        createOverlayButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.hasExtra(EXTRA_X_COORDINATE) && it.hasExtra(EXTRA_Y_COORDINATE)) {
                xCoordinate = it.getFloatExtra(EXTRA_X_COORDINATE, 500f)
                yCoordinate = it.getFloatExtra(EXTRA_Y_COORDINATE, 800f)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Auto Clicker Service"
            val descriptionText = "Allows clicking on screen over other apps"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Auto Clicker Active")
            .setContentText("Service is running in background")
           // .setSmallIcon(android.R.drawable.ic_menu_click)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun createOverlayButton() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Create a floating button
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = FrameLayout(this).apply {
            val button = Button(context).apply {
                text = "Click"
                setOnClickListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        performClick(xCoordinate, yCoordinate)
                    }
                }
            }
            addView(button)
        }

        // Set layout parameters for the overlay
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        // Add the view to the window
        windowManager?.addView(overlayView, params)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun performClick(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                // Click completed
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                super.onCancelled(gestureDescription)
                // Click was cancelled
            }
        }, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not needed for basic clicking functionality
    }

    override fun onInterrupt() {
        // Not needed for basic clicking functionality
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        instance = null

        // Remove overlay view
        if (overlayView != null && windowManager != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
        }
    }
}