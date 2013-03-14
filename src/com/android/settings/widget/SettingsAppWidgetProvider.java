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
import android.content.Intent;
import android.database.ContentObserver;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.settings.R;
import com.android.settings.bluetooth.LocalBluetoothAdapter;
import com.android.settings.bluetooth.LocalBluetoothManager;

/**
 * Provides control of power-related settings from a widget.
 */
public class SettingsAppWidgetProvider extends AppWidgetProvider {
    static final String TAG = "SettingsAppWidgetProvider";

    static final ComponentName THIS_APPWIDGET =
            new ComponentName("com.android.settings",
                    "com.android.settings.widget.SettingsAppWidgetProvider");

    private static LocalBluetoothAdapter sLocalBluetoothAdapter = null;

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

    // Position in the widget bar, to enable different graphics for left, center and right buttons
    private static final int POS_LEFT = 0;
    private static final int POS_CENTER = 1;
    private static final int POS_RIGHT = 2;

    private static final int[] IND_DRAWABLE_OFF = {
        R.drawable.appwidget_settings_ind_off_l_holo,
        R.drawable.appwidget_settings_ind_off_c_holo,
        R.drawable.appwidget_settings_ind_off_r_holo
    };

    private static final int[] IND_DRAWABLE_MID = {
        R.drawable.appwidget_settings_ind_mid_l_holo,
        R.drawable.appwidget_settings_ind_mid_c_holo,
        R.drawable.appwidget_settings_ind_mid_r_holo
    };

    private static final int[] IND_DRAWABLE_ON = {
        R.drawable.appwidget_settings_ind_on_l_holo,
        R.drawable.appwidget_settings_ind_on_c_holo,
        R.drawable.appwidget_settings_ind_on_r_holo
    };

    /** Minimum brightness at which the indicator is shown at half-full and ON */
    private static final float HALF_BRIGHTNESS_THRESHOLD = 0.3f;
    /** Minimum brightness at which the indicator is shown at full */
    private static final float FULL_BRIGHTNESS_THRESHOLD = 0.8f;

    private static final StateTracker sWifiState = new WifiStateTracker();
    private static final StateTracker sBluetoothState = new BluetoothStateTracker();
    private static final StateTracker sGpsState = new GpsStateTracker();
    private static final StateTracker sSyncState = new SyncStateTracker();
    private static SettingsObserver sSettingsObserver;

    /**
     * The state machine for a setting's toggling, tracking reality
     * versus the user's intent.
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
         * Return the ID of the clickable container for the setting.
         */
        public abstract int getContainerId();

        /**
         * Return the ID of the main large image button for the setting.
         */
        public abstract int getButtonId();

        /**
         * Returns the small indicator image ID underneath the setting.
         */
        public abstract int getIndicatorId();

        /**
         * Returns the resource ID of the setting's content description.
         */
        public abstract int getButtonDescription();

        /**
         * Returns the resource ID of the image to show as a function of
         * the on-vs-off state.
         */
        public abstract int getButtonImageId(boolean on);

        /**
         * Returns the position in the button bar - either POS_LEFT, POS_RIGHT or POS_CENTER.
         */
        public int getPosition() { return POS_CENTER; }

        /**
         * Updates the remote views depending on the state (off, on,
         * turning off, turning on) of the setting.
         */
        public final void setImageViewResources(Context context, RemoteViews views) {
            int containerId = getContainerId();
            int buttonId = getButtonId();
            int indicatorId = getIndicatorId();
            int pos = getPosition();
            switch (getTriState(context)) {
                case STATE_DISABLED:
                    views.setContentDescription(containerId,
                        getContentDescription(context, R.string.gadget_state_off));
                    views.setImageViewResource(buttonId, getButtonImageId(false));
                    views.setImageViewResource(
                        indicatorId, IND_DRAWABLE_OFF[pos]);
                    break;
                case STATE_ENABLED:
                    views.setContentDescription(containerId,
                        getContentDescription(context, R.string.gadget_state_on));
                    views.setImageViewResource(buttonId, getButtonImageId(true));
                    views.setImageViewResource(
                        indicatorId, IND_DRAWABLE_ON[pos]);
                    break;
                case STATE_INTERMEDIATE:
                    // In the transitional state, the bottom green bar
                    // shows the tri-state (on, off, transitioning), but
                    // the top dark-gray-or-bright-white logo shows the
                    // user's intent.  This is much easier to see in
                    // sunlight.
                    if (isTurningOn()) {
                        views.setContentDescription(containerId,
                            getContentDescription(context, R.string.gadget_state_turning_on));
                        views.setImageViewResource(buttonId, getButtonImageId(true));
                        views.setImageViewResource(
                            indicatorId, IND_DRAWABLE_MID[pos]);
                    } else {
                        views.setContentDescription(containerId,
                            getContentDescription(context, R.string.gadget_state_turning_off));
                        views.setImageViewResource(buttonId, getButtonImageId(false));
                        views.setImageViewResource(
                            indicatorId, IND_DRAWABLE_OFF[pos]);
                    }
                    break;
            }
        }

