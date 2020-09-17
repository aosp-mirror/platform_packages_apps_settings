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

package com.android.settings.network.telephony;

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.ims.ImsMmTelManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.network.ims.VolteQueryImsState;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.SliceBroadcastReceiver;

/**
 * Helper class to control slices for enhanced 4g LTE settings.
 */
public class Enhanced4gLteSliceHelper {

    private static final String TAG = "Enhanced4gLteSlice";

    /**
     * Action passed for changes to enhanced 4g LTE slice (toggle).
     */
    public static final String ACTION_ENHANCED_4G_LTE_CHANGED =
            "com.android.settings.mobilenetwork.action.ENHANCED_4G_LTE_CHANGED";

    /**
     * Action for mobile network settings activity which
     * allows setting configuration for Enhanced 4G LTE
     * related settings
     */
    public static final String ACTION_MOBILE_NETWORK_SETTINGS_ACTIVITY =
            "android.settings.NETWORK_OPERATOR_SETTINGS";

    private final Context mContext;

    /**
     * Phone package name
     */
    private static final String PACKAGE_PHONE = "com.android.phone";

    /**
     * String resource type
     */
    private static final String RESOURCE_TYPE_STRING = "string";

    /**
     * Enhanced 4g lte mode title variant resource name
     */
    private static final String RESOURCE_ENHANCED_4G_LTE_MODE_TITLE_VARIANT =
            "enhanced_4g_lte_mode_title_variant";

    @VisibleForTesting
    public Enhanced4gLteSliceHelper(Context context) {
        mContext = context;
    }

    /**
     * Returns Slice object for enhanced_4g_lte settings.
     *
     * If enhanced 4g LTE is not supported for the current carrier, this method will return slice
     * with not supported message.
     *
     * If enhanced 4g LTE is not editable for the current carrier, this method will return slice
     * with not editable message.
     *
     * If enhanced 4g LTE setting can be changed, this method will return the slice to toggle
     * enhanced 4g LTE option with ACTION_ENHANCED_4G_LTE_CHANGED as endItem.
     */
    public Slice createEnhanced4gLteSlice(Uri sliceUri) {
        final int subId = getDefaultVoiceSubId();

        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            Log.d(TAG, "Invalid subscription Id");
            return null;
        }

        if (isCarrierConfigManagerKeyEnabled(
                CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL, subId, false)
                || !isCarrierConfigManagerKeyEnabled(
                CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, subId,
                true)) {
            Log.d(TAG, "Setting is either hidden or not editable");
            return null;
        }

        final VolteQueryImsState queryState = queryImsState(subId);
        if (!queryState.isVoLteProvisioned()) {
            Log.d(TAG, "Setting is either not provisioned or not enabled by Platform");
            return null;
        }

