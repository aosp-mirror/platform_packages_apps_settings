/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wifi.calling;

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PersistableBundle;
import android.provider.Settings;
import androidx.core.graphics.drawable.IconCompat;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.ims.ImsManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.slices.SettingsSliceProvider;
import com.android.settings.slices.SliceBroadcastReceiver;
import com.android.settings.slices.SliceBuilderUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Helper class to control slices for wifi calling settings.
 */
public class WifiCallingSliceHelper {

    private static final String TAG = "WifiCallingSliceHelper";

    /**
     * Settings slice path to wifi calling setting.
     */
    public static final String PATH_WIFI_CALLING = "wifi_calling";

    /**
     * Action passed for changes to wifi calling slice (toggle).
     */
    public static final String ACTION_WIFI_CALLING_CHANGED =
            "com.android.settings.wifi.calling.action.WIFI_CALLING_CHANGED";

    /**
     * Action for Wifi calling Settings activity which
     * allows setting configuration for Wifi calling
     * related settings
     */
    public static final String ACTION_WIFI_CALLING_SETTINGS_ACTIVITY =
            "android.settings.WIFI_CALLING_SETTINGS";

    /**
     * Full {@link Uri} for the Wifi Calling Slice.
     */
    public static final Uri WIFI_CALLING_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(PATH_WIFI_CALLING)
            .build();

    /**
     * Timeout for querying wifi calling setting from ims manager.
     */
    private static final int TIMEOUT_MILLIS = 2000;

    protected SubscriptionManager mSubscriptionManager;
    private final Context mContext;

    @VisibleForTesting
    public WifiCallingSliceHelper(Context context) {
        mContext = context;
    }

    /**
     * Returns Slice object for wifi calling settings.
     *
     * If wifi calling is being turned on and if wifi calling activation is needed for the current
     * carrier, this method will return Slice with instructions to go to Settings App.
     *
     * If wifi calling is not supported for the current carrier, this method will return slice with
     * not supported message.
     *
     * If wifi calling setting can be changed, this method will return the slice to toggle wifi
     * calling option with ACTION_WIFI_CALLING_CHANGED as endItem.
     */
    public Slice createWifiCallingSlice(Uri sliceUri) {
        final int subId = getDefaultVoiceSubId();
        final String carrierName = getSimCarrierName();

        if (subId <= SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.d(TAG, "Invalid subscription Id");
            return getNonActionableWifiCallingSlice(
                    mContext.getString(R.string.wifi_calling_settings_title),
                    mContext.getString(R.string.wifi_calling_not_supported, carrierName),
                    sliceUri, getSettingsIntent(mContext));
        }

        final ImsManager imsManager = getImsManager(subId);

        if (!imsManager.isWfcEnabledByPlatform()
                || !imsManager.isWfcProvisionedOnDevice()) {
            Log.d(TAG, "Wifi calling is either not provisioned or not enabled by Platform");
            return getNonActionableWifiCallingSlice(
                    mContext.getString(R.string.wifi_calling_settings_title),
                    mContext.getString(R.string.wifi_calling_not_supported, carrierName),
                    sliceUri, getSettingsIntent(mContext));
        }

        try {
            final boolean isWifiCallingEnabled = isWifiCallingEnabled(imsManager);
            final Intent activationAppIntent =
                    getWifiCallingCarrierActivityIntent(subId);

            // Send this actionable wifi calling slice to toggle the setting
            // only when there is no need for wifi calling activation with the server
            if (activationAppIntent != null && !isWifiCallingEnabled) {
                Log.d(TAG, "Needs Activation");
                // Activation needed for the next action of the user
                // Give instructions to go to settings app
                return getNonActionableWifiCallingSlice(
                        mContext.getString(R.string.wifi_calling_settings_title),
                        mContext.getString(
                                R.string.wifi_calling_settings_activation_instructions),
                        sliceUri, getActivityIntent(ACTION_WIFI_CALLING_SETTINGS_ACTIVITY));
            }
            return getWifiCallingSlice(sliceUri, mContext, isWifiCallingEnabled);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            Log.e(TAG, "Unable to read the current WiFi calling status", e);
            return getNonActionableWifiCallingSlice(
                    mContext.getString(R.string.wifi_calling_settings_title),
                    mContext.getString(R.string.wifi_calling_turn_on),
                    sliceUri, getActivityIntent(ACTION_WIFI_CALLING_SETTINGS_ACTIVITY));
        }
    }

