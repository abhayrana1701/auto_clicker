/*** MainActivity.kt ***/
package com.example.myapplication

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var xCoordinateInput: EditText
    private lateinit var yCoordinateInput: EditText
    private lateinit var startButton: Button
    private lateinit var setupButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find UI elements
        xCoordinateInput = findViewById(R.id.x_coordinate)
        yCoordinateInput = findViewById(R.id.y_coordinate)
        startButton = findViewById(R.id.start_button)
        setupButton = findViewById(R.id.setup_button)

        // Load saved coordinates if available
        loadSavedCoordinates()

        // Setup permissions button
        setupButton.setOnClickListener {
            AutoClickerHelper.setupAutoClicker(this)
        }

        // Start auto-clicker service button
        startButton.setOnClickListener {
            val x = xCoordinateInput.text.toString().toFloatOrNull()
            val y = yCoordinateInput.text.toString().toFloatOrNull()

            if (x != null && y != null) {
                // Save coordinates
                saveCoordinates(x, y)

                // Check if all permissions are granted
                if (AutoClickerHelper.areAllPermissionsGranted(this)) {
                    // Start service
                    startAutoClickerService()
                    Toast.makeText(this, "Auto-clicker service started! You can close the app now.",
                        Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Please grant all permissions first",
                        Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter valid coordinates",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startAutoClickerService() {
        val x = xCoordinateInput.text.toString().toFloatOrNull() ?: 500f
        val y = yCoordinateInput.text.toString().toFloatOrNull() ?: 800f

        AutoClickerHelper.startService(this, x, y)
    }

    private fun saveCoordinates(x: Float, y: Float) {
        val sharedPreferences = getSharedPreferences("AutoClickerPrefs", MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putFloat("x_coordinate", x)
            putFloat("y_coordinate", y)
            apply()
        }
    }

    private fun loadSavedCoordinates() {
        val sharedPreferences = getSharedPreferences("AutoClickerPrefs", MODE_PRIVATE)
        val x = sharedPreferences.getFloat("x_coordinate", 500f)
        val y = sharedPreferences.getFloat("y_coordinate", 800f)

        xCoordinateInput.setText(x.toString())
        yCoordinateInput.setText(y.toString())
    }
}

