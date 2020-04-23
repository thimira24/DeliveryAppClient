package com.developement.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.developement.app.Common.Common
import com.developement.app.Model.UserModel
import com.developement.app.Remote.ICloudFunctions
import com.developement.app.Remote.RetrofitCloudClient
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dmax.dialog.SpotsDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity() {

    private var placeSelected: Place? = null
    private var places_fragment: AutocompleteSupportFragment? = null
    private lateinit var placesClient: PlacesClient
    private val placeFields = Arrays.asList(
        Place.Field.ID,
        Place.Field.NAME,
        Place.Field.ADDRESS,
        Place.Field.LAT_LNG
    )

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener
    private lateinit var dialog: AlertDialog
    private val compositeDisposable = CompositeDisposable()
    private lateinit var cloudFunctions: ICloudFunctions

    private lateinit var userRef: DatabaseReference
    private var providers: List<AuthUI.IdpConfig>? = null

    companion object {
        private val APP_REQUEST_CODE = 7171 // any number
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(listener)
    }

    override fun onStop() {
        if (listener != null)
            firebaseAuth.removeAuthStateListener(listener)
        compositeDisposable.clear()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        init()
    }

    private fun init() {

        Places.initialize(this, getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)

        providers = Arrays.asList<AuthUI.IdpConfig>(AuthUI.IdpConfig.PhoneBuilder().build())
        userRef = FirebaseDatabase.getInstance().getReference(Common.USER_REFERENCE)
        firebaseAuth = FirebaseAuth.getInstance()
        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()
        cloudFunctions = RetrofitCloudClient.getInstance().create(ICloudFunctions::class.java)
        listener = FirebaseAuth.AuthStateListener { firebaseAuth ->

            Dexter.withActivity(this@MainActivity)
                .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                        val user = firebaseAuth.currentUser
                        if (user != null) {

                            checkUserFromFirebase(user!!)

                        } else {

                            phoneLogin()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permission: PermissionRequest?,
                        token: PermissionToken?
                    ) {

                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                        Toast.makeText(
                            this@MainActivity,
                            "You should enable location to access the application",
                            Toast.LENGTH_SHORT
                        ).show()

                    }

                }).check()


        }
    }

    private fun checkUserFromFirebase(user: FirebaseUser) {
        dialog.show()
        userRef!!.child(user!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    Toast.makeText(this@MainActivity, "" + p0!!.message, Toast.LENGTH_LONG).show()
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists()) {

                        FirebaseAuth.getInstance().currentUser!!
                            .getIdToken(true)
                            .addOnFailureListener { t ->
                                Toast.makeText(
                                    this@MainActivity,
                                    "" + t.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnCompleteListener {
                                Common.authorizeToken = it.result!!.token

                                val headers = HashMap<String, String>()
                                headers.put(
                                    "Authorization",
                                    Common.buildToken(Common.authorizeToken!!)
                                )

                                compositeDisposable.add(
                                    cloudFunctions!!.getToken(headers)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe({ braintreeToken ->

                                            dialog.dismiss()
                                            val userModel = p0.getValue(UserModel::class.java)
                                            goToHomeActivity(userModel, braintreeToken.token)

                                        }, { throwable ->
                                            dialog.dismiss()
                                            Toast.makeText(
                                                this@MainActivity,
                                                "" + throwable.message,
                                                Toast.LENGTH_LONG
                                            ).show()

                                        })
                                )
                            }

                    } else {
                        dialog!!.dismiss()
                        showRegisterDialog(user!!)
                    }

                }

            })

    }

    private fun showRegisterDialog(user: FirebaseUser) {


        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        // builder.setTitle("Create Account")
        // builder.setMessage("It will take less than a minute")

        val itemView = LayoutInflater.from(this@MainActivity)
            .inflate(R.layout.layout_register, null)

        val edt_name = itemView.findViewById<EditText>(R.id.edt_name)
        val edt_phone = itemView.findViewById<EditText>(R.id.edt_phone)
        val edt_hometown = itemView.findViewById<EditText>(R.id.edt_hometown)
        val edt_email = itemView.findViewById<EditText>(R.id.edt_email)
        val edt_nic = itemView.findViewById<EditText>(R.id.edt_nic)
        val lable_mumber = itemView.findViewById<TextView>(R.id.lable_number)

        // sign up button, work address and text view dissmiss
        val edt_work_address = itemView.findViewById<EditText>(R.id.edt_work_address)
        val btn_signup = itemView.findViewById<Button>(R.id.btn_signup)
        val txt_dismiss = itemView.findViewById<TextView>(R.id.txt_dismiss)
        val edt_address = itemView.findViewById<EditText>(R.id.txt_address_detail)

        // click activity for sign up button
        btn_signup.setOnClickListener {

            if (placeSelected != null) {


                if (TextUtils.isDigitsOnly(edt_name.text.toString())) {
                    Toast.makeText(this@MainActivity, "Enter your name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener

                } else if (TextUtils.isDigitsOnly(edt_hometown.text.toString())) {
                    Toast.makeText(this@MainActivity, "Hometown", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener

                } else if (TextUtils.isDigitsOnly(edt_email.text.toString())) {
                    Toast.makeText(this@MainActivity, "Email", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener

                } else if (TextUtils.isDigitsOnly(edt_email.text.toString())) {
                    Toast.makeText(this@MainActivity, "NIC", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener

                } else if (TextUtils.isDigitsOnly(edt_work_address.text.toString())) {
                    Toast.makeText(this@MainActivity, "Work Address", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }


                val userModel = UserModel()
                userModel.uid = user!!.uid
                userModel.name = edt_name.text.toString()
                userModel.address = edt_address.text.toString()
                userModel.workaddress = edt_work_address.text.toString()
                userModel.phone = edt_phone.text.toString()
                userModel.hometown = edt_hometown.text.toString()
                userModel.email = edt_email.text.toString()
                userModel.nic = edt_nic.text.toString()
                userModel.lat = placeSelected!!.latLng!!.latitude
                userModel.lng = placeSelected!!.latLng!!.longitude

                userRef!!.child(user!!.uid)
                    .setValue(userModel)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            FirebaseAuth.getInstance().currentUser!!
                                .getIdToken(true)
                                .addOnFailureListener { t ->
                                    Toast.makeText(
                                        this@MainActivity,
                                        "" + t.message,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnCompleteListener {
                                    Common.authorizeToken = it!!.result!!.token

                                    val headers = HashMap<String, String>()
                                    headers.put(
                                        "Authorization",
                                        Common.buildToken(Common.authorizeToken!!)
                                    )

                                    compositeDisposable.add(
                                        cloudFunctions.getToken(headers)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe({ braintreeToken ->


                                                goToHomeActivity(userModel, braintreeToken.token)
                                            }, { t: Throwable? ->


                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "" + t!!.message,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            })
                                    )
                                }
                        }
                    }
            } else {
                Toast.makeText(this@MainActivity, "Please select address", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // click activity for dissmiss textview
        txt_dismiss.setOnClickListener {
            System.exit(-1)
        }

        //lable_mumber.setText(user!!.phoneNumber)
        lable_mumber!!.text = StringBuilder("Verified your number ").append(user!!.phoneNumber)

        places_fragment =
            supportFragmentManager.findFragmentById(R.id.places_autocomplete_fragment) as AutocompleteSupportFragment
        places_fragment!!.setPlaceFields(placeFields)
        places_fragment!!.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(p0: Place) {
                placeSelected = p0
                //edt_address.text = placeSelected!!.address
                edt_address.setText(placeSelected!!.address)
            }

            override fun onError(p0: Status) {
                Toast.makeText(this@MainActivity, "" + p0.statusMessage, Toast.LENGTH_SHORT).show()
            }

        })

        // set mobile number
        edt_phone.setText(user!!.phoneNumber)
        builder.setView(itemView)

        // important show dialog
        val dialog = builder.create()
        dialog.setOnDismissListener{
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.remove(places_fragment!!)
            fragmentTransaction.commit()
        }
        dialog.show()
    }

    private fun goToHomeActivity(userModel: UserModel?, token: String?) {

        // update token
        FirebaseInstanceId.getInstance()
            .instanceId
            .addOnFailureListener { e ->
                Toast.makeText(this@MainActivity, "" + e.message, Toast.LENGTH_SHORT).show()
                Common.currentUser = userModel!!
                Common.currentToken = token!!
                startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                finish()
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    Common.currentUser = userModel!!
                    Common.currentToken = token!!
                    Common.updateToken(this@MainActivity, task.result!!.token)
                    startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                    finish()
                }
            }
    }

    private fun phoneLogin() {


        startActivityForResult(
            AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(
                providers!!
            ).build(), APP_REQUEST_CODE
        )

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser

            } else {
                Toast.makeText(this, "Failed login", Toast.LENGTH_SHORT).show()

            }
        }
    }


}
