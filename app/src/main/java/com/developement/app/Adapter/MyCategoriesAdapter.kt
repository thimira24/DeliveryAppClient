package com.developement.app.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.developement.app.Callback.IRecyclerItemClickListner
import com.developement.app.Common.Common
import com.developement.app.EventBus.CategoryClick
import com.developement.app.Model.CategoryModel
import com.developement.app.R
import org.greenrobot.eventbus.EventBus


class MyCategoriesAdapter(
    internal var context: Context,
    internal var categoriesList: List<CategoryModel>
) :
    RecyclerView.Adapter<MyCategoriesAdapter.MyViewHolder>() {
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(categoriesList.get(position).image).into(holder.category_image!!)
        holder.category_name!!.setText(categoriesList.get(position).name)

        // event
        holder.setListner(object : IRecyclerItemClickListner {
            override fun onItemClick(view: View, pos: Int) {
                Common.categorySelected = categoriesList.get(pos)
                EventBus.getDefault().postSticky(CategoryClick(true, categoriesList.get(pos)))
            }

        })
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyCategoriesAdapter.MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.layout_category_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return categoriesList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (categoriesList.size == 1)
            Common.DEFAULT_COLUMN_COUNT
        else {
            if (categoriesList.size % 2 == 0)
                Common.DEFAULT_COLUMN_COUNT
            else
                if (position > 1 && position == categoriesList.size - 1) Common.FULL_WIDTH_COLUMN else Common.DEFAULT_COLUMN_COUNT
        }

    }

    fun getCategoryList(): List<CategoryModel>{
        return categoriesList
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        override fun onClick(view: View?) {
            listener!!.onItemClick(view!!, adapterPosition)
        }


        var category_name: TextView? = null

        var category_image: ImageView? = null

        //Create click listner event for category
        // when user click a category it shows items inside the selected category
        internal var listener: IRecyclerItemClickListner? = null

        fun setListner(listner: IRecyclerItemClickListner)
        {
            this.listener = listner
        }


        init {
            category_name = itemView.findViewById(R.id.category_name) as TextView
            category_image = itemView.findViewById(R.id.category_image) as ImageView
            itemView.setOnClickListener(this)
        }
    }
}