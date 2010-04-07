/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IContentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;
import com.android.settings.R;
import com.android.settings.bluetooth.LocalBluetoothManager;

/**
 * Provides control of power-related settings from a widget.
 */
public class SettingsAppWidgetProvider extends AppWidgetProvider {
    static final String TAG = "SettingsAppWidgetProvider";

    static final ComponentName THIS_APPWIDGET =
            new ComponentName("com.android.settings",
                    "com.android.settings.widget.SettingsAppWidgetProvider");

    private static LocalBluetoothManager sLocalBluetoothManager = null;

    private static final int BUTTON_WIFI = 0;
    private static final int BUTTON_BRIGHTNESS = 1;
    private static final int BUTTON_SYNC = 2;
    private static final int BUTTON_GPS = 3;
    private static final int BUTTON_BLUETOOTH = 4;

    // This widget keeps track of two sets of states:
    // "3-state": STATE_DISABLED, STATE_ENABLED, STATE_INTERMEDIATE
    // "5-state": STATE_DISABLED, STATE_ENABLED, STATE_TURNING_ON, STATE_TURNING_OFF, STATE_UNKNOWN
    private static final int STATE_DISABLED = 0;
    private static final int STATE_ENABLED = 1;
    private static final int STATE_TURNING_ON = 2;
    private static final int STATE_TURNING_OFF = 3;
    private static final int STATE_UNKNOWN = 4;
    private static final int STATE_INTERMEDIATE = 5;


    /**
     * Minimum and maximum brightnesses.  Don't go to 0 since that makes the display unusable
     */
    private static final int MINIMUM_BACKLIGHT = android.os.Power.BRIGHTNESS_DIM + 10;
    private static final int MAXIMUM_BACKLIGHT = android.os.Power.BRIGHTNESS_ON;
    private static final int DEFAULT_BACKLIGHT = (int) (android.os.Power.BRIGHTNESS_ON * 0.4f);

    private static final StateTracker sWifiState = new WifiStateTracker();
    private static final StateTracker sBluetoothState = new BluetoothStateTracker();

    /**
     * The state machine for Wifi and Bluetooth toggling, tracking
     * reality versus the user's intent.
     *
     * This is necessary because reality moves relatively slowly
     * (turning on &amp; off radio drivers), compared to user's
     * expectations.
     */
    private abstract static class StateTracker {
        // Is the state in the process of changing?
        private boolean mInTransition = false;
        private Boolean mActualState = null;  // initially not set
        private Boolean mIntendedState = null;  // initially not set

        // Did a toggle request arrive while a state update was
        // already in-flight?  If so, the mIntendedState needs to be
        // requested when the other one is done, unless we happened to
        // arrive at that state already.
        private boolean mDeferredStateChangeRequestNeeded = false;

        /**
         * User pressed a button to change the state.  Something
         * should immediately appear to the user afterwards, even if
         * we effectively do nothing.  Their press must be heard.
         */
        public final void toggleState(Context context) {
            int currentState = getTriState(context);
            boolean newState = false;
            switch (currentState) {
                case STATE_ENABLED:
                    newState = false;
                    break;
                case STATE_DISABLED:
                    newState = true;
                    break;
                case STATE_INTERMEDIATE:
                    if (mIntendedState != null) {
                        newState = !mIntendedState;
                    }
                    break;
            }
            mIntendedState = newState;
            if (mInTransition) {
                // We don't send off a transition request if we're
                // already transitioning.  Makes our state tracking
                // easier, and is probably nicer on lower levels.
                // (even though they should be able to take it...)
                mDeferredStateChangeRequestNeeded = true;
            } else {
                mInTransition = true;
                requestStateChange(context, newState);
            }
        }

        /**
         * Update internal state from a broadcast state change.
         */
        public abstract void onActualStateChange(Context context, Intent intent);

