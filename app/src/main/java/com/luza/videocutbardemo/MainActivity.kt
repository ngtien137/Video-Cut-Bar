package com.luza.videocutbardemo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import com.luza.videocutbar.VideoCutBar
import com.luza.videocutbar.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val REQUEST_MUSIC = 100
        const val REQUEST_PERMISSION = 100
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        videoCutBar.formatDuration = SimpleDateFormat("mm:ss", Locale.getDefault())
        videoCutBar.loadingListener = object : VideoCutBar.ILoadingListener {
            override fun onLoadingStart() {
                Toast.makeText(this@MainActivity, "Video Loading", Toast.LENGTH_SHORT).show()
            }

            override fun onLoadingComplete() {
                Toast.makeText(this@MainActivity, "Video Loaded", Toast.LENGTH_SHORT).show()
            }
        }
        videoCutBar.rangeChangeListener = object : VideoCutBar.OnCutRangeChangeListener{
            override fun onRangeChanging(minValue: Long, maxValue: Long, isInteract: Boolean) {
                tvStatus.text = "Status: Changing"
                tvMin.text = "Min: $minValue"
                tvMax.text = "Max: $maxValue"
            }

            override fun onRangeChanged(minValue: Long, maxValue: Long, isInteract: Boolean) {
                tvStatus.text = "Status: Changed"
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnChooseAudio -> {
                doRequestPermission(
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),{
                        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                        startActivityForResult(intent, REQUEST_MUSIC)
                    },{finish()}
                )
            }
            R.id.btnRange -> {
                if (videoCutBar.duration==0L){
                    Toast.makeText(this, "Please choose video!", Toast.LENGTH_SHORT).show()
                    return
                }
                try{
                    val min = edtMin.text.toString().toLong()
                    val max = edtMax.text.toString().toLong()
                    videoCutBar.setSelectedRange(min,max)
                }catch (e:Exception){
                    Toast.makeText(this, "Wrong range", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.btnProgress -> {
                if (videoCutBar.duration==0L){
                    Toast.makeText(this, "Please choose video!", Toast.LENGTH_SHORT).show()
                    return
                }
                try{
                    val progress = edtProgress.text.toString().toLong()
                    videoCutBar.setProgress(progress)
                }catch (e:Exception){
                    Toast.makeText(this, "Wrong progress", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_MUSIC -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.let {
                        val path = getPath(it.data!!)
                        if (path != null) {
                            Log.e("Path Video: $path")
                            if (File(path).exists()) {
                                videoCutBar.videoPath = path
                            } else
                                Toast.makeText(
                                    this,
                                    "File is not exists",
                                    Toast.LENGTH_SHORT
                                ).show()
                        } else {
                            Toast.makeText(this, "Can't find path by uri", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("Recycle")
    fun getPath(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        if (cursor != null) {
            val column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(column_index)
        } else
            return null
    }


    private var onAllow: (() -> Unit)? = null
    private var onDenied: (() -> Unit)? = null
    fun checkPermission(permissions: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions.forEach {
                if (checkSelfPermission(it) ==
                    PackageManager.PERMISSION_DENIED
                ) {
                    return false
                }
            }
        }
        return true
    }

    protected fun doRequestPermission(
        permissions: Array<String>,
        onAllow: () -> Unit = {}, onDenied: () -> Unit = {}
    ) {
        this.onAllow = onAllow
        this.onDenied = onDenied
        if (checkPermission(permissions)) {
            onAllow()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, REQUEST_PERMISSION)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (checkPermission(permissions)) {
            onAllow?.invoke()
        } else {
            onDenied?.invoke()
        }
    }
}
