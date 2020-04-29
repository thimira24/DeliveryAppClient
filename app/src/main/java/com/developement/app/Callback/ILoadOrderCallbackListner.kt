package com.developement.app.Callback

import com.developement.app.Model.OrderModel

interface ILoadOrderCallbackListner {
    fun onLoadOrderSuccess(orderList: List<OrderModel>)
    fun onLoadOrderFailed(message: String)
}