        /**
         * Returns the gadget state template populated with the gadget
         * description and state.
         */
        private final String getContentDescription(Context context, int stateResId) {
            final String gadget = context.getString(getButtonDescription());
            final String state = context.getString(stateResId);
            return context.getString(R.string.gadget_state_template, gadget, state);
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
        public int getContainerId() { return R.id.btn_wifi; }
        public int getButtonId() { return R.id.img_wifi; }
        public int getIndicatorId() { return R.id.ind_wifi; }
        public int getButtonDescription() { return R.string.gadget_wifi; }
        public int getButtonImageId(boolean on) {
            return on ? R.drawable.ic_appwidget_settings_wifi_on_holo
                    : R.drawable.ic_appwidget_settings_wifi_off_holo;
        }

        @Override
        public int getPosition() { return POS_LEFT; }

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
            final WifiManager wifiManager =
                    (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
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
        public int getContainerId() { return R.id.btn_bluetooth; }
        public int getButtonId() { return R.id.img_bluetooth; }
        public int getIndicatorId() { return R.id.ind_bluetooth; }
        public int getButtonDescription() { return R.string.gadget_bluetooth; }
        public int getButtonImageId(boolean on) {
            return on ? R.drawable.ic_appwidget_settings_bluetooth_on_holo
                    : R.drawable.ic_appwidget_settings_bluetooth_off_holo;
        }

        @Override
        public int getActualState(Context context) {
            if (sLocalBluetoothAdapter == null) {
                LocalBluetoothManager manager = LocalBluetoothManager.getInstance(context);
                if (manager == null) {
                    return STATE_UNKNOWN;  // On emulator?
                }
                sLocalBluetoothAdapter = manager.getBluetoothAdapter();
            }
            return bluetoothStateToFiveState(sLocalBluetoothAdapter.getBluetoothState());
        }

        @Override
        protected void requestStateChange(Context context, final boolean desiredState) {
            if (sLocalBluetoothAdapter == null) {
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
                    sLocalBluetoothAdapter.setBluetoothEnabled(desiredState);
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

    /**
     * Subclass of StateTracker for GPS state.
     */
    private static final class GpsStateTracker extends StateTracker {
        public int getContainerId() { return R.id.btn_gps; }
        public int getButtonId() { return R.id.img_gps; }
        public int getIndicatorId() { return R.id.ind_gps; }
        public int getButtonDescription() { return R.string.gadget_gps; }
        public int getButtonImageId(boolean on) {
            return on ? R.drawable.ic_appwidget_settings_gps_on_holo
                    : R.drawable.ic_appwidget_settings_gps_off_holo;
        }

        @Override
        public int getActualState(Context context) {
            ContentResolver resolver = context.getContentResolver();
            boolean on = Settings.Secure.isLocationProviderEnabled(
                resolver, LocationManager.GPS_PROVIDER);
            return on ? STATE_ENABLED : STATE_DISABLED;
        }

        @Override
        public void onActualStateChange(Context context, Intent unused) {
            // Note: the broadcast location providers changed intent
            // doesn't include an extras bundles saying what the new value is.
            setCurrentState(context, getActualState(context));
        }

        @Override
        public void requestStateChange(final Context context, final boolean desiredState) {
            final ContentResolver resolver = context.getContentResolver();
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... args) {
                    final UserManager um =
                            (UserManager) context.getSystemService(Context.USER_SERVICE);
                    if (!um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION)) {
                        Settings.Secure.setLocationProviderEnabled(
                            resolver,
                            LocationManager.GPS_PROVIDER,
                            desiredState);
                        return desiredState;
                    }
                    return Settings.Secure.isLocationProviderEnabled(
                            resolver,
                            LocationManager.GPS_PROVIDER);
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    setCurrentState(
                        context,
                        result ? STATE_ENABLED : STATE_DISABLED);
                    updateWidget(context);
                }
            }.execute();
        }
    }

    /**
     * Subclass of StateTracker for sync state.
     */
    private static final class SyncStateTracker extends StateTracker {
        public int getContainerId() { return R.id.btn_sync; }
        public int getButtonId() { return R.id.img_sync; }
        public int getIndicatorId() { return R.id.ind_sync; }
        public int getButtonDescription() { return R.string.gadget_sync; }
        public int getButtonImageId(boolean on) {
            return on ? R.drawable.ic_appwidget_settings_sync_on_holo
                    : R.drawable.ic_appwidget_settings_sync_off_holo;
        }

        @Override
        public int getActualState(Context context) {
            boolean on = ContentResolver.getMasterSyncAutomatically();
            return on ? STATE_ENABLED : STATE_DISABLED;
        }

        @Override
        public void onActualStateChange(Context context, Intent unused) {
            setCurrentState(context, getActualState(context));
        }

        @Override
        public void requestStateChange(final Context context, final boolean desiredState) {
            final ConnectivityManager connManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final boolean sync = ContentResolver.getMasterSyncAutomatically();

            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... args) {
                    // Turning sync on.
                    if (desiredState) {
                        if (!sync) {
                            ContentResolver.setMasterSyncAutomatically(true);
                        }
                        return true;
                    }

                    // Turning sync off
                    if (sync) {
                        ContentResolver.setMasterSyncAutomatically(false);
                    }
                    return false;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    setCurrentState(
                        context,
                        result ? STATE_ENABLED : STATE_DISABLED);
                    updateWidget(context);
                }
            }.execute();
        }
    }

    private static void checkObserver(Context context) {
        if (sSettingsObserver == null) {
            sSettingsObserver = new SettingsObserver(new Handler(),
                    context.getApplicationContext());
            sSettingsObserver.startObserving();
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        // Update each requested appWidgetId
        RemoteViews view = buildUpdate(context);

        for (int i = 0; i < appWidgetIds.length; i++) {
            appWidgetManager.updateAppWidget(appWidgetIds[i], view);
        }
    }

    @Override
    public void onEnabled(Context context) {
        checkObserver(context);
    }

    @Override
    public void onDisabled(Context context) {
        if (sSettingsObserver != null) {
            sSettingsObserver.stopObserving();
            sSettingsObserver = null;
        }
    }

    /**
     * Load image for given widget and build {@link RemoteViews} for it.
     */
    static RemoteViews buildUpdate(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.widget);
        views.setOnClickPendingIntent(R.id.btn_wifi, getLaunchPendingIntent(context,
                BUTTON_WIFI));
        views.setOnClickPendingIntent(R.id.btn_brightness,
                getLaunchPendingIntent(context,
                        BUTTON_BRIGHTNESS));
        views.setOnClickPendingIntent(R.id.btn_sync,
                getLaunchPendingIntent(context,
                        BUTTON_SYNC));
        views.setOnClickPendingIntent(R.id.btn_gps,
                getLaunchPendingIntent(context, BUTTON_GPS));
        views.setOnClickPendingIntent(R.id.btn_bluetooth,
                getLaunchPendingIntent(context,
                        BUTTON_BLUETOOTH));

        updateButtons(views, context);
        return views;
    }

    /**
     * Updates the widget when something changes, or when a button is pushed.
     *
     * @param context
     */
    public static void updateWidget(Context context) {
        RemoteViews views = buildUpdate(context);
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        gm.updateAppWidget(THIS_APPWIDGET, views);
        checkObserver(context);
    }

    /**
     * Updates the buttons based on the underlying states of wifi, etc.
     *
     * @param views   The RemoteViews to update.
     * @param context
     */
    private static void updateButtons(RemoteViews views, Context context) {
        sWifiState.setImageViewResources(context, views);
        sBluetoothState.setImageViewResources(context, views);
        sGpsState.setImageViewResources(context, views);
        sSyncState.setImageViewResources(context, views);

        if (getBrightnessMode(context)) {
            views.setContentDescription(R.id.btn_brightness,
                    context.getString(R.string.gadget_brightness_template,
                            context.getString(R.string.gadget_brightness_state_auto)));
            views.setImageViewResource(R.id.img_brightness,
                    R.drawable.ic_appwidget_settings_brightness_auto_holo);
            views.setImageViewResource(R.id.ind_brightness,
                    R.drawable.appwidget_settings_ind_on_r_holo);
        } else {
            final int brightness = getBrightness(context);
            final PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            // Set the icon
            final int full = (int)(pm.getMaximumScreenBrightnessSetting()
                    * FULL_BRIGHTNESS_THRESHOLD);
            final int half = (int)(pm.getMaximumScreenBrightnessSetting()
                    * HALF_BRIGHTNESS_THRESHOLD);
            if (brightness > full) {
                views.setContentDescription(R.id.btn_brightness,
                        context.getString(R.string.gadget_brightness_template,
                                context.getString(R.string.gadget_brightness_state_full)));
                views.setImageViewResource(R.id.img_brightness,
                        R.drawable.ic_appwidget_settings_brightness_full_holo);
            } else if (brightness > half) {
                views.setContentDescription(R.id.btn_brightness,
                        context.getString(R.string.gadget_brightness_template,
                                context.getString(R.string.gadget_brightness_state_half)));
                views.setImageViewResource(R.id.img_brightness,
                        R.drawable.ic_appwidget_settings_brightness_half_holo);
            } else {
                views.setContentDescription(R.id.btn_brightness,
                        context.getString(R.string.gadget_brightness_template,
                                context.getString(R.string.gadget_brightness_state_off)));
                views.setImageViewResource(R.id.img_brightness,
                        R.drawable.ic_appwidget_settings_brightness_off_holo);
            }
            // Set the ON state
            if (brightness > half) {
                views.setImageViewResource(R.id.ind_brightness,
                        R.drawable.appwidget_settings_ind_on_r_holo);
            } else {
                views.setImageViewResource(R.id.ind_brightness,
                        R.drawable.appwidget_settings_ind_off_r_holo);
            }
        }
    }

    /**
     * Creates PendingIntent to notify the widget of a button click.
     *
     * @param context
     * @return
     */
    private static PendingIntent getLaunchPendingIntent(Context context,
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
        String action = intent.getAction();
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            sWifiState.onActualStateChange(context, intent);
        } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            sBluetoothState.onActualStateChange(context, intent);
        } else if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(action)) {
            sGpsState.onActualStateChange(context, intent);
        } else if (ContentResolver.ACTION_SYNC_CONN_STATUS_CHANGED.equals(action)) {
            sSyncState.onActualStateChange(context, intent);
        } else if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
            Uri data = intent.getData();
            int buttonId = Integer.parseInt(data.getSchemeSpecificPart());
            if (buttonId == BUTTON_WIFI) {
                sWifiState.toggleState(context);
            } else if (buttonId == BUTTON_BRIGHTNESS) {
                toggleBrightness(context);
            } else if (buttonId == BUTTON_SYNC) {
                sSyncState.toggleState(context);
            } else if (buttonId == BUTTON_GPS) {
                sGpsState.toggleState(context);
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
     * Gets brightness level.
     *
     * @param context
     * @return brightness level between 0 and 255.
     */
    private static int getBrightness(Context context) {
        try {
            int brightness = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
            return brightness;
        } catch (Exception e) {
        }
        return 0;
    }

    /**
     * Gets state of brightness mode.
     *
     * @param context
     * @return true if auto brightness is on.
     */
    private static boolean getBrightnessMode(Context context) {
        try {
            int brightnessMode = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE);
            return brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
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
                PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);

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
                    brightness = pm.getMinimumScreenBrightnessSetting();
                    brightnessMode = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
                } else if (brightness < pm.getDefaultScreenBrightnessSetting()) {
                    brightness = pm.getDefaultScreenBrightnessSetting();
                } else if (brightness < pm.getMaximumScreenBrightnessSetting()) {
                    brightness = pm.getMaximumScreenBrightnessSetting();
                } else {
                    brightnessMode = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
                    brightness = pm.getMinimumScreenBrightnessSetting();
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
                    power.setTemporaryScreenBrightnessSettingOverride(brightness);
                    Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, brightness);
                }
            }
        } catch (RemoteException e) {
            Log.d(TAG, "toggleBrightness: " + e);
        } catch (Settings.SettingNotFoundException e) {
            Log.d(TAG, "toggleBrightness: " + e);
        }
    }

    /** Observer to watch for changes to the BRIGHTNESS setting */
    private static class SettingsObserver extends ContentObserver {

        private Context mContext;

        SettingsObserver(Handler handler, Context context) {
            super(handler);
            mContext = context;
        }

        void startObserving() {
            ContentResolver resolver = mContext.getContentResolver();
            // Listen to brightness and brightness mode
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.SCREEN_BRIGHTNESS), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), false, this);
        }

        void stopObserving() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateWidget(mContext);
        }
    }

}
