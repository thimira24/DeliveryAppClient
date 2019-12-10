package com.developement.app.Database

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

class LocalClassDataSource (private val cartDAO: CartDAO) : CartDataSource {
    override fun getItemWithAllOptions(
        uid: String,
        foodID: String,
        foodAddon: String
    ): Single<CartItem> {
        return cartDAO.getItemWithAllOptions(uid, foodID, foodAddon)
    }

    override fun getAllCart(uid: String): Flowable<List<CartItem>> {
        return cartDAO.getAllCart(uid)
    }

    override fun countItemInCart(uid: String): Single<Int> {
       return cartDAO.countItemInCart(uid)
    }

    override fun sumPrice(uid: String): Single<Double> {
        return cartDAO.sumPrice(uid)
    }

    override fun getItemInCart(foodID: String, uid: String): Single<CartItem> {
       return cartDAO.getItemInCart(foodID, uid)
    }

    override fun insertOrReplaceAll(vararg cartItems: CartItem): Completable {
       return cartDAO.insertOrReplaceAll(*cartItems)
    }

    override fun updateCart(cart: CartItem): Single<Int> {
        return cartDAO.updateCart(cart)
    }

    override fun deleteCart(cart: CartItem): Single<Int> {
        return cartDAO.deleteCart(cart)
    }

    override fun cleanCart(uid: String): Single<Int> {
        return cartDAO.cleanCart(uid)
    }
}