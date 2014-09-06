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
    private SensorManager sensorMan;
    private Sensor        acceleration;
    private MediaPlayer   player;
    private String        uniqueID;
    SharedPreferences dropPreference;
    int dropType;

    public boolean isFalling = false;

    public DropService() { }

    @Override
    public void onCreate() {
        sensorMan = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        acceleration = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        startSensor();
        dropPreference=getSharedPreferences(Constants.dropPreference,0);
        dropType = dropPreference.getInt(Constants.dropType,Constants.DROPTYPE_MUSIC);

        LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        Log.d(TAG, "Location X:" + (location.getLatitude() * 10000) + " ,Y:" + (location.getLatitude() * 10000));

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
            uniqueID = sb.toString();
        }
        catch (NoSuchAlgorithmException err) { }

        //Firebase sync
        Firebase firebase = new Firebase("https://e2g0l1uaxa8.firebaseio-demo.com/");
        Map<String, String> users = new HashMap<String, String>();
        users.put("latitude",  Double.toString(location.getLatitude()));
        users.put("longitude", Double.toString(location.getLongitude()));
        users.put("id",        uniqueID);
        firebase.setValue(users);
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
        if (Math.sqrt(
                Math.pow(sensorEvent.values[0], 2) +
                        Math.pow(sensorEvent.values[1], 2) +
                        Math.pow(sensorEvent.values[2], 2) )  < EPSILON) {
            //This code is executed when we get a drop phone event

            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);

            /* Location stuff */


            Intent dropEventBroadcast = new Intent(Constants.dropEventAction);
            sendBroadcast(dropEventBroadcast);
            isFalling=true;
        } else {
            if(isFalling){
                isFalling=false;
            }


        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }
}
