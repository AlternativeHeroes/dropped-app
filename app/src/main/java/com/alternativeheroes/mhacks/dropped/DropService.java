package com.alternativeheroes.mhacks.dropped;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.firebase.client.Firebase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class DropService extends Service implements SensorEventListener {

    private static final double EPSILON = 1.0;
    private static final String TAG = DropService.class.getName();
    private final IBinder mBinder = new LocalBinder();
    private SharedPreferences dropPreference;
    private SensorManager     sensorMan;
    private LocationManager   locationMan;
    private Sensor            acceleration;
    private MediaPlayer       player;
    private String            uniqueID;

    public int     dropType;
    public boolean isFalling = false;

    public DropService() { }

    @Override
    public void onCreate() {
        locationMan = (LocationManager) getSystemService(LOCATION_SERVICE);

        sensorMan   = (SensorManager)   getSystemService(Context.SENSOR_SERVICE);
        acceleration = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        startSensor();

        player = MediaPlayer.create(getBaseContext(), R.raw.wheatley_sp_a1_wakeup_panic01);
        player.setLooping(true);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        dropType = prefs.getInt(Constants.dropType, Constants.DROPTYPE_MUSIC);
        changeDropMode(dropType);

        uniqueID = getUniqueID();
    }

    @Override
    public IBinder onBind(Intent intent) {
        //startSensor();
        return mBinder;
    }

    private void startSensor() {
        sensorMan.registerListener(this, acceleration, SensorManager.SENSOR_DELAY_GAME);
    }

    private void stopSensor() {
        sensorMan.unregisterListener(this);
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        super.unbindService(conn);
    }

    @Override
    public void onDestroy() {
        stopSensor();
        player.release();
        player = null;
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        DropService getService() {
            // Return this instance of LocalService so clients can call public methods
            return DropService.this;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if ( magnitude(sensorEvent.values) < EPSILON ) {
            //This code is executed when we get a drop phone event
            if (!isFalling) {
                isFalling = true;
                player.start();
                sendFirebaseMessage();
            }
        }
        else if (isFalling) {
            player.pause();
            isFalling = false;
        }
    }

    private double magnitude(float[] values) {
        return Math.sqrt(Math.pow(values[0], 2) + Math.pow(values[1], 2) + Math.pow(values[2], 2));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    private String getUniqueID() {
        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        String mac = info.getMacAddress();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(mac.getBytes());
            byte[] digest = md.digest();
            StringBuffer sb = new StringBuffer();

            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        }
        catch (NoSuchAlgorithmException err) {
            return mac;
        }
    }

    private void sendFirebaseMessage() {
        Location location = locationMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            location = locationMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        Log.d(TAG, "Location X:" + (location.getLatitude() * 10000) + " ,Y:" + (location.getLatitude() * 10000));

        //Firebase sync
        Firebase firebase = new Firebase("https://shining-fire-2142.firebaseio.com/");
        firebase = firebase.child(uniqueID);
        firebase = firebase.child(Long.toString(System.currentTimeMillis()));
        Map<String, String> users = new HashMap<String, String>();
        users.put("latitude",  Double.toString(location.getLatitude()));
        users.put("longitude", Double.toString(location.getLongitude()));
        firebase.setValue(users);
    }

    public void changeDropMode(int mode) {
        if (mode == dropType) {
            return;
        }


    }
}
