package com.marks.metro.yichenzhou.metromarker.model

import io.realm.RealmObject

/**
 * Created by yichenzhou on 10/13/17.
 */

open class Token: RealmObject() {
    open var yelpTokenStr: String? = null
}