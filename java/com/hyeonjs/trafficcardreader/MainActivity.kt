package com.hyeonjs.trafficcardreader

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this)
        layout.orientation = 1

        val txt = TextView(this)
        txt.text = "1. Enable NFC on your Smartphone\n" +
                "2. Tag IC Card to Smartphone"
        txt.layoutParams = LinearLayout.LayoutParams(-1, -1)
        txt.gravity = Gravity.CENTER or Gravity.CENTER_VERTICAL
        layout.addView(txt)

        setContentView(layout)
    }
}