package com.example.melon

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.Button

class splash_screen : AppCompatActivity() {

    private lateinit var mulaiButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        mulaiButton = findViewById(R.id.button12)
        mulaiButton.setOnClickListener{
            val intent = Intent(this, tutorial::class.java)
            startActivity(intent)
        }
    }
}