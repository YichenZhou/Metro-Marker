package com.marks.metro.yichenzhou.metromarker.activity

import android.content.DialogInterface
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.ImageView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.marks.metro.yichenzhou.metromarker.R
import com.marks.metro.yichenzhou.metromarker.model.Favourite
import com.marks.metro.yichenzhou.metromarker.model.Landmark
import io.realm.Realm
import kotlinx.android.synthetic.main.landmark_detail.*
import kotlin.properties.Delegates

class LandMarkDetailActivity : AppCompatActivity(), OnMapReadyCallback {
    private val TAG = "LandmarkDetailActivity"
    private var landmark = Landmark()
    private var realm: Realm by Delegates.notNull()

    private lateinit var mapFragment: SupportMapFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.landmark_detail)

        // Properties Initialization
        Realm.init(applicationContext)
        this.landmark.parseIntentData(intent)
        this.mapFragment = supportFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        this.realm = Realm.getDefaultInstance()
        if (this.realm.where(Favourite::class.java).equalTo("id", this.landmark.id).findAll().count() != 0) {
            this.favoriteImageView.visibility = ImageView.VISIBLE
        } else {
            this.favoriteImageView.visibility = ImageView.INVISIBLE
        }

        //setup toolbar title
        this.landmark_filter_toolbar.title = this.landmark.name
        
        if (this.landmark.latitude != 0.0 && this.landmark.longitude != 0.0) {
            this.mapFragment.getMapAsync(this)
        }
        this.nameTextView.text = this.landmark.name
        this.ratingAndReviewTextView.text = "Rating ${landmark.rating}/5.0 || ${landmark.reviewCount} Reviews"
        this.categoryTextView.text = "${landmark.category} || price: ${landmark.price}"
        this.addressTextView.text = "${landmark.address}"

        this.addFavoriteButton.setOnClickListener {
            this.addOrRemoveFavorite()
        }

        this.shareButton.setOnClickListener {
            this.shareLandmark()
        }
    }
    
    override fun onMapReady(map: GoogleMap?) {
        if (map == null) {
            Log.e(TAG, "map is null")
            return
        }
        val builder = LatLngBounds.Builder()
        val location = LatLng(this.landmark.latitude, this.landmark.longitude)
        builder.include(location)
        map.addMarker(MarkerOptions().position(location).title(this.landmark.name).icon(this.markerIcon(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE)))
        map.setMaxZoomPreference(17.0f)
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        val padding = (width * 0.12).toInt()
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), width, height, padding))
    }
    // Set marker's color
    private fun markerIcon(color: Float): BitmapDescriptor {
        return BitmapDescriptorFactory.defaultMarker(color)
    }

    private fun addOrRemoveFavorite() {
        val results = this.realm.where(Favourite::class.java).equalTo("id", this.landmark.id).findAll()
        if (results.count() == 0) {
            this.showAlertView(false)
        } else {
            this.showAlertView(true)
        }
    }

    private fun showAlertView(isSaved: Boolean) {
        val builder = AlertDialog.Builder(this)
        if (isSaved) {
            builder.setMessage("Already saved this landmark, do you want to remove it?")

        } else {
            builder.setMessage("Do you want to save this landmark to your favorite list?")
        }

        builder.setCancelable(true)
        builder.setPositiveButton("Sure", DialogInterface.OnClickListener {
            dialogInterface, _ ->
            if (isSaved) {
                // Remove this landmark from favorite list
                this.realm.executeTransaction {
                    val favorites = this.realm.where(Favourite::class.java).equalTo("id", this.landmark.id).findAll()
                    val favorite = favorites.first()
                    favorite.deleteFromRealm()
                }
                this.favoriteImageView.visibility = ImageView.INVISIBLE
            } else {
                // Save this landmark to database
                this.realm.executeTransaction {
                    val favorite = this.realm.createObject(Favourite::class.java)
                    favorite.id = this.landmark.id
                    favorite.name = this.landmark.name
                    favorite.latitude = this.landmark.latitude
                    favorite.longitude = this.landmark.longitude
                    favorite.address = this.landmark.address
                    favorite.category = this.landmark.category
                    favorite.rating = this.landmark.rating
                    favorite.price = this.landmark.price
                    favorite.reviewURL = this.landmark.reviewURL
                    favorite.imageURL = this.landmark.imageURL
                    favorite.isClose = this.landmark.isClose
                    favorite.reviewCount = this.landmark.reviewCount
                    favorite.distance = this.landmark.distance
                }
                this.favoriteImageView.visibility = ImageView.VISIBLE
            }
            dialogInterface.cancel()
        })
        builder.setNegativeButton("No", DialogInterface.OnClickListener {
            dialogInterface, _ ->
            dialogInterface.cancel()
        })
        val alert = builder.create()
        alert.show()
    }

    private fun shareLandmark() {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_TEXT, "${landmark.name} is awesome. Check it on Yelp: ${landmark.reviewURL}")
        intent.type = "text/plain"
        startActivity(intent)
    }
}