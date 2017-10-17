package com.marks.metro.yichenzhou.metromarker.model

import io.realm.RealmObject

/**
 * Created by yichenzhou on 9/20/17.
 */

open class Favourite: RealmObject() {
    open var id: String? = null
    open var name: String? = null
    open var imageURL: String? = null
    open var isClose = false
    open var reviewURL: String? = null
    open var reviewCount: Int = 0
    open var category: String? = null
    open var rating: Double = 0.0

    open var latitude: Double = 0.0
    open var longitude: Double = 0.0
    open var price: String? = null
    open var address: String? = null
    open var distance: Double = 0.0
}