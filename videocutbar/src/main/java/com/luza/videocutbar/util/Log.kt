package com.luza.videocutbar.util

object Log{
    const val TAG = "LuzaVideoCutBar"
    var SHOWLOG = true
    fun e(message:String){
        if (SHOWLOG)
            android.util.Log.e(TAG,message)
    }
}