package com.dicoding.asclepius.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityMainBinding
import java.io.FileInputStream
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var currentImageUri: Uri? = null
    private var selectedBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.galleryButton.setOnClickListener {
            startGallery()
        }

        binding.analyzeButton.setOnClickListener {
            analyzeImage()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            showImage()
        } else {
            showToast("Gambar tidak dipilih.")
        }
    }

    private fun startGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun showImage() {
        currentImageUri?.let {
            selectedBitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
            binding.previewImageView.setImageBitmap(selectedBitmap)
        }
    }

    private fun analyzeImage() {
        val bitmap = selectedBitmap
        if (bitmap == null) {
            showToast("Pilih gambar terlebih dahulu.")
            return
        }

        val model = loadModel()
        val input = preprocessImage(bitmap)
        val output = Array(1) { FloatArray(2) } // 2 karena model memiliki dua kelas output

        model.run(input, output)

        val confidence = output[0][1]
        val result = if (confidence > 0.5) "Cancer Detected" else "No Cancer"

        moveToResult(result, confidence)
    }

    private fun moveToResult(result: String, confidence: Float) {
        if (currentImageUri == null) {
            showToast("Pilih gambar terlebih dahulu.")
            return
        }

        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("RESULT", result)
            putExtra("CONFIDENCE", confidence)
            putExtra("IMAGE_URI", currentImageUri.toString())
        }

        // Debugging log untuk memastikan data dikirim
        Log.d("MainActivity", "Intent: RESULT=$result, CONFIDENCE=$confidence, IMAGE_URI=${currentImageUri.toString()}")

        startActivity(intent)
    }

    private fun preprocessImage(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val batchSize = 1
        val height = 224
        val width = 224
        val channel = 3

        val input = Array(batchSize) {
            Array(height) {
                Array(width) {
                    FloatArray(channel)
                }
            }
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = resizedBitmap.getPixel(x, y)
                input[0][y][x][0] = (pixel shr 16 and 0xFF) / 255.0f
                input[0][y][x][1] = (pixel shr 8 and 0xFF) / 255.0f
                input[0][y][x][2] = (pixel and 0xFF) / 255.0f
            }
        }

        return input
    }

    private fun loadModel(): Interpreter {
        // Membuka model .tflite dari folder assets
        val assetFileDescriptor = assets.openFd("cancer_classification.tflite")
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        // Mengembalikan instance Interpreter
        return Interpreter(buffer)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