        try {
            return getEnhanced4gLteSlice(sliceUri,
                    queryState.isEnabledByUser(), subId);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unable to read the current Enhanced 4g LTE status", e);
            return null;
        }
    }

    /**
     * Builds a toggle slice where the intent takes you to the Enhanced 4G LTE page and the toggle
     * enables/disables Enhanced 4G LTE mode setting.
     */
    private Slice getEnhanced4gLteSlice(Uri sliceUri, boolean isEnhanced4gLteEnabled, int subId) {
        final IconCompat icon = IconCompat.createWithResource(mContext,
                R.drawable.ic_launcher_settings);

        return new ListBuilder(mContext, sliceUri, ListBuilder.INFINITY)
                .setAccentColor(Utils.getColorAccentDefaultColor(mContext))
                .addRow(new RowBuilder()
                        .setTitle(getEnhanced4glteModeTitle(subId))
                        .addEndItem(
                                SliceAction.createToggle(
                                        getBroadcastIntent(ACTION_ENHANCED_4G_LTE_CHANGED),
                                        null /* actionTitle */, isEnhanced4gLteEnabled))
                        .setPrimaryAction(
                                SliceAction.createDeeplink(
                                        getActivityIntent(ACTION_MOBILE_NETWORK_SETTINGS_ACTIVITY),
                                        icon,
                                        ListBuilder.ICON_IMAGE,
                                        getEnhanced4glteModeTitle(subId))))
                .build();
    }

    /**
     * Handles Enhanced 4G LTE mode setting change from Enhanced 4G LTE slice and posts
     * notification. Should be called when intent action is ACTION_ENHANCED_4G_LTE_CHANGED
     *
     * @param intent action performed
     */
    public void handleEnhanced4gLteChanged(Intent intent) {
        // skip checking when no toggle state update contained within Intent
        final boolean newValue = intent.getBooleanExtra(EXTRA_TOGGLE_STATE, false);
        if (newValue != intent.getBooleanExtra(EXTRA_TOGGLE_STATE, true)) {
            notifyEnhanced4gLteUpdate();
            return;
        }

        final int subId = getDefaultVoiceSubId();
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            notifyEnhanced4gLteUpdate();
            return;
        }

        final VolteQueryImsState queryState = queryImsState(subId);
        final boolean currentValue = queryState.isEnabledByUser()
                && queryState.isAllowUserControl();
        if (newValue == currentValue) {
            notifyEnhanced4gLteUpdate();
            return;
        }

        // isVoLteProvisioned() is the last item to check since it might block the main thread
        if (queryState.isVoLteProvisioned()) {
            setEnhanced4gLteModeSetting(subId, newValue);
        }
        notifyEnhanced4gLteUpdate();
    }

    private void notifyEnhanced4gLteUpdate() {
        // notify change in slice in any case to get re-queried. This would result in displaying
        // appropriate message with the updated setting.
        mContext.getContentResolver().notifyChange(CustomSliceRegistry.ENHANCED_4G_SLICE_URI, null);
    }

    @VisibleForTesting
    void setEnhanced4gLteModeSetting(int subId, boolean isEnabled) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return;
        }
        final ImsMmTelManager imsMmTelManager = ImsMmTelManager.createForSubscriptionId(subId);
        if (imsMmTelManager == null) {
            return;
        }
        try {
            imsMmTelManager.setAdvancedCallingSettingEnabled(isEnabled);
        } catch (IllegalArgumentException exception) {
            Log.w(TAG, "Unable to change the Enhanced 4g LTE to " + isEnabled + ". subId=" + subId,
                    exception);
        }
    }

    private CharSequence getEnhanced4glteModeTitle(int subId) {
        CharSequence ret = mContext.getText(R.string.enhanced_4g_lte_mode_title);
        try {
            if (isCarrierConfigManagerKeyEnabled(
                    CarrierConfigManager.KEY_ENHANCED_4G_LTE_TITLE_VARIANT_BOOL,
                    subId,
                    false)) {
                final PackageManager manager = mContext.getPackageManager();
                final Resources resources = manager.getResourcesForApplication(
                        PACKAGE_PHONE);
                final int resId = resources.getIdentifier(
                        RESOURCE_ENHANCED_4G_LTE_MODE_TITLE_VARIANT,
                        RESOURCE_TYPE_STRING, PACKAGE_PHONE);
                ret = resources.getText(resId);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "package name not found");
        }
        return ret;
    }

    /**
     * Returns {@code true} when the key is enabled for the carrier, and {@code false} otherwise.
     */
    private boolean isCarrierConfigManagerKeyEnabled(String key,
            int subId, boolean defaultValue) {
        final CarrierConfigManager configManager = getCarrierConfigManager();
        boolean ret = defaultValue;
        if (configManager != null) {
            final PersistableBundle bundle = configManager.getConfigForSubId(subId);
            if (bundle != null) {
                ret = bundle.getBoolean(key, defaultValue);
            }
        }
        return ret;
    }

    protected CarrierConfigManager getCarrierConfigManager() {
        return mContext.getSystemService(CarrierConfigManager.class);
    }

    private PendingIntent getBroadcastIntent(String action) {
        final Intent intent = new Intent(action);
        intent.setClass(mContext, SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(mContext, 0 /* requestCode */, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    /**
     * Returns the current default voice subId obtained from SubscriptionManager
     */
    protected int getDefaultVoiceSubId() {
        return SubscriptionManager.getDefaultVoiceSubscriptionId();
    }

    /**
     * Returns PendingIntent to start activity specified by action
     */
    private PendingIntent getActivityIntent(String action) {
        final Intent intent = new Intent(action);
        intent.setPackage(SETTINGS_PACKAGE_NAME);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(mContext, 0 /* requestCode */, intent, 0 /* flags */);
    }

    @VisibleForTesting
    VolteQueryImsState queryImsState(int subId) {
        return new VolteQueryImsState(mContext, subId);
    }
}

