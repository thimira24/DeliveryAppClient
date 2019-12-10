package com.developement.app.Database

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Cart", primaryKeys = ["uid", "foodID", "foodAddon"])
class CartItem {
    @NonNull
    @ColumnInfo(name = "foodID")
    var foodID: String = ""

    @ColumnInfo(name = "foodName")
    var foodName: String? = null

    @ColumnInfo(name = "foodImage")
    var foodImage: String? = null

    @ColumnInfo(name = "foodPrice")
    var foodPrice: Double = 0.0

    @ColumnInfo(name = "foodQuantity")
    var foodQuantity: Int = 0

    @NonNull
    @ColumnInfo(name = "foodAddon")
    var foodAddon: String? = ""

   /// @ColumnInfo(name = "foodSize")
   // var foodSize: String? = ""

    @ColumnInfo(name = "userPhone")
    var userPhone: String? = ""

    @ColumnInfo(name = "foodExtraPrice")
    var foodExtraPrice: Double = 0.0

    @NonNull
    @ColumnInfo(name = "uid")
    var uid: String? = ""

    //ctrl +o
    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is CartItem)
            return false
        val cartItem = other as CartItem?
        return cartItem!!.foodID == this.foodID &&
                cartItem.foodAddon == this.foodAddon
    }
}