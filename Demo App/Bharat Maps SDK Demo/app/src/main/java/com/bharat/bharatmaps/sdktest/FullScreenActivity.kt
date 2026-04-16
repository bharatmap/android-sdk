package com.bharat.bharatmaps.sdktest

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity

open class FullScreenActivity() : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {

        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            // Hide the navigation bar
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN
        }

        super.onCreate(savedInstanceState)

    }
}