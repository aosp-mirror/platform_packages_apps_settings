/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.settings.development.qstile;

import static com.android.settings.development.AdbPreferenceController.ADB_SETTING_OFF;
import static com.android.settings.development.AdbPreferenceController.ADB_SETTING_ON;

import android.app.KeyguardManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.hardware.SensorPrivacyManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.sysprop.DisplayProperties;
import android.util.Log;
import android.view.IWindowManager;
import android.view.ThreadedRenderer;
import android.view.WindowManagerGlobal;
import android.widget.Toast;

import com.android.internal.app.LocalePicker;
import com.android.internal.statusbar.IStatusBarService;
import com.android.settings.development.WirelessDebuggingPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.development.SystemPropPoker;

public abstract class DevelopmentTiles extends TileService {

    /**
     * Meta-data for a development tile to declare a sysprop flag that needs to be enabled for
     * the tile to be available.
     *
     * To define the flag, set this meta-data on the tile's manifest declaration.
     * <pre class="prettyprint">
     * {@literal
     * <meta-data android:name="com.android.settings.development.qstile.REQUIRES_SYSTEM_PROPERTY"
     *     android:value="persist.debug.flag_name_here" />
     * }
     * </pre>
     */
    public static final String META_DATA_REQUIRES_SYSTEM_PROPERTY =
            "com.android.settings.development.qstile.REQUIRES_SYSTEM_PROPERTY";

    private static final String TAG = "DevelopmentTiles";

    protected abstract boolean isEnabled();

    protected abstract void setIsEnabled(boolean isEnabled);

    @Override
    public void onStartListening() {
        super.onStartListening();
        refresh();
    }

    public void refresh() {
        final int state;
        if (!DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(this)) {
            // Reset to disabled state if dev option is off.
            if (isEnabled()) {
                setIsEnabled(false);
                SystemPropPoker.getInstance().poke();
            }
            final ComponentName cn = new ComponentName(getPackageName(), getClass().getName());
            try {
                getPackageManager().setComponentEnabledSetting(
                        cn, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                final IStatusBarService statusBarService = IStatusBarService.Stub.asInterface(
                        ServiceManager.checkService(Context.STATUS_BAR_SERVICE));
                if (statusBarService != null) {
                    statusBarService.remTile(cn);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to modify QS tile for component " +
                        cn.toString(), e);
            }
            state = Tile.STATE_UNAVAILABLE;
        } else {
            state = isEnabled() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
        }
        getQsTile().setState(state);
        getQsTile().updateTile();
    }

    @Override
    public void onClick() {
        setIsEnabled(getQsTile().getState() == Tile.STATE_INACTIVE);
        SystemPropPoker.getInstance().poke(); // Settings app magic
        refresh();
    }

    /**
     * Tile to control the "Show layout bounds" developer setting
     */
    public static class ShowLayout extends DevelopmentTiles {

        @Override
        protected boolean isEnabled() {
            return DisplayProperties.debug_layout().orElse(false);
        }

        @Override
        protected void setIsEnabled(boolean isEnabled) {
            DisplayProperties.debug_layout(isEnabled);
        }
    }

    /**
     * Tile to control the "GPU profiling" developer setting
     */
    public static class GPUProfiling extends DevelopmentTiles {

        @Override
        protected boolean isEnabled() {
            final String value = SystemProperties.get(ThreadedRenderer.PROFILE_PROPERTY);
            return value.equals("visual_bars");
        }

        @Override
        protected void setIsEnabled(boolean isEnabled) {
            SystemProperties.set(ThreadedRenderer.PROFILE_PROPERTY, isEnabled ? "visual_bars" : "");
        }
    }

    /**
     * Tile to control the "Force RTL" developer setting
     */
    public static class ForceRTL extends DevelopmentTiles {

        @Override
        protected boolean isEnabled() {
            return Settings.Global.getInt(
                    getContentResolver(), Settings.Global.DEVELOPMENT_FORCE_RTL, 0) != 0;
        }

        @Override
        protected void setIsEnabled(boolean isEnabled) {
            Settings.Global.putInt(
                    getContentResolver(), Settings.Global.DEVELOPMENT_FORCE_RTL, isEnabled ? 1 : 0);
            DisplayProperties.debug_force_rtl(isEnabled);
            LocalePicker.updateLocales(getResources().getConfiguration().getLocales());
        }
    }

    /**
     * Tile to control the "Animation speed" developer setting
     */
    public static class AnimationSpeed extends DevelopmentTiles {

        @Override
        protected boolean isEnabled() {
            IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
            try {
                return wm.getAnimationScale(0) != 1;
            } catch (RemoteException e) {
            }
            return false;
        }

        @Override
        protected void setIsEnabled(boolean isEnabled) {
            IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
            float scale = isEnabled ? 10 : 1;
            try {
                wm.setAnimationScale(0, scale);
                wm.setAnimationScale(1, scale);
                wm.setAnimationScale(2, scale);
            } catch (RemoteException e) {
            }
        }
    }

    /**
     * Tile to toggle sensors off to control camera, mic, and sensors managed by the SensorManager.
     */
    public static class SensorsOff extends DevelopmentTiles {
        private Context mContext;
        private SensorPrivacyManager mSensorPrivacyManager;
        private KeyguardManager mKeyguardManager;
        private MetricsFeatureProvider mMetricsFeatureProvider;
        private boolean mIsEnabled;

        @Override
        public void onCreate() {
            super.onCreate();
            mContext = getApplicationContext();
            mSensorPrivacyManager = (SensorPrivacyManager) mContext.getSystemService(
                    Context.SENSOR_PRIVACY_SERVICE);
            mIsEnabled = mSensorPrivacyManager.isAllSensorPrivacyEnabled();
            mMetricsFeatureProvider = FeatureFactory.getFeatureFactory()
                    .getMetricsFeatureProvider();
            mKeyguardManager = (KeyguardManager) mContext.getSystemService(
                    Context.KEYGUARD_SERVICE);
        }

        @Override
        protected boolean isEnabled() {
            return mIsEnabled;
        }

        @Override
        public void setIsEnabled(boolean isEnabled) {
            // Don't allow sensors to be toggled from the lock screen.
            if (mKeyguardManager.isKeyguardLocked()) {
                return;
            }
            mMetricsFeatureProvider.action(getApplicationContext(), SettingsEnums.QS_SENSOR_PRIVACY,
                    isEnabled);
            mIsEnabled = isEnabled;
            mSensorPrivacyManager.setAllSensorPrivacy(isEnabled);
        }
    }

    /**
     * Tile to control the "Wireless debugging" developer setting
     */
    public static class WirelessDebugging extends DevelopmentTiles {
        private Context mContext;
        private KeyguardManager mKeyguardManager;
        private Toast mToast;
        private final Handler mHandler = new Handler(Looper.getMainLooper());
        private final ContentObserver mSettingsObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                refresh();
            }
        };

