package com.emami.pedometerplayground.google_sample;
/**
 * Listens for alerts about steps being detected.
 */
public interface StepListener {

    /**
     * Called when a step has been detected.  Given the time in nanoseconds at
     * which the step was detected.
     */
    public void step(long timeNs);

}