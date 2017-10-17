package com.marks.metro.yichenzhou.metromarker.activity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import com.marks.metro.yichenzhou.metromarker.R
import com.marks.metro.yichenzhou.metromarker.adapter.LandMarksAdapter
import com.marks.metro.yichenzhou.metromarker.model.Favourite
import com.marks.metro.yichenzhou.metromarker.model.Landmark
import io.realm.Realm
import kotlinx.android.synthetic.main.landmark_list.*
import kotlin.properties.Delegates

class FavoriteActivity : AppCompatActivity() {
    private var realm: Realm by Delegates.notNull()
    private var favoriteList = ArrayList<Landmark>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.landmark_list)

        //setup toolbar
        this.landmark_filter_toolbar.title = "Favorite List"

        // Get favorites from database
        Realm.init(applicationContext)
        this.realm = Realm.getDefaultInstance()
        this.loadFavorites()

        //setup recyclerView adapter
        this.favoriteListView.layoutManager = LinearLayoutManager(this)
        this.favoriteListView.adapter = LandMarksAdapter(this.favoriteList)
    }

    override fun onResume() {
        super.onResume()

        this.loadFavorites()
        this.favoriteListView.layoutManager = LinearLayoutManager(this)
        this.favoriteListView.adapter = LandMarksAdapter(this.favoriteList)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.landmarkfilter, menu)

        return true
    }

    private fun loadFavorites() {
        this.favoriteList.removeAll { true }
        val results = this.realm.where(Favourite::class.java).findAll()
        for (result in results) {
            var landmark = Landmark()
            landmark.id = result.id
            landmark.name = result.name
            landmark.imageURL = result.imageURL
            landmark.isClose = result.isClose
            landmark.reviewURL = result.reviewURL
            landmark.reviewCount = result.reviewCount
            landmark.category = result.category
            landmark.rating = result.rating
            landmark.latitude = result.latitude
            landmark.longitude = result.longitude
            landmark.price = result.price
            landmark.address = result.address
            landmark.distance = result.distance
            this.favoriteList.add(landmark)
        }
    }
}
