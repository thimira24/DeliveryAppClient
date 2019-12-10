package com.developement.app.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.developement.app.Callback.IRecyclerItemClickListner
import com.developement.app.Common.Common
import com.developement.app.Database.CartDataSource
import com.developement.app.Database.CartDatabase
import com.developement.app.Database.CartItem
import com.developement.app.Database.LocalClassDataSource
import com.developement.app.EventBus.CounterCartEvent
import com.developement.app.EventBus.FoodItemClick
import com.developement.app.Model.FoodModel
import com.developement.app.R
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus

class MyFoodListAdapter(
    internal var context: Context,
    internal var foodList: List<FoodModel>
) :
    RecyclerView.Adapter<MyFoodListAdapter.MyViewHolder>() {

    private val compositeDisposable : CompositeDisposable
    private val cartDataSource : CartDataSource

    init {
        compositeDisposable = CompositeDisposable()
        cartDataSource = LocalClassDataSource(CartDatabase.getInstance(context).cartDAO())
    }


    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(foodList.get(position).image).into(holder.img_food_image!!)
        holder.txt_food_name!!.setText(foodList.get(position).name)
        holder.txt_food_price!!.setText(foodList.get(position).price.toString())

        //event
        holder.setListner(object:IRecyclerItemClickListner{
            override fun onItemClick(view: View, pos: Int) {
                Common.foodSelected = foodList.get(pos)
                Common.foodSelected!!.key = pos.toString()
                EventBus.getDefault().postSticky(FoodItemClick(true, foodList.get(pos)))
            }

        })

        holder.img_cart!!.setOnClickListener{
            val cartItem = CartItem()
            cartItem.uid = Common.currentUser!!.uid
            cartItem.userPhone = Common.currentUser!!.phone

            cartItem.foodID = foodList.get(position).id!!
            cartItem.foodName = foodList.get(position).name!!
            cartItem.foodImage = foodList.get(position).image!!
            cartItem.foodPrice = foodList.get(position).price!!.toDouble()
            cartItem.foodQuantity = 1
            cartItem.foodExtraPrice = 0.0
            cartItem.foodAddon = "Default"
            //cartItem.foodSize = "Default"

            cartDataSource.getItemWithAllOptions(Common.currentUser!!.uid!!,
                cartItem.foodID,
                cartItem.foodAddon!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object: SingleObserver<CartItem>{
                    override fun onSuccess(cartItemFromDB: CartItem) {
                       if (cartItemFromDB.equals(cartItem))
                       {
                           // if item already in databse, just update
                           cartItemFromDB.foodExtraPrice = cartItem.foodExtraPrice
                           cartItemFromDB.foodAddon = cartItem.foodAddon
                           cartItemFromDB.foodQuantity = cartItemFromDB.foodQuantity + cartItem.foodQuantity

                           cartDataSource.updateCart(cartItemFromDB)
                               .subscribeOn(Schedulers.io())
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe(object: SingleObserver<Int>{
                                   override fun onSuccess(t: Int) {
                                       Toast.makeText(context, "Cart updated", Toast.LENGTH_SHORT).show()
                                       EventBus.getDefault().postSticky(CounterCartEvent(true))
                                   }

                                   override fun onSubscribe(d: Disposable) {

                                   }

                                   override fun onError(e: Throwable) {
                                       Toast.makeText(context, "[UPDATE CART]"+e.message, Toast.LENGTH_SHORT).show()
                                   }

                               })
                       }
                        else
                       {
                           // if item not available in the cart , then insert
                           compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItem)
                               .subscribeOn(Schedulers.io())
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe({
                                   Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show()
                                   //send notification
                                   EventBus.getDefault().postSticky(CounterCartEvent(true))
                               },{
                                       t:Throwable -> Toast.makeText(context, "[INSERT CART]"+t!!.message,Toast.LENGTH_SHORT).show()
                               }))
                       }
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        if (e.message!!.contains("empty"))
                        {
                            compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItem)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show()
                                    //send notification
                                    EventBus.getDefault().postSticky(CounterCartEvent(true))
                                },{
                                        t:Throwable -> Toast.makeText(context, "[INSERT CART]"+t!!.message,Toast.LENGTH_SHORT).show()
                                }))
                        }
                        else
                            Toast.makeText(context, "[CART ERROR]"+e.message, Toast.LENGTH_SHORT).show()
                    }

                })
        }

    }

    fun onStop(){
        if (compositeDisposable != null)
            compositeDisposable.clear()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyFoodListAdapter.MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.layout_food_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return foodList.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        override fun onClick(view: View?) {
            listener!!.onItemClick(view!!, adapterPosition)
        }

        var txt_food_name: TextView? = null
        var txt_food_price: TextView? = null

        var img_food_image: ImageView? = null
        var img_fav: ImageView? = null
        var img_cart: ImageView? = null

        internal var listener: IRecyclerItemClickListner? = null

        fun setListner(listner: IRecyclerItemClickListner)
        {
            this.listener = listner
        }


        init {
            txt_food_name = itemView.findViewById(R.id.txt_food_name) as TextView
            txt_food_price = itemView.findViewById(R.id.txt_food_price) as TextView
            img_food_image = itemView.findViewById(R.id.img_food_image) as ImageView
            img_fav = itemView.findViewById(R.id.img_fav) as ImageView
            img_cart = itemView.findViewById(R.id.img_quick_cart) as ImageView

            itemView.setOnClickListener(this)

        }
    }
}