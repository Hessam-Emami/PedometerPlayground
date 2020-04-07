package com.emami.pedometerplayground

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.math.sqrt


class PedometerForegroundService : Service(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var accelerometerSensor: Sensor? = null
    private var stepDetectorSensor: Sensor? = null
    private var stepCounterSensor: Sensor? = null
    //if the sensors exist these values will be 0 otherwise -1 indicating the sensor is null
    private var countedStepCounter = -1
    private var countedStepDetector = -1
    private var countedAccelerometerOne = -1
    private var countedAccelerometerTwo = -1
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    val stepLogLiveData = MutableLiveData<StepLogData>()

    override fun onCreate() {
        super.onCreate()
        startForeground(
            123123,
            NotificationUtil.createNotification(notificationManager,this)
        )
        sensorManager = ContextCompat.getSystemService(this, SensorManager::class.java)?.also {
            initSensors(it)
        }
    }


    private fun initSensors(sensorManager: SensorManager) {
        sensorManager.run {
            getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
                countedAccelerometerOne = 0
                countedAccelerometerTwo = 0
                accelerometerSensor = sensor
                registerListener(
                    this@PedometerForegroundService,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                val mProcessDataTask =
                    ProcessDataTask()
                mScheduledProcessDataTask = mScheduledExecutorService.scheduleWithFixedDelay(
                    mProcessDataTask,
                    10,
                    10,
                    TimeUnit.SECONDS
                )
            }
            getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)?.let { sensor ->
                countedStepDetector = 0
                stepDetectorSensor = sensor
                registerListener(
                    this@PedometerForegroundService,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
            getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let { sensor ->
                countedStepCounter = 0
                stepCounterSensor = sensor
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
        when (event?.sensor) {
            stepCounterSensor -> {
                countedStepCounter++
            }
            stepDetectorSensor -> {
                countedStepDetector++
            }
            accelerometerSensor -> {
                processEventUsingFirstAccelerometerAlgorithm(event)
                processEventUsingSecondAccelerometerAlgorithm(event)
            }
        }
    }

    var previousMagnitude = 0.0
    private fun processEventUsingFirstAccelerometerAlgorithm(event: SensorEvent?) {
        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]
            val magnitude = sqrt(
                ((x.pow(2)) + (y.pow(2)) + (z.pow(2))).toDouble()
            )
            val diff = magnitude - previousMagnitude
            previousMagnitude = magnitude
            // 7 is an experimental threshold
            if (diff > 7) {
                countedAccelerometerOne++
            }
        }
    }

    private val WALKINGPEAK = 18

    var mAccelerometerDataList =
        ArrayList<AccelerometerData>()
    var mRawDataList =
        ArrayList<AccelerometerData>()
    var mAboveThresholdValuesList =
        ArrayList<AccelerometerData>()
    var mHighestPeakList =
        ArrayList<AccelerometerData>()
    private var mScheduledProcessDataTask: ScheduledFuture<*>? = null
    private var mScheduledExecutorService: ScheduledExecutorService =
        Executors.newScheduledThreadPool(2)

    private fun processEventUsingSecondAccelerometerAlgorithm(event: SensorEvent?) {
        event?.let {
            //start recording
            val mAccelerometerData = AccelerometerData()
            mAccelerometerData.x = event.values[0]
            mAccelerometerData.y = event.values[1]
            mAccelerometerData.z = event.values[2]
            mAccelerometerData.time = event.timestamp
            mAccelerometerDataList.add(mAccelerometerData)
        }
    }

    inner class ProcessDataTask : Runnable {
        override fun run() {
            //Copy accelerometer data from main sensor array in separate array for processing
            mRawDataList.addAll(mAccelerometerDataList)
            mAccelerometerDataList.clear()
            //Calculating the magnitude (Square root of sum of squares of x, y, z) & converting time from nano seconds from boot time to epoc time
            val timeOffsetValue =
                System.currentTimeMillis() - SystemClock.elapsedRealtime()
            val dataSize: Int = mRawDataList.size
            for (i in 0 until dataSize) {
                mRawDataList[i].value = sqrt(
                    mRawDataList[i].x.toDouble().pow(2.0) + mRawDataList[i].y.toDouble().pow(2.0) + mRawDataList[i].z.toDouble().pow(
                        2.0
                    )
                )
                mRawDataList[i].time = mRawDataList[i].time / 1000000L + timeOffsetValue
            }
            //Calculating the High Peaks
            findHighPeaks()
            //Remove high peaks close to each other which are within range of 0.4 seconds
            removeClosePeaks()
            //Find the type of step (Running, jogging, walking) & store in Database
            findStepTypeAndStoreInDB()
            mRawDataList.clear()
            mAboveThresholdValuesList.clear()
            mHighestPeakList.clear()
        }

        private fun findHighPeaks() { //Calculating the High Peaks
            var isAboveMeanLastValueTrue = false
            mRawDataList.forEach {
                isAboveMeanLastValueTrue =
                    if (it.value > WALKINGPEAK) {
                        mAboveThresholdValuesList.add(it)
                        false
                    } else {
                        if (!isAboveMeanLastValueTrue && mAboveThresholdValuesList.size > 0) {
                            Collections.sort(mAboveThresholdValuesList, DataSorter())
                            mHighestPeakList.add(
                                mAboveThresholdValuesList[mAboveThresholdValuesList.size - 1]
                            )
                            mAboveThresholdValuesList.clear()
                        }
                        true
                    }
            }
        }

        private fun removeClosePeaks() {
            for (i in 0 until mHighestPeakList.size - 1) {
                if (mHighestPeakList[i].getIsRealPeak()) {
                    if (mHighestPeakList[i + 1].time - mHighestPeakList[i].time < 400) {
                        if (mHighestPeakList[i + 1].value > mHighestPeakList[i].value) {
                            mHighestPeakList[i].isRealPeak = false
                        } else {
                            mHighestPeakList[i + 1].isRealPeak = false
                        }
                    }
                }
            }
        }

        private fun findStepTypeAndStoreInDB() {
            val size: Int = mHighestPeakList.size

            for (i in 0 until size) {
                if (mHighestPeakList[i].getIsRealPeak()) {
                    countedAccelerometerTwo++
                }
            }
        }

        inner class DataSorter : Comparator<AccelerometerData> {
            override fun compare(obj1: AccelerometerData, obj2: AccelerometerData): Int {
                var returnVal = 0
                if (obj1.value < obj2.value) {
                    returnVal = -1
                } else if (obj1.value > obj2.value) {
                    returnVal = 1
                }
                return returnVal
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(this)
    }

    inner class StepBinder : Binder() {
        fun getStepService() = this@PedometerForegroundService
    }
}

