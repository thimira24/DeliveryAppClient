package com.developement.app.Callback

import com.developement.app.Model.OrderModel

interface ILoadTimeFromCallback {
    // this interface use for sync phone time with server
    // time to make sure the time is real
    fun onLoadTimeSuccess(order: OrderModel, estimatedTimeMs:Long)
    fun onLoadTimeFailed(message:String)
}