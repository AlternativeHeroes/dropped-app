package com.alternativeheroes.mhacks.dropped.SensorHandlers;

import android.hardware.SensorEventListener;

/**
 * Created by mde on 9/6/14.
 */
public interface SensorHandlerInterface extends SensorEventListener {
    public int getAudioResource();
    public void onInit();
}
