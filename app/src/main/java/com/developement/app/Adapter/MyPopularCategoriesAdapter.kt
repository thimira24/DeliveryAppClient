package com.developement.app.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.developement.app.Callback.IRecyclerItemClickListner
import com.developement.app.EventBus.PopularFoodItemClick
import com.developement.app.Model.PopularCategoryModel
import com.developement.app.R
import de.hdodenhof.circleimageview.CircleImageView
import org.greenrobot.eventbus.EventBus


class MyPopularCategoriesAdapter(
    internal var context: Context,
    internal var popularCategoryModel: List<PopularCategoryModel>
) :
    RecyclerView.Adapter<MyPopularCategoriesAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.layout_grocery_categories_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return popularCategoryModel.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(popularCategoryModel.get(position).image).into(holder.category_image!!)
        holder.category_name!!.setText(popularCategoryModel.get(position).name)

        holder.setListner(object:IRecyclerItemClickListner{
            override fun onItemClick(view: View, pos: Int) {
                EventBus.getDefault()
                    .postSticky(PopularFoodItemClick(popularCategoryModel[pos]))
            }
        })
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
    View.OnClickListener {

        override fun onClick(p0: View?) {
            listener!!.onItemClick(p0!!, adapterPosition)
        }

        var category_name: TextView? = null

        var category_image: CircleImageView? = null

        internal var listener: IRecyclerItemClickListner? = null

        fun setListner(listner: IRecyclerItemClickListner)
        {
            this.listener = listner
        }


        init {
            category_name = itemView.findViewById(R.id.category_name) as TextView
            category_image = itemView.findViewById(R.id.category_image) as CircleImageView
            itemView.setOnClickListener(this)
        }
    }
}