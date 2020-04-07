package com.emami.pedometerplayground.model

class AccelerometerData {
    var isRealPeak: Boolean = true
    var value = 0.0
    var x = 0f
    var y = 0f
    var z = 0f
    var time: Long = 0

    fun getIsRealPeak()= isRealPeak
}