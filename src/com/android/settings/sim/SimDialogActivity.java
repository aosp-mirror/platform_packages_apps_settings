/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.sim;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.network.CarrierConfigCache;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.ims.WifiCallingQueryImsState;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.network.telephony.SubscriptionActionDialogActivity;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.List;

/**
 * This activity provides singleton semantics per dialog type for showing various kinds of
 * dialogs asking the user to make choices about which SIM to use for various services
 * (calls, SMS, and data).
 */
public class SimDialogActivity extends FragmentActivity {
    private static String TAG = "SimDialogActivity";

    public static String PREFERRED_SIM = "preferred_sim";
    public static String DIALOG_TYPE_KEY = "dialog_type";
    // sub ID returned from startActivityForResult
    public static String RESULT_SUB_ID = "result_sub_id";
    public static final int INVALID_PICK = -1;
    public static final int DATA_PICK = 0;
    public static final int CALLS_PICK = 1;
    public static final int SMS_PICK = 2;
    public static final int PREFERRED_PICK = 3;
    // Show the "select SMS subscription" dialog, but don't save as default, just return a result
    public static final int SMS_PICK_FOR_MESSAGE = 4;
    // Dismiss the current dialog and finish the activity.
    public static final int PICK_DISMISS = 5;
    // Show auto data switch dialog(when user enables multi-SIM)
    public static final int ENABLE_AUTO_DATA_SWITCH = 6;

