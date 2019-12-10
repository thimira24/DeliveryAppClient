package com.developement.app.Callback

import com.developement.app.Model.BestDealModel


interface IBestDealLoadCallback {
    fun onBestDealLoadSuccess(bestDealList: List<BestDealModel>)
    fun onBestDealLoadFailed(message:String)
}