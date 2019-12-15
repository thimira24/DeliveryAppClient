package com.developement.app.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton
import com.developement.app.Database.CartDataSource
import com.developement.app.Database.CartDatabase
import com.developement.app.Database.CartItem
import com.developement.app.Database.LocalClassDataSource
import com.developement.app.EventBus.UpdateItemInCart
import com.developement.app.R
import de.hdodenhof.circleimageview.CircleImageView
import io.reactivex.disposables.CompositeDisposable
import org.greenrobot.eventbus.EventBus


class MyCartAdapter (
    internal var context: Context,
    internal var cartItems: List<CartItem>
) :
    RecyclerView.Adapter<MyCartAdapter.MyViewHolder>() {

    internal var compositeDisposable: CompositeDisposable
    internal var cartDataSource: CartDataSource

    init {
        compositeDisposable = CompositeDisposable()
        cartDataSource = LocalClassDataSource(CartDatabase.getInstance(context).cartDAO())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_cart_item, parent, false))
    }

    override fun getItemCount(): Int {
        return cartItems.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(cartItems[position].foodImage).into(holder.img_cart)
        holder.txt_food_name.text = StringBuilder(cartItems[position].foodName!!)
        holder.txt_food_price.text = StringBuilder("").append(cartItems[position].foodPrice + cartItems[position].foodExtraPrice)
        holder.number_button.number = cartItems[position].foodQuantity.toString()
       // holder.rating_bar!!.rating = cartItems.get(position).ratingValue
        //EventBus
        holder.number_button.setOnValueChangeListener { view, oldValue, newValue ->
            cartItems[position].foodQuantity = newValue
            EventBus.getDefault().postSticky(UpdateItemInCart(cartItems[position]))
        }
    }

    fun getItemAtPosition(pos: Int): CartItem {
        return cartItems[pos]
    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        var img_cart: ImageView
        var txt_food_name: TextView
        var txt_food_price: TextView
        var number_button: ElegantNumberButton
       // var rating_bar: RatingBar? = null

        init {
            img_cart = itemView.findViewById(R.id.img_cart) as ImageView
            txt_food_name = itemView.findViewById(R.id.txt_food_name) as TextView
            txt_food_price = itemView.findViewById(R.id.txt_food_price) as TextView
            number_button = itemView.findViewById(R.id.number_button) as ElegantNumberButton
           // rating_bar = itemView.findViewById(R.id.rating_bar_cart) as RatingBar
        }
    }
}