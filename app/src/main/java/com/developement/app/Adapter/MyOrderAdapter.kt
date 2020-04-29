package com.developement.app.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.developement.app.Common.Common
import com.developement.app.Model.OrderModel
import com.developement.app.R
import java.text.SimpleDateFormat
import java.util.*

class MyOrderAdapter(
    private val context: Context,
    private val orderList: MutableList<OrderModel>
) : RecyclerView.Adapter<MyOrderAdapter.MyViewHolder>() {

    internal var calender : Calendar
    internal var simpleDateFormat: SimpleDateFormat

    init {
        calender = Calendar.getInstance()
        simpleDateFormat = SimpleDateFormat("dd-MM-yyyy, HH:mm")
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        internal var img_order: ImageView? = null
        internal var txt_order_date: TextView? = null
        internal var txt_order_status: TextView? = null
        internal var txt_order_number: TextView? = null
        internal var txt_order_comment: TextView? = null
        internal var txt_show_order: TextView? = null

        init {
            img_order = itemView.findViewById(R.id.img_order) as ImageView
            txt_order_comment = itemView.findViewById(R.id.txt_order_commnet) as TextView
            txt_order_status = itemView.findViewById(R.id.txt_order_stsus) as TextView
            txt_order_date = itemView.findViewById(R.id.txt_order_date) as TextView
            txt_order_number = itemView.findViewById(R.id.txt_order_number) as TextView
            txt_show_order = itemView.findViewById(R.id.show_total) as TextView
        }
    }

    override fun getItemCount(): Int {
        return orderList.size
    }

    fun getItemAtPosition(position: Int): OrderModel{
        return orderList[position]
    }

    fun setItemAtPosition(position: Int, orderModel: OrderModel){
        orderList[position] = orderModel
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context!!)
            .inflate(R.layout.layout_order_item, parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        Glide.with(context!!).load(orderList[position].cartItemList!![0].foodImage).into(holder.img_order!!)
        calender.timeInMillis = orderList[position].createDate
        val date = Date(orderList[position].createDate)

        holder.txt_order_date!!.text = StringBuilder(Common.getDateOfWeek(calender.get(Calendar.DAY_OF_WEEK)))
            .append(" ")
            .append(simpleDateFormat.format(date))

        holder.txt_order_number!!.text = StringBuilder("").append(orderList[position].orderNumber)
        holder.txt_order_comment!!.text = StringBuilder("Note: ").append(orderList[position].comment)
        holder.txt_order_status!!.text = StringBuilder("Order ").append(Common.convertStatusToText(orderList[position].orderStatus))
        holder.txt_show_order!!.text = StringBuilder("Rs ").append(orderList[position].totalPayment)
    }

}