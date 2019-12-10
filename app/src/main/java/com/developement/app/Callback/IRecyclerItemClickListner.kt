package com.developement.app.Callback

import android.view.View

interface IRecyclerItemClickListner {
    fun onItemClick(view: View, pos: Int)
}