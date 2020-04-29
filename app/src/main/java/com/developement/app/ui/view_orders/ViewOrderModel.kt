package com.developement.app.ui.view_orders

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.developement.app.Model.OrderModel

class ViewOrderModel : ViewModel() {

    val mutableLiveDataOrderList: MutableLiveData<List<OrderModel>>

    init {
        mutableLiveDataOrderList = MutableLiveData()
    }
    fun setmutableLiveDataOrderList(orderList: List<OrderModel>)
    {
        mutableLiveDataOrderList.value = orderList
    }
}