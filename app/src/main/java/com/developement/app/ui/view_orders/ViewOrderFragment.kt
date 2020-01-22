package com.developement.app.ui.view_orders

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.developement.app.Adapter.MyOrderAdapter
import com.developement.app.Callback.ILoadOrderCallbackListner
import com.developement.app.Common.Common
import com.developement.app.EventBus.MenuItemBack
import com.developement.app.Model.Order
import com.developement.app.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList

class ViewOrderFragment : Fragment(),  ILoadOrderCallbackListner{


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
            val adapter = MyOrderAdapter(context!!, it!!)
            recycler_order!!.adapter = adapter

        })

        return root
    }

    private fun loadOrderFromFirebase() {
        dialog.show()
        val orderList = ArrayList<Order>()

        FirebaseDatabase.getInstance().getReference(Common.ORDER_REF)
            .orderByChild("userID")
            .equalTo(Common.currentUser!!.uid!!)
            .limitToLast(100)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    listener.onLoadOrderFailed(p0.message!!)
                }

                override fun onDataChange(p0: DataSnapshot) {
                    for (orderSnapShot in p0.children)
                    {
                        // generate unique order if for each order
                        val order = orderSnapShot.getValue(Order::class.java)
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

    }

    override fun onLoadOrderSuccess(orderList: List<Order>) {
        dialog.dismiss()
        viewOrderModel!!.setmutableLiveDataOrderList(orderList)
    }

    override fun onLoadOrderFailed(message: String) {
        dialog.dismiss()
        Toast.makeText(context!!, message, Toast.LENGTH_SHORT).show()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu!!.findItem(R.id.action_settings).setVisible(false) // hide setting action in cart fragment
        menu!!.findItem(R.id.action_search).setVisible(false) // hide search action in cart fragment
        super.onPrepareOptionsMenu(menu)
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }

}