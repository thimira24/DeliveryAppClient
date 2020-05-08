package com.developement.app.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.developement.app.Database.CartItem
import com.developement.app.R
import com.google.gson.Gson

class MyOrderDetailAdapter(
    internal var context: Context,
    internal var cartItemList: MutableList<CartItem>
) : RecyclerView.Adapter<MyOrderDetailAdapter.MyViewHolder>() {
    val gson: Gson = Gson()

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txt_food_name: TextView? = null
        var txt_food_quantity: TextView? = null
        var txt_food_price: TextView? = null
        var img_food_image: ImageView? = null

        init {
            img_food_image = itemView.findViewById(R.id.img_food_image_order) as ImageView
            txt_food_name = itemView.findViewById(R.id.txt_food_name_order) as TextView
            txt_food_quantity = itemView.findViewById(R.id.txt_food_quantity_order) as TextView
            txt_food_price = itemView.findViewById(R.id.txt_food_price_order) as TextView
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.layout_order_detail_item, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return cartItemList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(cartItemList[position].foodImage).into(holder.img_food_image!!)
        holder.txt_food_quantity!!.setText(StringBuilder("Quantity: ").append(cartItemList[position].foodQuantity))
        holder.txt_food_name!!.setText(StringBuilder("").append(cartItemList[position].foodName))
        holder.txt_food_price!!.setText(StringBuilder("Rs ").append(cartItemList[position].foodPrice))
    }

}