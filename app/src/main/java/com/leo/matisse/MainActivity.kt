package com.leo.matisse

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.matisse.ui.view.MatisseActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textview.setOnClickListener {
            startActivity(Intent(this, MatisseActivity::class.java))
        }
    }
}
