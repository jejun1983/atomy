package kr.co.medialog

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import api.Constants
import api.OnResultListener
import api.OwlApi
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


class ApiManager(val context: Context) {

//    var mUmobiApi: OwlApi = OwlApi.create(context)

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: ApiManager? = null

        @JvmStatic
        fun getInstance(context: Context): ApiManager =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: ApiManager(context).also { INSTANCE = it }
                }
    }

//    fun getSettingInfo(url: String, listener: OnResultListener<Any>) {
//        if (url.isNullOrEmpty()) {
//            return
//        }
//
//        val call = mUmobiApi.getSettingInfo(url)
//
//        call.enqueue(object : Callback<SettingInfoData> {
//            override fun onResponse(call: Call<SettingInfoData>, response: Response<SettingInfoData>) {
//                if (response.code() == 200) {
//                    if (response.body() != null) {
//                        listener.onResult(response.body() as SettingInfoData, Constants.APP_SETTING_DATA)
//                    }
//                }
//            }
//
//            override fun onFailure(error: Call<SettingInfoData>, t: Throwable) {
//                listener.onFail(t, Constants.APP_SETTING_DATA)
//            }
//        })
//    }


    fun uploadFile(url: String, filePath: String, fileName: String, type: String, param: String, listener: OnResultListener<Any>) {
        if (filePath.isNullOrEmpty() && fileName.isNullOrEmpty()) {
            return
        }

        Log.e("bjj", "bjj getPlayInfo :: " + url + " ^ " + filePath + " ^ " + fileName + " ^ " + type + " ^ " + param)

        val myFile = File(filePath)
        val requestFile: RequestBody = RequestBody.create(MediaType.parse("multipart/form-data"), myFile)
        val img = MultipartBody.Part.createFormData("image", myFile.name, requestFile)
        val imgFileName: RequestBody = RequestBody.create(MediaType.parse("multipart/form-data"), filePath)
        val imgType: RequestBody = RequestBody.create(MediaType.parse("multipart/form-data"), type)
        val imgParam: RequestBody = RequestBody.create(MediaType.parse("multipart/form-data"), param)

        val mUmobiApi: OwlApi = OwlApi.create(context, "https://dment.wtest.biz/")
        val call = mUmobiApi.uploadFile(url, img, imgFileName, imgType, imgParam)

        call.enqueue(object : Callback<UploadInfoData> {
            override fun onResponse(call: Call<UploadInfoData>, response: Response<UploadInfoData>) {
                if (response.code() == 200) {
                    if (response.body() != null) {
                        listener.onResult(response.body() as UploadInfoData, Constants.APP_FILE_UPLOAD_DATA)
                    }
                }
            }

            override fun onFailure(error: Call<UploadInfoData>, t: Throwable) {
                listener.onFail(t, Constants.APP_FILE_UPLOAD_DATA)
            }
        })

    }

    fun startRecordDownload(baseUrl: String, url: String, listener: OnResultListener<Any>) {
        var myUrl = url

//        if (!myUrl.endsWith("/")) {
//            myUrl += "/"
//        }

        val mUmobiApi: OwlApi = OwlApi.create(context, baseUrl)
        val call = mUmobiApi.getRecordFile(myUrl)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>?, response: Response<ResponseBody>?) {
                if (response!!.isSuccessful) {
                    listener.onResult(response.body()!!, Constants.APP_FILE_DOWNLOAD_DATA)
                }
            }

            override fun onFailure(call: Call<ResponseBody>?, t: Throwable) {
                listener.onFail(t, Constants.APP_FILE_DOWNLOAD_DATA)
            }
        })
    }
}
