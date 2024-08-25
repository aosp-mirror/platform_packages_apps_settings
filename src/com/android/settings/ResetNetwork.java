/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.network.ResetNetworkRestrictionViewBuilder;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.telephony.EuiccRacConnectivityDialogActivity;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.password.ConfirmLockPattern;
import com.android.settings.system.reset.ResetNetworkConfirm;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Confirm and execute a reset of the device's network settings to a clean "just out of the box"
 * state.  Multiple confirmations are required: first, a general "are you sure you want to do this?"
 * prompt, followed by a keyguard pattern trace if the user has defined one, followed by a final
 * strongly-worded "THIS WILL RESET EVERYTHING" prompt.  If at any time the phone is allowed to go
 * to sleep, is locked, et cetera, then the confirmation sequence is abandoned.
 *
 * This is the initial screen.
 */
public class ResetNetwork extends InstrumentedFragment {
    private static final String TAG = "ResetNetwork";

    // Arbitrary to avoid conficts
    private static final int KEYGUARD_REQUEST = 55;

    private ActivityResultLauncher mActivityResultLauncher;
    private List<SubscriptionInfo> mSubscriptions;

    private View mContentView;
    private Spinner mSubscriptionSpinner;
    private Button mInitiateButton;
    @VisibleForTesting View mEsimContainer;
    @VisibleForTesting CheckBox mEsimCheckbox;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.reset_mobile_network_settings_title);

        mActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> onActivityLauncherResult(result));
    }

    /**
     * Keyguard validation is run using the standard {@link ConfirmLockPattern}
     * component as a subactivity
     * @param request the request code to be returned once confirmation finishes
     * @return true if confirmation launched
     */
    private boolean runKeyguardConfirmation(int request) {
        Resources res = getActivity().getResources();
        final ChooseLockSettingsHelper.Builder builder =
                new ChooseLockSettingsHelper.Builder(getActivity(), this);
        return builder.setRequestCode(request)
                .setTitle(res.getText(R.string.reset_mobile_network_settings_title))
                .setActivityResultLauncher(mActivityResultLauncher)
                .show();
    }

    public void onActivityLauncherResult(ActivityResult result) {
        // If the user entered a valid keyguard trace, present the final
        // confirmation prompt; otherwise, go back to the initial state.
        if (result.getResultCode() == Activity.RESULT_OK) {
            showFinalConfirmation();
        } else if (mContentView != null) {
            establishInitialState(getActiveSubscriptionInfoList());
        }
    }

    @VisibleForTesting
    void showFinalConfirmation() {
        Bundle args = new Bundle();
        Context context = getContext();
        boolean resetSims = false;

        // TODO(b/317276437) Simplify the logic once flag is released
        int resetOptions = ResetNetworkRequest.RESET_CONNECTIVITY_MANAGER
                        | ResetNetworkRequest.RESET_VPN_MANAGER;
        if (Flags.resetMobileNetworkSettings()) {
            resetOptions |= ResetNetworkRequest.RESET_IMS_STACK;
            resetOptions |= ResetNetworkRequest.RESET_PHONE_PROCESS;
        }
        ResetNetworkRequest request = new ResetNetworkRequest(resetOptions);
        if (mSubscriptions != null && mSubscriptions.size() > 0) {
            int selectedIndex = mSubscriptionSpinner.getSelectedItemPosition();
            SubscriptionInfo subscription = mSubscriptions.get(selectedIndex);
            int subId = subscription.getSubscriptionId();
            request.setResetTelephonyAndNetworkPolicyManager(subId)
                   .setResetApn(subId);
            if (Flags.resetMobileNetworkSettings()) {
                request.setResetImsSubId(subId);
            }
        }
        if (mEsimContainer.getVisibility() == View.VISIBLE && mEsimCheckbox.isChecked()) {
            resetSims = true;
            request.setResetEsim(context.getPackageName()).writeIntoBundle(args);
        } else {
            request.writeIntoBundle(args);
        }

        SubSettingLauncher launcher =
                new SubSettingLauncher(context)
                        .setDestination(ResetNetworkConfirm.class.getName())
                        .setArguments(args)
                        .setTitleRes(R.string.reset_mobile_network_settings_confirm_title)
                        .setSourceMetricsCategory(getMetricsCategory());

        if (resetSims && SubscriptionUtil.shouldShowRacDialogWhenErasingAllEsims(context)) {
            context.startActivity(
                    EuiccRacConnectivityDialogActivity.getIntent(context, launcher.toIntent()));
        } else {
            launcher.launch();
        }
    }

    /**
     * If the user clicks to begin the reset sequence, we next require a
     * keyguard confirmation if the user has currently enabled one.  If there
     * is no keyguard available, we simply go to the final confirmation prompt.
     */
    private final Button.OnClickListener mInitiateListener = new Button.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (!runKeyguardConfirmation(KEYGUARD_REQUEST)) {
                showFinalConfirmation();
            }
        }
    };

    /**
     * In its initial state, the activity presents a button for the user to
     * click in order to initiate a confirmation sequence.  This method is
     * called from various other points in the code to reset the activity to
     * this base state.
     *
     * <p>Reinflating views from resources is expensive and prevents us from
     * caching widget pointers, so we use a single-inflate pattern:  we lazy-
     * inflate each view, caching all of the widget pointers we'll need at the
     * time, then simply reuse the inflated views directly whenever we need
     * to change contents.
     *
     * @param subscriptionsList is a list of SubscriptionInfo(s) which allow user to select from
     */
    private void establishInitialState(List<SubscriptionInfo> subscriptionsList) {
        mSubscriptionSpinner = (Spinner) mContentView.findViewById(R.id.reset_network_subscription);
        mEsimContainer = mContentView.findViewById(R.id.erase_esim_container);
        mEsimCheckbox = mContentView.findViewById(R.id.erase_esim);

        mSubscriptions = subscriptionsList;
        if (mSubscriptions != null && mSubscriptions.size() > 0) {
            // Get the default subscription in the order of data, voice, sms, first up.
            int defaultSubscription = SubscriptionManager.getDefaultDataSubscriptionId();
            if (!SubscriptionManager.isUsableSubscriptionId(defaultSubscription)) {
                defaultSubscription = SubscriptionManager.getDefaultVoiceSubscriptionId();
            }
            if (!SubscriptionManager.isUsableSubscriptionId(defaultSubscription)) {
                defaultSubscription = SubscriptionManager.getDefaultSmsSubscriptionId();
            }
            if (!SubscriptionManager.isUsableSubscriptionId(defaultSubscription)) {
                defaultSubscription = SubscriptionManager.getDefaultSubscriptionId();
            }

            int selectedIndex = 0;
            int size = mSubscriptions.size();
            List<String> subscriptionNames = new ArrayList<>();
            for (SubscriptionInfo record : mSubscriptions) {
                if (record.getSubscriptionId() == defaultSubscription) {
                    // Set the first selected value to the default
                    selectedIndex = subscriptionNames.size();
                }
                String name = SubscriptionUtil.getUniqueSubscriptionDisplayName(
                        record, getContext()).toString();
                if (TextUtils.isEmpty(name)) {
                    name = record.getNumber();
                }
                if (TextUtils.isEmpty(name)) {
                    CharSequence carrierName = record.getCarrierName();
                    name = TextUtils.isEmpty(carrierName) ? "" : carrierName.toString();
                }
                if (TextUtils.isEmpty(name)) {
                    name = String.format("MCC:%s MNC:%s Slot:%s Id:%s", record.getMcc(),
                            record.getMnc(), record.getSimSlotIndex(), record.getSubscriptionId());
                }
                subscriptionNames.add(name);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_spinner_item, subscriptionNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSubscriptionSpinner.setAdapter(adapter);
            mSubscriptionSpinner.setSelection(selectedIndex);
            if (mSubscriptions.size() > 1) {
                mSubscriptionSpinner.setVisibility(View.VISIBLE);
            } else {
                mSubscriptionSpinner.setVisibility(View.INVISIBLE);
            }
        } else {
            mSubscriptionSpinner.setVisibility(View.INVISIBLE);
        }
        mInitiateButton = (Button) mContentView.findViewById(R.id.initiate_reset_network);
        mInitiateButton.setOnClickListener(mInitiateListener);
        if (showEuiccSettings(getContext())) {
            mEsimContainer.setVisibility(View.VISIBLE);
            mEsimContainer.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mEsimCheckbox.toggle();
                }
            });
        } else {
            mEsimCheckbox.setChecked(false /* checked */);
        }
    }

    private List<SubscriptionInfo> getActiveSubscriptionInfoList() {
        if (!SubscriptionUtil.isSimHardwareVisible(getActivity())) {
            return Collections.emptyList();
        }
        SubscriptionManager mgr = getActivity().getSystemService(SubscriptionManager.class);
        if (mgr == null) {
            Log.w(TAG, "No SubscriptionManager");
            return Collections.emptyList();
        }
        return Optional.ofNullable(mgr.getActiveSubscriptionInfoList())
                .orElse(Collections.emptyList());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mContentView == null) {
            return;
        }

        // update options if subcription has been changed
        List<SubscriptionInfo> updatedSubscriptions = getActiveSubscriptionInfoList();
        if ((mSubscriptions != null)
                && (mSubscriptions.size() == updatedSubscriptions.size())
                && mSubscriptions.containsAll(updatedSubscriptions)) {
            return;
        }
        Log.d(TAG, "subcription list changed");
        establishInitialState(updatedSubscriptions);
    }

    private boolean showEuiccSettings(Context context) {
        if (!SubscriptionUtil.isSimHardwareVisible(context)) {
            return false;
        }
        EuiccManager euiccManager =
                (EuiccManager) context.getSystemService(Context.EUICC_SERVICE);
        if (euiccManager == null || !euiccManager.isEnabled()) {
            return false;
        }
        ContentResolver resolver = context.getContentResolver();
        return Settings.Global.getInt(resolver, Global.EUICC_PROVISIONED, 0) != 0
                || DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = (new ResetNetworkRestrictionViewBuilder(getActivity())).build();
        if (view != null) {
            Log.w(TAG, "Access deny.");
            return view;
        }

        mContentView = inflater.inflate(R.layout.reset_mobile_network_settings, null);

        establishInitialState(getActiveSubscriptionInfoList());
        return mContentView;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.RESET_NETWORK;
    }
}
