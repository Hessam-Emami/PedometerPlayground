package com.emami.pedometerplayground.main

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.emami.pedometerplayground.R
import com.emami.pedometerplayground.daisy.DaisyDebugActivity
import com.emami.pedometerplayground.filtering.FilteringActivity
import com.emami.pedometerplayground.google_sample.GooglePedometerActivity
import com.emami.pedometerplayground.main.model.StepLogData
import com.emami.pedometerplayground.oxford.OxfordActivity
import com.emami.pedometerplayground.uwaterloo.lab3_201_04.WaterlooActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), ServiceConnection {

    private var isServiceBounded = false
    private var isServiceStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startPedometerService()

        main_btn_start.setOnClickListener {
            startPedometerService()
        }
        main_btn_stop.setOnClickListener {
            stopPedometerService()
        }
        main_btn_daisy.setOnClickListener {
            launchIntent(DaisyDebugActivity::class.java)
        }
        main_btn_filtering.setOnClickListener {
            launchIntent(FilteringActivity::class.java)
        }
        main_btn_google.setOnClickListener {
            launchIntent(GooglePedometerActivity::class.java)
        }
        main_btn_oxford.setOnClickListener {
            launchIntent(OxfordActivity::class.java)
        }
        main_btn_waterloo.setOnClickListener {
            launchIntent(WaterlooActivity::class.java)
        }
    }

    private fun <T> launchIntent(jClass: Class<T>) {
        startActivity(Intent(this, jClass))
    }



    private fun stopPedometerService() {
        if (isServiceBounded) {
            unbindService(this)
        }
        if (isServiceStarted) {
            Intent(this, PedometerForegroundService::class.java).also {
                stopService(it)
            }
        }
        isServiceBounded = false
        isServiceStarted = false
        changeButtonState(true)
    }

    private fun startPedometerService() {
        Intent(this, PedometerForegroundService::class.java).run {
            ContextCompat.startForegroundService(this@MainActivity, this)
            bindService(this, this@MainActivity, Context.BIND_AUTO_CREATE)
        }
        isServiceStarted = true
        changeButtonState(false)
    }

    private fun changeButtonState(shouldShowStartButton: Boolean) {
        if (shouldShowStartButton) {
            main_btn_start.visibility = View.VISIBLE
            main_btn_stop.visibility = View.GONE
        } else {
            main_btn_start.visibility = View.GONE
            main_btn_stop.visibility = View.VISIBLE
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
        main_tv_accelerometer_one.text = stepLog?.accelerometerOne
        main_tv_step_counter.text = stepLog?.stepCounter
        main_tv_step_detector.text = stepLog?.stepDetector
        main_tv_accelerometer_two.text = stepLog?.accelerometerTwo
    }
}
