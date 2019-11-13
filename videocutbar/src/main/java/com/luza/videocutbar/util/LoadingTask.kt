package com.luza.videocutbar.util

import android.os.AsyncTask
import java.lang.Exception

class LoadingTask(var iLoading:ILoadingTask) : AsyncTask<Void,Void,Void>(){

    override fun onPreExecute() {
        super.onPreExecute()
        iLoading.onLoadingStart()
    }

    override fun doInBackground(vararg p0: Void?): Void? {
        iLoading.onLoading()
        return null
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        iLoading.onLoadingEnd()
    }

    fun cancelTask(){
        try{
            cancel(true)
        }catch (e:Exception){}
    }

    fun run(): LoadingTask {
        execute()
        return this
    }

    interface ILoadingTask{
        fun onLoadingStart(){}
        fun onLoading()
        fun onLoadingEnd(){}
    }

}