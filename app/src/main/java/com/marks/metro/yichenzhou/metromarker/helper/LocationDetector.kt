package com.marks.metro.yichenzhou.metromarker.helper

/**
 * Created by PROFESSOR JARED ALEXANDER on 10/10/17.
 */

import android.content.Context
import android.location.Location
import android.support.v4.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import java.util.*
import kotlin.concurrent.timerTask

class LocationDetector(val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient = FusedLocationProviderClient(context)
    enum class FailureReason {
        TIMEOUT,
        NO_PERMISSION
    }

    interface LocationListener {
        fun locationFound(location: Location)
        fun locationNotFound(reason: FailureReason)
    }
    var locationListener: LocationListener? = null

    fun detectLocation() {
        val locationRequest = LocationRequest()
        locationRequest.interval = 0L
        //check for location permission
        val permissionResult = ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        //if location permission granted, proceed with location detection
        if(permissionResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    locationListener?.locationFound(locationResult.locations.first())
                }
            }
            //start a timer to ensure location detection ends after 10 seconds
            val timer = Timer()
            timer.schedule(timerTask {
                fusedLocationClient?.removeLocationUpdates(locationCallback)
                locationListener?.locationNotFound(FailureReason.TIMEOUT)
            }, 10*1000)
            fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback, null)
        } else {
            locationListener?.locationNotFound(FailureReason.NO_PERMISSION)
        }
    }
}