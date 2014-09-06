package com.alternativeheroes.mhacks.dropped;

import com.alternativeheroes.mhacks.dropped.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ViewFlipper;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class Main extends FragmentActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    /**
     * Provides each view for the view pager.
     */
    DropModePagerAdapter dropAdapter;
    ViewPager pager;

    private static final int[] covers =
            {R.drawable.drop_beat, R.drawable.scream, R.drawable.taylor_swift};

    DropService dropService;
    boolean isServiceConnected=false;

    ServiceConnection dropServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            DropService.LocalBinder mBinder = (DropService.LocalBinder)iBinder;
            dropService = mBinder.getService();
            isServiceConnected=true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isServiceConnected=false;
            dropService=null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);
        Switch mainBreaker = ((Switch) findViewById(R.id.switch1));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (prefs.getBoolean(Constants.dropEnabled, true)) {

            Intent connectDropService = new Intent(this, DropService.class);
            bindService(connectDropService, dropServiceConnection, Service.BIND_AUTO_CREATE);

        } else {
            mainBreaker.setChecked(false);
            mainBreaker.setText(R.string.switch_text_off);
        }

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {

                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        mainBreaker.setOnTouchListener(mDelayHideTouchListener);
        mainBreaker.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        Main.this.changePowerState(buttonView, isChecked);
                    }
                }
        );

        dropAdapter = new DropModePagerAdapter(getSupportFragmentManager());
        ViewPager pager = ((ViewPager) contentView);
        pager.setAdapter(dropAdapter);
        pager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                Main.this.changeMode(position);
            }
        });
        int curr_mode = prefs.getInt(Constants.dropType, Constants.DROPTYPE_FLUX_PAVILION);
        pager.setCurrentItem(curr_mode);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isServiceConnected) {
            unbindService(dropServiceConnection);
        }
    }

    private void changeMode(int mode) {
        if (isServiceConnected) {
            //Toast.makeText(getBaseContext(), "HEY ", Toast.LENGTH_SHORT).show();
            dropService.changeDropMode(mode);
        }
    }

    private void changePowerState(CompoundButton buttonView, boolean isOn) {

        if (isServiceConnected && !isOn) {
            SharedPreferences.Editor prefsEdit =
                    PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
            prefsEdit.putBoolean(Constants.dropEnabled, false);
            prefsEdit.apply();

            unbindService(dropServiceConnection);

            buttonView.setText(R.string.switch_text_off);
            isServiceConnected = false;
        }
        else if (!isServiceConnected && isOn) {
            SharedPreferences.Editor prefsEdit =
                    PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
            prefsEdit.putBoolean(Constants.dropEnabled, true);
            prefsEdit.apply();

            Intent connectDropService = new Intent(this,DropService.class);
            bindService(connectDropService, dropServiceConnection, Service.BIND_AUTO_CREATE);

            buttonView.setText(R.string.switch_text);
            isServiceConnected = true;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public static class DropModePagerAdapter extends FragmentPagerAdapter {

        public DropModePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment frag = new FullscreenImage();
            Bundle args = new Bundle();
            args.putInt(FullscreenImage.IMAGE_ID_TAG, covers[i]);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public int getCount() {
            return covers.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Section " + (position + 1);
        }
    }

    public static class FullscreenImage extends Fragment {

        public static final String IMAGE_ID_TAG = "Image_R_ID";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.image, container, false);

            ((ImageView) rootView.findViewById(R.id.coverImage)).setImageResource(
                    getArguments().getInt(IMAGE_ID_TAG));

            return rootView;
        }
    }
}
