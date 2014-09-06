package com.alternativeheroes.mhacks.dropped;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.alternativeheroes.mhacks.dropped.SensorHandlers.SensorHandlerInterface;
import com.firebase.client.Firebase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class DropService extends Service {

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

    private SensorHandlerInterface[] profiles =
            { new FluxPavilionHandler(), new WheatleyHandler() };

    private SensorHandlerInterface profile;

    public DropService() { }

    @Override
    public void onCreate() {
        locationMan = (LocationManager) getSystemService(LOCATION_SERVICE);

        sensorMan   = (SensorManager)   getSystemService(Context.SENSOR_SERVICE);
        acceleration = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        dropType = prefs.getInt(Constants.dropType, Constants.DROPTYPE_FLUX_PAVILION);
        profile = profiles[dropType];
        startSensor();
        player = MediaPlayer.create(getBaseContext(), profile.getAudioResource());

        uniqueID = getUniqueID();
    }

    @Override
    public IBinder onBind(Intent intent) {
        //startSensor();
        return mBinder;
    }

    private void startSensor() {
        sensorMan.registerListener(profile, acceleration, SensorManager.SENSOR_DELAY_GAME);
    }

    private void stopSensor() {
        sensorMan.unregisterListener(profile);
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        super.unbindService(conn);
    }

    @Override
    public void onDestroy() {
        stopSensor();
        releasePlayer();
        super.onDestroy();
    }

    private void releasePlayer() {
        player.release();
        player = null;
    }

    public class LocalBinder extends Binder {
        DropService getService() {
            // Return this instance of LocalService so clients can call public methods
            return DropService.this;
        }
    }

    private static double magnitude(float[] values) {
        return Math.sqrt(Math.pow(values[0], 2) + Math.pow(values[1], 2) + Math.pow(values[2], 2));
    }

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
        if (location == null) {
            return;
        }
        //Log.d(TAG, "Location X:" + (location.getLatitude() * 10000) + " ,Y:" + (location.getLatitude() * 10000));
        try {
            //Firebase sync
            Firebase firebase = new Firebase("https://shining-fire-2142.firebaseio.com/");
            firebase = firebase.child(uniqueID);
            firebase = firebase.child(Long.toString(System.currentTimeMillis()));
            Map<String, String> users = new HashMap<String, String>();
            users.put("latitude", Double.toString(location.getLatitude()));
            users.put("longitude", Double.toString(location.getLongitude()));
            firebase.setValue(users);
        }
        catch (Exception err) { }
    }

    public void changeDropMode(int mode) {
        if (mode == dropType) {
            return;
        }

        releasePlayer();
        stopSensor();
        profile = profiles[mode];
        player = MediaPlayer.create(getBaseContext(), profile.getAudioResource());
        profile.onInit();
        startSensor();

        SharedPreferences.Editor prefsEdit =
                PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
        prefsEdit.putInt(Constants.dropType, mode);
        prefsEdit.apply();
        dropType = mode;
    }

    public class FluxPavilionHandler implements SensorHandlerInterface {

        private static final int loopStart = 2870;
        private static final int loopEnd   = 6315;
        private static final int dropStart = 54318;
        private static final int dropEnd   = 55343;
        private static final int songReset = 90000;
        private boolean isInDrop   = false;
        private boolean hasDropped = false;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if ( magnitude(event.values) < EPSILON ) {
                if (!isInDrop) {
                    player.start();
                    isInDrop = true;
                }
            }
            else if (isInDrop) {
                isInDrop = false;
                hasDropped = true;
                player.seekTo(dropEnd);
            }
            else if (hasDropped) {
                if (player.getCurrentPosition() >= songReset) {
                    hasDropped = false;
                }
            }
            else {
                if (player.getCurrentPosition() >= loopEnd) {
                    player.seekTo(loopStart);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }

        @Override
        public int getAudioResource() {
            return R.raw.i_cant_stop;
        }

        @Override
        public void onInit() {
            player.seekTo(loopStart);
            player.start();
            isInDrop = false;
            hasDropped = false;
        }
    }

    public class WheatleyHandler implements SensorHandlerInterface {
        private boolean isFalling = false;

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

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }

        @Override
        public int getAudioResource() {
            return R.raw.wheatley_sp_a1_wakeup_panic01;
        }

        @Override
        public void onInit() {
            isFalling = false;
            player.setLooping(true);
        }
    }
}
