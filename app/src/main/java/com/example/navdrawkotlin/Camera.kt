package com.example.navdrawkotlin

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class Camera : AppCompatActivity() {

    //variables
    var tvTitle: TextView? = null
    var imgViewCamera: ImageView? = null
    var btnTakePic: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        //typecasting for design elements
        tvTitle = findViewById(R.id.textView)
        imgViewCamera = findViewById(R.id.imageView)
        btnTakePic = findViewById(R.id.button2)
    }

    fun CaptureOnClick(view: View?) {

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        startActivityForResult(intent, 0)

    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        try {

            super.onActivityResult(requestCode, resultCode, data)

            val bm = data!!.extras!!["data"] as Bitmap?

            imgViewCamera!!.setImageBitmap(bm)

        } catch (ex: Exception) {

            Toast.makeText(this, "Pic not saved", Toast.LENGTH_SHORT).show()

        }

    }


}