    private boolean isWifiCallingEnabled(ImsManager imsManager)
            throws InterruptedException, ExecutionException, TimeoutException {
        final FutureTask<Boolean> isWifiOnTask = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return imsManager.isWfcEnabledByUser();
            }
        });
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(isWifiOnTask);

        Boolean isWifiEnabledByUser = false;
        isWifiEnabledByUser = isWifiOnTask.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        return isWifiEnabledByUser && imsManager.isNonTtyOrTtyOnVolteEnabled();
    }

    /**
     * Builds a toggle slice where the intent takes you to the wifi calling page and the toggle
     * enables/disables wifi calling.
     */
    private Slice getWifiCallingSlice(Uri sliceUri, Context mContext,
            boolean isWifiCallingEnabled) {

        final IconCompat icon = IconCompat.createWithResource(mContext, R.drawable.wifi_signal);
        final String title = mContext.getString(R.string.wifi_calling_settings_title);
        return new ListBuilder(mContext, sliceUri, ListBuilder.INFINITY)
                .setColor(R.color.material_blue_500)
                .addRow(b -> b
                        .setTitle(title)
                        .addEndItem(
                                new SliceAction(
                                        getBroadcastIntent(ACTION_WIFI_CALLING_CHANGED),
                                        null /* actionTitle */, isWifiCallingEnabled))
                        .setPrimaryAction(new SliceAction(
                                getActivityIntent(ACTION_WIFI_CALLING_SETTINGS_ACTIVITY),
                                icon,
                                title)))
                .build();
    }

    protected ImsManager getImsManager(int subId) {
        return ImsManager.getInstance(mContext, SubscriptionManager.getPhoneId(subId));
    }

    private Integer getWfcMode(ImsManager imsManager)
            throws InterruptedException, ExecutionException, TimeoutException {
        FutureTask<Integer> wfcModeTask = new FutureTask<>(new Callable<Integer>() {
            @Override
            public Integer call() {
                return imsManager.getWfcMode(false);
            }
        });
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(wfcModeTask);
        return wfcModeTask.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * Handles wifi calling setting change from wifi calling slice and posts notification. Should be
     * called when intent action is ACTION_WIFI_CALLING_CHANGED. Executed in @WorkerThread
     *
     * @param intent action performed
     */
    public void handleWifiCallingChanged(Intent intent) {
        final int subId = getDefaultVoiceSubId();

        if (subId > SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            final ImsManager imsManager = getImsManager(subId);
            if (imsManager.isWfcEnabledByPlatform()
                    || imsManager.isWfcProvisionedOnDevice()) {
                final boolean currentValue = imsManager.isWfcEnabledByUser()
                        && imsManager.isNonTtyOrTtyOnVolteEnabled();
                final boolean newValue = intent.getBooleanExtra(EXTRA_TOGGLE_STATE,
                        currentValue);
                final Intent activationAppIntent =
                        getWifiCallingCarrierActivityIntent(subId);
                if (!newValue || activationAppIntent == null) {
                    // If either the action is to turn off wifi calling setting
                    // or there is no activation involved - Update the setting
                    if (newValue != currentValue) {
                        imsManager.setWfcSetting(newValue);
                    }
                }
            }
        }
        // notify change in slice in any case to get re-queried. This would result in displaying
        // appropriate message with the updated setting.
        final Uri uri = SliceBuilderUtils.getUri(PATH_WIFI_CALLING, false /*isPlatformSlice*/);
        mContext.getContentResolver().notifyChange(uri, null);
    }

    /**
     * Returns Slice with the title and subtitle provided as arguments with wifi signal Icon.
     *
     * @param title Title of the slice
     * @param subtitle Subtitle of the slice
     * @param sliceUri slice uri
     * @return Slice with title and subtitle
     */
    // TODO(b/79548264) asses different scenarios and return null instead of non-actionable slice
    private Slice getNonActionableWifiCallingSlice(String title, String subtitle, Uri sliceUri,
            PendingIntent primaryActionIntent) {
        final IconCompat icon = IconCompat.createWithResource(mContext, R.drawable.wifi_signal);
        return new ListBuilder(mContext, sliceUri, ListBuilder.INFINITY)
                .setColor(R.color.material_blue_500)
                .addRow(b -> b
                        .setTitle(title)
                        .setSubtitle(subtitle)
                        .setPrimaryAction(new SliceAction(
                                primaryActionIntent, icon,
                                title)))
                .build();
    }

    /**
     * Returns {@code true} when the key is enabled for the carrier, and {@code false} otherwise.
     */
    private boolean isCarrierConfigManagerKeyEnabled(Context mContext, String key,
            int subId, boolean defaultValue) {
        final CarrierConfigManager configManager = getCarrierConfigManager(mContext);
        boolean ret = false;
        if (configManager != null) {
            final PersistableBundle bundle = configManager.getConfigForSubId(subId);
            if (bundle != null) {
                ret = bundle.getBoolean(key, defaultValue);
            }
        }
        return ret;
    }

    protected CarrierConfigManager getCarrierConfigManager(Context mContext) {
        return mContext.getSystemService(CarrierConfigManager.class);
    }

    /**
     * Returns the current default voice subId obtained from SubscriptionManager
     */
    protected int getDefaultVoiceSubId() {
        if (mSubscriptionManager == null) {
            mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        }
        return SubscriptionManager.getDefaultVoiceSubscriptionId();
    }

    /**
     * Returns Intent of the activation app required to activate wifi calling or null if there is no
     * need for activation.
     */
    protected Intent getWifiCallingCarrierActivityIntent(int subId) {
        final CarrierConfigManager configManager = getCarrierConfigManager(mContext);
        if (configManager == null) {
            return null;
        }

        final PersistableBundle bundle = configManager.getConfigForSubId(subId);
        if (bundle == null) {
            return null;
        }

        final String carrierApp = bundle.getString(
                CarrierConfigManager.KEY_WFC_EMERGENCY_ADDRESS_CARRIER_APP_STRING);
        if (TextUtils.isEmpty(carrierApp)) {
            return null;
        }

        final ComponentName componentName = ComponentName.unflattenFromString(carrierApp);
        if (componentName == null) {
            return null;
        }

        final Intent intent = new Intent();
        intent.setComponent(componentName);
        return intent;
    }

    /**
     * @return {@link PendingIntent} to the Settings home page.
     */
    public static PendingIntent getSettingsIntent(Context context) {
        final Intent intent = new Intent(Settings.ACTION_SETTINGS);
        return PendingIntent.getActivity(context, 0 /* requestCode */, intent, 0 /* flags */);
    }

    private PendingIntent getBroadcastIntent(String action) {
        final Intent intent = new Intent(action);
        intent.setClass(mContext, SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(mContext, 0 /* requestCode */, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    /**
     * Returns PendingIntent to start activity specified by action
     */
    private PendingIntent getActivityIntent(String action) {
        final Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(mContext, 0 /* requestCode */, intent, 0 /* flags */);
    }

    /**
     * Returns carrier id name of the current Subscription
     */
    private String getSimCarrierName() {
        final TelephonyManager telephonyManager = mContext.getSystemService(TelephonyManager.class);
        final CharSequence carrierName = telephonyManager.getSimCarrierIdName();
        if (carrierName == null) {
            return mContext.getString(R.string.carrier);
        }
        return carrierName.toString();
    }

}
