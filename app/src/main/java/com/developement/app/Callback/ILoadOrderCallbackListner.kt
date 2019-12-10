package com.developement.app.Callback

import com.developement.app.Model.Order

interface ILoadOrderCallbackListner {
    fun onLoadOrderSuccess(orderList: List<Order>)
    fun onLoadOrderFailed(message: String)
}