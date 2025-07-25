package com.example.internshiptask

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class SplashScreen : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var goButton: MaterialButton

    private val imageList = listOf(
        R.drawable.communication_social_media_icons,
        R.drawable.communication_social_media_icons__1_,
        R.drawable.communication_social_media_icons__2_
    )

    private val handler = Handler(Looper.getMainLooper())
    private var currentIndex = 0
    private val delayMillis: Long = 2000 // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabDots)
        goButton = findViewById(R.id.GoBtn)

        viewPager.adapter = ImageAdapter(imageList)

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        goButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        autoSlideImages()
    }

    private fun autoSlideImages() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                currentIndex = (currentIndex + 1) % imageList.size
                viewPager.currentItem = currentIndex
                handler.postDelayed(this, delayMillis)
            }
        }, delayMillis)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Prevent memory leaks
    }
}
