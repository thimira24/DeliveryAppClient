package com.developement.app

import android.animation.ValueAnimator
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.developement.app.Common.Common
import com.developement.app.Model.ShippingOrderModel
import com.developement.app.Remote.IGoogleAPI
import com.developement.app.Remote.ISingleShippingOrderCallbackListner
import com.developement.app.Remote.RetrofitGoogleAPIClient

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import retrofit2.create
import java.lang.StringBuilder

class TrackingOrderActivity : AppCompatActivity(), OnMapReadyCallback, ValueEventListener,
    ISingleShippingOrderCallbackListner {

    private lateinit var mMap: GoogleMap

    private var shipperMarker: Marker? = null
    private var polylineOptions: PolylineOptions? = null
    private var blackPolylineOptions: PolylineOptions? = null
    private var blackPolyline: Polyline? = null
    private var grayPolyline: Polyline? = null
    private var redPolyline: Polyline? = null
    private var polylineList: List<LatLng> = ArrayList()

    private var isISingleShippingOrderCallbackListner: ISingleShippingOrderCallbackListner?=null
    private lateinit var iGoogleAPI: IGoogleAPI
    private val compositeDisposable = CompositeDisposable()

    private lateinit var shipperRef: DatabaseReference
    private var isInit = false

    //move marker
    private var handler: Handler? = null
    private var index = 0
    private var next: Int = 0
    private var v = 0f
    private var lat = 0.0
    private var lng = 0.0
    private var startPosition = LatLng(0.0, 0.0)
    private var endPosition = LatLng(0.0, 0.0)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking_order)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        initViews()
        subscribeShipperMove()
    }

    private fun initViews() {
        isISingleShippingOrderCallbackListner = this
        iGoogleAPI = RetrofitGoogleAPIClient.instance!!.create(IGoogleAPI::class.java)
    }

    private fun subscribeShipperMove() {
        shipperRef = FirebaseDatabase.getInstance().getReference(Common.SHIPPING_ORDER_REF)
            .child(Common.currentShippingOrder!!.key!!)
        shipperRef.addValueEventListener(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isCompassEnabled = true
        mMap!!.uiSettings.isZoomControlsEnabled = true
        try {
            val sucess = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this,
                    R.raw.uber_style2
                )
            )
            if (!sucess)
                Log.d("DeliveryApp", "Failed to load google map style")
        } catch (ex: Resources.NotFoundException) {
            Log.d("DeliveryApp", "Not found google map style")
        }
        checkorderfromfirebase()
    }

    private fun checkorderfromfirebase() {
        FirebaseDatabase.getInstance()
            .getReference(Common.SHIPPING_ORDER_REF)
            .child(Common.currentOrderSelected!!.orderNumber!!)
            .addListenerForSingleValueEvent(object:ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                    Toast.makeText(this@TrackingOrderActivity, p0.message, Toast.LENGTH_SHORT).show()
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists())
                    {
                        val shippingOrderModel = p0.getValue(ShippingOrderModel::class.java)
                        shippingOrderModel!!.key = p0.key

                        isISingleShippingOrderCallbackListner!!.onSingleShippingOrderSuccess(shippingOrderModel)
                    }
                    else
                    {
                        Toast.makeText(this@TrackingOrderActivity, "Order not found", Toast.LENGTH_SHORT).show()
                    }
                }

            })
    }

    private fun drawRoutes() {

        val locationOrder = LatLng(
            Common.currentShippingOrder!!.orderModel!!.lat,
            Common.currentShippingOrder!!.orderModel!!.lng
        )
        val locationShipper = LatLng(
            Common.currentShippingOrder!!.currentLat,
            Common.currentShippingOrder!!.currentLng
        )
        //addbox
        mMap.addMarker(
            MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.box))
                .title(Common.currentShippingOrder!!.orderModel!!.userName)
                .snippet(Common.currentShippingOrder!!.orderModel!!.shippingAddress)
                .position(locationOrder))
        //add shipper
        if (shipperMarker == null)
        {
            val height = 80
            val width = 80
            val bitmapDrawable = ContextCompat.getDrawable(this@TrackingOrderActivity, R.drawable.car) as BitmapDrawable
            val resized = Bitmap.createScaledBitmap(bitmapDrawable.bitmap, width, height, false)

            shipperMarker = mMap.addMarker(
                MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(resized))
                    .title(Common.currentShippingOrder!!.shipperName)
                    .snippet(Common.currentShippingOrder!!.shipperPhone)
                    .position(locationShipper)
            )
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 18.0f))
        }
        else
        {
            shipperMarker!!.position = locationShipper
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 18.0f))
        }


        //draw routes
        val to = StringBuilder().append(Common.currentShippingOrder!!.orderModel!!.lat)
            .append(",")
            .append(Common.currentShippingOrder!!.orderModel!!.lng)
            .toString()

        val from = StringBuilder().append(Common.currentShippingOrder!!.currentLat)
            .append(",")
            .append(Common.currentShippingOrder!!.currentLng)
            .toString()

        compositeDisposable.add(
            iGoogleAPI!!.getDirections(
                "driving",
                "less_driving",
                from, to,
                getString(R.string.google_maps_key))!!
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ s ->
                    try {
                        val jsonObject = JSONObject(s)
                        val jsonArray = jsonObject.getJSONArray("routes")
                        for (i in 0 until jsonArray.length()) {

                            val route = jsonArray.getJSONObject(i)
                            val poly = route.getJSONObject("overview_polyline")
                            val polyline = poly.getString("points")
                            polylineList = Common.decodePoly(polyline)
                        }

                        polylineOptions = PolylineOptions()
                        polylineOptions!!.color(Color.GREEN)
                        polylineOptions!!.width(12.0f)
                        polylineOptions!!.startCap(SquareCap())
                        polylineOptions!!.endCap(SquareCap())
                        polylineOptions!!.jointType(JointType.ROUND)
                        polylineOptions!!.addAll(polylineList)
                        redPolyline = mMap.addPolyline(polylineOptions)


                    } catch (e: Exception) {
                        Log.d("Debug", e.message)
                    }
                }, { throwable ->
                    Toast.makeText(
                        this@TrackingOrderActivity,
                        "" + throwable.message,
                        Toast.LENGTH_SHORT
                    ).show()
                })
        )

    }

    override fun onDestroy() {
        shipperRef.removeEventListener(this)
        isInit = false
        super.onDestroy()
    }

    override fun onCancelled(p0: DatabaseError) {

    }

    override fun onDataChange(dataSnapshot: DataSnapshot) {
        //save old location
        val from = StringBuilder()
            .append(Common.currentShippingOrder!!.currentLat)
            .append(",")
            .append(Common.currentShippingOrder!!.currentLng).toString()

        //update location
        Common.currentShippingOrder = dataSnapshot.getValue(ShippingOrderModel::class.java)
        Common.currentShippingOrder!!.key = dataSnapshot.key

        //save new location
        val to = StringBuilder()
            .append(Common.currentShippingOrder!!.currentLat)
            .append(",")
            .append(Common.currentShippingOrder!!.currentLng).toString()

        if (dataSnapshot.exists()) {
            if (isInit) moveMarkerAnimation(shipperMarker, from, to) else isInit = true
        }
    }

    private fun moveMarkerAnimation(shipperMarker: Marker?, from: String, to: String) {
        compositeDisposable.add(
            iGoogleAPI!!.getDirections(
                "driving",
                "less_driving",
                from,
                to,
                getString(R.string.google_maps_key)
            )
            !!.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ s ->
                    Log.d("DEBUG", s)
                    try {
                        val jsonObject = JSONObject(s)
                        val jsonArray = jsonObject.getJSONArray("routes")
                        for (i in 0 until jsonArray.length()) {
                            val route = jsonArray.getJSONObject(i)
                            val poly = route.getJSONObject("overview_polyline")
                            val polyline = poly.getString("points")
                            polylineList = Common.decodePoly(polyline)
                        }

                        polylineOptions = PolylineOptions()
                        polylineOptions!!.color(Color.GRAY)
                        polylineOptions!!.width(5.0f)
                        polylineOptions!!.startCap(SquareCap())
                        polylineOptions!!.endCap(SquareCap())
                        polylineOptions!!.jointType(JointType.ROUND)
                        polylineOptions!!.addAll(polylineList)
                        grayPolyline = mMap.addPolyline(polylineOptions)

                        blackPolylineOptions = PolylineOptions()
                        blackPolylineOptions!!.color(Color.BLACK)
                        blackPolylineOptions!!.width(5.0f)
                        blackPolylineOptions!!.startCap(SquareCap())
                        blackPolylineOptions!!.endCap(SquareCap())
                        blackPolylineOptions!!.jointType(JointType.ROUND)
                        blackPolylineOptions!!.addAll(polylineList)
                        blackPolyline = mMap.addPolyline(blackPolylineOptions)

                        //animator
                        val polylineAnimator = ValueAnimator.ofInt(0, 100)
                        polylineAnimator.setDuration(2000)
                        polylineAnimator.setInterpolator(LinearInterpolator())
                        polylineAnimator.addUpdateListener { valueAnimator ->
                            val points = grayPolyline!!.points
                            val precentValue =
                                Integer.parseInt(valueAnimator.animatedValue.toString())
                            val size = points.size
                            val newPoints = (size * (precentValue / 100.0f).toInt())
                            val p = points.subList(0, newPoints)
                            blackPolyline!!.points = p
                        }

                        polylineAnimator.start()

                        // car moving
                        index = -1
                        next = 1
                        val r = object : Runnable {
                            override fun run() {
                                if (index < polylineList.size - 1) {
                                    index++
                                    next = index + 1
                                    startPosition = polylineList[index]
                                    endPosition = polylineList[next]
                                }
                                val valueAnimator = ValueAnimator.ofInt(0, 1)
                                valueAnimator.setDuration(1500)
                                valueAnimator.setInterpolator(LinearInterpolator())
                                valueAnimator.addUpdateListener { valueAnimator ->
                                    v = valueAnimator.animatedFraction
                                    lat =
                                        v * endPosition!!.latitude + (1 - v) * startPosition!!.latitude
                                    lng =
                                        v * endPosition!!.longitude + (1 - v) * startPosition!!.longitude

                                    val newPos = LatLng(lat, lng)
                                    shipperMarker!!.position = newPos
                                    shipperMarker!!.setAnchor(0.5f, 0.5f)
                                    shipperMarker!!.rotation =
                                        Common.getBearing(startPosition!!, newPos)

                                    mMap.moveCamera(CameraUpdateFactory.newLatLng(shipperMarker.position))
                                }

                                valueAnimator.start()
                                if (index < polylineList.size - 2)
                                    handler!!.postDelayed(this, 1500)
                            }

                        }

                        handler = Handler()
                        handler!!.postDelayed(r, 1500)


                    } catch (e: Exception) {
                        Log.d("Debug", e.message)
                    }
                }, { throwable ->
                    Toast.makeText(
                        this@TrackingOrderActivity,
                        "" + throwable.message,
                        Toast.LENGTH_SHORT
                    ).show()
                })
        )
    }

    override fun onSingleShippingOrderSuccess(shippingOrderModel: ShippingOrderModel) {

        val locationOrder = LatLng(
            shippingOrderModel!!.orderModel!!.lat,
            shippingOrderModel!!.orderModel!!.lng
        )
        val locationShipper = LatLng(
            shippingOrderModel!!.currentLat,
            shippingOrderModel!!.currentLng
        )

        //addbox
        mMap.addMarker(
            MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.box))
                .title(shippingOrderModel!!.orderModel!!.userName)
                .snippet(shippingOrderModel!!.orderModel!!.shippingAddress)
                .position(locationOrder))

        //add shipper
        if (shipperMarker == null)
        {
            val height = 80
            val width = 80
            val bitmapDrawable = ContextCompat.getDrawable(this@TrackingOrderActivity, R.drawable.car) as BitmapDrawable
            val resized = Bitmap.createScaledBitmap(bitmapDrawable.bitmap, width, height, false)

            shipperMarker = mMap.addMarker(
                MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(resized))
                    .title(shippingOrderModel!!.shipperName)
                    .snippet(shippingOrderModel!!.shipperPhone)
                    .position(locationShipper)
            )
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 18.0f))
        }
        else
        {
            shipperMarker!!.position = locationShipper
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 18.0f))
        }

        //draw routes
        val to = StringBuilder().append(shippingOrderModel!!.orderModel!!.lat)
            .append(",")
            .append(shippingOrderModel!!.orderModel!!.lng)
            .toString()

        val from = StringBuilder().append(shippingOrderModel!!.currentLat)
            .append(",")
            .append(shippingOrderModel!!.currentLng)
            .toString()

        compositeDisposable.add(
            iGoogleAPI!!.getDirections(
                "driving",
                "less_driving",
                from, to,
                getString(R.string.google_maps_key))!!
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ s ->
                    try {
                        val jsonObject = JSONObject(s)
                        val jsonArray = jsonObject.getJSONArray("routes")
                        for (i in 0 until jsonArray.length()) {

                            val route = jsonArray.getJSONObject(i)
                            val poly = route.getJSONObject("overview_polyline")
                            val polyline = poly.getString("points")
                            polylineList = Common.decodePoly(polyline)
                        }

                        polylineOptions = PolylineOptions()
                        polylineOptions!!.color(Color.GREEN)
                        polylineOptions!!.width(12.0f)
                        polylineOptions!!.startCap(SquareCap())
                        polylineOptions!!.endCap(SquareCap())
                        polylineOptions!!.jointType(JointType.ROUND)
                        polylineOptions!!.addAll(polylineList)
                        redPolyline = mMap.addPolyline(polylineOptions)


                    } catch (e: Exception) {
                        Log.d("Debug", e.message)
                    }
                }, { throwable ->
                    Toast.makeText(
                        this@TrackingOrderActivity,
                        "" + throwable.message,
                        Toast.LENGTH_SHORT
                    ).show()
                })
        )

    }
}
