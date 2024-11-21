package com.dicoding.asclepius.view

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dicoding.asclepius.R

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val result = intent.getStringExtra("RESULT")
        val confidence = intent.getFloatExtra("CONFIDENCE", 0.0f)
        val imageUri = intent.getStringExtra("IMAGE_URI")

        val imageView = findViewById<ImageView>(R.id.result_image)
        val resultTextView = findViewById<TextView>(R.id.result_text)

        // Menangani jika imageUri null
        if (imageUri != null) {
            imageView.setImageURI(Uri.parse(imageUri))
        } else {
            imageView.setImageResource(R.drawable.ic_place_holder)
            showToast("Gambar tidak tersedia.")
        }

        resultTextView.text = "$result\nConfidence: ${(confidence * 100).toInt()}%"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
