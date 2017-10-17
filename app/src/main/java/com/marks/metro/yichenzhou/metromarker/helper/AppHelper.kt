package com.marks.metro.yichenzhou.metromarker.helper

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.net.ConnectivityManager
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.ImageView
import com.koushikdutta.ion.Ion
import com.marks.metro.yichenzhou.metromarker.model.Landmark
import com.marks.metro.yichenzhou.metromarker.model.MetroStation
import com.marks.metro.yichenzhou.metromarker.model.Token
import io.realm.Case
import io.realm.Realm
import java.nio.charset.Charset

/**
 * Created by yichenzhou on 9/29/17.
 */

object AppHelper {
    val TAG = "AppHelper"
    val GOOGLE_PLACES_KEY = "AIzaSyAGQWfAqWM8pzYtjbHIN_hhNhcE4BzS2UU"
    val GOOGLE_PLACES_SEARCH_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"

    val YELP_GRANT_TYPE = "OAuth2"
    val YELP_CLIENT_ID = "T4BbE1hqE-GAFC8HZvpqDQ"
    val YELP_CLIENT_SECRET = "99TMJOdPSOMv26oelKEQowXPAnRZ7Jt3crrPuAGkniaizVkzDiqSiysGXCgbiXgW"
    val YELP_TOKEN_URL = "https://api.yelp.com/oauth2/token"
    val YELP_SEARCH_URL = "https://api.yelp.com/v3/businesses/search"

    val WMATA_API_URL = "https://api.wmata.com/Rail.svc/json/jStations"
    val WMATA_API_KEY = "6f41a1603c704b57ba2e1e5741f79450"

    val LOCATION_PERMISSION_REQUEST_CODE: Int = 777
    val LOCATION_DEFAULT_CODE: String = android.Manifest.permission.ACCESS_FINE_LOCATION

    var placeNameList = ArrayList<String>()
    lateinit var placeAPIListener: GooglePlacesAPICompletionListener
    interface GooglePlacesAPICompletionListener {
        fun dataFetched()
        fun dataNotFetched()
    }

    var landmarkList = ArrayList<Landmark>()
    lateinit var yelpAPIListener: YelpAPICompletionListener
    interface YelpAPICompletionListener {
        fun yelpDataFetched()
        fun yelpDataNotFetched()
    }

    // Extension for AssetManager
    private fun AssetManager.fileAsString(fileName: String): String {
        return open(fileName).use {
            it.readBytes().toString(Charset.defaultCharset())
        }
    }

    // Load local csv file into Metro Station Realm Database
    fun loadStationsData(fileName: String, context: Context): MetroStation? {
        try {
            val realm = Realm.getDefaultInstance()
            val csvStr = context.assets.fileAsString(fileName)
            val lines = csvStr.split(";\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (line in lines) {
                val units = line.split(",")
                //Make sure data format is correct
                if (units.count() != 7) {
                    throw  Exception("Invalid data format")
                }
                realm.executeTransaction {
                    val station = realm.createObject(MetroStation::class.java)
                    station.setValues(units)
                }
            }
        } catch (e: Exception) {
            AppHelper.metroDatabaseRecover(context)
            Log.e(TAG, e.message)
        }
        return  null
    }

    // Search typed metro from TextField
    // Return searched metro
    fun searchTextMetro(content: String): List<MetroStation> {
        val realm = Realm.getDefaultInstance()
        return realm.where(MetroStation::class.java).contains("name", content, Case.INSENSITIVE).findAll()
    }


    // Search nearby metro stations based on device's last known location
    // Return or callback will give nearby metro stations
    fun searchNearbyMerto(location: String, context: Context) {
        Ion.with(context).load(GOOGLE_PLACES_SEARCH_URL)
                .addQuery("location", location)
                .addQuery("radius", "1000")
                .addQuery("types", "subway_station")
                .addQuery("key", GOOGLE_PLACES_KEY)
                .asJsonObject()
                .setCallback { e, result ->
                    if (e != null) {
                        this.placeAPIListener.dataNotFetched()
                        Log.e(TAG, "Google Place Request Error: ${e.message}")
                    }

                    result?.let {
                        if (result["status"].asString != "OK") {
                            this.placeAPIListener.dataNotFetched()
                            Log.d(TAG, "Failed to fetch data: ${result["status"].asString}")
                        }
                        this.placeNameList.removeAll { true }
                        val rootDataArr = result["results"].asJsonArray
                        for (data in rootDataArr) {
                            val indexDataArr = data.asJsonObject
                            var name = indexDataArr["name"].toString()
                            name = strTrim(name)
                            val placeID = indexDataArr["place_id"].toString()
                            this.placeNameList.add(name)
                        }
                        this.placeAPIListener.dataFetched()
                    }
                }
    }

    // Ask Package Manager is the permission granted
    fun checkPermissionStatus(serviceDefaultCode: String, context: Context): Boolean {
        val permissionStatus = ContextCompat.checkSelfPermission(context, serviceDefaultCode)
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            return true
        }

