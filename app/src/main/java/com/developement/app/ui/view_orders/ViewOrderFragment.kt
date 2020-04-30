package com.developement.app.ui.view_orders

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidwidgets.formatedittext.widgets.FormatEditText
import com.developement.app.Adapter.MyOrderAdapter
import com.developement.app.Callback.ILoadOrderCallbackListner
import com.developement.app.Callback.IMyButtonCallback
import com.developement.app.Common.Common
import com.developement.app.Common.MySwipeHelper
import com.developement.app.EventBus.MenuItemBack
import com.developement.app.Model.OrderModel
import com.developement.app.Model.RefundRequestModel
import com.developement.app.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.layout_order_item.*
import kotlinx.android.synthetic.main.layout_refund.*
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ViewOrderFragment : Fragment(), ILoadOrderCallbackListner {


    private var viewOrderModel: ViewOrderModel? = null
    internal lateinit var dialog: AlertDialog
    internal lateinit var recycler_order: RecyclerView
    internal lateinit var listener: ILoadOrderCallbackListner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewOrderModel = ViewModelProviders.of(this).get(ViewOrderModel::class.java)
        val root = inflater.inflate(R.layout.fragment_view_orders, container, false)
        initViews(root)
        loadOrderFromFirebase()

        viewOrderModel!!.mutableLiveDataOrderList.observe(this, Observer {
            Collections.reverse(it!!)
            val adapter = MyOrderAdapter(context!!, it!!.toMutableList())
            recycler_order!!.adapter = adapter

        })

        return root
    }

    private fun loadOrderFromFirebase() {
        dialog.show()
        val orderList = ArrayList<OrderModel>()

        FirebaseDatabase.getInstance().getReference(Common.ORDER_REF)
            .orderByChild("userID")
            .equalTo(Common.currentUser!!.uid!!)
            .limitToLast(100)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    listener.onLoadOrderFailed(p0.message!!)
                }

                override fun onDataChange(p0: DataSnapshot) {
                    for (orderSnapShot in p0.children) {
                        // generate unique order if for each order
                        val order = orderSnapShot.getValue(OrderModel::class.java)
                        order!!.orderNumber = orderSnapShot.key // unique key
                        orderList.add(order!!)
                    }
                    listener.onLoadOrderSuccess(orderList)
                }

            })
    }

    private fun initViews(root: View?) {
        listener = this
        dialog = SpotsDialog.Builder().setContext(context!!).setCancelable(false).build()

        recycler_order = root!!.findViewById(R.id.recycler_order) as RecyclerView
        recycler_order.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context!!)
        recycler_order.layoutManager = layoutManager

        val swipe = object : MySwipeHelper(context!!, recycler_order!!, 250) {
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(
                    MyButton(context!!,
                        "Cancel Order",
                        30,
                        0,
                        Color.parseColor("#FF3c30"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {
                                val orderModel =
                                    (recycler_order.adapter as MyOrderAdapter).getItemAtPosition(pos)
                                if (orderModel.orderStatus == 0) {

                                   if(orderModel.isCod){
                                       val builder = androidx.appcompat.app.AlertDialog.Builder(context!!)
                                       builder.setTitle("Cancel Order")
                                           .setMessage("Do you need to cancel the order? ")
                                           .setNegativeButton("NO"){dialogInterface, i ->
                                               dialogInterface.dismiss()
                                           }
                                           .setPositiveButton("YES"){dialogInterface, i ->

                                               val update_data = HashMap<String, Any>()
                                               update_data.put("orderStatus", -1)
                                               FirebaseDatabase.getInstance().getReference(Common.ORDER_REF).child(orderModel.orderNumber!!)
                                                   .updateChildren(update_data)
                                                   .addOnFailureListener { e->
                                                       Toast.makeText(context!!, e.message, Toast.LENGTH_SHORT).show()
                                                   }
                                                   .addOnSuccessListener {
                                                       orderModel.orderStatus = -1
                                                       (recycler_order.adapter as MyOrderAdapter).setItemAtPosition(pos, orderModel)
                                                       (recycler_order.adapter as MyOrderAdapter).notifyItemChanged(pos)

                                                       Toast.makeText(context!!, "Cancelled", Toast.LENGTH_SHORT).show()
                                                   }

                                           }
                                       val dialog = builder.create()
                                       dialog.show()
                                   }
                                    else
                                   {
                                        val view = LayoutInflater.from(context!!).inflate(R.layout.layout_refund, null)

                                       val edt_name = view.findViewById<EditText>(R.id.edt_card_name)
                                       val edt_card_number = view.findViewById<FormatEditText>(R.id.edt_card_number)
                                       val edt_card_exp = view.findViewById<FormatEditText>(R.id.edt_exp)
                                       val btn_back = view.findViewById<Button>(R.id.btn_back_ref)
                                       val btn_send= view.findViewById<Button>(R.id.btn_send_req)

                                       // set format
                                       edt_card_number.setFormat("---- ---- ---- ----")
                                       edt_card_exp.setFormat("--/--")

                                       val builder = androidx.appcompat.app.AlertDialog.Builder(context!!)
                                       builder.setView(view)
                                       val dialog = builder.create()
                                        btn_send.setOnClickListener {
                                            val refundRequestModel = RefundRequestModel()
                                            refundRequestModel.name = Common.currentUser!!.name!!
                                            refundRequestModel.phone =Common.currentUser!!.phone!!
                                            refundRequestModel.cardNumber = edt_card_number.text.toString()
                                            refundRequestModel.cardExp = edt_card_exp.text.toString()
                                            refundRequestModel.amount = orderModel.finalPayment
                                            refundRequestModel.cardName = edt_name.text.toString()

                                            FirebaseDatabase.getInstance().getReference(Common.REFUND_REQUEST_REF)
                                                .child(orderModel.orderNumber!!)
                                                .setValue(refundRequestModel)
                                                .addOnFailureListener { e->
                                                    Toast.makeText(context!!, e.message, Toast.LENGTH_SHORT).show()
                                                }
                                                .addOnSuccessListener {
                                                    // update data firebase
                                                    val update_data = HashMap<String, Any>()
                                                    update_data.put("orderStatus", -1)
                                                    FirebaseDatabase.getInstance().getReference(Common.ORDER_REF).child(orderModel.orderNumber!!)
                                                        .updateChildren(update_data)
                                                        .addOnFailureListener { e->
                                                            Toast.makeText(context!!, e.message, Toast.LENGTH_SHORT).show()
                                                        }
                                                        .addOnSuccessListener {
                                                            orderModel.orderStatus = -1
                                                            (recycler_order.adapter as MyOrderAdapter).setItemAtPosition(pos, orderModel)
                                                            (recycler_order.adapter as MyOrderAdapter).notifyItemChanged(pos)

                                                            Toast.makeText(context!!, "Cancelled", Toast.LENGTH_SHORT).show()
                                                            dialog.dismiss()
                                                        }
                                                }
                                        }


                                       btn_back.setOnClickListener{
                                           dialog.dismiss()
                                       }
                                       dialog.show()
                                   }

                                } else {
                                    Toast.makeText(
                                        context!!,
                                        StringBuilder("Your order is ")
                                            .append(Common.convertStatusToText(orderModel.orderStatus))
                                            .append(", Therefore you cannot cancel the order."),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                            }

                        })
                )
            }
        }

    }

    override fun onLoadOrderSuccess(orderList: List<OrderModel>) {
        dialog.dismiss()
        viewOrderModel!!.setmutableLiveDataOrderList(orderList)
    }

    override fun onLoadOrderFailed(message: String) {
        dialog.dismiss()
        Toast.makeText(context!!, message, Toast.LENGTH_SHORT).show()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu!!.findItem(R.id.action_settings)
            .setVisible(false) // hide setting action in cart fragment
        menu!!.findItem(R.id.action_search).setVisible(false) // hide search action in cart fragment
        super.onPrepareOptionsMenu(menu)
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }

}