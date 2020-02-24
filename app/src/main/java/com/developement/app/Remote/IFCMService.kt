package com.developement.app.Remote


import com.developement.app.Model.BraintreeToken
import com.developement.app.Model.BraintreeTransaction
import com.developement.app.Model.FCMRespone
import com.developement.app.Model.FCMSendData
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.*


interface IFCMService {
    @Headers(
        "Content-Type:application/json",
        "Authorization:key=AAAAsrQWZyo:APA91bG3QfLIR8T6wdcPZBVRwOYc2D4dkKcF_cHMJpA2Tj-lC8lxTwo113Z65YNzIyW62x64RAu5eQHCQQx74XRGc98mz1x6Per96UaNN_Y6LgwgJSwCxvLn8v5unkA3EJ4bxGJsJcvL"
    )
    @POST("fcm/send")
    fun sendNofitication(@Body body: FCMSendData): Observable<FCMRespone>


}