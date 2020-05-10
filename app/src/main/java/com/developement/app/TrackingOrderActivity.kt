package com.developement.app

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.developement.app.Common.Common
import com.developement.app.Remote.IGoogleAPI
import com.developement.app.Remote.RetrofitGoogleAPIClient

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import retrofit2.create
import java.lang.StringBuilder

class TrackingOrderActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private var shipperMarker: Marker? = null
    private var polylineOptions: PolylineOptions? = null
    private var blackPolygonOptions: PolylineOptions? = null
    private var yellowPolyline: Polyline? = null
    private var polylineList: List<LatLng> = ArrayList()

    private lateinit var iGoogleAPI: IGoogleAPI
    private val compositeDisposable = CompositeDisposable()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking_order)

        iGoogleAPI = RetrofitGoogleAPIClient.instance!!.create(IGoogleAPI::class.java)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
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

        //add box
        mMap.addMarker(
            MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.box))
                .title(Common.currentShippingOrder!!.orderModel!!.userName)
                .snippet(Common.currentShippingOrder!!.orderModel!!.shippingAddress)
                .position(locationOrder)
        )

        //add SHipper
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

        // drawroutes
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
                getString(R.string.google_maps_key)
            )
            !!.subscribeOn(Schedulers.io())
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
                        polylineOptions!!.color(Color.BLACK)
                        polylineOptions!!.width(12.0f)
                        polylineOptions!!.startCap(SquareCap())
                        polylineOptions!!.endCap(SquareCap())
                        polylineOptions!!.jointType(JointType.ROUND)
                        polylineOptions!!.addAll(polylineList)
                        yellowPolyline = mMap.addPolyline(polylineOptions)


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
