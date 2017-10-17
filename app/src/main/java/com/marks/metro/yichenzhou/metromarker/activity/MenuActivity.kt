package com.marks.metro.yichenzhou.metromarker.activity

import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.Menu
import android.support.v7.widget.Toolbar
import android.widget.ProgressBar
import ca.allanwang.kau.searchview.SearchItem
import ca.allanwang.kau.searchview.SearchView
import ca.allanwang.kau.searchview.bindSearchView
import ca.allanwang.kau.utils.bindView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.marks.metro.yichenzhou.metromarker.R
import com.marks.metro.yichenzhou.metromarker.helper.AppHelper
import com.marks.metro.yichenzhou.metromarker.helper.LocationDetector
import com.marks.metro.yichenzhou.metromarker.model.MetroStation
import io.realm.Realm
import kotlin.properties.Delegates
import kotlinx.android.synthetic.main.main_menu.*
import org.jetbrains.anko.activityUiThread
import org.jetbrains.anko.doAsync

class MenuActivity : AppCompatActivity(), LocationDetector.LocationListener, OnMapReadyCallback, AppHelper.GooglePlacesAPICompletionListener, GoogleMap.OnInfoWindowClickListener, AppHelper.YelpAPICompletionListener {
    private val TAG = "MenuActivity"
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var realm: Realm by Delegates.notNull()
    private var searchView: SearchView? = null
    private var metroList = ArrayList<MetroStation>()
    val toolbar: Toolbar by bindView(R.id.station_filter_toolbar)
    private lateinit var locationManager: LocationManager
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var locationDetector: LocationDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_menu)

        // Check Network Status
        AppHelper.networkStatusChecker(this)
        // Request Location Fetching Permission
        this.requestPermission()
        // Properties Initialization
        Realm.init(applicationContext)
        this.realm = Realm.getDefaultInstance()
        this.loadMetroData()
        this.locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        this.mapFragment = supportFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        this.showLoading(true)
        this.locationDetector = LocationDetector(this)
        this.locationDetector.locationListener = this
        AppHelper.placeAPIListener = this
        AppHelper.yelpAPIListener = this

        // Fetch current location and show on the mapView
        if (AppHelper.checkPermissionStatus(AppHelper.LOCATION_DEFAULT_CODE, this)) {
            this.locationDetector.detectLocation()
        }

        //Setup toolBar
        this.setSupportActionBar(toolbar)


        this.favorite_button.setOnClickListener {
            //favorite button listener
            this.loadFavoriteData()
        }
        
        this.nearest_button.setOnClickListener {
            AppHelper.networkStatusChecker(this)
            this.showLoading(true)
            this.fetchNearbyMetroStation()
        }

        this.clear_button.setOnClickListener{
            this.metroList.removeAll { true }
            this.locationDetector.detectLocation()
            this.mapFragment.getMapAsync(this)
        }

        // Fetch Yelp Token for later using
        AppHelper.yelpTokenChecker(this)
    }

    private fun loadMetroData() {
        val stationCount = this.realm.where(MetroStation::class.java).findAll().count()
        if (stationCount == 0) {
            AppHelper.loadStationsData("Stations.csv", applicationContext)
        }
    }
    private fun loadFavoriteData() {
        doAsync {
            activityUiThread {
                //TODO
                //load the favorite list data and jump to the List UI
                val intent = Intent(this@MenuActivity, FavoriteActivity::class.java)

                startActivity(intent)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        if(show) {
            menu_progress_bar.visibility = ProgressBar.VISIBLE
        }
        else {
            menu_progress_bar.visibility = ProgressBar.INVISIBLE
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        if (map !is GoogleMap) {
            Log.e(TAG, "Invalid type for map")
            return
        }
        val builder = LatLngBounds.Builder()
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        val padding = (width * 0.12).toInt()
        if (this.metroList.count() == 0) {
            // Show my current location when user launch the app
            val location = LatLng(this.latitude, this.longitude)
            builder.include(location)
            map.clear()
            map.addMarker(MarkerOptions().position(location).title("My Location"))
        } else {
            // Show nearby metro stations
            for (metro in metroList) {
                val location = LatLng(metro.lang.toDouble(), metro.long.toDouble())
                map.addMarker(MarkerOptions().position(location).title(metro.name).icon(this.markerIcon(BitmapDescriptorFactory.HUE_AZURE)))
                builder.include(location)
            }
            this.metroList.removeAll { true }
        }
        map.setMaxZoomPreference(15.0f)
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), width, height, padding))
        map.setOnInfoWindowClickListener(this)
    }

    override fun onInfoWindowClick(marker: Marker?) {
        if (marker == null) {
            Log.e(TAG, "marker object is null")
            return
        }

        if (marker.title == "My Location") {
            return
        }
        val intent = Intent(this, MetroDetailActivity::class.java)
        intent.putExtra("name", marker.title)
        intent.putExtra("lang", marker.position.latitude)
        intent.putExtra("long", marker.position.longitude)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AppHelper.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.first() != PackageManager.PERMISSION_GRANTED) {
                this.requestPermission()
            }
        }
        // Where to fetch location again after location permission granted
        this.locationDetector.detectLocation()
    }

    private fun markerIcon(color: Float): BitmapDescriptor {
        return BitmapDescriptorFactory.defaultMarker(color)
    }

    private fun requestPermission() {
        if (!AppHelper.checkPermissionStatus(AppHelper.LOCATION_DEFAULT_CODE, this)) {
            ActivityCompat.requestPermissions(this, arrayOf(AppHelper.LOCATION_DEFAULT_CODE), AppHelper.LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    override fun locationFound(location: Location) {
        this.latitude = location.latitude
        this.longitude = location.longitude
        mapFragment.getMapAsync(this@MenuActivity)
        this.showLoading(false)
    }

    override fun locationNotFound(reason: LocationDetector.FailureReason) {
        this.showLoading(false)
        when(reason) {
            LocationDetector.FailureReason.TIMEOUT -> Log.e(TAG, "Location Detection Time Out")
            LocationDetector.FailureReason.NO_PERMISSION -> Log.e(TAG, "No Permission to detect location")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menuInflater.inflate(R.menu.stationfilter, menu)
        if (menu == null) {
            Log.e(TAG, "Menu instance is null")
            return false
        }
        if (this.searchView == null) this.searchView = bindSearchView(menu, R.id.station_filter_search) {
            textCallback = { query, searchView ->
                searchView.findFocus()
                val stations = AppHelper.searchTextMetro(query).sortedBy { it.name }.map { SearchItem(it.name) }
                searchView.results = stations
            }

            searchCallback = {query, _ ->
                Log.d(TAG, "Query Content: $query")
                true
            }

            textDebounceInterval = 0
            noResultsFound = R.string.no_results
            shouldClearOnClose = false
            onItemClick = {position, key, content, searchView ->
                Log.d(TAG, "Query Positiont: $position")
                Log.d(TAG, "Query Key: $key")
                Log.d(TAG, "Query Content: $content")
                
                val results = AppHelper.searchTextMetro(key)
                if (results.count() == 0) {
                    Log.e(TAG, "No valid metro data")
                } else {
                    metroList.removeAll { true }
                    metroList.add(results.first())
                    mapFragment.getMapAsync(this@MenuActivity)
                }
                searchView.revealClose()
            }
        }
        return true
    }

    private fun fetchNearbyMetroStation() {
        if (this.latitude == 0.0 || this.longitude == 0.0) {
            Log.e(TAG, "Invalid latitude or longitude")
            return
        }
        val currentLocation = "${this.latitude}, ${this.longitude}"
        AppHelper.searchNearbyMerto(currentLocation, this)
    }

    override fun dataNotFetched() {
        this.showLoading(false)
    }

    override fun dataFetched() {
        val nameList = AppHelper.placeNameList
        if (nameList.count() == 0) {
            Log.e(TAG, "Invalid placeName list")
            return
        }
        this.metroList.removeAll { true }
        for (name in nameList) {
            val results = AppHelper.searchTextMetro(name)
            if (results.count() == 0) {
                Log.e(TAG, "No valid result")
                return
            }
            this.metroList.add(results.first())

        }
        if (this.metroList.count() == 0) {
            this.showLoading(false)
            Log.e(TAG, "No valid data in metroList")
            return
        }
        this.showLoading(false)
        this.mapFragment.getMapAsync(this@MenuActivity)
    }

    override fun yelpDataFetched() {
        Log.d(TAG, "YELP Token Fetched")
    }

    override fun yelpDataNotFetched() {
        Log.e(TAG, "YELP Token Not Fetched")
    }
}