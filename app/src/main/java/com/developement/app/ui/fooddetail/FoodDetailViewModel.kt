package com.developement.app.ui.fooddetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.developement.app.Common.Common
import com.developement.app.Model.CommentModel
import com.developement.app.Model.FoodModel

class FoodDetailViewModel : ViewModel() {

    private var mutableLiveDataFood: MutableLiveData<FoodModel>? = null
    private var mutableLiveDataComment: MutableLiveData<CommentModel>? = null

    init {
        mutableLiveDataComment = MutableLiveData()
    }

    fun getmutableLiveDataFood(): MutableLiveData<FoodModel> {
        if (mutableLiveDataFood == null)
            mutableLiveDataFood = MutableLiveData()
        mutableLiveDataFood!!.value = Common.foodSelected
        return mutableLiveDataFood!!
    }

    fun getmutableLiveDataComment(): MutableLiveData<CommentModel> {
        if (mutableLiveDataComment == null)
            mutableLiveDataComment = MutableLiveData()
        return mutableLiveDataComment!!
    }

    fun setCommentModel(commentModel: CommentModel) {
        if (mutableLiveDataComment != null)
            mutableLiveDataComment!!.value = (commentModel)
    }

    fun setFoodModel(foodModel: FoodModel) {
        if (mutableLiveDataFood != null)
            mutableLiveDataFood!!.value = foodModel
    }


}