package com.developement.app.ui.fooddetail

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.andremion.counterfab.CounterFab
import com.bumptech.glide.Glide
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton
import com.developement.app.Common.Common
import com.developement.app.Database.CartDataSource
import com.developement.app.Database.CartDatabase
import com.developement.app.Database.CartItem
import com.developement.app.Database.LocalClassDataSource
import com.developement.app.EventBus.CounterCartEvent
import com.developement.app.EventBus.HideFABCart
import com.developement.app.EventBus.MenuItemBack
import com.developement.app.Model.CommentModel
import com.developement.app.Model.FoodModel
import com.developement.app.R
import com.developement.app.ui.comment.CommentFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.google.gson.Gson
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_food_detail.*
import org.greenrobot.eventbus.EventBus
import java.math.RoundingMode
import java.text.DecimalFormat

class FoodDetailFragment : Fragment(), TextWatcher {



    override fun afterTextChanged(p0: Editable?) {

    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

    }

    override fun onTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
        chip_group_addon!!.clearCheck()
        chip_group_addon!!.removeAllViews()
        for (addonModel in Common.foodSelected!!.addon!!)
        {
            if (addonModel.name!!.toLowerCase().contains(charSequence.toString().toLowerCase()))
            {
                val chip = layoutInflater.inflate(R.layout.layout_chip, null, false) as Chip
                chip.text = StringBuilder(addonModel.name!!).append("(+$").append(addonModel.price).append(")").toString()
                chip.setOnCheckedChangeListener{compoundButton, b ->
                    if (b){
                        if (Common.foodSelected!!.userSelectedAddon == null)
                            Common.foodSelected!!.userSelectedAddon = ArrayList()
                        Common.foodSelected!!.userSelectedAddon!!.add(addonModel)
                    }
                }
                chip_group_addon!!.addView(chip)
            }
        }

    }

    private val compositeDisposable = CompositeDisposable()
    private lateinit var cartDataSource:  CartDataSource

    private lateinit var foodDetailViewModel: FoodDetailViewModel
    private lateinit var addonBottomSheetDialog: BottomSheetDialog

    private var img_food: ImageView? = null
    private var btncart: Button? = null
    private var btnRating: TextView? = null
    private var food_name: TextView? = null
    private var food_descryption: TextView? = null
    private var food_price: TextView? = null
    private var number_button: ElegantNumberButton? = null
    private var ratingBar: RatingBar? = null
    private var btnShowComment: TextView? = null
    private var rdi_group_size: RadioGroup? = null
    private var img_add_on: ImageView? = null
    private var chip_group_user_selected_addon: ChipGroup? = null
    private var comment_count: TextView? = null
    // Addon Layout
    private var chip_group_addon: ChipGroup? = null
    private var edt_search_addon: EditText? = null


    private var waitingDialog: android.app.AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        EventBus.getDefault().postSticky(HideFABCart(true))

        foodDetailViewModel =
            ViewModelProviders.of(this).get(FoodDetailViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_food_detail, container, false)
        initViews(root)

        foodDetailViewModel.getmutableLiveDataFood().observe(this, Observer {
            displayInfo(it)
        })

        foodDetailViewModel.getmutableLiveDataComment().observe(this, Observer {
            submitRatingToFirebase(it)
        })
        return root
    }

    private fun submitRatingToFirebase(commentModel: CommentModel?) {
        waitingDialog!!.show()

        // submit commont ref
        FirebaseDatabase.getInstance().getReference(Common.COMMENT_REF)
            .child(Common.foodSelected!!.id!!)
            .push()
            .setValue(commentModel)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    addRatingToFood(commentModel!!.ratingValue.toDouble())
                }
                waitingDialog!!.dismiss()
            }
    }

    private fun addRatingToFood(ratingValue: Double) {
        FirebaseDatabase.getInstance()

            .getReference(Common.CATEGORY_REF) // select  category
            .child(Common.categorySelected!!.menu_id!!)//select menu in category
            .child("foods") // select foods array
            .child(Common.foodSelected!!.key!!) // select key
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    waitingDialog!!.dismiss()
                    Toast.makeText(context!!, "" + p0.message, Toast.LENGTH_SHORT).show()
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val foodModel = dataSnapshot.getValue(FoodModel::class.java)
                        foodModel!!.key = Common.foodSelected!!.key

                        //apply rating
                        val sumRating = foodModel.ratingValue!!.toDouble() + (ratingValue)
                        val ratingCount = foodModel.ratingCount + 1
                 

                        val updateData = HashMap<String, Any>()
                        updateData["ratingValue"] = sumRating
                        updateData["ratingCount"] = ratingCount

                        //update data in variable
                        foodModel.ratingCount = ratingCount
                        foodModel.ratingValue = sumRating

                        dataSnapshot.ref
                            .updateChildren(updateData)
                            .addOnCompleteListener { task ->
                                waitingDialog!!.dismiss()
                                if (task.isSuccessful) {
                                    Common.foodSelected = foodModel
                                    foodDetailViewModel!!.setFoodModel(foodModel)
                                    //Toast.makeText(context!!, "Thank you for your feedback!", Toast.LENGTH_SHORT)
                                 
                                    Snackbar.make(view!!, "Thank you for your feedback!", Snackbar.LENGTH_LONG).show()




                                }
                            }


                    } else
                        waitingDialog!!.dismiss()
                }

            })
    }

    private fun displayInfo(it: FoodModel?) {
        Glide.with(context!!).load(it!!.image).into(img_food!!)
        food_name!!.text = StringBuilder(it!!.name!!)
        food_descryption!!.text = StringBuilder(it!!.description!!)
        food_price!!.text = StringBuilder(it!!.price!!.toString())
        count_comment!!.text = StringBuilder(it!!.ratingCount!!.toString())

        ratingBar!!.rating = it!!.ratingValue.toFloat() / it!!.ratingCount

        //set Size
        for (sizeModel in it!!.size) {
            val radioButton = RadioButton(context)
            radioButton.setOnCheckedChangeListener { compoundButton, b ->
                if (b)
                    Common.foodSelected!!.userSelectedSize = sizeModel
                calculateTotalPrice()
            }
            val params = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.0f
            )
            radioButton.layoutParams = params
            radioButton.text = sizeModel.name
            radioButton.tag = sizeModel.price

            rdi_group_size!!.addView(radioButton)
        }
        // default first radio button select
        if (rdi_group_size!!.childCount > 0) {
            val radioButton = rdi_group_size!!.getChildAt(0) as RadioButton
            radioButton.isChecked = true
        }
    }

    private fun calculateTotalPrice() {
        var totalPrice = Common.foodSelected!!.price.toDouble()
        var displayPrice = 0.0

        //addon
        if (Common.foodSelected!!.userSelectedAddon != null && Common.foodSelected!!.userSelectedAddon!!.size > 0) {
            for (addonModel in Common.foodSelected!!.userSelectedAddon!!)
                totalPrice += addonModel.price!!.toDouble()
        }

        //size
        totalPrice += Common.foodSelected!!.userSelectedSize!!.price!!.toDouble()
        displayPrice = totalPrice * number_button!!.number.toInt()
        displayPrice = Math.round(displayPrice * 100.0) / 100.0

        //food_price!!.text = StringBuilder("").append(Common.formatPrice(displayPrice)).toString()
        food_price!!.text = Common.foodSelected!!.price!!.toString()



    }

    private fun initViews(root: View?) {
        (activity as AppCompatActivity).supportActionBar!!.setTitle(Common.categorySelected!!.name)
        
        cartDataSource = LocalClassDataSource(CartDatabase.getInstance(context!!).cartDAO())

        addonBottomSheetDialog = BottomSheetDialog(context!!, R.style.DialogStyle)
        val layout_user_selected_addon = layoutInflater.inflate(R.layout.layout_addon_display, null)
        chip_group_addon =
            layout_user_selected_addon.findViewById(R.id.chip_group_addons) as ChipGroup
        edt_search_addon = layout_user_selected_addon.findViewById(R.id.edt_search) as EditText
        addonBottomSheetDialog.setContentView(layout_user_selected_addon)

        addonBottomSheetDialog.setOnDismissListener { dialogInterface ->
            displayUserSelectedAddon()
            calculateTotalPrice()
        }



        waitingDialog = SpotsDialog.Builder().setContext(context!!).setCancelable(false).build()

        // for testing
        btnRating = root!!.findViewById(R.id.btnRate) as TextView
        btncart = root!!.findViewById(R.id.add_to_cart) as Button
        //end


        img_food = root!!.findViewById(R.id.img_food) as ImageView
        //btnRating = root!!.findViewById(R.id.btn_rating) as FloatingActionButton
        food_name = root!!.findViewById(R.id.food_name) as TextView
        food_descryption = root!!.findViewById(R.id.food_descryption) as TextView
        food_price = root!!.findViewById(R.id.food_price) as TextView
        comment_count = root!!.findViewById(R.id.count_comment) as TextView
        number_button = root!!.findViewById(R.id.number_button) as ElegantNumberButton
        ratingBar = root!!.findViewById(R.id.rating_Bar) as RatingBar
        btnShowComment = root!!.findViewById(R.id.btnShowComment) as TextView
        rdi_group_size = root!!.findViewById(R.id.rdi_group_size) as RadioGroup
        img_add_on = root!!.findViewById(R.id.img_add_addon) as ImageView
        chip_group_user_selected_addon =
            root!!.findViewById(R.id.chip_group_user_selected_addon) as ChipGroup


        //event
        img_add_on!!.setOnClickListener {
            if (Common.foodSelected!!.addon != null)
            {
                displyAllAddon()
                addonBottomSheetDialog.show()
            }
        }

        btnRating!!.setOnClickListener {
            showDialogRating()
        }

        btnShowComment!!.setOnClickListener {
            val commentFragment = CommentFragment.getinstance()
            commentFragment.show(activity!!.supportFragmentManager, "CommentFragment")
        }

        btncart!!.setOnClickListener {
            val cartItem = CartItem()
            cartItem.uid = Common.currentUser!!.uid
            cartItem.userPhone = Common.currentUser!!.phone

            cartItem.foodID = Common.foodSelected!!.id!!
            cartItem.foodName = Common.foodSelected!!.name!!
            cartItem.foodImage = Common.foodSelected!!.image!!
            cartItem.foodPrice = Common.foodSelected!!.price!!.toDouble()
            cartItem.foodQuantity = number_button!!.number.toInt()
            //cartItem.foodExtraPrice = Common.calculateExtraPrice(Common.foodSelected!!.userSelectedAddon, Common.foodSelected!!.userSelectedSize)
            if (Common.foodSelected!!.userSelectedAddon !== null)
                cartItem.foodAddon = Gson().toJson(Common.foodSelected!!.userSelectedAddon)
            else
                cartItem.foodAddon = "Default"

            cartDataSource.getItemWithAllOptions(Common.currentUser!!.uid!!,
                cartItem.foodID,
                cartItem.foodAddon!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object: SingleObserver<CartItem> {
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
                                .subscribe(object: SingleObserver<Int> {
                                    override fun onSuccess(t: Int) {
                                        //Toast.makeText(context, "Cart updated", Toast.LENGTH_SHORT).show()
                                        Snackbar.make(view!!, "Cart updated", Snackbar.LENGTH_LONG).show()
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
                                    //Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show()

                                    Snackbar.make(view!!, "Added to the cart", Snackbar.LENGTH_LONG).show()

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
                                    //Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show()
                                    Snackbar.make(view!!, "Added to the cart", Snackbar.LENGTH_LONG).show()
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

    private fun displyAllAddon() {
        if (Common.foodSelected!!.addon!!.size > 0)
        {
            chip_group_addon!!.clearCheck()
            chip_group_addon!!.removeAllViews()

            edt_search_addon!!.addTextChangedListener(this)

            for (addonModel in Common.foodSelected!!.addon!!)
            {

                    val chip = layoutInflater.inflate(R.layout.layout_chip, null, false) as Chip
                    chip.text = StringBuilder(addonModel.name!!).append("(+Rs ").append(addonModel.price).append(")").toString()
                    chip.setOnCheckedChangeListener{compoundButton, b ->
                        if (b){
                            if (Common.foodSelected!!.userSelectedAddon == null)
                                Common.foodSelected!!.userSelectedAddon = ArrayList()
                            Common.foodSelected!!.userSelectedAddon!!.add(addonModel)
                        }
                    }
                    chip_group_addon!!.addView(chip)

            }
        }
    }

    private fun displayUserSelectedAddon() {
        if (Common.foodSelected!!.userSelectedAddon != null && Common.foodSelected!!.userSelectedAddon!!.size > 0) {
            chip_group_user_selected_addon!!.removeAllViews()
            for (addonModel in Common.foodSelected!!.userSelectedAddon!!) {
                val chip =
                    layoutInflater.inflate(R.layout.layout_chip_with_delete, null, false) as Chip
                chip.text = StringBuilder(addonModel!!.name!!).append("(+Rs ").append(addonModel.price)
                    .append(")").toString()
                chip.isClickable = false
                chip.setOnCloseIconClickListener { view ->
                    chip_group_user_selected_addon!!.removeView(view)
                    Common.foodSelected!!.userSelectedAddon!!.remove(addonModel)
                    calculateTotalPrice()
                }
                chip_group_user_selected_addon!!.addView(chip)
            }
        } else
            chip_group_user_selected_addon!!.removeAllViews()
    }

    private fun showDialogRating() {
        var builder = AlertDialog.Builder(context!!)
        builder.setTitle("Rate this product")
        builder.setMessage("Wee need your feedback!")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_rating_comment, null)

        val ratingBars = itemView.findViewById<RatingBar>(R.id.rating_bars)
        val edt_comment = itemView.findViewById<EditText>(R.id.edt_comment)

        builder.setView(itemView)
        builder.setNegativeButton("Cancel") { dialogInterface, i -> dialogInterface.dismiss() }
        builder.setPositiveButton("POST") { dialogInterface, i ->
            val commentModel = CommentModel()
            commentModel.name = Common.currentUser!!.name
            commentModel.uid = Common.currentUser!!.uid
            commentModel.comment = edt_comment.text.toString()
            commentModel.ratingValue = ratingBars.rating

            val serverTimestamp = HashMap<String, Any>()
            serverTimestamp["timeStamp"] = ServerValue.TIMESTAMP
            commentModel.commentTimeStamp = (serverTimestamp)

            foodDetailViewModel!!.setCommentModel(commentModel)


        }

        val dialog = builder.create()
        dialog.show()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }
}