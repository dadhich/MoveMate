package com.abhishekdadhich.movemate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // We've removed enableEdgeToEdge() and the ViewCompat.setOnApplyWindowInsetsListener block
        setContentView(R.layout.activity_main)
    }
}