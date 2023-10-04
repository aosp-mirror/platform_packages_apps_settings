/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.flags.Flags;
import com.android.settings.network.CarrierConfigCache;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * Preference controller for "Enable 2G"
 *
 * <p>
 * This preference controller is invoked per subscription id, which means toggling 2g is a per-sim
 * operation. The requested 2g preference is delegated to
 * {@link TelephonyManager#setAllowedNetworkTypesForReason(int reason, long allowedNetworkTypes)}
 * with:
 * <ul>
 *     <li>{@code reason} {@link TelephonyManager#ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G}.</li>
 *     <li>{@code allowedNetworkTypes} with set or cleared 2g-related bits, depending on the
 *     requested preference state. </li>
 * </ul>
 */
public class Enable2gPreferenceController extends TelephonyTogglePreferenceController {

    private static final String LOG_TAG = "Enable2gPreferenceController";
    private static final long BITMASK_2G = TelephonyManager.NETWORK_TYPE_BITMASK_GSM
            | TelephonyManager.NETWORK_TYPE_BITMASK_GPRS
            | TelephonyManager.NETWORK_TYPE_BITMASK_EDGE
            | TelephonyManager.NETWORK_TYPE_BITMASK_CDMA
            | TelephonyManager.NETWORK_TYPE_BITMASK_1xRTT;

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private CarrierConfigCache mCarrierConfigCache;
    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;
    private RestrictedSwitchPreference mRestrictedPreference;

    /**
     * Class constructor of "Enable 2G" toggle.
     *
     * @param context of settings
     * @param key     assigned within UI entry of XML file
     */
    public Enable2gPreferenceController(Context context, String key) {
        super(context, key);
        mCarrierConfigCache = CarrierConfigCache.getInstance(context);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mRestrictedPreference = null;
    }

    /**
     * Initialization based on a given subscription id.
     *
     * @param subId is the subscription id
     * @return this instance after initialization
     */
    public Enable2gPreferenceController init(int subId) {
        mSubId = subId;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(mSubId);
        return this;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mRestrictedPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        // The device admin decision overrides any carrier preferences
        if (isDisabledByAdmin()) {
            return;
        }

        if (preference == null || !SubscriptionManager.isUsableSubscriptionId(mSubId)) {
            return;
        }

        // TODO: b/303411083 remove all dynamic logic and rely on summary in resource file once flag
        //  is no longer needed
        if (Flags.removeKeyHideEnable2g()) {
            preference.setSummary(mContext.getString(R.string.enable_2g_summary));
        } else {
            final PersistableBundle carrierConfig = mCarrierConfigCache.getConfigForSubId(mSubId);
            boolean isDisabledByCarrier =
                    carrierConfig != null
                            && carrierConfig.getBoolean(CarrierConfigManager.KEY_HIDE_ENABLE_2G);
            preference.setEnabled(!isDisabledByCarrier);
            String summary;
            if (isDisabledByCarrier) {
                summary = mContext.getString(R.string.enable_2g_summary_disabled_carrier,
                        getSimCardName());
            } else {
                summary = mContext.getString(R.string.enable_2g_summary);
            }
            preference.setSummary(summary);
        }
    }

    private String getSimCardName() {
        SubscriptionInfo subInfo = SubscriptionUtil.getSubById(mSubscriptionManager, mSubId);
        if (subInfo == null) {
            return "";
        }
        // It is the sim card name, and it should be the same name as the sim page.
        CharSequence simCardName = subInfo.getDisplayName();
        return TextUtils.isEmpty(simCardName) ? "" : simCardName.toString();
    }

    /**
     * Get the {@link com.android.settings.core.BasePreferenceController.AvailabilityStatus} for
     * this preference given a {@code subId}.
     * <p>
     * A return value of {@link #AVAILABLE} denotes that the 2g status can be updated for this
     * particular subscription.
     * We return {@link #AVAILABLE} if the following conditions are met and {@link
     * #CONDITIONALLY_UNAVAILABLE} otherwise.
     * <ul>
     *     <li>The subscription is usable {@link SubscriptionManager#isUsableSubscriptionId}</li>
     *     <li>The carrier has not opted to disable this preference
     *     {@link CarrierConfigManager#KEY_HIDE_ENABLE_2G}</li>
     *     <li>The device supports
     *     <a href="https://cs.android.com/android/platform/superproject/+/master:hardware/interfaces/radio/1.6/IRadio.hal">Radio HAL version 1.6 or greater</a> </li>
     * </ul>
     */
    @Override
    public int getAvailabilityStatus(int subId) {
        if (mTelephonyManager == null) {
            Log.w(LOG_TAG, "Telephony manager not yet initialized");
            mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        }
        boolean visible =
                SubscriptionManager.isUsableSubscriptionId(subId)
                        && mTelephonyManager.isRadioInterfaceCapabilitySupported(
                        mTelephonyManager.CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK);
        return visible ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    /**
     * Return {@code true} if 2g is currently enabled.
     *
     * <p><b>NOTE:</b> This method returns the active state of the preference controller and is not
     * the parameter passed into {@link #setChecked(boolean)}, which is instead the requested future
     * state.</p>
     */
    @Override
    public boolean isChecked() {
        // If an enterprise admin has disabled 2g, we show the toggle as not checked to avoid
        // user confusion of seeing a checked toggle, but having 2g actually disabled.
        // The RestrictedSwitchPreference will take care of transparently informing the user that
        // the setting was disabled by their admin
        if (isDisabledByAdmin()) {
            return false;
        }

        long currentlyAllowedNetworkTypes = mTelephonyManager.getAllowedNetworkTypesForReason(
                mTelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G);
        return (currentlyAllowedNetworkTypes & BITMASK_2G) != 0;
    }

    /**
     * Ensure that the modem's allowed network types are configured according to the user's
     * preference.
     * <p>
     * See {@link com.android.settings.core.TogglePreferenceController#setChecked(boolean)} for
     * details.
     *
     * @param isChecked The toggle value that we're being requested to enforce. A value of {@code
     *                  false} denotes that 2g will be disabled by the modem after this function
     *                  completes, if it is not already.
     */
    @Override
    public boolean setChecked(boolean isChecked) {
        if (isDisabledByAdmin()) {
            return false;
        }

        if (!SubscriptionManager.isUsableSubscriptionId(mSubId)) {
            return false;
        }
        long currentlyAllowedNetworkTypes = mTelephonyManager.getAllowedNetworkTypesForReason(
                mTelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G);
        boolean enabled = (currentlyAllowedNetworkTypes & BITMASK_2G) != 0;
        if (enabled == isChecked) {
            return false;
        }
        long newAllowedNetworkTypes = currentlyAllowedNetworkTypes;
        if (isChecked) {
            newAllowedNetworkTypes = currentlyAllowedNetworkTypes | BITMASK_2G;
            Log.i(LOG_TAG, "Enabling 2g. Allowed network types: " + newAllowedNetworkTypes);
        } else {
            newAllowedNetworkTypes = currentlyAllowedNetworkTypes & ~BITMASK_2G;
            Log.i(LOG_TAG, "Disabling 2g. Allowed network types: " + newAllowedNetworkTypes);
        }
        mTelephonyManager.setAllowedNetworkTypesForReason(
                mTelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G, newAllowedNetworkTypes);
        mMetricsFeatureProvider.action(
                mContext, SettingsEnums.ACTION_2G_ENABLED, isChecked);
        return true;
    }

    private boolean isDisabledByAdmin() {
        return (mRestrictedPreference != null && mRestrictedPreference.isDisabledByAdmin());
    }
}
