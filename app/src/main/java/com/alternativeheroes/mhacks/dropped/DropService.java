package com.alternativeheroes.mhacks.dropped;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class DropService extends Service implements SensorEventListener {

    private static final double EPSILON = 1.0;
    private static final String TAG = DropService.class.getName();
    private SensorManager sensorMan;
    private Sensor        acceleration;
    private final IBinder mBinder = new LocalBinder();
    private MediaPlayer player;

    public DropService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        sensorMan = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        acceleration = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMan.registerListener(this, acceleration, SensorManager.SENSOR_DELAY_GAME);
        return mBinder;
    }

    public class LocalBinder extends Binder {
        DropService getService() {
            // Return this instance of LocalService so clients can call public methods
            return DropService.this;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.d(TAG, "Sensor Event");
        if (Math.sqrt(
                Math.pow(sensorEvent.values[0], 2) +
                        Math.pow(sensorEvent.values[1], 2) +
                        Math.pow(sensorEvent.values[2], 2) )  < EPSILON) {
            //This code is executed when we get a drop phone event
            Log.d(TAG, "Drop phone event");
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
        }

}

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
