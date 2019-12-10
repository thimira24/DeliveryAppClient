package com.developement.app.ui.foodlist


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.developement.app.Common.Common
import com.developement.app.Model.FoodModel


class FoodListViewModel : ViewModel() {

    private var mutableFoodModelListData: MutableLiveData<List<FoodModel>>? = null

    fun mutableFoodModelListData(): MutableLiveData<List<FoodModel>> {
        if (mutableFoodModelListData == null)
            mutableFoodModelListData = MutableLiveData()
        mutableFoodModelListData!!.value = Common.categorySelected!!.foods
        return mutableFoodModelListData!!
    }
}