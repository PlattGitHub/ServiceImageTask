package com.example.serviceimagetask

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Launcher Activity that is container for [DownloadFragment].
 *
 * @author Alexander Gorin
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, DownloadFragment.newInstance()).commit()
        }
    }
}
