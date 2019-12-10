package com.developement.app.Database

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface CartDAO {
    @Query("SELECT * FROM Cart WHERE uid =:uid")
    fun getAllCart(uid: String): Flowable<List<CartItem>>

    @Query("SELECT SUM (foodQuantity) FROM Cart WHERE uid =:uid")
    fun countItemInCart(uid: String): Single<Int>

    @Query("SELECT SUM ((foodPrice+foodExtraPrice)*foodQuantity)  FROM Cart WHERE uid =:uid")
    fun sumPrice(uid: String): Single<Double>

    @Query("SELECT * FROM Cart WHERE foodID=:foodID AND uid =:uid")
    fun getItemInCart(foodID: String, uid: String): Single<CartItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplaceAll(vararg cartItems: CartItem): Completable

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateCart(cart: CartItem): Single<Int>

    @Delete
    fun deleteCart(cart: CartItem): Single<Int>

    @Query("DELETE FROM Cart WHERE uid=:uid")
    fun cleanCart(uid: String): Single<Int>

    @Query("SELECT * FROM Cart WHERE foodID=:foodID AND uid =:uid AND foodAddon =:foodAddon")
    fun getItemWithAllOptions(
        uid: String,
        foodID: String,
        foodAddon: String
    ): Single<CartItem>
}