package com.developement.app.Callback

import com.developement.app.Model.CategoryModel


interface ICategoryCallbackListner {
    fun onCategoryLoadSuccess(categoriesList: List<CategoryModel>)
    fun onCategoryLoadFailed(message:String)
}