        /**
         * Sets the value that we're now in.  To be called from onActualStateChange.
         *
         * @param newState one of STATE_DISABLED, STATE_ENABLED, STATE_TURNING_ON,
         *                 STATE_TURNING_OFF, STATE_UNKNOWN
         */
        protected final void setCurrentState(Context context, int newState) {
            final boolean wasInTransition = mInTransition;
            switch (newState) {
                case STATE_DISABLED:
                    mInTransition = false;
                    mActualState = false;
                    break;
                case STATE_ENABLED:
                    mInTransition = false;
                    mActualState = true;
                    break;
                case STATE_TURNING_ON:
                    mInTransition = true;
                    mActualState = false;
                    break;
                case STATE_TURNING_OFF:
                    mInTransition = true;
                    mActualState = true;
                    break;
            }

            if (wasInTransition && !mInTransition) {
                if (mDeferredStateChangeRequestNeeded) {
                    Log.v(TAG, "processing deferred state change");
                    if (mActualState != null && mIntendedState != null &&
                        mIntendedState.equals(mActualState)) {
                        Log.v(TAG, "... but intended state matches, so no changes.");
                    } else if (mIntendedState != null) {
                        mInTransition = true;
                        requestStateChange(context, mIntendedState);
                    }
                    mDeferredStateChangeRequestNeeded = false;
                }
            }
        }


        /**
         * If we're in a transition mode, this returns true if we're
         * transitioning towards being enabled.
         */
        public final boolean isTurningOn() {
            return mIntendedState != null && mIntendedState;
        }

        /**
         * Returns simplified 3-state value from underlying 5-state.
         *
         * @param context
         * @return STATE_ENABLED, STATE_DISABLED, or STATE_INTERMEDIATE
         */
        public final int getTriState(Context context) {
            if (mInTransition) {
                // If we know we just got a toggle request recently
                // (which set mInTransition), don't even ask the
                // underlying interface for its state.  We know we're
                // changing.  This avoids blocking the UI thread
                // during UI refresh post-toggle if the underlying
                // service state accessor has coarse locking on its
                // state (to be fixed separately).
                return STATE_INTERMEDIATE;
            }
            switch (getActualState(context)) {
                case STATE_DISABLED:
                    return STATE_DISABLED;
                case STATE_ENABLED:
                    return STATE_ENABLED;
                default:
                    return STATE_INTERMEDIATE;
            }
        }

        /**
         * Gets underlying actual state.
         *
         * @param context
         * @return STATE_ENABLED, STATE_DISABLED, STATE_ENABLING, STATE_DISABLING,
         *         or or STATE_UNKNOWN.
         */
        public abstract int getActualState(Context context);

        /**
         * Actually make the desired change to the underlying radio
         * API.
         */
        protected abstract void requestStateChange(Context context, boolean desiredState);
    }

