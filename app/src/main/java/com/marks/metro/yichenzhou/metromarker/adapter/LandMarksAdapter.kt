package com.marks.metro.yichenzhou.metromarker.adapter

import android.content.Intent
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.marks.metro.yichenzhou.metromarker.R
import com.marks.metro.yichenzhou.metromarker.activity.LandMarkDetailActivity
import com.marks.metro.yichenzhou.metromarker.helper.AppHelper
import com.marks.metro.yichenzhou.metromarker.model.Landmark

/**
 * Created by mc.xia on 2017/10/9.
 */

class LandMarksAdapter(landmarks: ArrayList<Landmark>) : RecyclerView.Adapter<LandMarksAdapter.ViewHolder>() {
    private val TAG = "LandMarksAdapter"
    private val landmarkList = landmarks

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        val landmark = this.landmarkList?.get(position)
        landmark?.let {
            (holder as ViewHolder).bind(landmark, position)
        }
    }

    override fun getItemCount(): Int {
        return this.landmarkList.count()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent?.context)
        return ViewHolder(layoutInflater.inflate(R.layout.row_landmark, parent, false))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private lateinit var landmark: Landmark
        private var position: Int? = null
        private val nameTextView: TextView = view.findViewById(R.id.nameText)
        private val ratingTextView: TextView = view.findViewById(R.id.ratingText)
        private val infoTextView: TextView = view.findViewById(R.id.infoText)
        private val distanceTextView: TextView = view.findViewById(R.id.distanceText)
        private val imageView: ImageView = view.findViewById(R.id.imageView)

        init {
            view.setOnClickListener(this)
        }

        fun bind(landmark: Landmark, position: Int) {
            this.landmark = landmark
            this.position = position
            nameTextView.text = (position + 1).toString() + ". " + landmark.name
            ratingTextView.text = landmark.rating.toString() + "/5.0"
            infoTextView.text = landmark.category
            distanceTextView.text = "%.2f mi".format(landmark.distance/1000.0)
            if (landmark.imageURL != null) {
                AppHelper.yelpImageFetcher(landmark.imageURL!!, this.imageView)
            }
         }

        override fun onClick(view: View?) {
            Log.d(TAG, "You pressed cell ${this.position}")
            val intent = Intent(view?.context, LandMarkDetailActivity::class.java)
            intent.putExtra("id", this.landmark.id)
            intent.putExtra("name", this.landmark.name)
            intent.putExtra("lang", this.landmark.latitude)
            intent.putExtra("long", this.landmark.longitude)
            intent.putExtra("rating", this.landmark.rating)
            intent.putExtra("category", this.landmark.category)
            intent.putExtra("address", this.landmark.address)
            intent.putExtra("price", this.landmark.price)
            intent.putExtra("isClose", this.landmark.isClose)
            intent.putExtra("url", this.landmark.reviewURL)
            intent.putExtra("imageURL", this.landmark.imageURL)
            intent.putExtra("reviewCount", this.landmark.reviewCount)
            intent.putExtra("distance", this.landmark.distance)
            startActivity(view?.context, intent, null)
        }
    }
}