        @Override
        public void onCreate() {
            super.onCreate();
            mContext = getApplicationContext();
            mKeyguardManager = (KeyguardManager) mContext.getSystemService(
                    Context.KEYGUARD_SERVICE);
            mToast = Toast.makeText(mContext,
                    com.android.settingslib.R.string.adb_wireless_no_network_msg,
                    Toast.LENGTH_LONG);
        }

        @Override
        public void onStartListening() {
            super.onStartListening();
            getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.ADB_WIFI_ENABLED), false,
                    mSettingsObserver);
        }

        @Override
        public void onStopListening() {
            super.onStopListening();
            getContentResolver().unregisterContentObserver(mSettingsObserver);
        }

        @Override
        protected boolean isEnabled() {
            return isAdbWifiEnabled();
        }

        @Override
        public void setIsEnabled(boolean isEnabled) {
            // Don't allow Wireless Debugging to be enabled from the lock screen.
            if (isEnabled && mKeyguardManager.isKeyguardLocked()) {
                return;
            }

            // Show error toast if not connected to Wi-Fi
            if (isEnabled && !WirelessDebuggingPreferenceController.isWifiConnected(mContext)) {
                // Close quick shade
                sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                mToast.show();
                return;
            }

            writeAdbWifiSetting(isEnabled);
        }

        private boolean isAdbWifiEnabled() {
            return Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_WIFI_ENABLED,
                    ADB_SETTING_OFF) != ADB_SETTING_OFF;
        }

        protected void writeAdbWifiSetting(boolean enabled) {
            Settings.Global.putInt(getContentResolver(), Settings.Global.ADB_WIFI_ENABLED,
                    enabled ? ADB_SETTING_ON : ADB_SETTING_OFF);
        }
    }

    /**
     * Tile to control the "Show taps" developer setting
     */
    public static class ShowTaps extends DevelopmentTiles {
        private static final int SETTING_VALUE_ON = 1;
        private static final int SETTING_VALUE_OFF = 0;
        private Context mContext;

        @Override
        public void onCreate() {
            super.onCreate();
            mContext = getApplicationContext();
        }

        @Override
        protected boolean isEnabled() {
            return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SHOW_TOUCHES, SETTING_VALUE_OFF) == SETTING_VALUE_ON;
        }

        @Override
        protected void setIsEnabled(boolean isEnabled) {
            Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SHOW_TOUCHES, isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        }
    }
}