    /**
     * Subclass of StateTracker to get/set Wifi state.
     */
    private static final class WifiStateTracker extends StateTracker {
        @Override
        public int getActualState(Context context) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                return wifiStateToFiveState(wifiManager.getWifiState());
            }
            return STATE_UNKNOWN;
        }

        @Override
        protected void requestStateChange(Context context, final boolean desiredState) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                Log.d(TAG, "No wifiManager.");
                return;
            }

            // Actually request the wifi change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    /**
                     * Disable tethering if enabling Wifi
                     */
                    int wifiApState = wifiManager.getWifiApState();
                    if (desiredState && ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING) ||
                                         (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED))) {
                        wifiManager.setWifiApEnabled(null, false);
                    }

                    wifiManager.setWifiEnabled(desiredState);
                    return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            if (!WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                return;
            }
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            setCurrentState(context, wifiStateToFiveState(wifiState));
        }

        /**
         * Converts WifiManager's state values into our
         * Wifi/Bluetooth-common state values.
         */
        private static int wifiStateToFiveState(int wifiState) {
            switch (wifiState) {
                case WifiManager.WIFI_STATE_DISABLED:
                    return STATE_DISABLED;
                case WifiManager.WIFI_STATE_ENABLED:
                    return STATE_ENABLED;
                case WifiManager.WIFI_STATE_DISABLING:
                    return STATE_TURNING_OFF;
                case WifiManager.WIFI_STATE_ENABLING:
                    return STATE_TURNING_ON;
                default:
                    return STATE_UNKNOWN;
            }
        }
    }

    /**
     * Subclass of StateTracker to get/set Bluetooth state.
     */
    private static final class BluetoothStateTracker extends StateTracker {

        @Override
        public int getActualState(Context context) {
            if (sLocalBluetoothManager == null) {
                sLocalBluetoothManager = LocalBluetoothManager.getInstance(context);
                if (sLocalBluetoothManager == null) {
                    return STATE_UNKNOWN;  // On emulator?
                }
            }
            return bluetoothStateToFiveState(sLocalBluetoothManager.getBluetoothState());
        }

        @Override
        protected void requestStateChange(Context context, final boolean desiredState) {
            if (sLocalBluetoothManager == null) {
                Log.d(TAG, "No LocalBluetoothManager");
                return;
            }
            // Actually request the Bluetooth change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    sLocalBluetoothManager.setBluetoothEnabled(desiredState);
                    return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                return;
            }
            int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            setCurrentState(context, bluetoothStateToFiveState(bluetoothState));
        }

        /**
         * Converts BluetoothAdapter's state values into our
         * Wifi/Bluetooth-common state values.
         */
        private static int bluetoothStateToFiveState(int bluetoothState) {
            switch (bluetoothState) {
                case BluetoothAdapter.STATE_OFF:
                    return STATE_DISABLED;
                case BluetoothAdapter.STATE_ON:
                    return STATE_ENABLED;
                case BluetoothAdapter.STATE_TURNING_ON:
                    return STATE_TURNING_ON;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    return STATE_TURNING_OFF;
                default:
                    return STATE_UNKNOWN;
            }
        }
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        // Update each requested appWidgetId
        RemoteViews view = buildUpdate(context, -1);

        for (int i = 0; i < appWidgetIds.length; i++) {
            appWidgetManager.updateAppWidget(appWidgetIds[i], view);
        }
    }

    @Override
    public void onEnabled(Context context) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(
                new ComponentName("com.android.settings", ".widget.SettingsAppWidgetProvider"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    @Override
    public void onDisabled(Context context) {
        Class clazz = com.android.settings.widget.SettingsAppWidgetProvider.class;
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(
                new ComponentName("com.android.settings", ".widget.SettingsAppWidgetProvider"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    /**
     * Load image for given widget and build {@link RemoteViews} for it.
     */
    static RemoteViews buildUpdate(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.widget);
        views.setOnClickPendingIntent(R.id.btn_wifi, getLaunchPendingIntent(context, appWidgetId,
                BUTTON_WIFI));
        views.setOnClickPendingIntent(R.id.btn_brightness,
                getLaunchPendingIntent(context,
                        appWidgetId, BUTTON_BRIGHTNESS));
        views.setOnClickPendingIntent(R.id.btn_sync,
                getLaunchPendingIntent(context,
                        appWidgetId, BUTTON_SYNC));
        views.setOnClickPendingIntent(R.id.btn_gps,
                getLaunchPendingIntent(context, appWidgetId, BUTTON_GPS));
        views.setOnClickPendingIntent(R.id.btn_bluetooth,
                getLaunchPendingIntent(context,
                        appWidgetId, BUTTON_BLUETOOTH));

        updateButtons(views, context);
        return views;
    }

    /**
     * Updates the widget when something changes, or when a button is pushed.
     *
     * @param context
     */
    public static void updateWidget(Context context) {
        RemoteViews views = buildUpdate(context, -1);
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        gm.updateAppWidget(THIS_APPWIDGET, views);
    }

    /**
     * Updates the buttons based on the underlying states of wifi, etc.
     *
     * @param views   The RemoteViews to update.
     * @param context
     */
    private static void updateButtons(RemoteViews views, Context context) {
        switch (sWifiState.getTriState(context)) {
            case STATE_DISABLED:
                views.setImageViewResource(R.id.img_wifi,
                                           R.drawable.ic_appwidget_settings_wifi_off);
                views.setImageViewResource(R.id.ind_wifi,
                                           R.drawable.appwidget_settings_ind_off_l);
                break;
            case STATE_ENABLED:
                views.setImageViewResource(R.id.img_wifi,
                                           R.drawable.ic_appwidget_settings_wifi_on);
                views.setImageViewResource(R.id.ind_wifi,
                                           R.drawable.appwidget_settings_ind_on_l);
                break;
            case STATE_INTERMEDIATE:
                // In the transitional state, the bottom green bar
                // shows the tri-state (on, off, transitioning), but
                // the top dark-gray-or-bright-white logo shows the
                // user's intent.  This is much easier to see in
                // sunlight.
                if (sWifiState.isTurningOn()) {
                    views.setImageViewResource(R.id.img_wifi,
                                               R.drawable.ic_appwidget_settings_wifi_on);
                    views.setImageViewResource(R.id.ind_wifi,
                                               R.drawable.appwidget_settings_ind_mid_l);
                } else {
                    views.setImageViewResource(R.id.img_wifi,
                                               R.drawable.ic_appwidget_settings_wifi_off);
                    views.setImageViewResource(R.id.ind_wifi,
                                               R.drawable.appwidget_settings_ind_off_l);
                }
                break;
        }
        if (getBrightnessMode(context)) {
            views.setImageViewResource(R.id.img_brightness,
                                       R.drawable.ic_appwidget_settings_brightness_auto);
            views.setImageViewResource(R.id.ind_brightness,
                                       R.drawable.appwidget_settings_ind_on_r);
        } else if (getBrightness(context)) {
            views.setImageViewResource(R.id.img_brightness,
                                       R.drawable.ic_appwidget_settings_brightness_on);
            views.setImageViewResource(R.id.ind_brightness,
                                       R.drawable.appwidget_settings_ind_on_r);
        } else {
            views.setImageViewResource(R.id.img_brightness,
                                       R.drawable.ic_appwidget_settings_brightness_off);
            views.setImageViewResource(R.id.ind_brightness,
                                       R.drawable.appwidget_settings_ind_off_r);
        }
        if (getSync(context)) {
            views.setImageViewResource(R.id.img_sync, R.drawable.ic_appwidget_settings_sync_on);
            views.setImageViewResource(R.id.ind_sync, R.drawable.appwidget_settings_ind_on_c);
        } else {
            views.setImageViewResource(R.id.img_sync, R.drawable.ic_appwidget_settings_sync_off);
            views.setImageViewResource(R.id.ind_sync, R.drawable.appwidget_settings_ind_off_c);
        }
        if (getGpsState(context)) {
            views.setImageViewResource(R.id.img_gps, R.drawable.ic_appwidget_settings_gps_on);
            views.setImageViewResource(R.id.ind_gps, R.drawable.appwidget_settings_ind_on_c);
        } else {
            views.setImageViewResource(R.id.img_gps, R.drawable.ic_appwidget_settings_gps_off);
            views.setImageViewResource(R.id.ind_gps, R.drawable.appwidget_settings_ind_off_c);
        }
        switch (sBluetoothState.getTriState(context)) {
            case STATE_DISABLED:
                views.setImageViewResource(R.id.img_bluetooth,
                                           R.drawable.ic_appwidget_settings_bluetooth_off);
                views.setImageViewResource(R.id.ind_bluetooth,
                                           R.drawable.appwidget_settings_ind_off_c);
                break;
            case STATE_ENABLED:
                views.setImageViewResource(R.id.img_bluetooth,
                                           R.drawable.ic_appwidget_settings_bluetooth_on);
                views.setImageViewResource(R.id.ind_bluetooth,
                                           R.drawable.appwidget_settings_ind_on_c);
                break;
            case STATE_INTERMEDIATE:
                // In the transitional state, the bottom green bar
                // shows the tri-state (on, off, transitioning), but
                // the top dark-gray-or-bright-white logo shows the
                // user's intent.  This is much easier to see in
                // sunlight.
                if (sBluetoothState.isTurningOn()) {
                    views.setImageViewResource(R.id.img_bluetooth,
                                               R.drawable.ic_appwidget_settings_bluetooth_on);
                    views.setImageViewResource(R.id.ind_bluetooth,
                                               R.drawable.appwidget_settings_ind_mid_c);
                } else {
                    views.setImageViewResource(R.id.img_bluetooth,
                                               R.drawable.ic_appwidget_settings_bluetooth_off);
                    views.setImageViewResource(R.id.ind_bluetooth,
                                               R.drawable.appwidget_settings_ind_off_c);
                }
                break;
        }
    }

    /**
     * Creates PendingIntent to notify the widget of a button click.
     *
     * @param context
     * @param appWidgetId
     * @return
     */
    private static PendingIntent getLaunchPendingIntent(Context context, int appWidgetId,
            int buttonId) {
        Intent launchIntent = new Intent();
        launchIntent.setClass(context, SettingsAppWidgetProvider.class);
        launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        launchIntent.setData(Uri.parse("custom:" + buttonId));
        PendingIntent pi = PendingIntent.getBroadcast(context, 0 /* no requestCode */,
                launchIntent, 0 /* no flags */);
        return pi;
    }

    /**
     * Receives and processes a button pressed intent or state change.
     *
     * @param context
     * @param intent  Indicates the pressed button.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            sWifiState.onActualStateChange(context, intent);
        } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
            sBluetoothState.onActualStateChange(context, intent);
        } else if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
            Uri data = intent.getData();
            int buttonId = Integer.parseInt(data.getSchemeSpecificPart());
            if (buttonId == BUTTON_WIFI) {
                sWifiState.toggleState(context);
            } else if (buttonId == BUTTON_BRIGHTNESS) {
                toggleBrightness(context);
            } else if (buttonId == BUTTON_SYNC) {
                toggleSync(context);
            } else if (buttonId == BUTTON_GPS) {
                toggleGps(context);
            } else if (buttonId == BUTTON_BLUETOOTH) {
                sBluetoothState.toggleState(context);
            }
        } else {
            // Don't fall-through to updating the widget.  The Intent
            // was something unrelated or that our super class took
            // care of.
            return;
        }

        // State changes fall through
        updateWidget(context);
    }

    /**
     * Gets the state of background data.
     *
     * @param context
     * @return true if enabled
     */
    private static boolean getBackgroundDataState(Context context) {
        ConnectivityManager connManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connManager.getBackgroundDataSetting();
    }

    /**
     * Gets the state of auto-sync.
     *
     * @param context
     * @return true if enabled
     */
    private static boolean getSync(Context context) {
        boolean backgroundData = getBackgroundDataState(context);
        boolean sync = ContentResolver.getMasterSyncAutomatically();
        return backgroundData && sync;
    }

    /**
     * Toggle auto-sync
     *
     * @param context
     */
    private void toggleSync(Context context) {
        ConnectivityManager connManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean backgroundData = getBackgroundDataState(context);
        boolean sync = ContentResolver.getMasterSyncAutomatically();

        // four cases to handle:
        // setting toggled from off to on:
        // 1. background data was off, sync was off: turn on both
        if (!backgroundData && !sync) {
            connManager.setBackgroundDataSetting(true);
            ContentResolver.setMasterSyncAutomatically(true);
        }

        // 2. background data was off, sync was on: turn on background data
        if (!backgroundData && sync) {
            connManager.setBackgroundDataSetting(true);
        }

        // 3. background data was on, sync was off: turn on sync
        if (backgroundData && !sync) {
            ContentResolver.setMasterSyncAutomatically(true);
        }

        // setting toggled from on to off:
        // 4. background data was on, sync was on: turn off sync
        if (backgroundData && sync) {
            ContentResolver.setMasterSyncAutomatically(false);
        }
    }

    /**
     * Gets the state of GPS location.
     *
     * @param context
     * @return true if enabled.
     */
    private static boolean getGpsState(Context context) {
        ContentResolver resolver = context.getContentResolver();
        return Settings.Secure.isLocationProviderEnabled(resolver, LocationManager.GPS_PROVIDER);
    }

    /**
     * Toggles the state of GPS.
     *
     * @param context
     */
    private void toggleGps(Context context) {
        ContentResolver resolver = context.getContentResolver();
        boolean enabled = getGpsState(context);
        Settings.Secure.setLocationProviderEnabled(resolver, LocationManager.GPS_PROVIDER,
                !enabled);
    }

    /**
     * Gets state of brightness.
     *
     * @param context
     * @return true if more than moderately bright.
     */
    private static boolean getBrightness(Context context) {
        try {
            IPowerManager power = IPowerManager.Stub.asInterface(
                    ServiceManager.getService("power"));
            if (power != null) {
                int brightness = Settings.System.getInt(context.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS);
                return brightness > 100;
            }
        } catch (Exception e) {
            Log.d(TAG, "getBrightness: " + e);
        }
        return false;
    }

    /**
     * Gets state of brightness mode.
     *
     * @param context
     * @return true if auto brightness is on.
     */
    private static boolean getBrightnessMode(Context context) {
        try {
            IPowerManager power = IPowerManager.Stub.asInterface(
                    ServiceManager.getService("power"));
            if (power != null) {
                int brightnessMode = Settings.System.getInt(context.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE);
                return brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
            }
        } catch (Exception e) {
            Log.d(TAG, "getBrightnessMode: " + e);
        }
        return false;
    }

    /**
     * Increases or decreases the brightness.
     *
     * @param context
     */
    private void toggleBrightness(Context context) {
        try {
            IPowerManager power = IPowerManager.Stub.asInterface(
                    ServiceManager.getService("power"));
            if (power != null) {
                ContentResolver cr = context.getContentResolver();
                int brightness = Settings.System.getInt(cr,
                        Settings.System.SCREEN_BRIGHTNESS);
                int brightnessMode = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
                //Only get brightness setting if available
                if (context.getResources().getBoolean(
                        com.android.internal.R.bool.config_automatic_brightness_available)) {
                    brightnessMode = Settings.System.getInt(cr,
                            Settings.System.SCREEN_BRIGHTNESS_MODE);
                }

                // Rotate AUTO -> MINIMUM -> DEFAULT -> MAXIMUM
                // Technically, not a toggle...
                if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    brightness = MINIMUM_BACKLIGHT;
                    brightnessMode = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
                } else if (brightness < DEFAULT_BACKLIGHT) {
                    brightness = DEFAULT_BACKLIGHT;
                } else if (brightness < MAXIMUM_BACKLIGHT) {
                    brightness = MAXIMUM_BACKLIGHT;
                } else {
                    brightnessMode = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
                    brightness = MINIMUM_BACKLIGHT;
                }

                if (context.getResources().getBoolean(
                        com.android.internal.R.bool.config_automatic_brightness_available)) {
                    // Set screen brightness mode (automatic or manual)
                    Settings.System.putInt(context.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            brightnessMode);
                } else {
                    // Make sure we set the brightness if automatic mode isn't available
                    brightnessMode = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
                }
                if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) {
                    power.setBacklightBrightness(brightness);
                    Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, brightness);
                }
            }
        } catch (RemoteException e) {
            Log.d(TAG, "toggleBrightness: " + e);
        } catch (Settings.SettingNotFoundException e) {
            Log.d(TAG, "toggleBrightness: " + e);
        }
    }
}
