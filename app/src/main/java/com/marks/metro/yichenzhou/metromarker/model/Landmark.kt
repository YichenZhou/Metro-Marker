package com.marks.metro.yichenzhou.metromarker.model

import android.content.Intent
import com.google.gson.JsonElement

/**
 * Created by yichenzhou on 9/20/17.
 */

open class Landmark {
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

    fun parseData(data: JsonElement) {
        val indexDataArr = data.asJsonObject
        val dataID = indexDataArr["id"].toString().removeSurrounding("\"")
        val dataName = indexDataArr["name"].toString().removeSurrounding("\"")
        val imageURL = indexDataArr["image_url"].toString().removeSurrounding("\"")
        val _isClosed = indexDataArr["is_closed"].asBoolean
        val yelpURL = indexDataArr["url"].toString().removeSurrounding("\"")
        val reviewCount = indexDataArr["review_count"].asInt
        val categories = indexDataArr["categories"].asJsonArray
        var category: String? = null
        if (categories.count() > 0) {
            val cateObject = categories.first().asJsonObject
            category = cateObject["title"].toString().removeSurrounding("\"")
        }
        var rating = indexDataArr["rating"].asDouble
        val location = indexDataArr["coordinates"].asJsonObject
        val latitude: Double = location["latitude"].asDouble
        val longitude: Double = location["longitude"].asDouble
        val price = indexDataArr["price"]?.toString()?.removeSurrounding("\"")
        var address = ""
        val addresses = indexDataArr["location"].asJsonObject
        val displayAddress = addresses["display_address"].asJsonArray
        for (unit in displayAddress) {
            address += (unit.asString + " ")
        }
        val distance: Double = indexDataArr["distance"].asDouble

        this.id = dataID
        this.name = dataName
        this.imageURL = imageURL
        this.isClose = _isClosed
        this.reviewURL = yelpURL
        this.reviewCount = reviewCount
        this.category = category
        this.rating = rating
        this.latitude = latitude
        this.longitude = longitude
        this.price = price
        this.address = address
        this.distance = distance
    }

    fun parseIntentData(intent: Intent) {
        this.id = intent.getStringExtra("id")
        this.name = intent.getStringExtra("name")
        this.latitude = intent.getDoubleExtra("lang", 0.0)
        this.longitude = intent.getDoubleExtra("long", 0.0)
        this.address = intent.getStringExtra("address")
        this.category = intent.getStringExtra("category")
        this.rating = intent.getDoubleExtra("rating", 0.0)
        this.price = intent.getStringExtra("price")
        this.reviewURL = intent.getStringExtra("url")
        this.imageURL = intent.getStringExtra("imageURL")
        this.isClose = intent.getBooleanExtra("isClose", true)
        this.reviewCount = intent.getIntExtra("reviewCount", 0)
        this.distance = intent.getDoubleExtra("distance", 0.0)
    }
}