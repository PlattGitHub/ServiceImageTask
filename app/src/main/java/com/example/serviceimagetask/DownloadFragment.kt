package com.example.serviceimagetask


import android.content.*
import android.graphics.Bitmap
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

/**
 * Simple [Fragment] subclass that has [Button] to download an image
 * and then displays it in [ImageView].
 *
 * @author Alexander Gorin
 */
class DownloadFragment : Fragment() {

    private lateinit var button: Button
    private lateinit var imageView: ImageView

    private lateinit var mActivity: AppCompatActivity

    private lateinit var downloadService: DownloadService

    private var isBound = false
    private var isDownloading = false

    var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_download, container, false).apply {
            button = findViewById(R.id.button)
            imageView = findViewById(R.id.image_view)
        }

        if (isDownloading) {
            context?.startService(Intent(context, DownloadService::class.java))
        }

        bitmap?.let {
            imageView.setImageBitmap(it)
        }

        button.setOnClickListener {
            setupService()
            downloadService.downloadImage(imageLink)
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mActivity = context as AppCompatActivity
    }

    private fun setupService() {
        isDownloading = true
        context?.startService(Intent(context, DownloadService::class.java))
    }

    override fun onStart() {
        super.onStart()
        bindService()
    }

    override fun onResume() {
        super.onResume()
        mActivity.registerReceiver(resultReceiver, IntentFilter(DownloadService.ACTION_DOWNLOAD))
    }

    override fun onPause() {
        super.onPause()
        mActivity.unregisterReceiver(resultReceiver)
    }

    override fun onStop() {
        super.onStop()
        unbindService()
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            isBound = true
            downloadService = (iBinder as DownloadService.LocalBinder).instance
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            isBound = false
        }
    }

    /**
     * [BroadcastReceiver] that triggers when action [DownloadService.ACTION_DOWNLOAD] occurs.
     * Registered/Unregistered in onResume/onPause.
     *
     * @author Alexander Gorin
     */
    private val resultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DownloadService.ACTION_DOWNLOAD) {
                if (intent.getBooleanExtra(DownloadService.ACTION_SUCCESS, false)) {
                    postImage()
                    stopService()
                }
            }
        }
    }

    private fun postImage() {
        downloadService.bitmap?.let {
            bitmap = Bitmap.createScaledBitmap(it, BITMAP_WIDTH, BITMAP_HEIGHT, false)
            imageView.setImageBitmap(bitmap)
        }
    }

    private fun unbindService() {
        if (isBound) {
            isBound = false
            mActivity.unbindService(serviceConnection)
        }
    }

    private fun bindService() {
        mActivity.bindService(
            Intent(mActivity, DownloadService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        isBound = true
    }

    private fun stopService() {
        isDownloading = false
        mActivity.stopService(Intent(mActivity, DownloadService::class.java))
    }

    companion object {
        fun newInstance() = DownloadFragment()
        private const val BITMAP_WIDTH = 300
        private const val BITMAP_HEIGHT = 200
        private const val imageLink = "http://luxfon.com/images/201203/luxfon.com_3795.jpg"
    }
}