    private MetricsFeatureProvider mMetricsFeatureProvider;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isUiRestricted()) {
            finish();
            return;
        }
        if (!SubscriptionUtil.isSimHardwareVisible(this)) {
            Log.d(TAG, "Not support on device without SIM.");
            finish();
            return;
        }
        SimDialogProhibitService.supportDismiss(this);

        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        getWindow().addSystemFlags(
                WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
        showOrUpdateDialog();
    }

    @VisibleForTesting
    boolean isUiRestricted() {
        if (MobileNetworkUtils.isMobileNetworkUserRestricted(getApplicationContext())) {
            Log.e(TAG, "This setting isn't available due to user restriction.");
            return true;
        }
        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showOrUpdateDialog();
    }

    private int getProgressState() {
        final SharedPreferences prefs = getSharedPreferences(
                SubscriptionActionDialogActivity.SIM_ACTION_DIALOG_PREFS, MODE_PRIVATE);
        return prefs.getInt(SubscriptionActionDialogActivity.KEY_PROGRESS_STATE,
                SubscriptionActionDialogActivity.PROGRESS_IS_NOT_SHOWING);
    }

    private void showOrUpdateDialog() {
        final int dialogType = getIntent().getIntExtra(DIALOG_TYPE_KEY, INVALID_PICK);

        if (dialogType == PICK_DISMISS) {
            finishAndRemoveTask();
            return;
        }

        if (dialogType == PREFERRED_PICK
                && getProgressState() == SubscriptionActionDialogActivity.PROGRESS_IS_SHOWING) {
            Log.d(TAG, "Finish the sim dialog since the sim action dialog is showing the progress");
            finish();
            return;
        }

        final String tag = Integer.toString(dialogType);
        final FragmentManager fragmentManager = getSupportFragmentManager();
        SimDialogFragment fragment = (SimDialogFragment) fragmentManager.findFragmentByTag(tag);

        if (fragment == null) {
            fragment = createFragment(dialogType);
            fragment.show(fragmentManager, tag);
        } else {
            fragment.updateDialog();
        }
    }

    private SimDialogFragment createFragment(int dialogType) {
        switch (dialogType) {
            case DATA_PICK:
                return getDataPickDialogFragment();
            case CALLS_PICK:
                return CallsSimListDialogFragment.newInstance(dialogType,
                        R.string.select_sim_for_calls,
                        true /* includeAskEveryTime */,
                        false /* isCancelItemShowed */);
            case SMS_PICK:
                return SimListDialogFragment.newInstance(dialogType, R.string.select_sim_for_sms,
                        true /* includeAskEveryTime */,
                        false /* isCancelItemShowed */);
            case PREFERRED_PICK:
                if (!getIntent().hasExtra(PREFERRED_SIM)) {
                    throw new IllegalArgumentException("Missing required extra " + PREFERRED_SIM);
                }
                return PreferredSimDialogFragment.newInstance();
            case SMS_PICK_FOR_MESSAGE:
                return SimListDialogFragment.newInstance(dialogType, R.string.select_sim_for_sms,
                        false /* includeAskEveryTime */,
                        false /* isCancelItemShowed */);
            case ENABLE_AUTO_DATA_SWITCH:
                return EnableAutoDataSwitchDialogFragment.newInstance();
            default:
                throw new IllegalArgumentException("Invalid dialog type " + dialogType + " sent.");
        }
    }

    private SimDialogFragment getDataPickDialogFragment() {
        if (SubscriptionManager.getDefaultDataSubscriptionId()
                == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return SimListDialogFragment.newInstance(DATA_PICK, R.string.select_sim_for_data,
                    false /* includeAskEveryTime */,
                    true /* isCancelItemShowed */);
        }
        return SelectSpecificDataSimDialogFragment.newInstance();
    }

    public void onSubscriptionSelected(int dialogType, int subId) {
        if (getSupportFragmentManager().findFragmentByTag(Integer.toString(dialogType)) == null) {
            Log.w(TAG, "onSubscriptionSelected ignored because stored fragment was null");
            return;
        }
        switch (dialogType) {
            case DATA_PICK:
                setDefaultDataSubId(subId);
                break;
            case CALLS_PICK:
                setDefaultCallsSubId(subId);
                break;
            case SMS_PICK:
                setDefaultSmsSubId(subId);
                break;
            case PREFERRED_PICK:
                setPreferredSim(subId);
                break;
            case SMS_PICK_FOR_MESSAGE:
                // Don't set a default here.
                // The caller has created this dialog waiting for a result.
                Intent intent = new Intent();
                intent.putExtra(RESULT_SUB_ID, subId);
                setResult(Activity.RESULT_OK, intent);
                break;
            case ENABLE_AUTO_DATA_SWITCH:
                onEnableAutoDataSwitch(subId);
                break;
            default:
                throw new IllegalArgumentException(
                        "Invalid dialog type " + dialogType + " sent.");
        }
    }

    private PersistableBundle getCarrierConfigForSubId(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return null;
        }
        return CarrierConfigCache.getInstance(this).getConfigForSubId(subId);
    }

    private boolean isCrossSimCallingAllowedByPlatform(int subId) {
        if ((new WifiCallingQueryImsState(this, subId)).isWifiCallingSupported()) {
            PersistableBundle bundle = getCarrierConfigForSubId(subId);
            return (bundle != null) && bundle.getBoolean(
                    CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL,
                    false /*default*/);
        }
        return false;
    }

    private ImsMmTelManager getImsMmTelManager(int subId) {
        ImsManager imsMgr = getSystemService(ImsManager.class);
        return (imsMgr == null) ? null : imsMgr.getImsMmTelManager(subId);
    }

    private void trySetCrossSimCallingPerSub(int subId, boolean enabled) {
        try {
            getImsMmTelManager(subId).setCrossSimCallingEnabled(enabled);
        } catch (ImsException | IllegalArgumentException | NullPointerException exception) {
            Log.w(TAG, "failed to change cross SIM calling configuration to " + enabled
                    + " for subID " + subId + "with exception: ", exception);
        }
    }

    private boolean autoDataSwitchEnabledOnNonDataSub(@NonNull int[] subIds, int defaultDataSub) {
        for (int subId : subIds) {
            if (subId != defaultDataSub) {
                final TelephonyManager telephonyManager = getSystemService(
                        TelephonyManager.class).createForSubscriptionId(subId);
                if (telephonyManager.isMobileDataPolicyEnabled(
                        TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void trySetCrossSimCalling(int[] subIds, boolean enabled) {
        mMetricsFeatureProvider.action(this,
                SettingsEnums.ACTION_UPDATE_CROSS_SIM_CALLING_ON_2ND_SIM_ENABLE, enabled);
        for (int subId : subIds) {
            if (isCrossSimCallingAllowedByPlatform(subId)) {
                trySetCrossSimCallingPerSub(subId, enabled);
            }
        }
    }

    /**
     * Show dialog prompting the user to enable auto data switch
     */
    public void showEnableAutoDataSwitchDialog() {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        SimDialogFragment fragment = createFragment(ENABLE_AUTO_DATA_SWITCH);

        if (fragmentManager.isStateSaved()) {
            Log.w(TAG, "Failed to show EnableAutoDataSwitchDialog. The fragmentManager "
                    + "is StateSaved.");
            forceClose();
            return;
        }
        try {
            fragment.show(fragmentManager, Integer.toString(ENABLE_AUTO_DATA_SWITCH));
        } catch (Exception e) {
            Log.e(TAG, "Failed to show EnableAutoDataSwitchDialog.", e);
            forceClose();
            return;
        }
        if (getResources().getBoolean(
                R.bool.config_auto_data_switch_enables_cross_sim_calling)) {
            // If auto data switch is already enabled on the non-DDS, the dialog for enabling it
            // is suppressed (no onEnableAutoDataSwitch()). so we ensure cross-SIM calling is
            // enabled.

            // OTOH, if auto data switch is disabled on the new non-DDS, the user may still not
            // enable it in the dialog. So we ensure cross-SIM calling is disabled before the
            // dialog. If the user does enable auto data switch, we will re-enable cross-SIM calling
            // through onEnableAutoDataSwitch()- a minor redundancy to ensure correctness.
            final SubscriptionManager subscriptionManager =
                    getSystemService(SubscriptionManager.class);
            int[] subIds = subscriptionManager.getActiveSubscriptionIdList();
            int defaultDataSub = subscriptionManager.getDefaultDataSubscriptionId();
            if (subIds.length > 1) {
                trySetCrossSimCalling(subIds,
                        autoDataSwitchEnabledOnNonDataSub(subIds, defaultDataSub));
            }
        }
    }

    /**
     * @param subId The sub Id to enable auto data switch
     */
    public void onEnableAutoDataSwitch(int subId) {
        Log.d(TAG, "onEnableAutoDataSwitch subId:" + subId);
        final TelephonyManager telephonyManager = getSystemService(
                TelephonyManager.class).createForSubscriptionId(subId);
        telephonyManager.setMobileDataPolicyEnabled(
                TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH, true);

        if (getResources().getBoolean(
                R.bool.config_auto_data_switch_enables_cross_sim_calling)) {
            final SubscriptionManager subscriptionManager =
                    getSystemService(SubscriptionManager.class);
            trySetCrossSimCalling(subscriptionManager.getActiveSubscriptionIdList(),
                    true /* enabled */);
        }
    }

    public void onFragmentDismissed(SimDialogFragment simDialogFragment) {
        final List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments.size() == 1 && fragments.get(0) == simDialogFragment
                || simDialogFragment.getDialogType() == ENABLE_AUTO_DATA_SWITCH) {
            Log.d(TAG, "onFragmentDismissed dialogType:" + simDialogFragment.getDialogType());
            finishAndRemoveTask();
        }
    }

    private void setDefaultDataSubId(final int subId) {
        final SubscriptionManager subscriptionManager = getSystemService(SubscriptionManager.class);
        final TelephonyManager telephonyManager = getSystemService(
                TelephonyManager.class).createForSubscriptionId(subId);
        subscriptionManager.setDefaultDataSubId(subId);
        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            telephonyManager.setDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER,
                    true);
            Toast.makeText(this, R.string.data_switch_started, Toast.LENGTH_LONG).show();
        }
    }

    private void setDefaultCallsSubId(final int subId) {
        final PhoneAccountHandle phoneAccount = subscriptionIdToPhoneAccountHandle(subId);
        final TelecomManager telecomManager = getSystemService(TelecomManager.class);
        telecomManager.setUserSelectedOutgoingPhoneAccount(phoneAccount);
    }

    private void setDefaultSmsSubId(final int subId) {
        final SubscriptionManager subscriptionManager = getSystemService(SubscriptionManager.class);
        subscriptionManager.setDefaultSmsSubId(subId);
    }

    private void setPreferredSim(final int subId) {
        setDefaultDataSubId(subId);
        setDefaultSmsSubId(subId);
        setDefaultCallsSubId(subId);
    }

    private PhoneAccountHandle subscriptionIdToPhoneAccountHandle(final int subId) {
        final TelecomManager telecomManager = getSystemService(TelecomManager.class);
        final TelephonyManager telephonyManager = getSystemService(TelephonyManager.class);

        for (PhoneAccountHandle handle : telecomManager.getCallCapablePhoneAccounts()) {
            if (subId == telephonyManager.getSubscriptionId(handle)) {
                return handle;
            }
        }
        return null;
    }

    /*
     * Force dismiss this Activity.
     */
    protected void forceClose() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        Log.d(TAG, "Dismissed by Service");
        finishAndRemoveTask();
    }
}
