package com.example.drawingview

import android.app.Dialog
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var ivBackground: ImageView

    // Local Storage Image Picker Launcher
    private val openGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            ivBackground.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        ivBackground = findViewById(R.id.iv_background)

        // Undo & Redo Controls
        findViewById<ImageButton>(R.id.btn_undo).setOnClickListener { drawingView.onClickUndo() }
        findViewById<ImageButton>(R.id.btn_redo).setOnClickListener { drawingView.onClickRedo() }

        // Brush Size Selector
        findViewById<ImageButton>(R.id.btn_brush).setOnClickListener { showBrushSizeDialog() }

        // Import Background Image
        findViewById<ImageButton>(R.id.btn_gallery).setOnClickListener {
            openGalleryLauncher.launch("image/*")
        }

        // Save Drawing Button
        findViewById<ImageButton>(R.id.btn_save).setOnClickListener {
            Toast.makeText(this, "Canvas saved successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    fun colorClicked(view: View) {
        val colorTag = view.tag.toString()
        drawingView.setColor(Color.parseColor(colorTag))
    }

    private fun showBrushSizeDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size:")

        // Setup options inside custom dialog view (Small, Medium, Large)
        brushDialog.show()
    }
}