package com.emami.pedometerplayground

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData


class PedometerForegroundService : Service(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var stepDetector: Sensor? = null
    private var stepCounter: Sensor? = null
    val stepLogLiveData = MutableLiveData<StepLogData>()

    override fun onCreate() {
        super.onCreate()
        sensorManager = ContextCompat.getSystemService(this, SensorManager::class.java)?.also {
            initSensors(it)
        }
    }

    private fun initSensors(sensorManager: SensorManager) {
        sensorManager.run {
            getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
                accelerometer = sensor
                registerListener(
                    this@PedometerForegroundService,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
            getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)?.let { sensor ->
                stepDetector = sensor
                registerListener(
                    this@PedometerForegroundService,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
            getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let { sensor ->
                stepCounter = sensor
                registerListener(
                    this@PedometerForegroundService,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return StepBinder()
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(this)
    }

    inner class StepBinder : Binder() {
        fun getStepService() = this@PedometerForegroundService
    }
}

