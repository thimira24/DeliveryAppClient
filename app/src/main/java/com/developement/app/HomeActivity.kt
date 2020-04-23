package com.developement.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.developement.app.Common.Common
import com.developement.app.Database.CartDataSource
import com.developement.app.Database.CartDatabase
import com.developement.app.Database.LocalClassDataSource
import com.developement.app.EventBus.*
import com.developement.app.Model.CategoryModel
import com.developement.app.Model.FoodModel
import com.developement.app.Model.UserModel
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.app_bar_home.*
import kotlinx.android.synthetic.main.layout_register.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.collections.HashMap

class HomeActivity : AppCompatActivity() {

    private var placeSelected: Place? = null
    private var places_fragment: AutocompleteSupportFragment? = null
    private lateinit var placesClient: PlacesClient
    private val placeFields = Arrays.asList(
        Place.Field.ID,
        Place.Field.NAME,
        Place.Field.ADDRESS,
        Place.Field.LAT_LNG
    )

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var cartDataSource: CartDataSource
    private lateinit var navController: NavController
    private var drawer: DrawerLayout? = null
    private var dialog: AlertDialog? = null

    private var menuItemClick = -1

    override fun onResume() {
        super.onResume()
        countCartItem()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()

        //check this
        cartDataSource = LocalClassDataSource(CartDatabase.getInstance(this).cartDAO())

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            navController.navigate(R.id.nav_cart)
        }
        drawer = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_menu, R.id.nav_food_detail,
                R.id.nav_cart
            ), drawer
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        var headerView = navView.getHeaderView(0)
        var txt_user = headerView.findViewById<TextView>(R.id.txt_user)
        var txt_phone_user = headerView.findViewById<TextView>(R.id.txt_user_phone)
        var txt_user_account = headerView.findViewById<ImageView>(R.id.edit_account_details)
        Common.setSpanString("Hello, ", Common.currentUser!!.name, txt_user)
        Common.setSpanString("", Common.currentUser!!.phone, txt_phone_user)

        navView.setNavigationItemSelectedListener(object :
            NavigationView.OnNavigationItemSelectedListener {
            override fun onNavigationItemSelected(p0: MenuItem): Boolean {

                p0.isChecked = true
                drawer!!.closeDrawers()
                if (p0.itemId == R.id.nav_sign_out) {
                    signOut()
                } else if (p0.itemId == R.id.nav_home) {
                    if (menuItemClick != p0.itemId)
                        navController.navigate(R.id.nav_home)
                } else if (p0.itemId == R.id.nav_cart) {
                    if (menuItemClick != p0.itemId)
                        navController.navigate(R.id.nav_cart)
                } else if (p0.itemId == R.id.nav_menu) {
                    if (menuItemClick != p0.itemId)
                        navController.navigate(R.id.nav_menu)
                } else if (p0.itemId == R.id.nav_view_order) {
                    if (menuItemClick != p0.itemId)
                        navController.navigate(R.id.nav_view_order)
                }
                menuItemClick = p0!!.itemId
                return true
            }
        })

       initPlacesClient()
        countCartItem()

        txt_user_account.setOnClickListener {
            showUpdateInfo()
        }
    }

    private fun initPlacesClient() {
            Places.initialize(this, getString(R.string.google_maps_key))
            placesClient = Places.createClient(this)
    }

    private fun showUpdateInfo() {

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        val itemView = LayoutInflater.from(this@HomeActivity)
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
        val txt_dismiss = itemView.findViewById<ImageView>(R.id.txt_dismiss)
        val edt_address = itemView.findViewById<EditText>(R.id.txt_address_detail)

        val txt_window_title = itemView.findViewById<TextView>(R.id.window_title)
        val txt_search_title = itemView.findViewById<TextView>(R.id.txt_search_message)
        txt_window_title.setText("Update Account")
        txt_search_title.setText("Search here to Change your Home address")
        btn_signup.setText("Update")

        // set mobile number
        edt_phone.setText(Common.currentUser!!.phone)
        edt_address.setText(Common.currentUser!!.address)
        edt_name.setText(Common.currentUser!!.name)
        edt_email.setText(Common.currentUser!!.email)
        edt_nic.setText(Common.currentUser!!.nic)
        edt_work_address.setText(Common.currentUser!!.workaddress)

        // click activity for sign up button
        btn_signup.setOnClickListener {

            if (placeSelected != null) {

                if (TextUtils.isDigitsOnly(edt_name.text.toString())) {
                    Toast.makeText(this@HomeActivity, "Enter your name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener

                } else if (TextUtils.isDigitsOnly(edt_hometown.text.toString())) {
                    Toast.makeText(this@HomeActivity, "Hometown", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener

                } else if (TextUtils.isDigitsOnly(edt_email.text.toString())) {
                    Toast.makeText(this@HomeActivity, "Email", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener

                } else if (TextUtils.isDigitsOnly(edt_email.text.toString())) {
                    Toast.makeText(this@HomeActivity, "NIC", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener

                } else if (TextUtils.isDigitsOnly(edt_work_address.text.toString())) {
                    Toast.makeText(this@HomeActivity, "Work Address", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val update_data = HashMap<String, Any>()
                update_data.put("name", edt_name.text.toString())
                update_data.put("email", edt_email.text.toString())
                update_data.put("address", edt_address.text.toString())
                update_data.put("workaddress", edt_work_address.text.toString())
                update_data.put("nic", edt_nic.text.toString())
                update_data.put("phone", edt_phone.text.toString())

                update_data.put("lat", placeSelected!!.latLng!!.latitude)
                update_data.put("lng", placeSelected!!.latLng!!.longitude)

                FirebaseDatabase.getInstance()
                    .getReference(Common.USER_REFERENCE)
                    .child(Common.currentUser!!.uid!!)
                    .updateChildren(update_data)
                    .addOnFailureListener {
                        Toast.makeText(this@HomeActivity, it.message, Toast.LENGTH_SHORT)
                    }
                    .addOnSuccessListener {
                        Snackbar.make(itemView, "Updated", Snackbar.LENGTH_LONG).show()

                    }
            }
            else
            {
                Toast.makeText(this@HomeActivity, "Please Update your details or close this window.", Toast.LENGTH_SHORT).show()

            }
        }

        lable_mumber!!.text = StringBuilder("Verified your number ").append(Common.currentUser!!.phone)

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
                Toast.makeText(this@HomeActivity, "" + p0.statusMessage, Toast.LENGTH_SHORT).show()
            }

        })

        builder.setView(itemView)
        // important show dialog
        val dialog = builder.create()
        dialog.setOnDismissListener {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.remove(places_fragment!!)
            fragmentTransaction.commit()
        }

        // click activity for dissmiss textview
        txt_dismiss.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun signOut() {

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_sign_out, null)

        val btn_sign_out = itemView.findViewById<View>(R.id.btn_sign_out) as Button
        val btn_cancelled = itemView.findViewById<View>(R.id.btn_cancel) as ImageView

        builder.setView(itemView)
        val shows = builder.create()

        btn_sign_out.setOnClickListener {
            Common.foodSelected = null
            Common.categorySelected = null
            Common.currentUser = null
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this@HomeActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        btn_cancelled.setOnClickListener {
            shows.dismiss()
        }

        shows.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()

    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCategorySelected(event: CategoryClick) {
        if (event.isSuccess) {
            //Toast.makeText(this,"you clicked "+event.category.name,Toast.LENGTH_SHORT).show()
            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_food_list)
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onFoodSelected(event: FoodItemClick) {
        if (event.isSuccess) {

            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_food_detail)
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCountCartEvent(event: CounterCartEvent) {
        if (event.isSuccess) {
            countCartItem()

        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onHideFabEvent(event: HideFABCart) {
        if (event.isHide) {
            fab.hide()

        } else
            fab.show()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onPopularFoodItemClick(event: PopularFoodItemClick) {

        if (event.popularCategoryModel != null) {
            dialog!!.show()

            FirebaseDatabase.getInstance()
                .getReference("Category")
                .child(event.popularCategoryModel!!.menu_id!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                        dialog!!.dismiss()
                        Toast.makeText(this@HomeActivity, "" + p0.message, Toast.LENGTH_SHORT)
                            .show()
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        if (p0.exists()) {
                            Common.categorySelected = p0.getValue(CategoryModel::class.java)
                            Common.categorySelected!!.menu_id = p0.key
                            // load products
                            FirebaseDatabase.getInstance()
                                .getReference("Category")
                                .child(event.popularCategoryModel!!.menu_id!!)
                                .child("foods")
                                .orderByChild("id")
                                .equalTo(event.popularCategoryModel.food_id)
                                .limitToLast(1)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onCancelled(p0: DatabaseError) {
                                        dialog!!.dismiss()
                                        Toast.makeText(
                                            this@HomeActivity,
                                            "" + p0.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    override fun onDataChange(p0: DataSnapshot) {
                                        if (p0.exists()) {
                                            for (foodSnapShot in p0.children) {
                                                Common.foodSelected =
                                                    foodSnapShot.getValue(FoodModel::class.java)
                                                Common.foodSelected!!.key = foodSnapShot.key
                                            }
                                            navController!!.navigate(R.id.nav_food_detail)

                                        } else {

                                            Toast.makeText(
                                                this@HomeActivity,
                                                "Not Available",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        dialog!!.dismiss()
                                    }

                                })
                        } else {
                            dialog!!.dismiss()
                            Toast.makeText(this@HomeActivity, "Not Available", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

                })
        }


    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onBestDealFoodItemClick(event: BestDealItemClick) {
        if (event.model != null) {

            dialog!!.show()

            FirebaseDatabase.getInstance()
                .getReference("Category")
                .child(event.model!!.menu_id!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                        dialog!!.dismiss()
                        Toast.makeText(this@HomeActivity, "" + p0.message, Toast.LENGTH_SHORT)
                            .show()
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        if (p0.exists()) {
                            Common.categorySelected = p0.getValue(CategoryModel::class.java)
                            Common.categorySelected!!.menu_id = p0.key
                            // load products
                            FirebaseDatabase.getInstance()
                                .getReference("Category")
                                .child(event.model!!.menu_id!!)
                                .child("foods")
                                .orderByChild("id")
                                .equalTo(event.model.food_id)
                                .limitToLast(1)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onCancelled(p0: DatabaseError) {
                                        dialog!!.dismiss()
                                        Toast.makeText(
                                            this@HomeActivity,
                                            "" + p0.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    override fun onDataChange(p0: DataSnapshot) {
                                        if (p0.exists()) {
                                            for (foodSnapShot in p0.children) {
                                                Common.foodSelected =
                                                    foodSnapShot.getValue(FoodModel::class.java)
                                                Common.foodSelected!!.key = foodSnapShot.key
                                            }
                                            navController!!.navigate(R.id.nav_food_detail)

                                        } else {

                                            Toast.makeText(
                                                this@HomeActivity,
                                                "Not Available",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        dialog!!.dismiss()
                                    }

                                })
                        } else {
                            dialog!!.dismiss()
                            Toast.makeText(this@HomeActivity, "Not Available", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

                })


        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onMenuItemBack(event: MenuItemBack) {
        menuItemClick = -1
        if (supportFragmentManager.backStackEntryCount > 0)
            supportFragmentManager.popBackStack()
    }

    private fun countCartItem() {
        cartDataSource.countItemInCart(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Int> {
                override fun onSuccess(t: Int) {
                    fab.count = t
                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    if (!e.message!!.contains("Query returned empty"))
                        Toast.makeText(
                            this@HomeActivity,
                            "[COUNT CART]" + e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    else {
                        fab.count = 0
                    }
                }

            })
    }

}