        return false
    }
    // Trim string
    private fun strTrim(content: String): String {
        var copy = content.removeSurrounding("\"")
        val dataArr = copy.split(" ")
        copy = ""
        dataArr
                .filter { it != "Station" }
                .forEach{ copy += (it + " ") }
        return copy.trim()
    }
    // Check yelp token if is available
    fun yelpTokenChecker(context: Context) {
        val realm = Realm.getDefaultInstance()
        val objects = realm.where(Token::class.java).findAll()
        if (objects.count() == 0) {
            this.yelpTokenFetcher(context)
        } else {
            Log.d(TAG, "Yelp API Token Already Fetched")
        }
    }
    // Yelp token fetcher
    private fun yelpTokenFetcher(context: Context) {
        Ion.with(context)
                .load(YELP_TOKEN_URL)
                .setBodyParameter("grant_type", YELP_GRANT_TYPE)
                .setBodyParameter("client_id", YELP_CLIENT_ID)
                .setBodyParameter("client_secret", YELP_CLIENT_SECRET)
                .asJsonObject()
                .setCallback { e, result ->
                    if (e != null) {
                        this.yelpAPIListener.yelpDataNotFetched()
                        Log.e(TAG, "Yelp API Token Request Error ${e.message}")
                        return@setCallback
                    }

                    result?.let {
                        val dataObject = result.asJsonObject
                        val tokenStr = dataObject["access_token"].toString().removeSurrounding("\"")
                        try {
                            val realm = Realm.getDefaultInstance()
                            realm.executeTransaction {
                                val tokenObject = realm.createObject(Token::class.java)
                                tokenObject.yelpTokenStr = tokenStr
                            }
                            this.yelpAPIListener.yelpDataFetched()
                        } catch (e: Exception) {
                            this.yelpAPIListener.yelpDataNotFetched()
                            Log.e(TAG, e.message)
                        }
                    }
                }
    }
    // Yelp data fetcher
    fun yelpLandmarkFetcher(latitude: Double, longitude: Double, context: Context) {
        // Get Yelp API Token From Database
        val realm = Realm.getDefaultInstance()
        val objects = realm.where(Token::class.java).findAll()
        if (objects.count() == 0) {
            Log.e(TAG, "No valid token data")
            return
        }
        val token = objects.first().yelpTokenStr
        if (token == null) {
            Log.e(TAG, "Toke data is null")
            return
        }
        Ion.with(context)
                .load(YELP_SEARCH_URL)
                .setHeader("Authorization", "Bearer $token")
                .addQuery("latitude", latitude.toString())
                .addQuery("longitude", longitude.toString())
                .asJsonObject()
                .setCallback { e, result ->
                    if (e != null) {
                        this.yelpAPIListener.yelpDataNotFetched()
                        Log.e(TAG, "Yelp API Token Request Error ${e.message}")
                        return@setCallback
                    }
                    result?.let {
                        this.landmarkList.removeAll { true }
                        val rootDataArr = result["businesses"].asJsonArray
                        for (data in rootDataArr) {
                            var landmark = Landmark()
                            landmark.parseData(data)
                            this.landmarkList.add(landmark)
                        }
                        this.yelpAPIListener.yelpDataFetched()
                    }
                }
    }

    // Yelp data image fetcher
    fun yelpImageFetcher(url: String, imageView: ImageView) {
        Ion.with(imageView).load(url).setCallback { e, _ ->
            if (e != null) {
                Log.e(TAG, "${e.localizedMessage}")
            }
        }
    }

    // Device Network status checker
    fun networkStatusChecker(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        val isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting

        val builder = AlertDialog.Builder(context)
        if (!isConnected) {
            builder.setMessage("Bummer! Network is not available, please check your network status.")
            builder.setCancelable(true)
            builder.setPositiveButton("OK", DialogInterface.OnClickListener {
                dialogInterface, _ ->
                dialogInterface.cancel()
                val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                context.startActivity(intent)
            })
            val alert = builder.create()
            alert.show()
        }
    }

    // Reload Metro Station Database by Requesting from WMATA API
    private fun metroDatabaseRecover(context: Context) {
        Realm.init(context)
        val realm = Realm.getDefaultInstance()
        realm.where(MetroStation::class.java).findAll().deleteAllFromRealm()
        Ion.with(context).load(WMATA_API_URL)
                .setHeader("api_key", WMATA_API_KEY)
                .asJsonObject()
                .setCallback {
                    e, result ->
                    if (e != null) {
                        Log.e(TAG, "${e.message}")
                    }

                    result?.let {
                        val rootDataArr = result["Stations"].asJsonArray
                        for (data in rootDataArr) {
                            val dataArr = data.asJsonObject
                            realm.executeTransaction {
                                val metro = realm.createObject(MetroStation::class.java)
                                metro.name = dataArr["Name"].toString().removeSurrounding("\"")
                                metro.lang = dataArr["Lat"].asDouble.toString()
                                metro.long = dataArr["Lon"].asDouble.toString()
                                val address = dataArr["Address"].asJsonObject
                                metro.street = address["Street"].asString
                                metro.city = address["City"].asString
                                metro.state = address["State"].asString
                                metro.zip = address["Zip"].asString
                            }
                        }
                    }
                }
    }
}