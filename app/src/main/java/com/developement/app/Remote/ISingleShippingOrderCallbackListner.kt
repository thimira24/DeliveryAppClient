package com.developement.app.Remote

import com.developement.app.Model.ShippingOrderModel

interface ISingleShippingOrderCallbackListner {
    fun onSingleShippingOrderSuccess(shippingOrderModel: ShippingOrderModel)
}