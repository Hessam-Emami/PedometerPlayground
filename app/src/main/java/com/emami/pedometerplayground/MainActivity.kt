package com.emami.pedometerplayground

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), ServiceConnection {

    private var isServiceBounded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main_btn_start.setOnClickListener {
            startPedometerService()
        }
        main_btn_stop.setOnClickListener {
            stopPedometerService()
        }
    }

    private fun stopPedometerService() {
        if (isServiceBounded) {
            unbindService(this)
        }
        Intent(this, PedometerForegroundService::class.java).also {
            stopService(it)
        }
    }

    private fun startPedometerService() {
        Intent(this, PedometerForegroundService::class.java).run {
            ContextCompat.startForegroundService(this@MainActivity, this)
            bindService(this, this@MainActivity, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        isServiceBounded = false
    }

    override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
        isServiceBounded = true
        p1?.let {
            if (it is PedometerForegroundService.StepBinder) {
                it.getStepService()
                    .stepLogLiveData.observe(this@MainActivity, Observer { stepLog ->
                    populateStepData(stepLog)
                })
            }
        }
    }

    private fun populateStepData(stepLog: StepLogData?) {
        main_tv_accelerometer_one.text = stepLog?.stepDetector
        main_tv_step_counter.text = stepLog?.accelerometer
        main_tv_step_detector.text = stepLog?.stepCounter
    }
}
