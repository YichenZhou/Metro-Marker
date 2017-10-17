package com.marks.metro.yichenzhou.metromarker.model

import io.realm.RealmObject

/**
 * Created by yichenzhou on 9/20/17.
 */

open class MetroStation: RealmObject() {
    open var name: String = ""
    open var long: String = ""
    open var lang: String = ""
    open var street: String = ""
    open var city: String = ""
    open var state: String = ""
    open var zip: String = ""

    fun setValues(dataList: List<String>) {
        val _name = dataList[0]
        if (_name == "") {
            throw Exception("Invalid string for name")
        }
        this.name = _name

        val _lang = dataList[1]
        if (_lang == "") {
            throw  Exception("Invalid string for lang")
        }
        this.lang = _lang

        val _long = dataList[2]
        if (_long == "") {
            throw  Exception("Invalid string for long")
        }
        this.long = _long

        val _street = dataList[3]
        if (_street == "") {
            throw  Exception("Invalid string for street")
        }
        this.street = _street

        val _city = dataList[4]
        if (_city == "") {
            throw  Exception("Invalid string for city")
        }
        this.city = _city

        val _state = dataList[5]
        if (_state == "") {
            throw  Exception("Invalid string for state")
        }
        this.state = _state

        val _zip = dataList[6]
        if (_zip == "") {
            throw  Exception("Invalid string for zip")
        }
        this.zip = _zip
    }
}