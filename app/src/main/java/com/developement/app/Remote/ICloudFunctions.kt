package com.developement.app.Remote


import com.developement.app.Model.BraintreeToken
import com.developement.app.Model.BraintreeTransaction
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.*


interface ICloudFunctions {
    @GET("token")
    fun getToken(@HeaderMap headers: Map<String, String>): Observable<BraintreeToken>

    @POST("checkout")
    @FormUrlEncoded
    fun submitPayment(
        @HeaderMap headers: Map<String, String>,
        @Field("amount") amount: Double,
        @Field("payment_method_nonce") nonce: String
    ): Observable<BraintreeTransaction>
}