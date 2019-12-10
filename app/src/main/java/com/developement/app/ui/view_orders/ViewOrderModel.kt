package com.developement.app.ui.view_orders

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.developement.app.Model.Order

class ViewOrderModel : ViewModel() {

    val mutableLiveDataOrderList: MutableLiveData<List<Order>>

    init {
        mutableLiveDataOrderList = MutableLiveData()
    }
    fun setmutableLiveDataOrderList(orderList: List<Order>)
    {
        mutableLiveDataOrderList.value = orderList
    }
}