package com.developement.app.ui.cart

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.braintreepayments.api.dropin.DropInRequest
import com.braintreepayments.api.dropin.DropInResult
import com.developement.app.Adapter.MyCartAdapter
import com.developement.app.Callback.ILoadTimeFromCallback
import com.developement.app.Callback.IMyButtonCallback
import com.developement.app.Common.Common
import com.developement.app.Common.MySwipeHelper
import com.developement.app.Database.CartDataSource
import com.developement.app.Database.CartDatabase
import com.developement.app.Database.CartItem
import com.developement.app.Database.LocalClassDataSource
import com.developement.app.EventBus.CounterCartEvent
import com.developement.app.EventBus.HideFABCart
import com.developement.app.EventBus.MenuItemBack
import com.developement.app.EventBus.UpdateItemInCart
import com.developement.app.HomeActivity
import com.developement.app.Model.FCMSendData
import com.developement.app.Model.OrderModel
import com.developement.app.R
import com.developement.app.Remote.ICloudFunctions
import com.developement.app.Remote.IFCMService
import com.developement.app.Remote.RetrofitCloudClient
import com.developement.app.Remote.RetrofitFCMClient
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class CartFragment : Fragment(), ILoadTimeFromCallback {

    private val REQUEST_BRAINTREE_CODE: Int = 8888
    private var cartDataSource:CartDataSource?=null
    private var compositeDisposable: CompositeDisposable = CompositeDisposable()
    private var RecyclerViewState:Parcelable?= null
    private lateinit var cartViewModel: CartViewModel
    private lateinit var btn_place_order:Button

    var txt_empty_cart: TextView? = null
    var txt_total_price: TextView? = null
    var group_place_holder: RelativeLayout? = null
    var continue_shopping: TextView? = null
    var recycler_cart: RecyclerView? = null
    var empty_img_cart: ImageView? = null
    var btn_shopping: Button? = null
    var adapter: MyCartAdapter? = null



    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentLocation:Location

    internal var address:String = ""
    internal var comment:String = ""

    lateinit var cloudFunctions: ICloudFunctions
    lateinit var ifcmService: IFCMService
    lateinit var listener: ILoadTimeFromCallback

    override fun onResume() {
        super.onResume()
        calculateTotalPrice()
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        setHasOptionsMenu(true)

        EventBus.getDefault().postSticky(HideFABCart(true))

        cartViewModel =
            ViewModelProviders.of(this).get(CartViewModel::class.java)
        cartViewModel.initCartdataSource(context!!)
        val root = inflater.inflate(R.layout.fragment_cart, container, false)
        initViews(root)
        initLocation()
        cartViewModel.getMutableLiveDataCartItem().observe(this, Observer{
            if (it == null || it.isEmpty())
            {
                recycler_cart!!.visibility = View.GONE
                group_place_holder!!.visibility = View.GONE
                txt_empty_cart!!.visibility = View.VISIBLE
                empty_img_cart!!.visibility = View.VISIBLE
                continue_shopping!!.visibility = View.VISIBLE
            }
            else
            {
                recycler_cart!!.visibility = View.VISIBLE
                group_place_holder!!.visibility = View.VISIBLE
                txt_empty_cart!!.visibility = View.GONE
                empty_img_cart!!.visibility = View.GONE
                continue_shopping!!.visibility = View.GONE

                adapter = MyCartAdapter(context!!, it)
                recycler_cart!!.adapter = adapter
            }
        })
        return root
    }

    private fun initLocation() {
        buildLocationRequest()
        buildLocationCallback()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context!!)
        fusedLocationProviderClient!!.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun buildLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                currentLocation = p0!!.lastLocation
            }
        }
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setInterval(5000)
        locationRequest.setFastestInterval(3000)
        locationRequest.setSmallestDisplacement(10f)
    }

    private fun initViews(root:View) {

        setHasOptionsMenu(true)//note
        EventBus.getDefault().postSticky(HideFABCart(true))
        cloudFunctions = RetrofitCloudClient.getInstance().create(ICloudFunctions::class.java)
        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService::class.java)

        listener = this

        cartDataSource = LocalClassDataSource(CartDatabase.getInstance(context!!).cartDAO())

        recycler_cart = root.findViewById(R.id.recycler_cart) as RecyclerView
        recycler_cart!!.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(context)
        recycler_cart!!.layoutManager = layoutManager


        val swipe = object: MySwipeHelper(context!!, recycler_cart!!, 200) {
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(MyButton(context!!,
                    "Delete",
                    30,
                    0,
                    Color.parseColor("#FF3c30"),
                    object :IMyButtonCallback{
                        override fun onClick(pos: Int) {

                            val deleteItem = adapter!!.getItemAtPosition(pos)
                            cartDataSource!!.deleteCart(deleteItem)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(object: SingleObserver<Int>{
                                    override fun onSuccess(t: Int) {
                                        adapter!!.notifyItemRemoved(pos)
                                        sumCart()
                                        EventBus.getDefault().postSticky(CounterCartEvent(true))
                                        //Toast.makeText(context, "Deleted from cart", Toast.LENGTH_SHORT).show()
                                        Snackbar.make(view!!, "Deleted in your cart", Snackbar.LENGTH_SHORT).show()
                                    }

                                    override fun onSubscribe(d: Disposable) {

                                    }

                                    override fun onError(e: Throwable) {
                                        Toast.makeText(context, ""+e.message, Toast.LENGTH_SHORT).show()
                                    }

                                })

                        }

                    }))
            }

        }

        txt_empty_cart = root.findViewById(R.id.txt_empty_cart) as TextView
        txt_total_price = root.findViewById(R.id.txt_total_price) as TextView
        group_place_holder = root.findViewById(R.id.group_Place_holder) as RelativeLayout
        continue_shopping = root.findViewById(R.id.btn_shopping) as TextView
        empty_img_cart = root.findViewById(R.id.empty_img_cart) as ImageView
        btn_shopping = root.findViewById(R.id.btn_shopping) as Button


        // continue shopping button action

        btn_shopping!!.setOnClickListener {
            startActivity(Intent(context, HomeActivity::class.java))

        }

        btn_place_order = root.findViewById(R.id.btn_place_order) as Button
        // event
        btn_place_order.setOnClickListener {

            val builder = AlertDialog.Builder(context!!)
            val view = LayoutInflater.from(context).inflate(R.layout.layout_place_order, null)

            val  edt_address = view.findViewById<View>(R.id.edt_address) as EditText
            val  edt_comment = view.findViewById<View>(R.id.edt_txt_comment) as EditText
            val  txt_address = view.findViewById<View>(R.id.txt_address_deetails) as TextView
            val  rdi_home = view.findViewById<View>(R.id.rdi_home_address) as RadioButton
            val  rdi_other_address = view.findViewById<View>(R.id.rdi_other_address) as RadioButton
            val  rdi_ship_to_this_address = view.findViewById<View>(R.id.rdi_ship_this_address) as RadioButton
            val  rdi_cod = view.findViewById<View>(R.id.rdi_cod) as RadioButton
            val  rdi_braintree = view.findViewById<View>(R.id.rdi_braintree) as RadioButton
            val  edit_address = view.findViewById<View>(R.id.edt_edit_address) as TextView



            //pay button and dissmiss button
            val btn_pay = view.findViewById<View>(R.id.btn_pay) as Button
            val txt_back = view.findViewById<View>(R.id.txt_back) as ImageView

            //show total amount
            val show_total_amount_in_dialog = view.findViewById<View>(R.id.txt_back) as ImageView

            // set address not editable
            edt_address.isEnabled = false

            //set edit text enable
            edit_address.setOnClickListener {
                edt_address.isEnabled = true
            }

            //data
            edt_address.setText (Common.currentUser!!.address!!)

            //event home address
            rdi_home.setOnCheckedChangeListener { compoundButton, b ->
                if (b)
                {
                    edt_address.setText (Common.currentUser!!.address!!)
                    txt_address.visibility = View.GONE
                    rdi_ship_to_this_address.setText("Current Location")
                    edt_address.isEnabled = false
                }
            }

            // other address
            rdi_other_address.setOnCheckedChangeListener { compoundButton, b ->
                if (b)
                {
//                    edt_address.setText ("")
//                    edt_address.setHint(" ")
//                    txt_address.visibility = View.GONE
                    edt_address.setText (Common.currentUser!!.workaddress)
                    txt_address.visibility = View.GONE
                    rdi_ship_to_this_address.setText("Current Location")
                    edt_address.isEnabled = false

                }
            }

            // current location radio button event listner
            rdi_ship_to_this_address.setOnCheckedChangeListener { compoundButton, b ->
                if (b)
                {
                   fusedLocationProviderClient!!.lastLocation
                       .addOnFailureListener { e ->
                           txt_address.visibility = View.GONE
                           Toast.makeText(context!!,""+e.message,
                           Toast.LENGTH_SHORT).show() }
                       .addOnCompleteListener {
                           task ->
                           val coordinates = StringBuilder()
                               .append(task.result!!.latitude)
                               .append(" / ")
                               .append(task.result!!.longitude)
                               .toString()

                           val singleAddress = Single.just(getAddressFromLatLng(task.result!!.latitude,
                               task.result!!.longitude))

                           val disposable = singleAddress.subscribeWith(object:DisposableSingleObserver<String>(){
                               override fun onSuccess(t: String) {
                                   edt_address.setText(coordinates)
                                   txt_address.visibility = View.VISIBLE
                                   txt_address.setText(t)
                                   //rdi_ship_to_this_address.setText(t)
                                   edt_address.isEnabled = false
                               }

                               override fun onError(e: Throwable) {
                                   edt_address.setText(coordinates)
                                  txt_address.visibility = View.VISIBLE
                                  txt_address.setText(e.message!!)

                               }
                           })
                       }
                }
            }

            builder.setView(view)

            val dialog = builder.create()

            //payment button listner
            btn_pay.setOnClickListener {

                if (rdi_cod.isChecked)
                    paymentCOD(edt_address.text.toString(), edt_comment.text.toString())
                else if (rdi_braintree.isChecked)
                {
                    address = edt_address.text.toString()
                    comment = edt_comment.text.toString()
                    if (!TextUtils.isEmpty(Common.currentToken))
                    {
                        val dropInRequest = DropInRequest().clientToken(Common.currentToken)
                        startActivityForResult(dropInRequest.getIntent(context), REQUEST_BRAINTREE_CODE)

                    }
                }
                dialog.dismiss()

            }

            //when click back button in checkout view
            txt_back.setOnClickListener {  dialog.dismiss()}

            dialog.show()



        }
    }

    private fun paymentCOD(address: String, comment: String) {
        compositeDisposable.add(cartDataSource!!.getAllCart(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ cartItemList ->

                // we have all cart items, get total amount of products.
                cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object: SingleObserver<Double>{
                        override fun onSuccess(totalPrice: Double) {

                            val finalPrice = totalPrice
                            val order = OrderModel()
                            order.userID = Common.currentUser!!.uid!!
                            order.userName = Common.currentUser!!.name!!
                            order.userPhone = Common.currentUser!!.phone!!
                            order.shippingAddress = address
                            order.comment = comment

                            if(currentLocation != null)
                            {
                                order.lat = currentLocation!!.latitude
                                order.lng = currentLocation!!.longitude
                            }

                            order.cartItemList = cartItemList
                            order.totalPayment = totalPrice
                            order.finalPayment = finalPrice
                            order.discount = 0
                            order.isCod = true
                            order.transactionId = "Cash On Delivery"

                            //save on firebase order informations
                            syncLocalTimeWithServerTime(order)
                        }

                        override fun onSubscribe(d: Disposable) {

                        }

                        override fun onError(e: Throwable) {
                            Toast.makeText(context!!, ""+e.message, Toast.LENGTH_SHORT).show()
                        }

                    })

            },{ throwable -> Toast.makeText(context!!, ""+throwable.message, Toast.LENGTH_SHORT).show()}))
    }

    private fun writeOrderToFirebase(order: OrderModel) {
        FirebaseDatabase.getInstance()
            .getReference(Common.ORDER_REF)
            .child(Common.createOrderNumber())
            .setValue(order)
            .addOnFailureListener { e -> Toast.makeText(context!!, ""+e.message, Toast.LENGTH_SHORT).show() }
            .addOnCompleteListener { task ->
                // clean cart after place an order
                if (task.isSuccessful)
                {
                    cartDataSource!!.cleanCart(Common.currentUser!!.uid!!)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object:  SingleObserver<Int>{
                            override fun onSuccess(t: Int) {

                                // after payments shows this alert dialog

                                val builder = AlertDialog.Builder(context!!)
                                val view = LayoutInflater.from(context).inflate(R.layout.layout_order_placed_message, null)

                                val txt_order_ok = view.findViewById<Button>(R.id.btn_after_order_message)

                                builder.setView(view)
                                val dialog = builder.create()
                                dialog.show()

                                txt_order_ok.setOnClickListener {
                                    dialog.dismiss()
                                }

                                val dataSend = HashMap<String, String>()
                                dataSend.put(Common.NOTI_TITLE, "Hey! you have a new order")
                                dataSend.put(Common.NOTI_CONTENT, "The order from "+Common.currentUser!!.name +" " +Common.currentUser!!.phone )


                                val sendData = FCMSendData(Common.getNewOrderTopic(), dataSend)

                                compositeDisposable.add(
                                    ifcmService.sendNofitication(sendData)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe()
                                )

                                clearcart()
                                txt_empty_cart!!.setText("Now you can see your order status in Order History page!")

                            }

                            override fun onSubscribe(d: Disposable) {

                            }

                            override fun onError(e: Throwable) {
                                Toast.makeText(context!!, ""+e.message, Toast.LENGTH_SHORT).show()
                            }

                        })
                }
            }
    }

    private fun getAddressFromLatLng(latitude: Double, longitude: Double): String {
        val geoCoder = Geocoder(context!!, Locale.getDefault())
        var result:String? = null
        try {
            val addressList = geoCoder.getFromLocation(latitude, longitude, 1)
            if (addressList != null && addressList.size > 0)
            {
                val address = addressList[0]
                val sb = StringBuilder(address.getAddressLine(0))
                result = sb.toString()
            }
            else
                result = "Address not found!"
            return result
        } catch (e:IOException)
        {
            return e.message!!
        }
    }

    private fun sumCart(){
        cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object: SingleObserver<Double>{
                override fun onSuccess(t: Double) {
                    txt_total_price!!.text = StringBuilder("RS ")
                        .append(t)
                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                  if (!e.message!!.contains("Query returned empty"))
                      Toast.makeText(context, ""+e.message!!, Toast.LENGTH_SHORT)
                }

            })
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
        EventBus.getDefault().postSticky(HideFABCart(true))

    }

    override fun onStop() {
        cartViewModel!!.onStop()
        compositeDisposable.clear()
        EventBus.getDefault().postSticky(HideFABCart(false))
        if (EventBus.getDefault().isRegistered(this))
            (EventBus.getDefault().unregister(this))
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()

    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onUpdateItemInCart(event:UpdateItemInCart){
        if (event.cartItem != null)
        {
            RecyclerViewState = recycler_cart!!.layoutManager!!.onSaveInstanceState()
            cartDataSource!!.updateCart(event.cartItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object: SingleObserver<Int>{
                    override fun onSuccess(t: Int) {
                        calculateTotalPrice()
                        recycler_cart!!.layoutManager!!.onRestoreInstanceState(RecyclerViewState)
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        //Toast.makeText(context, "[UPDATE CART]"+e.message, Toast.LENGTH_SHORT).show()
                        Toast.makeText(context, "Your cart is empty.", Toast.LENGTH_SHORT).show()
                    }

                })
        }
    }

    private fun calculateTotalPrice() {
        cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object: SingleObserver<Double>{
                override fun onSuccess(price: Double) {
                        txt_total_price!!.text = StringBuilder("LKR ")
                            .append(Common.formatPrice(price))
                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                   if (!e.message!!.contains("Query returned empty"))
                       Toast.makeText(context, "[SUM CART]"+e.message, Toast.LENGTH_SHORT).show()
                }

            })
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu!!.findItem(R.id.action_settings).setVisible(false) // hide setting action in cart fragment
        super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater!!.inflate(R.menu.cart_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item!!.itemId == R.id.action_clear_cart)
        {
            cartDataSource!!.cleanCart(Common.currentUser!!.uid!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object: SingleObserver<Int>{
                    override fun onSuccess(t: Int) {
                       // Toast.makeText(context, "Sucessfully clear cart", Toast.LENGTH_SHORT).show()
                        Snackbar.make(view!!, "Deleted all items in your Cart", Snackbar.LENGTH_LONG).show()
                        EventBus.getDefault().postSticky(CounterCartEvent(true))
                        EventBus.getDefault().postSticky(HideFABCart(true))
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context, ""+e.message, Toast.LENGTH_SHORT).show()
                    }

                })

            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_BRAINTREE_CODE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                val result = data!!.getParcelableExtra<DropInResult>(DropInResult.EXTRA_DROP_IN_RESULT)
                val nonce = result!!.paymentMethodNonce

                // calculate sum of the cart
                cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object: SingleObserver<Double>{
                        override fun onSuccess(totalPrice: Double) {
                            // get all item to create cart
                            compositeDisposable.add(
                                cartDataSource!!.getAllCart(Common.currentUser!!.uid!!)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({ cartItems: List<CartItem>? ->

                                        // submit payment
                                        val headers = HashMap<String, String>()
                                        headers.put("Authorization", Common.buildToken(Common.authorizeToken!!))
                                        compositeDisposable.add(cloudFunctions.submitPayment(
                                            headers,
                                            totalPrice,
                                            nonce!!.nonce)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe({ braintreeTransaction ->

                                                if (braintreeTransaction.success)
                                                {
                                                    //create order
                                                    val finalPrice = totalPrice
                                                    val order = OrderModel()
                                                    order.userID = Common.currentUser!!.uid!!
                                                    order.userName = Common.currentUser!!.name!!
                                                    order.userPhone = Common.currentUser!!.phone!!
                                                    order.shippingAddress = address
                                                    order.comment = comment

                                                    if(currentLocation != null)
                                                    {
                                                        order.lat = currentLocation!!.latitude
                                                        order.lng = currentLocation!!.longitude
                                                    }

                                                    order.cartItemList = cartItems
                                                    order.totalPayment = totalPrice
                                                    order.finalPayment = finalPrice
                                                    order.discount = 0
                                                    order.isCod = false
                                                    order.transactionId = braintreeTransaction.transaction!!.id

                                                    //save on firebase order informations
                                                    syncLocalTimeWithServerTime(order)

                                                }

                                            },
                                                { t: Throwable? ->
                                                    if (!t!!.message!!.contains("Query returned empty"))
                                                        Toast.makeText(context, "[SUM CART]"+t.message, Toast.LENGTH_SHORT)
                                                })
                                        )
                                    },
                                        { t: Throwable? ->
                                            Toast.makeText(context, ""+t!!.message, Toast.LENGTH_LONG).show()
                                        })
                            )
                        }

                        override fun onSubscribe(d: Disposable) {

                        }

                        override fun onError(e: Throwable) {

                                Toast.makeText(context, ""+e.message, Toast.LENGTH_SHORT).show()
                        }

                    })

            }
        }
    }

    private fun syncLocalTimeWithServerTime(order: OrderModel) {
        val offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
        offsetRef.addListenerForSingleValueEvent(object:ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                listener.onLoadTimeFailed(p0.message)
            }

            override fun onDataChange(p0: DataSnapshot) {
                val offset = p0.getValue(Long::class.java)
                val estimatedTimeInMs = System.currentTimeMillis()+offset!! // add missing offset to current time
                val sdf = SimpleDateFormat("MMM dd yyyy, HH:mm")
                val date = Date(estimatedTimeInMs)
                Log.d("Delivery Client App", ""+sdf.format(date))
                listener.onLoadTimeSuccess(order, estimatedTimeInMs)
            }

        })
    }

    override fun onLoadTimeSuccess(order: OrderModel, estimatedTimeMs: Long) {
        order.createDate = (estimatedTimeMs)
        order.orderStatus = 0
        writeOrderToFirebase(order)

    }

    override fun onLoadTimeFailed(message: String) {
      Toast.makeText(context!!, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }

    private fun clearcart(){

            cartDataSource!!.cleanCart(Common.currentUser!!.uid!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object: SingleObserver<Int>{
                    override fun onSuccess(t: Int) {

                        EventBus.getDefault().postSticky(CounterCartEvent(true))
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                      //  Toast.makeText(context, ""+e.message, Toast.LENGTH_SHORT).show()
                    }

                })
    }
}